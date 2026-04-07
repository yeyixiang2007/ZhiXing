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

public class MainView extends JFrame {
    private static final String ADMIN_SECTION_VERTEX = "VERTEX";
    private static final String ADMIN_SECTION_ROAD = "ROAD";
    private static final String ADMIN_SECTION_FORBIDDEN = "FORBIDDEN";
    private static final String ADMIN_SECTION_OVERVIEW = "OVERVIEW";
    private static final int LEFT_NAV_WIDTH = 392;
    private static final int LEFT_NAV_MIN_WIDTH = 340;
    private static final int MAP_OVERLAY_MARGIN = 16;
    private static final int MAP_OVERLAY_TOOLBAR_WIDTH = 84;

    private final AuthController authController;
    private final NavigationController navigationController;
    private final MapController mapController;

    private final MapWorkbenchView mapWorkbenchView;
    private final MapCanvas mapCanvas;
    private final LayerPanel layerPanel;
    private final LoadingOverlay loadingOverlay;
    private final WorkbenchFeedback feedback;
    private final Map<AppRoute, JButton> navButtons;
    private final Map<String, JButton> adminSectionButtons;
    private final Map<EditToolMode, JButton> adminToolButtons;
    private final List<JButton> adminOverlayButtons;
    private final CommandBus<Admin> adminCommandBus;
    private final MapViewState viewState;
    private final MainViewAdminEditCoordinator adminEditCoordinator;
    private final String startupTimeText;

    private PathQueryView pathQueryView;
    private PlaceBrowseView placeBrowseView;
    private VertexManageView vertexManageView;
    private RoadManageView roadManageView;
    private ForbiddenManageView forbiddenManageView;
    private OverviewDashboardView overviewDashboardView;

    private Admin currentAdmin;
    private AppRoute activeRoute;
    private CardLayout adminCardLayout;
    private JPanel adminCardPanel;
    private CardLayout adminWorkspaceLayout;
    private JPanel adminWorkspacePanel;
    private JLayeredPane mapLayeredPane;
    private JPanel mapOverlayToolbar;
    private JButton undoButton;
    private JButton redoButton;
    private JLabel topModeBadgeLabel;
    private JLabel topModeDescriptionLabel;
    private JLabel topSessionBadgeLabel;
    private JPanel topStatusPanel;

    public MainView(CampusGraph graph, PersistenceService persistenceService) {
        AuthService authService = new AuthService(persistenceService);
        MapService mapService = new MapService(graph);
        NavigationService navigationService = new NavigationService(new DijkstraStrategy());
        ConsolePathFormatter formatter = new ConsolePathFormatter();

        this.authController = new AuthController(authService);
        this.navigationController = new NavigationController(graph, navigationService, formatter);
        this.mapController = new MapController(graph, mapService, persistenceService);
        this.mapWorkbenchView = new MapWorkbenchView();
        this.mapCanvas = new MapCanvas();
        this.layerPanel = new LayerPanel(mapCanvas);
        this.loadingOverlay = new LoadingOverlay();
        this.feedback = new WorkbenchFeedback(this, loadingOverlay, mapWorkbenchView::setStatus);
        this.navButtons = new EnumMap<AppRoute, JButton>(AppRoute.class);
        this.adminSectionButtons = new LinkedHashMap<String, JButton>();
        this.adminToolButtons = new EnumMap<EditToolMode, JButton>(EditToolMode.class);
        this.adminOverlayButtons = new ArrayList<JButton>();
        this.adminCommandBus = new CommandBus<Admin>();
        this.viewState = new MapViewState();
        this.activeRoute = AppRoute.USER_MODE;
        this.startupTimeText = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.adminEditCoordinator = new MainViewAdminEditCoordinator(
                mapController,
                feedback,
                viewState,
                adminCommandBus,
                () -> currentAdmin,
                this::ensureAdminLoggedIn,
                () -> activeRoute,
                this::refreshAllData,
                this::refreshUndoRedoButtons
        );

        UiStyles.installDefaults();
        initializeFrame();
        initializeLayout();
        wireViewEvents();
        installGlobalShortcuts();
        refreshAllData();
        navigateTo(AppRoute.USER_MODE);
    }

    private void initializeFrame() {
        setTitle("知行校园智能路径规划系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1440, 820));
        setSize(1600, 920);
        setLocationRelativeTo(null);
        setGlassPane(loadingOverlay);
    }

    private void initializeLayout() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiStyles.PAGE_BACKGROUND);
        setContentPane(root);

        root.add(createTopBar(), BorderLayout.NORTH);

