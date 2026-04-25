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


final class MapCanvasInputHandler {
    private final MapCanvas canvas;

    MapCanvasInputHandler(MapCanvas canvas) {
        this.canvas = canvas;
    }

    void installKeyboardShortcuts() {
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed SPACE"), "space-pan-on");
        canvas.getActionMap().put("space-pan-on", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!canvas.spacePanMode) {
                    canvas.spacePanMode = true;
                    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }
        });
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released SPACE"), "space-pan-off");
        canvas.getActionMap().put("space-pan-off", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                canvas.spacePanMode = false;
                canvas.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    void installInteractions() {
        MouseAdapter mouseHandler = createMouseHandler();
        canvas.addMouseListener(mouseHandler);
        canvas.addMouseMotionListener(mouseHandler);
        canvas.addMouseWheelListener(createMouseWheelHandler());
    }

    MouseAdapter createMouseHandler() {
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

    MouseWheelListener createMouseWheelHandler() {
        return new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                double oldZoom = canvas.zoom;
                double factor = event.getWheelRotation() < 0 ? MapCanvas.ZOOM_STEP : (1.0 / MapCanvas.ZOOM_STEP);
                double nextZoom = MapCanvas.clamp(oldZoom * factor, MapCanvas.MIN_ZOOM, MapCanvas.MAX_ZOOM);
                if (Math.abs(nextZoom - oldZoom) < 1e-6) {
                    return;
                }

                canvas.updateProjection();
                Point2D.Double world = canvas.toWorld(event.getPoint());
                canvas.zoom = nextZoom;
                canvas.invalidateScene(true, true);
                canvas.updateProjection();

                Point2D.Double projected = canvas.project(world);
                if (projected != null) {
                    canvas.panX += event.getX() - projected.x;
                    canvas.panY += event.getY() - projected.y;
                    canvas.clampPanOffsets();
                    canvas.invalidateScene(true, true);
                }

                canvas.repaint();
                canvas.fireViewportChanged();
            }
        };
    }

    void handleMousePressed(MouseEvent event) {
        canvas.requestFocusInWindow();
        canvas.dragStartPoint = event.getPoint();
        canvas.dragCurrentPoint = event.getPoint();
        canvas.activeSnapGuide = MapCanvas.SnapGuide.none();
        canvas.draggingPreviewWorld = null;

        if (SwingUtilities.isMiddleMouseButton(event) || SwingUtilities.isRightMouseButton(event)) {
            canvas.dragMode = MapCanvas.DragMode.PAN;
            return;
        }

        if (!SwingUtilities.isLeftMouseButton(event)) {
            return;
        }

        if (canvas.spacePanMode) {
            canvas.dragMode = MapCanvas.DragMode.PAN;
            return;
        }

        if (canvas.editToolMode == EditToolMode.MOVE_VERTEX) {
            Vertex vertex = findVertexAt(event.getPoint());
            if (vertex != null) {
                canvas.draggingVertexId = vertex.getId();
                canvas.draggingVertexMoved = false;
                canvas.draggingPreviewWorld = new Point2D.Double(vertex.getX(), vertex.getY());
                canvas.dragMode = MapCanvas.DragMode.MOVE_VERTEX;
                return;
            }
        }

        if (canvas.editToolMode == EditToolMode.SELECT) {
            canvas.dragMode = MapCanvas.DragMode.BOX_SELECT;
            canvas.selectionRect = new Rectangle(event.getX(), event.getY(), 0, 0);
            canvas.repaint(canvas.selectionRect);
            return;
        }

        canvas.dragMode = MapCanvas.DragMode.NONE;
    }

    void handleMouseDragged(MouseEvent event) {
        if (canvas.dragStartPoint == null) {
            return;
        }
        if (canvas.dragMode == MapCanvas.DragMode.PAN) {
            handlePanDrag(event);
            return;
        }
        if (canvas.dragMode == MapCanvas.DragMode.BOX_SELECT) {
            handleBoxSelectDrag(event);
            return;
        }
        if (canvas.dragMode == MapCanvas.DragMode.MOVE_VERTEX) {
            handleMoveVertexDrag(event);
        }
    }

    void handleMouseReleased(MouseEvent event) {
        if (canvas.dragMode == MapCanvas.DragMode.PAN) {
            canvas.activeSnapGuide = MapCanvas.SnapGuide.none();
            resetDragState();
            return;
        }

        if (!SwingUtilities.isLeftMouseButton(event)) {
            resetDragState();
            return;
        }

        if (canvas.dragMode == MapCanvas.DragMode.MOVE_VERTEX) {
            if (canvas.draggingVertexId != null && (canvas.draggingVertexMoved || event.getPoint().distance(canvas.dragStartPoint) > 2.0)) {
                Point2D.Double target = canvas.draggingPreviewWorld != null ? canvas.draggingPreviewWorld : canvas.toWorld(event.getPoint());
                Point2D.Double snappedTarget = canvas.snapToWorld(target, canvas.draggingVertexId, false);
                canvas.fireMoveVertexRequested(canvas.draggingVertexId, snappedTarget.x, snappedTarget.y);
            } else if (canvas.draggingVertexId != null) {
                handleSingleSelection(event.getPoint(), event.isControlDown());
            }
            canvas.activeSnapGuide = MapCanvas.SnapGuide.none();
            resetDragState();
            return;
        }

        if (canvas.editToolMode == EditToolMode.SELECT && canvas.dragMode == MapCanvas.DragMode.BOX_SELECT) {
            Rectangle finalRect = canvas.selectionRect == null ? MapCanvas.createRect(canvas.dragStartPoint, event.getPoint()) : canvas.selectionRect;
            if (finalRect.width < 4 && finalRect.height < 4) {
                handleSingleSelection(event.getPoint(), event.isControlDown());
            } else {
                handleBoxSelection(finalRect, event.isControlDown());
            }

            Rectangle dirty = canvas.selectionRect == null ? finalRect : MapCanvas.union(canvas.selectionRect, finalRect);
            canvas.selectionRect = null;
            if (dirty != null) {
                canvas.repaint(dirty.x - 4, dirty.y - 4, dirty.width + 8, dirty.height + 8);
            } else {
                canvas.repaint();
            }
            resetDragState();
            return;
        }

        handleEditToolClick(event.getPoint(), event.isControlDown());
        canvas.activeSnapGuide = MapCanvas.SnapGuide.none();
        resetDragState();
    }

    void handlePanDrag(MouseEvent event) {
        int dx = event.getX() - canvas.dragCurrentPoint.x;
        int dy = event.getY() - canvas.dragCurrentPoint.y;
        canvas.panX += dx;
        canvas.panY += dy;
        canvas.clampPanOffsets();
        canvas.dragCurrentPoint = event.getPoint();
        canvas.invalidateScene(true, true);
        canvas.repaint();
        canvas.fireViewportChanged();
    }

    void handleBoxSelectDrag(MouseEvent event) {
        Rectangle oldRect = canvas.selectionRect == null ? null : new Rectangle(canvas.selectionRect);
        canvas.selectionRect = MapCanvas.createRect(canvas.dragStartPoint, event.getPoint());
        canvas.dragCurrentPoint = event.getPoint();
        canvas.repaintUnion(oldRect, canvas.selectionRect);
    }

    void handleMoveVertexDrag(MouseEvent event) {
        if (canvas.draggingVertexId == null) {
            return;
        }
        if (!canvas.draggingVertexMoved) {
            canvas.draggingVertexMoved = event.getPoint().distance(canvas.dragStartPoint) > 2.0;
        }
        Point2D.Double world = canvas.toWorld(event.getPoint());
        canvas.draggingPreviewWorld = canvas.snapToWorld(world, canvas.draggingVertexId, true);
        canvas.repaint();
    }

    void handleEditToolClick(Point point, boolean appendSelection) {
        Vertex vertex = findVertexAt(point);
        Edge edge = findEdgeAt(point);

        if (canvas.editToolMode == EditToolMode.ADD_VERTEX) {
            if (canvas.isLayerLocked(MapCanvas.Layer.VERTEX)) {
                canvas.fireCanvasHint("点位层已锁定，无法新增点位。");
                return;
            }
            if (vertex == null) {
                Point2D.Double world = canvas.toWorld(point);
                canvas.fireAddVertexRequested(world.x, world.y);
                return;
            }
            handleSingleSelection(point, appendSelection);
            return;
        }

        if (canvas.editToolMode == EditToolMode.ADD_EDGE) {
            if (canvas.isLayerLocked(MapCanvas.Layer.VERTEX) || canvas.isLayerLocked(MapCanvas.Layer.ROAD)) {
                canvas.fireCanvasHint("点位层或道路层已锁定，无法新建连线。");
                return;
            }
            if (vertex == null) {
                handleSingleSelection(point, appendSelection);
                return;
            }
            String vertexId = vertex.getId();
            handleSingleSelection(point, appendSelection);
            if (canvas.pendingEdgeStartVertexId == null) {
                canvas.pendingEdgeStartVertexId = vertexId;
                canvas.fireEdgeDraftChanged(canvas.pendingEdgeStartVertexId);
                return;
            }
            if (!canvas.pendingEdgeStartVertexId.equals(vertexId)) {
                canvas.fireConnectVerticesRequested(canvas.pendingEdgeStartVertexId, vertexId);
            }
            canvas.pendingEdgeStartVertexId = null;
            canvas.fireEdgeDraftChanged(null);
            return;
        }

        if (canvas.editToolMode == EditToolMode.DELETE_OBJECT) {
            if (vertex != null) {
                canvas.fireDeleteVertexRequested(vertex.getId());
                return;
            }
            if (edge != null) {
                canvas.fireDeleteEdgeRequested(MapCanvasEdgeIndex.edgeKey(edge));
                return;
            }
            if (canvas.isLayerLocked(MapCanvas.Layer.VERTEX) && canvas.isLayerLocked(MapCanvas.Layer.ROAD) && canvas.isLayerLocked(MapCanvas.Layer.FORBIDDEN)) {
                canvas.fireCanvasHint("相关图层已锁定，当前无可删除对象。");
            }
            handleSingleSelection(point, appendSelection);
            return;
        }

        handleSingleSelection(point, appendSelection);
    }

    void resetDragState() {
        canvas.dragMode = MapCanvas.DragMode.NONE;
        canvas.dragStartPoint = null;
        canvas.dragCurrentPoint = null;
        canvas.draggingVertexId = null;
        canvas.draggingVertexMoved = false;
        canvas.draggingPreviewWorld = null;
    }

    void handleSingleSelection(Point point, boolean appendSelection) {
        String previousEdgeKey = canvas.selectedEdgeKey;
        Set<String> before = new LinkedHashSet<String>(canvas.selectedVertexIds);

        Vertex vertex = findVertexAt(point);
        if (vertex != null) {
            String id = vertex.getId();
            if (!appendSelection) {
                canvas.selectedVertexIds.clear();
            }
            if (appendSelection && canvas.selectedVertexIds.contains(id)) {
                canvas.selectedVertexIds.remove(id);
            } else {
                canvas.selectedVertexIds.add(id);
            }
            canvas.selectedEdgeKey = null;
            canvas.repaintSelectionDelta(before, canvas.selectedVertexIds);
            canvas.fireSelectionChanged();
            if (canvas.selectedVertexIds.contains(id)) {
                canvas.fireVertexActivated(id);
            }
            return;
        }

        Edge edge = findEdgeAt(point);
        if (edge != null) {
            canvas.selectedEdgeKey = MapCanvasEdgeIndex.edgeKey(edge);
            if (!appendSelection) {
                canvas.selectedVertexIds.clear();
            }
            canvas.repaint();
            canvas.fireSelectionChanged();
            return;
        }

        if (!appendSelection) {
            canvas.selectedVertexIds.clear();
            canvas.selectedEdgeKey = null;
            if (!before.isEmpty() || previousEdgeKey != null) {
                canvas.repaint();
                canvas.fireSelectionChanged();
            }
        }
    }

    void handleBoxSelection(Rectangle rect, boolean appendSelection) {
        Set<String> before = new LinkedHashSet<String>(canvas.selectedVertexIds);
        if (!appendSelection) {
            canvas.selectedVertexIds.clear();
        }

        for (Vertex vertex : canvas.vertices) {
            Point2D.Double point = canvas.project(vertex);
            if (point == null) {
                continue;
            }
            if (rect.contains(point)) {
                canvas.selectedVertexIds.add(vertex.getId());
            }
        }

        canvas.selectedEdgeKey = null;
        canvas.repaintSelectionDelta(before, canvas.selectedVertexIds);
        canvas.fireSelectionChanged();
    }

    Vertex findVertexAt(Point point) {
        return MapCanvasHitTester.findVertexAt(
                point,
                canvas.vertices,
                canvas::project,
                canvas.isLayerVisible(MapCanvas.Layer.VERTEX),
                canvas.isLayerLocked(MapCanvas.Layer.VERTEX),
                MapCanvas.MOVE_HIT_THRESHOLD_PIXELS
        );
    }

    Edge findEdgeAt(Point point) {
        return MapCanvasHitTester.findEdgeAt(
                point,
                buildRenderableEdges(),
                canvas::project,
                edge -> canvas.isLayerVisible(edge.isForbidden() ? MapCanvas.Layer.FORBIDDEN : MapCanvas.Layer.ROAD),
                edge -> canvas.isLayerLocked(edge.isForbidden() ? MapCanvas.Layer.FORBIDDEN : MapCanvas.Layer.ROAD),
                6.0
        );
    }

    Edge findEdgeByKey(String key) {
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

    List<Edge> buildRenderableEdges() {
        return MapCanvasEdgeIndex.dedupeForRender(canvas.edges);
    }
}
