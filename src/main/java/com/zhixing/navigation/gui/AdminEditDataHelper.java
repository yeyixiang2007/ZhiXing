package com.zhixing.navigation.gui;

import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.controller.MapController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AdminEditDataHelper {
    private static final String AUTO_VERTEX_ID_PREFIX = "SCU-JA-OT-OT-";

    private final MapController mapController;
    private int autoVertexCounter;

    AdminEditDataHelper(MapController mapController) {
        this.mapController = mapController;
        this.autoVertexCounter = 1;
    }

    String generateAutoVertexId() {
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

    Vertex requireVertex(String vertexId) {
        for (Vertex vertex : mapController.listVertices()) {
            if (vertex.getId().equals(vertexId)) {
                return vertex;
            }
        }
        throw new IllegalArgumentException("未找到点位: " + vertexId);
    }

    Edge requireRoad(String fromId, String toId) {
        for (Edge edge : mapController.listRoads()) {
            if (edge.getFromVertex().getId().equals(fromId) && edge.getToVertex().getId().equals(toId)) {
                return edge;
            }
        }
        throw new IllegalArgumentException("未找到道路: " + fromId + " -> " + toId);
    }

    List<Edge> listCanonicalRoads() {
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

    List<Edge> listRelatedCanonicalRoads(Set<String> vertexIds) {
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

    void restoreRoads(Admin admin, List<Edge> roads) {
        for (Edge edge : roads) {
            mapController.addRoad(
                    admin,
                    edge.getFromVertex().getId(),
                    edge.getToVertex().getId(),
                    edge.getWeight(),
                    edge.isOneWay(),
                    edge.isForbidden(),
                    edge.getRoadType()
            );
        }
    }

    static String toRoadKey(Edge edge) {
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
}
