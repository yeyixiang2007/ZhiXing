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


final class MapCanvasDataManager {
    private final MapCanvas canvas;

    MapCanvasDataManager(MapCanvas canvas) {
        this.canvas = canvas;
    }

    public void setGraphData(Collection<Vertex> newVertices, Collection<Edge> newEdges) {
        canvas.vertices.clear();
        canvas.edges.clear();
        canvas.vertexIndex.clear();

        if (newVertices != null) {
            for (Vertex vertex : newVertices) {
                if (vertex == null) {
                    continue;
                }
                canvas.vertices.add(vertex);
                canvas.vertexIndex.put(vertex.getId(), vertex);
            }
        }
        if (newEdges != null) {
            for (Edge edge : newEdges) {
                if (edge == null) {
                    continue;
                }
                canvas.edges.add(edge);
            }
        }
        if (canvas.focusedRouteSegmentIndex >= canvas.currentRouteSegmentCount()) {
            canvas.focusedRouteSegmentIndex = -1;
            canvas.focusedSegmentVisible = true;
            canvas.focusedSegmentFlashCycle = 0;
            canvas.routeFlashTimer.stop();
        }

        boolean selectionChanged = canvas.selectedVertexIds.retainAll(canvas.vertexIndex.keySet());
        if (canvas.selectedEdgeKey != null && canvas.findEdgeByKey(canvas.selectedEdgeKey) == null) {
            canvas.selectedEdgeKey = null;
            selectionChanged = true;
        }
        if (canvas.pendingEdgeStartVertexId != null && !canvas.vertexIndex.containsKey(canvas.pendingEdgeStartVertexId)) {
            canvas.pendingEdgeStartVertexId = null;
            canvas.fireEdgeDraftChanged(null);
        }
        if (canvas.draggingVertexId != null && !canvas.vertexIndex.containsKey(canvas.draggingVertexId)) {
            canvas.draggingVertexId = null;
            canvas.draggingPreviewWorld = null;
            canvas.draggingVertexMoved = false;
            canvas.dragMode = MapCanvas.DragMode.NONE;
        }
        if (selectionChanged) {
            canvas.fireSelectionChanged();
        }

        canvas.recomputeWorldBounds();
        canvas.clampPanOffsets();
        canvas.invalidateScene(true, true);
        canvas.repaint();
    }

    public void setRouteComparison(RouteVisualizationDto currentRoute, RouteVisualizationDto previousRoute) {
        canvas.currentRouteVisualization = currentRoute;
        canvas.previousRouteVisualization = previousRoute;
        canvas.focusedRouteSegmentIndex = -1;
        canvas.focusedSegmentVisible = true;
        canvas.focusedSegmentFlashCycle = 0;
        canvas.routeFlashTimer.stop();
        canvas.repaint();
    }

    public void clearRouteComparison() {
        canvas.currentRouteVisualization = null;
        canvas.previousRouteVisualization = null;
        canvas.focusedRouteSegmentIndex = -1;
        canvas.focusedSegmentVisible = true;
        canvas.focusedSegmentFlashCycle = 0;
        canvas.routeFlashTimer.stop();
        canvas.repaint();
    }

    public void focusRouteSegment(int segmentIndex) {
        if (segmentIndex < 0 || segmentIndex >= canvas.currentRouteSegmentCount()) {
            canvas.focusedRouteSegmentIndex = -1;
            canvas.focusedSegmentVisible = true;
            canvas.focusedSegmentFlashCycle = 0;
            canvas.routeFlashTimer.stop();
            canvas.repaint();
            return;
        }
        canvas.focusedRouteSegmentIndex = segmentIndex;
        canvas.focusedSegmentVisible = true;
        canvas.focusedSegmentFlashCycle = 0;
        canvas.routeFlashTimer.restart();
        canvas.centerOnSegment(segmentIndex);
        canvas.repaint();
    }

    public EditToolMode editToolMode() {
        return canvas.editToolMode;
    }

    public void setEditToolMode(EditToolMode mode) {
        EditToolMode target = mode == null ? EditToolMode.SELECT : mode;
        if (canvas.editToolMode == target) {
            return;
        }
        canvas.editToolMode = target;
        canvas.pendingEdgeStartVertexId = null;
        canvas.draggingVertexId = null;
        canvas.draggingVertexMoved = false;
        canvas.draggingPreviewWorld = null;
        canvas.dragMode = MapCanvas.DragMode.NONE;
        canvas.selectionRect = null;
        canvas.fireEdgeDraftChanged(null);
        canvas.repaint();
    }

    public List<String> selectedVertexIds() {
        List<String> ids = new ArrayList<String>(canvas.selectedVertexIds);
        Collections.sort(ids);
        return ids;
    }

    public String selectedEdgeKey() {
        return canvas.selectedEdgeKey;
    }
}
