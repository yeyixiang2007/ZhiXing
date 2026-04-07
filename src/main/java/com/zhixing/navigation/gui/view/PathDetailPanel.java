package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

public class PathDetailPanel extends JPanel {
    private static final String DEFAULT_GUIDE = "请先选择起点与终点，然后点击“查询路径”。";
    private static final String DEFAULT_DETAIL = "查询后会在这里展示分段距离、途经点和详细路线说明。";

    private final JLabel statusBadgeLabel;
    private final JLabel routeHeadlineLabel;
    private final JTextArea routePathTextArea;
    private final JLabel distanceValueLabel;
    private final JLabel timeValueLabel;
    private final JLabel waypointValueLabel;
    private final JLabel stepValueLabel;
    private final JTextArea detailTextArea;
    private final DefaultListModel<String> instructionModel;
    private final JList<String> instructionList;

    private RouteSummaryData currentSummary;
    private boolean hasInstructions;
    private InstructionListener instructionListener;

    public PathDetailPanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel summaryCard = UiStyles.cardPanel(new BorderLayout(0, 12));
        summaryCard.setBackground(UiStyles.SURFACE);

        JPanel summaryHeader = new JPanel(new BorderLayout(12, 0));
        summaryHeader.setOpaque(false);

        JPanel summaryTitleGroup = new JPanel();
        summaryTitleGroup.setOpaque(false);
        summaryTitleGroup.setLayout(new BoxLayout(summaryTitleGroup, BoxLayout.Y_AXIS));

        JLabel summaryTitleLabel = new JLabel("路线摘要");
        summaryTitleLabel.setFont(UiStyles.SUBTITLE_FONT);
        summaryTitleLabel.setForeground(UiStyles.TEXT_PRIMARY);
        summaryTitleLabel.setAlignmentX(LEFT_ALIGNMENT);

        routeHeadlineLabel = new JLabel(DEFAULT_GUIDE);
        routeHeadlineLabel.setFont(UiStyles.BODY_FONT);
        routeHeadlineLabel.setForeground(UiStyles.TEXT_SECONDARY);
        routeHeadlineLabel.setAlignmentX(LEFT_ALIGNMENT);

        summaryTitleGroup.add(summaryTitleLabel);
        summaryTitleGroup.add(Box.createVerticalStrut(4));
        summaryTitleGroup.add(routeHeadlineLabel);

        statusBadgeLabel = new JLabel();
        statusBadgeLabel.setFont(UiStyles.CAPTION_FONT);

        summaryHeader.add(summaryTitleGroup, BorderLayout.CENTER);
        summaryHeader.add(statusBadgeLabel, BorderLayout.EAST);

        JPanel routeCard = UiStyles.softCardPanel(new BorderLayout(0, 6));
        routeCard.setBackground(UiStyles.SURFACE_ALT);

        JLabel routeLabel = UiStyles.captionLabel("路线概览");
        routePathTextArea = createReadOnlyTextArea(UiStyles.CAPTION_FONT);
        routeCard.add(routeLabel, BorderLayout.NORTH);
        routeCard.add(routePathTextArea, BorderLayout.CENTER);

        JPanel metricsPanel = new JPanel(new GridLayout(0, 2, 8, 8));
        metricsPanel.setOpaque(false);
        distanceValueLabel = createMetricCard(metricsPanel, "总距离");
        timeValueLabel = createMetricCard(metricsPanel, "预计时间");
        waypointValueLabel = createMetricCard(metricsPanel, "途经点数");
        stepValueLabel = createMetricCard(metricsPanel, "导航步骤");

        summaryCard.add(summaryHeader, BorderLayout.NORTH);
        summaryCard.add(routeCard, BorderLayout.CENTER);
        summaryCard.add(metricsPanel, BorderLayout.SOUTH);

        JPanel detailCard = UiStyles.cardPanel(new BorderLayout(0, 10));
        detailCard.add(createSectionHeader("路线详情", "展示途经点与分段距离说明。"), BorderLayout.NORTH);

