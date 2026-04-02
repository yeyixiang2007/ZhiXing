package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.gui.routing.AppRoute;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;

public class MapWorkbenchView extends JPanel {
    private final JLabel routeTitleLabel;
    private final JPanel mapSurfacePanel;
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
        mapSurfacePanel.setBackground(new Color(235, 241, 252));
        mapHintLabel = new JLabel("地图工作区已就绪，等待 MapCanvas 接入...", SwingConstants.CENTER);
        mapHintLabel.setFont(UiStyles.SUBTITLE_FONT);
        mapHintLabel.setForeground(UiStyles.TEXT_SECONDARY);
        mapSurfacePanel.add(mapHintLabel, BorderLayout.CENTER);

        JPanel mapRegion = new JPanel(new BorderLayout());
        mapRegion.setBackground(UiStyles.PANEL_BACKGROUND);
        mapRegion.setBorder(UiStyles.sectionBorder("地图区"));
        mapRegion.add(mapSurfacePanel, BorderLayout.CENTER);

        sidebarLayout = new CardLayout();
        sidebarPanel = new JPanel(sidebarLayout);
        sidebarPanel.setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel sideRegion = new JPanel(new BorderLayout());
        sideRegion.setBackground(UiStyles.PAGE_BACKGROUND);
        sideRegion.setPreferredSize(new java.awt.Dimension(470, 0));
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
        mapSurfacePanel.removeAll();
        mapSurfacePanel.add(component, BorderLayout.CENTER);
        mapSurfacePanel.revalidate();
        mapSurfacePanel.repaint();
    }

    public void resetMapHint() {
        mapSurfacePanel.removeAll();
        mapSurfacePanel.add(mapHintLabel, BorderLayout.CENTER);
        mapSurfacePanel.revalidate();
        mapSurfacePanel.repaint();
    }
}
