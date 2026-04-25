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


final class MapCanvasZoomManager {
    private final MapCanvas canvas;

    MapCanvasZoomManager(MapCanvas canvas) {
        this.canvas = canvas;
    }

    public void resetViewport() {
        canvas.zoom = 1.0;
        canvas.panX = 0;
        canvas.panY = 0;
        canvas.draggingPreviewWorld = null;
        canvas.activeSnapGuide = MapCanvas.SnapGuide.none();
        clampPanOffsets();
        invalidateScene(true, true);
        canvas.repaint();
        canvas.fireViewportChanged();
    }

    void updateProjection() {
        if (!canvas.projectionDirty) {
            return;
        }
        clampPanOffsets();

        int width = Math.max(canvas.getWidth(), 1);
        int height = Math.max(canvas.getHeight(), 1);

        double availableWidth = Math.max(width - MapCanvas.VIEW_PADDING * 2, 20);
        double availableHeight = Math.max(height - MapCanvas.VIEW_PADDING * 2, 20);

        double safeWorldWidth = Math.max(canvas.worldWidth, 1);
        double safeWorldHeight = Math.max(canvas.worldHeight, 1);

        canvas.baseScale = Math.min(availableWidth / safeWorldWidth, availableHeight / safeWorldHeight);
        canvas.baseScale = Math.max(canvas.baseScale, 0.01);

        double scaledWidth = safeWorldWidth * canvas.baseScale * canvas.zoom;
        double scaledHeight = safeWorldHeight * canvas.baseScale * canvas.zoom;

        canvas.viewOriginX = MapCanvas.VIEW_PADDING + (availableWidth - scaledWidth) / 2.0 + canvas.panX;
        canvas.viewOriginY = MapCanvas.VIEW_PADDING + (availableHeight - scaledHeight) / 2.0 + canvas.panY;

        canvas.projectedVertexCache.clear();
        canvas.projectionDirty = false;
    }

    Point2D.Double project(Vertex vertex) {
        if (vertex == null) {
            return null;
        }
        Point2D.Double cached = canvas.projectedVertexCache.get(vertex.getId());
        if (cached != null) {
            return cached;
        }

        updateProjection();
        double x = canvas.viewOriginX + (vertex.getX() - canvas.worldMinX) * canvas.baseScale * canvas.zoom;
        double y = canvas.viewOriginY + (vertex.getY() - canvas.worldMinY) * canvas.baseScale * canvas.zoom;
        Point2D.Double projected = new Point2D.Double(x, y);
        canvas.projectedVertexCache.put(vertex.getId(), projected);
        return projected;
    }

    Point2D.Double project(Point2D.Double world) {
        if (world == null) {
            return null;
        }
        updateProjection();
        double x = canvas.viewOriginX + (world.x - canvas.worldMinX) * canvas.baseScale * canvas.zoom;
        double y = canvas.viewOriginY + (world.y - canvas.worldMinY) * canvas.baseScale * canvas.zoom;
        return new Point2D.Double(x, y);
    }

    Point2D.Double toWorld(Point point) {
        updateProjection();
        double worldX = ((point.getX() - canvas.viewOriginX) / (canvas.baseScale * canvas.zoom)) + canvas.worldMinX;
        double worldY = ((point.getY() - canvas.viewOriginY) / (canvas.baseScale * canvas.zoom)) + canvas.worldMinY;
        return new Point2D.Double(worldX, worldY);
    }

    Point2D.Double snapToWorld(Point2D.Double world, String movingVertexId, boolean previewOnly) {
        updateProjection();
        double unitToScreen = Math.max(canvas.baseScale * canvas.zoom, 0.001);
        double axisThresholdWorld = MapCanvas.AXIS_ALIGN_PIXELS / unitToScreen;
        MapCanvasSnapEngine.Outcome outcome = MapCanvasSnapEngine.apply(
                world,
                movingVertexId,
                canvas.vertices,
                canvas.isLayerLocked(MapCanvas.Layer.VERTEX),
                MapCanvas.GRID_SNAP_SIZE,
                MapCanvas.GRID_SNAP_PIXELS,
                MapCanvas.SNAP_VERTEX_PIXELS,
                axisThresholdWorld,
                canvas::project,
                canvas::project
        );
        canvas.activeSnapGuide = (previewOnly && outcome.hasGuide())
                ? new MapCanvas.SnapGuide(outcome.verticalGuide, outcome.horizontalGuide, outcome.guideX, outcome.guideY)
                : MapCanvas.SnapGuide.none();
        return new Point2D.Double(outcome.x, outcome.y);
    }

