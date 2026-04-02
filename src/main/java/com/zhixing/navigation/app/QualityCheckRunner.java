package com.zhixing.navigation.app;

import com.zhixing.navigation.application.auth.AuthService;
import com.zhixing.navigation.application.auth.AuthorizationException;
import com.zhixing.navigation.application.map.MapService;
import com.zhixing.navigation.application.navigation.ConsolePathFormatter;
import com.zhixing.navigation.application.navigation.NavigationService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.NormalUser;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.User;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.planning.DijkstraStrategy;
import com.zhixing.navigation.domain.planning.NoRouteFoundException;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class QualityCheckRunner {
    private int passed;
    private int failed;
    private long pathCostMs;
    private long startupCostMs;

    public static void main(String[] args) {
        QualityCheckRunner runner = new QualityCheckRunner();
        runner.runAll();
    }

    private void runAll() {
        System.out.println("=== Quality Check Start ===");
        runCase("P0功能回归", this::testP0FunctionalRegression);
        runCase("异常测试-非法输入友好提示与重试", this::testInvalidInputRetry);
        runCase("异常测试-不可达路径", this::testUnreachablePath);
        runCase("异常测试-文件异常兜底", this::testFileFallback);
        runCase("异常测试-权限越界", this::testPermissionBoundary);
        runCase("性能测试-路径计算<=1s", this::testPathPerformance);
        runCase("性能测试-启动加载<=2s", this::testStartupPerformance);

        System.out.println();
        System.out.println("=== Quality Check Summary ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Path performance: " + pathCostMs + " ms");
        System.out.println("Startup performance: " + startupCostMs + " ms");

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
            System.out.println("[PASS] " + caseName + " (" + elapsedMs + " ms)");
        } catch (Throwable ex) {
            long elapsedMs = elapsedMs(startNs);
            failed++;
            System.out.println("[FAIL] " + caseName + " (" + elapsedMs + " ms)");
            System.out.println("       -> " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
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
        User normalUser = new NormalUser("guest", "sha256$0000000000000000000000000000000000000000000000000000000000000000");
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

        long startNs = System.nanoTime();
        PathResult result = navigationService.navigate(graph, "V0", "V199");
        pathCostMs = elapsedMs(startNs);

        assertTrue(result.getTotalDistance() > 0, "性能测试路径应可达");
        assertTrue(pathCostMs <= 1000, "路径计算耗时应<=1000ms，当前=" + pathCostMs + "ms");
    }

    private void testStartupPerformance() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-startup");
        PersistenceService persistence = new PersistenceService(tempDir);
        persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();

        long startNs = System.nanoTime();
        CampusGraph graph = persistence.loadGraphOrDefault();
        persistence.loadUsersOrDefault();
        startupCostMs = elapsedMs(startNs);

        assertTrue(graph.vertexCount() > 0, "启动加载后应有默认地图");
        assertTrue(startupCostMs <= 2000, "启动加载耗时应<=2000ms，当前=" + startupCostMs + "ms");
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

    private static long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
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