        JSplitPane shellSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftNavigation(), createCenterWorkspace());
        shellSplitPane.setResizeWeight(0.0);
        shellSplitPane.setContinuousLayout(true);
        UiStyles.applySplitPaneStyle(shellSplitPane, 6);
        SwingUtilities.invokeLater(() -> shellSplitPane.setDividerLocation(LEFT_NAV_WIDTH + 12));

        root.add(shellSplitPane, BorderLayout.CENTER);
    }

    private JPanel createTopBar() {
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
        subtitle.setForeground(new Color(214, 232, 255));

        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(subtitle);

        brandPanel.add(productBadge);
        brandPanel.add(titleBlock);

        topModeBadgeLabel = new JLabel();
        topModeBadgeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        topModeDescriptionLabel = new JLabel();
        topModeDescriptionLabel.setFont(UiStyles.CAPTION_FONT);
        topModeDescriptionLabel.setForeground(new Color(214, 232, 255));
        topModeDescriptionLabel.setAlignmentX(RIGHT_ALIGNMENT);
        topModeDescriptionLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        topSessionBadgeLabel = new JLabel();
        topSessionBadgeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel timeLabel = new JLabel("启动时间  " + startupTimeText, SwingConstants.RIGHT);
        timeLabel.setFont(UiStyles.CAPTION_FONT);
        timeLabel.setForeground(new Color(214, 232, 255));
        timeLabel.setAlignmentX(RIGHT_ALIGNMENT);

        JPanel badgeRow = new JPanel();
        badgeRow.setOpaque(false);
        badgeRow.setLayout(new BoxLayout(badgeRow, BoxLayout.X_AXIS));
        badgeRow.setAlignmentX(RIGHT_ALIGNMENT);
        badgeRow.add(Box.createHorizontalGlue());
        badgeRow.add(topModeBadgeLabel);
        badgeRow.add(Box.createHorizontalStrut(8));
        badgeRow.add(topSessionBadgeLabel);

        topStatusPanel = new JPanel();
        topStatusPanel.setOpaque(false);
        topStatusPanel.setLayout(new BoxLayout(topStatusPanel, BoxLayout.Y_AXIS));
        topStatusPanel.add(badgeRow);
        topStatusPanel.add(Box.createVerticalStrut(8));
        topStatusPanel.add(topModeDescriptionLabel);
        topStatusPanel.add(Box.createVerticalStrut(4));
        topStatusPanel.add(timeLabel);

        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(topStatusPanel, BorderLayout.EAST);
        updateShellHeaderState();
        return topBar;
    }

    private JPanel createLeftNavigation() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setPreferredSize(new Dimension(LEFT_NAV_WIDTH, 0));
        shell.setMinimumSize(new Dimension(LEFT_NAV_MIN_WIDTH, 0));
        shell.setBackground(UiStyles.PAGE_BACKGROUND);
        shell.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 0));

        JPanel navigation = new ViewportFillPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBackground(UiStyles.PAGE_BACKGROUND);
        navigation.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel modeRail = UiStyles.cardPanel();
        modeRail.setLayout(new BoxLayout(modeRail, BoxLayout.Y_AXIS));
        modeRail.setAlignmentX(LEFT_ALIGNMENT);
        modeRail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        modeRail.add(createRailHeader("模式切换", "快速切换用户、管理员和系统视图"));
        modeRail.add(Box.createVerticalStrut(12));

        for (AppRoute route : AppRoute.values()) {
            JButton navButton = new JButton(route.getTitle());
            navButton.setFont(UiStyles.BODY_FONT);
            navButton.setFocusPainted(false);
            navButton.setAlignmentX(LEFT_ALIGNMENT);
            navButton.setHorizontalAlignment(SwingConstants.LEFT);
            navButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            navButton.setPreferredSize(new Dimension(0, 46));
            navButton.addActionListener(e -> navigateTo(route));
            navButtons.put(route, navButton);
            applyNavigationButtonStyle(navButton, false);
            modeRail.add(navButton);
            modeRail.add(Box.createVerticalStrut(8));
        }

        JPanel layerShell = UiStyles.cardPanel(new BorderLayout(0, 12));
        layerShell.setAlignmentX(LEFT_ALIGNMENT);
        layerShell.add(createRailHeader("地图图层", "显隐、锁定与渲染顺序控制"), BorderLayout.NORTH);
        layerPanel.setAlignmentX(LEFT_ALIGNMENT);
        layerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 560));
        layerPanel.setOpaque(false);
        layerPanel.setBackground(UiStyles.SURFACE);
        layerPanel.setBorder(BorderFactory.createEmptyBorder());
        layerShell.add(layerPanel, BorderLayout.CENTER);

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

    private JPanel createCenterWorkspace() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UiStyles.PAGE_BACKGROUND);
        center.setMinimumSize(new Dimension(980, 0));

        mapWorkbenchView.registerRoutePanel(AppRoute.USER_MODE, createUserModeView());
        mapWorkbenchView.registerRoutePanel(AppRoute.ADMIN_MODE, createAdminModeView());
        mapWorkbenchView.registerRoutePanel(AppRoute.SYSTEM_SETTINGS, createSystemSettingsView());
        mapWorkbenchView.setMapContent(createMapOverlayContent());

        center.add(mapWorkbenchView, BorderLayout.CENTER);
        return center;
    }

    private JLayeredPane createMapOverlayContent() {
        mapLayeredPane = new JLayeredPane();
        mapLayeredPane.setOpaque(true);
        mapLayeredPane.setLayout(null);
        mapLayeredPane.add(mapCanvas, JLayeredPane.DEFAULT_LAYER);

        mapOverlayToolbar = createMapOverlayToolbar();
        mapLayeredPane.add(mapOverlayToolbar, JLayeredPane.PALETTE_LAYER);
        mapLayeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutMapOverlayComponents();
            }
        });
        SwingUtilities.invokeLater(this::layoutMapOverlayComponents);
        return mapLayeredPane;
    }

    private void layoutMapOverlayComponents() {
        if (mapLayeredPane == null) {
            return;
        }
        int width = Math.max(0, mapLayeredPane.getWidth());
        int height = Math.max(0, mapLayeredPane.getHeight());
        mapCanvas.setBounds(0, 0, width, height);
        if (mapOverlayToolbar == null) {
            return;
        }
        Dimension preferred = mapOverlayToolbar.getPreferredSize();
        int availableHeight = Math.max(0, height - (MAP_OVERLAY_MARGIN * 2));
        int overlayWidth = preferred.width;
        int overlayHeight = Math.min(preferred.height, availableHeight);
        int x = Math.max(12, width - overlayWidth - MAP_OVERLAY_MARGIN);
        int y = MAP_OVERLAY_MARGIN;
        mapOverlayToolbar.setBounds(x, y, overlayWidth, overlayHeight);
        mapOverlayToolbar.revalidate();
        mapOverlayToolbar.repaint();
    }

    private JPanel createUserModeView() {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(UiStyles.PAGE_BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        pathQueryView = new PathQueryView();
        placeBrowseView = new PlaceBrowseView();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 12, 0);
        page.add(pathQueryView, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        page.add(placeBrowseView, gbc);

        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        page.add(filler, gbc);
        return page;
    }

    private JPanel createAdminModeView() {
        JPanel page = new JPanel(new BorderLayout(12, 12));
        page.setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionBar.setOpaque(false);
        JButton loginButton = UiStyles.primaryButton("管理员登录");
        styleAdminHeaderButton(loginButton);
        loginButton.addActionListener(e -> {
            if (showAdminLoginDialog()) {
                refreshAllData();
            }
        });
        JButton logoutButton = UiStyles.secondaryButton("退出登录");
        styleAdminHeaderButton(logoutButton);
        logoutButton.addActionListener(e -> handleAdminLogout());
        actionBar.add(loginButton);
        actionBar.add(logoutButton);

        adminCardLayout = new CardLayout();
        adminCardPanel = new JPanel(adminCardLayout);
        adminCardPanel.setBackground(UiStyles.PAGE_BACKGROUND);
        adminCardPanel.add(createAdminLockedPanel(), "LOCKED");
        adminCardPanel.add(createAdminWorkspace(), "UNLOCKED");

        page.add(actionBar, BorderLayout.NORTH);
        page.add(adminCardPanel, BorderLayout.CENTER);
        return page;
    }

    private JPanel createAdminLockedPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UiStyles.SURFACE_ALT);
        panel.setBorder(UiStyles.cardBorder());
        JLabel label = new JLabel("请先完成管理员登录，再进行地点/道路/禁行管理。", SwingConstants.CENTER);
        label.setFont(UiStyles.SUBTITLE_FONT);
        label.setForeground(UiStyles.TEXT_SECONDARY);
        panel.add(label);
        return panel;
    }

    private JPanel createAdminWorkspace() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(UiStyles.PAGE_BACKGROUND);

        vertexManageView = new VertexManageView();
        roadManageView = new RoadManageView();
        forbiddenManageView = new ForbiddenManageView();
        overviewDashboardView = new OverviewDashboardView();

        JPanel sectionBar = UiStyles.cardPanel(new GridLayout(0, 2, 12, 12));
        sectionBar.setBackground(UiStyles.SURFACE_ALT);
        sectionBar.add(createAdminSectionButton(ADMIN_SECTION_VERTEX, "地点管理"));
        sectionBar.add(createAdminSectionButton(ADMIN_SECTION_ROAD, "道路管理"));
        sectionBar.add(createAdminSectionButton(ADMIN_SECTION_FORBIDDEN, "禁行管理"));
        sectionBar.add(createAdminSectionButton(ADMIN_SECTION_OVERVIEW, "地图概览"));

        JPanel workspaceHeader = new JPanel(new BorderLayout(0, 8));
        workspaceHeader.setBackground(UiStyles.PAGE_BACKGROUND);
        workspaceHeader.add(sectionBar, BorderLayout.NORTH);

        adminWorkspaceLayout = new CardLayout();
        adminWorkspacePanel = new JPanel(adminWorkspaceLayout);
        adminWorkspacePanel.setBackground(UiStyles.PAGE_BACKGROUND);
        adminWorkspacePanel.add(vertexManageView, ADMIN_SECTION_VERTEX);
        adminWorkspacePanel.add(roadManageView, ADMIN_SECTION_ROAD);
        adminWorkspacePanel.add(forbiddenManageView, ADMIN_SECTION_FORBIDDEN);
        adminWorkspacePanel.add(overviewDashboardView, ADMIN_SECTION_OVERVIEW);

        panel.add(workspaceHeader, BorderLayout.NORTH);
        panel.add(adminWorkspacePanel, BorderLayout.CENTER);

        showAdminSection(ADMIN_SECTION_VERTEX);
        setAdminEditMode(EditToolMode.SELECT);
        refreshUndoRedoButtons();
        return panel;
    }

    private JPanel createMapOverlayToolbar() {
        Color toolbarBackground = new Color(255, 255, 255, 248);
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
        undoButton = createPaletteActionButton("↶", "撤销", this::undoLastEdit);
        redoButton = createPaletteActionButton("↷", "重做", this::redoLastEdit);
        palette.add(undoButton);
        palette.add(Box.createVerticalStrut(6));
        palette.add(redoButton);

        palette.add(Box.createVerticalStrut(12));
        palette.add(createPaletteSectionLabel("批量"));
        palette.add(createPaletteActionButton("⌦", "批量删除", this::handleBatchDeleteSelectedVertices));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteActionButton("⊘", "批量禁行", () -> handleBatchForbiddenBySelection(true)));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteActionButton("✓", "批量解禁", () -> handleBatchForbiddenBySelection(false)));
        palette.add(Box.createVerticalStrut(6));
        palette.add(createPaletteActionButton("⇄", "禁行切换", this::handleQuickToggleSelectedEdgeForbidden));
        palette.add(Box.createVerticalStrut(12));
        palette.add(createPaletteSectionLabel("工具"));
        JButton toolsMenuButton = createPaletteButton("☰", "工具菜单");
        toolsMenuButton.addActionListener(e -> showWorkbenchToolsMenu(toolsMenuButton));
        adminOverlayButtons.add(toolsMenuButton);
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
        paletteShell.setPreferredSize(new Dimension(MAP_OVERLAY_TOOLBAR_WIDTH, palette.getPreferredSize().height + 24));
        updateAdminToolButtonStyle();
        refreshUndoRedoButtons();
        return paletteShell;
    }

    private void configureOverlayScrollPane(JScrollPane scrollPane) {
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

    private JButton createPaletteModeButton(EditToolMode mode, String glyph, String tooltip) {
        JButton button = createPaletteButton(glyph, tooltip);
        button.addActionListener(e -> setAdminEditMode(mode));
        adminToolButtons.put(mode, button);
        adminOverlayButtons.add(button);
        return button;
    }

    private JButton createPaletteActionButton(String glyph, String tooltip, Runnable action) {
        JButton button = createPaletteButton(glyph, tooltip);
        button.addActionListener(e -> action.run());
        adminOverlayButtons.add(button);
        return button;
    }

    private JButton createPaletteButton(String glyph, String tooltip) {
        JButton button = new JButton(glyph);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setPreferredSize(new Dimension(46, 46));
        button.setMaximumSize(new Dimension(46, 46));
        button.setMinimumSize(new Dimension(46, 46));
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 223, 234)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        button.setBackground(new Color(249, 251, 253));
        button.setForeground(new Color(55, 69, 87));
        button.setOpaque(true);
        return button;
    }

    private JLabel createPaletteSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiStyles.CAPTION_FONT);
        label.setForeground(UiStyles.TEXT_SECONDARY);
        label.setAlignmentX(CENTER_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 6, 2));
        return label;
    }

    private JButton createAdminSectionButton(String sectionKey, String text) {
        JButton button = UiStyles.ghostButton(text);
        button.setPreferredSize(new Dimension(0, 44));
        button.setMinimumSize(new Dimension(0, 44));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        button.addActionListener(e -> showAdminSection(sectionKey));
        adminSectionButtons.put(sectionKey, button);
        return button;
    }

    private void styleAdminHeaderButton(JButton button) {
        button.setPreferredSize(new Dimension(132, 40));
        button.setMinimumSize(new Dimension(120, 40));
        button.setMaximumSize(new Dimension(160, 40));
    }

    private void showAdminSection(String sectionKey) {
        if (adminWorkspaceLayout == null || adminWorkspacePanel == null) {
            return;
        }
        adminWorkspaceLayout.show(adminWorkspacePanel, sectionKey);
        for (Map.Entry<String, JButton> entry : adminSectionButtons.entrySet()) {
            applyAdminSectionButtonStyle(entry.getValue(), sectionKey.equals(entry.getKey()));
        }
    }

    private void applyAdminSectionButtonStyle(JButton button, boolean active) {
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

    private void setAdminEditMode(EditToolMode mode) {
        EditToolMode target = mode == null ? EditToolMode.SELECT : mode;
        viewState.setActiveEditToolMode(target);
        mapCanvas.setEditToolMode(target);
        viewState.setPendingEdgeStartVertexId(null);
        updateAdminToolButtonStyle();
        if (activeRoute == AppRoute.ADMIN_MODE) {
            mapWorkbenchView.setMapHint(resolveMapHint(activeRoute));
        }
    }

    private void updateAdminToolButtonStyle() {
        for (Map.Entry<EditToolMode, JButton> entry : adminToolButtons.entrySet()) {
            applyAdminToolButtonStyle(entry.getValue(), entry.getKey() == viewState.getActiveEditToolMode());
        }
    }

    private void applyAdminToolButtonStyle(JButton button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            button.setBackground(new Color(223, 236, 251));
            button.setForeground(new Color(17, 58, 115));
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(17, 58, 115), 3),
                    BorderFactory.createEmptyBorder(1, 1, 1, 1)
            ));
            button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            return;
        }
        button.setBackground(new Color(249, 251, 253));
        button.setForeground(new Color(55, 69, 87));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 223, 234)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
    }

    private JPanel createSystemSettingsView() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel form = UiStyles.cardPanel(new GridBagLayout());
        form.setBackground(UiStyles.SURFACE_ALT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField dataDirField = UiStyles.formField(28);
        dataDirField.setEditable(false);
        dataDirField.setText(mapController.loadOverview().getDataDir());

        addFormRow(form, gbc, 0, "数据目录", dataDirField);
        JLabel movedLabel = UiStyles.formLabel("备份/恢复入口已迁移到「管理员模式 -> 地图编辑工具 -> 工具菜单」。");
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1;
        form.add(movedLabel, gbc);
        page.add(form, BorderLayout.NORTH);
        return page;
    }

    private void wireViewEvents() {
        bindPathQueryEvents();
        bindMapCanvasEvents();
        bindVertexManageEvents();
        bindRoadManageEvents();
        bindForbiddenManageEvents();
        bindOverviewAndLayerEvents();
    }

    private void bindPathQueryEvents() {
        pathQueryView.setListener(new PathQueryView.Listener() {
            @Override
            public void onQuery(String startId, String endId) {
                handlePathQuery(startId, endId);
            }

            @Override
            public void onMapPickTargetChanged(PathQueryView.MapPickTarget target) {
                if (target == PathQueryView.MapPickTarget.START) {
                    mapWorkbenchView.setMapHint("地图选点模式：请在地图中点击起点。");
                } else if (target == PathQueryView.MapPickTarget.END) {
                    mapWorkbenchView.setMapHint("地图选点模式：请在地图中点击终点。");
                } else {
                    mapWorkbenchView.setMapHint(resolveMapHint(activeRoute));
                }
            }

            @Override
            public void onInstructionSelected(int index) {
                int segmentIndex = index;
                RouteVisualizationDto currentRouteVisualization = viewState.getCurrentRouteVisualization();
                if (currentRouteVisualization != null && currentRouteVisualization.getSegmentCount() > 0) {
                    int maxSegment = currentRouteVisualization.getSegmentCount() - 1;
                    if (segmentIndex > maxSegment) {
                        segmentIndex = maxSegment;
                    }
                }
                mapCanvas.focusRouteSegment(segmentIndex);
                feedback.setStatus("步骤联动: 已定位到第 " + (index + 1) + " 步");
            }
        });
        placeBrowseView.setListener(this::refreshPlaceData);
    }

    private void bindMapCanvasEvents() {
        mapCanvas.setListener(new MapCanvas.Listener() {
            @Override
            public void onSelectionChanged(List<String> selectedVertexIds, String selectedEdgeKey) {
                viewState.setSelectedVertexIds(selectedVertexIds);
                viewState.setSelectedEdgeKey(selectedEdgeKey);
                if (activeRoute == AppRoute.ADMIN_MODE) {
                    handleAdminMapSelectionContext();
                    return;
                }
                if (selectedEdgeKey != null) {
                    feedback.setStatus("地图选择: " + selectedEdgeKey);
                    return;
                }
                if (selectedVertexIds.isEmpty()) {
                    feedback.setStatus("地图选择: 无");
                    return;
                }
                feedback.setStatus("地图选择: " + String.join(", ", selectedVertexIds));
            }

            @Override
            public void onViewportChanged(double zoom, double panX, double panY) {
                int percent = (int) Math.round(zoom * 100.0);
                feedback.setStatus("地图视图: 缩放 " + percent + "%, 平移(" + (int) Math.round(panX) + "," + (int) Math.round(panY) + ")");
            }

            @Override
            public void onVertexActivated(String vertexId) {
                handleMapVertexActivation(vertexId);
            }

            @Override
            public void onAddVertexRequested(double x, double y) {
                handleMapAddVertex(x, y);
            }

            @Override
            public void onConnectVerticesRequested(String fromId, String toId) {
                handleMapConnectVertices(fromId, toId);
            }

            @Override
            public void onMoveVertexRequested(String vertexId, double x, double y) {
                handleMapMoveVertex(vertexId, x, y);
            }

            @Override
            public void onDeleteVertexRequested(String vertexId) {
                handleMapDeleteVertex(vertexId);
            }

            @Override
            public void onDeleteEdgeRequested(String edgeKey) {
                handleMapDeleteEdge(edgeKey);
            }

            @Override
            public void onEdgeDraftChanged(String startVertexId) {
                viewState.setPendingEdgeStartVertexId(startVertexId);
                if (activeRoute == AppRoute.ADMIN_MODE && viewState.getActiveEditToolMode() == EditToolMode.ADD_EDGE) {
                    if (startVertexId == null) {
                        feedback.setStatus("管理员连线模式: 请选择第一点");
                    } else {
                        feedback.setStatus("管理员连线模式: 起点已选 " + startVertexId + "，请点击终点");
                    }
                }
            }

            @Override
            public void onCanvasHint(String message) {
                feedback.info(message);
                feedback.setStatus("画布提示: " + message);
            }
        });
    }

    private void bindVertexManageEvents() {
        vertexManageView.setListener(new VertexManageView.Listener() {
            @Override
            public void onAdd(VertexManageView.VertexFormData formData) {
                handleAddVertexFromForm(formData);
            }

            @Override
            public void onUpdate(VertexManageView.VertexFormData formData) {
                handleUpdateVertexFromForm(formData);
            }

            @Override
            public void onDelete(String id) {
                handleDeleteVertexById(id);
            }
        });
    }

    private void bindRoadManageEvents() {
        roadManageView.setListener(new RoadManageView.Listener() {
            @Override
            public void onAdd(RoadManageView.RoadFormData data) {
                handleAddRoadFromForm(data);
            }

            @Override
            public void onUpdate(RoadManageView.RoadFormData data) {
                handleUpdateRoadFromForm(data);
            }

            @Override
            public void onDelete(RoadManageView.RoadFormData data) {
                handleDeleteRoadFromForm(data);
            }
        });
    }

    private void bindForbiddenManageEvents() {
        forbiddenManageView.setListener(new ForbiddenManageView.Listener() {
            @Override
            public void onToggleForbidden(RoadOption roadOption, boolean forbidden) {
                if (roadOption == null) {
                    feedback.showErrorDialog("参数缺失", "当前没有可操作的道路。");
                    return;
                }
                handleSetRoadForbidden(roadOption.getFromId(), roadOption.getToId(), forbidden);
            }

            @Override
            public void onRefresh() {
                refreshForbiddenData();
            }
        });
    }

    private void bindOverviewAndLayerEvents() {
        overviewDashboardView.setListener(this::refreshOverviewData);
        layerPanel.setListener(message -> {
            feedback.info(message);
            feedback.setStatus("图层面板: " + message);
        });
    }

    private void installGlobalShortcuts() {
        JComponent root = getRootPane();
        bindShortcut(root, "shortcut-delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleShortcutDelete();
            }
        });
        bindShortcut(root, "shortcut-undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (activeRoute == AppRoute.ADMIN_MODE) {
                    undoLastEdit();
                }
            }
        });
        bindShortcut(root, "shortcut-redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (activeRoute == AppRoute.ADMIN_MODE) {
                    redoLastEdit();
                }
            }
        });
    }

    private void bindShortcut(JComponent component, String actionKey, KeyStroke keyStroke, Action action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionKey);
        component.getActionMap().put(actionKey, action);
    }

    private void handleShortcutDelete() {
        if (!isAdminEditingAvailable()) {
            return;
        }
        String mapSelectedEdgeKey = viewState.getSelectedEdgeKey();
        List<String> mapSelectedVertexIds = viewState.getSelectedVertexIds();
        if (mapSelectedEdgeKey != null) {
            handleMapDeleteEdge(mapSelectedEdgeKey);
            return;
        }
        if (mapSelectedVertexIds.isEmpty()) {
            feedback.info("Delete: 当前未选中对象。");
            return;
        }
        if (mapSelectedVertexIds.size() == 1) {
            handleMapDeleteVertex(mapSelectedVertexIds.get(0));
            return;
        }
        handleBatchDeleteSelectedVertices();
    }

    private void handleMapVertexActivation(String vertexId) {
        if (activeRoute != AppRoute.USER_MODE) {
            return;
        }
        if (pathQueryView.mapPickTarget() == PathQueryView.MapPickTarget.NONE) {
            return;
        }
        pathQueryView.pickVertex(vertexId);
        if (pathQueryView.mapPickTarget() == PathQueryView.MapPickTarget.NONE) {
            mapWorkbenchView.setMapHint(resolveMapHint(activeRoute));
        } else if (pathQueryView.mapPickTarget() == PathQueryView.MapPickTarget.START) {
            mapWorkbenchView.setMapHint("地图选点模式：请在地图中点击起点。");
        } else {
            mapWorkbenchView.setMapHint("地图选点模式：请在地图中点击终点。");
        }
    }

    private void handleAdminMapSelectionContext() {
        String mapSelectedEdgeKey = viewState.getSelectedEdgeKey();
        List<String> mapSelectedVertexIds = viewState.getSelectedVertexIds();
        if (mapSelectedEdgeKey != null) {
            Edge selectedEdge = adminEditCoordinator.findRoadByEdgeKey(mapSelectedEdgeKey);
            if (selectedEdge != null) {
                roadManageView.fillFromEdge(selectedEdge);
                showAdminSection(ADMIN_SECTION_ROAD);
                feedback.setStatus("地图选择: 道路 " + selectedEdge.getFromVertex().getId() + " -> " + selectedEdge.getToVertex().getId());
                return;
            }
        }
        if (mapSelectedVertexIds.size() == 1) {
            Vertex selectedVertex = adminEditCoordinator.findVertexById(mapSelectedVertexIds.get(0));
            if (selectedVertex != null) {
                vertexManageView.fillFromVertex(selectedVertex);
                showAdminSection(ADMIN_SECTION_VERTEX);
                feedback.setStatus("地图选择: 点位 " + selectedVertex.getId());
                return;
            }
        }
        if (mapSelectedVertexIds.size() > 1) {
            feedback.setStatus("地图选择: 已选中 " + mapSelectedVertexIds.size() + " 个点位（可批量操作）");
            return;
        }
        feedback.setStatus("地图选择: 无");
    }

    private void handleAddVertexFromForm(VertexManageView.VertexFormData formData) {
        adminEditCoordinator.handleAddVertexFromForm(formData);
    }

    private void handleUpdateVertexFromForm(VertexManageView.VertexFormData formData) {
        adminEditCoordinator.handleUpdateVertexFromForm(formData);
    }

    private void handleDeleteVertexById(String id) {
        adminEditCoordinator.handleDeleteVertexById(id);
    }

    private void handleAddRoadFromForm(RoadManageView.RoadFormData data) {
        adminEditCoordinator.handleAddRoadFromForm(data);
    }

    private void handleUpdateRoadFromForm(RoadManageView.RoadFormData data) {
        adminEditCoordinator.handleUpdateRoadFromForm(data);
    }

    private void handleDeleteRoadFromForm(RoadManageView.RoadFormData data) {
        adminEditCoordinator.handleDeleteRoadFromForm(data);
    }

    private void handleSetRoadForbidden(String fromId, String toId, boolean forbidden) {
        adminEditCoordinator.handleSetRoadForbidden(fromId, toId, forbidden);
    }

    private void handleMapAddVertex(double x, double y) {
        adminEditCoordinator.handleMapAddVertex(x, y);
    }

    private void handleMapConnectVertices(String fromId, String toId) {
        adminEditCoordinator.handleMapConnectVertices(fromId, toId);
    }

    private void handleMapMoveVertex(String vertexId, double x, double y) {
        adminEditCoordinator.handleMapMoveVertex(vertexId, x, y);
    }

    private void handleMapDeleteVertex(String vertexId) {
        adminEditCoordinator.handleMapDeleteVertex(vertexId);
    }

    private void handleMapDeleteEdge(String edgeKey) {
        adminEditCoordinator.handleMapDeleteEdge(edgeKey);
    }

    private void handleBatchDeleteSelectedVertices() {
        adminEditCoordinator.handleBatchDeleteSelectedVertices();
    }

    private void handleBatchForbiddenBySelection(boolean forbidden) {
        adminEditCoordinator.handleBatchForbiddenBySelection(forbidden);
    }

    private void handleQuickToggleSelectedEdgeForbidden() {
        adminEditCoordinator.handleQuickToggleSelectedEdgeForbidden();
    }

    private void undoLastEdit() {
        adminEditCoordinator.undoLastEdit();
    }

    private void redoLastEdit() {
        adminEditCoordinator.redoLastEdit();
    }

    private void refreshUndoRedoButtons() {
        if (undoButton != null) {
            undoButton.setEnabled(currentAdmin != null && adminEditCoordinator.canUndo());
        }
        if (redoButton != null) {
            redoButton.setEnabled(currentAdmin != null && adminEditCoordinator.canRedo());
        }
    }

    private boolean isAdminEditingAvailable() {
        return adminEditCoordinator.isAdminEditingAvailable();
    }

    private Vertex requireVertex(String vertexId) {
        Vertex vertex = adminEditCoordinator.findVertexById(vertexId);
        if (vertex != null) {
            return vertex;
        }
        throw new IllegalArgumentException("未找到点位: " + vertexId);
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private void handlePathQuery(String startId, String endId) {
        if (isBlank(startId) || isBlank(endId)) {
            feedback.showErrorDialog("参数缺失", "请先选择起点与终点。");
            return;
        }
        try {
            feedback.showLoading("正在计算最短路径...");
            NavigationController.NavigationVisualResult result = navigationController.queryPathVisual(startId, endId);
            PathResult pathResult = result.getPathResult();
            pathQueryView.setResultContent(navigationController.format(pathResult));
            pathQueryView.setInstructions(pathResult.getNaviInstructions());
            if (viewState.getCurrentPathResult() == null) {
                viewState.setPreviousRouteVisualization(null);
            } else {
                viewState.setPreviousRouteVisualization(navigationController.toTraceRouteVisualization(viewState.getCurrentPathResult()));
            }
            viewState.setCurrentPathResult(pathResult);
            viewState.setCurrentRouteVisualization(result.getRouteVisualization());
            mapCanvas.setRouteComparison(viewState.getCurrentRouteVisualization(), viewState.getPreviousRouteVisualization());
            feedback.success("路径查询成功。");
            feedback.setStatus("用户模式: 路径查询完成");
        } catch (RuntimeException ex) {
            feedback.showOperationError("查询失败", ex);
            feedback.setStatus("用户模式: 路径查询失败");
        } finally {
            feedback.hideLoading();
        }
    }

    private void refreshAllData() {
        refreshMapCanvas();
        refreshPathOptions();
        refreshPlaceData(placeBrowseView.selectedType());
        refreshVertexData();
        refreshRoadData();
        refreshForbiddenData();
        refreshOverviewData();
        updateAdminAccessUi();
        refreshUndoRedoButtons();
        if (activeRoute == AppRoute.ADMIN_MODE) {
            handleAdminMapSelectionContext();
        }
    }

    private void refreshPathOptions() {
        String selectedStart = pathQueryView.selectedStartId();
        String selectedEnd = pathQueryView.selectedEndId();
        List<VertexOption> options = mapController.listVertexOptions();
        pathQueryView.setVertexOptions(options, selectedStart, selectedEnd);
    }

    private void refreshMapCanvas() {
        List<Vertex> vertices = mapController.listVertices();
        List<Edge> roads = mapController.listRoads();
        mapCanvas.setGraphData(vertices, roads);
        mapCanvas.setRouteComparison(viewState.getCurrentRouteVisualization(), viewState.getPreviousRouteVisualization());
    }

    private void refreshPlaceData(String selectedType) {
        List<com.zhixing.navigation.domain.model.Vertex> vertices;
        if (PlaceBrowseView.ALL_PLACE_TYPES.equals(selectedType)) {
            vertices = mapController.listVertices();
        } else {
            vertices = mapController.listVerticesByType(PlaceType.valueOf(selectedType));
        }
        placeBrowseView.setPlaces(vertices);
    }

    private void refreshVertexData() {
        vertexManageView.setVertices(mapController.listVertices());
    }

    private void refreshRoadData() {
        String selectedFrom = roadManageView.selectedFromId();
        String selectedTo = roadManageView.selectedToId();
        roadManageView.setVertexOptions(mapController.listVertexOptions(), selectedFrom, selectedTo);
        roadManageView.setRoads(mapController.listRoads());
    }

    private void refreshForbiddenData() {
        String selectedKey = forbiddenManageView.selectedRoadKey();
        forbiddenManageView.setRoadOptions(mapController.listRoadOptions(), selectedKey);
    }

    private void refreshOverviewData() {
        OverviewData overview = mapController.loadOverview();
        overviewDashboardView.setOverviewData(overview);
    }

    private void showWorkbenchToolsMenu(JButton anchorButton) {
        if (anchorButton == null) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem importReferenceImageItem = new JMenuItem("导入参考图...");
        importReferenceImageItem.addActionListener(e -> promptImportReferenceImage());
        JMenuItem clearReferenceImageItem = new JMenuItem("清除参考图");
        clearReferenceImageItem.addActionListener(e -> handleClearReferenceImage());
        JMenuItem setReferenceScaleItem = new JMenuItem("设置参考图比例...");
        setReferenceScaleItem.addActionListener(e -> promptSetReferenceImageScale());
        JMenuItem setMapScaleItem = new JMenuItem("设置比例尺...");
        setMapScaleItem.addActionListener(e -> promptSetMetersPerWorldUnit());
        JMenuItem calibrateScaleItem = new JMenuItem("比例尺校准...");
        calibrateScaleItem.addActionListener(e -> promptCalibrateMetersPerWorldUnit());
        JMenuItem backupItem = new JMenuItem("执行备份...");
        backupItem.addActionListener(e -> promptBackupFromWorkbenchTools());
        JMenuItem restoreItem = new JMenuItem("执行恢复...");
        restoreItem.addActionListener(e -> promptRestoreFromWorkbenchTools());
        menu.add(importReferenceImageItem);
        menu.add(clearReferenceImageItem);
        menu.add(setReferenceScaleItem);
        menu.add(setMapScaleItem);
        menu.add(calibrateScaleItem);
        menu.addSeparator();
        menu.add(backupItem);
        menu.add(restoreItem);
        menu.show(anchorButton, 0, anchorButton.getHeight());
    }

    private void promptImportReferenceImage() {
        if (!ensureAdminLoggedIn()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择参考图");
        chooser.setFileFilter(new FileNameExtensionFilter("图片文件", "png", "jpg", "jpeg", "bmp", "gif"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null) {
            return;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                feedback.showErrorDialog("导入失败", "文件不是有效图片。");
                return;
            }
            mapCanvas.setReferenceImage(image);
            feedback.success("参考图导入成功。");
            feedback.setStatus("工作台工具: 已导入参考图");
        } catch (IOException ex) {
            feedback.showOperationError("导入失败", ex);
            feedback.setStatus("工作台工具: 参考图导入失败");
        }
    }

    private void handleClearReferenceImage() {
        if (!ensureAdminLoggedIn()) {
            return;
        }
        if (!mapCanvas.hasReferenceImage()) {
            feedback.info("当前没有已导入的参考图。");
            return;
        }
        mapCanvas.clearReferenceImage();
        feedback.info("已清除参考图。");
        feedback.setStatus("工作台工具: 已清除参考图");
    }

    private void promptSetReferenceImageScale() {
        if (!ensureAdminLoggedIn()) {
            return;
        }
        String defaultValue = String.format("%.2f", mapCanvas.getReferenceImageScale());
        String input = JOptionPane.showInputDialog(this, "请输入参考图比例（如 1.0）：", defaultValue);
        if (input == null) {
            return;
        }
        try {
            double scale = parsePositiveDouble(input, "参考图比例");
            mapCanvas.setReferenceImageScale(scale);
            feedback.success("参考图比例已更新。");
            feedback.setStatus("工作台工具: 参考图比例=" + String.format("%.2f", scale));
        } catch (RuntimeException ex) {
            feedback.showOperationError("设置失败", ex);
            feedback.setStatus("工作台工具: 参考图比例设置失败");
        }
    }

    private void promptSetMetersPerWorldUnit() {
        if (!ensureAdminLoggedIn()) {
            return;
        }
        String defaultValue = String.format("%.2f", mapCanvas.getMetersPerWorldUnit());
        String input = JOptionPane.showInputDialog(this, "请输入比例尺（每 1 坐标单位=多少米）：", defaultValue);
        if (input == null) {
            return;
        }
        try {
            double metersPerUnit = parsePositiveDouble(input, "比例尺");
            mapCanvas.setMetersPerWorldUnit(metersPerUnit);
            feedback.success("比例尺已更新。");
            feedback.setStatus("工作台工具: 比例尺已更新");
        } catch (RuntimeException ex) {
            feedback.showOperationError("设置失败", ex);
            feedback.setStatus("工作台工具: 比例尺设置失败");
        }
    }

    private void promptCalibrateMetersPerWorldUnit() {
        if (!ensureAdminLoggedIn()) {
            return;
        }
        List<String> selectedVertexIds = viewState.getSelectedVertexIds();
        String defaultFromId = selectedVertexIds.size() >= 1 ? selectedVertexIds.get(0) : "";
        String defaultToId = selectedVertexIds.size() >= 2 ? selectedVertexIds.get(1) : "";

        JTextField fromIdField = new JTextField(defaultFromId, 12);
        JTextField toIdField = new JTextField(defaultToId, 12);
        JTextField metersField = new JTextField("100", 12);
        Object[] message = new Object[]{
                "参考点A（节点ID）", fromIdField,
                "参考点B（节点ID）", toIdField,
                "A-B真实距离（米）", metersField
        };
        int result = JOptionPane.showConfirmDialog(
                this,
                message,
                "比例尺校准",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            String fromId = safeTrim(fromIdField.getText());
            String toId = safeTrim(toIdField.getText());
            if (isBlank(fromId) || isBlank(toId)) {
                throw new IllegalArgumentException("请先输入两个节点ID。");
            }
            if (fromId.equals(toId)) {
                throw new IllegalArgumentException("两个节点ID不能相同。");
            }
            double realMeters = parsePositiveDouble(metersField.getText(), "真实距离");
            Vertex from = requireVertex(fromId);
            Vertex to = requireVertex(toId);
            double worldDistance = Math.hypot(to.getX() - from.getX(), to.getY() - from.getY());
            if (worldDistance <= 0) {
                throw new IllegalArgumentException("两点在地图上的距离为0，无法校准比例尺。");
            }
            double metersPerUnit = realMeters / worldDistance;
            mapCanvas.setMetersPerWorldUnit(metersPerUnit);
            feedback.success("比例尺校准完成。");
            feedback.setStatus(String.format(
                    "工作台工具: 比例尺=%.4f 米/单位（参考 %s-%s）",
                    metersPerUnit,
                    fromId,
                    toId
            ));
        } catch (RuntimeException ex) {
            feedback.showOperationError("校准失败", ex);
            feedback.setStatus("工作台工具: 比例尺校准失败");
        }
    }

    private void promptBackupFromWorkbenchTools() {
        if (!ensureAdminLoggedIn()) {
            return;
        }
        String defaultName = "backup-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String input = JOptionPane.showInputDialog(this, "请输入备份名称：", defaultName);
        if (input == null) {
            return;
        }
        handleBackup(input, "工作台工具");
    }

    private void promptRestoreFromWorkbenchTools() {
        if (!ensureAdminLoggedIn()) {
            return;
        }
        String input = JOptionPane.showInputDialog(this, "请输入要恢复的备份名称：", "");
        if (input == null) {
            return;
        }
        handleRestore(input, "工作台工具");
    }

    private void handleBackup(String backupName, String sourceTag) {
        try {
            feedback.showLoading("正在执行数据备份...");
            mapController.backupData(backupName);
            feedback.success("数据备份成功。");
            feedback.setStatus(sourceTag + ": 数据备份完成");
        } catch (RuntimeException ex) {
            feedback.showOperationError("备份失败", ex);
            feedback.setStatus(sourceTag + ": 数据备份失败");
        } finally {
            feedback.hideLoading();
        }
    }

    private void handleRestore(String backupName, String sourceTag) {
        if (!feedback.confirm("确认恢复", "恢复会覆盖当前数据，确定继续吗？")) {
            return;
        }
        try {
            feedback.showLoading("正在恢复数据备份...");
            mapController.restoreData(backupName);
            feedback.warning("数据恢复成功，请重启应用以完全生效。");
            feedback.setStatus(sourceTag + ": 数据恢复完成");
        } catch (RuntimeException ex) {
            feedback.showOperationError("恢复失败", ex);
            feedback.setStatus(sourceTag + ": 数据恢复失败");
        } finally {
            feedback.hideLoading();
        }
    }

    private void navigateTo(AppRoute route) {
        activeRoute = route;
        viewState.setActiveRoute(route);
        if (route == AppRoute.ADMIN_MODE && currentAdmin == null) {
            showAdminLoginDialog();
        }
        mapWorkbenchView.showRoute(route);
        mapWorkbenchView.setMapHint(resolveMapHint(route));
        feedback.setStatus("当前页面: " + route.getTitle());
        updateNavState(route);
        updateAdminAccessUi();
        updateMapOverlayToolbarVisibility();
        updateShellHeaderState();
    }

    private String resolveMapHint(AppRoute route) {
        if (route == AppRoute.USER_MODE) {
            return "用户模式：左键框选/点选，右键拖拽平移，滚轮缩放。";
        }
        if (route == AppRoute.ADMIN_MODE) {
            if (currentAdmin == null) {
                return "管理员模式：请先登录，登录后可进行地图编辑与数据维护。";
            }
            EditToolMode activeEditToolMode = viewState.getActiveEditToolMode();
            if (activeEditToolMode == EditToolMode.ADD_VERTEX) {
                return "管理员模式-添加点：点击地图空白区域创建点位。";
            }
            if (activeEditToolMode == EditToolMode.ADD_EDGE) {
                String pendingEdgeStartVertexId = viewState.getPendingEdgeStartVertexId();
                if (pendingEdgeStartVertexId == null) {
                    return "管理员模式-连线：点击起点，再点击终点创建道路。";
                }
                return "管理员模式-连线：起点已选 " + pendingEdgeStartVertexId + "，请点击终点。";
            }
            if (activeEditToolMode == EditToolMode.MOVE_VERTEX) {
                return "管理员模式-移动点：拖拽点位到新位置（支持网格吸附与轴向对齐提示）。";
            }
            if (activeEditToolMode == EditToolMode.DELETE_OBJECT) {
                return "管理员模式-删除对象：点击点位或道路立即删除（Delete 快捷键可快速删除）。";
            }
            return "管理员模式-选择：可框选多点，支持批量删除与批量禁行；Ctrl+Z/Ctrl+Y 撤销重做，按住 Space 可拖拽平移。";
        }
        return "系统设置：可查看数据目录，备份恢复请前往管理员模式的工具菜单。";
    }

    private boolean showAdminLoginDialog() {
        AdminLoginDialog dialog = new AdminLoginDialog(this, authController);
        Admin admin = dialog.showDialog();
        if (admin == null) {
            feedback.info("管理员未登录。");
            feedback.setStatus("管理员模式: 未登录");
            updateAdminAccessUi();
            return false;
        }
        currentAdmin = admin;
        feedback.success("管理员登录成功：" + currentAdmin.getUsername());
        feedback.setStatus("管理员模式: 已登录");
        updateAdminAccessUi();
        return true;
    }

    private boolean ensureAdminLoggedIn() {
        if (currentAdmin != null) {
            return true;
        }
        return showAdminLoginDialog();
    }

    private void handleAdminLogout() {
        if (currentAdmin == null) {
            return;
        }
        if (!feedback.confirm("确认退出", "确定退出当前管理员账号吗？")) {
            return;
        }
        currentAdmin = null;
        updateAdminAccessUi();
        feedback.info("管理员已退出登录。");
        feedback.setStatus("管理员模式: 已退出登录");
    }

    private void updateAdminAccessUi() {
        if (adminCardLayout == null || adminCardPanel == null) {
            return;
        }
        if (currentAdmin == null) {
            adminCardLayout.show(adminCardPanel, "LOCKED");
            setAdminEditMode(EditToolMode.SELECT);
        } else {
            adminCardLayout.show(adminCardPanel, "UNLOCKED");
        }
        boolean enabled = currentAdmin != null;
        for (JButton button : adminToolButtons.values()) {
            button.setEnabled(enabled);
        }
        for (JButton button : adminOverlayButtons) {
            button.setEnabled(enabled);
        }
        if (undoButton != null) {
            undoButton.setEnabled(enabled && adminEditCoordinator.canUndo());
        }
        if (redoButton != null) {
            redoButton.setEnabled(enabled && adminEditCoordinator.canRedo());
        }
        if (activeRoute == AppRoute.ADMIN_MODE) {
            mapWorkbenchView.setMapHint(resolveMapHint(AppRoute.ADMIN_MODE));
        }
        updateMapOverlayToolbarVisibility();
        updateShellHeaderState();
    }

    private void updateMapOverlayToolbarVisibility() {
        if (mapOverlayToolbar == null) {
            return;
        }
        mapOverlayToolbar.setVisible(activeRoute == AppRoute.ADMIN_MODE);
    }

    private void updateNavState(AppRoute activeRoute) {
        for (Map.Entry<AppRoute, JButton> entry : navButtons.entrySet()) {
            applyNavigationButtonStyle(entry.getValue(), entry.getKey() == activeRoute);
        }
    }

    private JPanel createRailHeader(String title, String description) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiStyles.SUBTITLE_FONT);
        titleLabel.setForeground(UiStyles.TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JTextArea descriptionLabel = new JTextArea(description);
        descriptionLabel.setEditable(false);
        descriptionLabel.setLineWrap(true);
        descriptionLabel.setWrapStyleWord(true);
        descriptionLabel.setOpaque(false);
        descriptionLabel.setFocusable(false);
        descriptionLabel.setFont(UiStyles.CAPTION_FONT);
        descriptionLabel.setForeground(UiStyles.TEXT_SECONDARY);
        descriptionLabel.setAlignmentX(LEFT_ALIGNMENT);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder());
        descriptionLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(3));
        header.add(descriptionLabel);
        return header;
    }

    private void applyNavigationButtonStyle(JButton button, boolean active) {
        button.setOpaque(true);
        if (active) {
            button.setBackground(UiStyles.PRIMARY_SOFT);
            button.setForeground(UiStyles.PRIMARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiStyles.BRAND_500),
                    BorderFactory.createEmptyBorder(10, 14, 10, 12)
            ));
            return;
        }
        button.setBackground(UiStyles.SURFACE_ALT);
        button.setForeground(UiStyles.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.SURFACE_ALT),
                BorderFactory.createEmptyBorder(10, 14, 10, 12)
        ));
    }

    private void updateShellHeaderState() {
        if (topModeBadgeLabel != null) {
            topModeBadgeLabel.setText(activeRoute.getTitle());
            applyTopBarBadgeStyle(
                    topModeBadgeLabel,
                    new Color(255, 255, 255, 30),
                    Color.WHITE,
                    new Color(255, 255, 255, 60)
            );
        }
        if (topModeDescriptionLabel != null) {
            topModeDescriptionLabel.setText(resolveShellModeDescription(activeRoute));
        }
        if (topSessionBadgeLabel != null) {
            if (currentAdmin == null) {
                topSessionBadgeLabel.setText("访客模式");
                applyTopBarBadgeStyle(
                        topSessionBadgeLabel,
                        new Color(255, 255, 255, 24),
                        new Color(230, 236, 242),
                        new Color(255, 255, 255, 50)
                );
            } else {
                topSessionBadgeLabel.setText("管理员  " + currentAdmin.getUsername());
                applyTopBarBadgeStyle(
                        topSessionBadgeLabel,
                        new Color(35, 162, 109, 70),
                        Color.WHITE,
                        new Color(140, 230, 187, 120)
                );
            }
        }
        refreshTopBarLayout();
    }

    private void applyTopBarBadgeStyle(JLabel label, Color background, Color foreground, Color borderColor) {
        label.setOpaque(true);
        label.setFont(UiStyles.CAPTION_FONT);
        label.setForeground(foreground);
        label.setBackground(background);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        Dimension preferredSize = label.getPreferredSize();
        label.setMinimumSize(preferredSize);
        label.setPreferredSize(preferredSize);
        label.setMaximumSize(preferredSize);
    }

    private void refreshTopBarLayout() {
        if (topStatusPanel == null) {
            return;
        }
        topStatusPanel.revalidate();
        topStatusPanel.repaint();
    }

    private String resolveShellModeDescription(AppRoute route) {
        if (route == AppRoute.USER_MODE) {
            return "路径查询、分步导航与地点浏览";
        }
        if (route == AppRoute.ADMIN_MODE) {
            return "地图编辑、图层管理与数据维护";
        }
        return "系统信息、数据目录与维护说明";
    }

    private static void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(UiStyles.formLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
        gbc.weightx = 0;
    }

    private static double parseDouble(String text, String fieldName) {
        try {
            return Double.parseDouble(text == null ? "" : text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " 请输入有效数字。");
        }
    }

    private static double parsePositiveDouble(String text, String fieldName) {
        double value = parseDouble(text, fieldName);
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " 必须大于0。");
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
