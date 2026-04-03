package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.Edge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MapCanvasEdgeIndex {
    private MapCanvasEdgeIndex() {
    }

    static List<Edge> dedupeForRender(Collection<Edge> edges) {
        Map<String, Edge> deduped = new LinkedHashMap<String, Edge>();
        for (Edge edge : edges) {
            String key = edgeKey(edge);
            Edge existing = deduped.get(key);
            if (existing == null || (!existing.isForbidden() && edge.isForbidden())) {
                deduped.put(key, edge);
            }
        }
        return new ArrayList<Edge>(deduped.values());
    }

    static String edgeKey(Edge edge) {
        String from = edge.getFromVertex().getId();
        String to = edge.getToVertex().getId();
        if (edge.isOneWay()) {
            return "ONE:" + from + "->" + to;
        }
        if (from.compareTo(to) <= 0) {
            return "TWO:" + from + "<->" + to;
        }
        return "TWO:" + to + "<->" + from;
    }
}