    void centerOnSegment(int segmentIndex) {
        if (canvas.currentRouteVisualization == null) {
            return;
        }
        if (segmentIndex < 0 || segmentIndex >= canvas.currentRouteVisualization.getSegmentCount()) {
            return;
        }
        RouteVisualizationDto.Segment segment = canvas.currentRouteVisualization.getSegments().get(segmentIndex);
        Point2D.Double from = canvas.projectVertexById(segment.getFromVertexId());
        Point2D.Double to = canvas.projectVertexById(segment.getToVertexId());
        if (from == null || to == null) {
            return;
        }
        double centerX = (from.x + to.x) / 2.0;
        double centerY = (from.y + to.y) / 2.0;
        double targetX = canvas.getWidth() / 2.0;
        double targetY = canvas.getHeight() / 2.0;
        canvas.panX += targetX - centerX;
        canvas.panY += targetY - centerY;
        clampPanOffsets();
        invalidateScene(true, true);
        canvas.fireViewportChanged();
    }

    void recomputeWorldBounds() {
        if (canvas.vertices.isEmpty()) {
            canvas.worldMinX = -100;
            canvas.worldMaxX = 100;
            canvas.worldMinY = -100;
            canvas.worldMaxY = 100;
            canvas.worldWidth = 200;
            canvas.worldHeight = 200;
            canvas.projectionDirty = true;
            return;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        for (Vertex vertex : canvas.vertices) {
            minX = Math.min(minX, vertex.getX());
            minY = Math.min(minY, vertex.getY());
            maxX = Math.max(maxX, vertex.getX());
            maxY = Math.max(maxY, vertex.getY());
        }

        canvas.worldMinX = minX - MapCanvas.WORLD_PADDING;
        canvas.worldMaxX = maxX + MapCanvas.WORLD_PADDING;
        canvas.worldMinY = minY - MapCanvas.WORLD_PADDING;
        canvas.worldMaxY = maxY + MapCanvas.WORLD_PADDING;
        canvas.worldWidth = Math.max(1, canvas.worldMaxX - canvas.worldMinX);
        canvas.worldHeight = Math.max(1, canvas.worldMaxY - canvas.worldMinY);
        canvas.projectionDirty = true;
    }

    void clampPanOffsets() {
        int width = Math.max(canvas.getWidth(), 1);
        int height = Math.max(canvas.getHeight(), 1);
        double availableWidth = Math.max(width - MapCanvas.VIEW_PADDING * 2, 20);
        double availableHeight = Math.max(height - MapCanvas.VIEW_PADDING * 2, 20);
        double safeWorldWidth = Math.max(canvas.worldWidth, 1);
        double safeWorldHeight = Math.max(canvas.worldHeight, 1);
        double localBaseScale = Math.min(availableWidth / safeWorldWidth, availableHeight / safeWorldHeight);
        localBaseScale = Math.max(localBaseScale, 0.01);
        double scaledWidth = safeWorldWidth * localBaseScale * canvas.zoom;
        double scaledHeight = safeWorldHeight * localBaseScale * canvas.zoom;
        double extraOffsetX = Math.max(availableWidth, scaledWidth) * MapCanvas.PAN_OVERSCROLL_FACTOR;
        double extraOffsetY = Math.max(availableHeight, scaledHeight) * MapCanvas.PAN_OVERSCROLL_FACTOR;
        double maxOffsetX = Math.max((availableWidth + scaledWidth) / 2.0 - MapCanvas.PAN_VISIBLE_MIN_PIXELS + extraOffsetX, 0);
        double maxOffsetY = Math.max((availableHeight + scaledHeight) / 2.0 - MapCanvas.PAN_VISIBLE_MIN_PIXELS + extraOffsetY, 0);
        canvas.panX = MapCanvas.clamp(canvas.panX, -maxOffsetX, maxOffsetX);
        canvas.panY = MapCanvas.clamp(canvas.panY, -maxOffsetY, maxOffsetY);
    }

    int currentRouteSegmentCount() {
        return canvas.currentRouteVisualization == null ? 0 : canvas.currentRouteVisualization.getSegmentCount();
    }

    void invalidateScene(boolean markSceneDirty, boolean markProjectionDirty) {
        if (markSceneDirty) {
            canvas.sceneCache.markDirty();
        }
        if (markProjectionDirty) {
            canvas.projectionDirty = true;
        }
    }
}
