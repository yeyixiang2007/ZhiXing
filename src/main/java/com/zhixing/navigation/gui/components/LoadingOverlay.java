package com.zhixing.navigation.gui.components;

import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.BasicStroke;

public class LoadingOverlay extends JPanel {
    public enum ToastType {
        SUCCESS,
        INFO,
        WARNING,
        ERROR
    }

    private final JLabel messageLabel;
    private final Timer toastTimer;
    private boolean loadingVisible;
    private boolean toastVisible;
    private String toastMessage;
    private ToastType toastType;

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
        this.loadingVisible = false;
        this.toastVisible = false;
        this.toastMessage = "";
        this.toastType = ToastType.INFO;
        this.toastTimer = new Timer(2200, e -> {
            toastVisible = false;
            updateVisibility();
            repaint();
        });
        this.toastTimer.setRepeats(false);
        setVisible(false);
    }

    public void showLoading(String message) {
        messageLabel.setText(message);
        loadingVisible = true;
        updateVisibility();
        repaint();
    }

    public void hideLoading() {
        loadingVisible = false;
        updateVisibility();
        repaint();
    }

    public void showToast(String message, ToastType type) {
        toastMessage = safe(message, "操作已完成。");
        toastType = type == null ? ToastType.INFO : type;
        toastVisible = true;
        updateVisibility();
        repaint();
        toastTimer.restart();
    }

    @Override
    public boolean contains(int x, int y) {
        // Allow interaction passthrough when only toast is visible.
        return loadingVisible;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (loadingVisible) {
                g2.setColor(new Color(17, 24, 39, 120));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            if (toastVisible) {
                drawToast(g2);
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(graphics);
    }

    private void drawToast(Graphics2D g2) {
        String text = safe(toastMessage, "");
        if (text.isEmpty()) {
            return;
        }
        g2.setFont(UiStyles.BODY_FONT);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int paddingX = 16;
        int paddingY = 10;
        int boxWidth = textWidth + paddingX * 2;
        int boxHeight = textHeight + paddingY * 2;
        int rightMargin = 22;
        int topMargin = 18;
        int x = Math.max(8, getWidth() - rightMargin - boxWidth);
        int y = topMargin;

        Color background = toastBackground(toastType);
        Color foreground = toastForeground(toastType);
        Color border = toastBorder(toastType);

        g2.setColor(new Color(15, 23, 42, 45));
        g2.fillRoundRect(x + 2, y + 3, boxWidth, boxHeight, 14, 14);
        g2.setColor(background);
        g2.fillRoundRect(x, y, boxWidth, boxHeight, 14, 14);
        Stroke previous = g2.getStroke();
        g2.setStroke(new BasicStroke(1.1f));
        g2.setColor(border);
        g2.drawRoundRect(x, y, boxWidth, boxHeight, 14, 14);
        g2.setStroke(previous);
        g2.setColor(foreground);
        int tx = x + paddingX;
        int ty = y + paddingY + fm.getAscent();
        g2.drawString(text, tx, ty);
    }

    private void updateVisibility() {
        setVisible(loadingVisible || toastVisible);
    }

    private static String safe(String message, String fallback) {
        if (message == null || message.trim().isEmpty()) {
            return fallback;
        }
        return message.trim();
    }

    private static Color toastBackground(ToastType type) {
        if (type == ToastType.SUCCESS) {
            return new Color(233, 247, 237);
        }
        if (type == ToastType.WARNING) {
            return new Color(255, 246, 228);
        }
        if (type == ToastType.ERROR) {
            return new Color(255, 236, 236);
        }
        return new Color(232, 243, 255);
    }

    private static Color toastForeground(ToastType type) {
        if (type == ToastType.SUCCESS) {
            return UiStyles.SUCCESS;
        }
        if (type == ToastType.WARNING) {
            return UiStyles.WARNING;
        }
        if (type == ToastType.ERROR) {
            return UiStyles.ERROR;
        }
        return UiStyles.PRIMARY;
    }

    private static Color toastBorder(ToastType type) {
        if (type == ToastType.SUCCESS) {
            return new Color(174, 221, 189);
        }
        if (type == ToastType.WARNING) {
            return new Color(238, 206, 141);
        }
        if (type == ToastType.ERROR) {
            return new Color(235, 178, 178);
        }
        return new Color(168, 202, 241);
    }
}
