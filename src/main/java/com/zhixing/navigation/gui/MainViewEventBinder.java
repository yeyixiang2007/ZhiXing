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


final class MainViewEventBinder {
    private final MainView view;

    MainViewEventBinder(MainView view) {
        this.view = view;
    }

    void wireViewEvents() {
        bindPathQueryEvents();
        bindMapCanvasEvents();
        bindVertexManageEvents();
        bindRoadManageEvents();
        bindForbiddenManageEvents();
        bindOverviewAndLayerEvents();
    }

    void bindPathQueryEvents() {
        view.pathQueryView.setListener(new PathQueryView.Listener() {
            @Override
            public void onQuery(String startId, String endId) {
                view.handlePathQuery(startId, endId);
            }

            @Override
            public void onMapPickTargetChanged(PathQueryView.MapPickTarget target) {
                if (target == PathQueryView.MapPickTarget.START) {
                    view.mapWorkbenchView.setMapHint("地图选点模式：请在地图中点击起点。");
                } else if (target == PathQueryView.MapPickTarget.END) {
                    view.mapWorkbenchView.setMapHint("地图选点模式：请在地图中点击终点。");
                } else {
                    view.mapWorkbenchView.setMapHint(view.resolveMapHint(view.activeRoute));
                }
            }

            @Override
            public void onInstructionSelected(int index) {
                int segmentIndex = index;
                RouteVisualizationDto currentRouteVisualization = view.viewState.getCurrentRouteVisualization();
                if (currentRouteVisualization != null && currentRouteVisualization.getSegmentCount() > 0) {
                    int maxSegment = currentRouteVisualization.getSegmentCount() - 1;
                    if (segmentIndex > maxSegment) {
                        segmentIndex = maxSegment;
                    }
                }
                view.mapCanvas.focusRouteSegment(segmentIndex);
                view.feedback.setStatus("步骤联动: 已定位到第 " + (index + 1) + " 步");
            }
        });
        view.placeBrowseView.setListener(view::refreshPlaceData);
    }

    void bindMapCanvasEvents() {
        view.mapCanvas.setListener(new MapCanvas.Listener() {
            @Override
            public void onSelectionChanged(List<String> selectedVertexIds, String selectedEdgeKey) {
                view.viewState.setSelectedVertexIds(selectedVertexIds);
                view.viewState.setSelectedEdgeKey(selectedEdgeKey);
                if (view.activeRoute == AppRoute.ADMIN_MODE) {
                    handleAdminMapSelectionContext();
                    return;
                }
                if (selectedEdgeKey != null) {
                    view.feedback.setStatus("地图选择: " + selectedEdgeKey);
                    return;
                }
                if (selectedVertexIds.isEmpty()) {
                    view.feedback.setStatus("地图选择: 无");
                    return;
                }
                view.feedback.setStatus("地图选择: " + String.join(", ", selectedVertexIds));
            }

            @Override
            public void onViewportChanged(double zoom, double panX, double panY) {
                int percent = (int) Math.round(zoom * 100.0);
                view.feedback.setStatus("地图视图: 缩放 " + percent + "%, 平移(" + (int) Math.round(panX) + "," + (int) Math.round(panY) + ")");
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
                view.viewState.setPendingEdgeStartVertexId(startVertexId);
                if (view.activeRoute == AppRoute.ADMIN_MODE && view.viewState.getActiveEditToolMode() == EditToolMode.ADD_EDGE) {
                    if (startVertexId == null) {
                        view.feedback.setStatus("管理员连线模式: 请选择第一点");
                    } else {
                        view.feedback.setStatus("管理员连线模式: 起点已选 " + startVertexId + "，请点击终点");
                    }
                }
            }

            @Override
            public void onCanvasHint(String message) {
                view.feedback.info(message);
                view.feedback.setStatus("画布提示: " + message);
            }
        });
    }

