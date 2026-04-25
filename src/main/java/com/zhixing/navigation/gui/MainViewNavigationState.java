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


final class MainViewNavigationState {
    private final MainView view;

    MainViewNavigationState(MainView view) {
        this.view = view;
    }

    void navigateTo(AppRoute route) {
        view.activeRoute = route;
        view.viewState.setActiveRoute(route);
        if (route == AppRoute.ADMIN_MODE && view.currentAdmin == null) {
            showAdminLoginDialog();
        }
        view.mapWorkbenchView.showRoute(route);
        view.mapWorkbenchView.setMapHint(resolveMapHint(route));
        view.feedback.setStatus("当前页面: " + route.getTitle());
        updateNavState(route);
        updateAdminAccessUi();
        updateMapOverlayToolbarVisibility();
        updateShellHeaderState();
    }

    String resolveMapHint(AppRoute route) {
        if (route == AppRoute.USER_MODE) {
            return "用户模式：左键框选/点选，右键拖拽平移，滚轮缩放。";
        }
        if (route == AppRoute.ADMIN_MODE) {
            if (view.currentAdmin == null) {
                return "管理员模式：请先登录，登录后可进行地图编辑与数据维护。";
            }
            EditToolMode activeEditToolMode = view.viewState.getActiveEditToolMode();
            if (activeEditToolMode == EditToolMode.ADD_VERTEX) {
                return "管理员模式-添加点：点击地图空白区域创建点位。";
            }
            if (activeEditToolMode == EditToolMode.ADD_EDGE) {
                String pendingEdgeStartVertexId = view.viewState.getPendingEdgeStartVertexId();
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

    boolean showAdminLoginDialog() {
        AdminLoginDialog dialog = new AdminLoginDialog(view, view.authController);
        Admin admin = dialog.showDialog();
        if (admin == null) {
            view.feedback.info("管理员未登录。");
            view.feedback.setStatus("管理员模式: 未登录");
            updateAdminAccessUi();
            return false;
        }
        view.currentAdmin = admin;
        view.feedback.success("管理员登录成功：" + view.currentAdmin.getUsername());
        view.feedback.setStatus("管理员模式: 已登录");
        updateAdminAccessUi();
        return true;
    }

    boolean ensureAdminLoggedIn() {
        if (view.currentAdmin != null) {
            return true;
        }
        return showAdminLoginDialog();
    }

    void handleAdminLogout() {
        if (view.currentAdmin == null) {
            return;
        }
        if (!view.feedback.confirm("确认退出", "确定退出当前管理员账号吗？")) {
            return;
        }
        view.currentAdmin = null;
        updateAdminAccessUi();
        view.feedback.info("管理员已退出登录。");
        view.feedback.setStatus("管理员模式: 已退出登录");
    }

    void updateAdminAccessUi() {
        if (view.adminCardLayout == null || view.adminCardPanel == null) {
            return;
        }
        if (view.currentAdmin == null) {
            view.adminCardLayout.show(view.adminCardPanel, "LOCKED");
            view.setAdminEditMode(EditToolMode.SELECT);
        } else {
            view.adminCardLayout.show(view.adminCardPanel, "UNLOCKED");
        }
        boolean enabled = view.currentAdmin != null;
        for (JButton button : view.adminToolButtons.values()) {
            button.setEnabled(enabled);
        }
        for (JButton button : view.adminOverlayButtons) {
            button.setEnabled(enabled);
        }
        if (view.undoButton != null) {
            view.undoButton.setEnabled(enabled && view.adminEditCoordinator.canUndo());
        }
        if (view.redoButton != null) {
            view.redoButton.setEnabled(enabled && view.adminEditCoordinator.canRedo());
        }
        if (view.activeRoute == AppRoute.ADMIN_MODE) {
            view.mapWorkbenchView.setMapHint(resolveMapHint(AppRoute.ADMIN_MODE));
        }
        updateMapOverlayToolbarVisibility();
        updateShellHeaderState();
    }

    void updateMapOverlayToolbarVisibility() {
        if (view.mapOverlayToolbar == null) {
            return;
        }
        view.mapOverlayToolbar.setVisible(view.activeRoute == AppRoute.ADMIN_MODE);
    }

    void updateNavState(AppRoute activeRoute) {
        for (Map.Entry<AppRoute, JButton> entry : view.navButtons.entrySet()) {
            applyNavigationButtonStyle(entry.getValue(), entry.getKey() == view.activeRoute);
        }
    }

    JPanel createRailHeader(String title, String description) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiStyles.SUBTITLE_FONT);
        titleLabel.setForeground(UiStyles.TEXT_PRIMARY);
        titleLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        JTextArea descriptionLabel = new JTextArea(description);
        descriptionLabel.setEditable(false);
        descriptionLabel.setLineWrap(true);
        descriptionLabel.setWrapStyleWord(true);
        descriptionLabel.setOpaque(false);
        descriptionLabel.setFocusable(false);
        descriptionLabel.setFont(UiStyles.CAPTION_FONT);
        descriptionLabel.setForeground(UiStyles.TEXT_SECONDARY);
        descriptionLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder());
        descriptionLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(3));
        header.add(descriptionLabel);
        return header;
    }

    void applyNavigationButtonStyle(JButton button, boolean active) {
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

    void updateShellHeaderState() {
        if (view.topModeBadgeLabel != null) {
            view.topModeBadgeLabel.setText(view.activeRoute.getTitle());
            applyTopBarBadgeStyle(
                    view.topModeBadgeLabel,
                    new Color(255, 255, 255, 30),
                    Color.WHITE,
                    new Color(255, 255, 255, 60)
            );
        }
        if (view.topModeDescriptionLabel != null) {
            view.topModeDescriptionLabel.setText(resolveShellModeDescription(view.activeRoute));
        }
        if (view.topSessionBadgeLabel != null) {
            if (view.currentAdmin == null) {
                view.topSessionBadgeLabel.setText("访客模式");
                applyTopBarBadgeStyle(
                        view.topSessionBadgeLabel,
                        new Color(255, 255, 255, 24),
                        new Color(230, 236, 242),
                        new Color(255, 255, 255, 50)
                );
            } else {
                view.topSessionBadgeLabel.setText("管理员  " + view.currentAdmin.getUsername());
                applyTopBarBadgeStyle(
                        view.topSessionBadgeLabel,
                        new Color(35, 162, 109, 70),
                        Color.WHITE,
                        new Color(140, 230, 187, 120)
                );
            }
        }
        refreshTopBarLayout();
    }

    void applyTopBarBadgeStyle(JLabel label, Color background, Color foreground, Color borderColor) {
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

    void refreshTopBarLayout() {
        if (view.topStatusPanel == null) {
            return;
        }
        view.topStatusPanel.revalidate();
        view.topStatusPanel.repaint();
    }

    String resolveShellModeDescription(AppRoute route) {
        if (route == AppRoute.USER_MODE) {
            return "路径查询、分步导航与地点浏览";
        }
        if (route == AppRoute.ADMIN_MODE) {
            return "地图编辑、图层管理与数据维护";
        }
        return "系统信息、数据目录与维护说明";
    }

}
