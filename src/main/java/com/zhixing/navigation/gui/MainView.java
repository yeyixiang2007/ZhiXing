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
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.planning.DijkstraStrategy;
import com.zhixing.navigation.gui.components.AdminLoginDialog;
import com.zhixing.navigation.gui.components.LoadingOverlay;
import com.zhixing.navigation.gui.components.ResultMessageBar;
import com.zhixing.navigation.gui.controller.AuthController;
import com.zhixing.navigation.gui.controller.MapController;
import com.zhixing.navigation.gui.controller.NavigationController;
import com.zhixing.navigation.gui.model.OverviewData;
import com.zhixing.navigation.gui.model.RoadOption;
import com.zhixing.navigation.gui.model.VertexOption;
import com.zhixing.navigation.gui.routing.AppRoute;
import com.zhixing.navigation.gui.styles.UiStyles;
import com.zhixing.navigation.gui.view.ForbiddenManageView;
import com.zhixing.navigation.gui.view.OverviewDashboardView;
import com.zhixing.navigation.gui.view.PathQueryView;
import com.zhixing.navigation.gui.view.PlaceBrowseView;
import com.zhixing.navigation.gui.view.RoadManageView;
import com.zhixing.navigation.gui.view.VertexManageView;
import com.zhixing.navigation.gui.workbench.LayerPanel;
import com.zhixing.navigation.gui.workbench.MapCanvas;
import com.zhixing.navigation.gui.workbench.MapWorkbenchView;
import com.zhixing.navigation.gui.workbench.WorkbenchFeedback;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainView extends JFrame {
    private static final String ADMIN_SECTION_VERTEX = "VERTEX";
    private static final String ADMIN_SECTION_ROAD = "ROAD";
    private static final String ADMIN_SECTION_FORBIDDEN = "FORBIDDEN";
    private static final String ADMIN_SECTION_OVERVIEW = "OVERVIEW";

    private final AuthController authController;
    private final NavigationController navigationController;
    private final MapController mapController;

    private final MapWorkbenchView mapWorkbenchView;
    private final MapCanvas mapCanvas;
    private final LayerPanel layerPanel;
    private final ResultMessageBar messageBar;
    private final LoadingOverlay loadingOverlay;
    private final WorkbenchFeedback feedback;
    private final Map<AppRoute, JButton> navButtons;
    private final Map<String, JButton> adminSectionButtons;

    private PathQueryView pathQueryView;
    private PlaceBrowseView placeBrowseView;
    private VertexManageView vertexManageView;
    private RoadManageView roadManageView;
    private ForbiddenManageView forbiddenManageView;
    private OverviewDashboardView overviewDashboardView;

    private Admin currentAdmin;
    private AppRoute activeRoute;
    private JLabel adminInfoLabel;
    private CardLayout adminCardLayout;
    private JPanel adminCardPanel;
    private CardLayout adminWorkspaceLayout;
    private JPanel adminWorkspacePanel;
    private List<Vertex> currentRouteVertices;
    private List<Vertex> previousRouteVertices;

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
        this.messageBar = new ResultMessageBar();
        this.loadingOverlay = new LoadingOverlay();
        this.feedback = new WorkbenchFeedback(this, messageBar, loadingOverlay, mapWorkbenchView::setStatus);
        this.navButtons = new EnumMap<AppRoute, JButton>(AppRoute.class);
        this.adminSectionButtons = new LinkedHashMap<String, JButton>();
        this.activeRoute = AppRoute.USER_MODE;
        this.currentRouteVertices = new ArrayList<Vertex>();
        this.previousRouteVertices = new ArrayList<Vertex>();

        UiStyles.installDefaults();
        initializeFrame();
        initializeLayout();
        wireViewEvents();
        refreshAllData();
        navigateTo(AppRoute.USER_MODE);
    }

    private void initializeFrame() {
        setTitle("知行校园智能路径规划系统");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 760));
        setSize(1320, 860);
        setLocationRelativeTo(null);
        setGlassPane(loadingOverlay);
    }

    private void initializeLayout() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiStyles.PAGE_BACKGROUND);
        setContentPane(root);

        root.add(createTopBar(), BorderLayout.NORTH);
        root.add(createLeftNavigation(), BorderLayout.WEST);
        root.add(createCenterWorkspace(), BorderLayout.CENTER);
        root.add(createBottomBar(), BorderLayout.SOUTH);
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(18, 59, 96));
        topBar.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("校园智能路径规划导航系统");
        title.setFont(UiStyles.TITLE_FONT);
        title.setForeground(Color.WHITE);

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        JLabel timeLabel = new JLabel("启动时间: " + now, SwingConstants.RIGHT);
        timeLabel.setFont(UiStyles.CAPTION_FONT);
        timeLabel.setForeground(new Color(214, 232, 255));

        topBar.add(title, BorderLayout.WEST);
        topBar.add(timeLabel, BorderLayout.EAST);
        return topBar;
    }

    private JPanel createLeftNavigation() {
        JPanel navigation = new JPanel();
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setPreferredSize(new Dimension(230, 0));
        navigation.setBackground(new Color(242, 245, 249));
        navigation.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        JLabel navTitle = new JLabel("功能导航");
        navTitle.setFont(UiStyles.SUBTITLE_FONT);
        navTitle.setForeground(UiStyles.TEXT_SECONDARY);
        navTitle.setAlignmentX(LEFT_ALIGNMENT);
        navigation.add(navTitle);
        navigation.add(Box.createVerticalStrut(16));

        for (AppRoute route : AppRoute.values()) {
            JButton navButton = new JButton(route.getTitle());
            navButton.setFont(UiStyles.BODY_FONT);
            navButton.setFocusPainted(false);
            navButton.setAlignmentX(LEFT_ALIGNMENT);
            navButton.setHorizontalAlignment(SwingConstants.LEFT);
            navButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            navButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 10));
            navButton.addActionListener(e -> navigateTo(route));
            navButtons.put(route, navButton);
            navigation.add(navButton);
            navigation.add(Box.createVerticalStrut(10));
        }
        navigation.add(Box.createVerticalStrut(12));
        layerPanel.setAlignmentX(LEFT_ALIGNMENT);
        layerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        navigation.add(layerPanel);
        navigation.add(Box.createVerticalGlue());
        return navigation;
    }

    private JPanel createCenterWorkspace() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UiStyles.PAGE_BACKGROUND);

        mapWorkbenchView.registerRoutePanel(AppRoute.USER_MODE, createUserModeView());
        mapWorkbenchView.registerRoutePanel(AppRoute.ADMIN_MODE, createAdminModeView());
        mapWorkbenchView.registerRoutePanel(AppRoute.SYSTEM_SETTINGS, createSystemSettingsView());
        mapWorkbenchView.setMapContent(mapCanvas);

        center.add(mapWorkbenchView, BorderLayout.CENTER);
        return center;
    }

    private JPanel createBottomBar() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        bottom.setBackground(UiStyles.PAGE_BACKGROUND);
        bottom.add(messageBar, BorderLayout.CENTER);
        return bottom;
    }

    private JPanel createUserModeView() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UiStyles.PAGE_BACKGROUND);

        pathQueryView = new PathQueryView();
        placeBrowseView = new PlaceBrowseView();

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pathQueryView, placeBrowseView);
        splitPane.setResizeWeight(0.56);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(6);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        page.add(splitPane, BorderLayout.CENTER);
        return page;
    }

    private JPanel createAdminModeView() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(UiStyles.sectionBorder("管理员模式"));
        header.setBackground(UiStyles.PANEL_BACKGROUND);

        adminInfoLabel = new JLabel("当前状态：未登录");
        adminInfoLabel.setFont(UiStyles.BODY_FONT);
        adminInfoLabel.setForeground(UiStyles.TEXT_PRIMARY);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBar.setOpaque(false);
        JButton loginButton = UiStyles.primaryButton("管理员登录");
        loginButton.addActionListener(e -> {
            if (showAdminLoginDialog()) {
                refreshAllData();
            }
        });
        JButton logoutButton = UiStyles.secondaryButton("退出登录");
        logoutButton.addActionListener(e -> handleAdminLogout());
        actionBar.add(loginButton);
        actionBar.add(logoutButton);

        header.add(adminInfoLabel, BorderLayout.WEST);
        header.add(actionBar, BorderLayout.EAST);

        adminCardLayout = new CardLayout();
        adminCardPanel = new JPanel(adminCardLayout);
        adminCardPanel.setBackground(UiStyles.PAGE_BACKGROUND);
        adminCardPanel.add(createAdminLockedPanel(), "LOCKED");
        adminCardPanel.add(createAdminWorkspace(), "UNLOCKED");

        page.add(header, BorderLayout.NORTH);
        page.add(adminCardPanel, BorderLayout.CENTER);
        return page;
    }

    private JPanel createAdminLockedPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UiStyles.PANEL_BACKGROUND);
        panel.setBorder(UiStyles.sectionBorder("管理员登录"));
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

        JPanel sectionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sectionBar.setBackground(UiStyles.PANEL_BACKGROUND);
        sectionBar.setBorder(UiStyles.sectionBorder("管理工具"));
        sectionBar.add(createAdminSectionButton(ADMIN_SECTION_VERTEX, "地点管理"));
        sectionBar.add(createAdminSectionButton(ADMIN_SECTION_ROAD, "道路管理"));
        sectionBar.add(createAdminSectionButton(ADMIN_SECTION_FORBIDDEN, "禁行管理"));
        sectionBar.add(createAdminSectionButton(ADMIN_SECTION_OVERVIEW, "地图概览"));

        adminWorkspaceLayout = new CardLayout();
        adminWorkspacePanel = new JPanel(adminWorkspaceLayout);
        adminWorkspacePanel.setBackground(UiStyles.PAGE_BACKGROUND);
        adminWorkspacePanel.add(vertexManageView, ADMIN_SECTION_VERTEX);
        adminWorkspacePanel.add(roadManageView, ADMIN_SECTION_ROAD);
        adminWorkspacePanel.add(forbiddenManageView, ADMIN_SECTION_FORBIDDEN);
        adminWorkspacePanel.add(overviewDashboardView, ADMIN_SECTION_OVERVIEW);

        panel.add(sectionBar, BorderLayout.NORTH);
        panel.add(adminWorkspacePanel, BorderLayout.CENTER);

        showAdminSection(ADMIN_SECTION_VERTEX);
        return panel;
    }

    private JButton createAdminSectionButton(String sectionKey, String text) {
        JButton button = UiStyles.secondaryButton(text);
        button.addActionListener(e -> showAdminSection(sectionKey));
        adminSectionButtons.put(sectionKey, button);
        return button;
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
            button.setBackground(new Color(213, 228, 251));
            button.setForeground(UiStyles.PRIMARY);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(170, 201, 243)),
                    BorderFactory.createEmptyBorder(8, 14, 8, 14)
            ));
            return;
        }
        button.setBackground(Color.WHITE);
        button.setForeground(UiStyles.TEXT_PRIMARY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
    }

    private JPanel createSystemSettingsView() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiStyles.PANEL_BACKGROUND);
        form.setBorder(UiStyles.sectionBorder("系统设置"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField dataDirField = UiStyles.formField(28);
        dataDirField.setEditable(false);
        dataDirField.setText(mapController.loadOverview().getDataDir());

        JTextField backupNameField = UiStyles.formField(18);
        backupNameField.setText("backup-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()));
        JTextField restoreNameField = UiStyles.formField(18);

        addFormRow(form, gbc, 0, "数据目录", dataDirField);
        addFormRow(form, gbc, 1, "备份名称", backupNameField);
        addFormRow(form, gbc, 2, "恢复备份", restoreNameField);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionBar.setOpaque(false);
        JButton backupButton = UiStyles.primaryButton("执行备份");
        backupButton.addActionListener(e -> handleBackup(backupNameField.getText()));
        JButton restoreButton = UiStyles.secondaryButton("执行恢复");
        restoreButton.addActionListener(e -> handleRestore(restoreNameField.getText()));
        actionBar.add(backupButton);
        actionBar.add(restoreButton);

        gbc.gridx = 1;
        gbc.gridy = 3;
        form.add(actionBar, gbc);
        page.add(form, BorderLayout.NORTH);
        return page;
    }

    private void wireViewEvents() {
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
                if (currentRouteVertices != null && currentRouteVertices.size() >= 2) {
                    int maxSegment = currentRouteVertices.size() - 2;
                    if (segmentIndex > maxSegment) {
                        segmentIndex = maxSegment;
                    }
                }
                mapCanvas.focusRouteSegment(segmentIndex);
                feedback.setStatus("步骤联动: 已定位到第 " + (index + 1) + " 步");
            }
        });
        placeBrowseView.setListener(this::refreshPlaceData);
        mapCanvas.setListener(new MapCanvas.Listener() {
            @Override
            public void onSelectionChanged(List<String> selectedVertexIds, String selectedEdgeKey) {
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
        });

        vertexManageView.setListener(new VertexManageView.Listener() {
            @Override
            public void onAdd(VertexManageView.VertexFormData formData) {
                runAdminMutation("正在新增地点...", "地点新增成功。", () -> {
                    mapController.addVertex(
                            currentAdmin,
                            formData.getId(),
                            formData.getName(),
                            formData.getType(),
                            parseDouble(formData.getX(), "X坐标"),
                            parseDouble(formData.getY(), "Y坐标"),
                            formData.getDescription()
                    );
                    vertexManageView.clearForm();
                });
            }

            @Override
            public void onUpdate(VertexManageView.VertexFormData formData) {
                runAdminMutation("正在修改地点...", "地点修改成功。", () ->
                        mapController.updateVertex(
                                currentAdmin,
                                formData.getId(),
                                formData.getName(),
                                formData.getType(),
                                parseDouble(formData.getX(), "X坐标"),
                                parseDouble(formData.getY(), "Y坐标"),
                                formData.getDescription()
                        ));
            }

            @Override
            public void onDelete(String id) {
                if (id == null || id.trim().isEmpty()) {
                    feedback.showErrorDialog("参数缺失", "请先输入或选择要删除的地点ID。");
                    return;
                }
                if (!feedback.confirm("确认删除", "确定删除地点 " + id + " 吗？")) {
                    return;
                }
                runAdminMutation("正在删除地点...", "地点删除成功。", () -> mapController.deleteVertex(currentAdmin, id));
            }
        });

        roadManageView.setListener(new RoadManageView.Listener() {
            @Override
            public void onAdd(RoadManageView.RoadFormData data) {
                runAdminMutation("正在新增道路...", "道路新增成功。", () ->
                        mapController.addRoad(
                                currentAdmin,
                                data.getFromId(),
                                data.getToId(),
                                parsePositiveDouble(data.getWeight(), "道路距离"),
                                data.isOneWay(),
                                data.isForbidden(),
                                data.getRoadType()
                        ));
            }

            @Override
            public void onUpdate(RoadManageView.RoadFormData data) {
                runAdminMutation("正在修改道路...", "道路修改成功。", () ->
                        mapController.updateRoad(
                                currentAdmin,
                                data.getFromId(),
                                data.getToId(),
                                parsePositiveDouble(data.getWeight(), "道路距离"),
                                data.isOneWay(),
                                data.isForbidden(),
                                data.getRoadType()
                        ));
            }

            @Override
            public void onDelete(RoadManageView.RoadFormData data) {
                if (data.getFromId() == null || data.getToId() == null) {
                    feedback.showErrorDialog("参数缺失", "请先选择要删除的道路起点和终点。");
                    return;
                }
                if (!feedback.confirm("确认删除", "确定删除道路 " + data.getFromId() + " -> " + data.getToId() + " 吗？")) {
                    return;
                }
                runAdminMutation("正在删除道路...", "道路删除成功。", () ->
                        mapController.deleteRoad(currentAdmin, data.getFromId(), data.getToId()));
            }
        });

        forbiddenManageView.setListener(new ForbiddenManageView.Listener() {
            @Override
            public void onToggleForbidden(RoadOption roadOption, boolean forbidden) {
                if (roadOption == null) {
                    feedback.showErrorDialog("参数缺失", "当前没有可操作的道路。");
                    return;
                }
                String actionText = forbidden ? "设置禁行" : "解除禁行";
                runAdminMutation("正在" + actionText + "...", actionText + "成功。", () ->
                        mapController.setRoadForbidden(currentAdmin, roadOption.getFromId(), roadOption.getToId(), forbidden));
            }

            @Override
            public void onRefresh() {
                refreshForbiddenData();
            }
        });

        overviewDashboardView.setListener(this::refreshOverviewData);
    }

    private void handlePathQuery(String startId, String endId) {
        if (isBlank(startId) || isBlank(endId)) {
            feedback.showErrorDialog("参数缺失", "请先选择起点与终点。");
            return;
        }
        try {
            feedback.showLoading("正在计算最短路径...");
            PathResult result = navigationController.queryPath(startId, endId);
            pathQueryView.setResultContent(navigationController.format(result));
            pathQueryView.setInstructions(result.getNaviInstructions());
            previousRouteVertices = new ArrayList<Vertex>(currentRouteVertices);
            currentRouteVertices = new ArrayList<Vertex>(result.getPathList());
            mapCanvas.setRouteComparison(currentRouteVertices, previousRouteVertices);
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
        mapCanvas.setRouteComparison(currentRouteVertices, previousRouteVertices);
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

    private void runAdminMutation(String loadingText, String successText, Runnable operation) {
        if (!ensureAdminLoggedIn()) {
            return;
        }
        try {
            feedback.showLoading(loadingText);
            operation.run();
            refreshAllData();
            feedback.success(successText);
            feedback.setStatus("管理员模式: " + successText);
        } catch (RuntimeException ex) {
            feedback.showOperationError("操作失败", ex);
            feedback.setStatus("管理员模式: 操作失败");
        } finally {
            feedback.hideLoading();
        }
    }

    private void handleBackup(String backupName) {
        try {
            feedback.showLoading("正在执行数据备份...");
            mapController.backupData(backupName);
            feedback.success("数据备份成功。");
            feedback.setStatus("系统设置: 数据备份完成");
        } catch (RuntimeException ex) {
            feedback.showOperationError("备份失败", ex);
            feedback.setStatus("系统设置: 数据备份失败");
        } finally {
            feedback.hideLoading();
        }
    }

    private void handleRestore(String backupName) {
        if (!feedback.confirm("确认恢复", "恢复会覆盖当前数据，确定继续吗？")) {
            return;
        }
        try {
            feedback.showLoading("正在恢复数据备份...");
            mapController.restoreData(backupName);
            feedback.warning("数据恢复成功，请重启应用以完全生效。");
            feedback.setStatus("系统设置: 数据恢复完成");
        } catch (RuntimeException ex) {
            feedback.showOperationError("恢复失败", ex);
            feedback.setStatus("系统设置: 数据恢复失败");
        } finally {
            feedback.hideLoading();
        }
    }

    private void navigateTo(AppRoute route) {
        activeRoute = route;
        if (route == AppRoute.ADMIN_MODE && currentAdmin == null) {
            showAdminLoginDialog();
        }
        mapWorkbenchView.showRoute(route);
        mapWorkbenchView.setMapHint(resolveMapHint(route));
        feedback.setStatus("当前页面: " + route.getTitle());
        updateNavState(route);
        updateAdminAccessUi();
    }

    private String resolveMapHint(AppRoute route) {
        if (route == AppRoute.USER_MODE) {
            return "用户模式：左键框选/点选，右键拖拽平移，滚轮缩放。";
        }
        if (route == AppRoute.ADMIN_MODE) {
            if (currentAdmin == null) {
                return "管理员模式：请先登录，登录后可进行地图编辑与数据维护。";
            }
            return "管理员模式：可在地图区查看点线与禁行状态，编辑能力在模块D接入。";
        }
        return "系统设置：图层面板支持显隐与绘制顺序调整，可实时预览。";
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
        if (adminInfoLabel == null || adminCardLayout == null || adminCardPanel == null) {
            return;
        }
        if (currentAdmin == null) {
            adminInfoLabel.setText("当前状态：未登录");
            adminCardLayout.show(adminCardPanel, "LOCKED");
        } else {
            adminInfoLabel.setText("当前状态：已登录（" + currentAdmin.getUsername() + "）");
            adminCardLayout.show(adminCardPanel, "UNLOCKED");
        }
        if (activeRoute == AppRoute.ADMIN_MODE) {
            mapWorkbenchView.setMapHint(resolveMapHint(AppRoute.ADMIN_MODE));
        }
    }

    private void updateNavState(AppRoute activeRoute) {
        for (Map.Entry<AppRoute, JButton> entry : navButtons.entrySet()) {
            JButton button = entry.getValue();
            if (entry.getKey() == activeRoute) {
                button.setBackground(new Color(213, 228, 251));
                button.setForeground(UiStyles.PRIMARY);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(170, 201, 243)),
                        BorderFactory.createEmptyBorder(8, 12, 8, 10)
                ));
            } else {
                button.setBackground(new Color(242, 245, 249));
                button.setForeground(UiStyles.TEXT_PRIMARY);
                button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 10));
            }
        }
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
