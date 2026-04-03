package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.Vertex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class MapCanvasWorkbenchTest {

    @Test
    void shouldManageLayerVisibilityOpacityAndOrder() {
        MapCanvas canvas = new MapCanvas();
        canvas.setSize(1024, 768);

        List<Vertex> vertices = Arrays.asList(
                new Vertex("A", "A", PlaceType.GATE, 0, 0, ""),
                new Vertex("B", "B", PlaceType.LIBRARY, 100, 60, "")
        );
        List<Edge> edges = Arrays.asList(
                new Edge(vertices.get(0), vertices.get(1), 100, false, false, RoadType.MAIN_ROAD)
        );
        canvas.setGraphData(vertices, edges);

        Assertions.assertTrue(canvas.isLayerVisible(MapCanvas.Layer.LABEL));
        canvas.setLayerVisible(MapCanvas.Layer.LABEL, false);
        Assertions.assertFalse(canvas.isLayerVisible(MapCanvas.Layer.LABEL));

        canvas.setLayerOpacity(MapCanvas.Layer.ROAD, 0.03f);
        Assertions.assertEquals(0.15f, canvas.getLayerOpacity(MapCanvas.Layer.ROAD), 0.0001f);
        canvas.setLayerOpacity(MapCanvas.Layer.ROAD, 1.7f);
        Assertions.assertEquals(1.0f, canvas.getLayerOpacity(MapCanvas.Layer.ROAD), 0.0001f);

        List<MapCanvas.Layer> newOrder = Arrays.asList(
                MapCanvas.Layer.LABEL,
                MapCanvas.Layer.VERTEX,
                MapCanvas.Layer.ROAD,
                MapCanvas.Layer.FORBIDDEN
        );
        canvas.setRenderOrder(newOrder);
        Assertions.assertEquals(newOrder, canvas.getRenderOrder());

        canvas.resetViewport();
    }
}
