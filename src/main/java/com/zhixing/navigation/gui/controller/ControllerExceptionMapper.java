package com.zhixing.navigation.gui.controller;

import com.zhixing.navigation.application.auth.AuthenticationException;
import com.zhixing.navigation.application.auth.AuthorizationException;
import com.zhixing.navigation.domain.planning.NoRouteFoundException;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.Locale;

public final class ControllerExceptionMapper {
    private ControllerExceptionMapper() {
    }

    public static String toUserMessage(Throwable throwable) {
        if (throwable == null) {
            return "发生未知错误。";
        }
        String message = throwable.getMessage();
        if (throwable instanceof NoRouteFoundException) {
            return "路径不可达：" + safe(message, "请检查起点、终点或禁行路段。");
        }
        if (throwable instanceof AuthenticationException) {
            return "登录失败：" + safe(message, "账号或密码错误。");
        }
        if (throwable instanceof AuthorizationException) {
            return "权限不足：" + safe(message, "当前账号无权限执行该操作。");
        }
        if (throwable instanceof IllegalArgumentException) {
            return "输入错误：" + normalizeInputMessage(message);
        }
        if (isFileError(throwable)) {
            return "文件异常：" + normalizeFileMessage(throwable);
        }
        if (throwable instanceof IllegalStateException) {
            return "系统异常：" + safe(message, "系统状态异常。");
        }
        return "操作失败：" + safe(message, throwable.getClass().getSimpleName());
    }

    private static boolean isFileError(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof IOException || cursor instanceof InvalidPathException) {
                return true;
            }
            String message = safe(cursor.getMessage(), "");
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("data file")
                    || normalized.contains("backup")
                    || normalized.contains("json")
                    || normalized.contains("failed to read")
                    || normalized.contains("failed to write")
                    || normalized.contains("failed to restore")
                    || normalized.contains("failed to create data directories")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private static String normalizeInputMessage(String message) {
        String normalized = safe(message, "输入参数不合法。");
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.contains("must not be blank")) {
            return "必填字段不能为空。";
        }
        if (lower.contains("vertex not found")) {
            return "未找到指定地点，请确认点位标识。";
        }
        if (lower.contains("road not found")) {
            return "未找到指定道路，请确认起终点。";
        }
        if (lower.contains("already exists")) {
            return "对象已存在，请避免重复创建。";
        }
        if (lower.contains("must be finite") || lower.contains("greater than 0")) {
            return "数值范围不合法，请检查输入。";
        }
        return normalized;
    }

    private static String normalizeFileMessage(Throwable throwable) {
        String message = safe(findDeepestMessage(throwable), "");
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("backup not found")) {
            return "备份不存在，请确认备份名称。";
        }
        if (lower.contains("required backup file missing")) {
            return "备份文件不完整，无法恢复。";
        }
        if (lower.contains("failed to backup data")) {
            return "数据备份失败，请检查目录权限。";
        }
        if (lower.contains("failed to restore backup")) {
            return "数据恢复失败，请检查备份内容。";
        }
        if (lower.contains("failed to read data file") || lower.contains("invalid json format")) {
            return "数据文件读取失败或格式损坏。";
        }
        if (lower.contains("failed to write data file")) {
            return "数据文件写入失败，请检查磁盘空间与写入权限。";
        }
        if (lower.contains("data file not found")) {
            return "数据文件不存在，请先初始化数据或恢复备份。";
        }
        if (lower.contains("failed to create data directories")) {
            return "无法创建数据目录，请检查目录权限。";
        }
        return safe(message, "文件读写失败，请检查数据目录和权限。");
    }

    private static String findDeepestMessage(Throwable throwable) {
        Throwable cursor = throwable;
        String latest = null;
        while (cursor != null) {
            if (cursor.getMessage() != null && !cursor.getMessage().trim().isEmpty()) {
                latest = cursor.getMessage();
            }
            cursor = cursor.getCause();
        }
        return latest;
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
