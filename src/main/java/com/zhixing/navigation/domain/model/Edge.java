package com.zhixing.navigation.domain.model;

import java.util.Objects;

public final class Edge {
    private final Vertex fromVertex;
    private final Vertex toVertex;
    private final double weight;
    private final boolean oneWay;
    private final boolean forbidden;
    private final RoadType roadType;

    public Edge(Vertex fromVertex, Vertex toVertex, double weight, boolean oneWay, boolean forbidden, RoadType roadType) {
        this.fromVertex = Objects.requireNonNull(fromVertex, "fromVertex must not be null");
        this.toVertex = Objects.requireNonNull(toVertex, "toVertex must not be null");
        if (this.fromVertex.equals(this.toVertex)) {
            throw new IllegalArgumentException("fromVertex and toVertex must be different");
        }
        if (Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0) {
            throw new IllegalArgumentException("weight must be finite and greater than 0");
        }
        this.weight = weight;
        this.oneWay = oneWay;
        this.forbidden = forbidden;
        this.roadType = Objects.requireNonNull(roadType, "roadType must not be null");
    }

    public Vertex getFromVertex() {
        return fromVertex;
    }

    public Vertex getToVertex() {
        return toVertex;
    }

    public double getWeight() {
        return weight;
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public boolean isForbidden() {
        return forbidden;
    }

    public RoadType getRoadType() {
        return roadType;
    }

    public boolean isAvailable() {
        return !forbidden;
    }

    public Edge withForbidden(boolean newForbidden) {
        return new Edge(fromVertex, toVertex, weight, oneWay, newForbidden, roadType);
    }

    public Edge reverseForTwoWay() {
        if (oneWay) {
            throw new IllegalStateException("one-way edge cannot create reverse edge");
        }
        return new Edge(toVertex, fromVertex, weight, false, forbidden, roadType);
    }

    @Override
    public String toString() {
        return "Edge{" +
                "from=" + fromVertex.getId() +
                ", to=" + toVertex.getId() +
                ", weight=" + weight +
                ", oneWay=" + oneWay +
                ", forbidden=" + forbidden +
                ", roadType=" + roadType +
                '}';
    }
}
