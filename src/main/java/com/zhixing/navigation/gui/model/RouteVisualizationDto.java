package com.zhixing.navigation.gui.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RouteVisualizationDto {
    private final List<String> vertexIds;
    private final List<Segment> segments;
    private final boolean showMarkers;
    private final boolean showStepBadges;
    private final Color startMarkerColor;
    private final Color endMarkerColor;
    private final Color badgeFillColor;
    private final Color focusedBadgeFillColor;

    public RouteVisualizationDto(
            List<String> vertexIds,
            List<Segment> segments,
            boolean showMarkers,
            boolean showStepBadges,
            Color startMarkerColor,
            Color endMarkerColor,
            Color badgeFillColor,
            Color focusedBadgeFillColor
    ) {
        this.vertexIds = normalizeVertexIds(vertexIds);
        this.segments = normalizeSegments(segments);
        this.showMarkers = showMarkers;
        this.showStepBadges = showStepBadges;
        this.startMarkerColor = Objects.requireNonNull(startMarkerColor, "startMarkerColor must not be null");
        this.endMarkerColor = Objects.requireNonNull(endMarkerColor, "endMarkerColor must not be null");
        this.badgeFillColor = Objects.requireNonNull(badgeFillColor, "badgeFillColor must not be null");
        this.focusedBadgeFillColor = Objects.requireNonNull(focusedBadgeFillColor, "focusedBadgeFillColor must not be null");
    }

    public List<String> getVertexIds() {
        return vertexIds;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public int getSegmentCount() {
        return segments.size();
    }

    public boolean isShowMarkers() {
        return showMarkers;
    }

    public boolean isShowStepBadges() {
        return showStepBadges;
    }

    public Color getStartMarkerColor() {
        return startMarkerColor;
    }

    public Color getEndMarkerColor() {
        return endMarkerColor;
    }

    public Color getBadgeFillColor() {
        return badgeFillColor;
    }

    public Color getFocusedBadgeFillColor() {
        return focusedBadgeFillColor;
    }

    private static List<String> normalizeVertexIds(List<String> source) {
        if (source == null || source.size() < 2) {
            throw new IllegalArgumentException("vertexIds must contain at least 2 vertices");
        }
        List<String> normalized = new ArrayList<String>(source.size());
        for (String vertexId : source) {
            if (vertexId == null || vertexId.trim().isEmpty()) {
                throw new IllegalArgumentException("vertexIds must not contain blank id");
            }
            normalized.add(vertexId.trim());
        }
        return Collections.unmodifiableList(normalized);
    }

    private static List<Segment> normalizeSegments(List<Segment> source) {
        if (source == null) {
            throw new IllegalArgumentException("segments must not be null");
        }
        List<Segment> normalized = new ArrayList<Segment>(source.size());
        for (Segment segment : source) {
            normalized.add(Objects.requireNonNull(segment, "segments must not contain null"));
        }
        return Collections.unmodifiableList(normalized);
    }

    public static final class Segment {
        private final int index;
        private final String fromVertexId;
        private final String toVertexId;
        private final Color strokeColor;
        private final float strokeWidth;
        private final boolean dashed;

        public Segment(
                int index,
                String fromVertexId,
                String toVertexId,
                Color strokeColor,
                float strokeWidth,
                boolean dashed
        ) {
            if (index < 0) {
                throw new IllegalArgumentException("index must be >= 0");
            }
            if (fromVertexId == null || fromVertexId.trim().isEmpty()) {
                throw new IllegalArgumentException("fromVertexId must not be blank");
            }
            if (toVertexId == null || toVertexId.trim().isEmpty()) {
                throw new IllegalArgumentException("toVertexId must not be blank");
            }
            if (fromVertexId.trim().equals(toVertexId.trim())) {
                throw new IllegalArgumentException("segment vertices must be different");
            }
            if (Float.isNaN(strokeWidth) || Float.isInfinite(strokeWidth) || strokeWidth <= 0f) {
                throw new IllegalArgumentException("strokeWidth must be finite and > 0");
            }
            this.index = index;
            this.fromVertexId = fromVertexId.trim();
            this.toVertexId = toVertexId.trim();
            this.strokeColor = Objects.requireNonNull(strokeColor, "strokeColor must not be null");
            this.strokeWidth = strokeWidth;
            this.dashed = dashed;
        }

        public int getIndex() {
            return index;
        }

        public String getFromVertexId() {
            return fromVertexId;
        }

        public String getToVertexId() {
            return toVertexId;
        }

        public Color getStrokeColor() {
            return strokeColor;
        }

        public float getStrokeWidth() {
            return strokeWidth;
        }

        public boolean isDashed() {
            return dashed;
        }
    }
}
