package com.zhixing.navigation.gui.components;

import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class LoadingOverlay extends JPanel {
    private final JLabel messageLabel;

    public LoadingOverlay() {
        setOpaque(false);
        setLayout(new GridBagLayout());

        JPanel content = new JPanel();
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER),
                BorderFactory.createEmptyBorder(16, 22, 16, 22)
        ));
        content.setLayout(new GridBagLayout());

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        progressBar.setForeground(UiStyles.PRIMARY);

        messageLabel = new JLabel("正在处理，请稍候...");
        messageLabel.setFont(UiStyles.BODY_FONT);
        messageLabel.setForeground(UiStyles.TEXT_PRIMARY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        content.add(progressBar, gbc);
        gbc.gridy = 1;
        gbc.insets.top = 8;
        content.add(messageLabel, gbc);
        add(content);
        setVisible(false);
    }

    public void showLoading(String message) {
        messageLabel.setText(message);
        setVisible(true);
    }

    public void hideLoading() {
        setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        graphics.setColor(new Color(17, 24, 39, 120));
        graphics.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(graphics);
    }
}
