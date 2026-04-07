package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.gui.model.VertexOption;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class RoadManageView extends JPanel {
    private final JComboBox<VertexOption> fromCombo;
    private final JComboBox<VertexOption> toCombo;
    private final JTextField weightField;
    private final JCheckBox oneWayCheck;
    private final JCheckBox forbiddenCheck;
    private final JComboBox<RoadType> roadTypeCombo;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public RoadManageView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiStyles.PANEL_BACKGROUND);
        form.setBorder(UiStyles.sectionBorder("道路编辑"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        fromCombo = new JComboBox<VertexOption>();
        fromCombo.setFont(UiStyles.BODY_FONT);
        toCombo = new JComboBox<VertexOption>();
        toCombo.setFont(UiStyles.BODY_FONT);
        weightField = UiStyles.formField(16);
        oneWayCheck = new JCheckBox("单行道");
        oneWayCheck.setFont(UiStyles.BODY_FONT);
        oneWayCheck.setOpaque(false);
        forbiddenCheck = new JCheckBox("禁行");
        forbiddenCheck.setFont(UiStyles.BODY_FONT);
        forbiddenCheck.setOpaque(false);
        roadTypeCombo = new JComboBox<RoadType>(RoadType.values());
        roadTypeCombo.setFont(UiStyles.BODY_FONT);

        ViewUtils.addFormRow(form, gbc, 0, "起点", fromCombo);
        ViewUtils.addFormRow(form, gbc, 1, "终点", toCombo);
        ViewUtils.addFormRow(form, gbc, 2, "距离(米)", weightField);
        ViewUtils.addFormRow(form, gbc, 3, "道路类型", roadTypeCombo);

        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        checkPanel.setOpaque(false);
        checkPanel.add(oneWayCheck);
        checkPanel.add(forbiddenCheck);
        ViewUtils.addFormRow(form, gbc, 4, "属性", checkPanel);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionBar.setOpaque(false);
        JButton addButton = UiStyles.primaryButton("新增");
        JButton updateButton = UiStyles.secondaryButton("修改");
        JButton deleteButton = UiStyles.secondaryButton("删除");
        JButton clearButton = UiStyles.secondaryButton("清空表单");
        actionBar.add(addButton);
        actionBar.add(updateButton);
        actionBar.add(deleteButton);
        actionBar.add(clearButton);
        ViewUtils.addFormRow(form, gbc, 5, "操作", actionBar);

        tableModel = ViewUtils.createReadOnlyTableModel(new String[]{"起点ID", "终点ID", "距离", "单行", "禁行", "道路类型"});
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(UiStyles.BODY_FONT);
        table.getTableHeader().setFont(UiStyles.SUBTITLE_FONT);
        table.getSelectionModel().addListSelectionListener(this::handleRowSelectionChanged);

        addButton.addActionListener(e -> {
            if (listener != null) {
                listener.onAdd(buildFormData());
            }
        });
        updateButton.addActionListener(e -> {
            if (listener != null) {
                listener.onUpdate(buildFormData());
            }
        });
        deleteButton.addActionListener(e -> {
            if (listener != null) {
                listener.onDelete(buildFormData());
            }
        });
        clearButton.addActionListener(e -> clearForm());

        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(UiStyles.sectionBorder("道路列表"));
        UiStyles.applyTableScrollPaneStyle(tablePane);

        add(form, BorderLayout.NORTH);
        add(tablePane, BorderLayout.CENTER);
    }

    public void setVertexOptions(List<VertexOption> options, String selectedFrom, String selectedTo) {
        refillCombo(fromCombo, options, selectedFrom);
        refillCombo(toCombo, options, selectedTo);
    }

    public void setRoads(List<Edge> roads) {
        tableModel.setRowCount(0);
        for (Edge edge : roads) {
            tableModel.addRow(new Object[]{
                    edge.getFromVertex().getId(),
                    edge.getToVertex().getId(),
                    edge.getWeight(),
                    edge.isOneWay() ? "是" : "否",
                    edge.isForbidden() ? "是" : "否",
                    edge.getRoadType().name()
            });
        }
    }

    public String selectedFromId() {
        VertexOption option = (VertexOption) fromCombo.getSelectedItem();
        return option == null ? null : option.getId();
    }

    public String selectedToId() {
        VertexOption option = (VertexOption) toCombo.getSelectedItem();
        return option == null ? null : option.getId();
    }

    public void clearForm() {
        if (fromCombo.getItemCount() > 0) {
            fromCombo.setSelectedIndex(0);
        }
        if (toCombo.getItemCount() > 0) {
            toCombo.setSelectedIndex(0);
        }
        weightField.setText("");
        oneWayCheck.setSelected(false);
        forbiddenCheck.setSelected(false);
        roadTypeCombo.setSelectedIndex(0);
    }

    public void fillFromEdge(Edge edge) {
        if (edge == null) {
            return;
        }
        String fromId = edge.getFromVertex().getId();
        String toId = edge.getToVertex().getId();
        ViewUtils.selectComboByMatcher(fromCombo, value -> value != null && fromId.equals(value.getId()));
        ViewUtils.selectComboByMatcher(toCombo, value -> value != null && toId.equals(value.getId()));
        weightField.setText(String.valueOf(edge.getWeight()));
        oneWayCheck.setSelected(edge.isOneWay());
        forbiddenCheck.setSelected(edge.isForbidden());
        roadTypeCombo.setSelectedItem(edge.getRoadType());
    }

    public void setForbiddenQuick(boolean forbidden) {
        forbiddenCheck.setSelected(forbidden);
    }

    private RoadFormData buildFormData() {
        return new RoadFormData(
                selectedFromId(),
                selectedToId(),
                safeTrim(weightField.getText()),
                oneWayCheck.isSelected(),
                forbiddenCheck.isSelected(),
                (RoadType) roadTypeCombo.getSelectedItem()
        );
    }

    private void handleRowSelectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        String fromId = String.valueOf(tableModel.getValueAt(row, 0));
        String toId = String.valueOf(tableModel.getValueAt(row, 1));
        ViewUtils.selectComboByMatcher(fromCombo, value -> value != null && fromId.equals(value.getId()));
        ViewUtils.selectComboByMatcher(toCombo, value -> value != null && toId.equals(value.getId()));
        weightField.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        oneWayCheck.setSelected("是".equals(String.valueOf(tableModel.getValueAt(row, 3))));
        forbiddenCheck.setSelected("是".equals(String.valueOf(tableModel.getValueAt(row, 4))));
        roadTypeCombo.setSelectedItem(RoadType.valueOf(String.valueOf(tableModel.getValueAt(row, 5))));
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

    private static String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onAdd(RoadFormData data);

        void onUpdate(RoadFormData data);

        void onDelete(RoadFormData data);
    }

    public static final class RoadFormData {
        private final String fromId;
        private final String toId;
        private final String weight;
        private final boolean oneWay;
        private final boolean forbidden;
        private final RoadType roadType;

        public RoadFormData(String fromId, String toId, String weight, boolean oneWay, boolean forbidden, RoadType roadType) {
            this.fromId = fromId;
            this.toId = toId;
            this.weight = weight;
            this.oneWay = oneWay;
            this.forbidden = forbidden;
            this.roadType = roadType;
        }

        public String getFromId() {
            return fromId;
        }

        public String getToId() {
            return toId;
        }

        public String getWeight() {
            return weight;
        }

        public boolean isOneWay() {
            return oneWay;
        }

        public boolean isForbidden() {
            return forbidden;
        }

        public RoadType getRoadType() {
            return roadType;
        }
    }
}
