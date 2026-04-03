package com.zhixing.navigation.gui.workbench.state;

import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.gui.model.RouteVisualizationDto;
import com.zhixing.navigation.gui.routing.AppRoute;
import com.zhixing.navigation.gui.workbench.EditToolMode;

import java.util.ArrayList;
import java.util.List;

public class MapViewState {
    private AppRoute activeRoute;
    private PathResult currentPathResult;
    private RouteVisualizationDto currentRouteVisualization;
    private RouteVisualizationDto previousRouteVisualization;
    private List<String> selectedVertexIds;
    private String selectedEdgeKey;
    private String pendingEdgeStartVertexId;
    private EditToolMode activeEditToolMode;

    public MapViewState() {
        this.activeRoute = AppRoute.USER_MODE;
        this.currentPathResult = null;
        this.currentRouteVisualization = null;
        this.previousRouteVisualization = null;
        this.selectedVertexIds = new ArrayList<String>();
        this.selectedEdgeKey = null;
        this.pendingEdgeStartVertexId = null;
        this.activeEditToolMode = EditToolMode.SELECT;
    }

    public AppRoute getActiveRoute() {
        return activeRoute;
    }

    public void setActiveRoute(AppRoute activeRoute) {
        this.activeRoute = activeRoute == null ? AppRoute.USER_MODE : activeRoute;
    }

    public PathResult getCurrentPathResult() {
        return currentPathResult;
    }

    public void setCurrentPathResult(PathResult currentPathResult) {
        this.currentPathResult = currentPathResult;
    }

    public RouteVisualizationDto getCurrentRouteVisualization() {
        return currentRouteVisualization;
    }

    public void setCurrentRouteVisualization(RouteVisualizationDto currentRouteVisualization) {
        this.currentRouteVisualization = currentRouteVisualization;
    }

    public RouteVisualizationDto getPreviousRouteVisualization() {
        return previousRouteVisualization;
    }

    public void setPreviousRouteVisualization(RouteVisualizationDto previousRouteVisualization) {
        this.previousRouteVisualization = previousRouteVisualization;
    }

    public List<String> getSelectedVertexIds() {
        return new ArrayList<String>(selectedVertexIds);
    }

    public void setSelectedVertexIds(List<String> selectedVertexIds) {
        this.selectedVertexIds = selectedVertexIds == null
                ? new ArrayList<String>()
                : new ArrayList<String>(selectedVertexIds);
    }

    public String getSelectedEdgeKey() {
        return selectedEdgeKey;
    }

    public void setSelectedEdgeKey(String selectedEdgeKey) {
        this.selectedEdgeKey = selectedEdgeKey;
    }

    public String getPendingEdgeStartVertexId() {
        return pendingEdgeStartVertexId;
    }

    public void setPendingEdgeStartVertexId(String pendingEdgeStartVertexId) {
        this.pendingEdgeStartVertexId = pendingEdgeStartVertexId;
    }

    public EditToolMode getActiveEditToolMode() {
        return activeEditToolMode;
    }

    public void setActiveEditToolMode(EditToolMode activeEditToolMode) {
        this.activeEditToolMode = activeEditToolMode == null ? EditToolMode.SELECT : activeEditToolMode;
    }
}
