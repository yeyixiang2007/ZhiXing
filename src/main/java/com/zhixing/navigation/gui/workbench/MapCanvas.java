package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapCanvas extends JPanel {
    public enum Layer {
        ROAD,
        FORBIDDEN,
        VERTEX,
        LABEL
    }

    private static final double MIN_ZOOM = 0.35;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 1.12;
    private static final int VIEW_PADDING = 40;
    private static final int WORLD_PADDING = 20;
    private static final Stroke GRID_STROKE = new BasicStroke(1f);

    private final List<Vertex> vertices;
    private final List<Edge> edges;
    private final Map<String, Vertex> vertexIndex;
    private final Map<String, LabelMetrics> labelMetricsCache;
    private final Map<String, Point2D.Double> projectedVertexCache;
    private final Set<String> selectedVertexIds;
    private final Map<Layer, Boolean> layerVisibility;
    private final List<Layer> renderOrder;

    private BufferedImage sceneCache;
    private boolean sceneDirty;
    private boolean projectionDirty;

    private double worldMinX;
    private double worldMaxX;
    private double worldMinY;
    private double worldMaxY;
    private double worldWidth;
    private double worldHeight;

    private double zoom;
    private double panX;
    private double panY;
    private double baseScale;
    private double viewOriginX;
    private double viewOriginY;

    private Point dragStartPoint;
    private Point dragCurrentPoint;
    private DragMode dragMode;
    private Rectangle selectionRect;
    private String selectedEdgeKey;

    private Font labelFont;
    private boolean wasAntialias;

    private Listener listener;

    private enum DragMode {
        NONE,
        PAN,
        BOX_SELECT
    }

    public MapCanvas() {
        setBackground(new Color(248, 250, 252));
        setOpaque(true);

        this.vertices = new ArrayList<Vertex>();
        this.edges = new ArrayList<Edge>();
        this.vertexIndex = new HashMap<String, Vertex>();
        this.labelMetricsCache = new HashMap<String, LabelMetrics>();
        this.projectedVertexCache = new HashMap<String, Point2D.Double>();
        this.selectedVertexIds = new LinkedHashSet<String>();
        this.layerVisibility = new EnumMap<Layer, Boolean>(Layer.class);
        this.renderOrder = new ArrayList<Layer>(Arrays.asList(Layer.ROAD, Layer.FORBIDDEN, Layer.VERTEX, Layer.LABEL));

        for (Layer layer : Layer.values()) {
            layerVisibility.put(layer, Boolean.TRUE);
        }

        this.sceneDirty = true;
        this.projectionDirty = true;
        this.zoom = 1.0;
        this.panX = 0;
        this.panY = 0;
        this.dragMode = DragMode.NONE;

        recomputeWorldBounds();
        installInteractions();
    }

    public void setGraphData(Collection<Vertex> newVertices, Collection<Edge> newEdges) {
        vertices.clear();
        edges.clear();
        vertexIndex.clear();

        if (newVertices != null) {
            for (Vertex vertex : newVertices) {
                if (vertex == null) {
                    continue;
                }
                vertices.add(vertex);
                vertexIndex.put(vertex.getId(), vertex);
            }
        }
        if (newEdges != null) {
            for (Edge edge : newEdges) {
                if (edge == null) {
                    continue;
                }
                edges.add(edge);
            }
        }

        boolean selectionChanged = selectedVertexIds.retainAll(vertexIndex.keySet());
        if (selectedEdgeKey != null && findEdgeByKey(selectedEdgeKey) == null) {
            selectedEdgeKey = null;
            selectionChanged = true;
        }
        if (selectionChanged) {
            fireSelectionChanged();
        }

        recomputeWorldBounds();
        invalidateScene(true, true);
        repaint();
    }

    public void setLayerVisible(Layer layer, boolean visible) {
        if (layer == null) {
            return;
        }
        Boolean current = layerVisibility.get(layer);
        if (current != null && current.booleanValue() == visible) {
            return;
        }
        layerVisibility.put(layer, Boolean.valueOf(visible));
        invalidateScene(true, false);
        repaint();
    }

    public boolean isLayerVisible(Layer layer) {
        Boolean visible = layerVisibility.get(layer);
        return visible == null || visible.booleanValue();
    }

    public List<Layer> getRenderOrder() {
        return new ArrayList<Layer>(renderOrder);
    }

    public void setRenderOrder(List<Layer> order) {
        if (order == null || order.size() != Layer.values().length) {
            return;
        }
        Set<Layer> unique = new LinkedHashSet<Layer>(order);
        if (unique.size() != Layer.values().length) {
            return;
        }
        if (renderOrder.equals(order)) {
            return;
        }
        renderOrder.clear();
        renderOrder.addAll(order);
        invalidateScene(true, false);
        repaint();
    }

    public void resetViewport() {
        zoom = 1.0;
        panX = 0;
        panY = 0;
        invalidateScene(true, true);
        repaint();
        fireViewportChanged();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            ensureSceneCache();
            if (sceneCache != null) {
                g2.drawImage(sceneCache, 0, 0, null);
            }

            drawSelectionOverlay(g2);
            drawSelectionRect(g2);
        } finally {
            g2.dispose();
        }
    }

    private void ensureSceneCache() {
        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);

        if (sceneCache == null || sceneCache.getWidth() != width || sceneCache.getHeight() != height) {
            sceneCache = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            sceneDirty = true;
            projectionDirty = true;
        }

        if (!sceneDirty) {
            return;
        }

        Graphics2D g2 = sceneCache.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, width, height);
            renderScene(g2);
        } finally {
            g2.dispose();
        }
        sceneDirty = false;
    }

    private void renderScene(Graphics2D g2) {
        updateProjection();
        drawGrid(g2);

        for (Layer layer : renderOrder) {
            if (!isLayerVisible(layer)) {
                continue;
            }
            if (layer == Layer.ROAD) {
                drawRoadLayer(g2, false);
            } else if (layer == Layer.FORBIDDEN) {
                drawRoadLayer(g2, true);
            } else if (layer == Layer.VERTEX) {
                drawVertexLayer(g2);
            } else if (layer == Layer.LABEL) {
                drawLabelLayer(g2);
            }
        }
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(new Color(231, 236, 243));
        g2.setStroke(GRID_STROKE);

        int step = 80;
        for (int x = VIEW_PADDING; x < getWidth() - VIEW_PADDING; x += step) {
            g2.drawLine(x, VIEW_PADDING, x, getHeight() - VIEW_PADDING);
        }
        for (int y = VIEW_PADDING; y < getHeight() - VIEW_PADDING; y += step) {
            g2.drawLine(VIEW_PADDING, y, getWidth() - VIEW_PADDING, y);
        }
    }

    private void drawRoadLayer(Graphics2D g2, boolean forbiddenLayer) {
        List<Edge> renderableEdges = buildRenderableEdges();
        for (Edge edge : renderableEdges) {
            if (forbiddenLayer != edge.isForbidden()) {
                continue;
            }

            Point2D.Double from = project(edge.getFromVertex());
            Point2D.Double to = project(edge.getToVertex());
            if (from == null || to == null) {
                continue;
            }

            Color color = forbiddenLayer ? new Color(204, 51, 48) : roadColor(edge.getRoadType());
            float width = forbiddenLayer ? 4.0f : roadWidth(edge.getRoadType());
            Stroke stroke = forbiddenLayer
                    ? new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{10f, 8f}, 0f)
                    : new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

            g2.setColor(color);
            g2.setStroke(stroke);
            g2.draw(new Line2D.Double(from, to));

            if (!forbiddenLayer && edge.isOneWay()) {
                drawDirectionArrow(g2, from, to, color);
            }
        }
    }

    private void drawDirectionArrow(Graphics2D g2, Point2D.Double from, Point2D.Double to, Color color) {
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

    private void drawVertexLayer(Graphics2D g2) {
        for (Vertex vertex : vertices) {
            Point2D.Double point = project(vertex);
            if (point == null) {
                continue;
            }

            int radius = (int) clamp(6 + zoom * 2, 6, 12);
            int diameter = radius * 2;
            int left = (int) Math.round(point.x) - radius;
            int top = (int) Math.round(point.y) - radius;

            g2.setColor(placeColor(vertex.getType()));
            g2.fillOval(left, top, diameter, diameter);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawOval(left, top, diameter, diameter);
        }
    }

    private void drawLabelLayer(Graphics2D g2) {
        Font base = UiStyles.BODY_FONT;
        Font scaled = base.deriveFont((float) clamp(base.getSize2D() * zoom, 11f, 16f));
        g2.setFont(scaled);

        boolean antialias = RenderingHints.VALUE_TEXT_ANTIALIAS_ON.equals(g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
        if (!scaled.equals(labelFont) || antialias != wasAntialias) {
            labelMetricsCache.clear();
            labelFont = scaled;
            wasAntialias = antialias;
        }

        FontMetrics metrics = g2.getFontMetrics(scaled);
        g2.setColor(UiStyles.TEXT_PRIMARY);

        for (Vertex vertex : vertices) {
            Point2D.Double point = project(vertex);
            if (point == null) {
                continue;
            }

            LabelMetrics labelMetrics = getLabelMetrics(metrics, vertex.getName());
            int x = (int) Math.round(point.x + 9);
            int y = (int) Math.round(point.y - 8);

            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillRoundRect(x - 3, y - labelMetrics.ascent, labelMetrics.width + 6, labelMetrics.height + 2, 8, 8);
            g2.setColor(UiStyles.TEXT_PRIMARY);
            g2.drawString(vertex.getName(), x, y);
        }
    }

    private void drawSelectionOverlay(Graphics2D g2) {
        if (!selectedVertexIds.isEmpty() && isLayerVisible(Layer.VERTEX)) {
            g2.setStroke(new BasicStroke(2.0f));
            for (String vertexId : selectedVertexIds) {
                Vertex vertex = vertexIndex.get(vertexId);
                Point2D.Double point = project(vertex);
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
            Edge edge = findEdgeByKey(selectedEdgeKey);
            if (edge != null) {
                Point2D.Double from = project(edge.getFromVertex());
                Point2D.Double to = project(edge.getToVertex());
                if (from != null && to != null) {
                    g2.setColor(new Color(255, 153, 0));
                    g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(new Line2D.Double(from, to));
                }
            }
        }
    }

    private void drawSelectionRect(Graphics2D g2) {
        if (selectionRect == null) {
            return;
        }
        g2.setColor(new Color(30, 106, 255, 35));
        g2.fill(selectionRect);
        g2.setColor(new Color(30, 106, 255, 160));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(selectionRect);
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

    private void installInteractions() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocusInWindow();
                dragStartPoint = event.getPoint();
                dragCurrentPoint = event.getPoint();

                if (SwingUtilities.isMiddleMouseButton(event) || SwingUtilities.isRightMouseButton(event)) {
                    dragMode = DragMode.PAN;
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(event)) {
                    dragMode = DragMode.BOX_SELECT;
                    selectionRect = new Rectangle(event.getX(), event.getY(), 0, 0);
                    repaint(selectionRect);
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (dragStartPoint == null) {
                    return;
                }
                if (dragMode == DragMode.PAN) {
                    int dx = event.getX() - dragCurrentPoint.x;
                    int dy = event.getY() - dragCurrentPoint.y;
                    panX += dx;
                    panY += dy;
                    dragCurrentPoint = event.getPoint();
                    invalidateScene(true, true);
                    repaint();
                    fireViewportChanged();
                    return;
                }
                if (dragMode == DragMode.BOX_SELECT) {
                    Rectangle oldRect = selectionRect == null ? null : new Rectangle(selectionRect);
                    selectionRect = createRect(dragStartPoint, event.getPoint());
                    dragCurrentPoint = event.getPoint();
                    repaintUnion(oldRect, selectionRect);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (dragMode == DragMode.PAN) {
                    dragMode = DragMode.NONE;
                    dragStartPoint = null;
                    dragCurrentPoint = null;
                    return;
                }

                if (dragMode == DragMode.BOX_SELECT && SwingUtilities.isLeftMouseButton(event)) {
                    Rectangle finalRect = selectionRect == null ? createRect(dragStartPoint, event.getPoint()) : selectionRect;
                    boolean clickAction = finalRect.width < 4 && finalRect.height < 4;
                    if (clickAction) {
                        handleSingleSelection(event.getPoint(), event.isControlDown());
                    } else {
                        handleBoxSelection(finalRect, event.isControlDown());
                    }

                    Rectangle dirty = selectionRect == null ? finalRect : union(selectionRect, finalRect);
                    selectionRect = null;
                    if (dirty != null) {
                        repaint(dirty.x - 4, dirty.y - 4, dirty.width + 8, dirty.height + 8);
                    } else {
                        repaint();
                    }
                }

                dragMode = DragMode.NONE;
                dragStartPoint = null;
                dragCurrentPoint = null;
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        MouseWheelListener wheelHandler = new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                double oldZoom = zoom;
                double factor = event.getWheelRotation() < 0 ? ZOOM_STEP : (1.0 / ZOOM_STEP);
                double nextZoom = clamp(oldZoom * factor, MIN_ZOOM, MAX_ZOOM);
                if (Math.abs(nextZoom - oldZoom) < 1e-6) {
                    return;
                }

                updateProjection();
                Point2D.Double world = toWorld(event.getPoint());
                zoom = nextZoom;
                invalidateScene(true, true);
                updateProjection();

                Point2D.Double projected = project(world);
                if (projected != null) {
                    panX += event.getX() - projected.x;
                    panY += event.getY() - projected.y;
                    invalidateScene(true, true);
                }

                repaint();
                fireViewportChanged();
            }
        };
        addMouseWheelListener(wheelHandler);
    }

    private void handleSingleSelection(Point point, boolean appendSelection) {
        String previousEdgeKey = selectedEdgeKey;
        Set<String> before = new LinkedHashSet<String>(selectedVertexIds);

        Vertex vertex = findVertexAt(point);
        if (vertex != null) {
            String id = vertex.getId();
            if (!appendSelection) {
                selectedVertexIds.clear();
            }
            if (appendSelection && selectedVertexIds.contains(id)) {
                selectedVertexIds.remove(id);
            } else {
                selectedVertexIds.add(id);
            }
            selectedEdgeKey = null;
            repaintSelectionDelta(before, selectedVertexIds);
            fireSelectionChanged();
            return;
        }

        Edge edge = findEdgeAt(point);
        if (edge != null) {
            selectedEdgeKey = edgeKey(edge);
            if (!appendSelection) {
                selectedVertexIds.clear();
            }
            repaint();
            fireSelectionChanged();
            return;
        }

        if (!appendSelection) {
            selectedVertexIds.clear();
            selectedEdgeKey = null;
            if (!before.isEmpty() || previousEdgeKey != null) {
                repaint();
                fireSelectionChanged();
            }
        }
    }

    private void handleBoxSelection(Rectangle rect, boolean appendSelection) {
        Set<String> before = new LinkedHashSet<String>(selectedVertexIds);
        if (!appendSelection) {
            selectedVertexIds.clear();
        }

        for (Vertex vertex : vertices) {
            Point2D.Double point = project(vertex);
            if (point == null) {
                continue;
            }
            if (rect.contains(point)) {
                selectedVertexIds.add(vertex.getId());
            }
        }

        selectedEdgeKey = null;
        repaintSelectionDelta(before, selectedVertexIds);
        fireSelectionChanged();
    }

    private Vertex findVertexAt(Point point) {
        if (!isLayerVisible(Layer.VERTEX)) {
            return null;
        }
        Vertex nearest = null;
        double nearestDist = Double.MAX_VALUE;
        double threshold = 12;

        for (Vertex vertex : vertices) {
            Point2D.Double projected = project(vertex);
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

    private Edge findEdgeAt(Point point) {
        List<Edge> renderableEdges = buildRenderableEdges();
        Edge nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Edge edge : renderableEdges) {
            boolean visible = edge.isForbidden() ? isLayerVisible(Layer.FORBIDDEN) : isLayerVisible(Layer.ROAD);
            if (!visible) {
                continue;
            }
            Point2D.Double from = project(edge.getFromVertex());
            Point2D.Double to = project(edge.getToVertex());
            if (from == null || to == null) {
                continue;
            }
            double distance = Line2D.ptSegDist(from.x, from.y, to.x, to.y, point.x, point.y);
            if (distance <= 6 && distance < nearestDist) {
                nearest = edge;
                nearestDist = distance;
            }
        }
        return nearest;
    }

    private Edge findEdgeByKey(String key) {
        if (key == null) {
            return null;
        }
        for (Edge edge : buildRenderableEdges()) {
            if (key.equals(edgeKey(edge))) {
                return edge;
            }
        }
        return null;
    }

    private List<Edge> buildRenderableEdges() {
        Map<String, Edge> deduped = new LinkedHashMap<String, Edge>();
        for (Edge edge : edges) {
            String key = edgeKey(edge);
            Edge existing = deduped.get(key);
            if (existing == null) {
                deduped.put(key, edge);
                continue;
            }
            // Keep deterministic representation while preferring forbidden state when inconsistent.
            if (!existing.isForbidden() && edge.isForbidden()) {
                deduped.put(key, edge);
            }
        }
        return new ArrayList<Edge>(deduped.values());
    }

    private static String edgeKey(Edge edge) {
        String from = edge.getFromVertex().getId();
        String to = edge.getToVertex().getId();
        if (edge.isOneWay()) {
            return "ONE:" + from + "->" + to;
        }
        if (from.compareTo(to) <= 0) {
            return "TWO:" + from + "<->" + to;
        }
        return "TWO:" + to + "<->" + from;
    }

    private void updateProjection() {
        if (!projectionDirty) {
            return;
        }

        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);

        double availableWidth = Math.max(width - VIEW_PADDING * 2, 20);
        double availableHeight = Math.max(height - VIEW_PADDING * 2, 20);

        double safeWorldWidth = Math.max(worldWidth, 1);
        double safeWorldHeight = Math.max(worldHeight, 1);

        baseScale = Math.min(availableWidth / safeWorldWidth, availableHeight / safeWorldHeight);
        baseScale = Math.max(baseScale, 0.01);

        double scaledWidth = safeWorldWidth * baseScale * zoom;
        double scaledHeight = safeWorldHeight * baseScale * zoom;

        viewOriginX = VIEW_PADDING + (availableWidth - scaledWidth) / 2.0 + panX;
        viewOriginY = VIEW_PADDING + (availableHeight - scaledHeight) / 2.0 + panY;

        projectedVertexCache.clear();
        projectionDirty = false;
    }

    private Point2D.Double project(Vertex vertex) {
        if (vertex == null) {
            return null;
        }
        Point2D.Double cached = projectedVertexCache.get(vertex.getId());
        if (cached != null) {
            return cached;
        }

        updateProjection();
        double x = viewOriginX + (vertex.getX() - worldMinX) * baseScale * zoom;
        double y = viewOriginY + (vertex.getY() - worldMinY) * baseScale * zoom;
        Point2D.Double projected = new Point2D.Double(x, y);
        projectedVertexCache.put(vertex.getId(), projected);
        return projected;
    }

    private Point2D.Double project(Point2D.Double world) {
        if (world == null) {
            return null;
        }
        updateProjection();
        double x = viewOriginX + (world.x - worldMinX) * baseScale * zoom;
        double y = viewOriginY + (world.y - worldMinY) * baseScale * zoom;
        return new Point2D.Double(x, y);
    }

    private Point2D.Double toWorld(Point point) {
        updateProjection();
        double worldX = ((point.getX() - viewOriginX) / (baseScale * zoom)) + worldMinX;
        double worldY = ((point.getY() - viewOriginY) / (baseScale * zoom)) + worldMinY;
        return new Point2D.Double(worldX, worldY);
    }

    private void recomputeWorldBounds() {
        if (vertices.isEmpty()) {
            worldMinX = -100;
            worldMaxX = 100;
            worldMinY = -100;
            worldMaxY = 100;
            worldWidth = 200;
            worldHeight = 200;
            projectionDirty = true;
            return;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (Vertex vertex : vertices) {
            minX = Math.min(minX, vertex.getX());
            minY = Math.min(minY, vertex.getY());
            maxX = Math.max(maxX, vertex.getX());
            maxY = Math.max(maxY, vertex.getY());
        }

        worldMinX = minX - WORLD_PADDING;
        worldMaxX = maxX + WORLD_PADDING;
        worldMinY = minY - WORLD_PADDING;
        worldMaxY = maxY + WORLD_PADDING;
        worldWidth = Math.max(1, worldMaxX - worldMinX);
        worldHeight = Math.max(1, worldMaxY - worldMinY);
        projectionDirty = true;
    }

    private void invalidateScene(boolean markSceneDirty, boolean markProjectionDirty) {
        if (markSceneDirty) {
            sceneDirty = true;
        }
        if (markProjectionDirty) {
            projectionDirty = true;
        }
    }

    private void repaintSelectionDelta(Set<String> before, Set<String> after) {
        Rectangle dirty = null;
        Set<String> union = new LinkedHashSet<String>();
        union.addAll(before);
        union.addAll(after);

        for (String vertexId : union) {
            Vertex vertex = vertexIndex.get(vertexId);
            Point2D.Double point = project(vertex);
            if (point == null) {
                continue;
            }
            Rectangle rect = aroundPoint(point, 22);
            dirty = union(dirty, rect);
        }

        if (dirty == null) {
            repaint();
        } else {
            repaint(dirty.x, dirty.y, dirty.width, dirty.height);
        }
    }

    private static Rectangle aroundPoint(Point2D.Double point, int radius) {
        return new Rectangle((int) Math.round(point.x) - radius, (int) Math.round(point.y) - radius, radius * 2 + 1, radius * 2 + 1);
    }

    private static Rectangle createRect(Point start, Point end) {
        int left = Math.min(start.x, end.x);
        int top = Math.min(start.y, end.y);
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        return new Rectangle(left, top, width, height);
    }

    private static Rectangle union(Rectangle a, Rectangle b) {
        if (a == null) {
            return b == null ? null : new Rectangle(b);
        }
        if (b == null) {
            return new Rectangle(a);
        }
        return a.union(b);
    }

    private void repaintUnion(Rectangle oldRect, Rectangle newRect) {
        Rectangle dirty = union(oldRect, newRect);
        if (dirty == null) {
            repaint();
            return;
        }
        repaint(dirty.x - 4, dirty.y - 4, dirty.width + 8, dirty.height + 8);
    }

    private void fireSelectionChanged() {
        if (listener == null) {
            return;
        }
        List<String> selected = new ArrayList<String>(selectedVertexIds);
        Collections.sort(selected);
        listener.onSelectionChanged(selected, selectedEdgeKey);
    }

    private void fireViewportChanged() {
        if (listener == null) {
            return;
        }
        listener.onViewportChanged(zoom, panX, panY);
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

    private static Color roadColor(RoadType roadType) {
        if (roadType == RoadType.MAIN_ROAD) {
            return new Color(53, 99, 233);
        }
        if (roadType == RoadType.STAIRS) {
            return new Color(118, 86, 227);
        }
        return new Color(92, 105, 117);
    }

    private static float roadWidth(RoadType roadType) {
        if (roadType == RoadType.MAIN_ROAD) {
            return 4.4f;
        }
        if (roadType == RoadType.STAIRS) {
            return 3.2f;
        }
        return 2.8f;
    }

    private static Color placeColor(PlaceType placeType) {
        if (placeType == PlaceType.GATE) {
            return new Color(57, 135, 245);
        }
        if (placeType == PlaceType.LIBRARY) {
            return new Color(36, 171, 104);
        }
        if (placeType == PlaceType.CANTEEN) {
            return new Color(242, 153, 63);
        }
        if (placeType == PlaceType.TEACHING_BUILDING) {
            return new Color(140, 113, 235);
        }
        if (placeType == PlaceType.DORMITORY) {
            return new Color(62, 167, 188);
        }
        if (placeType == PlaceType.OFFICE) {
            return new Color(230, 111, 81);
        }
        if (placeType == PlaceType.SPORTS_CENTER) {
            return new Color(68, 189, 98);
        }
        return new Color(115, 125, 142);
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

    public interface Listener {
        void onSelectionChanged(List<String> selectedVertexIds, String selectedEdgeKey);

        void onViewportChanged(double zoom, double panX, double panY);
    }
}
