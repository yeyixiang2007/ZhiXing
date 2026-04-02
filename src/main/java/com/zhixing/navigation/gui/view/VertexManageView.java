package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JButton;
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

public class VertexManageView extends JPanel {
    private final JTextField idField;
    private final JTextField nameField;
    private final JComboBox<PlaceType> typeCombo;
    private final JTextField xField;
    private final JTextField yField;
    private final JTextField descriptionField;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public VertexManageView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiStyles.PANEL_BACKGROUND);
        form.setBorder(UiStyles.sectionBorder("地点编辑"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        idField = UiStyles.formField(16);
        nameField = UiStyles.formField(16);
        typeCombo = new JComboBox<PlaceType>(PlaceType.values());
        typeCombo.setFont(UiStyles.BODY_FONT);
        xField = UiStyles.formField(16);
        yField = UiStyles.formField(16);
        descriptionField = UiStyles.formField(16);

        ViewUtils.addFormRow(form, gbc, 0, "地点ID", idField);
        ViewUtils.addFormRow(form, gbc, 1, "地点名称", nameField);
        ViewUtils.addFormRow(form, gbc, 2, "地点类型", typeCombo);
        ViewUtils.addFormRow(form, gbc, 3, "X坐标", xField);
        ViewUtils.addFormRow(form, gbc, 4, "Y坐标", yField);
        ViewUtils.addFormRow(form, gbc, 5, "地点描述", descriptionField);

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
        ViewUtils.addFormRow(form, gbc, 6, "操作", actionBar);

        tableModel = ViewUtils.createReadOnlyTableModel(new String[]{"ID", "名称", "类型", "X", "Y", "描述"});
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
                listener.onDelete(safeTrim(idField.getText()));
            }
        });
        clearButton.addActionListener(e -> clearForm());

        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(UiStyles.sectionBorder("地点列表"));

        add(form, BorderLayout.NORTH);
        add(tablePane, BorderLayout.CENTER);
    }

    public void setVertices(List<Vertex> vertices) {
        tableModel.setRowCount(0);
        for (Vertex vertex : vertices) {
            tableModel.addRow(new Object[]{
                    vertex.getId(),
                    vertex.getName(),
                    vertex.getType().name(),
                    vertex.getX(),
                    vertex.getY(),
                    vertex.getDescription()
            });
        }
    }

    public void clearForm() {
        idField.setText("");
        nameField.setText("");
        typeCombo.setSelectedIndex(0);
        xField.setText("");
        yField.setText("");
        descriptionField.setText("");
    }

    private VertexFormData buildFormData() {
        return new VertexFormData(
                safeTrim(idField.getText()),
                safeTrim(nameField.getText()),
                (PlaceType) typeCombo.getSelectedItem(),
                safeTrim(xField.getText()),
                safeTrim(yField.getText()),
                safeTrim(descriptionField.getText())
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
        idField.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        nameField.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        typeCombo.setSelectedItem(PlaceType.valueOf(String.valueOf(tableModel.getValueAt(row, 2))));
        xField.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        yField.setText(String.valueOf(tableModel.getValueAt(row, 4)));
        descriptionField.setText(String.valueOf(tableModel.getValueAt(row, 5)));
    }

    private static String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onAdd(VertexFormData formData);

        void onUpdate(VertexFormData formData);

        void onDelete(String id);
    }

    public static final class VertexFormData {
        private final String id;
        private final String name;
        private final PlaceType type;
        private final String x;
        private final String y;
        private final String description;

        public VertexFormData(String id, String name, PlaceType type, String x, String y, String description) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.x = x;
            this.y = y;
            this.description = description;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public PlaceType getType() {
            return type;
        }

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }

        public String getDescription() {
            return description;
        }
    }
}