        detailTextArea = createReadOnlyTextArea(UiStyles.BODY_FONT);
        detailTextArea.setRows(6);
        detailTextArea.setBackground(UiStyles.SURFACE);
        JScrollPane detailScrollPane = new JScrollPane(detailTextArea);
        detailScrollPane.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));
        detailCard.add(detailScrollPane, BorderLayout.CENTER);

        JPanel stepsCard = UiStyles.cardPanel(new BorderLayout(0, 10));
        stepsCard.add(createSectionHeader("分步导航", "点击步骤可联动地图高亮对应路段。"), BorderLayout.NORTH);

        instructionModel = new DefaultListModel<String>();
        instructionList = new JList<String>(instructionModel);
        instructionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instructionList.setFont(UiStyles.BODY_FONT);
        instructionList.setCellRenderer(new InstructionCellRenderer());
        instructionList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            if (instructionListener != null && hasInstructions && instructionList.getSelectedIndex() >= 0) {
                instructionListener.onInstructionSelected(instructionList.getSelectedIndex());
            }
        });

        JScrollPane instructionScrollPane = new JScrollPane(instructionList);
        instructionScrollPane.setBorder(BorderFactory.createLineBorder(UiStyles.BORDER));
        instructionScrollPane.setPreferredSize(new Dimension(0, 188));
        stepsCard.add(instructionScrollPane, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 12, 0);
        add(summaryCard, gbc);

        gbc.gridy = 1;
        add(detailCard, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(12, 0, 0, 0);
        add(stepsCard, gbc);

        gbc.gridy = 3;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        add(filler, gbc);

        currentSummary = RouteSummaryData.empty();
        applySummary(currentSummary);
        setInstructions(new ArrayList<String>());
    }

    public void setContent(String content) {
        currentSummary = RouteSummaryData.fromContent(content);
        applySummary(currentSummary);
    }

    public void setInstructions(List<String> instructions) {
        instructionModel.clear();
        hasInstructions = instructions != null && !instructions.isEmpty();
        if (!hasInstructions) {
            instructionModel.addElement("查询后会在这里展示逐步导航。");
            instructionList.clearSelection();
            updateStepMetric(currentSummary.stepCountText());
            return;
        }

        for (int i = 0; i < instructions.size(); i++) {
            instructionModel.addElement((i + 1) + ". " + instructions.get(i));
        }
        updateStepMetric(String.valueOf(instructions.size()));
        instructionList.setSelectedIndex(0);
    }

    public void clearInstructions() {
        setInstructions(new ArrayList<String>());
    }

    public void setInstructionListener(InstructionListener instructionListener) {
        this.instructionListener = instructionListener;
    }

    private void applySummary(RouteSummaryData summary) {
        routeHeadlineLabel.setText(summary.headlineText());
        routePathTextArea.setText(summary.routeText());
        distanceValueLabel.setText(summary.distanceText());
        timeValueLabel.setText(summary.timeText());
        waypointValueLabel.setText(summary.waypointCountText());
        updateStepMetric(summary.stepCountText());
        detailTextArea.setText(summary.detailText());
        applyStatusBadge(summary.ready);
    }

    private void applyStatusBadge(boolean ready) {
        statusBadgeLabel.setOpaque(true);
        if (ready) {
            statusBadgeLabel.setText("路线已生成");
            statusBadgeLabel.setForeground(UiStyles.SUCCESS);
            statusBadgeLabel.setBackground(UiStyles.SUCCESS_SOFT);
            statusBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiStyles.SUCCESS),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
            return;
        }
        statusBadgeLabel.setText("等待查询");
        statusBadgeLabel.setForeground(UiStyles.TEXT_SECONDARY);
        statusBadgeLabel.setBackground(UiStyles.SURFACE);
        statusBadgeLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    private void updateStepMetric(String value) {
        stepValueLabel.setText(value);
    }

    private static JComponent createSectionHeader(String title, String description) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiStyles.SUBTITLE_FONT);
        titleLabel.setForeground(UiStyles.TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel descLabel = UiStyles.captionLabel(description);
        descLabel.setAlignmentX(LEFT_ALIGNMENT);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(3));
        header.add(descLabel);
        return header;
    }

    private static JLabel createMetricCard(JPanel parent, String labelText) {
        JPanel card = UiStyles.softCardPanel(new BorderLayout(0, 4));
        card.setBackground(UiStyles.SURFACE_ALT);

        JLabel label = new JLabel(labelText);
        label.setFont(UiStyles.CAPTION_FONT);
        label.setForeground(UiStyles.TEXT_SECONDARY);

        JLabel value = new JLabel("--");
        value.setFont(UiStyles.METRIC_FONT);
        value.setForeground(UiStyles.TEXT_PRIMARY);

        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        parent.add(card);
        return value;
    }

    private static JTextArea createReadOnlyTextArea(java.awt.Font font) {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setOpaque(false);
        textArea.setBorder(BorderFactory.createEmptyBorder());
        textArea.setFont(font);
        textArea.setForeground(UiStyles.TEXT_PRIMARY);
        return textArea;
    }

    private final class InstructionCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            int wrapWidth = Math.max(140, list.getWidth() - 44);
            String text = value == null ? "" : value.toString();
            label.setText("<html><div style='width:" + wrapWidth + "px;'>" + text + "</div></html>");
            label.setFont(UiStyles.BODY_FONT);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isSelected && hasInstructions ? UiStyles.BRAND_500 : UiStyles.BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            if (!hasInstructions) {
                label.setBackground(UiStyles.SURFACE);
                label.setForeground(UiStyles.TEXT_SECONDARY);
                return label;
            }
            if (isSelected) {
                label.setBackground(UiStyles.PRIMARY_SOFT);
                label.setForeground(UiStyles.PRIMARY);
            } else {
                label.setBackground(UiStyles.SURFACE);
                label.setForeground(UiStyles.TEXT_PRIMARY);
            }
            return label;
        }
    }

    private static final class RouteSummaryData {
        private final boolean ready;
        private final String startName;
        private final String endName;
        private final String routePath;
        private final String distance;
        private final String estimatedTime;
        private final int waypointCount;
        private final int stepCount;
        private final String detailText;

        private RouteSummaryData(
                boolean ready,
                String startName,
                String endName,
                String routePath,
                String distance,
                String estimatedTime,
                int waypointCount,
                int stepCount,
                String detailText
        ) {
            this.ready = ready;
            this.startName = startName;
            this.endName = endName;
            this.routePath = routePath;
            this.distance = distance;
            this.estimatedTime = estimatedTime;
            this.waypointCount = waypointCount;
            this.stepCount = stepCount;
            this.detailText = detailText;
        }

        private static RouteSummaryData empty() {
            return new RouteSummaryData(
                    false,
                    "",
                    "",
                    "支持地图点选或下拉选择起点与终点。",
                    "--",
                    "--",
                    0,
                    0,
                    DEFAULT_DETAIL
            );
        }

        private static RouteSummaryData fromContent(String content) {
            if (content == null || content.trim().isEmpty()) {
                return empty();
            }
            String[] lines = content.replace("\r\n", "\n").split("\n");
            String start = "";
            String end = "";
            String distance = "--";
            String time = "--";
            String route = "";
            List<String> details = new ArrayList<String>();
            List<String> instructions = new ArrayList<String>();
            boolean collectDetails = false;
            boolean collectInstructions = false;

            for (String line : lines) {
                String trimmed = line == null ? "" : line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("起点：")) {
                    start = trimmed.substring("起点：".length()).trim();
                    continue;
                }
                if (trimmed.startsWith("终点：")) {
                    end = trimmed.substring("终点：".length()).trim();
                    continue;
                }
                if (trimmed.startsWith("总距离：")) {
                    distance = trimmed.substring("总距离：".length()).trim();
                    continue;
                }
                if (trimmed.startsWith("预计步行耗时：")) {
                    time = trimmed.substring("预计步行耗时：".length()).trim();
                    continue;
                }
                if (trimmed.startsWith("途经点：")) {
                    route = trimmed.substring("途经点：".length()).trim();
                    continue;
                }
                if ("----- 分段距离 -----".equals(trimmed)) {
                    collectDetails = true;
                    collectInstructions = false;
                    continue;
                }
                if ("----- 分步导航 -----".equals(trimmed)) {
                    collectDetails = false;
                    collectInstructions = true;
                    continue;
                }
                if (trimmed.startsWith("==========") || trimmed.startsWith("==================================")) {
                    continue;
                }
                if (collectDetails) {
                    details.add(trimmed);
                } else if (collectInstructions) {
                    instructions.add(trimmed);
                }
            }

            boolean ready = !start.isEmpty() || !end.isEmpty();
            int waypointCount = route.isEmpty() ? 0 : route.split("\\s*->\\s*").length;
            String detailText = details.isEmpty() ? DEFAULT_DETAIL : joinLines(details);
            String routeText = route.isEmpty()
                    ? (ready ? start + " → " + end : "支持地图点选或下拉选择起点与终点。")
                    : route;
            return new RouteSummaryData(
                    ready,
                    start,
                    end,
                    routeText,
                    distance,
                    time,
                    waypointCount,
                    instructions.size(),
                    detailText
            );
        }

        private static String joinLines(List<String> lines) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) {
                    builder.append(System.lineSeparator());
                }
                builder.append(lines.get(i));
            }
            return builder.toString();
        }

        private String headlineText() {
            if (!ready) {
                return DEFAULT_GUIDE;
            }
            return startName + "  →  " + endName;
        }

        private String routeText() {
            return routePath;
        }

        private String distanceText() {
            return distance;
        }

        private String timeText() {
            return estimatedTime;
        }

        private String waypointCountText() {
            return waypointCount <= 0 ? "--" : String.valueOf(waypointCount);
        }

        private String stepCountText() {
            return stepCount <= 0 ? "--" : String.valueOf(stepCount);
        }

        private String detailText() {
            return detailText;
        }
    }

    public interface InstructionListener {
        void onInstructionSelected(int index);
    }
}
