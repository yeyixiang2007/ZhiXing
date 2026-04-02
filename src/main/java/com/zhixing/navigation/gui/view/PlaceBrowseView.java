package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

public class PlaceBrowseView extends JPanel {
    public static final String ALL_PLACE_TYPES = "全部类型";

    private final JComboBox<String> typeFilterCombo;
    private final DefaultTableModel tableModel;

    public PlaceBrowseView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filterBar.setBackground(UiStyles.PANEL_BACKGROUND);
        filterBar.setBorder(UiStyles.sectionBorder("地点筛选"));

        typeFilterCombo = new JComboBox<String>();
        typeFilterCombo.setFont(UiStyles.BODY_FONT);
        typeFilterCombo.addItem(ALL_PLACE_TYPES);
        for (PlaceType placeType : PlaceType.values()) {
            typeFilterCombo.addItem(placeType.name());
        }

        JButton refreshButton = UiStyles.secondaryButton("刷新列表");
        refreshButton.addActionListener(e -> notifyFilterChanged());
        typeFilterCombo.addActionListener(e -> notifyFilterChanged());

        filterBar.add(UiStyles.formLabel("地点类型"));
        filterBar.add(typeFilterCombo);
        filterBar.add(refreshButton);

        tableModel = ViewUtils.createReadOnlyTableModel(new String[]{"地点ID", "地点名称", "地点类型", "X", "Y", "描述"});
        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(UiStyles.BODY_FONT);
        table.getTableHeader().setFont(UiStyles.SUBTITLE_FONT);

        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(UiStyles.sectionBorder("地点列表"));

        add(filterBar, BorderLayout.NORTH);
        add(tablePane, BorderLayout.CENTER);
    }

    public String selectedType() {
        Object selected = typeFilterCombo.getSelectedItem();
        return selected == null ? ALL_PLACE_TYPES : String.valueOf(selected);
    }

    public void setPlaces(List<Vertex> vertices) {
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

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void notifyFilterChanged() {
        if (listener != null) {
            listener.onFilterChanged(selectedType());
        }
    }

    public interface Listener {
        void onFilterChanged(String selectedType);
    }
}
