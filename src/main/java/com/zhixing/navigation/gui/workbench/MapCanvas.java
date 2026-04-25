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
import java.awt.GraphicsConfiguration;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
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
    static final double MIN_ZOOM = 0.50;
    static final double MAX_ZOOM = 4.0;
    static final double ZOOM_STEP = 1.12;
    static final int VIEW_PADDING = 40;
    static final int WORLD_PADDING = 20;
    static final double GRID_SNAP_SIZE = 20.0;
    static final int GRID_SNAP_PIXELS = 10;
    static final int SNAP_VERTEX_PIXELS = 14;
    static final int AXIS_ALIGN_PIXELS = 10;
    static final double MOVE_HIT_THRESHOLD_PIXELS = 20.0;
    static final int PAN_VISIBLE_MIN_PIXELS = 36;
    static final double PAN_OVERSCROLL_FACTOR = 0.35;
    static final int FLASH_CYCLE_LIMIT = 8;
    static final int FLASH_INTERVAL_MS = 180;
    final List<Vertex> vertices;
    final List<Edge> edges;
    final Map<String, Vertex> vertexIndex;
    final Map<String, Point2D.Double> projectedVertexCache;
    final Set<String> selectedVertexIds;
    final Map<Layer, Boolean> layerVisibility;
    final Map<Layer, Boolean> layerLocked;
    final Map<Layer, Float> layerOpacity;
    final List<Layer> renderOrder;
    final MapSceneCache sceneCache;
    final MapCanvasBaseSceneRenderer baseSceneRenderer;
    final MapCanvasRouteRenderer routeRenderer;
    final MapCanvasOverlayRenderer overlayRenderer;
    final Timer routeFlashTimer;
