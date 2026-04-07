package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.styles.UiStyles;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

final class MapCanvasBaseSceneRenderer {
    private static final Stroke GRID_STROKE = new BasicStroke(1f);
    private static final Color GRID_MINOR = new Color(228, 234, 242);
    private static final Color GRID_MAJOR = new Color(214, 223, 234);
    private static final Color FORBIDDEN_ROAD = new Color(194, 94, 82);

    private final Map<String, LabelMetrics> labelMetricsCache;
    private Font labelFont;
    private boolean wasAntialias;

    MapCanvasBaseSceneRenderer() {
        this.labelMetricsCache = new HashMap<String, LabelMetrics>();
        this.labelFont = null;
        this.wasAntialias = false;
    }

    void drawScene(
            Graphics2D g2,
            int canvasWidth,
            int canvasHeight,
            int viewPadding,
            double zoom,
            double baseScale,
            double worldMinX,
            double worldMaxX,
            double worldMinY,
            double worldMaxY,
            BufferedImage referenceImage,
            double referenceImageScale,
            List<MapCanvas.Layer> renderOrder,
            Collection<Edge> edges,
            Collection<Vertex> vertices,
            Predicate<MapCanvas.Layer> layerVisible,
            Function<MapCanvas.Layer, Float> layerOpacity,
            Function<Vertex, Point2D.Double> vertexProjector,
            Function<Point2D.Double, Point2D.Double> worldProjector,
            Function<RoadType, Color> roadColorProvider,
            Function<RoadType, Float> roadWidthProvider,
            Function<PlaceType, Color> placeColorProvider,
            BiFunction<Color, Float, Color> opacityApplier
    ) {
        drawReferenceImageLayer(
                g2,
                canvasWidth,
                canvasHeight,
                zoom,
                baseScale,
                referenceImage,
                referenceImageScale,
                worldProjector
        );
        drawGrid(g2, canvasWidth, canvasHeight, viewPadding);

        for (MapCanvas.Layer layer : renderOrder) {
            if (!layerVisible.test(layer)) {
                continue;
            }
            if (layer == MapCanvas.Layer.ROAD) {
                drawRoadLayer(g2, false, zoom, edges, vertexProjector, layerOpacity, roadColorProvider, roadWidthProvider, opacityApplier);
            } else if (layer == MapCanvas.Layer.FORBIDDEN) {
                drawRoadLayer(g2, true, zoom, edges, vertexProjector, layerOpacity, roadColorProvider, roadWidthProvider, opacityApplier);
            } else if (layer == MapCanvas.Layer.VERTEX) {
                drawVertexLayer(g2, zoom, vertices, vertexProjector, layerOpacity, placeColorProvider, opacityApplier);
            } else if (layer == MapCanvas.Layer.LABEL) {
                drawLabelLayer(g2, zoom, vertices, vertexProjector, layerOpacity, opacityApplier);
            }
        }
    }

    private void drawReferenceImageLayer(
            Graphics2D g2,
            int canvasWidth,
            int canvasHeight,
            double zoom,
            double baseScale,
            BufferedImage referenceImage,
            double referenceImageScale,
            Function<Point2D.Double, Point2D.Double> worldProjector
    ) {
        if (referenceImage == null) {
            return;
        }
        double pixelsPerWorldUnit = Math.max(baseScale * zoom, 0.001);
        double tileWidth = Math.max(16.0, referenceImage.getWidth() * referenceImageScale * pixelsPerWorldUnit);
        double tileHeight = Math.max(16.0, referenceImage.getHeight() * referenceImageScale * pixelsPerWorldUnit);
        int tileWidthInt = Math.max(1, (int) Math.round(tileWidth));
        int tileHeightInt = Math.max(1, (int) Math.round(tileHeight));
        if (tileWidthInt <= 0 || tileHeightInt <= 0) {
            return;
        }

        Point2D.Double worldOrigin = worldProjector.apply(new Point2D.Double(0.0, 0.0));
        int anchorX = worldOrigin == null ? 0 : (int) Math.round(worldOrigin.x);
        int anchorY = worldOrigin == null ? 0 : (int) Math.round(worldOrigin.y);
        int startX = floorMod(anchorX, tileWidthInt) - tileWidthInt;
        int startY = floorMod(anchorY, tileHeightInt) - tileHeightInt;

        for (int x = startX; x < canvasWidth; x += tileWidthInt) {
            for (int y = startY; y < canvasHeight; y += tileHeightInt) {
                g2.drawImage(referenceImage, x, y, tileWidthInt, tileHeightInt, null);
            }
        }
    }

