package com.zhixing.navigation.application.map;

import com.zhixing.navigation.application.auth.AuthorizationException;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.NormalUser;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.User;
import com.zhixing.navigation.domain.model.Vertex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class MapServiceTest {
    private final User admin = new Admin("admin", "sha256$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    private final User normalUser = new NormalUser("guest", "sha256$bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

    @Test
    void shouldManageVertexCrudAndFilterByType() {
        MapService service = new MapService(new CampusGraph());
        Vertex gate = new Vertex("G1", "East Gate", PlaceType.GATE, 0, 0, "");
        Vertex library = new Vertex("L1", "Library", PlaceType.LIBRARY, 10, 10, "");
        Vertex dorm = new Vertex("D1", "Dorm A", PlaceType.DORMITORY, 20, 20, "");

        service.addVertex(admin, gate);
        service.addVertex(admin, library);
        service.addVertex(admin, dorm);

        Assertions.assertEquals(3, service.listVertices().size());
        List<Vertex> libraries = service.listVerticesByType(PlaceType.LIBRARY);
        Assertions.assertEquals(1, libraries.size());
        Assertions.assertEquals("L1", libraries.get(0).getId());

        service.updateVertex(admin, new Vertex("L1", "Main Library", PlaceType.LIBRARY, 11, 11, "updated"));
        Assertions.assertEquals("Main Library", service.getVertex("L1").getName());

        service.deleteVertex(admin, "D1");
        Assertions.assertEquals(2, service.listVertices().size());
    }

    @Test
    void shouldManageRoadCrud() {
        MapService service = createServiceWithVertices();

        service.addRoad(admin, "A", "B", 100, false, false, RoadType.MAIN_ROAD);
        Assertions.assertNotNull(service.getRoad("A", "B"));
        Assertions.assertNotNull(service.getRoad("B", "A"));

        service.updateRoad(admin, "A", "B", 120, true, false, RoadType.PATH);
        Edge updated = service.getRoad("A", "B");
        Assertions.assertNotNull(updated);
        Assertions.assertEquals(120, updated.getWeight());
        Assertions.assertTrue(updated.isOneWay());
        Assertions.assertNull(service.getRoad("B", "A"));

        service.deleteRoad(admin, "A", "B");
        Assertions.assertNull(service.getRoad("A", "B"));
    }

    @Test
    void shouldSetRoadForbiddenAndEnableAgain() {
        MapService service = createServiceWithVertices();
        service.addRoad(admin, "A", "B", 80, false, false, RoadType.MAIN_ROAD);

        service.disableRoad(admin, "A", "B");
        Assertions.assertTrue(service.getRoad("A", "B").isForbidden());
        Assertions.assertTrue(service.getRoad("B", "A").isForbidden());

        service.enableRoad(admin, "A", "B");
        Assertions.assertFalse(service.getRoad("A", "B").isForbidden());
        Assertions.assertFalse(service.getRoad("B", "A").isForbidden());
    }

    @Test
    void shouldRejectDuplicateRoadOnAdd() {
        MapService service = createServiceWithVertices();
        service.addRoad(admin, "A", "B", 80, true, false, RoadType.MAIN_ROAD);

        IllegalArgumentException ex = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.addRoad(admin, "A", "B", 90, true, false, RoadType.PATH)
        );
        Assertions.assertTrue(ex.getMessage().contains("road already exists"));
    }

    @Test
    void shouldRejectMapModificationByNormalUser() {
        MapService service = createServiceWithVertices();
        Vertex newPlace = new Vertex("X", "Visitor Center", PlaceType.OTHER, 30, 30, "");

        Assertions.assertThrows(AuthorizationException.class, () -> service.addVertex(normalUser, newPlace));
    }

    private MapService createServiceWithVertices() {
        MapService service = new MapService(new CampusGraph());
        service.addVertex(admin, new Vertex("A", "Gate", PlaceType.GATE, 0, 0, ""));
        service.addVertex(admin, new Vertex("B", "Library", PlaceType.LIBRARY, 10, 10, ""));
        service.addVertex(admin, new Vertex("C", "Teaching Building", PlaceType.TEACHING_BUILDING, 20, 20, ""));
        return service;
    }
}
