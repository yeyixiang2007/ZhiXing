package com.zhixing.navigation.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VertexTest {

    @Test
    void shouldRejectBlankId() {
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Vertex("   ", "Library", PlaceType.LIBRARY, 1, 1, "")
        );
        Assertions.assertTrue(ex.getMessage().contains("id"));
    }

    @Test
    void shouldRejectNonFiniteCoordinate() {
        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new Vertex("LIB", "Library", PlaceType.LIBRARY, Double.NaN, 1, "")
        );
        Assertions.assertTrue(ex.getMessage().contains("x"));
    }
}

