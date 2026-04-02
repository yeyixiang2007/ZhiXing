package com.zhixing.navigation.gui.components;

import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;

public class ResultMessageBar extends JPanel {
    public enum MessageType {
        SUCCESS,
        INFO,
        WARNING,
        ERROR
    }

    private final JLabel messageLabel;
    private final Timer hideTimer;

    public ResultMessageBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        messageLabel = new JLabel();
        messageLabel.setFont(UiStyles.BODY_FONT);
        add(messageLabel, BorderLayout.CENTER);
        setVisible(false);

        hideTimer = new Timer(2500, e -> setVisible(false));
        hideTimer.setRepeats(false);
    }

    public void showMessage(String message, MessageType type) {
        messageLabel.setText(message);
        applyType(type);
        setVisible(true);
        hideTimer.restart();
    }

    private void applyType(MessageType type) {
        if (type == MessageType.SUCCESS) {
            setBarStyle(new Color(227, 246, 235), UiStyles.SUCCESS);
            return;
        }
        if (type == MessageType.WARNING) {
            setBarStyle(new Color(255, 244, 224), UiStyles.WARNING);
            return;
        }
        if (type == MessageType.ERROR) {
            setBarStyle(new Color(255, 232, 232), UiStyles.ERROR);
            return;
        }
        setBarStyle(new Color(229, 243, 255), UiStyles.PRIMARY);
    }

    private void setBarStyle(Color background, Color textColor) {
        setBackground(background);
        messageLabel.setForeground(textColor);
    }
}
