package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.gui.model.RouteVisualizationDto;
import com.zhixing.navigation.gui.styles.UiStyles;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

final class MapCanvasRouteRenderer {
    interface VertexProjector {
        Point2D.Double project(String vertexId);
    }

    void draw(
            Graphics2D g2,
            RouteVisualizationDto currentRouteVisualization,
            RouteVisualizationDto previousRouteVisualization,
            int focusedRouteSegmentIndex,
            boolean focusedSegmentVisible,
            VertexProjector projector
    ) {
        if (projector == null) {
            return;
        }
        if (previousRouteVisualization != null && previousRouteVisualization.getSegmentCount() > 0) {
            drawRouteSegments(g2, previousRouteVisualization, -1, false, true, projector);
        }
        if (currentRouteVisualization != null && currentRouteVisualization.getSegmentCount() > 0) {
            drawRouteSegments(g2, currentRouteVisualization, focusedRouteSegmentIndex, true, focusedSegmentVisible, projector);
            if (currentRouteVisualization.isShowMarkers()) {
                drawRouteMarkers(g2, currentRouteVisualization, projector);
            }
            if (currentRouteVisualization.isShowStepBadges()) {
                drawRouteStepBadges(g2, currentRouteVisualization, focusedRouteSegmentIndex, projector);
            }
        }
    }

    private void drawRouteSegments(
            Graphics2D g2,
            RouteVisualizationDto routeVisualization,
            int focusedIndex,
            boolean respectFocusedBlink,
            boolean focusedSegmentVisible,
            VertexProjector projector
    ) {
        for (RouteVisualizationDto.Segment segment : routeVisualization.getSegments()) {
            Point2D.Double from = projector.project(segment.getFromVertexId());
            Point2D.Double to = projector.project(segment.getToVertexId());
            if (from == null || to == null) {
                continue;
            }
            if (respectFocusedBlink && segment.getIndex() == focusedIndex && !focusedSegmentVisible) {
                continue;
            }
            Stroke stroke = segment.isDashed()
                    ? new BasicStroke(segment.getStrokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{14f, 10f}, 0f)
                    : new BasicStroke(segment.getStrokeWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g2.setColor(segment.getStrokeColor());
            g2.setStroke(stroke);
            g2.draw(new Line2D.Double(from, to));
        }
    }

    private void drawRouteMarkers(Graphics2D g2, RouteVisualizationDto routeVisualization, VertexProjector projector) {
        java.util.List<String> vertexIds = routeVisualization.getVertexIds();
        if (vertexIds.isEmpty()) {
            return;
        }
        Point2D.Double start = projector.project(vertexIds.get(0));
        Point2D.Double end = projector.project(vertexIds.get(vertexIds.size() - 1));
        if (start != null) {
            drawMarker(g2, start, "S", routeVisualization.getStartMarkerColor());
        }
        if (end != null) {
            drawMarker(g2, end, "E", routeVisualization.getEndMarkerColor());
        }
    }

    private void drawRouteStepBadges(
            Graphics2D g2,
            RouteVisualizationDto routeVisualization,
            int focusedRouteSegmentIndex,
            VertexProjector projector
    ) {
        Font previous = g2.getFont();
        g2.setFont(UiStyles.CAPTION_FONT);
        for (RouteVisualizationDto.Segment segment : routeVisualization.getSegments()) {
            Point2D.Double from = projector.project(segment.getFromVertexId());
            Point2D.Double to = projector.project(segment.getToVertexId());
            if (from == null || to == null) {
                continue;
            }
            double cx = (from.x + to.x) / 2.0;
            double cy = (from.y + to.y) / 2.0;
            int radius = 10;

            g2.setColor(segment.getIndex() == focusedRouteSegmentIndex
                    ? routeVisualization.getFocusedBadgeFillColor()
                    : routeVisualization.getBadgeFillColor());
            g2.fillOval((int) Math.round(cx) - radius, (int) Math.round(cy) - radius, radius * 2, radius * 2);
            g2.setColor(new Color(102, 114, 128));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval((int) Math.round(cx) - radius, (int) Math.round(cy) - radius, radius * 2, radius * 2);

            String text = String.valueOf(segment.getIndex() + 1);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (int) Math.round(cx) - fm.stringWidth(text) / 2;
            int ty = (int) Math.round(cy) + fm.getAscent() / 2 - 2;
            g2.setColor(UiStyles.TEXT_PRIMARY);
            g2.drawString(text, tx, ty);
        }
        g2.setFont(previous);
    }

    private void drawMarker(Graphics2D g2, Point2D.Double point, String text, Color fillColor) {
        int radius = 12;
        int cx = (int) Math.round(point.x);
        int cy = (int) Math.round(point.y);

        g2.setColor(fillColor);
        g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);

        Font previous = g2.getFont();
        g2.setFont(UiStyles.CAPTION_FONT.deriveFont(Font.BOLD));
        FontMetrics metrics = g2.getFontMetrics();
        int tx = cx - metrics.stringWidth(text) / 2;
        int ty = cy + metrics.getAscent() / 2 - 2;
        g2.drawString(text, tx, ty);
        g2.setFont(previous);
    }
}
