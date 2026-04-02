package com.zhixing.navigation.app;

import com.zhixing.navigation.application.auth.AuthService;
import com.zhixing.navigation.application.map.MapService;
import com.zhixing.navigation.application.navigation.ConsolePathFormatter;
import com.zhixing.navigation.application.navigation.NavigationService;
import com.zhixing.navigation.domain.graph.CampusGraph;
import com.zhixing.navigation.domain.planning.DijkstraStrategy;
import com.zhixing.navigation.domain.planning.PathPlanningStrategy;
import com.zhixing.navigation.infrastructure.persistence.PersistenceService;

import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String dataDir = resolveDataDir();
        PersistenceService persistenceService = new PersistenceService(Paths.get(dataDir));
        CampusGraph graph = persistenceService.loadGraphOrDefault();
        persistenceService.loadUsersOrDefault();

        PathPlanningStrategy planner = new DijkstraStrategy();
        NavigationService navigationService = new NavigationService(planner);
        ConsolePathFormatter formatter = new ConsolePathFormatter();
        AuthService authService = new AuthService(persistenceService);
        MapService mapService = new MapService(graph);

        Scanner scanner = new Scanner(System.in);
        try {
            ConsoleApplication app = new ConsoleApplication(
                    graph,
                    persistenceService,
                    authService,
                    mapService,
                    navigationService,
                    formatter,
                    scanner
            );
            app.run();
        } finally {
            scanner.close();
        }
    }

    private static String resolveDataDir() {
        String envValue = System.getenv("ZHIXING_DATA_DIR");
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        return "data";
    }
}
