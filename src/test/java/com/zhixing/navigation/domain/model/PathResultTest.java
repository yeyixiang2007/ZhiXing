package com.zhixing.navigation.domain.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

class PathResultTest {

    @Test
    void shouldBuildValidPathResult() {
        Vertex start = new Vertex("GATE_E", "East Gate", PlaceType.GATE, 0, 0, "");
        Vertex mid = new Vertex("LIB", "Library", PlaceType.LIBRARY, 10, 10, "");
        Vertex end = new Vertex("TB_A", "Teaching Building A", PlaceType.TEACHING_BUILDING, 20, 20, "");

        PathResult result = new PathResult(
                start,
                end,
                Arrays.asList(start, mid, end),
                380,
                5.2,
                Arrays.asList(200.0, 180.0),
                Arrays.asList("Go straight for 200m", "Turn left for 180m")
        );

        Assertions.assertEquals(3, result.getPathList().size());
        Assertions.assertEquals(380, result.getTotalDistance());
        Assertions.assertEquals(Arrays.asList(200.0, 180.0), result.getSegmentDistances());
    }

    @Test
    void shouldRejectPathNotStartingFromStartVertex() {
        Vertex start = new Vertex("A", "A", PlaceType.OTHER, 0, 0, "");
        Vertex end = new Vertex("B", "B", PlaceType.OTHER, 10, 10, "");
        Vertex mid = new Vertex("C", "C", PlaceType.OTHER, 20, 20, "");

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PathResult(start, end, Arrays.asList(mid, end), 10, 1, Collections.singletonList(10.0), Collections.singletonList("x"))
        );
        Assertions.assertTrue(ex.getMessage().contains("start"));
    }

    @Test
    void shouldRejectNegativeDistance() {
        Vertex start = new Vertex("A", "A", PlaceType.OTHER, 0, 0, "");
        Vertex end = new Vertex("B", "B", PlaceType.OTHER, 10, 10, "");

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PathResult(start, end, Arrays.asList(start, end), -1, 1, Collections.singletonList(1.0), Collections.singletonList("x"))
        );
        Assertions.assertTrue(ex.getMessage().contains("totalDistance"));
    }

    @Test
    void shouldRejectInvalidSegmentDistanceSize() {
        Vertex start = new Vertex("A", "A", PlaceType.OTHER, 0, 0, "");
        Vertex end = new Vertex("B", "B", PlaceType.OTHER, 10, 10, "");

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PathResult(
                        start,
                        end,
                        Arrays.asList(start, end),
                        10,
                        1,
                        Arrays.asList(1.0, 2.0),
                        Collections.singletonList("x")
                )
        );
        Assertions.assertTrue(ex.getMessage().contains("segmentDistances"));
    }
}
