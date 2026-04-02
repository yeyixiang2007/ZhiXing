package com.zhixing.navigation.gui.model;

public final class OverviewData {
    private final int vertexCount;
    private final int roadCount;
    private final int forbiddenRoadCount;
    private final String dataDir;

    public OverviewData(int vertexCount, int roadCount, int forbiddenRoadCount, String dataDir) {
        this.vertexCount = vertexCount;
        this.roadCount = roadCount;
        this.forbiddenRoadCount = forbiddenRoadCount;
        this.dataDir = dataDir;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getRoadCount() {
        return roadCount;
    }

    public int getForbiddenRoadCount() {
        return forbiddenRoadCount;
    }

    public String getDataDir() {
        return dataDir;
    }
}
