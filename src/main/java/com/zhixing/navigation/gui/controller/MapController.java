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
import java.util.List;
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
        mapService.addVertex(admin, new Vertex(id, name, type, x, y, description));
        persistenceService.saveGraph(graph);
    }

    public void updateVertex(Admin admin, String id, String name, PlaceType type, double x, double y, String description) {
        mapService.updateVertex(admin, new Vertex(id, name, type, x, y, description));
        persistenceService.saveGraph(graph);
    }

    public void deleteVertex(Admin admin, String vertexId) {
        mapService.deleteVertex(admin, vertexId);
        persistenceService.saveGraph(graph);
    }

    public void addRoad(Admin admin, String fromId, String toId, double weight, boolean oneWay, boolean forbidden, RoadType roadType) {
        mapService.addRoad(admin, fromId, toId, weight, oneWay, forbidden, roadType);
        persistenceService.saveGraph(graph);
    }

    public void updateRoad(Admin admin, String fromId, String toId, double weight, boolean oneWay, boolean forbidden, RoadType roadType) {
        mapService.updateRoad(admin, fromId, toId, weight, oneWay, forbidden, roadType);
        persistenceService.saveGraph(graph);
    }

    public void deleteRoad(Admin admin, String fromId, String toId) {
        mapService.deleteRoad(admin, fromId, toId);
        persistenceService.saveGraph(graph);
    }

    public void setRoadForbidden(Admin admin, String fromId, String toId, boolean forbidden) {
        mapService.setRoadForbidden(admin, fromId, toId, forbidden);
        persistenceService.saveGraph(graph);
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
}
