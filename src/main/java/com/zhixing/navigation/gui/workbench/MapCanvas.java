package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.model.RouteVisualizationDto;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
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

    private static final double MIN_ZOOM = 0.50;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 1.12;
    private static final int VIEW_PADDING = 40;
    private static final int WORLD_PADDING = 20;
    private static final double GRID_SNAP_SIZE = 20.0;
    private static final int GRID_SNAP_PIXELS = 10;
    private static final int SNAP_VERTEX_PIXELS = 14;
    private static final int AXIS_ALIGN_PIXELS = 10;
    private static final double MOVE_HIT_THRESHOLD_PIXELS = 20.0;
    private static final int PAN_VISIBLE_MIN_PIXELS = 36;
    private static final double PAN_OVERSCROLL_FACTOR = 0.35;
    private static final int FLASH_CYCLE_LIMIT = 8;
    private static final int FLASH_INTERVAL_MS = 180;

    private final List<Vertex> vertices;
    private final List<Edge> edges;
    private final Map<String, Vertex> vertexIndex;
    private final Map<String, Point2D.Double> projectedVertexCache;
    private final Set<String> selectedVertexIds;
    private final Map<Layer, Boolean> layerVisibility;
    private final Map<Layer, Boolean> layerLocked;
    private final Map<Layer, Float> layerOpacity;
    private final List<Layer> renderOrder;
    private final MapSceneCache sceneCache;
    private final MapCanvasBaseSceneRenderer baseSceneRenderer;
    private final MapCanvasRouteRenderer routeRenderer;
    private final MapCanvasOverlayRenderer overlayRenderer;
    private final Timer routeFlashTimer;

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
    private int focusedRouteSegmentIndex;
    private boolean focusedSegmentVisible;
    private int focusedSegmentFlashCycle;
    private EditToolMode editToolMode;
    private String pendingEdgeStartVertexId;
    private String draggingVertexId;
    private boolean draggingVertexMoved;
    private Point2D.Double draggingPreviewWorld;
    private boolean spacePanMode;
    private SnapGuide activeSnapGuide;
    private RouteVisualizationDto currentRouteVisualization;
    private RouteVisualizationDto previousRouteVisualization;
    private BufferedImage referenceImage;
    private double referenceImageScale;
    private double metersPerWorldUnit;

    private Listener listener;

    private enum DragMode {
        NONE,
        PAN,
        BOX_SELECT,
        MOVE_VERTEX
    }

    public MapCanvas() {
        setBackground(new Color(248, 250, 252));
        setOpaque(true);

        this.vertices = new ArrayList<Vertex>();
        this.edges = new ArrayList<Edge>();
        this.vertexIndex = new HashMap<String, Vertex>();
        this.projectedVertexCache = new HashMap<String, Point2D.Double>();
        this.selectedVertexIds = new LinkedHashSet<String>();
        this.layerVisibility = new EnumMap<Layer, Boolean>(Layer.class);
        this.layerLocked = new EnumMap<Layer, Boolean>(Layer.class);
        this.layerOpacity = new EnumMap<Layer, Float>(Layer.class);
        this.renderOrder = new ArrayList<Layer>(Arrays.asList(Layer.ROAD, Layer.FORBIDDEN, Layer.VERTEX, Layer.LABEL));

        for (Layer layer : Layer.values()) {
            layerVisibility.put(layer, Boolean.TRUE);
            layerLocked.put(layer, Boolean.FALSE);
            layerOpacity.put(layer, Float.valueOf(1.0f));
        }

        this.sceneCache = new MapSceneCache();
        this.baseSceneRenderer = new MapCanvasBaseSceneRenderer();
        this.routeRenderer = new MapCanvasRouteRenderer();
        this.overlayRenderer = new MapCanvasOverlayRenderer();
        this.projectionDirty = true;
        this.zoom = 1.0;
        this.panX = 0;
        this.panY = 0;
        this.dragMode = DragMode.NONE;
        this.focusedRouteSegmentIndex = -1;
        this.focusedSegmentVisible = true;
        this.focusedSegmentFlashCycle = 0;
        this.editToolMode = EditToolMode.SELECT;
        this.pendingEdgeStartVertexId = null;
        this.draggingVertexId = null;
        this.draggingVertexMoved = false;
        this.draggingPreviewWorld = null;
        this.spacePanMode = false;
        this.activeSnapGuide = SnapGuide.none();
        this.currentRouteVisualization = null;
        this.previousRouteVisualization = null;
        this.referenceImage = null;
        this.referenceImageScale = 1.0;
        this.metersPerWorldUnit = 1.0;
        setFocusable(true);

        this.routeFlashTimer = new Timer(FLASH_INTERVAL_MS, e -> {
            focusedSegmentFlashCycle++;
            focusedSegmentVisible = !focusedSegmentVisible;
            if (focusedSegmentFlashCycle >= FLASH_CYCLE_LIMIT) {
                Object source = e.getSource();
                if (source instanceof Timer) {
                    ((Timer) source).stop();
                }
                focusedSegmentVisible = true;
            }
            repaint();
        });
        this.routeFlashTimer.setRepeats(true);

        recomputeWorldBounds();
        installInteractions();
        installKeyboardShortcuts();
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
        if (focusedRouteSegmentIndex >= currentRouteSegmentCount()) {
            focusedRouteSegmentIndex = -1;
            focusedSegmentVisible = true;
            focusedSegmentFlashCycle = 0;
            routeFlashTimer.stop();
        }

        boolean selectionChanged = selectedVertexIds.retainAll(vertexIndex.keySet());
        if (selectedEdgeKey != null && findEdgeByKey(selectedEdgeKey) == null) {
            selectedEdgeKey = null;
            selectionChanged = true;
        }
        if (pendingEdgeStartVertexId != null && !vertexIndex.containsKey(pendingEdgeStartVertexId)) {
            pendingEdgeStartVertexId = null;
            fireEdgeDraftChanged(null);
        }
        if (draggingVertexId != null && !vertexIndex.containsKey(draggingVertexId)) {
            draggingVertexId = null;
            draggingPreviewWorld = null;
            draggingVertexMoved = false;
            dragMode = DragMode.NONE;
        }
        if (selectionChanged) {
            fireSelectionChanged();
        }

        recomputeWorldBounds();
        clampPanOffsets();
        invalidateScene(true, true);
        repaint();
    }

    public void setRouteComparison(RouteVisualizationDto currentRoute, RouteVisualizationDto previousRoute) {
        currentRouteVisualization = currentRoute;
        previousRouteVisualization = previousRoute;
        focusedRouteSegmentIndex = -1;
        focusedSegmentVisible = true;
        focusedSegmentFlashCycle = 0;
        routeFlashTimer.stop();
        repaint();
    }

    public void clearRouteComparison() {
        currentRouteVisualization = null;
        previousRouteVisualization = null;
        focusedRouteSegmentIndex = -1;
        focusedSegmentVisible = true;
        focusedSegmentFlashCycle = 0;
        routeFlashTimer.stop();
        repaint();
    }

    public void focusRouteSegment(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= currentRouteSegmentCount()) {
            focusedRouteSegmentIndex = -1;
            focusedSegmentVisible = true;
            focusedSegmentFlashCycle = 0;
            routeFlashTimer.stop();
            repaint();
            return;
        }
        focusedRouteSegmentIndex = segmentIndex;
        focusedSegmentVisible = true;
        focusedSegmentFlashCycle = 0;
        routeFlashTimer.restart();
        centerOnSegment(segmentIndex);
        repaint();
    }

    public EditToolMode editToolMode() {
        return editToolMode;
    }

    public void setEditToolMode(EditToolMode mode) {
        EditToolMode target = mode == null ? EditToolMode.SELECT : mode;
        if (editToolMode == target) {
            return;
        }
        editToolMode = target;
        pendingEdgeStartVertexId = null;
        draggingVertexId = null;
        draggingVertexMoved = false;
        draggingPreviewWorld = null;
        dragMode = DragMode.NONE;
        selectionRect = null;
        fireEdgeDraftChanged(null);
        repaint();
    }

    public List<String> selectedVertexIds() {
        List<String> ids = new ArrayList<String>(selectedVertexIds);
        Collections.sort(ids);
        return ids;
    }

    public String selectedEdgeKey() {
        return selectedEdgeKey;
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

    public void setLayerLocked(Layer layer, boolean locked) {
        if (layer == null) {
            return;
        }
        Boolean current = layerLocked.get(layer);
        if (current != null && current.booleanValue() == locked) {
            return;
        }
        layerLocked.put(layer, Boolean.valueOf(locked));
        if (locked) {
            // Clear incompatible selections when a layer gets locked.
            if (layer == Layer.VERTEX && !selectedVertexIds.isEmpty()) {
                selectedVertexIds.clear();
                fireSelectionChanged();
            }
            if ((layer == Layer.ROAD || layer == Layer.FORBIDDEN) && selectedEdgeKey != null) {
                Edge selected = findEdgeByKey(selectedEdgeKey);
                if (selected != null) {
                    Layer selectedLayer = selected.isForbidden() ? Layer.FORBIDDEN : Layer.ROAD;
                    if (selectedLayer == layer) {
                        selectedEdgeKey = null;
                        fireSelectionChanged();
                    }
                }
            }
        }
        repaint();
    }

    public boolean isLayerLocked(Layer layer) {
        Boolean locked = layerLocked.get(layer);
        return locked != null && locked.booleanValue();
    }

    public void setLayerOpacity(Layer layer, float opacity) {
        if (layer == null) {
            return;
        }
        float next = (float) clamp(opacity, 0.15, 1.0);
        Float current = layerOpacity.get(layer);
        if (current != null && Math.abs(current.floatValue() - next) < 0.0001f) {
            return;
        }
        layerOpacity.put(layer, Float.valueOf(next));
        invalidateScene(true, false);
        repaint();
    }

    public float getLayerOpacity(Layer layer) {
        Float opacity = layerOpacity.get(layer);
        if (opacity == null) {
            return 1.0f;
        }
        return opacity.floatValue();
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
        draggingPreviewWorld = null;
        activeSnapGuide = SnapGuide.none();
        clampPanOffsets();
        invalidateScene(true, true);
        repaint();
        fireViewportChanged();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setReferenceImage(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("reference image must be a non-empty bitmap");
        }
        this.referenceImage = image;
        invalidateScene(true, false);
        repaint();
    }

    public boolean hasReferenceImage() {
        return referenceImage != null;
    }

    public void clearReferenceImage() {
        this.referenceImage = null;
        invalidateScene(true, false);
        repaint();
    }

    public void setReferenceImageScale(double scale) {
        if (Double.isNaN(scale) || Double.isInfinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("reference image scale must be finite and > 0");
        }
        this.referenceImageScale = scale;
        invalidateScene(true, false);
        repaint();
    }

    public double getReferenceImageScale() {
        return referenceImageScale;
    }

    public void setMetersPerWorldUnit(double metersPerWorldUnit) {
        if (Double.isNaN(metersPerWorldUnit) || Double.isInfinite(metersPerWorldUnit) || metersPerWorldUnit <= 0) {
            throw new IllegalArgumentException("metersPerWorldUnit must be finite and > 0");
        }
        this.metersPerWorldUnit = metersPerWorldUnit;
        repaint();
    }

    public double getMetersPerWorldUnit() {
        return metersPerWorldUnit;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            ensureSceneCache();
            if (sceneCache.image() != null) {
                g2.drawImage(sceneCache.image(), 0, 0, null);
            }

            routeRenderer.draw(
                    g2,
                    currentRouteVisualization,
                    previousRouteVisualization,
                    focusedRouteSegmentIndex,
                    focusedSegmentVisible,
                    this::projectVertexById
            );
            overlayRenderer.drawOverlay(
                    g2,
                    getHeight(),
                    VIEW_PADDING,
                    zoom,
                    baseScale,
                    metersPerWorldUnit,
                    worldMinX,
                    worldMaxX,
                    worldMinY,
                    worldMaxY,
                    selectedVertexIds,
                    selectedEdgeKey,
                    pendingEdgeStartVertexId,
                    dragMode == DragMode.MOVE_VERTEX,
                    draggingVertexId,
                    draggingPreviewWorld,
                    selectionRect,
                    activeSnapGuide != null && !activeSnapGuide.isEmpty(),
                    activeSnapGuide != null && activeSnapGuide.vertical,
                    activeSnapGuide != null && activeSnapGuide.horizontal,
                    activeSnapGuide == null ? 0.0 : activeSnapGuide.worldX,
                    activeSnapGuide == null ? 0.0 : activeSnapGuide.worldY,
                    this::isLayerVisible,
                    vertexIndex::get,
                    this::findEdgeByKey,
                    vertex -> project(vertex),
                    world -> project(world),
                    MapCanvas::placeColor
            );
        } finally {
            g2.dispose();
        }
    }

    private void ensureSceneCache() {
        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);

        if (sceneCache.ensureSize(width, height)) {
            projectionDirty = true;
        }

        if (!sceneCache.isDirty()) {
            return;
        }

        Graphics2D g2 = sceneCache.image().createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, width, height);
            renderScene(g2);
        } finally {
            g2.dispose();
        }
        sceneCache.markClean();
    }

    private void renderScene(Graphics2D g2) {
        updateProjection();
        baseSceneRenderer.drawScene(
                g2,
                getWidth(),
                getHeight(),
                VIEW_PADDING,
                zoom,
                baseScale,
                worldMinX,
                worldMaxX,
                worldMinY,
                worldMaxY,
                referenceImage,
                referenceImageScale,
                renderOrder,
                edges,
                vertices,
                this::isLayerVisible,
                layer -> Float.valueOf(getLayerOpacity(layer)),
                vertex -> project(vertex),
                world -> project(world),
                MapCanvas::roadColor,
                roadType -> Float.valueOf(roadWidth(roadType)),
                MapCanvas::placeColor,
                MapCanvas::withOpacity
        );
    }

    private Point2D.Double projectVertexById(String vertexId) {
        if (vertexId == null) {
            return null;
        }
        Vertex vertex = vertexIndex.get(vertexId);
        return project(vertex);
    }

    private void installKeyboardShortcuts() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed SPACE"), "space-pan-on");
        getActionMap().put("space-pan-on", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!spacePanMode) {
                    spacePanMode = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }
        });
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released SPACE"), "space-pan-off");
        getActionMap().put("space-pan-off", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                spacePanMode = false;
                setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private void installInteractions() {
        MouseAdapter mouseHandler = createMouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(createMouseWheelHandler());
    }

    private MouseAdapter createMouseHandler() {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                handleMousePressed(event);
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                handleMouseDragged(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                handleMouseReleased(event);
            }
        };
    }

    private MouseWheelListener createMouseWheelHandler() {
        return new MouseWheelListener() {
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
                    clampPanOffsets();
                    invalidateScene(true, true);
                }

                repaint();
                fireViewportChanged();
            }
        };
    }

    private void handleMousePressed(MouseEvent event) {
        requestFocusInWindow();
        dragStartPoint = event.getPoint();
        dragCurrentPoint = event.getPoint();
        activeSnapGuide = SnapGuide.none();
        draggingPreviewWorld = null;

        if (SwingUtilities.isMiddleMouseButton(event) || SwingUtilities.isRightMouseButton(event)) {
            dragMode = DragMode.PAN;
            return;
        }

        if (!SwingUtilities.isLeftMouseButton(event)) {
            return;
        }

        if (spacePanMode) {
            dragMode = DragMode.PAN;
            return;
        }

        if (editToolMode == EditToolMode.MOVE_VERTEX) {
            Vertex vertex = findVertexAt(event.getPoint());
            if (vertex != null) {
                draggingVertexId = vertex.getId();
                draggingVertexMoved = false;
                draggingPreviewWorld = new Point2D.Double(vertex.getX(), vertex.getY());
                dragMode = DragMode.MOVE_VERTEX;
                return;
            }
        }

        if (editToolMode == EditToolMode.SELECT) {
            dragMode = DragMode.BOX_SELECT;
            selectionRect = new Rectangle(event.getX(), event.getY(), 0, 0);
            repaint(selectionRect);
            return;
        }

        dragMode = DragMode.NONE;
    }

    private void handleMouseDragged(MouseEvent event) {
        if (dragStartPoint == null) {
            return;
        }
        if (dragMode == DragMode.PAN) {
            handlePanDrag(event);
            return;
        }
        if (dragMode == DragMode.BOX_SELECT) {
            handleBoxSelectDrag(event);
            return;
        }
        if (dragMode == DragMode.MOVE_VERTEX) {
            handleMoveVertexDrag(event);
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        if (dragMode == DragMode.PAN) {
            activeSnapGuide = SnapGuide.none();
            resetDragState();
            return;
        }

        if (!SwingUtilities.isLeftMouseButton(event)) {
            resetDragState();
            return;
        }

        if (dragMode == DragMode.MOVE_VERTEX) {
            if (draggingVertexId != null && (draggingVertexMoved || event.getPoint().distance(dragStartPoint) > 2.0)) {
                Point2D.Double target = draggingPreviewWorld != null ? draggingPreviewWorld : toWorld(event.getPoint());
                Point2D.Double snappedTarget = snapToWorld(target, draggingVertexId, false);
                fireMoveVertexRequested(draggingVertexId, snappedTarget.x, snappedTarget.y);
            } else if (draggingVertexId != null) {
                handleSingleSelection(event.getPoint(), event.isControlDown());
            }
            activeSnapGuide = SnapGuide.none();
            resetDragState();
            return;
        }

        if (editToolMode == EditToolMode.SELECT && dragMode == DragMode.BOX_SELECT) {
            Rectangle finalRect = selectionRect == null ? createRect(dragStartPoint, event.getPoint()) : selectionRect;
            if (finalRect.width < 4 && finalRect.height < 4) {
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
            resetDragState();
            return;
        }

        handleEditToolClick(event.getPoint(), event.isControlDown());
        activeSnapGuide = SnapGuide.none();
        resetDragState();
    }

    private void handlePanDrag(MouseEvent event) {
        int dx = event.getX() - dragCurrentPoint.x;
        int dy = event.getY() - dragCurrentPoint.y;
        panX += dx;
        panY += dy;
        clampPanOffsets();
        dragCurrentPoint = event.getPoint();
        invalidateScene(true, true);
        repaint();
        fireViewportChanged();
    }

    private void handleBoxSelectDrag(MouseEvent event) {
        Rectangle oldRect = selectionRect == null ? null : new Rectangle(selectionRect);
        selectionRect = createRect(dragStartPoint, event.getPoint());
        dragCurrentPoint = event.getPoint();
        repaintUnion(oldRect, selectionRect);
    }

    private void handleMoveVertexDrag(MouseEvent event) {
        if (draggingVertexId == null) {
            return;
        }
        if (!draggingVertexMoved) {
            draggingVertexMoved = event.getPoint().distance(dragStartPoint) > 2.0;
        }
        Point2D.Double world = toWorld(event.getPoint());
        draggingPreviewWorld = snapToWorld(world, draggingVertexId, true);
        repaint();
    }

    private void handleEditToolClick(Point point, boolean appendSelection) {
        Vertex vertex = findVertexAt(point);
        Edge edge = findEdgeAt(point);

        if (editToolMode == EditToolMode.ADD_VERTEX) {
            if (isLayerLocked(Layer.VERTEX)) {
                fireCanvasHint("点位层已锁定，无法新增点位。");
                return;
            }
            if (vertex == null) {
                Point2D.Double world = toWorld(point);
                fireAddVertexRequested(world.x, world.y);
                return;
            }
            handleSingleSelection(point, appendSelection);
            return;
        }

        if (editToolMode == EditToolMode.ADD_EDGE) {
            if (isLayerLocked(Layer.VERTEX) || isLayerLocked(Layer.ROAD)) {
                fireCanvasHint("点位层或道路层已锁定，无法新建连线。");
                return;
            }
            if (vertex == null) {
                handleSingleSelection(point, appendSelection);
                return;
            }
            String vertexId = vertex.getId();
            handleSingleSelection(point, appendSelection);
            if (pendingEdgeStartVertexId == null) {
                pendingEdgeStartVertexId = vertexId;
                fireEdgeDraftChanged(pendingEdgeStartVertexId);
                return;
            }
            if (!pendingEdgeStartVertexId.equals(vertexId)) {
                fireConnectVerticesRequested(pendingEdgeStartVertexId, vertexId);
            }
            pendingEdgeStartVertexId = null;
            fireEdgeDraftChanged(null);
            return;
        }

        if (editToolMode == EditToolMode.DELETE_OBJECT) {
            if (vertex != null) {
                fireDeleteVertexRequested(vertex.getId());
                return;
            }
            if (edge != null) {
                fireDeleteEdgeRequested(MapCanvasEdgeIndex.edgeKey(edge));
                return;
            }
            if (isLayerLocked(Layer.VERTEX) && isLayerLocked(Layer.ROAD) && isLayerLocked(Layer.FORBIDDEN)) {
                fireCanvasHint("相关图层已锁定，当前无可删除对象。");
            }
            handleSingleSelection(point, appendSelection);
            return;
        }

        handleSingleSelection(point, appendSelection);
    }

    private void resetDragState() {
        dragMode = DragMode.NONE;
        dragStartPoint = null;
        dragCurrentPoint = null;
        draggingVertexId = null;
        draggingVertexMoved = false;
        draggingPreviewWorld = null;
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
            if (selectedVertexIds.contains(id)) {
                fireVertexActivated(id);
            }
            return;
        }

        Edge edge = findEdgeAt(point);
        if (edge != null) {
            selectedEdgeKey = MapCanvasEdgeIndex.edgeKey(edge);
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
        return MapCanvasHitTester.findVertexAt(
                point,
                vertices,
                this::project,
                isLayerVisible(Layer.VERTEX),
                isLayerLocked(Layer.VERTEX),
                MOVE_HIT_THRESHOLD_PIXELS
        );
    }

    private Edge findEdgeAt(Point point) {
        return MapCanvasHitTester.findEdgeAt(
                point,
                buildRenderableEdges(),
                this::project,
                edge -> isLayerVisible(edge.isForbidden() ? Layer.FORBIDDEN : Layer.ROAD),
                edge -> isLayerLocked(edge.isForbidden() ? Layer.FORBIDDEN : Layer.ROAD),
                6.0
        );
    }

    private Edge findEdgeByKey(String key) {
        if (key == null) {
            return null;
        }
        for (Edge edge : buildRenderableEdges()) {
            if (key.equals(MapCanvasEdgeIndex.edgeKey(edge))) {
                return edge;
            }
        }
        return null;
    }

    private List<Edge> buildRenderableEdges() {
        return MapCanvasEdgeIndex.dedupeForRender(edges);
    }

    private void updateProjection() {
        if (!projectionDirty) {
            return;
        }
        clampPanOffsets();

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

    private Point2D.Double snapToWorld(Point2D.Double world, String movingVertexId, boolean previewOnly) {
        updateProjection();
        double unitToScreen = Math.max(baseScale * zoom, 0.001);
        double axisThresholdWorld = AXIS_ALIGN_PIXELS / unitToScreen;
        MapCanvasSnapEngine.Outcome outcome = MapCanvasSnapEngine.apply(
                world,
                movingVertexId,
                vertices,
                isLayerLocked(Layer.VERTEX),
                GRID_SNAP_SIZE,
                GRID_SNAP_PIXELS,
                SNAP_VERTEX_PIXELS,
                axisThresholdWorld,
                this::project,
                this::project
        );
        activeSnapGuide = (previewOnly && outcome.hasGuide())
                ? new SnapGuide(outcome.verticalGuide, outcome.horizontalGuide, outcome.guideX, outcome.guideY)
                : SnapGuide.none();
        return new Point2D.Double(outcome.x, outcome.y);
    }

    private void centerOnSegment(int segmentIndex) {
        if (currentRouteVisualization == null) {
            return;
        }
        if (segmentIndex < 0 || segmentIndex >= currentRouteVisualization.getSegmentCount()) {
            return;
        }
        RouteVisualizationDto.Segment segment = currentRouteVisualization.getSegments().get(segmentIndex);
        Point2D.Double from = projectVertexById(segment.getFromVertexId());
        Point2D.Double to = projectVertexById(segment.getToVertexId());
        if (from == null || to == null) {
            return;
        }
        double centerX = (from.x + to.x) / 2.0;
        double centerY = (from.y + to.y) / 2.0;
        double targetX = getWidth() / 2.0;
        double targetY = getHeight() / 2.0;
        panX += targetX - centerX;
        panY += targetY - centerY;
        clampPanOffsets();
        invalidateScene(true, true);
        fireViewportChanged();
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

    private void clampPanOffsets() {
        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);
        double availableWidth = Math.max(width - VIEW_PADDING * 2, 20);
        double availableHeight = Math.max(height - VIEW_PADDING * 2, 20);
        double safeWorldWidth = Math.max(worldWidth, 1);
        double safeWorldHeight = Math.max(worldHeight, 1);
        double localBaseScale = Math.min(availableWidth / safeWorldWidth, availableHeight / safeWorldHeight);
        localBaseScale = Math.max(localBaseScale, 0.01);
        double scaledWidth = safeWorldWidth * localBaseScale * zoom;
        double scaledHeight = safeWorldHeight * localBaseScale * zoom;
        double extraOffsetX = Math.max(availableWidth, scaledWidth) * PAN_OVERSCROLL_FACTOR;
        double extraOffsetY = Math.max(availableHeight, scaledHeight) * PAN_OVERSCROLL_FACTOR;
        double maxOffsetX = Math.max((availableWidth + scaledWidth) / 2.0 - PAN_VISIBLE_MIN_PIXELS + extraOffsetX, 0);
        double maxOffsetY = Math.max((availableHeight + scaledHeight) / 2.0 - PAN_VISIBLE_MIN_PIXELS + extraOffsetY, 0);
        panX = clamp(panX, -maxOffsetX, maxOffsetX);
        panY = clamp(panY, -maxOffsetY, maxOffsetY);
    }

    private int currentRouteSegmentCount() {
        return currentRouteVisualization == null ? 0 : currentRouteVisualization.getSegmentCount();
    }

    private void invalidateScene(boolean markSceneDirty, boolean markProjectionDirty) {
        if (markSceneDirty) {
            sceneCache.markDirty();
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

    private void fireAddVertexRequested(double x, double y) {
        if (listener == null) {
            return;
        }
        listener.onAddVertexRequested(x, y);
    }

    private void fireConnectVerticesRequested(String fromId, String toId) {
        if (listener == null || fromId == null || toId == null) {
            return;
        }
        listener.onConnectVerticesRequested(fromId, toId);
    }

    private void fireMoveVertexRequested(String vertexId, double x, double y) {
        if (listener == null || vertexId == null) {
            return;
        }
        listener.onMoveVertexRequested(vertexId, x, y);
    }

    private void fireDeleteVertexRequested(String vertexId) {
        if (listener == null || vertexId == null) {
            return;
        }
        listener.onDeleteVertexRequested(vertexId);
    }

    private void fireDeleteEdgeRequested(String edgeKey) {
        if (listener == null || edgeKey == null) {
            return;
        }
        listener.onDeleteEdgeRequested(edgeKey);
    }

    private void fireEdgeDraftChanged(String startVertexId) {
        if (listener == null) {
            return;
        }
        listener.onEdgeDraftChanged(startVertexId);
    }

    private void fireVertexActivated(String vertexId) {
        if (listener == null || vertexId == null) {
            return;
        }
        listener.onVertexActivated(vertexId);
    }

    private void fireViewportChanged() {
        if (listener == null) {
            return;
        }
        listener.onViewportChanged(zoom, panX, panY);
    }

    private void fireCanvasHint(String message) {
        if (listener == null || message == null || message.trim().isEmpty()) {
            return;
        }
        listener.onCanvasHint(message.trim());
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

    private static Color withOpacity(Color color, float opacity) {
        if (color == null) {
            return null;
        }
        int alpha = (int) Math.round(clamp(opacity, 0, 1) * color.getAlpha());
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
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

    private static final class SnapGuide {
        private static final SnapGuide EMPTY = new SnapGuide(false, false, 0, 0);
        private final boolean vertical;
        private final boolean horizontal;
        private final double worldX;
        private final double worldY;

        private SnapGuide(boolean vertical, boolean horizontal, double worldX, double worldY) {
            this.vertical = vertical;
            this.horizontal = horizontal;
            this.worldX = worldX;
            this.worldY = worldY;
        }

        private boolean isEmpty() {
            return !vertical && !horizontal;
        }

        private static SnapGuide none() {
            return EMPTY;
        }
    }

    public interface Listener {
        void onSelectionChanged(List<String> selectedVertexIds, String selectedEdgeKey);

        void onViewportChanged(double zoom, double panX, double panY);

        void onVertexActivated(String vertexId);

        void onAddVertexRequested(double x, double y);

        void onConnectVerticesRequested(String fromId, String toId);

        void onMoveVertexRequested(String vertexId, double x, double y);

        void onDeleteVertexRequested(String vertexId);

        void onDeleteEdgeRequested(String edgeKey);

        void onEdgeDraftChanged(String startVertexId);

        default void onCanvasHint(String message) {
        }
    }
}
