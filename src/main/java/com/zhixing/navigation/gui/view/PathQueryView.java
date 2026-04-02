package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.gui.model.VertexOption;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class PathQueryView extends JPanel {
    private final JComboBox<VertexOption> startCombo;
    private final JComboBox<VertexOption> endCombo;
    private final PathDetailPanel detailPanel;

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

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionBar.setOpaque(false);
        actionBar.add(queryButton);
        actionBar.add(switchButton);
        gbc.gridx = 1;
        gbc.gridy = 2;
        formPanel.add(actionBar, gbc);

        detailPanel = new PathDetailPanel();

        add(formPanel, BorderLayout.NORTH);
        add(detailPanel, BorderLayout.CENTER);

        queryButton.addActionListener(e -> {
            if (listener != null) {
                listener.onQuery(selectedStartId(), selectedEndId());
            }
        });
        switchButton.addActionListener(e -> swapSelection());
    }

    public void setVertexOptions(List<VertexOption> options, String selectedStartId, String selectedEndId) {
        refillCombo(startCombo, options, selectedStartId);
        refillCombo(endCombo, options, selectedEndId);
    }

    public void setResultContent(String content) {
        detailPanel.setContent(content);
    }

    public String selectedStartId() {
        VertexOption selected = (VertexOption) startCombo.getSelectedItem();
        return selected == null ? null : selected.getId();
    }

    public String selectedEndId() {
        VertexOption selected = (VertexOption) endCombo.getSelectedItem();
        return selected == null ? null : selected.getId();
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

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onQuery(String startId, String endId);
    }
}
