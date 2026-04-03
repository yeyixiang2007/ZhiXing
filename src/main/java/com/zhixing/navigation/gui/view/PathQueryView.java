package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.gui.model.VertexOption;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class PathQueryView extends JPanel {
    public enum MapPickTarget {
        NONE,
        START,
        END
    }

    private final JComboBox<VertexOption> startCombo;
    private final JComboBox<VertexOption> endCombo;
    private final PathDetailPanel detailPanel;
    private final JButton pickStartButton;
    private final JButton pickEndButton;
    private final JLabel mapPickHintLabel;

    private MapPickTarget mapPickTarget;

    public PathQueryView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(UiStyles.PANEL_BACKGROUND);
        formPanel.setBorder(UiStyles.sectionBorder("路径查询"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        startCombo = new JComboBox<VertexOption>();
        startCombo.setFont(UiStyles.BODY_FONT);
        endCombo = new JComboBox<VertexOption>();
        endCombo.setFont(UiStyles.BODY_FONT);

        pickStartButton = UiStyles.secondaryButton("地图选起点");
        pickEndButton = UiStyles.secondaryButton("地图选终点");
        JButton queryButton = UiStyles.primaryButton("查询路径");
        JButton switchButton = UiStyles.secondaryButton("交换起终点");

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(UiStyles.formLabel("起点"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(startCombo, gbc);
        gbc.weightx = 0;

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(UiStyles.formLabel("终点"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        formPanel.add(endCombo, gbc);
        gbc.weightx = 0;

        JPanel actionBar = new JPanel();
        actionBar.setOpaque(false);
        actionBar.setLayout(new BoxLayout(actionBar, BoxLayout.Y_AXIS));

        JPanel actionRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow1.setOpaque(false);
        actionRow1.add(pickStartButton);
        actionRow1.add(pickEndButton);

        JPanel actionRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow2.setOpaque(false);
        actionRow2.add(queryButton);
        actionRow2.add(switchButton);

        actionBar.add(actionRow1);
        actionBar.add(Box.createVerticalStrut(6));
        actionBar.add(actionRow2);
        gbc.gridx = 1;
        gbc.gridy = 2;
        formPanel.add(actionBar, gbc);

        mapPickHintLabel = new JLabel("地图选点模式：关闭");
        mapPickHintLabel.setFont(UiStyles.CAPTION_FONT);
        mapPickHintLabel.setForeground(UiStyles.TEXT_SECONDARY);
        gbc.gridx = 1;
        gbc.gridy = 3;
        formPanel.add(mapPickHintLabel, gbc);

        detailPanel = new PathDetailPanel();
        mapPickTarget = MapPickTarget.NONE;
        refreshMapPickUi();

        add(formPanel, BorderLayout.NORTH);
        add(detailPanel, BorderLayout.CENTER);

        pickStartButton.addActionListener(e -> setMapPickTargetInternal(
                mapPickTarget == MapPickTarget.START ? MapPickTarget.NONE : MapPickTarget.START,
                true
        ));
        pickEndButton.addActionListener(e -> setMapPickTargetInternal(
                mapPickTarget == MapPickTarget.END ? MapPickTarget.NONE : MapPickTarget.END,
                true
        ));
        queryButton.addActionListener(e -> {
            if (listener != null) {
                listener.onQuery(selectedStartId(), selectedEndId());
            }
        });
        switchButton.addActionListener(e -> swapSelection());
        detailPanel.setInstructionListener(index -> {
            if (listener != null) {
                listener.onInstructionSelected(index);
            }
        });
    }

    public void setVertexOptions(List<VertexOption> options, String selectedStartId, String selectedEndId) {
        refillCombo(startCombo, options, selectedStartId);
        refillCombo(endCombo, options, selectedEndId);
    }

    public void setResultContent(String content) {
        detailPanel.setContent(content);
    }

    public void setInstructions(List<String> instructions) {
        detailPanel.setInstructions(instructions);
    }

    public String selectedStartId() {
        VertexOption selected = (VertexOption) startCombo.getSelectedItem();
        return selected == null ? null : selected.getId();
    }

    public String selectedEndId() {
        VertexOption selected = (VertexOption) endCombo.getSelectedItem();
        return selected == null ? null : selected.getId();
    }

    public void pickVertex(String vertexId) {
        if (vertexId == null || vertexId.trim().isEmpty()) {
            return;
        }
        if (mapPickTarget == MapPickTarget.START) {
            setSelectedStartId(vertexId);
            setMapPickTargetInternal(MapPickTarget.END, true);
            setMapSelectionStatus("已选择起点：" + selectedStartId() + "，请继续点击地图选择终点。");
            return;
        }
        if (mapPickTarget == MapPickTarget.END) {
            setSelectedEndId(vertexId);
            setMapPickTargetInternal(MapPickTarget.NONE, true);
            setMapSelectionStatus("已选择终点：" + selectedEndId() + "，可直接查询路径。");
        }
    }

    public MapPickTarget mapPickTarget() {
        return mapPickTarget;
    }

    public void setMapPickTarget(MapPickTarget target) {
        setMapPickTargetInternal(target == null ? MapPickTarget.NONE : target, false);
    }

    public void setMapSelectionStatus(String text) {
        mapPickHintLabel.setText(text == null || text.trim().isEmpty() ? "地图选点模式：关闭" : text.trim());
    }

    public void setSelectedStartId(String vertexId) {
        selectById(startCombo, vertexId);
    }

    public void setSelectedEndId(String vertexId) {
        selectById(endCombo, vertexId);
    }

    private void swapSelection() {
        Object start = startCombo.getSelectedItem();
        Object end = endCombo.getSelectedItem();
        startCombo.setSelectedItem(end);
        endCombo.setSelectedItem(start);
    }

    private static void refillCombo(JComboBox<VertexOption> combo, List<VertexOption> options, String selectedId) {
        combo.removeAllItems();
        for (VertexOption option : options) {
            combo.addItem(option);
        }
        if (selectedId != null) {
            ViewUtils.selectComboByMatcher(combo, value -> value != null && selectedId.equals(value.getId()));
        } else if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private static void selectById(JComboBox<VertexOption> combo, String vertexId) {
        if (vertexId == null || vertexId.trim().isEmpty()) {
            return;
        }
        ViewUtils.selectComboByMatcher(combo, value -> value != null && vertexId.equals(value.getId()));
    }

    private void setMapPickTargetInternal(MapPickTarget target, boolean notify) {
        mapPickTarget = target;
        refreshMapPickUi();
        if (notify && listener != null) {
            listener.onMapPickTargetChanged(mapPickTarget);
        }
    }

    private void refreshMapPickUi() {
        stylePickButton(pickStartButton, mapPickTarget == MapPickTarget.START);
        stylePickButton(pickEndButton, mapPickTarget == MapPickTarget.END);
        if (mapPickTarget == MapPickTarget.START) {
            mapPickHintLabel.setText("地图选点模式：请选择起点");
        } else if (mapPickTarget == MapPickTarget.END) {
            mapPickHintLabel.setText("地图选点模式：请选择终点");
        } else if (mapPickHintLabel.getText() == null || mapPickHintLabel.getText().startsWith("地图选点模式：")) {
            mapPickHintLabel.setText("地图选点模式：关闭");
        }
    }

    private static void stylePickButton(JButton button, boolean active) {
        if (active) {
            button.setBackground(new Color(213, 228, 251));
            button.setForeground(UiStyles.PRIMARY);
            button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(new Color(170, 201, 243)),
                    javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14)
            ));
            return;
        }
        button.setBackground(Color.WHITE);
        button.setForeground(UiStyles.TEXT_PRIMARY);
        button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(UiStyles.BORDER),
                javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onQuery(String startId, String endId);

        void onMapPickTargetChanged(MapPickTarget target);

        void onInstructionSelected(int index);
    }
}
