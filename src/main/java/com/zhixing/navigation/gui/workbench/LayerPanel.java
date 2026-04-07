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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBackground(UiStyles.SURFACE);
        setBorder(BorderFactory.createEmptyBorder());

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(createLayerRow(MapCanvas.Layer.ROAD, "道路层"));
        stack.add(Box.createVerticalStrut(8));
        stack.add(createLayerRow(MapCanvas.Layer.FORBIDDEN, "禁行层"));
        stack.add(Box.createVerticalStrut(8));
        stack.add(createLayerRow(MapCanvas.Layer.VERTEX, "点位层"));
        stack.add(Box.createVerticalStrut(8));
        stack.add(createLayerRow(MapCanvas.Layer.LABEL, "标签层"));

        JPanel layerShell = UiStyles.softCardPanel(new BorderLayout(0, 10));
        layerShell.setBackground(UiStyles.SURFACE);
        layerShell.add(createSectionHeader("图层状态", "开关、锁定与透明度统一放在这里。"), BorderLayout.NORTH);
        layerShell.add(stack, BorderLayout.CENTER);

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
                JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof MapCanvas.Layer) {
                    comp.setText(layerText((MapCanvas.Layer) value));
                }
                comp.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(isSelected ? UiStyles.BRAND_500 : UiStyles.BORDER),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
                comp.setBackground(isSelected ? UiStyles.PRIMARY_SOFT : UiStyles.SURFACE);
                comp.setForeground(isSelected ? UiStyles.PRIMARY : UiStyles.TEXT_PRIMARY);
                return comp;
            }
        });

        JPanel orderPanel = UiStyles.softCardPanel(new BorderLayout(0, 10));
        orderPanel.setBackground(UiStyles.SURFACE);
        orderPanel.add(createSectionHeader("渲染顺序", "从上到下调整各层绘制优先级。"), BorderLayout.NORTH);
        JScrollPane orderScroll = new JScrollPane(orderList);
        orderScroll.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));
        orderScroll.setPreferredSize(new Dimension(220, 88));
        orderPanel.add(orderScroll, BorderLayout.CENTER);

        JPanel orderButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        orderButtons.setOpaque(false);
        JButton upButton = UiStyles.secondaryButton("上移");
        upButton.setPreferredSize(new Dimension(0, 36));
        upButton.setMinimumSize(new Dimension(86, 36));
        upButton.addActionListener(e -> moveSelected(-1));
        JButton downButton = UiStyles.secondaryButton("下移");
        downButton.setPreferredSize(new Dimension(0, 36));
        downButton.setMinimumSize(new Dimension(86, 36));
        downButton.addActionListener(e -> moveSelected(1));
        orderButtons.add(upButton);
        orderButtons.add(downButton);
        orderPanel.add(orderButtons, BorderLayout.SOUTH);

        add(layerShell, BorderLayout.NORTH);
        add(orderPanel, BorderLayout.CENTER);

        JButton resetViewportButton = UiStyles.ghostButton("重置视图");
        resetViewportButton.setPreferredSize(new Dimension(0, 38));
        resetViewportButton.setMinimumSize(new Dimension(0, 38));
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
        row.setBackground(UiStyles.SURFACE_ALT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 231, 240)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JPanel line1 = new JPanel(new BorderLayout(8, 0));
        line1.setOpaque(false);

        JCheckBox visibleCheck = UiStyles.formCheckBox(title);
        visibleCheck.setSelected(canvas.isLayerVisible(layer));
        visibleCheck.addActionListener(e -> {
            canvas.setLayerVisible(layer, visibleCheck.isSelected());
            fireMessage(title + (visibleCheck.isSelected() ? "已显示" : "已隐藏"));
        });
        layerChecks.put(layer, visibleCheck);

        JCheckBox lockCheck = UiStyles.formCheckBox("锁定");
        lockCheck.setSelected(canvas.isLayerLocked(layer));
        lockCheck.setHorizontalAlignment(SwingConstants.RIGHT);
        lockCheck.addActionListener(e -> {
            canvas.setLayerLocked(layer, lockCheck.isSelected());
            fireMessage(title + (lockCheck.isSelected() ? "已锁定" : "已解锁"));
        });
        lockChecks.put(layer, lockCheck);

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        visibleCheck.setAlignmentX(LEFT_ALIGNMENT);
        JLabel descLabel = UiStyles.captionLabel(layerDescription(layer));
        descLabel.setAlignmentX(LEFT_ALIGNMENT);
        titleStack.add(visibleCheck);
        titleStack.add(Box.createVerticalStrut(3));
        titleStack.add(descLabel);

        JPanel layerBadge = createLayerBadge(layer);
        line1.add(layerBadge, BorderLayout.WEST);
        line1.add(titleStack, BorderLayout.CENTER);
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

    private JComponent createSectionHeader(String title, String description) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiStyles.SUBTITLE_FONT);
        titleLabel.setForeground(UiStyles.TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel descLabel = UiStyles.captionLabel(description);
        descLabel.setAlignmentX(LEFT_ALIGNMENT);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(3));
        header.add(descLabel);
        return header;
    }

    private JPanel createLayerBadge(MapCanvas.Layer layer) {
        JPanel badge = new JPanel();
        badge.setPreferredSize(new Dimension(16, 16));
        badge.setMinimumSize(new Dimension(16, 16));
        badge.setMaximumSize(new Dimension(16, 16));
        badge.setOpaque(true);
        badge.setBackground(layerTint(layer));
        badge.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 220)));
        return badge;
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

    private static String layerDescription(MapCanvas.Layer layer) {
        if (layer == MapCanvas.Layer.ROAD) {
            return "基础道路、主干路与单行方向";
        }
        if (layer == MapCanvas.Layer.FORBIDDEN) {
            return "禁行道路和不可通行区域";
        }
        if (layer == MapCanvas.Layer.VERTEX) {
            return "校园地点节点与功能点位";
        }
        return "地点名称与重点标签";
    }

    private static Color layerTint(MapCanvas.Layer layer) {
        if (layer == MapCanvas.Layer.ROAD) {
            return new Color(77, 122, 186);
        }
        if (layer == MapCanvas.Layer.FORBIDDEN) {
            return new Color(201, 104, 88);
        }
        if (layer == MapCanvas.Layer.VERTEX) {
            return new Color(77, 169, 130);
        }
        return new Color(146, 124, 201);
    }

    public interface Listener {
        void onLayerPanelHint(String message);
    }
}
