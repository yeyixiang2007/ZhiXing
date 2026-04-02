package com.zhixing.navigation.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class PathResult {
    private final Vertex startVertex;
    private final Vertex endVertex;
    private final List<Vertex> pathList;
    private final double totalDistance;
    private final double estimatedTime;
    private final List<Double> segmentDistances;
    private final List<String> naviInstructions;

    public PathResult(
            Vertex startVertex,
            Vertex endVertex,
            List<Vertex> pathList,
            double totalDistance,
            double estimatedTime,
            List<Double> segmentDistances,
            List<String> naviInstructions
    ) {
        this.startVertex = Objects.requireNonNull(startVertex, "startVertex must not be null");
        this.endVertex = Objects.requireNonNull(endVertex, "endVertex must not be null");
        this.pathList = toValidatedPathList(pathList, startVertex, endVertex);
        this.totalDistance = requireNonNegativeFinite(totalDistance, "totalDistance");
        this.estimatedTime = requireNonNegativeFinite(estimatedTime, "estimatedTime");
        this.segmentDistances = toValidatedSegmentDistances(segmentDistances, this.pathList);
        this.naviInstructions = toValidatedInstructions(naviInstructions);
    }

    public Vertex getStartVertex() {
        return startVertex;
    }

    public Vertex getEndVertex() {
        return endVertex;
    }

    public List<Vertex> getPathList() {
        return pathList;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public double getEstimatedTime() {
        return estimatedTime;
    }

    public List<Double> getSegmentDistances() {
        return segmentDistances;
    }

    public List<String> getNaviInstructions() {
        return naviInstructions;
    }

    private static List<Vertex> toValidatedPathList(List<Vertex> pathList, Vertex start, Vertex end) {
        if (pathList == null || pathList.isEmpty()) {
            throw new IllegalArgumentException("pathList must not be empty");
        }
        List<Vertex> normalized = new ArrayList<Vertex>(pathList.size());
        for (Vertex vertex : pathList) {
            normalized.add(Objects.requireNonNull(vertex, "pathList must not contain null"));
        }
        Vertex first = normalized.get(0);
        Vertex last = normalized.get(normalized.size() - 1);
        if (!first.equals(start)) {
            throw new IllegalArgumentException("pathList must start from startVertex");
        }
        if (!last.equals(end)) {
            throw new IllegalArgumentException("pathList must end at endVertex");
        }
        return Collections.unmodifiableList(normalized);
    }

    private static List<Double> toValidatedSegmentDistances(List<Double> segmentDistances, List<Vertex> pathList) {
        if (segmentDistances == null) {
            throw new IllegalArgumentException("segmentDistances must not be null");
        }

        int expectedSize = pathList.size() > 0 ? pathList.size() - 1 : 0;
        if (segmentDistances.size() != expectedSize) {
            throw new IllegalArgumentException("segmentDistances size must match path edges count");
        }

        List<Double> normalized = new ArrayList<Double>(segmentDistances.size());
        for (Double distance : segmentDistances) {
            if (distance == null) {
                throw new IllegalArgumentException("segmentDistances must not contain null");
            }
            normalized.add(requireNonNegativeFinite(distance, "segmentDistance"));
        }
        return Collections.unmodifiableList(normalized);
    }

    private static List<String> toValidatedInstructions(List<String> instructions) {
        if (instructions == null) {
            throw new IllegalArgumentException("naviInstructions must not be null");
        }
        List<String> normalized = new ArrayList<String>(instructions.size());
        for (String item : instructions) {
            if (item == null || item.trim().isEmpty()) {
                throw new IllegalArgumentException("naviInstructions must not contain blank text");
            }
            normalized.add(item.trim());
        }
        return Collections.unmodifiableList(normalized);
    }

    private static double requireNonNegativeFinite(double value, String fieldName) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(fieldName + " must be finite and >= 0");
        }
        return value;
    }
}
