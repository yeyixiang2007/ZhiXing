package com.zhixing.navigation.domain.planning;

import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.PathResult;

public interface PathPlanningStrategy {
    PathResult plan(CampusGraph graph, String startId, String endId);
}

