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


final class MapCanvasRenderCoordinator {
    private final MapCanvas canvas;

    MapCanvasRenderCoordinator(MapCanvas canvas) {
        this.canvas = canvas;
    }

    void ensureSceneCache(double deviceScaleX, double deviceScaleY) {
        int width = Math.max(canvas.getWidth(), 1);
        int height = Math.max(canvas.getHeight(), 1);

        if (canvas.sceneCache.ensureSize(width, height, deviceScaleX, deviceScaleY)) {
            canvas.projectionDirty = true;
        }

        if (!canvas.sceneCache.isDirty()) {
            return;
        }

        Graphics2D g2 = canvas.sceneCache.image().createGraphics();
        try {
            applyQualityRenderingHints(g2);
            g2.scale(canvas.sceneCache.scaleX(), canvas.sceneCache.scaleY());
            g2.setColor(canvas.getBackground());
            g2.fillRect(0, 0, width, height);
            renderScene(g2);
        } finally {
            g2.dispose();
        }
        canvas.sceneCache.markClean();
    }

    void applyQualityRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    double resolveDeviceScale(Graphics2D g2, boolean horizontal) {
        AffineTransform transform = g2.getTransform();
        double transformScale = horizontal ? Math.abs(transform.getScaleX()) : Math.abs(transform.getScaleY());
        if (MapCanvas.isUsableScale(transformScale)) {
            return transformScale;
        }

        GraphicsConfiguration configuration = canvas.getGraphicsConfiguration();
        if (configuration != null) {
            AffineTransform defaultTransform = configuration.getDefaultTransform();
            double configurationScale = horizontal
                    ? Math.abs(defaultTransform.getScaleX())
                    : Math.abs(defaultTransform.getScaleY());
            if (MapCanvas.isUsableScale(configurationScale)) {
                return configurationScale;
            }
        }
        return 1.0;
    }

    void renderScene(Graphics2D g2) {
        canvas.updateProjection();
        canvas.baseSceneRenderer.drawScene(
                g2,
                canvas.getWidth(),
                canvas.getHeight(),
                MapCanvas.VIEW_PADDING,
                canvas.zoom,
                canvas.baseScale,
                canvas.worldMinX,
                canvas.worldMaxX,
                canvas.worldMinY,
                canvas.worldMaxY,
                canvas.referenceImage,
                canvas.referenceImageScale,
                canvas.renderOrder,
                canvas.edges,
                canvas.vertices,
                canvas::isLayerVisible,
                layer -> Float.valueOf(canvas.getLayerOpacity(layer)),
                vertex -> canvas.project(vertex),
                world -> canvas.project(world),
                MapCanvas::roadColor,
                roadType -> Float.valueOf(MapCanvas.roadWidth(roadType)),
                MapCanvas::placeColor,
                MapCanvas::withOpacity
        );
    }

    Point2D.Double projectVertexById(String vertexId) {
        if (vertexId == null) {
            return null;
        }
        Vertex vertex = canvas.vertexIndex.get(vertexId);
        return canvas.project(vertex);
    }
}
