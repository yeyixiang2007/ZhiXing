package com.zhixing.navigation.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zhixing.navigation.application.auth.AuthService;
import com.zhixing.navigation.application.auth.AuthenticationException;
import com.zhixing.navigation.application.map.MapService;
import com.zhixing.navigation.application.navigation.ConsolePathFormatter;
import com.zhixing.navigation.application.navigation.NavigationService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.planning.NoRouteFoundException;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;

import java.util.List;
import java.util.Scanner;

public class ConsoleApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleApplication.class);
    private final CampusGraph graph;
    private final PersistenceService persistenceService;
    private final AuthService authService;
    private final MapService mapService;
    private final NavigationService navigationService;
    private final ConsolePathFormatter pathFormatter;
    private final Scanner scanner;

    public ConsoleApplication(
            CampusGraph graph,
            PersistenceService persistenceService,
            AuthService authService,
            MapService mapService,
            NavigationService navigationService,
            ConsolePathFormatter pathFormatter,
            Scanner scanner
    ) {
        this.graph = graph;
        this.persistenceService = persistenceService;
        this.authService = authService;
        this.mapService = mapService;
        this.navigationService = navigationService;
        this.pathFormatter = pathFormatter;
        this.scanner = scanner;
    }

    public void run() {
        LOGGER.info("欢迎使用校园智能路径规划导航系统");
        try {
            while (true) {
                printMainMenu();
                int choice = readIntInRange("请选择菜单编号", 0, 2);
                if (choice == 0) {
                    LOGGER.info("系统已退出，感谢使用。");
                    return;
                }
                if (choice == 1) {
                    runUserMenu();
                } else {
                    runAdminMenu();
                }
            }
        } catch (InputClosedException ex) {
            LOGGER.info("");
            LOGGER.info("检测到输入流结束，系统自动退出。");
        }
    }

    private void printMainMenu() {
        LOGGER.info("");
        LOGGER.info("===== 主菜单 =====");
        LOGGER.info("1. 普通用户");
        LOGGER.info("2. 管理员");
        LOGGER.info("0. 退出系统");
    }

    private void runUserMenu() {
        while (true) {
            LOGGER.info("");
            LOGGER.info("===== 用户菜单 =====");
            LOGGER.info("1. 路径查询");
            LOGGER.info("2. 地点查看");
            LOGGER.info("0. 返回主菜单");
            int choice = readIntInRange("请选择菜单编号", 0, 2);
            if (choice == 0) {
                return;
            }
            if (choice == 1) {
                handlePathQuery();
            } else {
                handlePlaceView();
            }
        }
    }

    private void runAdminMenu() {
        Admin admin = loginAdminWithRetry();
        if (admin == null) {
            return;
        }
        while (true) {
            LOGGER.info("");
            LOGGER.info("===== 管理员菜单 =====");
            LOGGER.info("1. 地点管理");
            LOGGER.info("2. 道路管理");
            LOGGER.info("3. 禁行设置");
            LOGGER.info("4. 地图概览");
            LOGGER.info("0. 退出管理员菜单");
            int choice = readIntInRange("请选择菜单编号", 0, 4);
            if (choice == 0) {
                return;
            }
            if (choice == 1) {
                handleVertexManage(admin);
            } else if (choice == 2) {
                handleRoadManage(admin);
            } else if (choice == 3) {
                handleForbiddenManage(admin);
            } else {
                showGraphOverview();
            }
        }
    }

    private Admin loginAdminWithRetry() {
        while (true) {
            LOGGER.info("");
            LOGGER.info("===== 管理员登录 =====");
            String username = readNonBlank("请输入管理员账号（输入0返回）");
            if ("0".equals(username)) {
                return null;
            }
            String password = readNonBlank("请输入管理员密码");
            try {
                Admin admin = authService.loginAdmin(username, password);
                LOGGER.info("登录成功，欢迎：" + admin.getUsername());
                return admin;
            } catch (AuthenticationException ex) {
                LOGGER.info("登录失败：" + ex.getMessage() + " 请重试。");
            } catch (RuntimeException ex) {
                LOGGER.info("登录失败：" + ex.getMessage());
            }
        }
    }

    private void handlePathQuery() {
        if (graph.vertexCount() == 0) {
            LOGGER.info("当前地图无地点数据，无法查询路径。");
            return;
        }
        printVertexList(mapService.listVertices());
        String startId = readExistingVertexId("请输入起点ID");
        String endId = readExistingVertexId("请输入终点ID");
        try {
            PathResult result = navigationService.navigate(graph, startId, endId);
            LOGGER.info(pathFormatter.format(result));
        } catch (NoRouteFoundException ex) {
            LOGGER.info("路径不可达：" + ex.getMessage());
        } catch (RuntimeException ex) {
            LOGGER.info("路径查询失败：" + ex.getMessage());
        }
    }

    private void handlePlaceView() {
        while (true) {
            LOGGER.info("");
            LOGGER.info("===== 地点查看 =====");
            LOGGER.info("1. 查看全部地点");
            LOGGER.info("2. 按类型筛选");
            LOGGER.info("0. 返回");
            int choice = readIntInRange("请选择菜单编号", 0, 2);
            if (choice == 0) {
                return;
            }
            if (choice == 1) {
                printVertexList(mapService.listVertices());
            } else {
                PlaceType type = readPlaceType();
                printVertexList(mapService.listVerticesByType(type));
            }
        }
    }

    private void handleVertexManage(Admin admin) {
        while (true) {
            LOGGER.info("");
            LOGGER.info("===== 地点管理 =====");
            LOGGER.info("1. 新增地点");
            LOGGER.info("2. 修改地点");
            LOGGER.info("3. 删除地点");
            LOGGER.info("4. 查看地点列表");
            LOGGER.info("0. 返回");
            int choice = readIntInRange("请选择菜单编号", 0, 4);
            if (choice == 0) {
                return;
            }
            try {
                if (choice == 1) {
                    Vertex vertex = readVertexInput(null);
                    mapService.addVertex(admin, vertex);
                    persistGraph();
                    LOGGER.info("新增地点成功。");
                } else if (choice == 2) {
                    String id = readExistingVertexId("请输入需要修改的地点ID");
                    Vertex vertex = readVertexInput(id);
                    mapService.updateVertex(admin, vertex);
                    persistGraph();
                    LOGGER.info("修改地点成功。");
                } else if (choice == 3) {
                    String id = readExistingVertexId("请输入需要删除的地点ID");
                    mapService.deleteVertex(admin, id);
                    persistGraph();
                    LOGGER.info("删除地点成功。");
                } else {
                    printVertexList(mapService.listVertices());
                }
            } catch (RuntimeException ex) {
                LOGGER.info("操作失败：" + ex.getMessage());
            }
        }
    }

    private void handleRoadManage(Admin admin) {
        while (true) {
            LOGGER.info("");
            LOGGER.info("===== 道路管理 =====");
            LOGGER.info("1. 新增道路");
            LOGGER.info("2. 修改道路");
            LOGGER.info("3. 删除道路");
            LOGGER.info("4. 查看道路列表");
            LOGGER.info("0. 返回");
            int choice = readIntInRange("请选择菜单编号", 0, 4);
            if (choice == 0) {
                return;
            }
            try {
                if (choice == 1) {
                    RoadInput input = readRoadInput();
                    mapService.addRoad(admin, input.fromId, input.toId, input.weight, input.oneWay, input.forbidden, input.roadType);
                    persistGraph();
                    LOGGER.info("新增道路成功。");
                } else if (choice == 2) {
                    RoadInput input = readRoadInput();
                    mapService.updateRoad(admin, input.fromId, input.toId, input.weight, input.oneWay, input.forbidden, input.roadType);
                    persistGraph();
                    LOGGER.info("修改道路成功。");
                } else if (choice == 3) {
                    String fromId = readExistingVertexId("请输入道路起点ID");
                    String toId = readExistingVertexId("请输入道路终点ID");
                    mapService.deleteRoad(admin, fromId, toId);
                    persistGraph();
                    LOGGER.info("删除道路成功。");
                } else {
                    printRoadList(mapService.listRoads());
                }
            } catch (RuntimeException ex) {
                LOGGER.info("操作失败：" + ex.getMessage());
            }
        }
    }

    private void handleForbiddenManage(Admin admin) {
        while (true) {
            LOGGER.info("");
            LOGGER.info("===== 禁行设置 =====");
            LOGGER.info("1. 设置禁行");
            LOGGER.info("2. 解除禁行");
            LOGGER.info("3. 查看道路列表");
            LOGGER.info("0. 返回");
            int choice = readIntInRange("请选择菜单编号", 0, 3);
            if (choice == 0) {
                return;
            }
            try {
                if (choice == 1) {
                    String fromId = readExistingVertexId("请输入道路起点ID");
                    String toId = readExistingVertexId("请输入道路终点ID");
                    mapService.disableRoad(admin, fromId, toId);
                    persistGraph();
                    LOGGER.info("禁行设置成功。");
                } else if (choice == 2) {
                    String fromId = readExistingVertexId("请输入道路起点ID");
                    String toId = readExistingVertexId("请输入道路终点ID");
                    mapService.enableRoad(admin, fromId, toId);
                    persistGraph();
                    LOGGER.info("禁行解除成功。");
                } else {
                    printRoadList(mapService.listRoads());
                }
            } catch (RuntimeException ex) {
                LOGGER.info("操作失败：" + ex.getMessage());
            }
        }
    }

    private void showGraphOverview() {
        int forbiddenCount = 0;
        List<Edge> edges = mapService.listRoads();
        for (Edge edge : edges) {
            if (edge.isForbidden()) {
                forbiddenCount++;
            }
        }
        LOGGER.info("当前地点数量：" + graph.vertexCount());
        LOGGER.info("当前道路数量：" + graph.edgeCount());
        LOGGER.info("禁行道路数量：" + forbiddenCount);
    }

    private void printVertexList(List<Vertex> vertices) {
        LOGGER.info("");
        LOGGER.info("----- 地点列表 -----");
        if (vertices.isEmpty()) {
            LOGGER.info("暂无地点数据。");
            return;
        }
        for (Vertex vertex : vertices) {
            LOGGER.info(vertex.getId() + " | " + vertex.getName() + " | " + vertex.getType()
                    + " | (" + vertex.getX() + ", " + vertex.getY() + ")");
        }
    }

    private void printRoadList(List<Edge> roads) {
        LOGGER.info("");
        LOGGER.info("----- 道路列表 -----");
        if (roads.isEmpty()) {
            LOGGER.info("暂无道路数据。");
            return;
        }
        for (Edge edge : roads) {
            LOGGER.info(edge.getFromVertex().getId() + " -> " + edge.getToVertex().getId()
                    + " | " + edge.getWeight() + "m"
                    + " | oneWay=" + edge.isOneWay()
                    + " | forbidden=" + edge.isForbidden()
                    + " | type=" + edge.getRoadType());
        }
    }

    private Vertex readVertexInput(String fixedId) {
        String id = fixedId == null ? readNonBlank("请输入地点ID") : fixedId;
        String name = readNonBlank("请输入地点名称");
        PlaceType type = readPlaceType();
        double x = readDouble("请输入X坐标");
        double y = readDouble("请输入Y坐标");
        String description = readOptional("请输入地点描述（可留空）");
        return new Vertex(id, name, type, x, y, description);
    }

    private RoadInput readRoadInput() {
        String fromId = readExistingVertexId("请输入道路起点ID");
        String toId = readExistingVertexId("请输入道路终点ID");
        double weight = readPositiveDouble("请输入道路距离（米）");
        boolean oneWay = readYesNo("是否单行道（y/n）");
        boolean forbidden = readYesNo("是否禁行（y/n）");
        RoadType roadType = readRoadType();
        return new RoadInput(fromId, toId, weight, oneWay, forbidden, roadType);
    }

    private String readExistingVertexId(String prompt) {
        while (true) {
            String id = readNonBlank(prompt);
            if (graph.containsVertex(id)) {
                return id;
            }
            LOGGER.info("输入的地点ID不存在，请重新输入。");
        }
    }

    private PlaceType readPlaceType() {
        PlaceType[] values = PlaceType.values();
        LOGGER.info("地点类型：");
        for (int i = 0; i < values.length; i++) {
            LOGGER.info((i + 1) + ". " + values[i].name());
        }
        int index = readIntInRange("请选择地点类型", 1, values.length);
        return values[index - 1];
    }

    private RoadType readRoadType() {
        RoadType[] values = RoadType.values();
        LOGGER.info("道路类型：");
        for (int i = 0; i < values.length; i++) {
            LOGGER.info((i + 1) + ". " + values[i].name());
        }
        int index = readIntInRange("请选择道路类型", 1, values.length);
        return values[index - 1];
    }

    private int readIntInRange(String prompt, int min, int max) {
        while (true) {
            String raw = readLine(prompt + " [" + min + "-" + max + "]");
            try {
                int value = Integer.parseInt(raw.trim());
                if (value < min || value > max) {
                    LOGGER.info("输入超出范围，请重新输入。");
                    continue;
                }
                return value;
            } catch (NumberFormatException ex) {
                LOGGER.info("请输入有效数字。");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            String raw = readLine(prompt);
            try {
                return Double.parseDouble(raw.trim());
            } catch (NumberFormatException ex) {
                LOGGER.info("请输入有效数字。");
            }
        }
    }

    private double readPositiveDouble(String prompt) {
        while (true) {
            double value = readDouble(prompt);
            if (value > 0) {
                return value;
            }
            LOGGER.info("请输入大于0的数值。");
        }
    }

    private boolean readYesNo(String prompt) {
        while (true) {
            String value = readLine(prompt).trim().toLowerCase();
            if ("y".equals(value) || "yes".equals(value) || "1".equals(value)) {
                return true;
            }
            if ("n".equals(value) || "no".equals(value) || "0".equals(value)) {
                return false;
            }
            LOGGER.info("请输入 y 或 n。");
        }
    }

    private String readNonBlank(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.trim().isEmpty()) {
                return value.trim();
            }
            LOGGER.info("输入不能为空，请重新输入。");
        }
    }

    private String readOptional(String prompt) {
        return readLine(prompt).trim();
    }

    private String readLine(String prompt) {
        System.out.print(prompt + "：");
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        return scanner.nextLine();
    }

    private void persistGraph() {
        persistenceService.saveGraph(graph);
    }

    private static final class RoadInput {
        private final String fromId;
        private final String toId;
        private final double weight;
        private final boolean oneWay;
        private final boolean forbidden;
        private final RoadType roadType;

        private RoadInput(String fromId, String toId, double weight, boolean oneWay, boolean forbidden, RoadType roadType) {
            this.fromId = fromId;
            this.toId = toId;
            this.weight = weight;
            this.oneWay = oneWay;
            this.forbidden = forbidden;
            this.roadType = roadType;
        }
    }

    private static final class InputClosedException extends RuntimeException {
    }
}
