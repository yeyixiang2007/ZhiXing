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

import java.util.ArrayList;
import java.util.List;

final class DefaultDataFactory {
    private DefaultDataFactory() {
    }

    static CampusGraph defaultGraph() {
        CampusGraph graph = new CampusGraph();

        Vertex eastGate = new Vertex("GATE_E", "East Gate", PlaceType.GATE, 0, 0, "Main entrance");
        Vertex library = new Vertex("LIB", "Library", PlaceType.LIBRARY, 180, 120, "Central library");
        Vertex teachingA = new Vertex("TB_A", "Teaching Building A", PlaceType.TEACHING_BUILDING, 320, 180, "Classrooms");
        Vertex canteen = new Vertex("CAN_2", "Second Canteen", PlaceType.CANTEEN, 420, 260, "Dining hall");

        graph.addVertex(eastGate);
        graph.addVertex(library);
        graph.addVertex(teachingA);
        graph.addVertex(canteen);

        graph.addEdge(new Edge(eastGate, library, 220, false, false, RoadType.MAIN_ROAD));
        graph.addEdge(new Edge(library, teachingA, 180, false, false, RoadType.PATH));
        graph.addEdge(new Edge(teachingA, canteen, 150, false, false, RoadType.PATH));
        graph.addEdge(new Edge(library, canteen, 260, true, false, RoadType.MAIN_ROAD));

        return graph;
    }

    static List<User> defaultUsers() {
        List<User> users = new ArrayList<User>();
        users.add(new Admin("admin", PasswordHasher.hash("admin123")));
        users.add(new NormalUser("guest", PasswordHasher.hash("guest123")));
        return users;
    }
}
