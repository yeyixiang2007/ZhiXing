package com.zhixing.navigation.gui;

import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.model.RoadOption;
import com.zhixing.navigation.gui.view.RoadManageView;
import com.zhixing.navigation.gui.view.VertexManageView;
import com.zhixing.navigation.gui.workbench.state.MapViewState;
import com.zhixing.navigation.gui.workbench.WorkbenchFeedback;
import com.zhixing.navigation.gui.workbench.command.CommandBus;
import com.zhixing.navigation.gui.workbench.command.UndoableCommand;
import com.zhixing.navigation.gui.controller.MapController;
import com.zhixing.navigation.gui.routing.AppRoute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class MainViewAdminEditCoordinator {
    private static final String AUTO_VERTEX_ID_PREFIX = "SCU-JA-OT-OT-";

    private final MapController mapController;
    private final WorkbenchFeedback feedback;
    private final MapViewState viewState;
    private final CommandBus<Admin> adminCommandBus;
    private final Supplier<Admin> currentAdminSupplier;
    private final BooleanSupplier ensureAdminLoggedIn;
    private final Supplier<AppRoute> activeRouteSupplier;
    private final Runnable refreshAllData;
    private final Runnable refreshUndoRedoButtons;
    private int autoVertexCounter;

    MainViewAdminEditCoordinator(
            MapController mapController,
            WorkbenchFeedback feedback,
            MapViewState viewState,
            CommandBus<Admin> adminCommandBus,
            Supplier<Admin> currentAdminSupplier,
            BooleanSupplier ensureAdminLoggedIn,
            Supplier<AppRoute> activeRouteSupplier,
            Runnable refreshAllData,
            Runnable refreshUndoRedoButtons
    ) {
        this.mapController = mapController;
        this.feedback = feedback;
        this.viewState = viewState;
        this.adminCommandBus = adminCommandBus;
        this.currentAdminSupplier = currentAdminSupplier;
        this.ensureAdminLoggedIn = ensureAdminLoggedIn;
        this.activeRouteSupplier = activeRouteSupplier;
        this.refreshAllData = refreshAllData;
        this.refreshUndoRedoButtons = refreshUndoRedoButtons;
        this.autoVertexCounter = 1;
    }

    boolean canUndo() {
        return adminCommandBus.canUndo();
    }

    boolean canRedo() {
        return adminCommandBus.canRedo();
    }

    boolean isAdminEditingAvailable() {
        return activeRouteSupplier.get() == AppRoute.ADMIN_MODE && currentAdminSupplier.get() != null;
    }

    Vertex findVertexById(String vertexId) {
        if (isBlank(vertexId)) {
            return null;
        }
        for (Vertex vertex : mapController.listVertices()) {
            if (vertex.getId().equals(vertexId)) {
                return vertex;
            }
        }
        return null;
    }

    Edge findRoadByEdgeKey(String edgeKey) {
        if (isBlank(edgeKey)) {
            return null;
        }
        for (Edge edge : listCanonicalRoads()) {
            if (toRoadKey(edge).equals(edgeKey)) {
                return edge;
            }
        }
        return null;
    }

    void handleAddVertexFromForm(VertexManageView.VertexFormData formData) {
        final String id = safeTrim(formData.getId());
        final String name = safeTrim(formData.getName());
        final PlaceType type = formData.getType();
        final double x = parseDouble(formData.getX(), "X坐标");
        final double y = parseDouble(formData.getY(), "Y坐标");
        final String description = safeTrim(formData.getDescription());

        executeAdminEditCommand(
                "正在新增地点...",
                adminEditCommand(
                        "地点新增成功。",
                        admin -> mapController.addVertex(admin, id, name, type, x, y, description),
                        admin -> mapController.deleteVertex(admin, id)
                )
        );
    }

    void handleUpdateVertexFromForm(VertexManageView.VertexFormData formData) {
        final String targetId = safeTrim(formData.getId());
        final String originalId = isBlank(formData.getOriginalId()) ? targetId : safeTrim(formData.getOriginalId());
        final Vertex before = requireVertex(originalId);
        final String name = safeTrim(formData.getName());
        final PlaceType type = formData.getType();
        final double x = parseDouble(formData.getX(), "X坐标");
        final double y = parseDouble(formData.getY(), "Y坐标");
        final String description = safeTrim(formData.getDescription());

        if (originalId.equals(targetId)) {
            executeAdminEditCommand(
                    "正在修改地点...",
                    adminEditCommand(
                            "地点修改成功。",
                            admin -> mapController.updateVertex(admin, targetId, name, type, x, y, description),
                            admin -> mapController.updateVertex(
                                    admin,
                                    before.getId(),
                                    before.getName(),
                                    before.getType(),
                                    before.getX(),
                                    before.getY(),
                                    before.getDescription()
                            )
                    )
            );
            return;
        }

        final List<Edge> relatedRoads = listRelatedCanonicalRoads(Collections.singleton(originalId));
        executeAdminEditCommand(
                "正在修改地点...",
                adminEditCommand(
                        "地点修改成功（已更新ID）。",
                        admin -> {
                            mapController.addVertex(admin, targetId, name, type, x, y, description);
                            for (Edge edge : relatedRoads) {
                                String fromId = edge.getFromVertex().getId().equals(originalId) ? targetId : edge.getFromVertex().getId();
                                String toId = edge.getToVertex().getId().equals(originalId) ? targetId : edge.getToVertex().getId();
                                mapController.addRoad(admin, fromId, toId, edge.getWeight(), edge.isOneWay(), edge.isForbidden(), edge.getRoadType());
                            }
                            mapController.deleteVertex(admin, originalId);
                        },
                        admin -> {
                            mapController.addVertex(
                                    admin,
                                    before.getId(),
                                    before.getName(),
                                    before.getType(),
                                    before.getX(),
                                    before.getY(),
                                    before.getDescription()
                            );
                            restoreRoads(admin, relatedRoads);
                            mapController.deleteVertex(admin, targetId);
                        }
                )
        );
    }

    void handleDeleteVertexById(String id) {
        String vertexId = safeTrim(id);
        if (isBlank(vertexId)) {
            feedback.showErrorDialog("参数缺失", "请先输入或选择要删除的地点ID。");
            return;
        }
        if (!feedback.confirm("确认删除", "确定删除地点 " + vertexId + " 吗？")) {
            return;
        }
        final Vertex snapshot = requireVertex(vertexId);
        final List<Edge> relatedRoads = listRelatedCanonicalRoads(Collections.singleton(snapshot.getId()));
        executeAdminEditCommand(
                "正在删除地点...",
                adminEditCommand(
                        "地点删除成功。",
                        admin -> mapController.deleteVertex(admin, snapshot.getId()),
                        admin -> {
                            mapController.addVertex(
                                    admin,
                                    snapshot.getId(),
                                    snapshot.getName(),
                                    snapshot.getType(),
                                    snapshot.getX(),
                                    snapshot.getY(),
                                    snapshot.getDescription()
                            );
                            restoreRoads(admin, relatedRoads);
                        }
                )
        );
    }

    void handleAddRoadFromForm(RoadManageView.RoadFormData data) {
        final String fromId = safeTrim(data.getFromId());
        final String toId = safeTrim(data.getToId());
        final double weight = parsePositiveDouble(data.getWeight(), "道路距离");
        final boolean oneWay = data.isOneWay();
        final boolean forbidden = data.isForbidden();
        final RoadType roadType = data.getRoadType();

        executeAdminEditCommand(
                "正在新增道路...",
                adminEditCommand(
                        "道路新增成功。",
                        admin -> mapController.addRoad(admin, fromId, toId, weight, oneWay, forbidden, roadType),
                        admin -> mapController.deleteRoad(admin, fromId, toId)
                )
        );
    }

    void handleUpdateRoadFromForm(RoadManageView.RoadFormData data) {
        final String fromId = safeTrim(data.getFromId());
        final String toId = safeTrim(data.getToId());
        final Edge before = requireRoad(fromId, toId);
        final double weight = parsePositiveDouble(data.getWeight(), "道路距离");
        final boolean oneWay = data.isOneWay();
        final boolean forbidden = data.isForbidden();
        final RoadType roadType = data.getRoadType();

        executeAdminEditCommand(
                "正在修改道路...",
                adminEditCommand(
                        "道路修改成功。",
                        admin -> mapController.updateRoad(admin, fromId, toId, weight, oneWay, forbidden, roadType),
                        admin -> mapController.updateRoad(
                                admin,
                                before.getFromVertex().getId(),
                                before.getToVertex().getId(),
                                before.getWeight(),
                                before.isOneWay(),
                                before.isForbidden(),
                                before.getRoadType()
                        )
                )
        );
    }

    void handleDeleteRoadFromForm(RoadManageView.RoadFormData data) {
        String fromId = safeTrim(data.getFromId());
        String toId = safeTrim(data.getToId());
        if (isBlank(fromId) || isBlank(toId)) {
            feedback.showErrorDialog("参数缺失", "请先选择要删除的道路起点和终点。");
            return;
        }
        if (!feedback.confirm("确认删除", "确定删除道路 " + fromId + " -> " + toId + " 吗？")) {
            return;
        }
        final Edge snapshot = requireRoad(fromId, toId);
        executeAdminEditCommand(
                "正在删除道路...",
                adminEditCommand(
                        "道路删除成功。",
                        admin -> mapController.deleteRoad(admin, snapshot.getFromVertex().getId(), snapshot.getToVertex().getId()),
                        admin -> mapController.addRoad(
                                admin,
                                snapshot.getFromVertex().getId(),
                                snapshot.getToVertex().getId(),
                                snapshot.getWeight(),
                                snapshot.isOneWay(),
                                snapshot.isForbidden(),
                                snapshot.getRoadType()
                        )
                )
        );
    }

    void handleSetRoadForbidden(String fromId, String toId, boolean forbidden) {
        final Edge before = requireRoad(fromId, toId);
        final boolean target = forbidden;
        executeAdminEditCommand(
                target ? "正在设置禁行..." : "正在解除禁行...",
                adminEditCommand(
                        target ? "设置禁行成功。" : "解除禁行成功。",
                        admin -> mapController.setRoadForbidden(admin, before.getFromVertex().getId(), before.getToVertex().getId(), target),
                        admin -> mapController.setRoadForbidden(admin, before.getFromVertex().getId(), before.getToVertex().getId(), before.isForbidden())
                )
        );
    }

    void handleMapAddVertex(double x, double y) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        final String id = generateAutoVertexId();
        final String name = "地点-" + id;
        executeAdminEditCommand(
                "正在地图新增点位...",
                adminEditCommand(
                        "地图新增点位成功。",
                        admin -> mapController.addVertex(admin, id, name, PlaceType.OTHER, x, y, "地图编辑新增"),
                        admin -> mapController.deleteVertex(admin, id)
                )
        );
    }

    void handleMapConnectVertices(String fromId, String toId) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        if (isBlank(fromId) || isBlank(toId) || fromId.equals(toId)) {
            return;
        }
        Vertex fromVertex = requireVertex(fromId);
        Vertex toVertex = requireVertex(toId);
        final double distance = Math.max(1.0, Math.hypot(fromVertex.getX() - toVertex.getX(), fromVertex.getY() - toVertex.getY()));
        executeAdminEditCommand(
                "正在地图连线...",
                adminEditCommand(
                        "地图连线成功。",
                        admin -> mapController.addRoad(admin, fromId, toId, distance, false, false, RoadType.PATH),
                        admin -> mapController.deleteRoad(admin, fromId, toId)
                )
        );
    }

    void handleMapMoveVertex(String vertexId, double x, double y) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        final Vertex before = requireVertex(vertexId);
        executeAdminEditCommand(
                "正在移动点位...",
                adminEditCommand(
                        "点位移动成功。",
                        admin -> mapController.updateVertex(admin, before.getId(), before.getName(), before.getType(), x, y, before.getDescription()),
                        admin -> mapController.updateVertex(admin, before.getId(), before.getName(), before.getType(), before.getX(), before.getY(), before.getDescription())
                )
        );
    }

    void handleMapDeleteVertex(String vertexId) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        handleDeleteVertexById(vertexId);
    }

    void handleMapDeleteEdge(String edgeKey) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        Edge edge = findRoadByEdgeKey(edgeKey);
        if (edge == null) {
            feedback.showErrorDialog("删除失败", "未找到要删除的道路对象。");
            return;
        }
        if (!feedback.confirm("确认删除", "确定删除道路 " + edge.getFromVertex().getId() + " -> " + edge.getToVertex().getId() + " 吗？")) {
            return;
        }
        final Edge snapshot = edge;
        executeAdminEditCommand(
                "正在删除道路...",
                adminEditCommand(
                        "地图删除道路成功。",
                        admin -> mapController.deleteRoad(admin, snapshot.getFromVertex().getId(), snapshot.getToVertex().getId()),
                        admin -> mapController.addRoad(
                                admin,
                                snapshot.getFromVertex().getId(),
                                snapshot.getToVertex().getId(),
                                snapshot.getWeight(),
                                snapshot.isOneWay(),
                                snapshot.isForbidden(),
                                snapshot.getRoadType()
                        )
                )
        );
    }

    void handleBatchDeleteSelectedVertices() {
        if (!isAdminEditingAvailable()) {
            return;
        }
        List<String> mapSelectedVertexIds = viewState.getSelectedVertexIds();
        if (mapSelectedVertexIds.isEmpty()) {
            feedback.showErrorDialog("批量删除", "请先在地图中框选要删除的点位。");
            return;
        }
        if (!feedback.confirm("批量删除确认", "确定删除选中的 " + mapSelectedVertexIds.size() + " 个点位吗？")) {
            return;
        }
        final List<String> selectedIds = new ArrayList<String>(mapSelectedVertexIds);
        final Set<String> idSet = new HashSet<String>(selectedIds);
        final List<Vertex> vertexSnapshots = new ArrayList<Vertex>();
        for (String id : selectedIds) {
            Vertex vertex = findVertexById(id);
            if (vertex != null) {
                vertexSnapshots.add(vertex);
            }
        }
        final List<Edge> relatedRoads = listRelatedCanonicalRoads(idSet);
        executeAdminEditCommand(
                "正在批量删除点位...",
                adminEditCommand(
                        "批量删除成功。",
                        admin -> {
                            for (String id : selectedIds) {
                                mapController.deleteVertex(admin, id);
                            }
                        },
                        admin -> {
                            for (Vertex vertex : vertexSnapshots) {
                                mapController.addVertex(
                                        admin,
                                        vertex.getId(),
                                        vertex.getName(),
                                        vertex.getType(),
                                        vertex.getX(),
                                        vertex.getY(),
                                        vertex.getDescription()
                                );
                            }
                            restoreRoads(admin, relatedRoads);
                        }
                )
        );
    }

    void handleBatchForbiddenBySelection(boolean forbidden) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        List<String> mapSelectedVertexIds = viewState.getSelectedVertexIds();
        if (mapSelectedVertexIds.size() < 2) {
            feedback.showErrorDialog("批量禁行", "请先框选至少两个点位。");
            return;
        }
        final Set<String> selected = new HashSet<String>(mapSelectedVertexIds);
        final List<Edge> candidates = new ArrayList<Edge>();
        for (Edge edge : listCanonicalRoads()) {
            String fromId = edge.getFromVertex().getId();
            String toId = edge.getToVertex().getId();
            if (selected.contains(fromId) && selected.contains(toId) && edge.isForbidden() != forbidden) {
                candidates.add(edge);
            }
        }
        if (candidates.isEmpty()) {
            feedback.info("没有需要变更禁行状态的道路。");
            return;
        }

        final List<Edge> snapshots = new ArrayList<Edge>(candidates);
        executeAdminEditCommand(
                forbidden ? "正在批量设置禁行..." : "正在批量解除禁行...",
                adminEditCommand(
                        forbidden ? "批量禁行成功。" : "批量解禁成功。",
                        admin -> {
                            for (Edge edge : snapshots) {
                                mapController.setRoadForbidden(admin, edge.getFromVertex().getId(), edge.getToVertex().getId(), forbidden);
                            }
                        },
                        admin -> {
                            for (Edge edge : snapshots) {
                                mapController.setRoadForbidden(admin, edge.getFromVertex().getId(), edge.getToVertex().getId(), edge.isForbidden());
                            }
                        }
                )
        );
    }

    void handleQuickToggleSelectedEdgeForbidden() {
        if (!isAdminEditingAvailable()) {
            return;
        }
        String mapSelectedEdgeKey = viewState.getSelectedEdgeKey();
        if (mapSelectedEdgeKey == null) {
            feedback.showErrorDialog("禁行切换", "请先在地图上选中一条道路。");
            return;
        }
        Edge edge = findRoadByEdgeKey(mapSelectedEdgeKey);
        if (edge == null) {
            feedback.showErrorDialog("禁行切换", "未找到选中的道路。");
            return;
        }
        handleSetRoadForbidden(edge.getFromVertex().getId(), edge.getToVertex().getId(), !edge.isForbidden());
    }

    void undoLastEdit() {
        if (!adminCommandBus.canUndo()) {
            feedback.info("没有可撤销的编辑。");
            return;
        }
        if (!ensureAdminLoggedIn.getAsBoolean()) {
            return;
        }
        Admin operator = currentAdminSupplier.get();
        try {
            feedback.showLoading("正在撤销上一步编辑...");
            adminCommandBus.undo(operator);
            refreshAllData.run();
            feedback.info("撤销成功。");
            feedback.setStatus("管理员模式: 已撤销");
        } catch (RuntimeException ex) {
            feedback.showOperationError("撤销失败", ex);
            feedback.setStatus("管理员模式: 撤销失败");
        } finally {
            feedback.hideLoading();
            refreshUndoRedoButtons.run();
        }
    }

    void redoLastEdit() {
        if (!adminCommandBus.canRedo()) {
            feedback.info("没有可重做的编辑。");
            return;
        }
        if (!ensureAdminLoggedIn.getAsBoolean()) {
            return;
        }
        Admin operator = currentAdminSupplier.get();
        try {
            feedback.showLoading("正在重做上一步编辑...");
            adminCommandBus.redo(operator);
            refreshAllData.run();
            feedback.info("重做成功。");
            feedback.setStatus("管理员模式: 已重做");
        } catch (RuntimeException ex) {
            feedback.showOperationError("重做失败", ex);
            feedback.setStatus("管理员模式: 重做失败");
        } finally {
            feedback.hideLoading();
            refreshUndoRedoButtons.run();
        }
    }

    private void executeAdminEditCommand(String loadingText, AdminEditCommand command) {
        if (!ensureAdminLoggedIn.getAsBoolean()) {
            return;
        }
        Admin operator = currentAdminSupplier.get();
        try {
            feedback.showLoading(loadingText);
            adminCommandBus.execute(command, operator);
            refreshAllData.run();
            feedback.success(command.successMessage());
            feedback.setStatus("管理员模式: " + command.successMessage());
        } catch (RuntimeException ex) {
            feedback.showOperationError("操作失败", ex);
            feedback.setStatus("管理员模式: 操作失败");
        } finally {
            feedback.hideLoading();
            refreshUndoRedoButtons.run();
        }
    }

    private AdminEditCommand adminEditCommand(String successMessage, AdminAction executeAction, AdminAction undoAction) {
        return new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                executeAction.apply(admin);
            }

            @Override
            public void undo(Admin admin) {
                undoAction.apply(admin);
            }

            @Override
            public String successMessage() {
                return successMessage;
            }
        };
    }

    private String generateAutoVertexId() {
        Set<String> used = new HashSet<String>();
        for (Vertex vertex : mapController.listVertices()) {
            used.add(vertex.getId());
        }
        while (true) {
            String candidate = AUTO_VERTEX_ID_PREFIX + String.format("%03d", autoVertexCounter++);
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
    }

    private Vertex requireVertex(String vertexId) {
        Vertex vertex = findVertexById(vertexId);
        if (vertex == null) {
            throw new IllegalArgumentException("未找到点位: " + vertexId);
        }
        return vertex;
    }

    private Edge requireRoad(String fromId, String toId) {
        for (Edge edge : mapController.listRoads()) {
            if (edge.getFromVertex().getId().equals(fromId) && edge.getToVertex().getId().equals(toId)) {
                return edge;
            }
        }
        throw new IllegalArgumentException("未找到道路: " + fromId + " -> " + toId);
    }

    private List<Edge> listCanonicalRoads() {
        List<Edge> roads = mapController.listRoads();
        Map<String, Edge> deduped = new LinkedHashMap<String, Edge>();
        for (Edge edge : roads) {
            String key = toRoadKey(edge);
            if (!deduped.containsKey(key)) {
                deduped.put(key, edge);
            }
        }
        return new ArrayList<Edge>(deduped.values());
    }

    private List<Edge> listRelatedCanonicalRoads(Set<String> vertexIds) {
        List<Edge> roads = new ArrayList<Edge>();
        for (Edge edge : listCanonicalRoads()) {
            String fromId = edge.getFromVertex().getId();
            String toId = edge.getToVertex().getId();
            if (vertexIds.contains(fromId) || vertexIds.contains(toId)) {
                roads.add(edge);
            }
        }
        return roads;
    }

    private void restoreRoads(Admin admin, List<Edge> roads) {
        for (Edge edge : roads) {
            mapController.addRoad(admin, edge.getFromVertex().getId(), edge.getToVertex().getId(), edge.getWeight(), edge.isOneWay(), edge.isForbidden(), edge.getRoadType());
        }
    }

    private static String toRoadKey(Edge edge) {
        String from = edge.getFromVertex().getId();
        String to = edge.getToVertex().getId();
        if (edge.isOneWay()) {
            return "ONE:" + from + "->" + to;
        }
        if (from.compareTo(to) <= 0) {
            return "TWO:" + from + "<->" + to;
        }
        return "TWO:" + to + "<->" + from;
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static double parseDouble(String text, String fieldName) {
        try {
            return Double.parseDouble(text == null ? "" : text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " 请输入有效数字。");
        }
    }

    private static double parsePositiveDouble(String text, String fieldName) {
        double value = parseDouble(text, fieldName);
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " 必须大于0。");
        }
        return value;
    }

    private interface AdminEditCommand extends UndoableCommand<Admin> {
        String successMessage();
    }

    @FunctionalInterface
    private interface AdminAction {
        void apply(Admin admin);
    }
}
