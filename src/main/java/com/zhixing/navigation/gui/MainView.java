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
import com.zhixing.navigation.gui.workbench.EditToolMode;
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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
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
    private final Map<EditToolMode, JButton> adminToolButtons;
    private final Deque<AdminEditCommand> undoStack;
    private final Deque<AdminEditCommand> redoStack;

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
    private List<String> mapSelectedVertexIds;
    private String mapSelectedEdgeKey;
    private String pendingEdgeStartVertexId;
    private EditToolMode activeEditToolMode;
    private JButton undoButton;
    private JButton redoButton;
    private int autoVertexCounter;

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
        this.adminToolButtons = new EnumMap<EditToolMode, JButton>(EditToolMode.class);
        this.undoStack = new ArrayDeque<AdminEditCommand>();
        this.redoStack = new ArrayDeque<AdminEditCommand>();
        this.activeRoute = AppRoute.USER_MODE;
        this.currentRouteVertices = new ArrayList<Vertex>();
        this.previousRouteVertices = new ArrayList<Vertex>();
        this.mapSelectedVertexIds = new ArrayList<String>();
        this.mapSelectedEdgeKey = null;
        this.pendingEdgeStartVertexId = null;
        this.activeEditToolMode = EditToolMode.SELECT;
        this.autoVertexCounter = 1;

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

        JPanel workspaceHeader = new JPanel(new BorderLayout(0, 8));
        workspaceHeader.setBackground(UiStyles.PAGE_BACKGROUND);
        workspaceHeader.add(sectionBar, BorderLayout.NORTH);
        workspaceHeader.add(createAdminEditToolbar(), BorderLayout.SOUTH);

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

    private JPanel createAdminEditToolbar() {
        JPanel editBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        editBar.setBackground(UiStyles.PANEL_BACKGROUND);
        editBar.setBorder(UiStyles.sectionBorder("地图编辑工具"));

        editBar.add(createEditToolButton(EditToolMode.SELECT, "选择"));
        editBar.add(createEditToolButton(EditToolMode.ADD_VERTEX, "添加点"));
        editBar.add(createEditToolButton(EditToolMode.ADD_EDGE, "连线"));
        editBar.add(createEditToolButton(EditToolMode.MOVE_VERTEX, "移动点"));
        editBar.add(createEditToolButton(EditToolMode.DELETE_OBJECT, "删除对象"));

        undoButton = UiStyles.secondaryButton("撤销");
        undoButton.addActionListener(e -> undoLastEdit());
        redoButton = UiStyles.secondaryButton("重做");
        redoButton.addActionListener(e -> redoLastEdit());

        JButton batchDeleteButton = UiStyles.secondaryButton("批量删除");
        batchDeleteButton.addActionListener(e -> handleBatchDeleteSelectedVertices());
        JButton batchForbidButton = UiStyles.secondaryButton("批量禁行");
        batchForbidButton.addActionListener(e -> handleBatchForbiddenBySelection(true));
        JButton batchEnableButton = UiStyles.secondaryButton("批量解禁");
        batchEnableButton.addActionListener(e -> handleBatchForbiddenBySelection(false));
        JButton quickToggleButton = UiStyles.secondaryButton("禁行切换");
        quickToggleButton.addActionListener(e -> handleQuickToggleSelectedEdgeForbidden());

        editBar.add(undoButton);
        editBar.add(redoButton);
        editBar.add(batchDeleteButton);
        editBar.add(batchForbidButton);
        editBar.add(batchEnableButton);
        editBar.add(quickToggleButton);
        return editBar;
    }

    private JButton createEditToolButton(EditToolMode mode, String text) {
        JButton button = UiStyles.secondaryButton(text);
        button.addActionListener(e -> setAdminEditMode(mode));
        adminToolButtons.put(mode, button);
        return button;
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

    private void setAdminEditMode(EditToolMode mode) {
        EditToolMode target = mode == null ? EditToolMode.SELECT : mode;
        activeEditToolMode = target;
        mapCanvas.setEditToolMode(target);
        pendingEdgeStartVertexId = null;
        updateAdminToolButtonStyle();
        if (activeRoute == AppRoute.ADMIN_MODE) {
            mapWorkbenchView.setMapHint(resolveMapHint(activeRoute));
        }
    }

    private void updateAdminToolButtonStyle() {
        for (Map.Entry<EditToolMode, JButton> entry : adminToolButtons.entrySet()) {
            applyAdminToolButtonStyle(entry.getValue(), entry.getKey() == activeEditToolMode);
        }
    }

    private void applyAdminToolButtonStyle(JButton button, boolean active) {
        if (button == null) {
            return;
        }
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
                mapSelectedVertexIds = new ArrayList<String>(selectedVertexIds);
                mapSelectedEdgeKey = selectedEdgeKey;
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
                pendingEdgeStartVertexId = startVertexId;
                if (activeRoute == AppRoute.ADMIN_MODE && activeEditToolMode == EditToolMode.ADD_EDGE) {
                    if (startVertexId == null) {
                        feedback.setStatus("管理员连线模式: 请选择第一点");
                    } else {
                        feedback.setStatus("管理员连线模式: 起点已选 " + startVertexId + "，请点击终点");
                    }
                }
            }
        });

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

        overviewDashboardView.setListener(this::refreshOverviewData);
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
        if (mapSelectedEdgeKey != null) {
            Edge selectedEdge = findRoadByEdgeKey(mapSelectedEdgeKey);
            if (selectedEdge != null) {
                roadManageView.fillFromEdge(selectedEdge);
                showAdminSection(ADMIN_SECTION_ROAD);
                feedback.setStatus("地图选择: 道路 " + selectedEdge.getFromVertex().getId() + " -> " + selectedEdge.getToVertex().getId());
                return;
            }
        }
        if (mapSelectedVertexIds.size() == 1) {
            Vertex selectedVertex = findVertexById(mapSelectedVertexIds.get(0));
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
        final String id = safeTrim(formData.getId());
        final String name = safeTrim(formData.getName());
        final PlaceType type = formData.getType();
        final double x = parseDouble(formData.getX(), "X坐标");
        final double y = parseDouble(formData.getY(), "Y坐标");
        final String description = safeTrim(formData.getDescription());

        executeAdminEditCommand("正在新增地点...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.addVertex(admin, id, name, type, x, y, description);
            }

            @Override
            public void undo(Admin admin) {
                mapController.deleteVertex(admin, id);
            }

            @Override
            public String successMessage() {
                return "地点新增成功。";
            }
        });
    }

    private void handleUpdateVertexFromForm(VertexManageView.VertexFormData formData) {
        final String id = safeTrim(formData.getId());
        final Vertex before = requireVertex(id);
        final String name = safeTrim(formData.getName());
        final PlaceType type = formData.getType();
        final double x = parseDouble(formData.getX(), "X坐标");
        final double y = parseDouble(formData.getY(), "Y坐标");
        final String description = safeTrim(formData.getDescription());

        executeAdminEditCommand("正在修改地点...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.updateVertex(admin, id, name, type, x, y, description);
            }

            @Override
            public void undo(Admin admin) {
                mapController.updateVertex(admin, before.getId(), before.getName(), before.getType(), before.getX(), before.getY(), before.getDescription());
            }

            @Override
            public String successMessage() {
                return "地点修改成功。";
            }
        });
    }

    private void handleDeleteVertexById(String id) {
        String vertexId = safeTrim(id);
        if (isBlank(vertexId)) {
            feedback.showErrorDialog("参数缺失", "请先输入或选择要删除的地点ID。");
            return;
        }
        if (!feedback.confirm("确认删除", "确定删除地点 " + vertexId + " 吗？")) {
            return;
        }
        final Vertex snapshot = requireVertex(vertexId);
        final List<Edge> relatedRoads = listRelatedCanonicalRoads(Collections.singleton(snapshot.getId()));
        executeAdminEditCommand("正在删除地点...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.deleteVertex(admin, snapshot.getId());
            }

            @Override
            public void undo(Admin admin) {
                mapController.addVertex(admin, snapshot.getId(), snapshot.getName(), snapshot.getType(), snapshot.getX(), snapshot.getY(), snapshot.getDescription());
                restoreRoads(admin, relatedRoads);
            }

            @Override
            public String successMessage() {
                return "地点删除成功。";
            }
        });
    }

    private void handleAddRoadFromForm(RoadManageView.RoadFormData data) {
        final String fromId = safeTrim(data.getFromId());
        final String toId = safeTrim(data.getToId());
        final double weight = parsePositiveDouble(data.getWeight(), "道路距离");
        final boolean oneWay = data.isOneWay();
        final boolean forbidden = data.isForbidden();
        final RoadType roadType = data.getRoadType();

        executeAdminEditCommand("正在新增道路...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.addRoad(admin, fromId, toId, weight, oneWay, forbidden, roadType);
            }

            @Override
            public void undo(Admin admin) {
                mapController.deleteRoad(admin, fromId, toId);
            }

            @Override
            public String successMessage() {
                return "道路新增成功。";
            }
        });
    }

    private void handleUpdateRoadFromForm(RoadManageView.RoadFormData data) {
        final String fromId = safeTrim(data.getFromId());
        final String toId = safeTrim(data.getToId());
        final Edge before = requireRoad(fromId, toId);
        final double weight = parsePositiveDouble(data.getWeight(), "道路距离");
        final boolean oneWay = data.isOneWay();
        final boolean forbidden = data.isForbidden();
        final RoadType roadType = data.getRoadType();

        executeAdminEditCommand("正在修改道路...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.updateRoad(admin, fromId, toId, weight, oneWay, forbidden, roadType);
            }

            @Override
            public void undo(Admin admin) {
                mapController.updateRoad(admin, before.getFromVertex().getId(), before.getToVertex().getId(), before.getWeight(), before.isOneWay(), before.isForbidden(), before.getRoadType());
            }

            @Override
            public String successMessage() {
                return "道路修改成功。";
            }
        });
    }

    private void handleDeleteRoadFromForm(RoadManageView.RoadFormData data) {
        String fromId = safeTrim(data.getFromId());
        String toId = safeTrim(data.getToId());
        if (isBlank(fromId) || isBlank(toId)) {
            feedback.showErrorDialog("参数缺失", "请先选择要删除的道路起点和终点。");
            return;
        }
        if (!feedback.confirm("确认删除", "确定删除道路 " + fromId + " -> " + toId + " 吗？")) {
            return;
        }
        final Edge snapshot = requireRoad(fromId, toId);
        executeAdminEditCommand("正在删除道路...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.deleteRoad(admin, snapshot.getFromVertex().getId(), snapshot.getToVertex().getId());
            }

            @Override
            public void undo(Admin admin) {
                mapController.addRoad(admin, snapshot.getFromVertex().getId(), snapshot.getToVertex().getId(), snapshot.getWeight(), snapshot.isOneWay(), snapshot.isForbidden(), snapshot.getRoadType());
            }

            @Override
            public String successMessage() {
                return "道路删除成功。";
            }
        });
    }

    private void handleSetRoadForbidden(String fromId, String toId, boolean forbidden) {
        final Edge before = requireRoad(fromId, toId);
        final boolean target = forbidden;
        executeAdminEditCommand(target ? "正在设置禁行..." : "正在解除禁行...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.setRoadForbidden(admin, before.getFromVertex().getId(), before.getToVertex().getId(), target);
            }

            @Override
            public void undo(Admin admin) {
                mapController.setRoadForbidden(admin, before.getFromVertex().getId(), before.getToVertex().getId(), before.isForbidden());
            }

            @Override
            public String successMessage() {
                return target ? "设置禁行成功。" : "解除禁行成功。";
            }
        });
    }

    private void handleMapAddVertex(double x, double y) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        final String id = generateAutoVertexId();
        final String name = "地点-" + id;
        executeAdminEditCommand("正在地图新增点位...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.addVertex(admin, id, name, PlaceType.OTHER, x, y, "地图编辑新增");
            }

            @Override
            public void undo(Admin admin) {
                mapController.deleteVertex(admin, id);
            }

            @Override
            public String successMessage() {
                return "地图新增点位成功。";
            }
        });
    }

    private void handleMapConnectVertices(String fromId, String toId) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        if (isBlank(fromId) || isBlank(toId) || fromId.equals(toId)) {
            return;
        }
        Vertex fromVertex = requireVertex(fromId);
        Vertex toVertex = requireVertex(toId);
        final double distance = Math.max(1.0, Math.hypot(fromVertex.getX() - toVertex.getX(), fromVertex.getY() - toVertex.getY()));
        executeAdminEditCommand("正在地图连线...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.addRoad(admin, fromId, toId, distance, false, false, RoadType.PATH);
            }

            @Override
            public void undo(Admin admin) {
                mapController.deleteRoad(admin, fromId, toId);
            }

            @Override
            public String successMessage() {
                return "地图连线成功。";
            }
        });
    }

    private void handleMapMoveVertex(String vertexId, double x, double y) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        final Vertex before = requireVertex(vertexId);
        executeAdminEditCommand("正在移动点位...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.updateVertex(admin, before.getId(), before.getName(), before.getType(), x, y, before.getDescription());
            }

            @Override
            public void undo(Admin admin) {
                mapController.updateVertex(admin, before.getId(), before.getName(), before.getType(), before.getX(), before.getY(), before.getDescription());
            }

            @Override
            public String successMessage() {
                return "点位移动成功。";
            }
        });
    }

    private void handleMapDeleteVertex(String vertexId) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        handleDeleteVertexById(vertexId);
    }

    private void handleMapDeleteEdge(String edgeKey) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        Edge edge = findRoadByEdgeKey(edgeKey);
        if (edge == null) {
            feedback.showErrorDialog("删除失败", "未找到要删除的道路对象。");
            return;
        }
        if (!feedback.confirm("确认删除", "确定删除道路 " + edge.getFromVertex().getId() + " -> " + edge.getToVertex().getId() + " 吗？")) {
            return;
        }
        final Edge snapshot = edge;
        executeAdminEditCommand("正在删除道路...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                mapController.deleteRoad(admin, snapshot.getFromVertex().getId(), snapshot.getToVertex().getId());
            }

            @Override
            public void undo(Admin admin) {
                mapController.addRoad(admin, snapshot.getFromVertex().getId(), snapshot.getToVertex().getId(), snapshot.getWeight(), snapshot.isOneWay(), snapshot.isForbidden(), snapshot.getRoadType());
            }

            @Override
            public String successMessage() {
                return "地图删除道路成功。";
            }
        });
    }

    private void handleBatchDeleteSelectedVertices() {
        if (!isAdminEditingAvailable()) {
            return;
        }
        if (mapSelectedVertexIds.isEmpty()) {
            feedback.showErrorDialog("批量删除", "请先在地图中框选要删除的点位。");
            return;
        }
        if (!feedback.confirm("批量删除确认", "确定删除选中的 " + mapSelectedVertexIds.size() + " 个点位吗？")) {
            return;
        }
        final List<String> selectedIds = new ArrayList<String>(mapSelectedVertexIds);
        final Set<String> idSet = new HashSet<String>(selectedIds);
        final List<Vertex> vertexSnapshots = new ArrayList<Vertex>();
        for (String id : selectedIds) {
            Vertex vertex = findVertexById(id);
            if (vertex != null) {
                vertexSnapshots.add(vertex);
            }
        }
        final List<Edge> relatedRoads = listRelatedCanonicalRoads(idSet);
        executeAdminEditCommand("正在批量删除点位...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                for (String id : selectedIds) {
                    mapController.deleteVertex(admin, id);
                }
            }

            @Override
            public void undo(Admin admin) {
                for (Vertex vertex : vertexSnapshots) {
                    mapController.addVertex(admin, vertex.getId(), vertex.getName(), vertex.getType(), vertex.getX(), vertex.getY(), vertex.getDescription());
                }
                restoreRoads(admin, relatedRoads);
            }

            @Override
            public String successMessage() {
                return "批量删除成功。";
            }
        });
    }

    private void handleBatchForbiddenBySelection(boolean forbidden) {
        if (!isAdminEditingAvailable()) {
            return;
        }
        if (mapSelectedVertexIds.size() < 2) {
            feedback.showErrorDialog("批量禁行", "请先框选至少两个点位。");
            return;
        }
        final Set<String> selected = new HashSet<String>(mapSelectedVertexIds);
        final List<Edge> candidates = new ArrayList<Edge>();
        for (Edge edge : listCanonicalRoads()) {
            String fromId = edge.getFromVertex().getId();
            String toId = edge.getToVertex().getId();
            if (selected.contains(fromId) && selected.contains(toId) && edge.isForbidden() != forbidden) {
                candidates.add(edge);
            }
        }
        if (candidates.isEmpty()) {
            feedback.info("没有需要变更禁行状态的道路。");
            return;
        }

        final List<Edge> snapshots = new ArrayList<Edge>(candidates);
        executeAdminEditCommand(forbidden ? "正在批量设置禁行..." : "正在批量解除禁行...", new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                for (Edge edge : snapshots) {
                    mapController.setRoadForbidden(admin, edge.getFromVertex().getId(), edge.getToVertex().getId(), forbidden);
                }
            }

            @Override
            public void undo(Admin admin) {
                for (Edge edge : snapshots) {
                    mapController.setRoadForbidden(admin, edge.getFromVertex().getId(), edge.getToVertex().getId(), edge.isForbidden());
                }
            }

            @Override
            public String successMessage() {
                return forbidden ? "批量禁行成功。" : "批量解禁成功。";
            }
        });
    }

    private void handleQuickToggleSelectedEdgeForbidden() {
        if (!isAdminEditingAvailable()) {
            return;
        }
        if (mapSelectedEdgeKey == null) {
            feedback.showErrorDialog("禁行切换", "请先在地图上选中一条道路。");
            return;
        }
        Edge edge = findRoadByEdgeKey(mapSelectedEdgeKey);
        if (edge == null) {
            feedback.showErrorDialog("禁行切换", "未找到选中的道路。");
            return;
        }
        handleSetRoadForbidden(edge.getFromVertex().getId(), edge.getToVertex().getId(), !edge.isForbidden());
    }

    private void executeAdminEditCommand(String loadingText, AdminEditCommand command) {
        if (!ensureAdminLoggedIn()) {
            return;
        }
        Admin operator = currentAdmin;
        try {
            feedback.showLoading(loadingText);
            command.execute(operator);
            undoStack.push(command);
            redoStack.clear();
            refreshAllData();
            feedback.success(command.successMessage());
            feedback.setStatus("管理员模式: " + command.successMessage());
        } catch (RuntimeException ex) {
            feedback.showOperationError("操作失败", ex);
            feedback.setStatus("管理员模式: 操作失败");
        } finally {
            feedback.hideLoading();
            refreshUndoRedoButtons();
        }
    }

    private void undoLastEdit() {
        if (undoStack.isEmpty()) {
            feedback.info("没有可撤销的编辑。");
            return;
        }
        if (!ensureAdminLoggedIn()) {
            return;
        }
        Admin operator = currentAdmin;
        AdminEditCommand command = undoStack.pop();
        try {
            feedback.showLoading("正在撤销上一步编辑...");
            command.undo(operator);
            redoStack.push(command);
            refreshAllData();
            feedback.info("撤销成功。");
            feedback.setStatus("管理员模式: 已撤销");
        } catch (RuntimeException ex) {
            undoStack.push(command);
            feedback.showOperationError("撤销失败", ex);
            feedback.setStatus("管理员模式: 撤销失败");
        } finally {
            feedback.hideLoading();
            refreshUndoRedoButtons();
        }
    }

    private void redoLastEdit() {
        if (redoStack.isEmpty()) {
            feedback.info("没有可重做的编辑。");
            return;
        }
        if (!ensureAdminLoggedIn()) {
            return;
        }
        Admin operator = currentAdmin;
        AdminEditCommand command = redoStack.pop();
        try {
            feedback.showLoading("正在重做上一步编辑...");
            command.execute(operator);
            undoStack.push(command);
            refreshAllData();
            feedback.info("重做成功。");
            feedback.setStatus("管理员模式: 已重做");
        } catch (RuntimeException ex) {
            redoStack.push(command);
            feedback.showOperationError("重做失败", ex);
            feedback.setStatus("管理员模式: 重做失败");
        } finally {
            feedback.hideLoading();
            refreshUndoRedoButtons();
        }
    }

    private void refreshUndoRedoButtons() {
        if (undoButton != null) {
            undoButton.setEnabled(currentAdmin != null && !undoStack.isEmpty());
        }
        if (redoButton != null) {
            redoButton.setEnabled(currentAdmin != null && !redoStack.isEmpty());
        }
    }

    private boolean isAdminEditingAvailable() {
        return activeRoute == AppRoute.ADMIN_MODE && currentAdmin != null;
    }

    private String generateAutoVertexId() {
        Set<String> used = new HashSet<String>();
        for (Vertex vertex : mapController.listVertices()) {
            used.add(vertex.getId());
        }
        while (true) {
            String candidate = "NODE_" + autoVertexCounter++;
            if (!used.contains(candidate)) {
                return candidate;
            }
        }
    }

    private Vertex findVertexById(String vertexId) {
        if (isBlank(vertexId)) {
            return null;
        }
        for (Vertex vertex : mapController.listVertices()) {
            if (vertex.getId().equals(vertexId)) {
                return vertex;
            }
        }
        return null;
    }

    private Vertex requireVertex(String vertexId) {
        Vertex vertex = findVertexById(vertexId);
        if (vertex == null) {
            throw new IllegalArgumentException("未找到点位: " + vertexId);
        }
        return vertex;
    }

    private Edge requireRoad(String fromId, String toId) {
        for (Edge edge : mapController.listRoads()) {
            if (edge.getFromVertex().getId().equals(fromId) && edge.getToVertex().getId().equals(toId)) {
                return edge;
            }
        }
        throw new IllegalArgumentException("未找到道路: " + fromId + " -> " + toId);
    }

    private Edge findRoadByEdgeKey(String edgeKey) {
        if (isBlank(edgeKey)) {
            return null;
        }
        for (Edge edge : listCanonicalRoads()) {
            if (toRoadKey(edge).equals(edgeKey)) {
                return edge;
            }
        }
        return null;
    }

    private List<Edge> listCanonicalRoads() {
        List<Edge> roads = mapController.listRoads();
        Map<String, Edge> deduped = new LinkedHashMap<String, Edge>();
        for (Edge edge : roads) {
            String key = toRoadKey(edge);
            if (!deduped.containsKey(key)) {
                deduped.put(key, edge);
            }
        }
        return new ArrayList<Edge>(deduped.values());
    }

    private List<Edge> listRelatedCanonicalRoads(Set<String> vertexIds) {
        List<Edge> roads = new ArrayList<Edge>();
        for (Edge edge : listCanonicalRoads()) {
            String fromId = edge.getFromVertex().getId();
            String toId = edge.getToVertex().getId();
            if (vertexIds.contains(fromId) || vertexIds.contains(toId)) {
                roads.add(edge);
            }
        }
        return roads;
    }

    private void restoreRoads(Admin admin, List<Edge> roads) {
        for (Edge edge : roads) {
            mapController.addRoad(admin, edge.getFromVertex().getId(), edge.getToVertex().getId(), edge.getWeight(), edge.isOneWay(), edge.isForbidden(), edge.getRoadType());
        }
    }

    private static String toRoadKey(Edge edge) {
        String from = edge.getFromVertex().getId();
        String to = edge.getToVertex().getId();
        if (edge.isOneWay()) {
            return "ONE:" + from + "->" + to;
        }
        if (from.compareTo(to) <= 0) {
            return "TWO:" + from + "<->" + to;
        }
        return "TWO:" + to + "<->" + from;
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
            if (activeEditToolMode == EditToolMode.ADD_VERTEX) {
                return "管理员模式-添加点：点击地图空白区域创建点位。";
            }
            if (activeEditToolMode == EditToolMode.ADD_EDGE) {
                if (pendingEdgeStartVertexId == null) {
                    return "管理员模式-连线：点击起点，再点击终点创建道路。";
                }
                return "管理员模式-连线：起点已选 " + pendingEdgeStartVertexId + "，请点击终点。";
            }
            if (activeEditToolMode == EditToolMode.MOVE_VERTEX) {
                return "管理员模式-移动点：拖拽点位到新位置。";
            }
            if (activeEditToolMode == EditToolMode.DELETE_OBJECT) {
                return "管理员模式-删除对象：点击点位或道路立即删除。";
            }
            return "管理员模式-选择：可框选多点，支持批量删除与批量禁行。";
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
            setAdminEditMode(EditToolMode.SELECT);
        } else {
            adminInfoLabel.setText("当前状态：已登录（" + currentAdmin.getUsername() + "）");
            adminCardLayout.show(adminCardPanel, "UNLOCKED");
        }
        boolean enabled = currentAdmin != null;
        for (JButton button : adminToolButtons.values()) {
            button.setEnabled(enabled);
        }
        if (undoButton != null) {
            undoButton.setEnabled(enabled && !undoStack.isEmpty());
        }
        if (redoButton != null) {
            redoButton.setEnabled(enabled && !redoStack.isEmpty());
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

    private interface AdminEditCommand {
        void execute(Admin admin);

        void undo(Admin admin);

        String successMessage();
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
