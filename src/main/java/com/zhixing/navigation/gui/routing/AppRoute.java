package com.zhixing.navigation.gui.routing;

public enum AppRoute {
    USER_MODE("用户模式"),
    ADMIN_MODE("管理员模式"),
    SYSTEM_SETTINGS("系统设置");

    private final String title;

    AppRoute(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
