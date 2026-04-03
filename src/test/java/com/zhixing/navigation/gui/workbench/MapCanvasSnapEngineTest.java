package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.Vertex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

class MapCanvasSnapEngineTest {

    @Test
    void shouldSnapToGridWhenPointerNearGridPoint() {
        List<Vertex> vertices = Arrays.asList(
                new Vertex("A", "A", PlaceType.GATE, 0, 0, "")
        );
        Function<Point2D.Double, Point2D.Double> worldProjector = point -> point == null ? null : new Point2D.Double(point.x, point.y);
        Function<Vertex, Point2D.Double> vertexProjector = vertex -> vertex == null ? null : new Point2D.Double(vertex.getX(), vertex.getY());

        MapCanvasSnapEngine.Outcome outcome = MapCanvasSnapEngine.apply(
                new Point2D.Double(18.5, 19.2),
                null,
                vertices,
                false,
                20.0,
                10,
                2,
                0.0,
                worldProjector,
                vertexProjector
        );

        Assertions.assertEquals(20.0, outcome.x, 0.0001);
        Assertions.assertEquals(20.0, outcome.y, 0.0001);
    }

    @Test
    void shouldSnapToNearestVertexWhenCloseEnough() {
        Vertex a = new Vertex("A", "A", PlaceType.GATE, 100, 100, "");
        Vertex b = new Vertex("B", "B", PlaceType.LIBRARY, 140, 130, "");
        List<Vertex> vertices = Arrays.asList(a, b);
        Function<Point2D.Double, Point2D.Double> worldProjector = point -> point == null ? null : new Point2D.Double(point.x, point.y);
        Function<Vertex, Point2D.Double> vertexProjector = vertex -> vertex == null ? null : new Point2D.Double(vertex.getX(), vertex.getY());

        MapCanvasSnapEngine.Outcome outcome = MapCanvasSnapEngine.apply(
                new Point2D.Double(102, 101),
                null,
                vertices,
                false,
                20.0,
                1,
                10,
                1.0,
                worldProjector,
                vertexProjector
        );

        Assertions.assertEquals(100.0, outcome.x, 0.0001);
        Assertions.assertEquals(100.0, outcome.y, 0.0001);
        Assertions.assertTrue(outcome.hasGuide());
    }

    @Test
    void shouldAlignAxisWhenWithinThresholdAndNotSnapVertex() {
        Vertex a = new Vertex("A", "A", PlaceType.GATE, 50, 80, "");
        Vertex b = new Vertex("B", "B", PlaceType.LIBRARY, 200, 130, "");
        List<Vertex> vertices = Arrays.asList(a, b);
        Function<Point2D.Double, Point2D.Double> worldProjector = point -> point == null ? null : new Point2D.Double(point.x, point.y);
        Function<Vertex, Point2D.Double> vertexProjector = vertex -> vertex == null ? null : new Point2D.Double(vertex.getX(), vertex.getY());

        MapCanvasSnapEngine.Outcome outcome = MapCanvasSnapEngine.apply(
                new Point2D.Double(52.5, 110),
                "B",
                vertices,
                false,
                20.0,
                1,
                1,
                3.0,
                worldProjector,
                vertexProjector
        );

        Assertions.assertEquals(50.0, outcome.x, 0.0001);
        Assertions.assertEquals(110.0, outcome.y, 0.0001);
        Assertions.assertTrue(outcome.verticalGuide);
        Assertions.assertFalse(outcome.horizontalGuide);
    }
}