boolean projectionDirty; double worldMinX;
    double worldMaxX;
    double worldMinY;
    double worldMaxY;
    double worldWidth;
    double worldHeight;
    double zoom;
    double panX;
    double panY;
    double baseScale;
    double viewOriginX;
    double viewOriginY;
    Point dragStartPoint;
    Point dragCurrentPoint;
    DragMode dragMode;
    Rectangle selectionRect;
    String selectedEdgeKey;
    int focusedRouteSegmentIndex;
    boolean focusedSegmentVisible;
    int focusedSegmentFlashCycle;
    EditToolMode editToolMode;
    String pendingEdgeStartVertexId;
    String draggingVertexId;
    boolean draggingVertexMoved;
    Point2D.Double draggingPreviewWorld;
    boolean spacePanMode;
    SnapGuide activeSnapGuide;
    RouteVisualizationDto currentRouteVisualization;
    RouteVisualizationDto previousRouteVisualization;
    BufferedImage referenceImage;
    double referenceImageScale;
    double metersPerWorldUnit;
    Listener listener;
    final MapCanvasDataManager dataManager;
    final MapCanvasLayerManager layerManager;
    final MapCanvasReferenceManager referenceManager;
    final MapCanvasRenderCoordinator renderCoordinator;
    final MapCanvasInputHandler inputHandler;
    final MapCanvasZoomManager zoomManager;
    enum DragMode {
        NONE,
        PAN,
        BOX_SELECT,
        MOVE_VERTEX
    }
    public MapCanvas() {
        setBackground(new Color(243, 247, 251));
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
        this.dataManager = new MapCanvasDataManager(this);
        this.layerManager = new MapCanvasLayerManager(this);
        this.referenceManager = new MapCanvasReferenceManager(this);
        this.renderCoordinator = new MapCanvasRenderCoordinator(this);
        this.inputHandler = new MapCanvasInputHandler(this);
        this.zoomManager = new MapCanvasZoomManager(this);
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
    public void setGraphData(Collection<Vertex> newVertices, Collection<Edge> newEdges) { dataManager.setGraphData(newVertices, newEdges); }
    public void setRouteComparison(RouteVisualizationDto currentRoute, RouteVisualizationDto previousRoute) { dataManager.setRouteComparison(currentRoute, previousRoute); }
    public void clearRouteComparison() { dataManager.clearRouteComparison(); }
    public void focusRouteSegment(int segmentIndex) { dataManager.focusRouteSegment(segmentIndex); }
    public EditToolMode editToolMode() { return dataManager.editToolMode(); }
    public void setEditToolMode(EditToolMode mode) { dataManager.setEditToolMode(mode); }
    public List<String> selectedVertexIds() { return dataManager.selectedVertexIds(); }
    public String selectedEdgeKey() { return dataManager.selectedEdgeKey(); }
    public void setLayerVisible(Layer layer, boolean visible) { layerManager.setLayerVisible(layer, visible); }
    public boolean isLayerVisible(Layer layer) { return layerManager.isLayerVisible(layer); }
    public void setLayerLocked(Layer layer, boolean locked) { layerManager.setLayerLocked(layer, locked); }
    public boolean isLayerLocked(Layer layer) { return layerManager.isLayerLocked(layer); }
    public void setLayerOpacity(Layer layer, float opacity) { layerManager.setLayerOpacity(layer, opacity); }
    public float getLayerOpacity(Layer layer) { return layerManager.getLayerOpacity(layer); }
    public List<Layer> getRenderOrder() { return layerManager.getRenderOrder(); }
    public void setRenderOrder(List<Layer> order) { layerManager.setRenderOrder(order); }
    public void resetViewport() { zoomManager.resetViewport(); }
    public void setListener(Listener listener) {
        this.listener = listener;
    }
    public void setReferenceImage(BufferedImage image) { referenceManager.setReferenceImage(image); }
    public boolean hasReferenceImage() { return referenceManager.hasReferenceImage(); }
    public void clearReferenceImage() { referenceManager.clearReferenceImage(); }
    public void setReferenceImageScale(double scale) { referenceManager.setReferenceImageScale(scale); }
    public double getReferenceImageScale() { return referenceManager.getReferenceImageScale(); }
    public void setMetersPerWorldUnit(double metersPerWorldUnit) { referenceManager.setMetersPerWorldUnit(metersPerWorldUnit); }
    public double getMetersPerWorldUnit() { return referenceManager.getMetersPerWorldUnit(); }
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            applyQualityRenderingHints(g2);
            double deviceScaleX = resolveDeviceScale(g2, true);
            double deviceScaleY = resolveDeviceScale(g2, false);
            ensureSceneCache(deviceScaleX, deviceScaleY);
            if (sceneCache.image() != null) {
                g2.drawImage(sceneCache.image(), 0, 0, getWidth(), getHeight(), null);
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
    void ensureSceneCache(double deviceScaleX, double deviceScaleY) { renderCoordinator.ensureSceneCache(deviceScaleX, deviceScaleY); }
    void applyQualityRenderingHints(Graphics2D g2) { renderCoordinator.applyQualityRenderingHints(g2); }
    double resolveDeviceScale(Graphics2D g2, boolean horizontal) { return renderCoordinator.resolveDeviceScale(g2, horizontal); }
    static boolean isUsableScale(double scale) {
        return !Double.isNaN(scale) && !Double.isInfinite(scale) && scale > 0.0;
    }
    void renderScene(Graphics2D g2) { renderCoordinator.renderScene(g2); }
    Point2D.Double projectVertexById(String vertexId) { return renderCoordinator.projectVertexById(vertexId); }
    void installKeyboardShortcuts() { inputHandler.installKeyboardShortcuts(); }
    void installInteractions() { inputHandler.installInteractions(); }
    MouseAdapter createMouseHandler() { return inputHandler.createMouseHandler(); }
    MouseWheelListener createMouseWheelHandler() { return inputHandler.createMouseWheelHandler(); }
    void handleMousePressed(MouseEvent event) { inputHandler.handleMousePressed(event); }
    void handleMouseDragged(MouseEvent event) { inputHandler.handleMouseDragged(event); }
    void handleMouseReleased(MouseEvent event) { inputHandler.handleMouseReleased(event); }
    void handlePanDrag(MouseEvent event) { inputHandler.handlePanDrag(event); }
    void handleBoxSelectDrag(MouseEvent event) { inputHandler.handleBoxSelectDrag(event); }
    void handleMoveVertexDrag(MouseEvent event) { inputHandler.handleMoveVertexDrag(event); }
    void handleEditToolClick(Point point, boolean appendSelection) { inputHandler.handleEditToolClick(point, appendSelection); }
    void resetDragState() { inputHandler.resetDragState(); }
    void handleSingleSelection(Point point, boolean appendSelection) { inputHandler.handleSingleSelection(point, appendSelection); }
    void handleBoxSelection(Rectangle rect, boolean appendSelection) { inputHandler.handleBoxSelection(rect, appendSelection); }
    Vertex findVertexAt(Point point) { return inputHandler.findVertexAt(point); }
    Edge findEdgeAt(Point point) { return inputHandler.findEdgeAt(point); }
    Edge findEdgeByKey(String key) { return inputHandler.findEdgeByKey(key); }
    List<Edge> buildRenderableEdges() { return inputHandler.buildRenderableEdges(); }
    void updateProjection() { zoomManager.updateProjection(); }
    Point2D.Double project(Vertex vertex) { return zoomManager.project(vertex); }
    Point2D.Double project(Point2D.Double world) { return zoomManager.project(world); }
    Point2D.Double toWorld(Point point) { return zoomManager.toWorld(point); }
    Point2D.Double snapToWorld(Point2D.Double world, String movingVertexId, boolean previewOnly) { return zoomManager.snapToWorld(world, movingVertexId, previewOnly); }
    void centerOnSegment(int segmentIndex) { zoomManager.centerOnSegment(segmentIndex); }
    void recomputeWorldBounds() { zoomManager.recomputeWorldBounds(); }
    void clampPanOffsets() { zoomManager.clampPanOffsets(); }
    int currentRouteSegmentCount() { return zoomManager.currentRouteSegmentCount(); }
    void invalidateScene(boolean markSceneDirty, boolean markProjectionDirty) { zoomManager.invalidateScene(markSceneDirty, markProjectionDirty); }
    void repaintSelectionDelta(Set<String> before, Set<String> after) {
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
    static Rectangle aroundPoint(Point2D.Double point, int radius) {
        return new Rectangle((int) Math.round(point.x) - radius, (int) Math.round(point.y) - radius, radius * 2 + 1, radius * 2 + 1);
    }
    static Rectangle createRect(Point start, Point end) {
        int left = Math.min(start.x, end.x);
        int top = Math.min(start.y, end.y);
        int width = Math.abs(start.x - end.x);
        int height = Math.abs(start.y - end.y);
        return new Rectangle(left, top, width, height);
    }
    static Rectangle union(Rectangle a, Rectangle b) {
        if (a == null) {
            return b == null ? null : new Rectangle(b);
        }
        if (b == null) {
            return new Rectangle(a);
        }
        return a.union(b);
    }
    void repaintUnion(Rectangle oldRect, Rectangle newRect) {
        Rectangle dirty = union(oldRect, newRect);
        if (dirty == null) {
            repaint();
            return;
        }
        repaint(dirty.x - 4, dirty.y - 4, dirty.width + 8, dirty.height + 8);
    }
    void fireSelectionChanged() {
        if (listener == null) {
            return;
        }
        List<String> selected = new ArrayList<String>(selectedVertexIds);
        Collections.sort(selected);
        listener.onSelectionChanged(selected, selectedEdgeKey);
    }
    void fireAddVertexRequested(double x, double y) {
        if (listener == null) {
            return;
        }
        listener.onAddVertexRequested(x, y);
    }
    void fireConnectVerticesRequested(String fromId, String toId) {
        if (listener == null || fromId == null || toId == null) {
            return;
        }
        listener.onConnectVerticesRequested(fromId, toId);
    }
    void fireMoveVertexRequested(String vertexId, double x, double y) {
        if (listener == null || vertexId == null) {
            return;
        }
        listener.onMoveVertexRequested(vertexId, x, y);
    }
    void fireDeleteVertexRequested(String vertexId) {
        if (listener == null || vertexId == null) {
            return;
        }
        listener.onDeleteVertexRequested(vertexId);
    }
    void fireDeleteEdgeRequested(String edgeKey) {
        if (listener == null || edgeKey == null) {
            return;
        }
        listener.onDeleteEdgeRequested(edgeKey);
    }
    void fireEdgeDraftChanged(String startVertexId) {
        if (listener == null) {
            return;
        }
        listener.onEdgeDraftChanged(startVertexId);
    }
    void fireVertexActivated(String vertexId) {
        if (listener == null || vertexId == null) {
            return;
        }
        listener.onVertexActivated(vertexId);
    }
    void fireViewportChanged() {
        if (listener == null) {
            return;
        }
        listener.onViewportChanged(zoom, panX, panY);
    }
    void fireCanvasHint(String message) {
        if (listener == null || message == null || message.trim().isEmpty()) {
            return;
        }
        listener.onCanvasHint(message.trim());
    }
    static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
    static Color withOpacity(Color color, float opacity) {
        if (color == null) {
            return null;
        }
        int alpha = (int) Math.round(clamp(opacity, 0, 1) * color.getAlpha());
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
    static Color roadColor(RoadType roadType) {
        if (roadType == RoadType.MAIN_ROAD) {
            return new Color(46, 95, 165);
        }
        if (roadType == RoadType.STAIRS) {
            return new Color(115, 94, 164);
        }
        return new Color(128, 140, 154);
    }
    static float roadWidth(RoadType roadType) {
        if (roadType == RoadType.MAIN_ROAD) {
            return 4.8f;
        }
        if (roadType == RoadType.STAIRS) {
            return 3.4f;
        }
        return 2.9f;
    }
    static Color placeColor(PlaceType placeType) {
        if (placeType == PlaceType.GATE) {
            return new Color(40, 118, 209);
        }
        if (placeType == PlaceType.LIBRARY) {
            return new Color(31, 154, 111);
        }
        if (placeType == PlaceType.CANTEEN) {
            return new Color(219, 150, 53);
        }
        if (placeType == PlaceType.TEACHING_BUILDING) {
            return new Color(84, 104, 207);
        }
        if (placeType == PlaceType.DORMITORY) {
            return new Color(49, 150, 178);
        }
        if (placeType == PlaceType.OFFICE) {
            return new Color(204, 104, 88);
        }
        if (placeType == PlaceType.SPORTS_CENTER) {
            return new Color(66, 169, 99);
        }
        return new Color(116, 128, 144);
    }
    static final class SnapGuide {
        static final SnapGuide EMPTY = new SnapGuide(false, false, 0, 0);
        final boolean vertical;
        final boolean horizontal;
        final double worldX;
        final double worldY;
        SnapGuide(boolean vertical, boolean horizontal, double worldX, double worldY) {
            this.vertical = vertical;
            this.horizontal = horizontal;
            this.worldX = worldX;
            this.worldY = worldY;
        }
        boolean isEmpty() {
            return !vertical && !horizontal;
        }
        static SnapGuide none() {
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
