package com.zhixing.navigation.gui;

import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.gui.workbench.WorkbenchFeedback;
import com.zhixing.navigation.gui.workbench.command.CommandBus;
import com.zhixing.navigation.gui.workbench.command.UndoableCommand;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class EditCommandHandler {
    private final WorkbenchFeedback feedback;
    private final CommandBus<Admin> adminCommandBus;
    private final Supplier<Admin> currentAdminSupplier;
    private final BooleanSupplier ensureAdminLoggedIn;
    private final Runnable refreshAllData;
    private final Runnable refreshUndoRedoButtons;

    EditCommandHandler(
            WorkbenchFeedback feedback,
            CommandBus<Admin> adminCommandBus,
            Supplier<Admin> currentAdminSupplier,
            BooleanSupplier ensureAdminLoggedIn,
            Runnable refreshAllData,
            Runnable refreshUndoRedoButtons
    ) {
        this.feedback = feedback;
        this.adminCommandBus = adminCommandBus;
        this.currentAdminSupplier = currentAdminSupplier;
        this.ensureAdminLoggedIn = ensureAdminLoggedIn;
        this.refreshAllData = refreshAllData;
        this.refreshUndoRedoButtons = refreshUndoRedoButtons;
    }

    boolean canUndo() {
        return adminCommandBus.canUndo();
    }

    boolean canRedo() {
        return adminCommandBus.canRedo();
    }

    void undoLastEdit() {
        if (!adminCommandBus.canUndo()) {
            feedback.info("没有可撤销的编辑。");
            return;
        }
        if (!ensureAdminLoggedIn.getAsBoolean()) {
            return;
        }
        Admin operator = currentAdminSupplier.get();
        try {
            feedback.showLoading("正在撤销上一步编辑...");
            adminCommandBus.undo(operator);
            refreshAllData.run();
            feedback.info("撤销成功。");
            feedback.setStatus("管理员模式: 已撤销");
        } catch (RuntimeException ex) {
            feedback.showOperationError("撤销失败", ex);
            feedback.setStatus("管理员模式: 撤销失败");
        } finally {
            feedback.hideLoading();
            refreshUndoRedoButtons.run();
        }
    }

    void redoLastEdit() {
        if (!adminCommandBus.canRedo()) {
            feedback.info("没有可重做的编辑。");
            return;
        }
        if (!ensureAdminLoggedIn.getAsBoolean()) {
            return;
        }
        Admin operator = currentAdminSupplier.get();
        try {
            feedback.showLoading("正在重做上一步编辑...");
            adminCommandBus.redo(operator);
            refreshAllData.run();
            feedback.info("重做成功。");
            feedback.setStatus("管理员模式: 已重做");
        } catch (RuntimeException ex) {
            feedback.showOperationError("重做失败", ex);
            feedback.setStatus("管理员模式: 重做失败");
        } finally {
            feedback.hideLoading();
            refreshUndoRedoButtons.run();
        }
    }

    void executeAdminEditCommand(String loadingText, AdminEditCommand command) {
        if (!ensureAdminLoggedIn.getAsBoolean()) {
            return;
        }
        Admin operator = currentAdminSupplier.get();
        try {
            feedback.showLoading(loadingText);
            adminCommandBus.execute(command, operator);
            refreshAllData.run();
            feedback.success(command.successMessage());
            feedback.setStatus("管理员模式: " + command.successMessage());
        } catch (RuntimeException ex) {
            feedback.showOperationError("操作失败", ex);
            feedback.setStatus("管理员模式: 操作失败");
        } finally {
            feedback.hideLoading();
            refreshUndoRedoButtons.run();
        }
    }

    AdminEditCommand adminEditCommand(String successMessage, AdminAction executeAction, AdminAction undoAction) {
        return new AdminEditCommand() {
            @Override
            public void execute(Admin admin) {
                executeAction.apply(admin);
            }

            @Override
            public void undo(Admin admin) {
                undoAction.apply(admin);
            }

            @Override
            public String successMessage() {
                return successMessage;
            }
        };
    }

    interface AdminEditCommand extends UndoableCommand<Admin> {
        String successMessage();
    }

    @FunctionalInterface
    interface AdminAction {
        void apply(Admin admin);
    }
}