    void bindVertexManageEvents() {
        view.vertexManageView.setListener(new VertexManageView.Listener() {
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

    void bindRoadManageEvents() {
        view.roadManageView.setListener(new RoadManageView.Listener() {
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

    void bindForbiddenManageEvents() {
        view.forbiddenManageView.setListener(new ForbiddenManageView.Listener() {
            @Override
            public void onToggleForbidden(RoadOption roadOption, boolean forbidden) {
                if (roadOption == null) {
                    view.feedback.showErrorDialog("参数缺失", "当前没有可操作的道路。");
                    return;
                }
                handleSetRoadForbidden(roadOption.getFromId(), roadOption.getToId(), forbidden);
            }

            @Override
            public void onRefresh() {
                view.refreshForbiddenData();
            }
        });
    }

    void bindOverviewAndLayerEvents() {
        view.overviewDashboardView.setListener(view::refreshOverviewData);
        view.layerPanel.setListener(message -> {
            view.feedback.info(message);
            view.feedback.setStatus("图层面板: " + message);
        });
    }

    void installGlobalShortcuts() {
        JComponent root = view.getRootPane();
        bindShortcut(root, "shortcut-delete", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                handleShortcutDelete();
            }
        });
        bindShortcut(root, "shortcut-undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (view.activeRoute == AppRoute.ADMIN_MODE) {
                    undoLastEdit();
                }
            }
        });
        bindShortcut(root, "shortcut-redo", KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (view.activeRoute == AppRoute.ADMIN_MODE) {
                    redoLastEdit();
                }
            }
        });
    }

    void bindShortcut(JComponent component, String actionKey, KeyStroke keyStroke, Action action) {
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionKey);
        component.getActionMap().put(actionKey, action);
    }

    void handleShortcutDelete() {
        if (!isAdminEditingAvailable()) {
            return;
        }
        String mapSelectedEdgeKey = view.viewState.getSelectedEdgeKey();
        List<String> mapSelectedVertexIds = view.viewState.getSelectedVertexIds();
        if (mapSelectedEdgeKey != null) {
            handleMapDeleteEdge(mapSelectedEdgeKey);
            return;
        }
        if (mapSelectedVertexIds.isEmpty()) {
            view.feedback.info("Delete: 当前未选中对象。");
            return;
        }
        if (mapSelectedVertexIds.size() == 1) {
            handleMapDeleteVertex(mapSelectedVertexIds.get(0));
            return;
        }
        handleBatchDeleteSelectedVertices();
    }

    void handleMapVertexActivation(String vertexId) {
        if (view.activeRoute != AppRoute.USER_MODE) {
            return;
        }
        if (view.pathQueryView.mapPickTarget() == PathQueryView.MapPickTarget.NONE) {
            return;
        }
        view.pathQueryView.pickVertex(vertexId);
        if (view.pathQueryView.mapPickTarget() == PathQueryView.MapPickTarget.NONE) {
            view.mapWorkbenchView.setMapHint(view.resolveMapHint(view.activeRoute));
        } else if (view.pathQueryView.mapPickTarget() == PathQueryView.MapPickTarget.START) {
            view.mapWorkbenchView.setMapHint("地图选点模式：请在地图中点击起点。");
        } else {
            view.mapWorkbenchView.setMapHint("地图选点模式：请在地图中点击终点。");
        }
    }

    void handleAdminMapSelectionContext() {
        String mapSelectedEdgeKey = view.viewState.getSelectedEdgeKey();
        List<String> mapSelectedVertexIds = view.viewState.getSelectedVertexIds();
        if (mapSelectedEdgeKey != null) {
            Edge selectedEdge = view.adminEditCoordinator.findRoadByEdgeKey(mapSelectedEdgeKey);
            if (selectedEdge != null) {
                view.roadManageView.fillFromEdge(selectedEdge);
                view.showAdminSection(MainView.ADMIN_SECTION_ROAD);
                view.feedback.setStatus("地图选择: 道路 " + selectedEdge.getFromVertex().getId() + " -> " + selectedEdge.getToVertex().getId());
                return;
            }
        }
        if (mapSelectedVertexIds.size() == 1) {
            Vertex selectedVertex = view.adminEditCoordinator.findVertexById(mapSelectedVertexIds.get(0));
            if (selectedVertex != null) {
                view.vertexManageView.fillFromVertex(selectedVertex);
                view.showAdminSection(MainView.ADMIN_SECTION_VERTEX);
                view.feedback.setStatus("地图选择: 点位 " + selectedVertex.getId());
                return;
            }
        }
        if (mapSelectedVertexIds.size() > 1) {
            view.feedback.setStatus("地图选择: 已选中 " + mapSelectedVertexIds.size() + " 个点位（可批量操作）");
            return;
        }
        view.feedback.setStatus("地图选择: 无");
    }

    void handleAddVertexFromForm(VertexManageView.VertexFormData formData) {
        view.adminEditCoordinator.handleAddVertexFromForm(formData);
    }

    void handleUpdateVertexFromForm(VertexManageView.VertexFormData formData) {
        view.adminEditCoordinator.handleUpdateVertexFromForm(formData);
    }

    void handleDeleteVertexById(String id) {
        view.adminEditCoordinator.handleDeleteVertexById(id);
    }

    void handleAddRoadFromForm(RoadManageView.RoadFormData data) {
        view.adminEditCoordinator.handleAddRoadFromForm(data);
    }

    void handleUpdateRoadFromForm(RoadManageView.RoadFormData data) {
        view.adminEditCoordinator.handleUpdateRoadFromForm(data);
    }

    void handleDeleteRoadFromForm(RoadManageView.RoadFormData data) {
        view.adminEditCoordinator.handleDeleteRoadFromForm(data);
    }

    void handleSetRoadForbidden(String fromId, String toId, boolean forbidden) {
        view.adminEditCoordinator.handleSetRoadForbidden(fromId, toId, forbidden);
    }

    void handleMapAddVertex(double x, double y) {
        view.adminEditCoordinator.handleMapAddVertex(x, y);
    }

    void handleMapConnectVertices(String fromId, String toId) {
        view.adminEditCoordinator.handleMapConnectVertices(fromId, toId);
    }

    void handleMapMoveVertex(String vertexId, double x, double y) {
        view.adminEditCoordinator.handleMapMoveVertex(vertexId, x, y);
    }

    void handleMapDeleteVertex(String vertexId) {
        view.adminEditCoordinator.handleMapDeleteVertex(vertexId);
    }

    void handleMapDeleteEdge(String edgeKey) {
        view.adminEditCoordinator.handleMapDeleteEdge(edgeKey);
    }

    void handleBatchDeleteSelectedVertices() {
        view.adminEditCoordinator.handleBatchDeleteSelectedVertices();
    }

    void handleBatchForbiddenBySelection(boolean forbidden) {
        view.adminEditCoordinator.handleBatchForbiddenBySelection(forbidden);
    }

    void handleQuickToggleSelectedEdgeForbidden() {
        view.adminEditCoordinator.handleQuickToggleSelectedEdgeForbidden();
    }

    void undoLastEdit() {
        view.adminEditCoordinator.undoLastEdit();
    }

    void redoLastEdit() {
        view.adminEditCoordinator.redoLastEdit();
    }

    void refreshUndoRedoButtons() {
        if (view.undoButton != null) {
            view.undoButton.setEnabled(view.currentAdmin != null && view.adminEditCoordinator.canUndo());
        }
        if (view.redoButton != null) {
            view.redoButton.setEnabled(view.currentAdmin != null && view.adminEditCoordinator.canRedo());
        }
    }

    boolean isAdminEditingAvailable() {
        return view.adminEditCoordinator.isAdminEditingAvailable();
    }

    Vertex requireVertex(String vertexId) {
        Vertex vertex = view.adminEditCoordinator.findVertexById(vertexId);
        if (vertex != null) {
            return vertex;
        }
        throw new IllegalArgumentException("未找到点位: " + vertexId);
    }

}
