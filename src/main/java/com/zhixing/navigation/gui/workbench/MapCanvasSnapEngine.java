package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.Vertex;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.function.Function;

final class MapCanvasSnapEngine {
    private MapCanvasSnapEngine() {
    }

    static Outcome apply(
            Point2D.Double world,
            String movingVertexId,
            Collection<Vertex> vertices,
            boolean vertexLayerLocked,
            double gridSnapSize,
            int gridSnapPixels,
            int snapVertexPixels,
            double axisThresholdWorld,
            Function<Point2D.Double, Point2D.Double> worldProjector,
            Function<Vertex, Point2D.Double> vertexProjector
    ) {
        if (world == null) {
            return new Outcome(0, 0, false, false, 0, 0);
        }
        double snappedX = world.x;
        double snappedY = world.y;
        boolean verticalGuide = false;
        boolean horizontalGuide = false;
        double guideX = snappedX;
        double guideY = snappedY;

        Point2D.Double projectedPointer = worldProjector.apply(world);
        double gridX = Math.rint(world.x / gridSnapSize) * gridSnapSize;
        double gridY = Math.rint(world.y / gridSnapSize) * gridSnapSize;
        if (projectedPointer != null) {
            Point2D.Double projectedGrid = worldProjector.apply(new Point2D.Double(gridX, gridY));
            if (projectedGrid != null && projectedPointer.distance(projectedGrid) <= gridSnapPixels) {
                snappedX = gridX;
                snappedY = gridY;
                guideX = snappedX;
                guideY = snappedY;
            }
        }

        double nearestVertexScreen = Double.MAX_VALUE;
        Vertex nearestVertex = null;
        if (projectedPointer != null && !vertexLayerLocked && vertices != null && !vertices.isEmpty()) {
            for (Vertex vertex : vertices) {
                if (vertex == null) {
                    continue;
                }
                if (movingVertexId != null && movingVertexId.equals(vertex.getId())) {
                    continue;
                }
                Point2D.Double projectedVertex = vertexProjector.apply(vertex);
                if (projectedVertex == null) {
                    continue;
                }
                double distance = projectedPointer.distance(projectedVertex);
                if (distance < nearestVertexScreen) {
                    nearestVertexScreen = distance;
                    nearestVertex = vertex;
                }
            }
        }
        if (nearestVertex != null && nearestVertexScreen <= snapVertexPixels) {
            snappedX = nearestVertex.getX();
            snappedY = nearestVertex.getY();
            verticalGuide = true;
            horizontalGuide = true;
            guideX = snappedX;
            guideY = snappedY;
            return new Outcome(snappedX, snappedY, verticalGuide, horizontalGuide, guideX, guideY);
        }

        double minXGap = Double.MAX_VALUE;
        double minYGap = Double.MAX_VALUE;
        if (vertices != null) {
            for (Vertex vertex : vertices) {
                if (vertex == null) {
                    continue;
                }
                if (movingVertexId != null && movingVertexId.equals(vertex.getId())) {
                    continue;
                }
                double dx = Math.abs(snappedX - vertex.getX());
                if (dx <= axisThresholdWorld && dx < minXGap) {
                    minXGap = dx;
                    guideX = vertex.getX();
                    verticalGuide = true;
                }
                double dy = Math.abs(snappedY - vertex.getY());
                if (dy <= axisThresholdWorld && dy < minYGap) {
                    minYGap = dy;
                    guideY = vertex.getY();
                    horizontalGuide = true;
                }
            }
        }
        if (verticalGuide) {
            snappedX = guideX;
        }
        if (horizontalGuide) {
            snappedY = guideY;
        }
        return new Outcome(snappedX, snappedY, verticalGuide, horizontalGuide, guideX, guideY);
    }

    static final class Outcome {
        final double x;
        final double y;
        final boolean verticalGuide;
        final boolean horizontalGuide;
        final double guideX;
        final double guideY;

        Outcome(double x, double y, boolean verticalGuide, boolean horizontalGuide, double guideX, double guideY) {
            this.x = x;
            this.y = y;
            this.verticalGuide = verticalGuide;
            this.horizontalGuide = horizontalGuide;
            this.guideX = guideX;
            this.guideY = guideY;
        }

        boolean hasGuide() {
            return verticalGuide || horizontalGuide;
        }
    }
}
