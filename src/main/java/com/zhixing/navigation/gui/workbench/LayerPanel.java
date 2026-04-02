package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LayerPanel extends JPanel {
    private final MapCanvas canvas;
    private final Map<MapCanvas.Layer, JCheckBox> layerChecks;
    private final DefaultListModel<MapCanvas.Layer> orderModel;
    private final JList<MapCanvas.Layer> orderList;

    public LayerPanel(MapCanvas canvas) {
        this.canvas = canvas;
        this.layerChecks = new EnumMap<MapCanvas.Layer, JCheckBox>(MapCanvas.Layer.class);

        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(242, 245, 249));
        setBorder(UiStyles.sectionBorder("地图图层"));

        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        checkPanel.setOpaque(false);

        addLayerCheck(checkPanel, MapCanvas.Layer.ROAD, "道路层");
        addLayerCheck(checkPanel, MapCanvas.Layer.FORBIDDEN, "禁行层");
        addLayerCheck(checkPanel, MapCanvas.Layer.VERTEX, "点位层");
        addLayerCheck(checkPanel, MapCanvas.Layer.LABEL, "标签层");

        orderModel = new DefaultListModel<MapCanvas.Layer>();
        for (MapCanvas.Layer layer : canvas.getRenderOrder()) {
            orderModel.addElement(layer);
        }

        orderList = new JList<MapCanvas.Layer>(orderModel);
        orderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderList.setSelectedIndex(0);
        orderList.setVisibleRowCount(4);
        orderList.setFont(UiStyles.BODY_FONT);
        orderList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MapCanvas.Layer) {
                    setText(layerText((MapCanvas.Layer) value));
                }
                return comp;
            }
        });

        JPanel orderPanel = new JPanel(new BorderLayout(6, 6));
        orderPanel.setOpaque(false);
        orderPanel.add(new JScrollPane(orderList), BorderLayout.CENTER);

        JPanel orderButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        orderButtons.setOpaque(false);
        JButton upButton = UiStyles.secondaryButton("上移");
        upButton.addActionListener(e -> moveSelected(-1));
        JButton downButton = UiStyles.secondaryButton("下移");
        downButton.addActionListener(e -> moveSelected(1));
        orderButtons.add(upButton);
        orderButtons.add(downButton);
        orderPanel.add(orderButtons, BorderLayout.SOUTH);

        add(checkPanel, BorderLayout.NORTH);
        add(orderPanel, BorderLayout.CENTER);

        JButton resetViewportButton = UiStyles.secondaryButton("重置视图");
        resetViewportButton.addActionListener(e -> canvas.resetViewport());
        add(resetViewportButton, BorderLayout.SOUTH);
    }

    private void addLayerCheck(JPanel parent, MapCanvas.Layer layer, String text) {
        JCheckBox checkBox = new JCheckBox(text, canvas.isLayerVisible(layer));
        checkBox.setOpaque(false);
        checkBox.setFont(UiStyles.BODY_FONT);
        checkBox.setForeground(UiStyles.TEXT_PRIMARY);
        checkBox.addActionListener(e -> canvas.setLayerVisible(layer, checkBox.isSelected()));

        layerChecks.put(layer, checkBox);
        parent.add(checkBox);
        parent.add(Box.createVerticalStrut(4));
    }

    private void moveSelected(int direction) {
        int index = orderList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        int target = index + direction;
        if (target < 0 || target >= orderModel.getSize()) {
            return;
        }

        MapCanvas.Layer selected = orderModel.get(index);
        orderModel.set(index, orderModel.get(target));
        orderModel.set(target, selected);
        orderList.setSelectedIndex(target);
        syncOrderToCanvas();
    }

    private void syncOrderToCanvas() {
        List<MapCanvas.Layer> layers = new ArrayList<MapCanvas.Layer>();
        for (int i = 0; i < orderModel.size(); i++) {
            layers.add(orderModel.get(i));
        }
        canvas.setRenderOrder(layers);
    }

    private static String layerText(MapCanvas.Layer layer) {
        if (layer == MapCanvas.Layer.ROAD) {
            return "道路层";
        }
        if (layer == MapCanvas.Layer.FORBIDDEN) {
            return "禁行层";
        }
        if (layer == MapCanvas.Layer.VERTEX) {
            return "点位层";
        }
        return "标签层";
    }
}