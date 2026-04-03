package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.styles.UiStyles;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

final class MapCanvasOverlayRenderer {
    private static final Stroke ALIGNMENT_GUIDE_STROKE = new BasicStroke(
            1.4f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            10f,
            new float[]{8f, 6f},
            0f
    );

    void drawOverlay(
            Graphics2D g2,
            int canvasHeight,
            int viewPadding,
            double zoom,
            double baseScale,
            double metersPerWorldUnit,
            double worldMinX,
            double worldMaxX,
            double worldMinY,
            double worldMaxY,
            Set<String> selectedVertexIds,
            String selectedEdgeKey,
            String pendingEdgeStartVertexId,
            boolean movingVertexDragActive,
            String draggingVertexId,
            Point2D.Double draggingPreviewWorld,
            Rectangle selectionRect,
            boolean snapGuideVisible,
            boolean snapVertical,
            boolean snapHorizontal,
            double snapWorldX,
            double snapWorldY,
            Predicate<MapCanvas.Layer> layerVisible,
            Function<String, Vertex> vertexById,
            Function<String, Edge> edgeByKey,
            Function<Vertex, Point2D.Double> vertexProjector,
            Function<Point2D.Double, Point2D.Double> worldProjector,
            Function<PlaceType, Color> placeColorProvider
    ) {
        drawSelectionOverlay(
                g2,
                zoom,
                selectedVertexIds,
                selectedEdgeKey,
                pendingEdgeStartVertexId,
                layerVisible,
                vertexById,
                edgeByKey,
                vertexProjector
        );
        drawDraggingVertexPreview(
                g2,
                zoom,
                movingVertexDragActive,
                draggingVertexId,
                draggingPreviewWorld,
                layerVisible,
                vertexById,
                vertexProjector,
                worldProjector,
                placeColorProvider
        );
        drawSnapGuide(g2, snapGuideVisible, snapVertical, snapHorizontal, snapWorldX, snapWorldY, worldMinX, worldMaxX, worldMinY, worldMaxY, worldProjector);
        drawSelectionRect(g2, selectionRect);
        drawScaleBar(g2, canvasHeight, viewPadding, baseScale, zoom, metersPerWorldUnit);
    }

    private void drawSelectionOverlay(
            Graphics2D g2,
            double zoom,
            Set<String> selectedVertexIds,
            String selectedEdgeKey,
            String pendingEdgeStartVertexId,
            Predicate<MapCanvas.Layer> layerVisible,
            Function<String, Vertex> vertexById,
            Function<String, Edge> edgeByKey,
            Function<Vertex, Point2D.Double> vertexProjector
    ) {
        if (!selectedVertexIds.isEmpty() && layerVisible.test(MapCanvas.Layer.VERTEX)) {
            g2.setStroke(new BasicStroke(2.0f));
            for (String vertexId : selectedVertexIds) {
                Vertex vertex = vertexById.apply(vertexId);
                Point2D.Double point = vertexProjector.apply(vertex);
                if (point == null) {
                    continue;
                }
                int radius = (int) clamp(10 + zoom * 2, 10, 16);
                int diameter = radius * 2;
                int left = (int) Math.round(point.x) - radius;
                int top = (int) Math.round(point.y) - radius;
                g2.setColor(new Color(255, 196, 0));
                g2.drawOval(left, top, diameter, diameter);
            }
        }

        if (selectedEdgeKey != null) {
            Edge edge = edgeByKey.apply(selectedEdgeKey);
            if (edge != null) {
                Point2D.Double from = vertexProjector.apply(edge.getFromVertex());
                Point2D.Double to = vertexProjector.apply(edge.getToVertex());
                if (from != null && to != null) {
                    g2.setColor(new Color(255, 153, 0));
                    g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(new Line2D.Double(from, to));
                }
            }
        }

        if (pendingEdgeStartVertexId != null) {
            Vertex seed = vertexById.apply(pendingEdgeStartVertexId);
            Point2D.Double seedPoint = vertexProjector.apply(seed);
            if (seedPoint != null) {
                int radius = 16;
                int diameter = radius * 2;
                int left = (int) Math.round(seedPoint.x) - radius;
                int top = (int) Math.round(seedPoint.y) - radius;
                g2.setColor(new Color(0, 191, 255, 180));
                g2.setStroke(new BasicStroke(2.3f));
                g2.drawOval(left, top, diameter, diameter);
            }
        }
    }

