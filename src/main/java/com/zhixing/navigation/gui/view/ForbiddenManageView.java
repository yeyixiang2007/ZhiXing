package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.gui.model.RoadOption;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public class ForbiddenManageView extends JPanel {
    private final JComboBox<RoadOption> roadCombo;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public ForbiddenManageView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolBar.setBackground(UiStyles.PANEL_BACKGROUND);
        toolBar.setBorder(UiStyles.sectionBorder("禁行设置"));

        roadCombo = new JComboBox<RoadOption>();
        roadCombo.setFont(UiStyles.BODY_FONT);

        JButton disableButton = UiStyles.primaryButton("设置禁行");
        JButton enableButton = UiStyles.secondaryButton("解除禁行");
        JButton refreshButton = UiStyles.secondaryButton("刷新");

        disableButton.addActionListener(e -> {
            if (listener != null) {
                listener.onToggleForbidden(selectedRoad(), true);
            }
        });
        enableButton.addActionListener(e -> {
            if (listener != null) {
                listener.onToggleForbidden(selectedRoad(), false);
            }
        });
        refreshButton.addActionListener(e -> {
            if (listener != null) {
                listener.onRefresh();
            }
        });

        toolBar.add(UiStyles.formLabel("道路"));
        toolBar.add(roadCombo);
        toolBar.add(disableButton);
        toolBar.add(enableButton);
        toolBar.add(refreshButton);

        tableModel = ViewUtils.createReadOnlyTableModel(new String[]{"起点ID", "终点ID", "禁行状态", "道路类型", "距离"});
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(UiStyles.BODY_FONT);
        table.getTableHeader().setFont(UiStyles.SUBTITLE_FONT);
        table.getSelectionModel().addListSelectionListener(this::handleSelectionChanged);

        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(UiStyles.sectionBorder("道路禁行状态"));
        UiStyles.applyTableScrollPaneStyle(tablePane);

        add(toolBar, BorderLayout.NORTH);
        add(tablePane, BorderLayout.CENTER);
    }

    public void setRoadOptions(List<RoadOption> options, String selectedKey) {
        roadCombo.removeAllItems();
        tableModel.setRowCount(0);
        for (RoadOption option : options) {
            roadCombo.addItem(option);
            tableModel.addRow(new Object[]{
                    option.getFromId(),
                    option.getToId(),
                    option.isForbidden() ? "禁行中" : "可通行",
                    option.getRoadType().name(),
                    option.getWeight()
            });
        }
        if (selectedKey != null) {
            ViewUtils.selectComboByMatcher(roadCombo, value -> value != null && selectedKey.equals(value.key()));
        } else if (roadCombo.getItemCount() > 0) {
            roadCombo.setSelectedIndex(0);
        }
    }

    public String selectedRoadKey() {
        RoadOption option = selectedRoad();
        return option == null ? null : option.key();
    }

    public RoadOption selectedRoad() {
        return (RoadOption) roadCombo.getSelectedItem();
    }

    private void handleSelectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0 || row >= roadCombo.getItemCount()) {
            return;
        }
        String fromId = String.valueOf(tableModel.getValueAt(row, 0));
        String toId = String.valueOf(tableModel.getValueAt(row, 1));
        ViewUtils.selectComboByMatcher(roadCombo, value ->
                value != null && fromId.equals(value.getFromId()) && toId.equals(value.getToId()));
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onToggleForbidden(RoadOption roadOption, boolean forbidden);

        void onRefresh();
    }
}
