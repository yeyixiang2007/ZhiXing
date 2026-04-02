package com.zhixing.navigation.gui.controller;

import com.zhixing.navigation.application.map.MapService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

class MapControllerTest {

    @Test
    void shouldRollbackGraphWhenSaveFailed() throws Exception {
        CampusGraph graph = new CampusGraph();
        graph.addVertex(new Vertex("A", "East Gate", PlaceType.GATE, 0, 0, ""));

        Path tempDir = Files.createTempDirectory("map-controller-save-fail");
        MapController controller = new MapController(
                graph,
                new MapService(graph),
                new FailingPersistenceService(tempDir)
        );
        Admin admin = new Admin("admin", "hash");

        IllegalStateException ex = Assertions.assertThrows(
                IllegalStateException.class,
                () -> controller.addVertex(admin, "B", "Library", PlaceType.LIBRARY, 1, 1, "")
        );

        Assertions.assertTrue(ex.getMessage().contains("save failed"));
        Assertions.assertTrue(graph.containsVertex("A"));
        Assertions.assertFalse(graph.containsVertex("B"));
        Assertions.assertEquals(1, graph.vertexCount());
    }

    @Test
    void shouldAutoSaveAfterValidEdit() throws Exception {
        CampusGraph graph = new CampusGraph();
        graph.addVertex(new Vertex("A", "East Gate", PlaceType.GATE, 0, 0, ""));

        Path tempDir = Files.createTempDirectory("map-controller-save-ok");
        MapController controller = new MapController(
                graph,
                new MapService(graph),
                new PersistenceService(tempDir)
        );
        Admin admin = new Admin("admin", "hash");

        controller.addVertex(admin, "B", "Library", PlaceType.LIBRARY, 1, 1, "");

        Assertions.assertTrue(graph.containsVertex("B"));
        Assertions.assertEquals(2, graph.vertexCount());
    }

    private static final class FailingPersistenceService extends PersistenceService {
        private FailingPersistenceService(Path dataDir) {
            super(dataDir);
        }

        @Override
        public void saveGraph(CampusGraph graph) {
            throw new IllegalStateException("save failed");
        }
    }
}
