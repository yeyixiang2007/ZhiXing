package com.zhixing.navigation.gui.controller;

import com.zhixing.navigation.application.auth.AuthorizationException;
import com.zhixing.navigation.domain.planning.NoRouteFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class ControllerExceptionMapperTest {

    @Test
    void shouldMapNoRouteException() {
        String message = ControllerExceptionMapper.toUserMessage(new NoRouteFoundException("A 到 B 不可达"));
        Assertions.assertTrue(message.startsWith("路径不可达："));
    }

    @Test
    void shouldMapInputExceptionToFriendlyMessage() {
        String message = ControllerExceptionMapper.toUserMessage(new IllegalArgumentException("vertex not found: A"));
        Assertions.assertEquals("输入错误：未找到指定地点，请确认点位标识。", message);
    }

    @Test
    void shouldMapFileExceptionToFriendlyMessage() {
        Throwable ex = new IllegalStateException("Failed to restore backup", new IOException("disk io failed"));
        String message = ControllerExceptionMapper.toUserMessage(ex);
        Assertions.assertEquals("文件异常：数据恢复失败，请检查备份内容。", message);
    }

    @Test
    void shouldMapAuthorizationException() {
        String message = ControllerExceptionMapper.toUserMessage(new AuthorizationException("Permission denied"));
        Assertions.assertTrue(message.startsWith("权限不足："));
    }
}
