package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.gui.components.LoadingOverlay;
import com.zhixing.navigation.gui.components.UiDialogs;
import com.zhixing.navigation.gui.controller.ControllerExceptionMapper;

import java.awt.Component;
import java.util.function.Consumer;

public class WorkbenchFeedback {
    private final Component dialogParent;
    private final LoadingOverlay loadingOverlay;
    private final Consumer<String> statusUpdater;

    public WorkbenchFeedback(
            Component dialogParent,
            LoadingOverlay loadingOverlay,
            Consumer<String> statusUpdater
    ) {
        this.dialogParent = dialogParent;
        this.loadingOverlay = loadingOverlay;
        this.statusUpdater = statusUpdater;
    }

    public void info(String message) {
        loadingOverlay.showToast(message, LoadingOverlay.ToastType.INFO);
    }

    public void success(String message) {
        loadingOverlay.showToast(message, LoadingOverlay.ToastType.SUCCESS);
    }

    public void warning(String message) {
        loadingOverlay.showToast(message, LoadingOverlay.ToastType.WARNING);
    }

    public void error(String message) {
        loadingOverlay.showToast(message, LoadingOverlay.ToastType.ERROR);
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
