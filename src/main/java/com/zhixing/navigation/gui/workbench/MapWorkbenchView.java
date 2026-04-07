package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.gui.components.ViewportFillPanel;
import com.zhixing.navigation.gui.routing.AppRoute;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.CardLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MapWorkbenchView extends JPanel {
    private static final double DEFAULT_RIGHT_PANEL_RATIO = 0.38;
    private static final int DEFAULT_RIGHT_PANEL_WIDTH = 540;
    private static final int MIN_RIGHT_PANEL_WIDTH = 340;
    private static final int MAX_RIGHT_PANEL_WIDTH = 820;
    private static final int MIN_MAP_PANEL_WIDTH = 460;

    private final JLabel routeTitleLabel;
    private final JLabel routeSubtitleLabel;
    private final JPanel mapSurfacePanel;
    private final JPanel mapContentHolder;
    private final JLabel mapHintLabel;
    private final CardLayout sidebarLayout;
    private final JPanel sidebarPanel;
    private final JLabel drawerTitleLabel;
    private final JLabel drawerSubtitleLabel;
    private final JLabel statusLabel;
    private final JSplitPane workbenchSplitPane;

    private boolean dividerAdjustedBySystem;
    private Integer preferredRightPanelWidth;

    public MapWorkbenchView() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UiStyles.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        JPanel routeHeader = new JPanel(new BorderLayout(12, 0));
        routeHeader.setOpaque(false);

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));

        routeTitleLabel = new JLabel();
        routeTitleLabel.setFont(UiStyles.TITLE_FONT);
        routeTitleLabel.setForeground(UiStyles.TEXT_PRIMARY);

        routeSubtitleLabel = new JLabel();
        routeSubtitleLabel.setFont(UiStyles.CAPTION_FONT);
        routeSubtitleLabel.setForeground(UiStyles.TEXT_SECONDARY);

        titleStack.add(routeTitleLabel);
        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(routeSubtitleLabel);

        JLabel stageBadge = new JLabel("地图主舞台", SwingConstants.CENTER);
        stageBadge.setFont(UiStyles.CAPTION_FONT);
        stageBadge.setForeground(UiStyles.TEXT_SECONDARY);
        stageBadge.setOpaque(true);
        stageBadge.setBackground(UiStyles.SURFACE_ALT);
        stageBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        routeHeader.add(titleStack, BorderLayout.WEST);
        routeHeader.add(stageBadge, BorderLayout.EAST);

        mapSurfacePanel = UiStyles.cardPanel(new BorderLayout(0, 10));
        mapSurfacePanel.setBackground(UiStyles.SURFACE);
        mapSurfacePanel.setBorder(UiStyles.cardBorder(12, 12, 12, 12));

        mapContentHolder = new JPanel(new BorderLayout());
        mapContentHolder.setBackground(new Color(236, 242, 248));
        mapSurfacePanel.add(mapContentHolder, BorderLayout.CENTER);

        mapHintLabel = new JLabel();
        mapHintLabel.setFont(UiStyles.CAPTION_FONT);
        mapHintLabel.setForeground(UiStyles.TEXT_SECONDARY);
        mapHintLabel.setOpaque(true);
        mapHintLabel.setBackground(UiStyles.SURFACE_ALT);
        mapHintLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        mapSurfacePanel.add(mapHintLabel, BorderLayout.SOUTH);

        JPanel mapRegion = new JPanel(new BorderLayout());
        mapRegion.setOpaque(false);
        mapRegion.setMinimumSize(new Dimension(MIN_MAP_PANEL_WIDTH, 320));
        mapRegion.add(mapSurfacePanel, BorderLayout.CENTER);

        sidebarLayout = new CardLayout();
        sidebarPanel = new ViewportFillPanel(sidebarLayout);
        sidebarPanel.setOpaque(false);
        sidebarPanel.setBackground(UiStyles.SURFACE);

        drawerTitleLabel = new JLabel();
        drawerTitleLabel.setFont(UiStyles.SUBTITLE_FONT);
        drawerTitleLabel.setForeground(UiStyles.TEXT_PRIMARY);

        drawerSubtitleLabel = new JLabel();
        drawerSubtitleLabel.setFont(UiStyles.CAPTION_FONT);
        drawerSubtitleLabel.setForeground(UiStyles.TEXT_SECONDARY);

        JPanel drawerHeader = new JPanel();
        drawerHeader.setOpaque(false);
        drawerHeader.setLayout(new BoxLayout(drawerHeader, BoxLayout.Y_AXIS));
        drawerHeader.add(drawerTitleLabel);
        drawerHeader.add(Box.createVerticalStrut(3));
        drawerHeader.add(drawerSubtitleLabel);

        JPanel drawerShell = UiStyles.cardPanel(new BorderLayout(0, 12));
        drawerShell.setBackground(UiStyles.SURFACE);
        drawerShell.setPreferredSize(new Dimension(DEFAULT_RIGHT_PANEL_WIDTH, 0));
        drawerShell.setMinimumSize(new Dimension(MIN_RIGHT_PANEL_WIDTH, 320));

        JScrollPane sidebarScrollPane = new JScrollPane(sidebarPanel);
        sidebarScrollPane.setBorder(BorderFactory.createEmptyBorder());
        sidebarScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        sidebarScrollPane.getViewport().setBackground(UiStyles.SURFACE);

        drawerShell.add(drawerHeader, BorderLayout.NORTH);
        drawerShell.add(sidebarScrollPane, BorderLayout.CENTER);

        workbenchSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapRegion, drawerShell);
        workbenchSplitPane.setResizeWeight(1.0);
        workbenchSplitPane.setContinuousLayout(true);
        UiStyles.applySplitPaneStyle(workbenchSplitPane, 8);
        workbenchSplitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                applyResponsiveDividerLocation();
            }
        });
        workbenchSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
            if (dividerAdjustedBySystem) {
                return;
            }
            int rightWidth = currentRightPanelWidth();
            if (rightWidth > 0) {
                preferredRightPanelWidth = rightWidth;
            }
        });
        SwingUtilities.invokeLater(this::applyResponsiveDividerLocation);

        statusLabel = new JLabel("系统状态  ·  工作台已就绪");
        statusLabel.setFont(UiStyles.CAPTION_FONT);
        statusLabel.setForeground(UiStyles.TEXT_SECONDARY);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(UiStyles.SURFACE_ALT);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));

        add(routeHeader, BorderLayout.NORTH);
        add(workbenchSplitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void applyResponsiveDividerLocation() {
        int totalWidth = workbenchSplitPane.getWidth();
        int dividerSize = workbenchSplitPane.getDividerSize();
        int availableWidth = totalWidth - dividerSize;
        if (availableWidth <= 0) {
            return;
        }

        int compactRightWidth = Math.min(MIN_RIGHT_PANEL_WIDTH, Math.max(280, availableWidth / 3));
        int maxRightWidth = Math.max(compactRightWidth, Math.min(MAX_RIGHT_PANEL_WIDTH, availableWidth - MIN_MAP_PANEL_WIDTH));
        int targetRightWidth = preferredRightPanelWidth == null
                ? (int) Math.round(availableWidth * DEFAULT_RIGHT_PANEL_RATIO)
                : preferredRightPanelWidth;
        targetRightWidth = Math.max(compactRightWidth, Math.min(maxRightWidth, targetRightWidth));

        int dividerLocation = Math.max(0, availableWidth - targetRightWidth);
        dividerAdjustedBySystem = true;
        workbenchSplitPane.setDividerLocation(dividerLocation);
        dividerAdjustedBySystem = false;
    }

    private int currentRightPanelWidth() {
        int totalWidth = workbenchSplitPane.getWidth();
        if (totalWidth <= 0) {
            return -1;
        }
        return totalWidth - workbenchSplitPane.getDividerLocation() - workbenchSplitPane.getDividerSize();
    }

    public void registerRoutePanel(AppRoute route, JPanel panel) {
        panel.setOpaque(false);
        sidebarPanel.add(panel, route.name());
    }

    public void showRoute(AppRoute route) {
        routeTitleLabel.setText(route.getTitle());
        routeSubtitleLabel.setText(resolveRouteSubtitle(route));
        drawerTitleLabel.setText(resolveDrawerTitle(route));
        drawerSubtitleLabel.setText(resolveDrawerSubtitle(route));
        sidebarLayout.show(sidebarPanel, route.name());
    }

    public void setStatus(String text) {
        statusLabel.setText("系统状态  ·  " + text);
    }

    public void setMapHint(String text) {
        mapHintLabel.setText("地图提示  ·  " + text);
    }

    public void setMapContent(Component component) {
        mapContentHolder.removeAll();
        mapContentHolder.add(component, BorderLayout.CENTER);
        mapSurfacePanel.revalidate();
        mapSurfacePanel.repaint();
    }

    private String resolveRouteSubtitle(AppRoute route) {
        if (route == AppRoute.USER_MODE) {
            return "面向访客与学生的路径查询、路线联动和地点浏览";
        }
        if (route == AppRoute.ADMIN_MODE) {
            return "面向管理员的地图编辑、图层控制和数据维护工作流";
        }
        return "系统环境、数据目录与维护入口说明";
    }

    private String resolveDrawerSubtitle(AppRoute route) {
        if (route == AppRoute.USER_MODE) {
            return "路径查询优先，结果、步骤和地点浏览按任务顺序展开";
        }
        if (route == AppRoute.ADMIN_MODE) {
            return "登录、编辑工具和属性面板统一收纳在右侧任务抽屉";
        }
        return "系统信息与维护入口统一展示在右侧抽屉";
    }

    private String resolveDrawerTitle(AppRoute route) {
        if (route == AppRoute.ADMIN_MODE) {
            return "管理抽屉";
        }
        if (route == AppRoute.SYSTEM_SETTINGS) {
            return "系统抽屉";
        }
        return "任务抽屉";
    }
}
