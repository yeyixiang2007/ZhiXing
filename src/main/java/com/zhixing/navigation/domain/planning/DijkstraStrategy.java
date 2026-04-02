package com.zhixing.navigation.domain.planning;

import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

public class DijkstraStrategy implements PathPlanningStrategy {
    @Override
    public PathResult plan(CampusGraph graph, String startId, String endId) {
        Objects.requireNonNull(graph, "graph must not be null");

        String start = requireId(startId, "startId");
        String end = requireId(endId, "endId");
        ensureVertexExists(graph, start, "start");
        ensureVertexExists(graph, end, "end");

        Vertex startVertex = graph.getVertex(start);
        Vertex endVertex = graph.getVertex(end);

        if (start.equals(end)) {
            List<Vertex> single = Collections.singletonList(startVertex);
            return new PathResult(
                    startVertex,
                    endVertex,
                    single,
                    0.0,
                    0.0,
                    Collections.<Double>emptyList(),
                    Collections.emptyList()
            );
        }

        Map<String, Double> distances = new HashMap<String, Double>();
        Map<String, String> previous = new HashMap<String, String>();
        Map<String, Edge> previousEdge = new HashMap<String, Edge>();

        for (Vertex vertex : graph.getAllVertices()) {
            distances.put(vertex.getId(), Double.POSITIVE_INFINITY);
        }
        distances.put(start, 0.0);

        PriorityQueue<NodeDistance> queue = new PriorityQueue<NodeDistance>();
        queue.add(new NodeDistance(start, 0.0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            double knownDistance = distances.get(current.vertexId);
            if (current.distance > knownDistance) {
                continue;
            }
            if (current.vertexId.equals(end)) {
                break;
            }

            for (Edge edge : graph.getNeighbors(current.vertexId)) {
                if (!edge.isAvailable()) {
                    continue;
                }
                String nextId = edge.getToVertex().getId();
                double candidate = knownDistance + edge.getWeight();
                if (candidate < distances.get(nextId)) {
                    distances.put(nextId, candidate);
                    previous.put(nextId, current.vertexId);
                    previousEdge.put(nextId, edge);
                    queue.add(new NodeDistance(nextId, candidate));
                }
            }
        }

        if (!previous.containsKey(end)) {
            throw new NoRouteFoundException("No reachable path from " + start + " to " + end + ".");
        }

        return buildPathResult(graph, start, end, previous, previousEdge, distances.get(end));
    }

    private PathResult buildPathResult(
            CampusGraph graph,
            String start,
            String end,
            Map<String, String> previous,
            Map<String, Edge> previousEdge,
            double totalDistance
    ) {
        List<String> orderedIds = new ArrayList<String>();
        String cursor = end;
        orderedIds.add(cursor);
        while (!cursor.equals(start)) {
            cursor = previous.get(cursor);
            if (cursor == null) {
                throw new NoRouteFoundException("Path reconstruction failed from " + start + " to " + end + ".");
            }
            orderedIds.add(cursor);
        }
        Collections.reverse(orderedIds);

        List<Vertex> pathVertices = new ArrayList<Vertex>(orderedIds.size());
        for (String id : orderedIds) {
            pathVertices.add(graph.getVertex(id));
        }

        List<Double> segmentDistances = new ArrayList<Double>();
        for (int i = 1; i < orderedIds.size(); i++) {
            String currentId = orderedIds.get(i);
            String prevId = orderedIds.get(i - 1);
            Edge segment = previousEdge.get(currentId);
            if (segment == null || !segment.getFromVertex().getId().equals(prevId)) {
                throw new NoRouteFoundException("Path segment missing between " + prevId + " and " + currentId + ".");
            }
            segmentDistances.add(segment.getWeight());
        }

        return new PathResult(
                graph.getVertex(start),
                graph.getVertex(end),
                pathVertices,
                totalDistance,
                0.0,
                segmentDistances,
                Collections.emptyList()
        );
    }

    private static void ensureVertexExists(CampusGraph graph, String id, String label) {
        if (!graph.containsVertex(id)) {
            throw new IllegalArgumentException(label + " vertex not found: " + id);
        }
    }

    private static String requireId(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static final class NodeDistance implements Comparable<NodeDistance> {
        private final String vertexId;
        private final double distance;

        private NodeDistance(String vertexId, double distance) {
            this.vertexId = vertexId;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Double.compare(this.distance, other.distance);
        }
    }
}