    private void drawDraggingVertexPreview(
            Graphics2D g2,
            double zoom,
            boolean movingVertexDragActive,
            String draggingVertexId,
            Point2D.Double draggingPreviewWorld,
            Predicate<MapCanvas.Layer> layerVisible,
            Function<String, Vertex> vertexById,
            Function<Vertex, Point2D.Double> vertexProjector,
            Function<Point2D.Double, Point2D.Double> worldProjector,
            Function<PlaceType, Color> placeColorProvider
    ) {
        if (!movingVertexDragActive || draggingVertexId == null || draggingPreviewWorld == null || !layerVisible.test(MapCanvas.Layer.VERTEX)) {
            return;
        }
        Vertex vertex = vertexById.apply(draggingVertexId);
        if (vertex == null) {
            return;
        }
        Point2D.Double from = vertexProjector.apply(vertex);
        Point2D.Double to = worldProjector.apply(draggingPreviewWorld);
        if (to == null) {
            return;
        }
        if (from != null) {
            g2.setColor(new Color(0, 156, 255, 180));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{5f, 5f}, 0f));
            g2.draw(new Line2D.Double(from, to));
        }

        int radius = (int) clamp(8 + zoom * 2, 8, 14);
        int left = (int) Math.round(to.x) - radius;
        int top = (int) Math.round(to.y) - radius;
        int diameter = radius * 2;
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillOval(left - 2, top - 2, diameter + 4, diameter + 4);
        g2.setColor(new Color(30, 106, 255));
        g2.setStroke(new BasicStroke(2.1f));
        g2.drawOval(left - 2, top - 2, diameter + 4, diameter + 4);
        g2.setColor(placeColorProvider.apply(vertex.getType()));
        g2.fillOval(left, top, diameter, diameter);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawOval(left, top, diameter, diameter);
    }

    private void drawSelectionRect(Graphics2D g2, Rectangle selectionRect) {
        if (selectionRect == null) {
            return;
        }
        g2.setColor(new Color(30, 106, 255, 35));
        g2.fill(selectionRect);
        g2.setColor(new Color(30, 106, 255, 160));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(selectionRect);
    }

    private void drawSnapGuide(
            Graphics2D g2,
            boolean snapGuideVisible,
            boolean snapVertical,
            boolean snapHorizontal,
            double snapWorldX,
            double snapWorldY,
            double worldMinX,
            double worldMaxX,
            double worldMinY,
            double worldMaxY,
            Function<Point2D.Double, Point2D.Double> worldProjector
    ) {
        if (!snapGuideVisible) {
            return;
        }
        g2.setColor(new Color(0, 156, 255, 190));
        Stroke previous = g2.getStroke();
        g2.setStroke(ALIGNMENT_GUIDE_STROKE);
        if (snapVertical) {
            Point2D.Double top = worldProjector.apply(new Point2D.Double(snapWorldX, worldMinY));
            Point2D.Double bottom = worldProjector.apply(new Point2D.Double(snapWorldX, worldMaxY));
            if (top != null && bottom != null) {
                g2.draw(new Line2D.Double(top, bottom));
            }
        }
        if (snapHorizontal) {
            Point2D.Double left = worldProjector.apply(new Point2D.Double(worldMinX, snapWorldY));
            Point2D.Double right = worldProjector.apply(new Point2D.Double(worldMaxX, snapWorldY));
            if (left != null && right != null) {
                g2.draw(new Line2D.Double(left, right));
            }
        }
        g2.setStroke(previous);
    }

    private void drawScaleBar(
            Graphics2D g2,
            int canvasHeight,
            int viewPadding,
            double baseScale,
            double zoom,
            double metersPerWorldUnit
    ) {
        if (metersPerWorldUnit <= 0) {
            return;
        }
        double pixelsPerWorldUnit = Math.max(baseScale * zoom, 0.000001);
        double pixelsPerMeter = pixelsPerWorldUnit / metersPerWorldUnit;
        if (pixelsPerMeter <= 0 || Double.isInfinite(pixelsPerMeter) || Double.isNaN(pixelsPerMeter)) {
            return;
        }

        double targetPixels = 120.0;
        double targetMeters = targetPixels / pixelsPerMeter;
        double displayMeters = chooseNiceDistance(targetMeters);
        int barPixels = (int) Math.round(displayMeters * pixelsPerMeter);
        barPixels = Math.max(40, Math.min(barPixels, 240));

        int x = viewPadding + 14;
        int y = Math.max(canvasHeight - 24, viewPadding + 28);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillRoundRect(x - 8, y - 18, barPixels + 52, 28, 10, 10);

        g2.setColor(new Color(45, 57, 72));
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawLine(x, y, x + barPixels, y);
        g2.drawLine(x, y - 5, x, y + 5);
        g2.drawLine(x + barPixels, y - 5, x + barPixels, y + 5);

        String label = formatDistanceLabel(displayMeters);
        g2.setFont(UiStyles.CAPTION_FONT);
        g2.drawString(label, x + barPixels + 10, y + 5);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static double chooseNiceDistance(double rawDistance) {
        if (rawDistance <= 0 || Double.isInfinite(rawDistance) || Double.isNaN(rawDistance)) {
            return 10.0;
        }
        double exponent = Math.floor(Math.log10(rawDistance));
        double base = Math.pow(10.0, exponent);
        double normalized = rawDistance / base;
        double nice;
        if (normalized <= 1.0) {
            nice = 1.0;
        } else if (normalized <= 2.0) {
            nice = 2.0;
        } else if (normalized <= 5.0) {
            nice = 5.0;
        } else {
            nice = 10.0;
        }
        return nice * base;
    }

    private static String formatDistanceLabel(double meters) {
        if (meters >= 1000) {
            return String.format("%.1f km", meters / 1000.0);
        }
        return String.format("%.0f m", meters);
    }
}
