package com.zhixing.navigation.gui;

import com.zhixing.navigation.application.auth.AuthService;
import com.zhixing.navigation.application.map.MapService;
import com.zhixing.navigation.application.navigation.ConsolePathFormatter;
import com.zhixing.navigation.application.navigation.NavigationService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.planning.DijkstraStrategy;
import com.zhixing.navigation.gui.components.AdminLoginDialog;
import com.zhixing.navigation.gui.components.LoadingOverlay;
import com.zhixing.navigation.gui.components.ViewportFillPanel;
import com.zhixing.navigation.gui.controller.AuthController;
import com.zhixing.navigation.gui.controller.MapController;
import com.zhixing.navigation.gui.controller.NavigationController;
import com.zhixing.navigation.gui.model.OverviewData;
import com.zhixing.navigation.gui.model.RoadOption;
import com.zhixing.navigation.gui.model.RouteVisualizationDto;
import com.zhixing.navigation.gui.model.VertexOption;
import com.zhixing.navigation.gui.routing.AppRoute;
import com.zhixing.navigation.gui.styles.UiStyles;
import com.zhixing.navigation.gui.view.ForbiddenManageView;
import com.zhixing.navigation.gui.view.OverviewDashboardView;
import com.zhixing.navigation.gui.view.PathQueryView;
import com.zhixing.navigation.gui.view.PlaceBrowseView;
import com.zhixing.navigation.gui.view.RoadManageView;
import com.zhixing.navigation.gui.view.VertexManageView;
import com.zhixing.navigation.gui.workbench.EditToolMode;
import com.zhixing.navigation.gui.workbench.LayerPanel;
import com.zhixing.navigation.gui.workbench.MapCanvas;
import com.zhixing.navigation.gui.workbench.MapWorkbenchView;
import com.zhixing.navigation.gui.workbench.WorkbenchFeedback;
import com.zhixing.navigation.gui.workbench.command.CommandBus;
import com.zhixing.navigation.gui.workbench.state.MapViewState;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JLayeredPane;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


final class MainViewAdminToolbarBuilder {
    private final MainView view;

    MainViewAdminToolbarBuilder(MainView view) {
        this.view = view;
    }

