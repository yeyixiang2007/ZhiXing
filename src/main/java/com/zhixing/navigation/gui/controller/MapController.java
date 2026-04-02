package com.zhixing.navigation.gui.controller;

import com.zhixing.navigation.application.map.MapService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.model.OverviewData;
import com.zhixing.navigation.gui.model.RoadOption;
import com.zhixing.navigation.gui.model.VertexOption;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapController {
    private final CampusGraph graph;
    private final MapService mapService;
    private final PersistenceService persistenceService;

    public MapController(CampusGraph graph, MapService mapService, PersistenceService persistenceService) {
        this.graph = Objects.requireNonNull(graph, "graph must not be null");
        this.mapService = Objects.requireNonNull(mapService, "mapService must not be null");
        this.persistenceService = Objects.requireNonNull(persistenceService, "persistenceService must not be null");
    }

    public List<Vertex> listVertices() {
        List<Vertex> vertices = new ArrayList<Vertex>(mapService.listVertices());
        sortVertices(vertices);
        return vertices;
    }

    public List<Vertex> listVerticesByType(PlaceType placeType) {
        List<Vertex> vertices = new ArrayList<Vertex>(mapService.listVerticesByType(placeType));
        sortVertices(vertices);
        return vertices;
    }

    public List<VertexOption> listVertexOptions() {
        List<VertexOption> options = new ArrayList<VertexOption>();
        for (Vertex vertex : listVertices()) {
            options.add(VertexOption.fromVertex(vertex));
        }
        return options;
    }

    public List<Edge> listRoads() {
        List<Edge> roads = new ArrayList<Edge>(mapService.listRoads());
        sortRoads(roads);
        return roads;
    }

    public List<RoadOption> listRoadOptions() {
        List<RoadOption> options = new ArrayList<RoadOption>();
        for (Edge edge : listRoads()) {
            options.add(RoadOption.fromEdge(edge));
        }
        return options;
    }

    public void addVertex(Admin admin, String id, String name, PlaceType type, double x, double y, String description) {
        executeEditCommand(admin, (service, operator) -> service.addVertex(operator, new Vertex(id, name, type, x, y, description)));
    }

    public void updateVertex(Admin admin, String id, String name, PlaceType type, double x, double y, String description) {
        executeEditCommand(admin, (service, operator) -> service.updateVertex(operator, new Vertex(id, name, type, x, y, description)));
    }

    public void deleteVertex(Admin admin, String vertexId) {
        executeEditCommand(admin, (service, operator) -> service.deleteVertex(operator, vertexId));
    }

    public void addRoad(Admin admin, String fromId, String toId, double weight, boolean oneWay, boolean forbidden, RoadType roadType) {
        executeEditCommand(admin, (service, operator) -> service.addRoad(operator, fromId, toId, weight, oneWay, forbidden, roadType));
    }

    public void updateRoad(Admin admin, String fromId, String toId, double weight, boolean oneWay, boolean forbidden, RoadType roadType) {
        executeEditCommand(admin, (service, operator) -> service.updateRoad(operator, fromId, toId, weight, oneWay, forbidden, roadType));
    }

    public void deleteRoad(Admin admin, String fromId, String toId) {
        executeEditCommand(admin, (service, operator) -> service.deleteRoad(operator, fromId, toId));
    }

    public void setRoadForbidden(Admin admin, String fromId, String toId, boolean forbidden) {
        executeEditCommand(admin, (service, operator) -> service.setRoadForbidden(operator, fromId, toId, forbidden));
    }

    public void executeEditCommand(Admin admin, MapEditCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        GraphSnapshot snapshot = GraphSnapshot.capture(graph);
        try {
            command.apply(mapService, admin);
            persistenceService.saveGraph(graph);
        } catch (RuntimeException ex) {
            restoreGraph(snapshot);
            throw ex;
        }
    }

    public void backupData(String backupName) {
        persistenceService.backupData(backupName);
    }

    public void restoreData(String backupName) {
        persistenceService.restoreData(backupName);
    }

    public OverviewData loadOverview() {
        int forbiddenCount = 0;
        for (Edge edge : mapService.listRoads()) {
            if (edge.isForbidden()) {
                forbiddenCount++;
            }
        }
        return new OverviewData(
                graph.vertexCount(),
                graph.edgeCount(),
                forbiddenCount,
                persistenceService.getDataDir().toAbsolutePath().toString()
        );
    }

    private static void sortVertices(List<Vertex> vertices) {
        Collections.sort(vertices, new Comparator<Vertex>() {
            @Override
            public int compare(Vertex left, Vertex right) {
                return left.getId().compareTo(right.getId());
            }
        });
    }

    private static void sortRoads(List<Edge> roads) {
        Collections.sort(roads, new Comparator<Edge>() {
            @Override
            public int compare(Edge left, Edge right) {
                String leftKey = left.getFromVertex().getId() + "->" + left.getToVertex().getId();
                String rightKey = right.getFromVertex().getId() + "->" + right.getToVertex().getId();
                return leftKey.compareTo(rightKey);
            }
        });
    }

    private void restoreGraph(GraphSnapshot snapshot) {
        List<String> currentVertexIds = new ArrayList<String>();
        for (Vertex vertex : graph.getAllVertices()) {
            currentVertexIds.add(vertex.getId());
        }
        for (String vertexId : currentVertexIds) {
            graph.removeVertex(vertexId);
        }
        for (VertexSnapshot vertex : snapshot.vertices) {
            graph.addVertex(vertex.toVertex());
        }
        for (EdgeSnapshot edge : snapshot.edges) {
            Vertex from = graph.getVertex(edge.fromId);
            Vertex to = graph.getVertex(edge.toId);
            if (from == null || to == null) {
                continue;
            }
            graph.addEdge(new Edge(from, to, edge.weight, edge.oneWay, edge.forbidden, edge.roadType));
        }
    }

    @FunctionalInterface
    public interface MapEditCommand {
        void apply(MapService mapService, Admin admin);
    }

    private static final class GraphSnapshot {
        private final List<VertexSnapshot> vertices;
        private final List<EdgeSnapshot> edges;

        private GraphSnapshot(List<VertexSnapshot> vertices, List<EdgeSnapshot> edges) {
            this.vertices = vertices;
            this.edges = edges;
        }

        private static GraphSnapshot capture(CampusGraph graph) {
            List<VertexSnapshot> vertices = new ArrayList<VertexSnapshot>();
            for (Vertex vertex : graph.getAllVertices()) {
                vertices.add(VertexSnapshot.of(vertex));
            }

            Map<String, EdgeSnapshot> dedupedEdges = new LinkedHashMap<String, EdgeSnapshot>();
            for (Edge edge : graph.getAllEdges()) {
                EdgeSnapshot snapshot = EdgeSnapshot.of(edge);
                if (!dedupedEdges.containsKey(snapshot.key())) {
                    dedupedEdges.put(snapshot.key(), snapshot);
                }
            }
            return new GraphSnapshot(vertices, new ArrayList<EdgeSnapshot>(dedupedEdges.values()));
        }
    }

    private static final class VertexSnapshot {
        private final String id;
        private final String name;
        private final PlaceType type;
        private final double x;
        private final double y;
        private final String description;

        private VertexSnapshot(String id, String name, PlaceType type, double x, double y, String description) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.x = x;
            this.y = y;
            this.description = description;
        }

        private static VertexSnapshot of(Vertex vertex) {
            return new VertexSnapshot(
                    vertex.getId(),
                    vertex.getName(),
                    vertex.getType(),
                    vertex.getX(),
                    vertex.getY(),
                    vertex.getDescription()
            );
        }

        private Vertex toVertex() {
            return new Vertex(id, name, type, x, y, description);
        }
    }

    private static final class EdgeSnapshot {
        private final String fromId;
        private final String toId;
        private final double weight;
        private final boolean oneWay;
        private final boolean forbidden;
        private final RoadType roadType;

        private EdgeSnapshot(String fromId, String toId, double weight, boolean oneWay, boolean forbidden, RoadType roadType) {
            this.fromId = fromId;
            this.toId = toId;
            this.weight = weight;
            this.oneWay = oneWay;
            this.forbidden = forbidden;
            this.roadType = roadType;
        }

        private static EdgeSnapshot of(Edge edge) {
            return new EdgeSnapshot(
                    edge.getFromVertex().getId(),
                    edge.getToVertex().getId(),
                    edge.getWeight(),
                    edge.isOneWay(),
                    edge.isForbidden(),
                    edge.getRoadType()
            );
        }

        private String key() {
            if (oneWay) {
                return "ONE:" + fromId + "->" + toId;
            }
            if (fromId.compareTo(toId) <= 0) {
                return "TWO:" + fromId + "<->" + toId;
            }
            return "TWO:" + toId + "<->" + fromId;
        }
    }
}
