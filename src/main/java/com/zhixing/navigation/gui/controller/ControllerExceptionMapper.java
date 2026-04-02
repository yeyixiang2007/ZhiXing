package com.zhixing.navigation.gui.controller;

import com.zhixing.navigation.application.auth.AuthenticationException;
import com.zhixing.navigation.application.auth.AuthorizationException;
import com.zhixing.navigation.domain.planning.NoRouteFoundException;

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
            return safe(message, "输入参数不合法。");
        }
        if (throwable instanceof IllegalStateException) {
            return safe(message, "系统状态异常。");
        }
        return "操作失败：" + safe(message, throwable.getClass().getSimpleName());
    }

    private static String safe(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
