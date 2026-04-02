package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class LayerPanel extends JPanel {
    private final MapCanvas canvas;
    private final Map<MapCanvas.Layer, JCheckBox> layerChecks;
    private final Map<MapCanvas.Layer, JCheckBox> lockChecks;
    private final Map<MapCanvas.Layer, JLabel> opacityLabels;
    private final DefaultListModel<MapCanvas.Layer> orderModel;
    private final JList<MapCanvas.Layer> orderList;
    private Listener listener;

    public LayerPanel(MapCanvas canvas) {
        this.canvas = canvas;
        this.layerChecks = new EnumMap<MapCanvas.Layer, JCheckBox>(MapCanvas.Layer.class);
        this.lockChecks = new EnumMap<MapCanvas.Layer, JCheckBox>(MapCanvas.Layer.class);
        this.opacityLabels = new EnumMap<MapCanvas.Layer, JLabel>(MapCanvas.Layer.class);

        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(242, 245, 249));
        setBorder(UiStyles.sectionBorder("地图图层"));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(createLayerRow(MapCanvas.Layer.ROAD, "道路层"));
        stack.add(Box.createVerticalStrut(6));
        stack.add(createLayerRow(MapCanvas.Layer.FORBIDDEN, "禁行层"));
        stack.add(Box.createVerticalStrut(6));
        stack.add(createLayerRow(MapCanvas.Layer.VERTEX, "点位层"));
        stack.add(Box.createVerticalStrut(6));
        stack.add(createLayerRow(MapCanvas.Layer.LABEL, "标签层"));

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
        JScrollPane orderScroll = new JScrollPane(orderList);
        orderScroll.setPreferredSize(new Dimension(190, 88));
        orderPanel.add(orderScroll, BorderLayout.CENTER);

        JPanel orderButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        orderButtons.setOpaque(false);
        JButton upButton = UiStyles.secondaryButton("上移");
        upButton.addActionListener(e -> moveSelected(-1));
        JButton downButton = UiStyles.secondaryButton("下移");
        downButton.addActionListener(e -> moveSelected(1));
        orderButtons.add(upButton);
        orderButtons.add(downButton);
        orderPanel.add(orderButtons, BorderLayout.SOUTH);

        add(stack, BorderLayout.NORTH);
        add(orderPanel, BorderLayout.CENTER);

        JButton resetViewportButton = UiStyles.secondaryButton("重置视图");
        resetViewportButton.addActionListener(e -> {
            canvas.resetViewport();
            fireMessage("已重置视图。");
        });
        add(resetViewportButton, BorderLayout.SOUTH);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private JComponent createLayerRow(MapCanvas.Layer layer, String title) {
        JPanel row = new JPanel(new BorderLayout(4, 4));
        row.setOpaque(true);
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));

        JPanel line1 = new JPanel(new BorderLayout(4, 0));
        line1.setOpaque(false);

        JCheckBox visibleCheck = new JCheckBox(title, canvas.isLayerVisible(layer));
        visibleCheck.setOpaque(false);
        visibleCheck.setFont(UiStyles.BODY_FONT);
        visibleCheck.setForeground(UiStyles.TEXT_PRIMARY);
        visibleCheck.addActionListener(e -> {
            canvas.setLayerVisible(layer, visibleCheck.isSelected());
            fireMessage(title + (visibleCheck.isSelected() ? "已显示" : "已隐藏"));
        });
        layerChecks.put(layer, visibleCheck);

        JCheckBox lockCheck = new JCheckBox("锁定", canvas.isLayerLocked(layer));
        lockCheck.setOpaque(false);
        lockCheck.setFont(UiStyles.CAPTION_FONT);
        lockCheck.setHorizontalAlignment(SwingConstants.RIGHT);
        lockCheck.addActionListener(e -> {
            canvas.setLayerLocked(layer, lockCheck.isSelected());
            fireMessage(title + (lockCheck.isSelected() ? "已锁定" : "已解锁"));
        });
        lockChecks.put(layer, lockCheck);

        line1.add(visibleCheck, BorderLayout.WEST);
        line1.add(lockCheck, BorderLayout.EAST);

        JPanel line2 = new JPanel(new BorderLayout(4, 0));
        line2.setOpaque(false);
        JLabel opacityText = new JLabel("透明度");
        opacityText.setFont(UiStyles.CAPTION_FONT);
        opacityText.setForeground(UiStyles.TEXT_SECONDARY);
        line2.add(opacityText, BorderLayout.WEST);
        JLabel percentLabel = new JLabel(formatOpacity(canvas.getLayerOpacity(layer)), SwingConstants.RIGHT);
        percentLabel.setFont(UiStyles.CAPTION_FONT);
        percentLabel.setForeground(UiStyles.TEXT_SECONDARY);
        opacityLabels.put(layer, percentLabel);
        line2.add(percentLabel, BorderLayout.EAST);

        JSlider slider = new JSlider(15, 100, Math.round(canvas.getLayerOpacity(layer) * 100f));
        slider.setOpaque(false);
        slider.addChangeListener(e -> {
            float opacity = slider.getValue() / 100f;
            canvas.setLayerOpacity(layer, opacity);
            percentLabel.setText(formatOpacity(opacity));
            if (!slider.getValueIsAdjusting()) {
                fireMessage(title + "透明度设为 " + formatOpacity(opacity));
            }
        });

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(line1);
        body.add(Box.createVerticalStrut(2));
        body.add(line2);
        body.add(slider);

        row.add(body, BorderLayout.CENTER);
        return row;
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
        fireMessage("图层顺序已更新。");
    }

    private void syncOrderToCanvas() {
        List<MapCanvas.Layer> layers = new ArrayList<MapCanvas.Layer>();
        for (int i = 0; i < orderModel.size(); i++) {
            layers.add(orderModel.get(i));
        }
        canvas.setRenderOrder(layers);
    }

    private void fireMessage(String message) {
        if (listener == null || message == null || message.trim().isEmpty()) {
            return;
        }
        listener.onLayerPanelHint(message.trim());
    }

    private static String formatOpacity(float opacity) {
        int percent = (int) Math.round(opacity * 100f);
        return percent + "%";
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

    public interface Listener {
        void onLayerPanelHint(String message);
    }
}
