package com.zhixing.navigation.gui.controller;

import com.zhixing.navigation.application.navigation.ConsolePathFormatter;
import com.zhixing.navigation.application.navigation.NavigationService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.planning.DijkstraStrategy;
import com.zhixing.navigation.gui.model.RouteVisualizationDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;

class NavigationControllerTest {

    @Test
    void shouldBuildRouteVisualizationForCanvas() {
        CampusGraph graph = createGraph();
        NavigationController controller = createController(graph);

        NavigationController.NavigationVisualResult result = controller.queryPathVisual("A", "C");

        RouteVisualizationDto visualization = result.getRouteVisualization();
        Assertions.assertEquals(2, visualization.getSegmentCount());
        Assertions.assertEquals(3, visualization.getVertexIds().size());
        Assertions.assertTrue(visualization.isShowMarkers());
        Assertions.assertTrue(visualization.isShowStepBadges());
        Assertions.assertEquals(new Color(39, 174, 96), visualization.getStartMarkerColor());
        Assertions.assertEquals(new Color(231, 76, 60), visualization.getEndMarkerColor());

        RouteVisualizationDto.Segment first = visualization.getSegments().get(0);
        Assertions.assertEquals("A", first.getFromVertexId());
        Assertions.assertEquals("B", first.getToVertexId());
        Assertions.assertFalse(first.isDashed());
        Assertions.assertEquals(new Color(255, 170, 0), first.getStrokeColor());
        Assertions.assertEquals(5.2f, first.getStrokeWidth(), 0.0001f);
    }

    @Test
    void shouldBuildTraceVisualizationStyle() {
        CampusGraph graph = createGraph();
        NavigationController controller = createController(graph);
        PathResult path = controller.queryPath("A", "C");

        RouteVisualizationDto trace = controller.toTraceRouteVisualization(path);

        Assertions.assertFalse(trace.isShowMarkers());
        Assertions.assertFalse(trace.isShowStepBadges());
        Assertions.assertEquals(2, trace.getSegmentCount());
        RouteVisualizationDto.Segment first = trace.getSegments().get(0);
        Assertions.assertTrue(first.isDashed());
        Assertions.assertEquals(new Color(80, 130, 220, 110), first.getStrokeColor());
        Assertions.assertEquals(3.2f, first.getStrokeWidth(), 0.0001f);
    }

    private static NavigationController createController(CampusGraph graph) {
        NavigationService service = new NavigationService(new DijkstraStrategy());
        return new NavigationController(graph, service, new ConsolePathFormatter());
    }

    private static CampusGraph createGraph() {
        CampusGraph graph = new CampusGraph();
        Vertex a = new Vertex("A", "East Gate", PlaceType.GATE, 0, 0, "");
        Vertex b = new Vertex("B", "Library", PlaceType.LIBRARY, 1, 1, "");
        Vertex c = new Vertex("C", "Teaching Building", PlaceType.TEACHING_BUILDING, 2, 2, "");
        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addEdge(new Edge(a, b, 10.0, false, false, RoadType.MAIN_ROAD));
        graph.addEdge(new Edge(b, c, 8.0, false, false, RoadType.PATH));
        return graph;
    }
}
