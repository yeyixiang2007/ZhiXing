package com.zhixing.navigation.domain.model;

import java.util.Objects;

public final class Vertex {
    private final String id;
    private final String name;
    private final PlaceType type;
    private final double x;
    private final double y;
    private final String description;

    public Vertex(String id, String name, PlaceType type, double x, double y, String description) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.x = requireFinite(x, "x");
        this.y = requireFinite(y, "y");
        this.description = description == null ? "" : description.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PlaceType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String getDescription() {
        return description;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    private static double requireFinite(double value, String fieldName) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Vertex)) {
            return false;
        }
        Vertex other = (Vertex) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Vertex{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
