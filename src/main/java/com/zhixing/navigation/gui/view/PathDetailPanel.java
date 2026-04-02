package com.zhixing.navigation.gui.view;

import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

public class PathDetailPanel extends JPanel {
    private final JTextArea textArea;

    public PathDetailPanel() {
        setLayout(new BorderLayout());
        setBackground(UiStyles.PAGE_BACKGROUND);
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(UiStyles.BODY_FONT);
        textArea.setText("请先选择起点与终点，然后点击“查询路径”。");

        JScrollPane resultPane = new JScrollPane(textArea);
        resultPane.setBorder(UiStyles.sectionBorder("路径详情与分步导航"));
        add(resultPane, BorderLayout.CENTER);
    }

    public void setContent(String content) {
        textArea.setText(content);
    }
}
