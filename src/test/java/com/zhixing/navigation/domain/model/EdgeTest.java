package com.zhixing.navigation.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EdgeTest {

    @Test
    void shouldRejectNonPositiveWeight() {
        Vertex a = new Vertex("A", "A", PlaceType.OTHER, 0, 0, "");
        Vertex b = new Vertex("B", "B", PlaceType.OTHER, 1, 1, "");

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Edge(a, b, 0, false, false, RoadType.MAIN_ROAD)
        );
        Assertions.assertTrue(ex.getMessage().contains("weight"));
    }

    @Test
    void shouldRejectSameStartAndEndVertex() {
        Vertex a = new Vertex("A", "A", PlaceType.OTHER, 0, 0, "");

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Edge(a, a, 100, false, false, RoadType.MAIN_ROAD)
        );
        Assertions.assertTrue(ex.getMessage().contains("different"));
    }
}

