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
    static final String ADMIN_SECTION_VERTEX = "VERTEX";
    static final String ADMIN_SECTION_ROAD = "ROAD";
    static final String ADMIN_SECTION_FORBIDDEN = "FORBIDDEN";
    static final String ADMIN_SECTION_OVERVIEW = "OVERVIEW";
    static final int LEFT_NAV_WIDTH = 392;
    static final int LEFT_NAV_MIN_WIDTH = 340;
    static final int MAP_OVERLAY_MARGIN = 16;
    static final int MAP_OVERLAY_TOOLBAR_WIDTH = 84;

    final AuthController authController;
    final NavigationController navigationController;
    final MapController mapController;

    final MapWorkbenchView mapWorkbenchView;
    final MapCanvas mapCanvas;
    final LayerPanel layerPanel;
    final LoadingOverlay loadingOverlay;
    final WorkbenchFeedback feedback;
    final Map<AppRoute, JButton> navButtons;
    final Map<String, JButton> adminSectionButtons;
    final Map<EditToolMode, JButton> adminToolButtons;
    final List<JButton> adminOverlayButtons;
    final CommandBus<Admin> adminCommandBus;
    final MapViewState viewState;
    final MainViewAdminEditCoordinator adminEditCoordinator;
    final String startupTimeText;
    final MainViewLayoutBuilder layoutBuilder;
    final MainViewAdminToolbarBuilder adminToolbarBuilder;
    final MainViewEventBinder eventBinder;
    final MainViewDataRefresher dataRefresher;
    final MainViewWorkbenchTools workbenchTools;
    final MainViewNavigationState navigationState;

    PathQueryView pathQueryView;
    PlaceBrowseView placeBrowseView;
    VertexManageView vertexManageView;
    RoadManageView roadManageView;
    ForbiddenManageView forbiddenManageView;
    OverviewDashboardView overviewDashboardView;

    Admin currentAdmin;
    AppRoute activeRoute;
    CardLayout adminCardLayout;
    JPanel adminCardPanel;
    CardLayout adminWorkspaceLayout;
    JPanel adminWorkspacePanel;
    JLayeredPane mapLayeredPane;
    JPanel mapOverlayToolbar;
    JButton undoButton;
    JButton redoButton;
    JLabel topModeBadgeLabel;
    JLabel topModeDescriptionLabel;
    JLabel topSessionBadgeLabel;
    JPanel topStatusPanel;

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
        this.layoutBuilder = new MainViewLayoutBuilder(this);
        this.adminToolbarBuilder = new MainViewAdminToolbarBuilder(this);
        this.eventBinder = new MainViewEventBinder(this);
        this.dataRefresher = new MainViewDataRefresher(this);
        this.workbenchTools = new MainViewWorkbenchTools(this);
        this.navigationState = new MainViewNavigationState(this);

        UiStyles.installDefaults();
        initializeFrame();
        initializeLayout();
        wireViewEvents();
        installGlobalShortcuts();
        refreshAllData();
        navigateTo(AppRoute.USER_MODE);
    }

    void initializeFrame() { layoutBuilder.initializeFrame(); }
    void initializeLayout() { layoutBuilder.initializeLayout(); }
    JPanel createTopBar() { return layoutBuilder.createTopBar(); }
    JPanel createLeftNavigation() { return layoutBuilder.createLeftNavigation(); }
    JPanel createCenterWorkspace() { return layoutBuilder.createCenterWorkspace(); }
    JLayeredPane createMapOverlayContent() { return layoutBuilder.createMapOverlayContent(); }
    void layoutMapOverlayComponents() { layoutBuilder.layoutMapOverlayComponents(); }
    JPanel createUserModeView() { return layoutBuilder.createUserModeView(); }
    JPanel createAdminModeView() { return layoutBuilder.createAdminModeView(); }
    JPanel createAdminLockedPanel() { return layoutBuilder.createAdminLockedPanel(); }
    JPanel createAdminWorkspace() { return layoutBuilder.createAdminWorkspace(); }
    JPanel createMapOverlayToolbar() { return adminToolbarBuilder.createMapOverlayToolbar(); }
    void configureOverlayScrollPane(JScrollPane scrollPane) { adminToolbarBuilder.configureOverlayScrollPane(scrollPane); }
    JButton createPaletteModeButton(EditToolMode mode, String glyph, String tooltip) { return adminToolbarBuilder.createPaletteModeButton(mode, glyph, tooltip); }
    JButton createPaletteActionButton(String glyph, String tooltip, Runnable action) { return adminToolbarBuilder.createPaletteActionButton(glyph, tooltip, action); }
    JButton createPaletteButton(String glyph, String tooltip) { return adminToolbarBuilder.createPaletteButton(glyph, tooltip); }
    JLabel createPaletteSectionLabel(String text) { return adminToolbarBuilder.createPaletteSectionLabel(text); }
    JButton createAdminSectionButton(String sectionKey, String text) { return adminToolbarBuilder.createAdminSectionButton(sectionKey, text); }
    void styleAdminHeaderButton(JButton button) { adminToolbarBuilder.styleAdminHeaderButton(button); }
    void showAdminSection(String sectionKey) { adminToolbarBuilder.showAdminSection(sectionKey); }
    void applyAdminSectionButtonStyle(JButton button, boolean active) { adminToolbarBuilder.applyAdminSectionButtonStyle(button, active); }
    void setAdminEditMode(EditToolMode mode) { adminToolbarBuilder.setAdminEditMode(mode); }
    void updateAdminToolButtonStyle() { adminToolbarBuilder.updateAdminToolButtonStyle(); }
    void applyAdminToolButtonStyle(JButton button, boolean active) { adminToolbarBuilder.applyAdminToolButtonStyle(button, active); }
    JPanel createSystemSettingsView() {
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

    void wireViewEvents() { eventBinder.wireViewEvents(); }
    void bindPathQueryEvents() { eventBinder.bindPathQueryEvents(); }
    void bindMapCanvasEvents() { eventBinder.bindMapCanvasEvents(); }
    void bindVertexManageEvents() { eventBinder.bindVertexManageEvents(); }
    void bindRoadManageEvents() { eventBinder.bindRoadManageEvents(); }
    void bindForbiddenManageEvents() { eventBinder.bindForbiddenManageEvents(); }
    void bindOverviewAndLayerEvents() { eventBinder.bindOverviewAndLayerEvents(); }
    void installGlobalShortcuts() { eventBinder.installGlobalShortcuts(); }
    void bindShortcut(JComponent component, String actionKey, KeyStroke keyStroke, Action action) { eventBinder.bindShortcut(component, actionKey, keyStroke, action); }
    void handleShortcutDelete() { eventBinder.handleShortcutDelete(); }
    void handleMapVertexActivation(String vertexId) { eventBinder.handleMapVertexActivation(vertexId); }
    void handleAdminMapSelectionContext() { eventBinder.handleAdminMapSelectionContext(); }
    void handleAddVertexFromForm(VertexManageView.VertexFormData formData) { eventBinder.handleAddVertexFromForm(formData); }
    void handleUpdateVertexFromForm(VertexManageView.VertexFormData formData) { eventBinder.handleUpdateVertexFromForm(formData); }
    void handleDeleteVertexById(String id) { eventBinder.handleDeleteVertexById(id); }
    void handleAddRoadFromForm(RoadManageView.RoadFormData data) { eventBinder.handleAddRoadFromForm(data); }
    void handleUpdateRoadFromForm(RoadManageView.RoadFormData data) { eventBinder.handleUpdateRoadFromForm(data); }
    void handleDeleteRoadFromForm(RoadManageView.RoadFormData data) { eventBinder.handleDeleteRoadFromForm(data); }
    void handleSetRoadForbidden(String fromId, String toId, boolean forbidden) { eventBinder.handleSetRoadForbidden(fromId, toId, forbidden); }
    void handleMapAddVertex(double x, double y) { eventBinder.handleMapAddVertex(x, y); }
    void handleMapConnectVertices(String fromId, String toId) { eventBinder.handleMapConnectVertices(fromId, toId); }
    void handleMapMoveVertex(String vertexId, double x, double y) { eventBinder.handleMapMoveVertex(vertexId, x, y); }
    void handleMapDeleteVertex(String vertexId) { eventBinder.handleMapDeleteVertex(vertexId); }
    void handleMapDeleteEdge(String edgeKey) { eventBinder.handleMapDeleteEdge(edgeKey); }
    void handleBatchDeleteSelectedVertices() { eventBinder.handleBatchDeleteSelectedVertices(); }
    void handleBatchForbiddenBySelection(boolean forbidden) { eventBinder.handleBatchForbiddenBySelection(forbidden); }
    void handleQuickToggleSelectedEdgeForbidden() { eventBinder.handleQuickToggleSelectedEdgeForbidden(); }
    void undoLastEdit() { eventBinder.undoLastEdit(); }
    void redoLastEdit() { eventBinder.redoLastEdit(); }
    void refreshUndoRedoButtons() { eventBinder.refreshUndoRedoButtons(); }
    boolean isAdminEditingAvailable() { return eventBinder.isAdminEditingAvailable(); }
    Vertex requireVertex(String vertexId) { return eventBinder.requireVertex(vertexId); }
    static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    void handlePathQuery(String startId, String endId) { dataRefresher.handlePathQuery(startId, endId); }
    void refreshAllData() { dataRefresher.refreshAllData(); }
    void refreshPathOptions() { dataRefresher.refreshPathOptions(); }
    void refreshMapCanvas() { dataRefresher.refreshMapCanvas(); }
    void refreshPlaceData(String selectedType) { dataRefresher.refreshPlaceData(selectedType); }
    void refreshVertexData() { dataRefresher.refreshVertexData(); }
    void refreshRoadData() { dataRefresher.refreshRoadData(); }
    void refreshForbiddenData() { dataRefresher.refreshForbiddenData(); }
    void refreshOverviewData() { dataRefresher.refreshOverviewData(); }
    void showWorkbenchToolsMenu(JButton anchorButton) { workbenchTools.showWorkbenchToolsMenu(anchorButton); }
    void promptImportReferenceImage() { workbenchTools.promptImportReferenceImage(); }
    void handleClearReferenceImage() { workbenchTools.handleClearReferenceImage(); }
    void promptSetReferenceImageScale() { workbenchTools.promptSetReferenceImageScale(); }
    void promptSetMetersPerWorldUnit() { workbenchTools.promptSetMetersPerWorldUnit(); }
    void promptCalibrateMetersPerWorldUnit() { workbenchTools.promptCalibrateMetersPerWorldUnit(); }
    void promptBackupFromWorkbenchTools() { workbenchTools.promptBackupFromWorkbenchTools(); }
    void promptRestoreFromWorkbenchTools() { workbenchTools.promptRestoreFromWorkbenchTools(); }
    void handleBackup(String backupName, String sourceTag) { workbenchTools.handleBackup(backupName, sourceTag); }
    void handleRestore(String backupName, String sourceTag) { workbenchTools.handleRestore(backupName, sourceTag); }
    void navigateTo(AppRoute route) { navigationState.navigateTo(route); }
    String resolveMapHint(AppRoute route) { return navigationState.resolveMapHint(route); }
    boolean showAdminLoginDialog() { return navigationState.showAdminLoginDialog(); }
    boolean ensureAdminLoggedIn() { return navigationState.ensureAdminLoggedIn(); }
    void handleAdminLogout() { navigationState.handleAdminLogout(); }
    void updateAdminAccessUi() { navigationState.updateAdminAccessUi(); }
    void updateMapOverlayToolbarVisibility() { navigationState.updateMapOverlayToolbarVisibility(); }
    void updateNavState(AppRoute activeRoute) { navigationState.updateNavState(activeRoute); }
    JPanel createRailHeader(String title, String description) { return navigationState.createRailHeader(title, description); }
    void applyNavigationButtonStyle(JButton button, boolean active) { navigationState.applyNavigationButtonStyle(button, active); }
    void updateShellHeaderState() { navigationState.updateShellHeaderState(); }
    void applyTopBarBadgeStyle(JLabel label, Color background, Color foreground, Color borderColor) { navigationState.applyTopBarBadgeStyle(label, background, foreground, borderColor); }
    void refreshTopBarLayout() { navigationState.refreshTopBarLayout(); }
    String resolveShellModeDescription(AppRoute route) { return navigationState.resolveShellModeDescription(route); }
    static void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, java.awt.Component component) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(UiStyles.formLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
        gbc.weightx = 0;
    }

    static double parseDouble(String text, String fieldName) {
        try {
            return Double.parseDouble(text == null ? "" : text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " 请输入有效数字。");
        }
    }

    static double parsePositiveDouble(String text, String fieldName) {
        double value = parseDouble(text, fieldName);
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " 必须大于0。");
        }
        return value;
    }

    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
