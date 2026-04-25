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


final class MainViewDataRefresher {
    private final MainView view;

    MainViewDataRefresher(MainView view) {
        this.view = view;
    }

    void handlePathQuery(String startId, String endId) {
        if (MainView.isBlank(startId) || MainView.isBlank(endId)) {
            view.feedback.showErrorDialog("参数缺失", "请先选择起点与终点。");
            return;
        }
        try {
            view.feedback.showLoading("正在计算最短路径...");
            NavigationController.NavigationVisualResult result = view.navigationController.queryPathVisual(startId, endId);
            PathResult pathResult = result.getPathResult();
            view.pathQueryView.setResultContent(view.navigationController.format(pathResult));
            view.pathQueryView.setInstructions(pathResult.getNaviInstructions());
            if (view.viewState.getCurrentPathResult() == null) {
                view.viewState.setPreviousRouteVisualization(null);
            } else {
                view.viewState.setPreviousRouteVisualization(view.navigationController.toTraceRouteVisualization(view.viewState.getCurrentPathResult()));
            }
            view.viewState.setCurrentPathResult(pathResult);
            view.viewState.setCurrentRouteVisualization(result.getRouteVisualization());
            view.mapCanvas.setRouteComparison(view.viewState.getCurrentRouteVisualization(), view.viewState.getPreviousRouteVisualization());
            view.feedback.success("路径查询成功。");
            view.feedback.setStatus("用户模式: 路径查询完成");
        } catch (RuntimeException ex) {
            view.feedback.showOperationError("查询失败", ex);
            view.feedback.setStatus("用户模式: 路径查询失败");
        } finally {
            view.feedback.hideLoading();
        }
    }

    void refreshAllData() {
        refreshMapCanvas();
        refreshPathOptions();
        refreshPlaceData(view.placeBrowseView.selectedType());
        refreshVertexData();
        refreshRoadData();
        refreshForbiddenData();
        refreshOverviewData();
        view.updateAdminAccessUi();
        view.refreshUndoRedoButtons();
        if (view.activeRoute == AppRoute.ADMIN_MODE) {
            view.handleAdminMapSelectionContext();
        }
    }

    void refreshPathOptions() {
        String selectedStart = view.pathQueryView.selectedStartId();
        String selectedEnd = view.pathQueryView.selectedEndId();
        List<VertexOption> options = view.mapController.listVertexOptions();
        view.pathQueryView.setVertexOptions(options, selectedStart, selectedEnd);
    }

    void refreshMapCanvas() {
        List<Vertex> vertices = view.mapController.listVertices();
        List<Edge> roads = view.mapController.listRoads();
        view.mapCanvas.setGraphData(vertices, roads);
        view.mapCanvas.setRouteComparison(view.viewState.getCurrentRouteVisualization(), view.viewState.getPreviousRouteVisualization());
    }

    void refreshPlaceData(String selectedType) {
        List<com.zhixing.navigation.domain.model.Vertex> vertices;
        if (PlaceBrowseView.ALL_PLACE_TYPES.equals(selectedType)) {
            vertices = view.mapController.listVertices();
        } else {
            vertices = view.mapController.listVerticesByType(PlaceType.valueOf(selectedType));
        }
        view.placeBrowseView.setPlaces(vertices);
    }

    void refreshVertexData() {
        view.vertexManageView.setVertices(view.mapController.listVertices());
    }

    void refreshRoadData() {
        String selectedFrom = view.roadManageView.selectedFromId();
        String selectedTo = view.roadManageView.selectedToId();
        view.roadManageView.setVertexOptions(view.mapController.listVertexOptions(), selectedFrom, selectedTo);
        view.roadManageView.setRoads(view.mapController.listRoads());
    }

    void refreshForbiddenData() {
        String selectedKey = view.forbiddenManageView.selectedRoadKey();
        view.forbiddenManageView.setRoadOptions(view.mapController.listRoadOptions(), selectedKey);
    }

    void refreshOverviewData() {
        OverviewData overview = view.mapController.loadOverview();
        view.overviewDashboardView.setOverviewData(overview);
    }

}
