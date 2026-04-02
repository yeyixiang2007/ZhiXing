package com.zhixing.navigation.gui.model;

import com.zhixing.navigation.domain.model.Vertex;

public final class VertexOption {
    private final String id;
    private final String name;

    public VertexOption(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static VertexOption fromVertex(Vertex vertex) {
        return new VertexOption(vertex.getId(), vertex.getName());
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id + " - " + name;
    }
}