    private void drawGrid(Graphics2D g2, int canvasWidth, int canvasHeight, int viewPadding) {
        g2.setStroke(GRID_STROKE);
        int step = 80;
        for (int x = viewPadding; x < canvasWidth - viewPadding; x += step) {
            int index = (x - viewPadding) / step;
            g2.setColor(index % 2 == 0 ? GRID_MAJOR : GRID_MINOR);
            g2.drawLine(x, viewPadding, x, canvasHeight - viewPadding);
        }
        for (int y = viewPadding; y < canvasHeight - viewPadding; y += step) {
            int index = (y - viewPadding) / step;
            g2.setColor(index % 2 == 0 ? GRID_MAJOR : GRID_MINOR);
            g2.drawLine(viewPadding, y, canvasWidth - viewPadding, y);
        }
    }

    private void drawRoadLayer(
            Graphics2D g2,
            boolean forbiddenLayer,
            double zoom,
            Collection<Edge> edges,
            Function<Vertex, Point2D.Double> vertexProjector,
            Function<MapCanvas.Layer, Float> layerOpacity,
            Function<RoadType, Color> roadColorProvider,
            Function<RoadType, Float> roadWidthProvider,
            BiFunction<Color, Float, Color> opacityApplier
    ) {
        MapCanvas.Layer targetLayer = forbiddenLayer ? MapCanvas.Layer.FORBIDDEN : MapCanvas.Layer.ROAD;
        float opacity = layerOpacity.apply(targetLayer).floatValue();
        List<Edge> renderableEdges = MapCanvasEdgeIndex.dedupeForRender(edges);
        for (Edge edge : renderableEdges) {
            if (forbiddenLayer != edge.isForbidden()) {
                continue;
            }
            Point2D.Double from = vertexProjector.apply(edge.getFromVertex());
            Point2D.Double to = vertexProjector.apply(edge.getToVertex());
            if (from == null || to == null) {
                continue;
            }

            Color color = forbiddenLayer ? FORBIDDEN_ROAD : roadColorProvider.apply(edge.getRoadType());
            color = opacityApplier.apply(color, Float.valueOf(opacity));
            float width = forbiddenLayer ? 4.0f : roadWidthProvider.apply(edge.getRoadType()).floatValue();
            float zoomAdjustedWidth = width * (float) clamp(0.92 + zoom * 0.16, 0.95, 1.35);
            Stroke underlayStroke = new BasicStroke(
                    zoomAdjustedWidth + (forbiddenLayer ? 4.2f : 3.0f),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            );
            Stroke stroke = forbiddenLayer
                    ? new BasicStroke(
                    zoomAdjustedWidth,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND,
                    10f,
                    new float[]{10f, 8f},
                    0f
            )
                    : new BasicStroke(zoomAdjustedWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

            g2.setColor(opacityApplier.apply(new Color(255, 255, 255, forbiddenLayer ? 195 : 215), Float.valueOf(opacity)));
            g2.setStroke(underlayStroke);
            g2.draw(new Line2D.Double(from, to));

            if (!forbiddenLayer) {
                g2.setColor(opacityApplier.apply(lighten(roadColorProvider.apply(edge.getRoadType()), 0.55f), Float.valueOf(opacity * 0.75f)));
                g2.setStroke(new BasicStroke(Math.max(1.2f, zoomAdjustedWidth - 1.1f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Line2D.Double(from, to));
            }

            g2.setColor(color);
            g2.setStroke(stroke);
            g2.draw(new Line2D.Double(from, to));

            if (!forbiddenLayer && edge.isOneWay()) {
                drawDirectionArrow(g2, from, to, darken(roadColorProvider.apply(edge.getRoadType()), 0.22f), zoom);
            }
        }
    }

    private void drawDirectionArrow(Graphics2D g2, Point2D.Double from, Point2D.Double to, Color color, double zoom) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double length = Math.hypot(dx, dy);
        if (length < 8) {
            return;
        }

        double ux = dx / length;
        double uy = dy / length;
        double arrowX = from.x + ux * (length * 0.55);
        double arrowY = from.y + uy * (length * 0.55);
        double side = Math.max(6.0, 6.0 * zoom);

        Path2D path = new Path2D.Double();
        path.moveTo(arrowX, arrowY);
        path.lineTo(arrowX - ux * side - uy * side * 0.5, arrowY - uy * side + ux * side * 0.5);
        path.lineTo(arrowX - ux * side + uy * side * 0.5, arrowY - uy * side - ux * side * 0.5);
        path.closePath();

        g2.setColor(color);
        g2.fill(path);
    }

    private void drawVertexLayer(
            Graphics2D g2,
            double zoom,
            Collection<Vertex> vertices,
            Function<Vertex, Point2D.Double> vertexProjector,
            Function<MapCanvas.Layer, Float> layerOpacity,
            Function<PlaceType, Color> placeColorProvider,
            BiFunction<Color, Float, Color> opacityApplier
    ) {
        float opacity = layerOpacity.apply(MapCanvas.Layer.VERTEX).floatValue();
        for (Vertex vertex : vertices) {
            Point2D.Double point = vertexProjector.apply(vertex);
            if (point == null) {
                continue;
            }
            int radius = (int) clamp(6 + zoom * 2.2, 6, 13);
            int diameter = radius * 2;
            int left = (int) Math.round(point.x) - radius;
            int top = (int) Math.round(point.y) - radius;
            Color fill = opacityApplier.apply(placeColorProvider.apply(vertex.getType()), Float.valueOf(opacity));
            Color border = opacityApplier.apply(darken(placeColorProvider.apply(vertex.getType()), 0.20f), Float.valueOf(opacity));

            g2.setColor(opacityApplier.apply(new Color(255, 255, 255, 216), Float.valueOf(opacity)));
            g2.fillOval(left - 2, top - 2, diameter + 4, diameter + 4);
            g2.setColor(fill);
            g2.fillOval(left, top, diameter, diameter);
            g2.setColor(border);
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawOval(left, top, diameter, diameter);
            int gleamDiameter = Math.max(3, radius / 2);
            g2.setColor(opacityApplier.apply(new Color(255, 255, 255, 145), Float.valueOf(opacity)));
            g2.fillOval(left + radius / 2, top + radius / 3, gleamDiameter, gleamDiameter);
        }
    }

    private void drawLabelLayer(
            Graphics2D g2,
            double zoom,
            Collection<Vertex> vertices,
            Function<Vertex, Point2D.Double> vertexProjector,
            Function<MapCanvas.Layer, Float> layerOpacity,
            BiFunction<Color, Float, Color> opacityApplier
    ) {
        float opacity = layerOpacity.apply(MapCanvas.Layer.LABEL).floatValue();
        Font base = UiStyles.BODY_FONT;
        Font scaled = base.deriveFont((float) clamp(base.getSize2D() * (0.92 + zoom * 0.28), 11f, 15f));
        g2.setFont(scaled);

        boolean antialias = RenderingHints.VALUE_TEXT_ANTIALIAS_ON.equals(g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
        if (!scaled.equals(labelFont) || antialias != wasAntialias) {
            labelMetricsCache.clear();
            labelFont = scaled;
            wasAntialias = antialias;
        }

        FontMetrics metrics = g2.getFontMetrics(scaled);
        List<LabelCandidate> candidates = new ArrayList<LabelCandidate>();
        for (Vertex vertex : vertices) {
            Point2D.Double point = vertexProjector.apply(vertex);
            if (point == null) {
                continue;
            }
            int priority = labelPriority(vertex.getType());
            if (priority < minimumLabelPriority(zoom)) {
                continue;
            }
            candidates.add(new LabelCandidate(vertex, point, priority));
        }

        Collections.sort(candidates, new Comparator<LabelCandidate>() {
            @Override
            public int compare(LabelCandidate left, LabelCandidate right) {
                if (left.priority != right.priority) {
                    return right.priority - left.priority;
                }
                return left.vertex.getName().compareTo(right.vertex.getName());
            }
        });

        List<Rectangle> occupied = new ArrayList<Rectangle>();
        int xOffset = (int) clamp(11 + zoom * 2, 10, 16);
        int yOffset = (int) clamp(10 + zoom, 9, 14);
        int exclusionPadding = labelExclusionPadding(zoom);

        for (LabelCandidate candidate : candidates) {
            String text = candidate.vertex.getName();
            LabelMetrics labelMetrics = getLabelMetrics(metrics, text);
            int x = (int) Math.round(candidate.point.x + xOffset);
            int y = (int) Math.round(candidate.point.y - yOffset);
            Rectangle bubbleBounds = new Rectangle(
                    x - 5,
                    y - labelMetrics.ascent - 4,
                    labelMetrics.width + 10,
                    labelMetrics.height + 6
            );
            Rectangle exclusionBounds = new Rectangle(bubbleBounds);
            exclusionBounds.grow(exclusionPadding, Math.max(6, exclusionPadding / 2));
            if (intersectsAny(exclusionBounds, occupied)) {
                continue;
            }

            occupied.add(exclusionBounds);
            g2.setColor(opacityApplier.apply(new Color(255, 255, 255, 226), Float.valueOf(opacity)));
            g2.fillRoundRect(bubbleBounds.x, bubbleBounds.y, bubbleBounds.width, bubbleBounds.height, 10, 10);
            g2.setColor(opacityApplier.apply(new Color(214, 223, 234, 220), Float.valueOf(opacity)));
            g2.drawRoundRect(bubbleBounds.x, bubbleBounds.y, bubbleBounds.width, bubbleBounds.height, 10, 10);
            g2.setColor(opacityApplier.apply(UiStyles.TEXT_PRIMARY, Float.valueOf(opacity)));
            g2.drawString(text, x, y);
        }
    }

    private LabelMetrics getLabelMetrics(FontMetrics metrics, String text) {
        String key = text == null ? "" : text;
        LabelMetrics cached = labelMetricsCache.get(key);
        if (cached != null) {
            return cached;
        }
        LabelMetrics created = new LabelMetrics(metrics.stringWidth(key), metrics.getHeight(), metrics.getAscent());
        labelMetricsCache.put(key, created);
        return created;
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

    private static int floorMod(int value, int divisor) {
        int mod = value % divisor;
        return mod < 0 ? mod + divisor : mod;
    }

    private static int labelPriority(PlaceType placeType) {
        if (placeType == PlaceType.GATE || placeType == PlaceType.LIBRARY) {
            return 4;
        }
        if (placeType == PlaceType.CANTEEN || placeType == PlaceType.TEACHING_BUILDING) {
            return 3;
        }
        if (placeType == PlaceType.DORMITORY || placeType == PlaceType.SPORTS_CENTER) {
            return 2;
        }
        return 1;
    }

    private static int minimumLabelPriority(double zoom) {
        if (zoom < 0.85) {
            return 4;
        }
        if (zoom < 1.15) {
            return 3;
        }
        if (zoom < 1.55) {
            return 2;
        }
        return 1;
    }

    private static int labelExclusionPadding(double zoom) {
        if (zoom < 0.85) {
            return 28;
        }
        if (zoom < 1.15) {
            return 18;
        }
        if (zoom < 1.55) {
            return 10;
        }
        return 6;
    }

    private static boolean intersectsAny(Rectangle target, List<Rectangle> occupied) {
        for (Rectangle rect : occupied) {
            if (rect.intersects(target)) {
                return true;
            }
        }
        return false;
    }

    private static Color lighten(Color color, float amount) {
        int red = color.getRed() + Math.round((255 - color.getRed()) * amount);
        int green = color.getGreen() + Math.round((255 - color.getGreen()) * amount);
        int blue = color.getBlue() + Math.round((255 - color.getBlue()) * amount);
        return new Color(clamp(red, 0, 255), clamp(green, 0, 255), clamp(blue, 0, 255), color.getAlpha());
    }

    private static Color darken(Color color, float amount) {
        int red = color.getRed() - Math.round(color.getRed() * amount);
        int green = color.getGreen() - Math.round(color.getGreen() * amount);
        int blue = color.getBlue() - Math.round(color.getBlue() * amount);
        return new Color(clamp(red, 0, 255), clamp(green, 0, 255), clamp(blue, 0, 255), color.getAlpha());
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static final class LabelMetrics {
        private final int width;
        private final int height;
        private final int ascent;

        private LabelMetrics(int width, int height, int ascent) {
            this.width = width;
            this.height = height;
            this.ascent = ascent;
        }
    }

    private static final class LabelCandidate {
        private final Vertex vertex;
        private final Point2D.Double point;
        private final int priority;

        private LabelCandidate(Vertex vertex, Point2D.Double point, int priority) {
            this.vertex = vertex;
            this.point = point;
            this.priority = priority;
        }
    }
}
