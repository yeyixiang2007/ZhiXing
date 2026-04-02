package com.zhixing.navigation.application.navigation;

import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.planning.DijkstraStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NavigationServiceTest {

    @Test
    void shouldGenerateInstructionsAndEstimatedTime() {
        CampusGraph graph = createGraph();
        NavigationService service = new NavigationService(new DijkstraStrategy(), 100.0);

        PathResult result = service.navigate(graph, "A", "C");

        Assertions.assertEquals(12.0, result.getTotalDistance());
        Assertions.assertEquals(0.12, result.getEstimatedTime(), 0.000001);
        Assertions.assertEquals(2, result.getSegmentDistances().size());
        Assertions.assertEquals(3, result.getNaviInstructions().size());
        Assertions.assertTrue(result.getNaviInstructions().get(0).contains("第1步"));
        Assertions.assertTrue(result.getNaviInstructions().get(2).contains("已到达目的地"));
    }

    @Test
    void shouldHandleStartEqualsEnd() {
        CampusGraph graph = createGraph();
        NavigationService service = new NavigationService(new DijkstraStrategy());

        PathResult result = service.navigate(graph, "A", "A");

        Assertions.assertEquals(0.0, result.getTotalDistance());
        Assertions.assertEquals(0.0, result.getEstimatedTime(), 0.000001);
        Assertions.assertTrue(result.getSegmentDistances().isEmpty());
        Assertions.assertEquals(1, result.getNaviInstructions().size());
    }

    @Test
    void shouldRejectInvalidWalkingSpeed() {
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new NavigationService(new DijkstraStrategy(), 0.0)
        );
        Assertions.assertTrue(ex.getMessage().contains("walkingSpeedMetersPerMinute"));
    }

    private CampusGraph createGraph() {
        CampusGraph graph = new CampusGraph();
        Vertex a = new Vertex("A", "East Gate", PlaceType.GATE, 0, 0, "");
        Vertex b = new Vertex("B", "Library", PlaceType.LIBRARY, 1, 1, "");
        Vertex c = new Vertex("C", "Teaching Building A", PlaceType.TEACHING_BUILDING, 2, 2, "");

        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addEdge(new Edge(a, b, 5, false, false, RoadType.MAIN_ROAD));
        graph.addEdge(new Edge(b, c, 7, false, false, RoadType.PATH));
        return graph;
    }
}

