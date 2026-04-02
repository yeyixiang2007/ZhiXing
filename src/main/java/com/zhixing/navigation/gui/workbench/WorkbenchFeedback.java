package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.gui.components.LoadingOverlay;
import com.zhixing.navigation.gui.components.ResultMessageBar;
import com.zhixing.navigation.gui.components.UiDialogs;
import com.zhixing.navigation.gui.controller.ControllerExceptionMapper;

import java.awt.Component;
import java.util.function.Consumer;

public class WorkbenchFeedback {
    private final Component dialogParent;
    private final ResultMessageBar messageBar;
    private final LoadingOverlay loadingOverlay;
    private final Consumer<String> statusUpdater;

    public WorkbenchFeedback(
            Component dialogParent,
            ResultMessageBar messageBar,
            LoadingOverlay loadingOverlay,
            Consumer<String> statusUpdater
    ) {
        this.dialogParent = dialogParent;
        this.messageBar = messageBar;
        this.loadingOverlay = loadingOverlay;
        this.statusUpdater = statusUpdater;
    }

    public void info(String message) {
        messageBar.showMessage(message, ResultMessageBar.MessageType.INFO);
    }

    public void success(String message) {
        messageBar.showMessage(message, ResultMessageBar.MessageType.SUCCESS);
    }

    public void warning(String message) {
        messageBar.showMessage(message, ResultMessageBar.MessageType.WARNING);
    }

    public void error(String message) {
        messageBar.showMessage(message, ResultMessageBar.MessageType.ERROR);
    }

    public boolean confirm(String title, String message) {
        return UiDialogs.showConfirm(dialogParent, title, message);
    }

    public void showErrorDialog(String title, String message) {
        UiDialogs.showError(dialogParent, title, message);
        error(message);
    }

    public void showOperationError(String title, Throwable throwable) {
        String userMessage = ControllerExceptionMapper.toUserMessage(throwable);
        showErrorDialog(title, userMessage);
    }

    public void showLoading(String loadingText) {
        loadingOverlay.showLoading(loadingText);
    }

    public void hideLoading() {
        loadingOverlay.hideLoading();
    }

    public void setStatus(String text) {
        statusUpdater.accept(text);
    }
}
