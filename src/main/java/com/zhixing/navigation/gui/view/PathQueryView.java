package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.gui.model.VertexOption;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class PathQueryView extends JPanel {
    private static final int HEADER_GAP = 12;
    private static final int FIELD_CARD_GAP = 6;
    private static final int FIELD_CARD_PADDING_Y = 10;
    private static final int FIELD_CARD_PADDING_X = 12;
    private static final int FIELD_STACK_GAP = 8;

    public enum MapPickTarget {
        NONE,
        START,
        END
    }

    private static final String DEFAULT_IDLE_STATUS = "推荐先在地图上依次点击起点和终点，也可以直接使用下拉框完成选择。";
    private static final String DEFAULT_START_PICK_STATUS = "地图选点已开启：请在地图中点击起点。";
    private static final String DEFAULT_END_PICK_STATUS = "地图选点已开启：请在地图中点击终点。";

    private final JComboBox<VertexOption> startCombo;
    private final JComboBox<VertexOption> endCombo;
    private final JButton pickStartButton;
    private final JButton pickEndButton;
    private final JTextArea mapPickStatusArea;
    private final PathDetailPanel detailPanel;

    private MapPickTarget mapPickTarget;
    private String mapStatusOverrideText;
    private Listener listener;

    public PathQueryView() {
        setLayout(new GridBagLayout());
        setOpaque(false);
        setBackground(UiStyles.PAGE_BACKGROUND);

        JPanel queryCard = UiStyles.cardPanel(new BorderLayout(0, HEADER_GAP));
        queryCard.setBackground(UiStyles.SURFACE);

        queryCard.add(createHeader(), BorderLayout.NORTH);

        JPanel formStack = new JPanel(new GridBagLayout());
        formStack.setOpaque(false);

        startCombo = createStretchComboBox();
        endCombo = createStretchComboBox();
        pickStartButton = createActionButton(UiStyles.secondaryButton("地图选起点"), 40);
        pickEndButton = createActionButton(UiStyles.secondaryButton("地图选终点"), 40);
        JButton queryButton = createActionButton(UiStyles.primaryButton("查询路径"), 44);
        JButton swapButton = createActionButton(UiStyles.ghostButton("交换起终点"), 40);

        mapPickStatusArea = createStatusArea();

        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.gridx = 0;
        formGbc.weightx = 1;
        formGbc.anchor = GridBagConstraints.NORTHWEST;
        formGbc.fill = GridBagConstraints.HORIZONTAL;

        formGbc.gridy = 0;
        formGbc.insets = new Insets(0, 0, FIELD_STACK_GAP, 0);
        formStack.add(createFieldCard("起点", "从列表直接选择，或激活地图选点后点击地图。", startCombo), formGbc);

        formGbc.gridy = 1;
        formGbc.insets = new Insets(0, 0, FIELD_STACK_GAP, 0);
        formStack.add(createFieldCard("终点", "与起点保持同样的选择方式，避免在右侧来回找入口。", endCombo), formGbc);

        formGbc.gridy = 2;
        formGbc.insets = new Insets(0, 0, FIELD_STACK_GAP, 0);
        formStack.add(pickStartButton, formGbc);

        formGbc.gridy = 3;
        formGbc.insets = new Insets(0, 0, FIELD_STACK_GAP, 0);
        formStack.add(pickEndButton, formGbc);

        formGbc.gridy = 4;
        formGbc.insets = new Insets(10, 0, 8, 0);
        formStack.add(queryButton, formGbc);

        formGbc.gridy = 5;
        formGbc.insets = new Insets(0, 0, 12, 0);
        formStack.add(swapButton, formGbc);

        formGbc.gridy = 6;
        formGbc.insets = new Insets(0, 0, 0, 0);
        formStack.add(mapPickStatusArea, formGbc);

        queryCard.add(formStack, BorderLayout.CENTER);

        detailPanel = new PathDetailPanel();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 12, 0);
        add(queryCard, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(detailPanel, gbc);

        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        add(filler, gbc);

        mapPickTarget = MapPickTarget.NONE;
        refreshMapPickUi();

        pickStartButton.addActionListener(e -> toggleMapPickTarget(MapPickTarget.START));
        pickEndButton.addActionListener(e -> toggleMapPickTarget(MapPickTarget.END));
        queryButton.addActionListener(e -> {
            if (listener != null) {
                listener.onQuery(selectedStartId(), selectedEndId());
            }
        });
        swapButton.addActionListener(e -> swapSelection());
        detailPanel.setInstructionListener(index -> {
            if (listener != null) {
                listener.onInstructionSelected(index);
            }
        });
    }

    public void setVertexOptions(List<VertexOption> options, String selectedStartId, String selectedEndId) {
        refillCombo(startCombo, options, selectedStartId);
        refillCombo(endCombo, options, selectedEndId);
    }

    public void setResultContent(String content) {
        detailPanel.setContent(content);
    }

    public void setInstructions(List<String> instructions) {
        detailPanel.setInstructions(instructions);
    }

    public String selectedStartId() {
        VertexOption selected = (VertexOption) startCombo.getSelectedItem();
        return selected == null ? null : selected.getId();
    }

    public String selectedEndId() {
        VertexOption selected = (VertexOption) endCombo.getSelectedItem();
        return selected == null ? null : selected.getId();
    }

    public void pickVertex(String vertexId) {
        if (vertexId == null || vertexId.trim().isEmpty()) {
            return;
        }
        if (mapPickTarget == MapPickTarget.START) {
            setSelectedStartId(vertexId);
            setMapPickTargetInternal(MapPickTarget.END, true);
            setMapSelectionStatus("已选择起点：" + selectedStartId() + "，请继续点击地图选择终点。");
            return;
        }
        if (mapPickTarget == MapPickTarget.END) {
            setSelectedEndId(vertexId);
            setMapPickTargetInternal(MapPickTarget.NONE, true);
            setMapSelectionStatus("已选择终点：" + selectedEndId() + "，可以直接点击“查询路径”。");
        }
    }

    public MapPickTarget mapPickTarget() {
        return mapPickTarget;
    }

    public void setMapPickTarget(MapPickTarget target) {
        setMapPickTargetInternal(target == null ? MapPickTarget.NONE : target, false);
    }

    public void setMapSelectionStatus(String text) {
        mapStatusOverrideText = normalizeText(text);
        refreshMapPickUi();
    }

    public void setSelectedStartId(String vertexId) {
        selectById(startCombo, vertexId);
    }

    public void setSelectedEndId(String vertexId) {
        selectById(endCombo, vertexId);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("路径查询");
        titleLabel.setFont(UiStyles.SUBTITLE_FONT);
        titleLabel.setForeground(UiStyles.TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        JTextArea descriptionArea = createSupportingText(
                "从这里完成完整查询流程：先定起点，再定终点，最后查看路线摘要和分步导航。"
        );
        descriptionArea.setAlignmentX(LEFT_ALIGNMENT);

        titleStack.add(titleLabel);
        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(descriptionArea);

        JPanel stepStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        stepStrip.setOpaque(false);
        stepStrip.add(createStepBadge("1 选起点"));
        stepStrip.add(createStepBadge("2 选终点"));
        stepStrip.add(createStepBadge("3 查询路线"));

        header.add(titleStack, BorderLayout.NORTH);
        header.add(stepStrip, BorderLayout.CENTER);
        return header;
    }

    private static JPanel createFieldCard(String labelText, String helperText, JComponent field) {
        JPanel group = UiStyles.softCardPanel(new BorderLayout(0, FIELD_CARD_GAP));
        group.setAlignmentX(LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        group.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.SURFACE_SUBTLE),
                BorderFactory.createEmptyBorder(
                        FIELD_CARD_PADDING_Y,
                        FIELD_CARD_PADDING_X,
                        FIELD_CARD_PADDING_Y,
                        FIELD_CARD_PADDING_X
                )
        ));

        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));

        JLabel label = UiStyles.formLabel(labelText);
        label.setAlignmentX(LEFT_ALIGNMENT);

        JTextArea helper = createSupportingText(helperText);
        helper.setAlignmentX(LEFT_ALIGNMENT);

        titleStack.add(label);
        titleStack.add(Box.createVerticalStrut(2));
        titleStack.add(helper);

        group.add(titleStack, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        return group;
    }

    private static JButton createActionButton(JButton button, int height) {
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setPreferredSize(new Dimension(0, height));
        button.setMinimumSize(new Dimension(0, height));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return button;
    }

    private static <T> JComboBox<T> createStretchComboBox() {
        JComboBox<T> comboBox = UiStyles.formComboBox();
        Dimension preferred = comboBox.getPreferredSize();
        int height = Math.max(40, preferred.height);
        comboBox.setAlignmentX(LEFT_ALIGNMENT);
        comboBox.setPreferredSize(new Dimension(0, height));
        comboBox.setMinimumSize(new Dimension(0, height));
        comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return comboBox;
    }

    private static JTextArea createStatusArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFocusable(false);
        textArea.setFont(UiStyles.CAPTION_FONT);
        textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        textArea.setBackground(UiStyles.INFO_SOFT);
        textArea.setForeground(UiStyles.TEXT_SECONDARY);
        textArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return textArea;
    }

    private static JTextArea createSupportingText(String text) {
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setOpaque(false);
        textArea.setFocusable(false);
        textArea.setFont(UiStyles.CAPTION_FONT);
        textArea.setForeground(UiStyles.TEXT_SECONDARY);
        textArea.setBorder(BorderFactory.createEmptyBorder());
        textArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return textArea;
    }

    private static JLabel createStepBadge(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiStyles.CAPTION_FONT);
        label.setForeground(UiStyles.TEXT_SECONDARY);
        label.setOpaque(true);
        label.setBackground(UiStyles.SURFACE);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return label;
    }

    private static void refillCombo(JComboBox<VertexOption> combo, List<VertexOption> options, String selectedId) {
        combo.removeAllItems();
        for (VertexOption option : options) {
            combo.addItem(option);
        }
        if (selectedId != null) {
            ViewUtils.selectComboByMatcher(combo, value -> value != null && selectedId.equals(value.getId()));
        } else if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    private static void selectById(JComboBox<VertexOption> combo, String vertexId) {
        if (vertexId == null || vertexId.trim().isEmpty()) {
            return;
        }
        ViewUtils.selectComboByMatcher(combo, value -> value != null && vertexId.equals(value.getId()));
    }

    private void toggleMapPickTarget(MapPickTarget target) {
        setMapPickTargetInternal(mapPickTarget == target ? MapPickTarget.NONE : target, true);
    }

    private void swapSelection() {
        Object start = startCombo.getSelectedItem();
        Object end = endCombo.getSelectedItem();
        startCombo.setSelectedItem(end);
        endCombo.setSelectedItem(start);
    }

    private void setMapPickTargetInternal(MapPickTarget target, boolean notify) {
        mapPickTarget = target;
        mapStatusOverrideText = null;
        refreshMapPickUi();
        if (notify && listener != null) {
            listener.onMapPickTargetChanged(mapPickTarget);
        }
    }

    private void refreshMapPickUi() {
        stylePickButton(pickStartButton, mapPickTarget == MapPickTarget.START, "地图选起点", "正在选起点");
        stylePickButton(pickEndButton, mapPickTarget == MapPickTarget.END, "地图选终点", "正在选终点");

        if (mapPickTarget == MapPickTarget.START) {
            applyStatusStyle(UiStyles.PRIMARY_SOFT, UiStyles.PRIMARY, normalizeText(mapStatusOverrideText, DEFAULT_START_PICK_STATUS));
            return;
        }
        if (mapPickTarget == MapPickTarget.END) {
            applyStatusStyle(UiStyles.PRIMARY_SOFT, UiStyles.PRIMARY, normalizeText(mapStatusOverrideText, DEFAULT_END_PICK_STATUS));
            return;
        }
        applyStatusStyle(UiStyles.INFO_SOFT, UiStyles.TEXT_SECONDARY, normalizeText(mapStatusOverrideText, DEFAULT_IDLE_STATUS));
    }

    private void applyStatusStyle(java.awt.Color background, java.awt.Color foreground, String text) {
        mapPickStatusArea.setBackground(background);
        mapPickStatusArea.setForeground(foreground);
        mapPickStatusArea.setText(text);
    }

    private static void stylePickButton(JButton button, boolean active, String idleText, String activeText) {
        button.setText(active ? activeText : idleText);
        if (active) {
            UiStyles.applyPrimaryButtonStyle(button);
        } else {
            UiStyles.applySecondaryButtonStyle(button);
        }
    }

    private static String normalizeText(String text) {
        return normalizeText(text, null);
    }

    private static String normalizeText(String text, String fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        return text.trim();
    }

    public interface Listener {
        void onQuery(String startId, String endId);

        void onMapPickTargetChanged(MapPickTarget target);

        void onInstructionSelected(int index);
    }
}
