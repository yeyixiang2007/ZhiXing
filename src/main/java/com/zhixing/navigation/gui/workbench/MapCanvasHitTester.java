package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.Vertex;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

final class MapCanvasHitTester {
    private MapCanvasHitTester() {
    }

    static Vertex findVertexAt(
            Point point,
            Collection<Vertex> vertices,
            Function<Vertex, Point2D.Double> projector,
            boolean vertexLayerVisible,
            boolean vertexLayerLocked,
            double threshold
    ) {
        if (!vertexLayerVisible || vertexLayerLocked) {
            return null;
        }
        Vertex nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Vertex vertex : vertices) {
            Point2D.Double projected = projector.apply(vertex);
            if (projected == null) {
                continue;
            }
            double distance = point.distance(projected);
            if (distance <= threshold && distance < nearestDist) {
                nearest = vertex;
                nearestDist = distance;
            }
        }
        return nearest;
    }

    static Edge findEdgeAt(
            Point point,
            List<Edge> renderableEdges,
            Function<Vertex, Point2D.Double> projector,
            Function<Edge, Boolean> edgeVisible,
            Function<Edge, Boolean> edgeLocked,
            double threshold
    ) {
        Edge nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Edge edge : renderableEdges) {
            if (!edgeVisible.apply(edge) || edgeLocked.apply(edge)) {
                continue;
            }
            Point2D.Double from = projector.apply(edge.getFromVertex());
            Point2D.Double to = projector.apply(edge.getToVertex());
            if (from == null || to == null) {
                continue;
            }
            double distance = Line2D.ptSegDist(from.x, from.y, to.x, to.y, point.x, point.y);
            if (distance <= threshold && distance < nearestDist) {
                nearest = edge;
                nearestDist = distance;
            }
        }
        return nearest;
    }
}
