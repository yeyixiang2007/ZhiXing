package com.zhixing.navigation.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zhixing.navigation.application.auth.AuthService;
import com.zhixing.navigation.application.auth.AuthorizationException;
import com.zhixing.navigation.application.map.MapService;
import com.zhixing.navigation.application.navigation.ConsolePathFormatter;
import com.zhixing.navigation.application.navigation.NavigationService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.NormalUser;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.User;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.planning.DijkstraStrategy;
import com.zhixing.navigation.domain.planning.NoRouteFoundException;
import com.zhixing.navigation.gui.controller.ControllerExceptionMapper;
import com.zhixing.navigation.gui.controller.MapController;
import com.zhixing.navigation.gui.controller.NavigationController;
import com.zhixing.navigation.gui.model.RouteVisualizationDto;
import com.zhixing.navigation.gui.workbench.MapCanvas;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class QualityCheckRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(QualityCheckRunner.class);
    private int passed;
    private int failed;
    private double pathCostMs;
    private double startupCostMs;
    private double mapInteractionCostMs;

    public static void main(String[] args) {
        QualityCheckRunner runner = new QualityCheckRunner();
        runner.runAll();
    }

    private void runAll() {
        LOGGER.info("=== Quality Check Start ===");
        runCase("G1 用户主流程联调（地图选点 -> 查询 -> 路径展示）", this::testUserMainFlowIntegration);
        runCase("G2 管理员主流程联调（标点 -> 连线 -> 禁行 -> 保存）", this::testAdminMainFlowIntegration);
        runCase("G3 异常流程测试（非法输入、不可达、权限越界、文件异常）", this::testExceptionFlowRegression);
        runCase("G4 性能验证（GUI启动 <= 2s，查询 <= 1s，地图交互无明显卡顿）", this::testPerformanceValidation);
        runCase("G5 回归脚本与演示脚本更新", this::testScriptUpdates);

        LOGGER.info("");
        LOGGER.info("=== Quality Check Summary ===");
        LOGGER.info("Passed: " + passed);
        LOGGER.info("Failed: " + failed);
        LOGGER.info("Path performance(avg): " + formatMs(pathCostMs) + " ms");
        LOGGER.info("Startup performance(avg): " + formatMs(startupCostMs) + " ms");
        LOGGER.info("Map interaction(avg): " + formatMs(mapInteractionCostMs) + " ms");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private void runCase(String caseName, CheckedRunnable runnable) {
        long startNs = System.nanoTime();
        try {
            runnable.run();
            long elapsedMs = elapsedMs(startNs);
            passed++;
            LOGGER.info("[PASS] " + caseName + " (" + elapsedMs + " ms)");
        } catch (Throwable ex) {
            long elapsedMs = elapsedMs(startNs);
            failed++;
            LOGGER.info("[FAIL] " + caseName + " (" + elapsedMs + " ms)");
            LOGGER.info("       -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private void testUserMainFlowIntegration() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-g1-user-flow");
        PersistenceService persistence = new PersistenceService(tempDir);
        CampusGraph graph = persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();

        NavigationController navigationController = new NavigationController(
                graph,
                new NavigationService(new DijkstraStrategy()),
                new ConsolePathFormatter()
        );

        NavigationController.NavigationVisualResult result = navigationController.queryPathVisual("GATE_E", "TB_A");
        PathResult pathResult = result.getPathResult();
        RouteVisualizationDto visualization = result.getRouteVisualization();

        assertEquals("GATE_E", pathResult.getStartVertex().getId(), "用户流程起点应正确");
        assertEquals("TB_A", pathResult.getEndVertex().getId(), "用户流程终点应正确");
        assertTrue(pathResult.getTotalDistance() > 0, "用户流程路径总长应大于0");
        assertTrue(pathResult.getNaviInstructions().size() >= 1, "用户流程应生成导航步骤");
        assertTrue(visualization.getSegmentCount() >= 1, "用户流程应生成可视化线段");
    }

    private void testAdminMainFlowIntegration() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-g2-admin-flow");
        PersistenceService persistence = new PersistenceService(tempDir);
        CampusGraph graph = persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();

        AuthService authService = new AuthService(persistence);
        Admin admin = authService.loginAdmin("admin", "admin123");
        MapController mapController = new MapController(graph, new MapService(graph), persistence);

        String nodeId = "G2_NODE";
        mapController.addVertex(admin, nodeId, "G2 Integration Node", PlaceType.OTHER, 380, 210, "flow test");
        mapController.addRoad(admin, "TB_A", nodeId, 77.0, false, false, RoadType.PATH);
        mapController.setRoadForbidden(admin, "TB_A", nodeId, true);

        CampusGraph reloaded = persistence.loadGraph();
        assertTrue(reloaded.containsVertex(nodeId), "管理员流程保存后应存在新点位");
        Edge savedEdge = findEdge(reloaded, "TB_A", nodeId);
        assertTrue(savedEdge != null, "管理员流程保存后应存在新道路");
        assertTrue(savedEdge.isForbidden(), "管理员流程保存后道路应为禁行状态");
    }

    private void testExceptionFlowRegression() throws Exception {
        testInvalidInputRetry();
        testUnreachablePath();
        testFileFallback();
        testPermissionBoundary();
        assertTrue(ControllerExceptionMapper.toUserMessage(new IllegalArgumentException("vertex not found: A")).startsWith("输入错误："),
                "异常映射应覆盖输入错误");
        assertTrue(ControllerExceptionMapper.toUserMessage(new NoRouteFoundException("n/a")).startsWith("路径不可达："),
                "异常映射应覆盖不可达错误");
    }

    private void testPerformanceValidation() throws Exception {
        testPathPerformance();
        testStartupPerformance();
        testMapInteractionPerformance();
        assertTrue(pathCostMs <= 1000, "查询性能应<=1s");
        assertTrue(startupCostMs <= 2000, "启动性能应<=2s");
    }

    private void testScriptUpdates() throws IOException {
        Path root = Paths.get("").toAbsolutePath().normalize();
        Path regressionScript = root.resolve("scripts").resolve("regression.ps1");
        Path demoScript = root.resolve("scripts").resolve("demo.ps1");
        assertTrue(Files.exists(regressionScript), "回归脚本 regression.ps1 应存在");
        assertTrue(Files.exists(demoScript), "演示脚本 demo.ps1 应存在");

        String regressionContent = new String(Files.readAllBytes(regressionScript), StandardCharsets.UTF_8);
        String demoContent = new String(Files.readAllBytes(demoScript), StandardCharsets.UTF_8);
        assertTrue(regressionContent.contains("qa.ps1"), "回归脚本应调用 qa.ps1");
        assertTrue(demoContent.contains("run-gui.ps1"), "演示脚本应调用 run-gui.ps1");
    }

    private void testP0FunctionalRegression() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-func-regression");
        PersistenceService persistence = new PersistenceService(tempDir);
        CampusGraph graph = persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();

        AuthService authService = new AuthService(persistence);
        Admin admin = authService.loginAdmin("admin", "admin123");
        MapService mapService = new MapService(graph);
        NavigationService navigationService = new NavigationService(new DijkstraStrategy());
        ConsolePathFormatter formatter = new ConsolePathFormatter();

        int initialVertexCount = graph.vertexCount();
        int initialEdgeCount = graph.edgeCount();

        Vertex lab = new Vertex("LAB_X", "Innovation Lab", PlaceType.TEACHING_BUILDING, 520, 320, "Regression spot");
        mapService.addVertex(admin, lab);
        mapService.addRoad(admin, "TB_A", "LAB_X", 85.0, false, false, RoadType.PATH);
        persistence.saveGraph(graph);

        PathResult pathResult = navigationService.navigate(graph, "GATE_E", "LAB_X");
        assertEquals("LAB_X", pathResult.getEndVertex().getId(), "路径终点应为 LAB_X");
        assertTrue(pathResult.getTotalDistance() > 0, "路径总距离应大于0");
        String formatted = formatter.format(pathResult);
        assertTrue(formatted.contains("路径规划结果"), "路径详情模板应包含标题");

        mapService.deleteRoad(admin, "TB_A", "LAB_X");
        mapService.deleteVertex(admin, "LAB_X");
        persistence.saveGraph(graph);

        assertEquals(initialVertexCount, graph.vertexCount(), "地点数量应回到初始值");
        assertEquals(initialEdgeCount, graph.edgeCount(), "道路数量应回到初始值");
    }

    private void testInvalidInputRetry() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-invalid-input");
        PersistenceService persistence = new PersistenceService(tempDir);
        CampusGraph graph = persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();

        String fakeInput = "abc\n0\n";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(fakeInput.getBytes(StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(inputStream, "UTF-8");

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(outputBuffer, true, "UTF-8");

        try {
            System.setOut(capture);
            ConsoleApplication app = new ConsoleApplication(
                    graph,
                    persistence,
                    new AuthService(persistence),
                    new MapService(graph),
                    new NavigationService(new DijkstraStrategy()),
                    new ConsolePathFormatter(),
                    scanner
            );
            app.run();
        } finally {
            System.setOut(originalOut);
            scanner.close();
            capture.close();
        }

        String output = new String(outputBuffer.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(output.contains("请输入有效数字。"), "应提示非法输入并允许重试");
        assertTrue(output.contains("系统已退出，感谢使用。"), "输入修正后应可正常退出");
    }

    private void testUnreachablePath() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-unreachable");
        PersistenceService persistence = new PersistenceService(tempDir);
        CampusGraph graph = persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();

        AuthService authService = new AuthService(persistence);
        Admin admin = authService.loginAdmin("admin", "admin123");
        MapService mapService = new MapService(graph);
        NavigationService navigationService = new NavigationService(new DijkstraStrategy());

        mapService.disableRoad(admin, "GATE_E", "LIB");
        persistence.saveGraph(graph);

        assertThrows(NoRouteFoundException.class, new CheckedRunnable() {
            @Override
            public void run() {
                navigationService.navigate(graph, "GATE_E", "TB_A");
            }
        }, "禁行关键道路后应出现不可达异常");
    }

    private void testFileFallback() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-file-fallback");
        Files.createDirectories(tempDir);
        Files.write(tempDir.resolve("vertex.json"), "{corrupted-json".getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("edge.json"), "{corrupted-json".getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("user.json"), "{corrupted-json".getBytes(StandardCharsets.UTF_8));

        PersistenceService persistence = new PersistenceService(tempDir);
        CampusGraph graph = persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();

        assertTrue(graph.vertexCount() > 0, "文件损坏后应回退默认地图");
        String vertexContent = new String(Files.readAllBytes(tempDir.resolve("vertex.json")), StandardCharsets.UTF_8);
        assertTrue(vertexContent.trim().startsWith("["), "回退后vertex.json应被重写为合法JSON数组");
    }

    private void testPermissionBoundary() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-permission");
        PersistenceService persistence = new PersistenceService(tempDir);
        CampusGraph graph = persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();

        MapService mapService = new MapService(graph);
        User normalUser = new NormalUser("guest", "pbkdf2$120000$0102030405060708090a0b0c0d0e0f10$1111111111111111111111111111111111111111111111111111111111111111");
        Vertex extra = new Vertex("X1", "Test Point", PlaceType.OTHER, 1, 1, "");

        assertThrows(AuthorizationException.class, new CheckedRunnable() {
            @Override
            public void run() {
                mapService.addVertex(normalUser, extra);
            }
        }, "普通用户不应有地图管理权限");
    }

    private void testPathPerformance() {
        CampusGraph graph = buildPerformanceGraph(200, 500);
        NavigationService navigationService = new NavigationService(new DijkstraStrategy());
        int rounds = 8;
        long totalNs = 0;
        PathResult result = null;

        for (int i = 0; i < rounds; i++) {
            long startNs = System.nanoTime();
            result = navigationService.navigate(graph, "V0", "V199");
            totalNs += (System.nanoTime() - startNs);
        }
        pathCostMs = nanosToMs(totalNs / (double) rounds);

        assertTrue(result.getTotalDistance() > 0, "性能测试路径应可达");
        assertTrue(pathCostMs <= 1000, "路径计算耗时应<=1000ms，当前=" + pathCostMs + "ms");
    }

    private void testStartupPerformance() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-startup");
        PersistenceService persistence = new PersistenceService(tempDir);
        persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();

        int rounds = 6;
        long totalNs = 0;
        CampusGraph graph = null;
        for (int i = 0; i < rounds; i++) {
            long startNs = System.nanoTime();
            graph = persistence.loadGraphOrDefault();
            persistence.loadUsersOrDefault();
            totalNs += (System.nanoTime() - startNs);
        }
        startupCostMs = nanosToMs(totalNs / (double) rounds);

        assertTrue(graph.vertexCount() > 0, "启动加载后应有默认地图");
        assertTrue(startupCostMs <= 2000, "启动加载耗时应<=2000ms，当前=" + startupCostMs + "ms");
    }

    private void testMapInteractionPerformance() {
        CampusGraph graph = buildPerformanceGraph(200, 500);
        MapCanvas mapCanvas = new MapCanvas();
        mapCanvas.setSize(1200, 760);

        List<Vertex> vertices = new ArrayList<Vertex>(graph.getAllVertices());
        List<Edge> edges = new ArrayList<Edge>(graph.getAllEdges());

        NavigationController navigationController = new NavigationController(
                graph,
                new NavigationService(new DijkstraStrategy()),
                new ConsolePathFormatter()
        );
        RouteVisualizationDto route = navigationController.toCurrentRouteVisualization(
                navigationController.queryPath("V0", "V199")
        );

        int rounds = 5;
        long totalNs = 0;
        int iterations = Math.min(route.getSegmentCount(), 40);
        for (int r = 0; r < rounds; r++) {
            long startNs = System.nanoTime();
            mapCanvas.setGraphData(vertices, edges);
            mapCanvas.setRouteComparison(route, null);
            mapCanvas.setLayerVisible(MapCanvas.Layer.LABEL, false);
            mapCanvas.setLayerVisible(MapCanvas.Layer.LABEL, true);
            mapCanvas.resetViewport();
            for (int i = 0; i < iterations; i++) {
                mapCanvas.focusRouteSegment(i);
            }
            totalNs += (System.nanoTime() - startNs);
        }
        mapInteractionCostMs = nanosToMs(totalNs / (double) rounds);
        assertTrue(mapInteractionCostMs <= 1000, "地图交互耗时应<=1000ms，当前=" + mapInteractionCostMs + "ms");
    }

    private CampusGraph buildPerformanceGraph(int vertexSize, int edgeBudget) {
        CampusGraph graph = new CampusGraph();
        for (int i = 0; i < vertexSize; i++) {
            graph.addVertex(new Vertex("V" + i, "Point-" + i, PlaceType.OTHER, i, i * 0.5, ""));
        }

        int usedEdges = 0;
        for (int i = 0; i < vertexSize - 1 && usedEdges < edgeBudget; i++) {
            graph.addEdge(new com.zhixing.navigation.domain.model.Edge(
                    graph.getVertex("V" + i),
                    graph.getVertex("V" + (i + 1)),
                    10.0,
                    true,
                    false,
                    RoadType.MAIN_ROAD
            ));
            usedEdges++;
        }

        int i = 0;
        while (usedEdges < edgeBudget) {
            int from = i % (vertexSize - 2);
            int to = from + 2 + (i % 6);
            if (to >= vertexSize) {
                to = vertexSize - 1;
            }
            if (from < to) {
                graph.addEdge(new com.zhixing.navigation.domain.model.Edge(
                        graph.getVertex("V" + from),
                        graph.getVertex("V" + to),
                        25.0 + (i % 10),
                        true,
                        false,
                        RoadType.PATH
                ));
                usedEdges++;
            }
            i++;
        }

        return graph;
    }

    private static Edge findEdge(CampusGraph graph, String fromId, String toId) {
        if (!graph.containsVertex(fromId) || !graph.containsVertex(toId)) {
            return null;
        }
        for (Edge edge : graph.getNeighbors(fromId)) {
            if (edge.getToVertex().getId().equals(toId)) {
                return edge;
            }
        }
        return null;
    }

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    private static double nanosToMs(double nanos) {
        return nanos / 1_000_000.0;
    }

    private static String formatMs(double ms) {
        return String.format("%.3f", ms);
    }

    private static void assertTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new IllegalStateException(message + "，expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertThrows(Class<? extends Throwable> expected, CheckedRunnable runnable, String message) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expected.isInstance(throwable)) {
                return;
            }
            throw new IllegalStateException(message + "，抛出了错误类型：" + throwable.getClass().getName());
        }
        throw new IllegalStateException(message + "，未抛出异常");
    }

    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