    JPanel createMapOverlayToolbar() {
        Color toolbarBackground = UiStyles.OVERLAY_PANEL_BACKGROUND;
        JPanel palette = new JPanel();
        palette.setLayout(new BoxLayout(palette, BoxLayout.Y_AXIS));
        palette.setOpaque(true);
        palette.setBackground(toolbarBackground);

        palette.add(createPaletteSectionLabel("编辑"));
        palette.add(createPaletteModeButton(EditToolMode.SELECT, "选", "选择工具"));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteModeButton(EditToolMode.ADD_VERTEX, "+", "添加点位"));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteModeButton(EditToolMode.ADD_EDGE, "/", "连线工具"));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteModeButton(EditToolMode.MOVE_VERTEX, "✥", "移动点位"));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteModeButton(EditToolMode.DELETE_OBJECT, "✕", "删除对象"));

        palette.add(Box.createVerticalStrut(12));
        palette.add(createPaletteSectionLabel("历史"));
        view.undoButton = createPaletteActionButton("↶", "撤销", view::undoLastEdit);
        view.redoButton = createPaletteActionButton("↷", "重做", view::redoLastEdit);
        palette.add(view.undoButton);
        palette.add(Box.createVerticalStrut(6));
        palette.add(view.redoButton);

        palette.add(Box.createVerticalStrut(12));
        palette.add(createPaletteSectionLabel("批量"));
        palette.add(createPaletteActionButton("⌦", "批量删除", view::handleBatchDeleteSelectedVertices));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteActionButton("⊘", "批量禁行", () -> view.handleBatchForbiddenBySelection(true)));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteActionButton("✓", "批量解禁", () -> view.handleBatchForbiddenBySelection(false)));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteActionButton("⇄", "禁行切换", view::handleQuickToggleSelectedEdgeForbidden));
        palette.add(Box.createVerticalStrut(12));
        palette.add(createPaletteSectionLabel("工具"));
        JButton toolsMenuButton = createPaletteButton("☰", "工具菜单");
        toolsMenuButton.addActionListener(e -> view.showWorkbenchToolsMenu(toolsMenuButton));
        view.adminOverlayButtons.add(toolsMenuButton);
        palette.add(toolsMenuButton);

        JScrollPane scrollPane = new JScrollPane(palette);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(toolbarBackground);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        configureOverlayScrollPane(scrollPane);

        JPanel paletteShell = new JPanel(new BorderLayout());
        paletteShell.setOpaque(true);
        paletteShell.setBackground(toolbarBackground);
        paletteShell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 216, 228)),
                BorderFactory.createEmptyBorder(12, 10, 12, 10)
        ));
        paletteShell.add(scrollPane, BorderLayout.CENTER);
        paletteShell.setPreferredSize(new Dimension(MainView.MAP_OVERLAY_TOOLBAR_WIDTH, palette.getPreferredSize().height + 24));
        updateAdminToolButtonStyle();
        view.refreshUndoRedoButtons();
        return paletteShell;
    }

    void configureOverlayScrollPane(JScrollPane scrollPane) {
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getVerticalScrollBar().setBlockIncrement(72);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.getVerticalScrollBar().setMinimumSize(new Dimension(0, 0));
        scrollPane.getVerticalScrollBar().setMaximumSize(new Dimension(0, Integer.MAX_VALUE));
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        scrollPane.getHorizontalScrollBar().setMinimumSize(new Dimension(0, 0));
        scrollPane.getHorizontalScrollBar().setMaximumSize(new Dimension(0, 0));
        scrollPane.getHorizontalScrollBar().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setBorder(BorderFactory.createEmptyBorder());
    }

    JButton createPaletteModeButton(EditToolMode mode, String glyph, String tooltip) {
        JButton button = createPaletteButton(glyph, tooltip);
        button.addActionListener(e -> setAdminEditMode(mode));
        view.adminToolButtons.put(mode, button);
        view.adminOverlayButtons.add(button);
        return button;
    }

    JButton createPaletteActionButton(String glyph, String tooltip, Runnable action) {
        JButton button = createPaletteButton(glyph, tooltip);
        button.addActionListener(e -> action.run());
        view.adminOverlayButtons.add(button);
        return button;
    }

    JButton createPaletteButton(String glyph, String tooltip) {
        JButton button = new JButton(glyph);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setFont(UiStyles.PALETTE_BUTTON_FONT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setPreferredSize(new Dimension(UiStyles.PALETTE_BUTTON_SIZE, UiStyles.PALETTE_BUTTON_SIZE));
        button.setMaximumSize(new Dimension(UiStyles.PALETTE_BUTTON_SIZE, UiStyles.PALETTE_BUTTON_SIZE));
        button.setMinimumSize(new Dimension(UiStyles.PALETTE_BUTTON_SIZE, UiStyles.PALETTE_BUTTON_SIZE));
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.PALETTE_BUTTON_BORDER),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        button.setBackground(UiStyles.PALETTE_BUTTON_BACKGROUND);
        button.setForeground(UiStyles.PALETTE_BUTTON_TEXT);
        button.setOpaque(true);
        return button;
    }

    JLabel createPaletteSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiStyles.CAPTION_FONT);
        label.setForeground(UiStyles.TEXT_SECONDARY);
        label.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 2));
        return label;
    }

    JButton createAdminSectionButton(String sectionKey, String text) {
        JButton button = UiStyles.ghostButton(text);
        button.setPreferredSize(new Dimension(0, UiStyles.ADMIN_SECTION_BUTTON_HEIGHT));
        button.setMinimumSize(new Dimension(0, UiStyles.ADMIN_SECTION_BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, UiStyles.ADMIN_SECTION_BUTTON_HEIGHT));
        button.addActionListener(e -> showAdminSection(sectionKey));
        view.adminSectionButtons.put(sectionKey, button);
        return button;
    }

    void styleAdminHeaderButton(JButton button) {
        button.setPreferredSize(new Dimension(UiStyles.ADMIN_HEADER_BUTTON_WIDTH, UiStyles.ADMIN_HEADER_BUTTON_HEIGHT));
        button.setMinimumSize(new Dimension(120, UiStyles.ADMIN_HEADER_BUTTON_HEIGHT));
        button.setMaximumSize(new Dimension(160, UiStyles.ADMIN_HEADER_BUTTON_HEIGHT));
    }

    void showAdminSection(String sectionKey) {
        if (view.adminWorkspaceLayout == null || view.adminWorkspacePanel == null) {
            return;
        }
        view.adminWorkspaceLayout.show(view.adminWorkspacePanel, sectionKey);
        for (Map.Entry<String, JButton> entry : view.adminSectionButtons.entrySet()) {
            applyAdminSectionButtonStyle(entry.getValue(), sectionKey.equals(entry.getKey()));
        }
    }

    void applyAdminSectionButtonStyle(JButton button, boolean active) {
        if (active) {
            button.setBackground(UiStyles.PRIMARY_SOFT);
            button.setForeground(UiStyles.PRIMARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiStyles.BRAND_500),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)
            ));
            return;
        }
        button.setBackground(UiStyles.SURFACE);
        button.setForeground(UiStyles.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
    }

    void setAdminEditMode(EditToolMode mode) {
        EditToolMode target = mode == null ? EditToolMode.SELECT : mode;
        view.viewState.setActiveEditToolMode(target);
        view.mapCanvas.setEditToolMode(target);
        view.viewState.setPendingEdgeStartVertexId(null);
        updateAdminToolButtonStyle();
        if (view.activeRoute == AppRoute.ADMIN_MODE) {
            view.mapWorkbenchView.setMapHint(view.resolveMapHint(view.activeRoute));
        }
    }

    void updateAdminToolButtonStyle() {
        for (Map.Entry<EditToolMode, JButton> entry : view.adminToolButtons.entrySet()) {
            applyAdminToolButtonStyle(entry.getValue(), entry.getKey() == view.viewState.getActiveEditToolMode());
        }
    }

    void applyAdminToolButtonStyle(JButton button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            button.setBackground(UiStyles.PALETTE_BUTTON_ACTIVE_BACKGROUND);
            button.setForeground(UiStyles.PALETTE_BUTTON_ACTIVE);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiStyles.PALETTE_BUTTON_ACTIVE, 3),
                    BorderFactory.createEmptyBorder(1, 1, 1, 1)
            ));
            button.setFont(UiStyles.PALETTE_BUTTON_ACTIVE_FONT);
            return;
        }
        button.setBackground(UiStyles.PALETTE_BUTTON_BACKGROUND);
        button.setForeground(UiStyles.PALETTE_BUTTON_TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.PALETTE_BUTTON_BORDER),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        button.setFont(UiStyles.PALETTE_BUTTON_FONT);
    }

}
