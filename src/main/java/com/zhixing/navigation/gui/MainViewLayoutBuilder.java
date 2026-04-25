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


final class MainViewLayoutBuilder {
    private final MainView view;

    MainViewLayoutBuilder(MainView view) {
        this.view = view;
    }

    void initializeFrame() {
        view.setTitle("知行校园智能路径规划系统");
        view.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        view.setMinimumSize(new Dimension(1440, 820));
        view.setSize(1600, 920);
        view.setLocationRelativeTo(null);
        view.setGlassPane(view.loadingOverlay);
    }

    void initializeLayout() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiStyles.PAGE_BACKGROUND);
        view.setContentPane(root);

        root.add(createTopBar(), BorderLayout.NORTH);

        JSplitPane shellSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftNavigation(), createCenterWorkspace());
        shellSplitPane.setResizeWeight(0.0);
        shellSplitPane.setContinuousLayout(true);
        UiStyles.applySplitPaneStyle(shellSplitPane, 6);
        SwingUtilities.invokeLater(() -> shellSplitPane.setDividerLocation(MainView.LEFT_NAV_WIDTH + 12));

        root.add(shellSplitPane, BorderLayout.CENTER);
    }

    JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(20, 0));
        topBar.setBackground(UiStyles.BRAND_700);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 36)),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandPanel.setOpaque(false);

        JLabel productBadge = new JLabel("知行", SwingConstants.CENTER);
        productBadge.setFont(UiStyles.SUBTITLE_FONT);
        productBadge.setForeground(Color.WHITE);
        productBadge.setOpaque(true);
        productBadge.setBackground(new Color(255, 255, 255, 30));
        productBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 55)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("知行校园导航工作台");
        title.setFont(UiStyles.TITLE_FONT);
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("Campus Routing & Map Editor");
        subtitle.setFont(UiStyles.CAPTION_FONT);
        subtitle.setForeground(UiStyles.TOP_BAR_SUBTLE_TEXT);

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(subtitle);

        brandPanel.add(productBadge);
        brandPanel.add(titleBlock);

        view.topModeBadgeLabel = new JLabel();
        view.topModeBadgeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        view.topModeDescriptionLabel = new JLabel();
        view.topModeDescriptionLabel.setFont(UiStyles.CAPTION_FONT);
        view.topModeDescriptionLabel.setForeground(UiStyles.TOP_BAR_SUBTLE_TEXT);
        view.topModeDescriptionLabel.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        view.topModeDescriptionLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        view.topSessionBadgeLabel = new JLabel();
        view.topSessionBadgeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel timeLabel = new JLabel("启动时间  " + view.startupTimeText, SwingConstants.RIGHT);
        timeLabel.setFont(UiStyles.CAPTION_FONT);
        timeLabel.setForeground(UiStyles.TOP_BAR_SUBTLE_TEXT);
        timeLabel.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);

        JPanel badgeRow = new JPanel();
        badgeRow.setOpaque(false);
        badgeRow.setLayout(new BoxLayout(badgeRow, BoxLayout.X_AXIS));
        badgeRow.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        badgeRow.add(Box.createHorizontalGlue());
        badgeRow.add(view.topModeBadgeLabel);
        badgeRow.add(Box.createHorizontalStrut(8));
        badgeRow.add(view.topSessionBadgeLabel);

        view.topStatusPanel = new JPanel();
        view.topStatusPanel.setOpaque(false);
        view.topStatusPanel.setLayout(new BoxLayout(view.topStatusPanel, BoxLayout.Y_AXIS));
        view.topStatusPanel.add(badgeRow);
        view.topStatusPanel.add(Box.createVerticalStrut(8));
        view.topStatusPanel.add(view.topModeDescriptionLabel);
        view.topStatusPanel.add(Box.createVerticalStrut(4));
        view.topStatusPanel.add(timeLabel);

        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(view.topStatusPanel, BorderLayout.EAST);
        view.updateShellHeaderState();
        return topBar;
    }

    JPanel createLeftNavigation() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setPreferredSize(new Dimension(MainView.LEFT_NAV_WIDTH, 0));
        shell.setMinimumSize(new Dimension(MainView.LEFT_NAV_MIN_WIDTH, 0));
        shell.setBackground(UiStyles.PAGE_BACKGROUND);
        shell.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 0));

        JPanel navigation = new ViewportFillPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBackground(UiStyles.PAGE_BACKGROUND);
        navigation.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel modeRail = UiStyles.cardPanel();
        modeRail.setLayout(new BoxLayout(modeRail, BoxLayout.Y_AXIS));
        modeRail.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        modeRail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        modeRail.add(view.createRailHeader("模式切换", "快速切换用户、管理员和系统视图"));
        modeRail.add(Box.createVerticalStrut(12));

        for (AppRoute route : AppRoute.values()) {
            JButton navButton = new JButton(route.getTitle());
            navButton.setFont(UiStyles.BODY_FONT);
            navButton.setFocusPainted(false);
            navButton.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            navButton.setHorizontalAlignment(SwingConstants.LEFT);
            navButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            navButton.setPreferredSize(new Dimension(0, 46));
            navButton.addActionListener(e -> view.navigateTo(route));
            view.navButtons.put(route, navButton);
            view.applyNavigationButtonStyle(navButton, false);
            modeRail.add(navButton);
            modeRail.add(Box.createVerticalStrut(8));
        }

        JPanel layerShell = UiStyles.cardPanel(new BorderLayout(0, 12));
        layerShell.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        layerShell.add(view.createRailHeader("地图图层", "显隐、锁定与渲染顺序控制"), BorderLayout.NORTH);
        view.layerPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        view.layerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 560));
        view.layerPanel.setOpaque(false);
        view.layerPanel.setBackground(UiStyles.SURFACE);
        view.layerPanel.setBorder(BorderFactory.createEmptyBorder());
        layerShell.add(view.layerPanel, BorderLayout.CENTER);

        navigation.add(modeRail);
        navigation.add(Box.createVerticalStrut(12));
        navigation.add(layerShell);
        navigation.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(navigation);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getViewport().setBackground(UiStyles.PAGE_BACKGROUND);

        shell.add(scrollPane, BorderLayout.CENTER);
        return shell;
    }

    JPanel createCenterWorkspace() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UiStyles.PAGE_BACKGROUND);
        center.setMinimumSize(new Dimension(980, 0));

        view.mapWorkbenchView.registerRoutePanel(AppRoute.USER_MODE, createUserModeView());
        view.mapWorkbenchView.registerRoutePanel(AppRoute.ADMIN_MODE, createAdminModeView());
        view.mapWorkbenchView.registerRoutePanel(AppRoute.SYSTEM_SETTINGS, view.createSystemSettingsView());
        view.mapWorkbenchView.setMapContent(createMapOverlayContent());

        center.add(view.mapWorkbenchView, BorderLayout.CENTER);
        return center;
    }

    JLayeredPane createMapOverlayContent() {
        view.mapLayeredPane = new JLayeredPane();
        view.mapLayeredPane.setOpaque(true);
        view.mapLayeredPane.setLayout(null);
        view.mapLayeredPane.add(view.mapCanvas, JLayeredPane.DEFAULT_LAYER);

        view.mapOverlayToolbar = view.createMapOverlayToolbar();
        view.mapLayeredPane.add(view.mapOverlayToolbar, JLayeredPane.PALETTE_LAYER);
        view.mapLayeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutMapOverlayComponents();
            }
        });
        SwingUtilities.invokeLater(view::layoutMapOverlayComponents);
        return view.mapLayeredPane;
    }

    void layoutMapOverlayComponents() {
        if (view.mapLayeredPane == null) {
            return;
        }
        int width = Math.max(0, view.mapLayeredPane.getWidth());
        int height = Math.max(0, view.mapLayeredPane.getHeight());
        view.mapCanvas.setBounds(0, 0, width, height);
        if (view.mapOverlayToolbar == null) {
            return;
        }
        Dimension preferred = view.mapOverlayToolbar.getPreferredSize();
        int availableHeight = Math.max(0, height - (MainView.MAP_OVERLAY_MARGIN * 2));
        int overlayWidth = preferred.width;
        int overlayHeight = Math.min(preferred.height, availableHeight);
        int x = Math.max(12, width - overlayWidth - MainView.MAP_OVERLAY_MARGIN);
        int y = MainView.MAP_OVERLAY_MARGIN;
        view.mapOverlayToolbar.setBounds(x, y, overlayWidth, overlayHeight);
        view.mapOverlayToolbar.revalidate();
        view.mapOverlayToolbar.repaint();
    }

    JPanel createUserModeView() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(UiStyles.PAGE_BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        view.pathQueryView = new PathQueryView();
        view.placeBrowseView = new PlaceBrowseView();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 12, 0);
        page.add(view.pathQueryView, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        page.add(view.placeBrowseView, gbc);

        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        page.add(filler, gbc);
        return page;
    }

    JPanel createAdminModeView() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionBar.setOpaque(false);
        JButton loginButton = UiStyles.primaryButton("管理员登录");
        view.styleAdminHeaderButton(loginButton);
        loginButton.addActionListener(e -> {
            if (view.showAdminLoginDialog()) {
                view.refreshAllData();
            }
        });
        JButton logoutButton = UiStyles.secondaryButton("退出登录");
        view.styleAdminHeaderButton(logoutButton);
        logoutButton.addActionListener(e -> view.handleAdminLogout());
        actionBar.add(loginButton);
        actionBar.add(logoutButton);

        view.adminCardLayout = new CardLayout();
        view.adminCardPanel = new JPanel(view.adminCardLayout);
        view.adminCardPanel.setBackground(UiStyles.PAGE_BACKGROUND);
        view.adminCardPanel.add(createAdminLockedPanel(), "LOCKED");
        view.adminCardPanel.add(createAdminWorkspace(), "UNLOCKED");

        page.add(actionBar, BorderLayout.NORTH);
        page.add(view.adminCardPanel, BorderLayout.CENTER);
        return page;
    }

    JPanel createAdminLockedPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UiStyles.SURFACE_ALT);
        panel.setBorder(UiStyles.cardBorder());
        JLabel label = new JLabel("请先完成管理员登录，再进行地点/道路/禁行管理。", SwingConstants.CENTER);
        label.setFont(UiStyles.SUBTITLE_FONT);
        label.setForeground(UiStyles.TEXT_SECONDARY);
        panel.add(label);
        return panel;
    }

    JPanel createAdminWorkspace() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UiStyles.PAGE_BACKGROUND);

        view.vertexManageView = new VertexManageView();
        view.roadManageView = new RoadManageView();
        view.forbiddenManageView = new ForbiddenManageView();
        view.overviewDashboardView = new OverviewDashboardView();

        JPanel sectionBar = UiStyles.cardPanel(new GridLayout(0, 2, 12, 12));
        sectionBar.setBackground(UiStyles.SURFACE_ALT);
        sectionBar.add(view.createAdminSectionButton(MainView.ADMIN_SECTION_VERTEX, "地点管理"));
        sectionBar.add(view.createAdminSectionButton(MainView.ADMIN_SECTION_ROAD, "道路管理"));
        sectionBar.add(view.createAdminSectionButton(MainView.ADMIN_SECTION_FORBIDDEN, "禁行管理"));
        sectionBar.add(view.createAdminSectionButton(MainView.ADMIN_SECTION_OVERVIEW, "地图概览"));

        JPanel workspaceHeader = new JPanel(new BorderLayout(0, 8));
        workspaceHeader.setBackground(UiStyles.PAGE_BACKGROUND);
        workspaceHeader.add(sectionBar, BorderLayout.NORTH);

        view.adminWorkspaceLayout = new CardLayout();
        view.adminWorkspacePanel = new JPanel(view.adminWorkspaceLayout);
        view.adminWorkspacePanel.setBackground(UiStyles.PAGE_BACKGROUND);
        view.adminWorkspacePanel.add(view.vertexManageView, MainView.ADMIN_SECTION_VERTEX);
        view.adminWorkspacePanel.add(view.roadManageView, MainView.ADMIN_SECTION_ROAD);
        view.adminWorkspacePanel.add(view.forbiddenManageView, MainView.ADMIN_SECTION_FORBIDDEN);
        view.adminWorkspacePanel.add(view.overviewDashboardView, MainView.ADMIN_SECTION_OVERVIEW);

        panel.add(workspaceHeader, BorderLayout.NORTH);
        panel.add(view.adminWorkspacePanel, BorderLayout.CENTER);

        view.showAdminSection(MainView.ADMIN_SECTION_VERTEX);
        view.setAdminEditMode(EditToolMode.SELECT);
        view.refreshUndoRedoButtons();
        return panel;
    }

}
