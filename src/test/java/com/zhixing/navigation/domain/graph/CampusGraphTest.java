package com.zhixing.navigation.domain.graph;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CampusGraphTest {

    @Test
    void shouldAddTwoWayEdgeAsTwoDirections() {
        CampusGraph graph = new CampusGraph();
        Vertex a = new Vertex("A", "A", PlaceType.OTHER, 0, 0, "");
        Vertex b = new Vertex("B", "B", PlaceType.OTHER, 10, 10, "");

        graph.addVertex(a);
        graph.addVertex(b);
        graph.addEdge(new Edge(a, b, 100, false, false, RoadType.MAIN_ROAD));

        Assertions.assertEquals(1, graph.getNeighbors("A").size());
        Assertions.assertEquals(1, graph.getNeighbors("B").size());
        Assertions.assertEquals(2, graph.edgeCount());
    }

    @Test
    void shouldUpdateForbiddenState() {
        CampusGraph graph = new CampusGraph();
        Vertex a = new Vertex("A", "A", PlaceType.OTHER, 0, 0, "");
        Vertex b = new Vertex("B", "B", PlaceType.OTHER, 10, 10, "");

        graph.addVertex(a);
        graph.addVertex(b);
        graph.addEdge(new Edge(a, b, 100, false, false, RoadType.MAIN_ROAD));

        graph.setEdgeForbidden("A", "B", true);

        Assertions.assertTrue(graph.getNeighbors("A").get(0).isForbidden());
        Assertions.assertTrue(graph.getNeighbors("B").get(0).isForbidden());
    }

    @Test
    void shouldRejectDuplicateVertexId() {
        CampusGraph graph = new CampusGraph();
        graph.addVertex(new Vertex("A", "North Gate", PlaceType.GATE, 0, 0, ""));

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> graph.addVertex(new Vertex("A", "South Gate", PlaceType.GATE, 5, 5, ""))
        );

        Assertions.assertTrue(ex.getMessage().contains("vertex already exists"));
    }
}
