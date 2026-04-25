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


final class MapCanvasLayerManager {
    private final MapCanvas canvas;

    MapCanvasLayerManager(MapCanvas canvas) {
        this.canvas = canvas;
    }

    public void setLayerVisible(MapCanvas.Layer layer, boolean visible) {
        if (layer == null) {
            return;
        }
        Boolean current = canvas.layerVisibility.get(layer);
        if (current != null && current.booleanValue() == visible) {
            return;
        }
        canvas.layerVisibility.put(layer, Boolean.valueOf(visible));
        canvas.invalidateScene(true, false);
        canvas.repaint();
    }

    public boolean isLayerVisible(MapCanvas.Layer layer) {
        Boolean visible = canvas.layerVisibility.get(layer);
        return visible == null || visible.booleanValue();
    }

    public void setLayerLocked(MapCanvas.Layer layer, boolean locked) {
        if (layer == null) {
            return;
        }
        Boolean current = canvas.layerLocked.get(layer);
        if (current != null && current.booleanValue() == locked) {
            return;
        }
        canvas.layerLocked.put(layer, Boolean.valueOf(locked));
        if (locked) {
            // Clear incompatible selections when a layer gets locked.
            if (layer == MapCanvas.Layer.VERTEX && !canvas.selectedVertexIds.isEmpty()) {
                canvas.selectedVertexIds.clear();
                canvas.fireSelectionChanged();
            }
            if ((layer == MapCanvas.Layer.ROAD || layer == MapCanvas.Layer.FORBIDDEN) && canvas.selectedEdgeKey != null) {
                Edge selected = canvas.findEdgeByKey(canvas.selectedEdgeKey);
                if (selected != null) {
                    MapCanvas.Layer selectedLayer = selected.isForbidden() ? MapCanvas.Layer.FORBIDDEN : MapCanvas.Layer.ROAD;
                    if (selectedLayer == layer) {
                        canvas.selectedEdgeKey = null;
                        canvas.fireSelectionChanged();
                    }
                }
            }
        }
        canvas.repaint();
    }

    public boolean isLayerLocked(MapCanvas.Layer layer) {
        Boolean locked = canvas.layerLocked.get(layer);
        return locked != null && locked.booleanValue();
    }

    public void setLayerOpacity(MapCanvas.Layer layer, float opacity) {
        if (layer == null) {
            return;
        }
        float next = (float) MapCanvas.clamp(opacity, 0.15, 1.0);
        Float current = canvas.layerOpacity.get(layer);
        if (current != null && Math.abs(current.floatValue() - next) < 0.0001f) {
            return;
        }
        canvas.layerOpacity.put(layer, Float.valueOf(next));
        canvas.invalidateScene(true, false);
        canvas.repaint();
    }

    public float getLayerOpacity(MapCanvas.Layer layer) {
        Float opacity = canvas.layerOpacity.get(layer);
        if (opacity == null) {
            return 1.0f;
        }
        return opacity.floatValue();
    }

    public List<MapCanvas.Layer> getRenderOrder() {
        return new ArrayList<MapCanvas.Layer>(canvas.renderOrder);
    }

    public void setRenderOrder(List<MapCanvas.Layer> order) {
        if (order == null || order.size() != MapCanvas.Layer.values().length) {
            return;
        }
        Set<MapCanvas.Layer> unique = new LinkedHashSet<MapCanvas.Layer>(order);
        if (unique.size() != MapCanvas.Layer.values().length) {
            return;
        }
        if (canvas.renderOrder.equals(order)) {
            return;
        }
        canvas.renderOrder.clear();
        canvas.renderOrder.addAll(order);
        canvas.invalidateScene(true, false);
        canvas.repaint();
    }
}
