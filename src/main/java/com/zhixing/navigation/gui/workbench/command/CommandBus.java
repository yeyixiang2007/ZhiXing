package com.zhixing.navigation.gui.workbench.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class CommandBus<C> {
    private final Deque<UndoableCommand<C>> undoStack;
    private final Deque<UndoableCommand<C>> redoStack;

    public CommandBus() {
        this.undoStack = new ArrayDeque<UndoableCommand<C>>();
        this.redoStack = new ArrayDeque<UndoableCommand<C>>();
    }

    public void execute(UndoableCommand<C> command, C context) {
        Objects.requireNonNull(command, "command must not be null");
        command.execute(context);
        undoStack.push(command);
        redoStack.clear();
    }

    public UndoableCommand<C> undo(C context) {
        if (undoStack.isEmpty()) {
            return null;
        }
        UndoableCommand<C> command = undoStack.pop();
        try {
            command.undo(context);
            redoStack.push(command);
            return command;
        } catch (RuntimeException ex) {
            undoStack.push(command);
            throw ex;
        }
    }

    public UndoableCommand<C> redo(C context) {
        if (redoStack.isEmpty()) {
            return null;
        }
        UndoableCommand<C> command = redoStack.pop();
        try {
            command.execute(context);
            undoStack.push(command);
            return command;
        } catch (RuntimeException ex) {
            redoStack.push(command);
            throw ex;
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
