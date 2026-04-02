package com.zhixing.navigation.application.navigation;

import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.planning.PathPlanningStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NavigationService {
    private static final double DEFAULT_WALKING_SPEED_METERS_PER_MIN = 75.0;

    private final PathPlanningStrategy strategy;
    private final double walkingSpeedMetersPerMinute;

    public NavigationService(PathPlanningStrategy strategy) {
        this(strategy, DEFAULT_WALKING_SPEED_METERS_PER_MIN);
    }

    public NavigationService(PathPlanningStrategy strategy, double walkingSpeedMetersPerMinute) {
        this.strategy = Objects.requireNonNull(strategy, "strategy must not be null");
        if (Double.isNaN(walkingSpeedMetersPerMinute) || Double.isInfinite(walkingSpeedMetersPerMinute) || walkingSpeedMetersPerMinute <= 0) {
            throw new IllegalArgumentException("walkingSpeedMetersPerMinute must be finite and > 0");
        }
        this.walkingSpeedMetersPerMinute = walkingSpeedMetersPerMinute;
    }

    public PathResult navigate(CampusGraph graph, String startId, String endId) {
        PathResult rawPath = strategy.plan(graph, startId, endId);
        return enrich(rawPath);
    }

    public PathResult enrich(PathResult rawPath) {
        Objects.requireNonNull(rawPath, "rawPath must not be null");
        double estimatedTime = rawPath.getTotalDistance() / walkingSpeedMetersPerMinute;
        List<String> instructions = buildInstructions(rawPath);
        return new PathResult(
                rawPath.getStartVertex(),
                rawPath.getEndVertex(),
                rawPath.getPathList(),
                rawPath.getTotalDistance(),
                estimatedTime,
                rawPath.getSegmentDistances(),
                instructions
        );
    }

    public List<String> buildInstructions(PathResult pathResult) {
        Objects.requireNonNull(pathResult, "pathResult must not be null");
        List<Vertex> vertices = pathResult.getPathList();
        List<Double> segmentDistances = pathResult.getSegmentDistances();

        if (vertices.size() == 1) {
            List<String> single = new ArrayList<String>(1);
            single.add("已在目的地，无需移动。");
            return single;
        }

        List<String> instructions = new ArrayList<String>();
        for (int i = 1; i < vertices.size(); i++) {
            Vertex from = vertices.get(i - 1);
            Vertex to = vertices.get(i);
            double distance = segmentDistances.get(i - 1);
            instructions.add("第" + i + "步：从 " + from.getName() + " 前往 " + to.getName()
                    + "，步行约 " + Math.round(distance) + " 米。");
        }
        instructions.add("已到达目的地：" + pathResult.getEndVertex().getName() + "。");
        return instructions;
    }
}

