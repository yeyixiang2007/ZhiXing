package com.zhixing.navigation.gui;

import com.zhixing.navigation.application.auth.AuthService;
import com.zhixing.navigation.application.map.MapService;
import com.zhixing.navigation.application.navigation.ConsolePathFormatter;
import com.zhixing.navigation.application.navigation.NavigationService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.planning.DijkstraStrategy;
import com.zhixing.navigation.gui.components.AdminLoginDialog;
import com.zhixing.navigation.gui.components.LoadingOverlay;
import com.zhixing.navigation.gui.components.ViewportFillPanel;
import com.zhixing.navigation.gui.controller.AuthController;
import com.zhixing.navigation.gui.controller.MapController;
import com.zhixing.navigation.gui.controller.NavigationController;
import com.zhixing.navigation.gui.model.OverviewData;
import com.zhixing.navigation.gui.model.RoadOption;
import com.zhixing.navigation.gui.model.RouteVisualizationDto;
import com.zhixing.navigation.gui.model.VertexOption;
import com.zhixing.navigation.gui.routing.AppRoute;
import com.zhixing.navigation.gui.styles.UiStyles;
import com.zhixing.navigation.gui.view.ForbiddenManageView;
import com.zhixing.navigation.gui.view.OverviewDashboardView;
import com.zhixing.navigation.gui.view.PathQueryView;
import com.zhixing.navigation.gui.view.PlaceBrowseView;
import com.zhixing.navigation.gui.view.RoadManageView;
import com.zhixing.navigation.gui.view.VertexManageView;
import com.zhixing.navigation.gui.workbench.EditToolMode;
import com.zhixing.navigation.gui.workbench.LayerPanel;
import com.zhixing.navigation.gui.workbench.MapCanvas;
import com.zhixing.navigation.gui.workbench.MapWorkbenchView;
import com.zhixing.navigation.gui.workbench.WorkbenchFeedback;
import com.zhixing.navigation.gui.workbench.command.CommandBus;
import com.zhixing.navigation.gui.workbench.state.MapViewState;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JLayeredPane;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.imageio.ImageIO;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


final class MainViewWorkbenchTools {
    private final MainView view;

    MainViewWorkbenchTools(MainView view) {
        this.view = view;
    }

    void showWorkbenchToolsMenu(JButton anchorButton) {
        if (anchorButton == null) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem importReferenceImageItem = new JMenuItem("导入参考图...");
        importReferenceImageItem.addActionListener(e -> promptImportReferenceImage());
        JMenuItem clearReferenceImageItem = new JMenuItem("清除参考图");
        clearReferenceImageItem.addActionListener(e -> handleClearReferenceImage());
        JMenuItem setReferenceScaleItem = new JMenuItem("设置参考图比例...");
        setReferenceScaleItem.addActionListener(e -> promptSetReferenceImageScale());
        JMenuItem setMapScaleItem = new JMenuItem("设置比例尺...");
        setMapScaleItem.addActionListener(e -> promptSetMetersPerWorldUnit());
        JMenuItem calibrateScaleItem = new JMenuItem("比例尺校准...");
        calibrateScaleItem.addActionListener(e -> promptCalibrateMetersPerWorldUnit());
        JMenuItem backupItem = new JMenuItem("执行备份...");
        backupItem.addActionListener(e -> promptBackupFromWorkbenchTools());
        JMenuItem restoreItem = new JMenuItem("执行恢复...");
        restoreItem.addActionListener(e -> promptRestoreFromWorkbenchTools());
        menu.add(importReferenceImageItem);
        menu.add(clearReferenceImageItem);
        menu.add(setReferenceScaleItem);
        menu.add(setMapScaleItem);
        menu.add(calibrateScaleItem);
        menu.addSeparator();
        menu.add(backupItem);
        menu.add(restoreItem);
        menu.show(anchorButton, 0, anchorButton.getHeight());
    }

