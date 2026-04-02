package com.zhixing.navigation.domain.graph;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.Vertex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CampusGraph {
    private final Map<String, Vertex> vertices = new LinkedHashMap<String, Vertex>();
    private final Map<String, List<Edge>> adjList = new LinkedHashMap<String, List<Edge>>();

    public void addVertex(Vertex vertex) {
        Objects.requireNonNull(vertex, "vertex must not be null");
        String id = vertex.getId();
        if (vertices.containsKey(id)) {
            throw new IllegalArgumentException("vertex already exists: " + id);
        }
        vertices.put(id, vertex);
        adjList.put(id, new ArrayList<Edge>());
    }

    public void updateVertex(Vertex vertex) {
        Objects.requireNonNull(vertex, "vertex must not be null");
        String id = vertex.getId();
        ensureVertexExists(id);
        vertices.put(id, vertex);
        relinkEdgesForVertex(id, vertex);
    }

    public void removeVertex(String id) {
        String vertexId = requireId(id, "vertex id");
        ensureVertexExists(vertexId);
        vertices.remove(vertexId);
        adjList.remove(vertexId);
        for (List<Edge> edges : adjList.values()) {
            edges.removeIf(edge -> edge.getToVertex().getId().equals(vertexId));
        }
    }

    public void addEdge(Edge edge) {
        Objects.requireNonNull(edge, "edge must not be null");
        String fromId = edge.getFromVertex().getId();
        String toId = edge.getToVertex().getId();
        ensureVertexExists(fromId);
        ensureVertexExists(toId);

        Edge normalized = new Edge(
                vertices.get(fromId),
                vertices.get(toId),
                edge.getWeight(),
                edge.isOneWay(),
                edge.isForbidden(),
                edge.getRoadType()
        );

        addOrReplaceEdge(fromId, normalized);
        if (!normalized.isOneWay()) {
            addOrReplaceEdge(toId, normalized.reverseForTwoWay());
        }
    }

    public void removeEdge(String fromId, String toId) {
        String from = requireId(fromId, "fromId");
        String to = requireId(toId, "toId");
        ensureVertexExists(from);
        ensureVertexExists(to);

        List<Edge> fromEdges = adjList.get(from);
        fromEdges.removeIf(edge -> edge.getToVertex().getId().equals(to));

        List<Edge> toEdges = adjList.get(to);
        toEdges.removeIf(edge -> edge.getToVertex().getId().equals(from) && !edge.isOneWay());
    }

    public void setEdgeForbidden(String fromId, String toId, boolean forbidden) {
        String from = requireId(fromId, "fromId");
        String to = requireId(toId, "toId");
        ensureVertexExists(from);
        ensureVertexExists(to);

        boolean updated = replaceForbiddenFlag(from, to, forbidden);
        if (!updated) {
            throw new IllegalArgumentException("edge not found: " + from + " -> " + to);
        }
        replaceForbiddenFlag(to, from, forbidden);
    }

    public Vertex getVertex(String id) {
        String vertexId = requireId(id, "vertex id");
        return vertices.get(vertexId);
    }

    public boolean containsVertex(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        return vertices.containsKey(id.trim());
    }

    public List<Edge> getNeighbors(String id) {
        String vertexId = requireId(id, "vertex id");
        ensureVertexExists(vertexId);
        return Collections.unmodifiableList(adjList.get(vertexId));
    }

    public Collection<Vertex> getAllVertices() {
        return Collections.unmodifiableCollection(vertices.values());
    }

    public List<Edge> getAllEdges() {
        List<Edge> all = new ArrayList<Edge>();
        for (List<Edge> edges : adjList.values()) {
            all.addAll(edges);
        }
        return Collections.unmodifiableList(all);
    }

    public int vertexCount() {
        return vertices.size();
    }

    public int edgeCount() {
        int count = 0;
        for (List<Edge> edges : adjList.values()) {
            count += edges.size();
        }
        return count;
    }

    private void addOrReplaceEdge(String fromId, Edge newEdge) {
        List<Edge> edges = adjList.get(fromId);
        edges.removeIf(edge -> edge.getToVertex().getId().equals(newEdge.getToVertex().getId()));
        edges.add(newEdge);
    }

    private boolean replaceForbiddenFlag(String fromId, String toId, boolean forbidden) {
        List<Edge> edges = adjList.get(fromId);
        for (int i = 0; i < edges.size(); i++) {
            Edge current = edges.get(i);
            if (current.getToVertex().getId().equals(toId)) {
                edges.set(i, current.withForbidden(forbidden));
                return true;
            }
        }
        return false;
    }

    private void relinkEdgesForVertex(String vertexId, Vertex latestVertex) {
        for (Map.Entry<String, List<Edge>> entry : adjList.entrySet()) {
            List<Edge> edges = entry.getValue();
            for (int i = 0; i < edges.size(); i++) {
                Edge current = edges.get(i);
                boolean fromMatched = current.getFromVertex().getId().equals(vertexId);
                boolean toMatched = current.getToVertex().getId().equals(vertexId);
                if (!fromMatched && !toMatched) {
                    continue;
                }
                Vertex from = fromMatched ? latestVertex : current.getFromVertex();
                Vertex to = toMatched ? latestVertex : current.getToVertex();
                edges.set(i, new Edge(from, to, current.getWeight(), current.isOneWay(), current.isForbidden(), current.getRoadType()));
            }
        }
    }

    private void ensureVertexExists(String id) {
        if (!vertices.containsKey(id)) {
            throw new IllegalArgumentException("vertex not found: " + id);
        }
    }

    private String requireId(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
