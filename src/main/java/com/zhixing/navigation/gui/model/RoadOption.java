package com.zhixing.navigation.gui.model;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.RoadType;

public final class RoadOption {
    private final String fromId;
    private final String toId;
    private final RoadType roadType;
    private final boolean forbidden;
    private final double weight;

    public RoadOption(String fromId, String toId, RoadType roadType, boolean forbidden, double weight) {
        this.fromId = fromId;
        this.toId = toId;
        this.roadType = roadType;
        this.forbidden = forbidden;
        this.weight = weight;
    }

    public static RoadOption fromEdge(Edge edge) {
        return new RoadOption(
                edge.getFromVertex().getId(),
                edge.getToVertex().getId(),
                edge.getRoadType(),
                edge.isForbidden(),
                edge.getWeight()
        );
    }

    public String getFromId() {
        return fromId;
    }

    public String getToId() {
        return toId;
    }

    public RoadType getRoadType() {
        return roadType;
    }

    public boolean isForbidden() {
        return forbidden;
    }

    public double getWeight() {
        return weight;
    }

    public String key() {
        return fromId + "->" + toId;
    }

    @Override
    public String toString() {
        return fromId + " -> " + toId + " | " + roadType.name() + " | " + (forbidden ? "禁行中" : "可通行");
    }
}
