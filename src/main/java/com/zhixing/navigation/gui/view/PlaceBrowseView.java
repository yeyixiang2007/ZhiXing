package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

public class PlaceBrowseView extends JPanel {
    public static final String ALL_PLACE_TYPES = "全部类型";

    private final JComboBox<String> typeFilterCombo;
    private final DefaultTableModel tableModel;
    private final JPanel contentPanel;
    private final JButton toggleButton;
    private final JLabel countLabel;
    private boolean expanded;
    private Listener listener;

    public PlaceBrowseView() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel shell = UiStyles.cardPanel(new BorderLayout(0, 12));
        shell.setBackground(UiStyles.SURFACE);

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("地点浏览");
        titleLabel.setFont(UiStyles.SUBTITLE_FONT);
        titleLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JTextArea descriptionArea = createSupportingText("次级功能：用于按地点类型筛选校园地点列表，默认折叠以突出路径查询主流程。");

        titleStack.add(titleLabel);
        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(descriptionArea);

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        metaRow.setOpaque(false);

        countLabel = UiStyles.captionLabel("共 0 个地点");
        toggleButton = UiStyles.ghostButton("展开地点浏览");
        toggleButton.addActionListener(e -> setExpanded(!expanded));

        metaRow.add(countLabel);
        metaRow.add(toggleButton);

        header.add(titleStack, BorderLayout.CENTER);
        header.add(metaRow, BorderLayout.EAST);

        contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel filterCard = UiStyles.softCardPanel(new BorderLayout(0, 8));
        filterCard.setBackground(UiStyles.SURFACE_ALT);

        JLabel filterLabel = UiStyles.formLabel("地点类型");
        typeFilterCombo = UiStyles.formComboBox();
        typeFilterCombo.addItem(ALL_PLACE_TYPES);
        for (PlaceType placeType : PlaceType.values()) {
            typeFilterCombo.addItem(placeType.name());
        }
        stretchCombo(typeFilterCombo);
        typeFilterCombo.addActionListener(e -> notifyFilterChanged());

        JButton refreshButton = UiStyles.secondaryButton("刷新列表");
        stretchButton(refreshButton, 38);
        refreshButton.addActionListener(e -> notifyFilterChanged());

        filterCard.add(filterLabel, BorderLayout.NORTH);
        filterCard.add(typeFilterCombo, BorderLayout.CENTER);
        filterCard.add(refreshButton, BorderLayout.SOUTH);

        tableModel = ViewUtils.createReadOnlyTableModel(new String[]{"地点ID", "地点名称", "地点类型", "X", "Y", "描述"});
        JTable table = new JTable(tableModel);
        UiStyles.applyTableStyle(table);

        JScrollPane tablePane = new JScrollPane(table);
        tablePane.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));
        tablePane.setPreferredSize(new Dimension(0, 220));
        UiStyles.applyTableScrollPaneStyle(tablePane);

        contentPanel.add(filterCard);
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(tablePane);

        shell.add(header, BorderLayout.NORTH);
        shell.add(contentPanel, BorderLayout.CENTER);
        add(shell, BorderLayout.CENTER);

        setExpanded(false);
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
        countLabel.setText("共 " + vertices.size() + " 个地点");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void setExpanded(boolean expanded) {
        this.expanded = expanded;
        contentPanel.setVisible(expanded);
        toggleButton.setText(expanded ? "收起地点浏览" : "展开地点浏览");
        revalidate();
        repaint();
    }

    private void notifyFilterChanged() {
        if (listener != null) {
            listener.onFilterChanged(selectedType());
        }
    }

    private static JTextArea createSupportingText(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setOpaque(false);
        textArea.setFont(UiStyles.CAPTION_FONT);
        textArea.setForeground(UiStyles.TEXT_SECONDARY);
        textArea.setBorder(BorderFactory.createEmptyBorder());
        textArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return textArea;
    }

    private static void stretchCombo(JComboBox<?> comboBox) {
        Dimension preferred = comboBox.getPreferredSize();
        int height = Math.max(40, preferred.height);
        comboBox.setPreferredSize(new Dimension(0, height));
        comboBox.setMinimumSize(new Dimension(0, height));
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    private static void stretchButton(JButton button, int height) {
        button.setPreferredSize(new Dimension(0, height));
        button.setMinimumSize(new Dimension(0, height));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    public interface Listener {
        void onFilterChanged(String selectedType);
    }
}
