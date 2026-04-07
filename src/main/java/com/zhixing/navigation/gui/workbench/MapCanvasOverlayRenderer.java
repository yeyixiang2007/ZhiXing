package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.styles.UiStyles;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
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
    private static final Color SELECTION_ACCENT = new Color(42, 111, 214);
    private static final Color EDGE_SELECTION = new Color(244, 147, 53);
    private static final Color GUIDE_ACCENT = new Color(40, 140, 204, 190);
    private static final int SCALE_BAR_PANEL_HEIGHT = 30;
    private static final int SCALE_BAR_PANEL_RADIUS = 12;
    private static final int SCALE_BAR_LEFT_PADDING = 14;
    private static final int SCALE_BAR_RIGHT_PADDING = 18;
    private static final int SCALE_BAR_LABEL_GAP = 16;

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
            g2.setStroke(new BasicStroke(2.2f));
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
                g2.setColor(new Color(255, 255, 255, 228));
                g2.fillOval(left - 3, top - 3, diameter + 6, diameter + 6);
                g2.setColor(new Color(SELECTION_ACCENT.getRed(), SELECTION_ACCENT.getGreen(), SELECTION_ACCENT.getBlue(), 34));
                g2.fillOval(left - 1, top - 1, diameter + 2, diameter + 2);
                g2.setColor(SELECTION_ACCENT);
                g2.drawOval(left, top, diameter, diameter);
            }
        }

        if (selectedEdgeKey != null) {
            Edge edge = edgeByKey.apply(selectedEdgeKey);
            if (edge != null) {
                Point2D.Double from = vertexProjector.apply(edge.getFromVertex());
                Point2D.Double to = vertexProjector.apply(edge.getToVertex());
                if (from != null && to != null) {
                    g2.setColor(new Color(255, 255, 255, 215));
                    g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(new Line2D.Double(from, to));
                    g2.setColor(new Color(EDGE_SELECTION.getRed(), EDGE_SELECTION.getGreen(), EDGE_SELECTION.getBlue(), 90));
                    g2.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(new Line2D.Double(from, to));
                    g2.setColor(EDGE_SELECTION);
                    g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
                g2.setColor(new Color(255, 255, 255, 216));
                g2.fillOval(left - 3, top - 3, diameter + 6, diameter + 6);
                g2.setColor(new Color(40, 140, 204, 42));
                g2.fillOval(left - 1, top - 1, diameter + 2, diameter + 2);
                g2.setColor(new Color(40, 140, 204, 210));
                g2.setStroke(new BasicStroke(2.4f));
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
            g2.setColor(new Color(255, 255, 255, 205));
            g2.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{5f, 5f}, 0f));
            g2.draw(new Line2D.Double(from, to));
            g2.setColor(new Color(40, 140, 204, 180));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{5f, 5f}, 0f));
            g2.draw(new Line2D.Double(from, to));
        }

        int radius = (int) clamp(8 + zoom * 2, 8, 14);
        int left = (int) Math.round(to.x) - radius;
        int top = (int) Math.round(to.y) - radius;
        int diameter = radius * 2;
        g2.setColor(new Color(255, 255, 255, 232));
        g2.fillOval(left - 2, top - 2, diameter + 4, diameter + 4);
        g2.setColor(new Color(40, 140, 204));
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
        g2.setColor(new Color(SELECTION_ACCENT.getRed(), SELECTION_ACCENT.getGreen(), SELECTION_ACCENT.getBlue(), 28));
        g2.fill(selectionRect);
        g2.setColor(new Color(SELECTION_ACCENT.getRed(), SELECTION_ACCENT.getGreen(), SELECTION_ACCENT.getBlue(), 150));
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
        g2.setColor(GUIDE_ACCENT);
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
        String label = formatDistanceLabel(displayMeters);
        g2.setFont(UiStyles.CAPTION_FONT);
        FontMetrics metrics = g2.getFontMetrics();
        int labelWidth = metrics.stringWidth(label);
        int panelWidth = SCALE_BAR_LEFT_PADDING + barPixels + SCALE_BAR_LABEL_GAP + labelWidth + SCALE_BAR_RIGHT_PADDING;
        int panelX = x - SCALE_BAR_LEFT_PADDING;
        int panelY = y - (SCALE_BAR_PANEL_HEIGHT / 2) - 5;

        g2.setColor(new Color(255, 255, 255, 232));
        g2.fillRoundRect(panelX, panelY, panelWidth, SCALE_BAR_PANEL_HEIGHT, SCALE_BAR_PANEL_RADIUS, SCALE_BAR_PANEL_RADIUS);
        g2.setColor(new Color(210, 219, 230));
        g2.drawRoundRect(panelX, panelY, panelWidth, SCALE_BAR_PANEL_HEIGHT, SCALE_BAR_PANEL_RADIUS, SCALE_BAR_PANEL_RADIUS);

        g2.setColor(new Color(45, 57, 72));
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawLine(x, y, x + barPixels, y);
        g2.drawLine(x, y - 5, x, y + 5);
        g2.drawLine(x + barPixels, y - 5, x + barPixels, y + 5);

        int labelX = x + barPixels + SCALE_BAR_LABEL_GAP;
        int labelY = panelY + ((SCALE_BAR_PANEL_HEIGHT - metrics.getHeight()) / 2) + metrics.getAscent();
        g2.drawString(label, labelX, labelY);
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
