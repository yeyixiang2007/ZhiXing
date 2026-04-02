package com.zhixing.navigation.domain.planning;

import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class DijkstraStrategyTest {

    @Test
    void shouldReturnShortestPathAndSegmentDistances() {
        CampusGraph graph = createGraph();
        DijkstraStrategy strategy = new DijkstraStrategy();

        PathResult result = strategy.plan(graph, "A", "D");

        Assertions.assertEquals(10.0, result.getTotalDistance());
        Assertions.assertEquals(Arrays.asList(5.0, 5.0), result.getSegmentDistances());
        Assertions.assertEquals("A", result.getPathList().get(0).getId());
        Assertions.assertEquals("D", result.getPathList().get(result.getPathList().size() - 1).getId());
        Assertions.assertEquals(0.0, result.getEstimatedTime());
        Assertions.assertTrue(result.getNaviInstructions().isEmpty());
    }

    @Test
    void shouldSkipForbiddenEdges() {
        CampusGraph graph = createGraph();
        graph.setEdgeForbidden("B", "D", true);
        DijkstraStrategy strategy = new DijkstraStrategy();

        PathResult result = strategy.plan(graph, "A", "D");

        Assertions.assertEquals(23.0, result.getTotalDistance());
        Assertions.assertEquals(Arrays.asList(3.0, 20.0), result.getSegmentDistances());
        Assertions.assertEquals("C", result.getPathList().get(1).getId());
    }

    @Test
    void shouldThrowWhenNoRouteFound() {
        CampusGraph graph = createGraph();
        graph.setEdgeForbidden("B", "D", true);
        graph.setEdgeForbidden("C", "D", true);
        DijkstraStrategy strategy = new DijkstraStrategy();

        Assertions.assertThrows(NoRouteFoundException.class, () -> strategy.plan(graph, "A", "D"));
    }

    private CampusGraph createGraph() {
        CampusGraph graph = new CampusGraph();
        Vertex a = new Vertex("A", "A", PlaceType.OTHER, 0, 0, "");
        Vertex b = new Vertex("B", "B", PlaceType.OTHER, 1, 1, "");
        Vertex c = new Vertex("C", "C", PlaceType.OTHER, 2, 2, "");
        Vertex d = new Vertex("D", "D", PlaceType.OTHER, 3, 3, "");

        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addVertex(d);

        graph.addEdge(new Edge(a, b, 5, false, false, RoadType.MAIN_ROAD));
        graph.addEdge(new Edge(b, d, 5, false, false, RoadType.MAIN_ROAD));
        graph.addEdge(new Edge(a, c, 3, false, false, RoadType.PATH));
        graph.addEdge(new Edge(c, d, 20, false, false, RoadType.PATH));
        return graph;
    }
}
