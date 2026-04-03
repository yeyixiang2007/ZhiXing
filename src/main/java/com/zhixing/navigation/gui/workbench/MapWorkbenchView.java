package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.gui.routing.AppRoute;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class MapWorkbenchView extends JPanel {
    private static final int RIGHT_PANEL_TARGET_WIDTH = 420;
    private static final int RIGHT_PANEL_MIN_WIDTH = 340;

    private final JLabel routeTitleLabel;
    private final JPanel mapSurfacePanel;
    private final JPanel mapContentHolder;
    private final JLabel mapHintLabel;
    private final CardLayout sidebarLayout;
    private final JPanel sidebarPanel;
    private final JLabel statusLabel;
    private final JSplitPane workbenchSplitPane;

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
        sideRegion.setPreferredSize(new Dimension(430, 0));
        sideRegion.add(sidebarPanel, BorderLayout.CENTER);

        workbenchSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapRegion, sideRegion);
        workbenchSplitPane.setResizeWeight(1.0);
        workbenchSplitPane.setContinuousLayout(true);
        workbenchSplitPane.setBorder(BorderFactory.createEmptyBorder());
        workbenchSplitPane.setDividerSize(8);
        workbenchSplitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustWorkbenchDivider();
            }
        });
        SwingUtilities.invokeLater(this::adjustWorkbenchDivider);

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
        add(workbenchSplitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void adjustWorkbenchDivider() {
        int totalWidth = workbenchSplitPane.getWidth();
        if (totalWidth <= 0) {
            return;
        }
        int rightWidth = Math.max(RIGHT_PANEL_MIN_WIDTH, Math.min(RIGHT_PANEL_TARGET_WIDTH, totalWidth / 3));
        int dividerLocation = totalWidth - rightWidth - workbenchSplitPane.getDividerSize();
        workbenchSplitPane.setDividerLocation(Math.max(220, dividerLocation));
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
