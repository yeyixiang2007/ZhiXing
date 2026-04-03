package com.zhixing.navigation.gui.workbench;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;

class LayerPanelTest {

    @Test
    void shouldToggleLayerVisibilityAndEmitHint() throws Exception {
        MapCanvas canvas = new MapCanvas();
        LayerPanel panel = new LayerPanel(canvas);
        AtomicReference<String> latest = new AtomicReference<String>();
        panel.setListener(latest::set);

        JCheckBox labelLayer = findCheckBox(panel, "???");
        Assertions.assertNotNull(labelLayer);

        SwingUtilities.invokeAndWait(labelLayer::doClick);

        Assertions.assertFalse(canvas.isLayerVisible(MapCanvas.Layer.LABEL));
        Assertions.assertNotNull(latest.get());
        Assertions.assertTrue(latest.get().contains("???"));
    }

    @Test
    void shouldResetViewportFromButton() throws Exception {
        MapCanvas canvas = new MapCanvas();
        LayerPanel panel = new LayerPanel(canvas);
        AtomicReference<String> latest = new AtomicReference<String>();
        panel.setListener(latest::set);

        JButton reset = findButton(panel, "????");
        Assertions.assertNotNull(reset);

        SwingUtilities.invokeAndWait(reset::doClick);

        Assertions.assertEquals("??????", latest.get());
    }

    private static JCheckBox findCheckBox(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JCheckBox) {
                JCheckBox box = (JCheckBox) child;
                if (text.equals(box.getText())) {
                    return box;
                }
            }
            if (child instanceof Container) {
                JCheckBox nested = findCheckBox((Container) child, text);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static JButton findButton(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton) {
                JButton button = (JButton) child;
                if (text.equals(button.getText())) {
                    return button;
                }
            }
            if (child instanceof Container) {
                JButton nested = findButton((Container) child, text);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
