package com.zhixing.navigation.gui.styles;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

public final class UiStyles {
    public static final Color PAGE_BACKGROUND = new Color(245, 247, 250);
    public static final Color PANEL_BACKGROUND = Color.WHITE;
    public static final Color PRIMARY = new Color(30, 106, 255);
    public static final Color TEXT_PRIMARY = new Color(31, 35, 40);
    public static final Color TEXT_SECONDARY = new Color(101, 109, 118);
    public static final Color BORDER = new Color(216, 222, 228);
    public static final Color SUCCESS = new Color(30, 130, 76);
    public static final Color WARNING = new Color(180, 104, 0);
    public static final Color ERROR = new Color(196, 43, 28);

    public static final Font TITLE_FONT = new Font("Microsoft YaHei", Font.BOLD, 22);
    public static final Font SUBTITLE_FONT = new Font("Microsoft YaHei", Font.BOLD, 15);
    public static final Font BODY_FONT = new Font("Microsoft YaHei", Font.PLAIN, 13);
    public static final Font CAPTION_FONT = new Font("Microsoft YaHei", Font.PLAIN, 12);

    private UiStyles() {
    }

    public static void installDefaults() {
        UIManager.put("Label.font", BODY_FONT);
        UIManager.put("Button.font", BODY_FONT);
        UIManager.put("TextField.font", BODY_FONT);
        UIManager.put("ComboBox.font", BODY_FONT);
        UIManager.put("Table.font", BODY_FONT);
    }

    public static Border sectionBorder(String title) {
        Border line = BorderFactory.createLineBorder(BORDER);
        Border spacing = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        Border withTitle = BorderFactory.createTitledBorder(line, title);
        return BorderFactory.createCompoundBorder(withTitle, spacing);
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(PRIMARY);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(TEXT_PRIMARY);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        return button;
    }

    public static JTextField formField(int columns) {
        JTextField field = new JTextField(columns);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(7, 8, 7, 8)
        ));
        return field;
    }

    public static JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(TEXT_PRIMARY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    public static JTable standardTable(Object[][] data, Object[] headers) {
        JTable table = new JTable(data, headers);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setGridColor(BORDER);
        table.setSelectionBackground(new Color(223, 236, 255));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFont(BODY_FONT);
        JTableHeader header = table.getTableHeader();
        header.setFont(SUBTITLE_FONT);
        header.setBackground(new Color(235, 241, 252));
        header.setForeground(TEXT_PRIMARY);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 30));
        return table;
    }
}
