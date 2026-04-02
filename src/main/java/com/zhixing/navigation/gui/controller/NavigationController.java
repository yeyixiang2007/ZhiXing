package com.zhixing.navigation.gui.controller;

import com.zhixing.navigation.application.navigation.ConsolePathFormatter;
import com.zhixing.navigation.application.navigation.NavigationService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.PathResult;

import java.util.Objects;

public class NavigationController {
    private final CampusGraph graph;
    private final NavigationService navigationService;
    private final ConsolePathFormatter pathFormatter;

    public NavigationController(CampusGraph graph, NavigationService navigationService, ConsolePathFormatter pathFormatter) {
        this.graph = Objects.requireNonNull(graph, "graph must not be null");
        this.navigationService = Objects.requireNonNull(navigationService, "navigationService must not be null");
        this.pathFormatter = Objects.requireNonNull(pathFormatter, "pathFormatter must not be null");
    }

    public PathResult queryPath(String startId, String endId) {
        return navigationService.navigate(graph, startId, endId);
    }

    public String format(PathResult pathResult) {
        return pathFormatter.format(pathResult);
    }
}
