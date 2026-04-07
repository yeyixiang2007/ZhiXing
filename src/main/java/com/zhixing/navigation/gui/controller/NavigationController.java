package com.zhixing.navigation.gui.controller;

import com.zhixing.navigation.application.navigation.ConsolePathFormatter;
import com.zhixing.navigation.application.navigation.NavigationService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.gui.model.RouteVisualizationDto;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NavigationController {
    private static final RouteRenderStyle CURRENT_ROUTE_STYLE = new RouteRenderStyle(
            new Color(236, 152, 48),
            5.2f,
            false,
            true,
            true,
            new Color(34, 156, 108),
            new Color(219, 78, 74),
            new Color(247, 250, 253),
            new Color(236, 152, 48)
    );
    private static final RouteRenderStyle TRACE_ROUTE_STYLE = new RouteRenderStyle(
            new Color(96, 126, 168, 120),
            3.2f,
            true,
            false,
            false,
            new Color(34, 156, 108),
            new Color(219, 78, 74),
            new Color(247, 250, 253),
            new Color(236, 152, 48)
    );

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

    public NavigationVisualResult queryPathVisual(String startId, String endId) {
        PathResult pathResult = queryPath(startId, endId);
        return new NavigationVisualResult(pathResult, toCurrentRouteVisualization(pathResult));
    }

    public RouteVisualizationDto toCurrentRouteVisualization(PathResult pathResult) {
        return toRouteVisualization(pathResult, CURRENT_ROUTE_STYLE);
    }

    public RouteVisualizationDto toTraceRouteVisualization(PathResult pathResult) {
        return toRouteVisualization(pathResult, TRACE_ROUTE_STYLE);
    }

    public String format(PathResult pathResult) {
        return pathFormatter.format(pathResult);
    }

    private static RouteVisualizationDto toRouteVisualization(PathResult pathResult, RouteRenderStyle style) {
        Objects.requireNonNull(pathResult, "pathResult must not be null");
        Objects.requireNonNull(style, "style must not be null");

        List<String> vertexIds = new ArrayList<String>();
        List<RouteVisualizationDto.Segment> segments = new ArrayList<RouteVisualizationDto.Segment>();
        List<Vertex> path = pathResult.getPathList();
        for (Vertex vertex : path) {
            vertexIds.add(vertex.getId());
        }
        for (int i = 0; i < path.size() - 1; i++) {
            Vertex from = path.get(i);
            Vertex to = path.get(i + 1);
            segments.add(new RouteVisualizationDto.Segment(
                    i,
                    from.getId(),
                    to.getId(),
                    style.strokeColor,
                    style.strokeWidth,
                    style.dashed
            ));
        }
        return new RouteVisualizationDto(
                vertexIds,
                segments,
                style.showMarkers,
                style.showStepBadges,
                style.startMarkerColor,
                style.endMarkerColor,
                style.badgeFillColor,
                style.focusedBadgeFillColor
        );
    }

    public static final class NavigationVisualResult {
        private final PathResult pathResult;
        private final RouteVisualizationDto routeVisualization;

        public NavigationVisualResult(PathResult pathResult, RouteVisualizationDto routeVisualization) {
            this.pathResult = Objects.requireNonNull(pathResult, "pathResult must not be null");
            this.routeVisualization = Objects.requireNonNull(routeVisualization, "routeVisualization must not be null");
        }

        public PathResult getPathResult() {
            return pathResult;
        }

        public RouteVisualizationDto getRouteVisualization() {
            return routeVisualization;
        }
    }

    private static final class RouteRenderStyle {
        private final Color strokeColor;
        private final float strokeWidth;
        private final boolean dashed;
        private final boolean showMarkers;
        private final boolean showStepBadges;
        private final Color startMarkerColor;
        private final Color endMarkerColor;
        private final Color badgeFillColor;
        private final Color focusedBadgeFillColor;

        private RouteRenderStyle(
                Color strokeColor,
                float strokeWidth,
                boolean dashed,
                boolean showMarkers,
                boolean showStepBadges,
                Color startMarkerColor,
                Color endMarkerColor,
                Color badgeFillColor,
                Color focusedBadgeFillColor
        ) {
            this.strokeColor = Objects.requireNonNull(strokeColor, "strokeColor must not be null");
            this.strokeWidth = strokeWidth;
            this.dashed = dashed;
            this.showMarkers = showMarkers;
            this.showStepBadges = showStepBadges;
            this.startMarkerColor = Objects.requireNonNull(startMarkerColor, "startMarkerColor must not be null");
            this.endMarkerColor = Objects.requireNonNull(endMarkerColor, "endMarkerColor must not be null");
            this.badgeFillColor = Objects.requireNonNull(badgeFillColor, "badgeFillColor must not be null");
            this.focusedBadgeFillColor = Objects.requireNonNull(focusedBadgeFillColor, "focusedBadgeFillColor must not be null");
        }
    }
}
