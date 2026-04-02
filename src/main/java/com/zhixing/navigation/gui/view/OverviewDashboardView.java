package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.gui.model.OverviewData;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class OverviewDashboardView extends JPanel {
    private final JLabel vertexCountLabel;
    private final JLabel roadCountLabel;
    private final JLabel forbiddenCountLabel;
    private final JLabel dataDirLabel;

    public OverviewDashboardView() {
        setLayout(new GridBagLayout());
        setBackground(UiStyles.PANEL_BACKGROUND);
        setBorder(UiStyles.sectionBorder("地图概览"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        vertexCountLabel = new JLabel();
        vertexCountLabel.setFont(UiStyles.SUBTITLE_FONT);
        roadCountLabel = new JLabel();
        roadCountLabel.setFont(UiStyles.SUBTITLE_FONT);
        forbiddenCountLabel = new JLabel();
        forbiddenCountLabel.setFont(UiStyles.SUBTITLE_FONT);
        dataDirLabel = new JLabel();
        dataDirLabel.setFont(UiStyles.BODY_FONT);

        ViewUtils.addFormRow(this, gbc, 0, "地点数量", vertexCountLabel);
        ViewUtils.addFormRow(this, gbc, 1, "道路数量", roadCountLabel);
        ViewUtils.addFormRow(this, gbc, 2, "禁行道路数量", forbiddenCountLabel);
        ViewUtils.addFormRow(this, gbc, 3, "数据目录", dataDirLabel);

        JButton refreshButton = UiStyles.primaryButton("刷新统计");
        refreshButton.addActionListener(e -> {
            if (listener != null) {
                listener.onRefresh();
            }
        });
        gbc.gridx = 1;
        gbc.gridy = 4;
        add(refreshButton, gbc);
    }

    public void setOverviewData(OverviewData overviewData) {
        vertexCountLabel.setText(String.valueOf(overviewData.getVertexCount()));
        roadCountLabel.setText(String.valueOf(overviewData.getRoadCount()));
        forbiddenCountLabel.setText(String.valueOf(overviewData.getForbiddenRoadCount()));
        dataDirLabel.setText(overviewData.getDataDir());
    }

    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public interface Listener {
        void onRefresh();
    }
}
