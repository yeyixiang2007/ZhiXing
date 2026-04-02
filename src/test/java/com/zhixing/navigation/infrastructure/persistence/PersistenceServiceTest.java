package com.zhixing.navigation.infrastructure.persistence;

import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.NormalUser;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.User;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.security.PasswordHasher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class PersistenceServiceTest {

    @Test
    void shouldSaveAndLoadGraphAndUsers() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-persistence-test");
        PersistenceService service = new PersistenceService(tempDir);

        CampusGraph graph = createGraph();
        List<User> users = createUsers();

        service.saveGraph(graph);
        service.saveUsers(users);

        CampusGraph loadedGraph = service.loadGraph();
        List<User> loadedUsers = service.loadUsers();

        Assertions.assertEquals(graph.vertexCount(), loadedGraph.vertexCount());
        Assertions.assertEquals(graph.edgeCount(), loadedGraph.edgeCount());
        Assertions.assertEquals(2, loadedUsers.size());
        Assertions.assertEquals("admin", loadedUsers.get(0).getUsername());
    }

    @Test
    void shouldFallbackToDefaultWhenFilesMissing() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-persistence-missing");
        PersistenceService service = new PersistenceService(tempDir);

        CampusGraph graph = service.loadGraphOrDefault();
        List<User> users = service.loadUsersOrDefault();

        Assertions.assertTrue(graph.vertexCount() > 0);
        Assertions.assertTrue(users.size() > 0);
        Assertions.assertTrue(Files.exists(tempDir.resolve("vertex.json")));
        Assertions.assertTrue(Files.exists(tempDir.resolve("edge.json")));
        Assertions.assertTrue(Files.exists(tempDir.resolve("user.json")));
    }

    @Test
    void shouldFallbackToDefaultWhenJsonCorrupted() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-persistence-corrupted");
        Files.createDirectories(tempDir);
        Files.write(tempDir.resolve("vertex.json"), "{not-valid-json".getBytes(StandardCharsets.UTF_8));
        Files.write(tempDir.resolve("edge.json"), "[]".getBytes(StandardCharsets.UTF_8));

        PersistenceService service = new PersistenceService(tempDir);
        CampusGraph graph = service.loadGraphOrDefault();

        Assertions.assertTrue(graph.vertexCount() > 0);
        String repairedVertex = new String(Files.readAllBytes(tempDir.resolve("vertex.json")), StandardCharsets.UTF_8);
        Assertions.assertTrue(repairedVertex.trim().startsWith("["));
    }

    @Test
    void shouldBackupAndRestoreDataFiles() throws IOException {
        Path tempDir = Files.createTempDirectory("zhixing-persistence-backup");
        PersistenceService service = new PersistenceService(tempDir);

        service.saveGraph(createGraph());
        service.saveUsers(createUsers());
        service.backupData("v1");

        CampusGraph modified = new CampusGraph();
        Vertex x = new Vertex("X", "X", PlaceType.OTHER, 0, 0, "");
        Vertex y = new Vertex("Y", "Y", PlaceType.OTHER, 1, 1, "");
        modified.addVertex(x);
        modified.addVertex(y);
        modified.addEdge(new Edge(x, y, 50, true, false, RoadType.PATH));
        service.saveGraph(modified);
        service.saveUsers(new ArrayList<User>());

        service.restoreData("v1");

        CampusGraph restoredGraph = service.loadGraph();
        List<User> restoredUsers = service.loadUsers();
        Assertions.assertEquals(3, restoredGraph.vertexCount());
        Assertions.assertEquals(3, restoredGraph.edgeCount());
        Assertions.assertEquals(2, restoredUsers.size());
    }

    private CampusGraph createGraph() {
        CampusGraph graph = new CampusGraph();
        Vertex a = new Vertex("A", "Gate", PlaceType.GATE, 0, 0, "");
        Vertex b = new Vertex("B", "Library", PlaceType.LIBRARY, 10, 10, "");
        Vertex c = new Vertex("C", "Teaching", PlaceType.TEACHING_BUILDING, 20, 20, "");
        graph.addVertex(a);
        graph.addVertex(b);
        graph.addVertex(c);
        graph.addEdge(new Edge(a, b, 100, false, false, RoadType.MAIN_ROAD));
        graph.addEdge(new Edge(b, c, 120, true, false, RoadType.PATH));
        return graph;
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<User>();
        users.add(new Admin("admin", PasswordHasher.hash("admin123")));
        users.add(new NormalUser("guest", PasswordHasher.hash("guest123")));
        return users;
    }
}
