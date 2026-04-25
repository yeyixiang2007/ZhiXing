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


final class MapCanvasReferenceManager {
    private final MapCanvas canvas;

    MapCanvasReferenceManager(MapCanvas canvas) {
        this.canvas = canvas;
    }

    public void setReferenceImage(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("reference image must be a non-empty bitmap");
        }
        canvas.referenceImage = image;
        canvas.invalidateScene(true, false);
        canvas.repaint();
    }

    public boolean hasReferenceImage() {
        return canvas.referenceImage != null;
    }

    public void clearReferenceImage() {
        canvas.referenceImage = null;
        canvas.invalidateScene(true, false);
        canvas.repaint();
    }

    public void setReferenceImageScale(double scale) {
        if (Double.isNaN(scale) || Double.isInfinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("reference image scale must be finite and > 0");
        }
        canvas.referenceImageScale = scale;
        canvas.invalidateScene(true, false);
        canvas.repaint();
    }

    public double getReferenceImageScale() {
        return canvas.referenceImageScale;
    }

    public void setMetersPerWorldUnit(double metersPerWorldUnit) {
        if (Double.isNaN(metersPerWorldUnit) || Double.isInfinite(metersPerWorldUnit) || metersPerWorldUnit <= 0) {
            throw new IllegalArgumentException("metersPerWorldUnit must be finite and > 0");
        }
        canvas.metersPerWorldUnit = metersPerWorldUnit;
        canvas.repaint();
    }

    public double getMetersPerWorldUnit() {
        return canvas.metersPerWorldUnit;
    }
}
