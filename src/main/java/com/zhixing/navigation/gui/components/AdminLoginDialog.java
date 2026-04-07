package com.zhixing.navigation.gui.components;

import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.gui.controller.AuthController;
import com.zhixing.navigation.gui.styles.UiStyles;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class AdminLoginDialog extends JDialog {
    private static final int INPUT_FIELD_HEIGHT = 36;

    private final AuthController authController;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JLabel errorLabel;

    private Admin authenticatedAdmin;

    public AdminLoginDialog(JFrame owner, AuthController authController) {
        super(owner, "管理员登录", true);
        this.authController = authController;
        this.usernameField = UiStyles.formField(20);
        this.passwordField = new JPasswordField(20);
        UiStyles.applyTextFieldStyle(this.passwordField);
        applyInputHeight(this.usernameField);
        applyInputHeight(this.passwordField);
        this.errorLabel = new JLabel(" ");

        initializeLayout();
    }

    public Admin showDialog() {
        authenticatedAdmin = null;
        usernameField.setText("");
        passwordField.setText("");
        errorLabel.setText(" ");
        setVisible(true);
        return authenticatedAdmin;
    }

    private void initializeLayout() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(430, 260);
        setResizable(false);
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        JLabel title = new JLabel("请输入管理员账号和密码", SwingConstants.LEFT);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("账号"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(usernameField, gbc);
        gbc.weightx = 0;

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("密码"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(passwordField, gbc);
        gbc.weightx = 0;

        errorLabel.setForeground(new Color(196, 43, 28));
        gbc.gridx = 1;
        gbc.gridy = 2;
        form.add(errorLabel, gbc);

        root.add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> dispose());
        JButton loginButton = new JButton("登录");
        loginButton.addActionListener(e -> tryLogin());
        actions.add(cancelButton);
        actions.add(loginButton);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(loginButton);
    }

    private void tryLogin() {
        String username = safeTrim(usernameField.getText());
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.trim().isEmpty()) {
            errorLabel.setText("账号和密码不能为空。");
            return;
        }
        try {
            authenticatedAdmin = authController.loginAdmin(username, password);
            dispose();
        } catch (RuntimeException ex) {
            authenticatedAdmin = null;
            errorLabel.setText("登录失败：" + ex.getMessage());
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    private static String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private static void applyInputHeight(JTextField field) {
        Dimension preferredSize = field.getPreferredSize();
        field.setPreferredSize(new Dimension(preferredSize.width, INPUT_FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(140, INPUT_FIELD_HEIGHT));
    }
}
