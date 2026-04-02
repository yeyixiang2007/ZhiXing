package com.zhixing.navigation.application.map;

import com.zhixing.navigation.application.auth.AuthorizationException;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.User;
import com.zhixing.navigation.domain.model.Vertex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MapService {
    private final CampusGraph graph;

    public MapService(CampusGraph graph) {
        this.graph = Objects.requireNonNull(graph, "graph must not be null");
    }

    public void addVertex(User operator, Vertex vertex) {
        requireAdmin(operator);
        graph.addVertex(Objects.requireNonNull(vertex, "vertex must not be null"));
    }

    public void updateVertex(User operator, Vertex vertex) {
        requireAdmin(operator);
        graph.updateVertex(Objects.requireNonNull(vertex, "vertex must not be null"));
    }

    public void deleteVertex(User operator, String vertexId) {
        requireAdmin(operator);
        graph.removeVertex(requireId(vertexId, "vertexId"));
    }

    public Vertex getVertex(String vertexId) {
        String id = requireId(vertexId, "vertexId");
        Vertex vertex = graph.getVertex(id);
        if (vertex == null) {
            throw new IllegalArgumentException("vertex not found: " + id);
        }
        return vertex;
    }

    public List<Vertex> listVertices() {
        return new ArrayList<Vertex>(graph.getAllVertices());
    }

    public List<Vertex> listVerticesByType(PlaceType placeType) {
        Objects.requireNonNull(placeType, "placeType must not be null");
        List<Vertex> result = new ArrayList<Vertex>();
        for (Vertex vertex : graph.getAllVertices()) {
            if (vertex.getType() == placeType) {
                result.add(vertex);
            }
        }
        return result;
    }

    public void addRoad(User operator, String fromId, String toId, double weight, boolean oneWay, boolean forbidden, RoadType roadType) {
        requireAdmin(operator);
        String from = requireId(fromId, "fromId");
        String to = requireId(toId, "toId");
        ensureVertexExists(from);
        ensureVertexExists(to);
        if (hasRoad(from, to)) {
            throw new IllegalArgumentException("road already exists: " + from + " -> " + to);
        }
        if (!oneWay && hasRoad(to, from)) {
            throw new IllegalArgumentException("reverse road already exists, cannot create duplicated two-way road");
        }
        graph.addEdge(new Edge(getVertex(from), getVertex(to), weight, oneWay, forbidden, roadType));
    }

    public void updateRoad(User operator, String fromId, String toId, double weight, boolean oneWay, boolean forbidden, RoadType roadType) {
        requireAdmin(operator);
        String from = requireId(fromId, "fromId");
        String to = requireId(toId, "toId");
        ensureVertexExists(from);
        ensureVertexExists(to);
        if (!hasRoad(from, to)) {
            throw new IllegalArgumentException("road not found: " + from + " -> " + to);
        }

        graph.removeEdge(from, to);
        graph.addEdge(new Edge(getVertex(from), getVertex(to), weight, oneWay, forbidden, roadType));
    }

    public void deleteRoad(User operator, String fromId, String toId) {
        requireAdmin(operator);
        String from = requireId(fromId, "fromId");
        String to = requireId(toId, "toId");
        ensureVertexExists(from);
        ensureVertexExists(to);
        if (!hasRoad(from, to)) {
            throw new IllegalArgumentException("road not found: " + from + " -> " + to);
        }
        graph.removeEdge(from, to);
    }

    public Edge getRoad(String fromId, String toId) {
        String from = requireId(fromId, "fromId");
        String to = requireId(toId, "toId");
        ensureVertexExists(from);
        ensureVertexExists(to);
        for (Edge edge : graph.getNeighbors(from)) {
            if (edge.getToVertex().getId().equals(to)) {
                return edge;
            }
        }
        return null;
    }

    public List<Edge> listRoads() {
        return new ArrayList<Edge>(graph.getAllEdges());
    }

    public void setRoadForbidden(User operator, String fromId, String toId, boolean forbidden) {
        requireAdmin(operator);
        String from = requireId(fromId, "fromId");
        String to = requireId(toId, "toId");
        ensureVertexExists(from);
        ensureVertexExists(to);
        graph.setEdgeForbidden(from, to, forbidden);
    }

    public void enableRoad(User operator, String fromId, String toId) {
        setRoadForbidden(operator, fromId, toId, false);
    }

    public void disableRoad(User operator, String fromId, String toId) {
        setRoadForbidden(operator, fromId, toId, true);
    }

    private boolean hasRoad(String fromId, String toId) {
        for (Edge edge : graph.getNeighbors(fromId)) {
            if (edge.getToVertex().getId().equals(toId)) {
                return true;
            }
        }
        return false;
    }

    private void ensureVertexExists(String vertexId) {
        if (!graph.containsVertex(vertexId)) {
            throw new IllegalArgumentException("vertex not found: " + vertexId);
        }
    }

    private void requireAdmin(User operator) {
        if (operator == null || !operator.canManageMap()) {
            throw new AuthorizationException("Permission denied: admin role required.");
        }
    }

    private static String requireId(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
