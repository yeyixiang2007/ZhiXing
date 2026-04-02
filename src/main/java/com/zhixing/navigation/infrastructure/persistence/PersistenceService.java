package com.zhixing.navigation.infrastructure.persistence;

import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.model.Admin;
import com.zhixing.navigation.domain.model.Edge;
import com.zhixing.navigation.domain.model.NormalUser;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.Role;
import com.zhixing.navigation.domain.model.RoadType;
import com.zhixing.navigation.domain.model.User;
import com.zhixing.navigation.domain.model.Vertex;
import com.zhixing.navigation.domain.security.PasswordHasher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PersistenceService {
    private final Path dataDir;
    private final Path backupDir;
    private final Path vertexFile;
    private final Path edgeFile;
    private final Path userFile;

    public PersistenceService(Path dataDir) {
        if (dataDir == null) {
            throw new IllegalArgumentException("dataDir must not be null");
        }
        this.dataDir = dataDir.toAbsolutePath().normalize();
        this.backupDir = this.dataDir.resolve("backup");
        this.vertexFile = this.dataDir.resolve("vertex.json");
        this.edgeFile = this.dataDir.resolve("edge.json");
        this.userFile = this.dataDir.resolve("user.json");
    }

    public CampusGraph loadGraph() {
        List<Map<String, Object>> vertexRecords = readArrayFile(vertexFile);
        List<Map<String, Object>> edgeRecords = readArrayFile(edgeFile);

        CampusGraph graph = new CampusGraph();
        for (Map<String, Object> row : vertexRecords) {
            Vertex vertex = mapVertex(row);
            graph.addVertex(vertex);
        }
        for (Map<String, Object> row : edgeRecords) {
            String fromId = requiredString(row, "fromId");
            String toId = requiredString(row, "toId");
            if (!graph.containsVertex(fromId) || !graph.containsVertex(toId)) {
                throw new IllegalStateException("Edge references unknown vertices: " + fromId + " -> " + toId);
            }
            Edge edge = new Edge(
                    graph.getVertex(fromId),
                    graph.getVertex(toId),
                    requiredDouble(row, "weight"),
                    requiredBoolean(row, "oneWay"),
                    requiredBoolean(row, "forbidden"),
                    RoadType.valueOf(requiredString(row, "roadType"))
            );
            graph.addEdge(edge);
        }
        return graph;
    }

    public List<User> loadUsers() {
        List<Map<String, Object>> rows = readArrayFile(userFile);
        List<User> users = new ArrayList<User>();
        for (Map<String, Object> row : rows) {
            String username = requiredString(row, "username");
            String passwordHash = requiredString(row, "passwordHash");
            if (!PasswordHasher.isEncodedHash(passwordHash)) {
                throw new IllegalArgumentException("passwordHash must be encrypted with sha256 format");
            }
            Role role = Role.valueOf(requiredString(row, "role"));
            if (role == Role.ADMIN) {
                users.add(new Admin(username, passwordHash));
            } else {
                users.add(new NormalUser(username, passwordHash));
            }
        }
        return users;
    }

    public CampusGraph loadGraphOrDefault() {
        try {
            return loadGraph();
        } catch (RuntimeException ex) {
            CampusGraph defaults = DefaultDataFactory.defaultGraph();
            saveGraph(defaults);
            return defaults;
        }
    }

    public List<User> loadUsersOrDefault() {
        try {
            return loadUsers();
        } catch (RuntimeException ex) {
            List<User> defaults = DefaultDataFactory.defaultUsers();
            saveUsers(defaults);
            return defaults;
        }
    }

    public void saveGraph(CampusGraph graph) {
        Objects.requireNonNull(graph, "graph must not be null");
        ensureDataDir();

        List<Map<String, Object>> vertexRecords = new ArrayList<Map<String, Object>>();
        for (Vertex vertex : graph.getAllVertices()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("id", vertex.getId());
            row.put("name", vertex.getName());
            row.put("type", vertex.getType().name());
            row.put("x", vertex.getX());
            row.put("y", vertex.getY());
            row.put("description", vertex.getDescription());
            vertexRecords.add(row);
        }
        Collections.sort(vertexRecords, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                return String.valueOf(a.get("id")).compareTo(String.valueOf(b.get("id")));
            }
        });

        List<Map<String, Object>> edgeRecords = new ArrayList<Map<String, Object>>();
        for (Edge edge : graph.getAllEdges()) {
            if (!edge.isOneWay() && edge.getFromVertex().getId().compareTo(edge.getToVertex().getId()) > 0) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("fromId", edge.getFromVertex().getId());
            row.put("toId", edge.getToVertex().getId());
            row.put("weight", edge.getWeight());
            row.put("oneWay", edge.isOneWay());
            row.put("forbidden", edge.isForbidden());
            row.put("roadType", edge.getRoadType().name());
            edgeRecords.add(row);
        }
        Collections.sort(edgeRecords, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                String aKey = String.valueOf(a.get("fromId")) + "->" + String.valueOf(a.get("toId"));
                String bKey = String.valueOf(b.get("fromId")) + "->" + String.valueOf(b.get("toId"));
                return aKey.compareTo(bKey);
            }
        });

        writeArrayFile(vertexFile, vertexRecords);
        writeArrayFile(edgeFile, edgeRecords);
    }

    public void saveUsers(List<User> users) {
        Objects.requireNonNull(users, "users must not be null");
        ensureDataDir();

        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (User user : users) {
            if (!PasswordHasher.isEncodedHash(user.getPasswordHash())) {
                throw new IllegalArgumentException("passwordHash must be encrypted with sha256 format");
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("username", user.getUsername());
            row.put("passwordHash", user.getPasswordHash());
            row.put("role", user.getRole().name());
            rows.add(row);
        }
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> a, Map<String, Object> b) {
                return String.valueOf(a.get("username")).compareTo(String.valueOf(b.get("username")));
            }
        });
        writeArrayFile(userFile, rows);
    }

    public void backupData(String backupName) {
        String name = requireName(backupName, "backupName");
        ensureDataDir();
        Path targetDir = backupDir.resolve(name);
        try {
            Files.createDirectories(targetDir);
            copyIfExists(vertexFile, targetDir.resolve("vertex.json"));
            copyIfExists(edgeFile, targetDir.resolve("edge.json"));
            copyIfExists(userFile, targetDir.resolve("user.json"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to backup data", ex);
        }
    }

    public void restoreData(String backupName) {
        String name = requireName(backupName, "backupName");
        Path sourceDir = backupDir.resolve(name);
        if (!Files.isDirectory(sourceDir)) {
            throw new IllegalArgumentException("Backup not found: " + sourceDir);
        }
        ensureDataDir();
        try {
            copyRequired(sourceDir.resolve("vertex.json"), vertexFile);
            copyRequired(sourceDir.resolve("edge.json"), edgeFile);
            copyRequired(sourceDir.resolve("user.json"), userFile);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to restore backup", ex);
        }
    }

    public Path getDataDir() {
        return dataDir;
    }

    private List<Map<String, Object>> readArrayFile(Path file) {
        if (!Files.exists(file)) {
            throw new IllegalStateException("Data file not found: " + file);
        }
        try {
            String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            return SimpleJson.parseArrayOfObjects(content);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read data file: " + file, ex);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Invalid JSON format in file: " + file, ex);
        }
    }

    private void writeArrayFile(Path file, List<Map<String, Object>> records) {
        try {
            Files.write(file, SimpleJson.toJsonArray(records).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write data file: " + file, ex);
        }
    }

    private void ensureDataDir() {
        try {
            Files.createDirectories(dataDir);
            Files.createDirectories(backupDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create data directories", ex);
        }
    }

    private void copyIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void copyRequired(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IllegalStateException("Required backup file missing: " + source);
        }
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Vertex mapVertex(Map<String, Object> row) {
        return new Vertex(
                requiredString(row, "id"),
                requiredString(row, "name"),
                PlaceType.valueOf(requiredString(row, "type")),
                requiredDouble(row, "x"),
                requiredDouble(row, "y"),
                optionalString(row.get("description"))
        );
    }

    private static String requiredString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        String text = optionalString(value);
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Missing or blank field: " + key);
        }
        return text;
    }

    private static boolean requiredBoolean(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String s = ((String) value).trim().toLowerCase();
            if ("true".equals(s)) {
                return true;
            }
            if ("false".equals(s)) {
                return false;
            }
        }
        throw new IllegalArgumentException("Invalid boolean field: " + key);
    }

    private static double requiredDouble(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid numeric field: " + key);
            }
        }
        throw new IllegalArgumentException("Invalid numeric field: " + key);
    }

    private static String optionalString(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private static String requireName(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
