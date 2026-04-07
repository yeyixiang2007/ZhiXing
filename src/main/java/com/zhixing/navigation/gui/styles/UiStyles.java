package com.zhixing.navigation.gui.styles;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;

public final class UiStyles {
    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_24 = 24;
    public static final int SPACE_32 = 32;

    public static final Color BRAND_700 = new Color(18, 59, 96);
    public static final Color BRAND_600 = new Color(28, 93, 143);
    public static final Color BRAND_500 = new Color(47, 128, 193);
    public static final Color BRAND_100 = new Color(234, 244, 251);

    public static final Color BACKGROUND = new Color(244, 247, 250);
    public static final Color SURFACE = Color.WHITE;
    public static final Color SURFACE_ALT = new Color(248, 250, 252);
    public static final Color SURFACE_SUBTLE = new Color(242, 246, 250);

    public static final Color BORDER = new Color(217, 226, 236);
    public static final Color BORDER_STRONG = new Color(191, 201, 214);

    public static final Color TEXT_PRIMARY = new Color(31, 41, 51);
    public static final Color TEXT_SECONDARY = new Color(103, 114, 129);
    public static final Color TEXT_TERTIARY = new Color(148, 163, 184);

    public static final Color PRIMARY = BRAND_600;
    public static final Color PRIMARY_SOFT = BRAND_100;
    public static final Color SUCCESS = new Color(35, 162, 109);
    public static final Color SUCCESS_SOFT = new Color(231, 247, 240);
    public static final Color WARNING = new Color(217, 142, 4);
    public static final Color WARNING_SOFT = new Color(255, 246, 227);
    public static final Color ERROR = new Color(214, 69, 69);
    public static final Color ERROR_SOFT = new Color(252, 238, 238);
    public static final Color INFO = new Color(45, 108, 223);
    public static final Color INFO_SOFT = new Color(229, 240, 255);
    public static final Color ROUTE_HIGHLIGHT = new Color(255, 159, 10);

    public static final Color PAGE_BACKGROUND = BACKGROUND;
    public static final Color PANEL_BACKGROUND = SURFACE;

