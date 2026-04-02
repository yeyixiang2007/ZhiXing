package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

public class PathDetailPanel extends JPanel {
    private final JTextArea textArea;
    private final DefaultListModel<String> instructionModel;
    private final JList<String> instructionList;

    public PathDetailPanel() {
        setLayout(new BorderLayout(8, 8));
        setBackground(UiStyles.PAGE_BACKGROUND);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(UiStyles.BODY_FONT);
        textArea.setText("请先选择起点与终点，然后点击“查询路径”。");
        textArea.setBackground(Color.WHITE);
        JScrollPane summaryPane = new JScrollPane(textArea);
        summaryPane.setBorder(UiStyles.sectionBorder("路径详情"));

        instructionModel = new DefaultListModel<String>();
        instructionList = new JList<String>(instructionModel);
        instructionList.setFont(UiStyles.BODY_FONT);
        instructionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instructionList.setVisibleRowCount(7);
        instructionList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            if (instructionListener != null && instructionList.getSelectedIndex() >= 0) {
                instructionListener.onInstructionSelected(instructionList.getSelectedIndex());
            }
        });

        JScrollPane instructionPane = new JScrollPane(instructionList);
        instructionPane.setBorder(UiStyles.sectionBorder("分步导航（点击可联动地图）"));

        add(summaryPane, BorderLayout.CENTER);
        add(instructionPane, BorderLayout.SOUTH);
    }

    public void setContent(String content) {
        textArea.setText(content);
    }

    public void setInstructions(List<String> instructions) {
        instructionModel.clear();
        if (instructions == null || instructions.isEmpty()) {
            instructionModel.addElement("暂无导航步骤");
            instructionList.clearSelection();
            return;
        }
        for (int i = 0; i < instructions.size(); i++) {
            instructionModel.addElement((i + 1) + ". " + instructions.get(i));
        }
        instructionList.setSelectedIndex(0);
    }

    public void clearInstructions() {
        instructionModel.clear();
        instructionList.clearSelection();
    }

    private InstructionListener instructionListener;

    public void setInstructionListener(InstructionListener instructionListener) {
        this.instructionListener = instructionListener;
    }

    public interface InstructionListener {
        void onInstructionSelected(int index);
    }
}