    void promptImportReferenceImage() {
        if (!view.ensureAdminLoggedIn()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择参考图");
        chooser.setFileFilter(new FileNameExtensionFilter("图片文件", "png", "jpg", "jpeg", "bmp", "gif"));
        int result = chooser.showOpenDialog(view);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null) {
            return;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                view.feedback.showErrorDialog("导入失败", "文件不是有效图片。");
                return;
            }
            view.mapCanvas.setReferenceImage(image);
            view.feedback.success("参考图导入成功。");
            view.feedback.setStatus("工作台工具: 已导入参考图");
        } catch (IOException ex) {
            view.feedback.showOperationError("导入失败", ex);
            view.feedback.setStatus("工作台工具: 参考图导入失败");
        }
    }

    void handleClearReferenceImage() {
        if (!view.ensureAdminLoggedIn()) {
            return;
        }
        if (!view.mapCanvas.hasReferenceImage()) {
            view.feedback.info("当前没有已导入的参考图。");
            return;
        }
        view.mapCanvas.clearReferenceImage();
        view.feedback.info("已清除参考图。");
        view.feedback.setStatus("工作台工具: 已清除参考图");
    }

    void promptSetReferenceImageScale() {
        if (!view.ensureAdminLoggedIn()) {
            return;
        }
        String defaultValue = String.format("%.2f", view.mapCanvas.getReferenceImageScale());
        String input = JOptionPane.showInputDialog(view, "请输入参考图比例（如 1.0）：", defaultValue);
        if (input == null) {
            return;
        }
        try {
            double scale = MainView.parsePositiveDouble(input, "参考图比例");
            view.mapCanvas.setReferenceImageScale(scale);
            view.feedback.success("参考图比例已更新。");
            view.feedback.setStatus("工作台工具: 参考图比例=" + String.format("%.2f", scale));
        } catch (RuntimeException ex) {
            view.feedback.showOperationError("设置失败", ex);
            view.feedback.setStatus("工作台工具: 参考图比例设置失败");
        }
    }

    void promptSetMetersPerWorldUnit() {
        if (!view.ensureAdminLoggedIn()) {
            return;
        }
        String defaultValue = String.format("%.2f", view.mapCanvas.getMetersPerWorldUnit());
        String input = JOptionPane.showInputDialog(view, "请输入比例尺（每 1 坐标单位=多少米）：", defaultValue);
        if (input == null) {
            return;
        }
        try {
            double metersPerUnit = MainView.parsePositiveDouble(input, "比例尺");
            view.mapCanvas.setMetersPerWorldUnit(metersPerUnit);
            view.feedback.success("比例尺已更新。");
            view.feedback.setStatus("工作台工具: 比例尺已更新");
        } catch (RuntimeException ex) {
            view.feedback.showOperationError("设置失败", ex);
            view.feedback.setStatus("工作台工具: 比例尺设置失败");
        }
    }

    void promptCalibrateMetersPerWorldUnit() {
        if (!view.ensureAdminLoggedIn()) {
            return;
        }
        List<String> selectedVertexIds = view.viewState.getSelectedVertexIds();
        String defaultFromId = selectedVertexIds.size() >= 1 ? selectedVertexIds.get(0) : "";
        String defaultToId = selectedVertexIds.size() >= 2 ? selectedVertexIds.get(1) : "";

        JTextField fromIdField = new JTextField(defaultFromId, 12);
        JTextField toIdField = new JTextField(defaultToId, 12);
        JTextField metersField = new JTextField("100", 12);
        Object[] message = new Object[]{
                "参考点A（节点ID）", fromIdField,
                "参考点B（节点ID）", toIdField,
                "A-B真实距离（米）", metersField
        };
        int result = JOptionPane.showConfirmDialog(
                view,
                message,
                "比例尺校准",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            String fromId = MainView.safeTrim(fromIdField.getText());
            String toId = MainView.safeTrim(toIdField.getText());
            if (MainView.isBlank(fromId) || MainView.isBlank(toId)) {
                throw new IllegalArgumentException("请先输入两个节点ID。");
            }
            if (fromId.equals(toId)) {
                throw new IllegalArgumentException("两个节点ID不能相同。");
            }
            double realMeters = MainView.parsePositiveDouble(metersField.getText(), "真实距离");
            Vertex from = view.requireVertex(fromId);
            Vertex to = view.requireVertex(toId);
            double worldDistance = Math.hypot(to.getX() - from.getX(), to.getY() - from.getY());
            if (worldDistance <= 0) {
                throw new IllegalArgumentException("两点在地图上的距离为0，无法校准比例尺。");
            }
            double metersPerUnit = realMeters / worldDistance;
            view.mapCanvas.setMetersPerWorldUnit(metersPerUnit);
            view.feedback.success("比例尺校准完成。");
            view.feedback.setStatus(String.format(
                    "工作台工具: 比例尺=%.4f 米/单位（参考 %s-%s）",
                    metersPerUnit,
                    fromId,
                    toId
            ));
        } catch (RuntimeException ex) {
            view.feedback.showOperationError("校准失败", ex);
            view.feedback.setStatus("工作台工具: 比例尺校准失败");
        }
    }

    void promptBackupFromWorkbenchTools() {
        if (!view.ensureAdminLoggedIn()) {
            return;
        }
        String defaultName = "backup-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String input = JOptionPane.showInputDialog(view, "请输入备份名称：", defaultName);
        if (input == null) {
            return;
        }
        handleBackup(input, "工作台工具");
    }

    void promptRestoreFromWorkbenchTools() {
        if (!view.ensureAdminLoggedIn()) {
            return;
        }
        String input = JOptionPane.showInputDialog(view, "请输入要恢复的备份名称：", "");
        if (input == null) {
            return;
        }
        handleRestore(input, "工作台工具");
    }

    void handleBackup(String backupName, String sourceTag) {
        try {
            view.feedback.showLoading("正在执行数据备份...");
            view.mapController.backupData(backupName);
            view.feedback.success("数据备份成功。");
            view.feedback.setStatus(sourceTag + ": 数据备份完成");
        } catch (RuntimeException ex) {
            view.feedback.showOperationError("备份失败", ex);
            view.feedback.setStatus(sourceTag + ": 数据备份失败");
        } finally {
            view.feedback.hideLoading();
        }
    }

    void handleRestore(String backupName, String sourceTag) {
        if (!view.feedback.confirm("确认恢复", "恢复会覆盖当前数据，确定继续吗？")) {
            return;
        }
        try {
            view.feedback.showLoading("正在恢复数据备份...");
            view.mapController.restoreData(backupName);
            view.feedback.warning("数据恢复成功，请重启应用以完全生效。");
            view.feedback.setStatus(sourceTag + ": 数据恢复完成");
        } catch (RuntimeException ex) {
            view.feedback.showOperationError("恢复失败", ex);
            view.feedback.setStatus(sourceTag + ": 数据恢复失败");
        } finally {
            view.feedback.hideLoading();
        }
    }

}
