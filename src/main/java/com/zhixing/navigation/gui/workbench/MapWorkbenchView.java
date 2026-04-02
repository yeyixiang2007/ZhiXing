package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.gui.routing.AppRoute;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

public class MapWorkbenchView extends JPanel {
    private final JLabel routeTitleLabel;
    private final JPanel mapSurfacePanel;
    private final JPanel mapContentHolder;
    private final JLabel mapHintLabel;
    private final CardLayout sidebarLayout;
    private final JPanel sidebarPanel;
    private final JLabel statusLabel;

    public MapWorkbenchView() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiStyles.PAGE_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));

        routeTitleLabel = new JLabel();
        routeTitleLabel.setFont(UiStyles.SUBTITLE_FONT);
        routeTitleLabel.setForeground(UiStyles.TEXT_PRIMARY);
        routeTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

        mapSurfacePanel = new JPanel(new BorderLayout());
        mapSurfacePanel.setBackground(UiStyles.PANEL_BACKGROUND);

        mapContentHolder = new JPanel(new BorderLayout());
        mapContentHolder.setBackground(new Color(235, 241, 252));
        mapSurfacePanel.add(mapContentHolder, BorderLayout.CENTER);

        mapHintLabel = new JLabel("地图工作区已就绪。左键框选/点选，右键拖拽平移，滚轮缩放。");
        mapHintLabel.setFont(UiStyles.CAPTION_FONT);
        mapHintLabel.setForeground(UiStyles.TEXT_SECONDARY);
        mapHintLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        mapHintLabel.setOpaque(true);
        mapHintLabel.setBackground(new Color(245, 248, 252));
        mapSurfacePanel.add(mapHintLabel, BorderLayout.SOUTH);

        JPanel mapRegion = new JPanel(new BorderLayout());
        mapRegion.setBackground(UiStyles.PANEL_BACKGROUND);
        mapRegion.setBorder(UiStyles.sectionBorder("地图区"));
        mapRegion.add(mapSurfacePanel, BorderLayout.CENTER);

        sidebarLayout = new CardLayout();
        sidebarPanel = new JPanel(sidebarLayout);
        sidebarPanel.setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel sideRegion = new JPanel(new BorderLayout());
        sideRegion.setBackground(UiStyles.PAGE_BACKGROUND);
        sideRegion.setPreferredSize(new Dimension(470, 0));
        sideRegion.add(sidebarPanel, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapRegion, sideRegion);
        splitPane.setResizeWeight(0.65);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(8);

        statusLabel = new JLabel("状态: GUI 已就绪");
        statusLabel.setFont(UiStyles.CAPTION_FONT);
        statusLabel.setForeground(UiStyles.TEXT_SECONDARY);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.WHITE);

        add(routeTitleLabel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void registerRoutePanel(AppRoute route, JPanel panel) {
        sidebarPanel.add(panel, route.name());
    }

    public void showRoute(AppRoute route) {
        routeTitleLabel.setText(route.getTitle());
        sidebarLayout.show(sidebarPanel, route.name());
    }

    public void setStatus(String text) {
        statusLabel.setText("状态: " + text);
    }

    public void setMapHint(String text) {
        mapHintLabel.setText(text);
    }

    public void setMapContent(Component component) {
        mapContentHolder.removeAll();
        mapContentHolder.add(component, BorderLayout.CENTER);
        mapSurfacePanel.revalidate();
        mapSurfacePanel.repaint();
    }

    public void resetMapHint() {
        mapContentHolder.removeAll();
        mapContentHolder.setBackground(new Color(235, 241, 252));
        mapSurfacePanel.revalidate();
        mapSurfacePanel.repaint();
    }
}