    public static final Font DISPLAY_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 24);
    public static final Font TITLE_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 22);
    public static final Font SUBTITLE_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 15);
    public static final Font BODY_FONT = new Font("Microsoft YaHei UI", Font.PLAIN, 13);
    public static final Font CAPTION_FONT = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
    public static final Font METRIC_FONT = new Font("Microsoft YaHei UI", Font.BOLD, 20);

    private UiStyles() {
    }

    public static void installDefaults() {
        UIManager.put("Panel.background", PAGE_BACKGROUND);
        UIManager.put("Label.font", BODY_FONT);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Button.font", BODY_FONT);
        UIManager.put("Button.background", SURFACE);
        UIManager.put("Button.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.font", BODY_FONT);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("TextField.border", inputBorder());
        UIManager.put("ComboBox.font", BODY_FONT);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.border", inputBorder());
        UIManager.put("CheckBox.font", BODY_FONT);
        UIManager.put("CheckBox.foreground", TEXT_PRIMARY);
        UIManager.put("Table.font", BODY_FONT);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.selectionBackground", INFO_SOFT);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TableHeader.font", SUBTITLE_FONT);
        UIManager.put("TableHeader.background", SURFACE_SUBTLE);
        UIManager.put("TableHeader.foreground", TEXT_PRIMARY);
    }

    public static Border sectionBorder(String title) {
        TitledBorder titledBorder = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER), title);
        titledBorder.setTitleFont(SUBTITLE_FONT);
        titledBorder.setTitleColor(TEXT_SECONDARY);
        return BorderFactory.createCompoundBorder(
                titledBorder,
                BorderFactory.createEmptyBorder(SPACE_12, SPACE_12, SPACE_12, SPACE_12)
        );
    }

    public static Border cardBorder() {
        return cardBorder(SPACE_16);
    }

    public static Border cardBorder(int padding) {
        return cardBorder(padding, padding, padding, padding);
    }

    public static Border cardBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(top, left, bottom, right)
        );
    }

    public static Border softCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SURFACE_SUBTLE),
                BorderFactory.createEmptyBorder(SPACE_12, SPACE_12, SPACE_12, SPACE_12)
        );
    }

    public static JPanel cardPanel() {
        return createCardPanel(null, cardBorder());
    }

    public static JPanel cardPanel(LayoutManager layout) {
        return createCardPanel(layout, cardBorder());
    }

    public static JPanel softCardPanel(LayoutManager layout) {
        JPanel panel = createCardPanel(layout, softCardBorder());
        panel.setBackground(SURFACE_ALT);
        return panel;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        applyPrimaryButtonStyle(button);
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        applySecondaryButtonStyle(button);
        return button;
    }

    public static JButton dangerButton(String text) {
        JButton button = new JButton(text);
        applyDangerButtonStyle(button);
        return button;
    }

    public static JButton successButton(String text) {
        JButton button = new JButton(text);
        applySuccessButtonStyle(button);
        return button;
    }

    public static JButton ghostButton(String text) {
        JButton button = new JButton(text);
        applyGhostButtonStyle(button);
        return button;
    }

    public static void applyPrimaryButtonStyle(JButton button) {
        applyFilledButtonStyle(button, PRIMARY, Color.WHITE);
    }

    public static void applySecondaryButtonStyle(JButton button) {
        applyOutlinedButtonStyle(button, TEXT_PRIMARY, SURFACE, BORDER);
    }

    public static void applyDangerButtonStyle(JButton button) {
        applyFilledButtonStyle(button, ERROR, Color.WHITE);
    }

    public static void applySuccessButtonStyle(JButton button) {
        applyFilledButtonStyle(button, SUCCESS, Color.WHITE);
    }

    public static void applyGhostButtonStyle(JButton button) {
        applyOutlinedButtonStyle(button, PRIMARY, SURFACE_ALT, SURFACE_ALT);
        button.setBorder(BorderFactory.createEmptyBorder(SPACE_8, SPACE_16, SPACE_8, SPACE_16));
    }

    public static JTextField formField(int columns) {
        JTextField field = new JTextField(columns);
        applyTextFieldStyle(field);
        return field;
    }

    public static void applyTextFieldStyle(JTextField field) {
        field.setFont(BODY_FONT);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(SURFACE);
        field.setCaretColor(TEXT_PRIMARY);
        field.setBorder(inputBorder());
    }

    public static <T> JComboBox<T> formComboBox() {
        JComboBox<T> comboBox = new JComboBox<T>();
        applyComboBoxStyle(comboBox);
        return comboBox;
    }

    public static <T> JComboBox<T> formComboBox(T[] items) {
        JComboBox<T> comboBox = new JComboBox<T>(items);
        applyComboBoxStyle(comboBox);
        return comboBox;
    }

    public static void applyComboBoxStyle(JComboBox<?> comboBox) {
        comboBox.setFont(BODY_FONT);
        comboBox.setForeground(TEXT_PRIMARY);
        comboBox.setBackground(SURFACE);
        comboBox.setBorder(inputBorder());
        comboBox.setMaximumRowCount(10);
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String text = value == null ? "" : value.toString();
                label.setText(text);
                label.setToolTipText(text);
                label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                return label;
            }
        });
        installComboPopupWidthBehavior(comboBox);
    }

    public static JCheckBox formCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        applyCheckBoxStyle(checkBox);
        return checkBox;
    }

    public static void applyCheckBoxStyle(AbstractButton checkBox) {
        checkBox.setFont(BODY_FONT);
        checkBox.setForeground(TEXT_PRIMARY);
        checkBox.setFocusPainted(false);
        checkBox.setOpaque(false);
        checkBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        checkBox.setIconTextGap(SPACE_8);
    }

    public static Border inputBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        );
    }

    public static JLabel formLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(TEXT_PRIMARY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    public static JLabel captionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(CAPTION_FONT);
        label.setForeground(TEXT_SECONDARY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    public static JLabel metricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(METRIC_FONT);
        label.setForeground(TEXT_PRIMARY);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    public static JTable standardTable(Object[][] data, Object[] headers) {
        JTable table = new JTable(data, headers);
        applyTableStyle(table);
        return table;
    }

    public static void applyTableStyle(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setGridColor(BORDER);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(INFO_SOFT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setBackground(SURFACE);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(BODY_FONT);
        table.setBorder(BorderFactory.createEmptyBorder());

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setFont(SUBTITLE_FONT);
            header.setBackground(SURFACE_SUBTLE);
            header.setForeground(TEXT_PRIMARY);
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_STRONG));
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
            header.setReorderingAllowed(false);
        }
    }

    public static void applyTableScrollPaneStyle(JScrollPane scrollPane) {
        if (scrollPane == null) {
            return;
        }
        scrollPane.setBackground(SURFACE);
        if (scrollPane.getViewport() != null) {
            scrollPane.getViewport().setBackground(SURFACE);
        }
    }

    public static void applySplitPaneStyle(JSplitPane splitPane, int dividerSize) {
        if (splitPane == null) {
            return;
        }
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);
        splitPane.setOneTouchExpandable(false);
        splitPane.setDividerSize(dividerSize);
        splitPane.setBackground(PAGE_BACKGROUND);
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this) {
                    @Override
                    public void setBorder(Border border) {
                    }
                };
                divider.setBackground(PAGE_BACKGROUND);
                return divider;
            }
        });
    }

    private static JPanel createCardPanel(LayoutManager layout, Border border) {
        JPanel panel = layout == null ? new JPanel() : new JPanel(layout);
        panel.setOpaque(true);
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(border);
        return panel;
    }

    private static void installComboPopupWidthBehavior(JComboBox<?> comboBox) {
        if (Boolean.TRUE.equals(comboBox.getClientProperty("zhixing.combo.popup.sized"))) {
            return;
        }
        comboBox.putClientProperty("zhixing.combo.popup.sized", Boolean.TRUE);
        comboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                Object child = comboBox.getUI().getAccessibleChild(comboBox, 0);
                if (!(child instanceof BasicComboPopup)) {
                    return;
                }
                BasicComboPopup popup = (BasicComboPopup) child;
                int popupWidth = Math.max(comboBox.getWidth(), 220);
                Dimension preferredSize = popup.getPreferredSize();
                popup.setPreferredSize(new Dimension(popupWidth, preferredSize.height));
                popup.setPopupSize(popupWidth, preferredSize.height);
                if (popup.getComponentCount() <= 0 || !(popup.getComponent(0) instanceof JScrollPane)) {
                    return;
                }
                JScrollPane scrollPane = (JScrollPane) popup.getComponent(0);
                scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                scrollPane.setPreferredSize(new Dimension(popupWidth, preferredSize.height));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    private static void applyFilledButtonStyle(JButton button, Color background, Color foreground) {
        applyBaseButtonStyle(button);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setBorder(BorderFactory.createEmptyBorder(SPACE_8, SPACE_16, SPACE_8, SPACE_16));
        button.setBorderPainted(false);
    }

    private static void applyOutlinedButtonStyle(JButton button, Color foreground, Color background, Color borderColor) {
        applyBaseButtonStyle(button);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(SPACE_8, SPACE_16, SPACE_8, SPACE_16)
        ));
        button.setBorderPainted(true);
    }

    private static void applyBaseButtonStyle(JButton button) {
        button.setUI(new BasicButtonUI());
        button.setFont(BODY_FONT);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
    }
}
