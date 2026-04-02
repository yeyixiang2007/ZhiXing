package com.zhixing.navigation.application.navigation;

import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.Vertex;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class ConsolePathFormatter {

    public String format(PathResult pathResult) {
        Objects.requireNonNull(pathResult, "pathResult must not be null");

        StringBuilder builder = new StringBuilder();
        builder.append("========== 路径规划结果 ==========").append(System.lineSeparator());
        builder.append("起点：").append(pathResult.getStartVertex().getName()).append(System.lineSeparator());
        builder.append("终点：").append(pathResult.getEndVertex().getName()).append(System.lineSeparator());
        builder.append("总距离：").append(String.format("%.1f", pathResult.getTotalDistance())).append(" 米").append(System.lineSeparator());
        builder.append("预计步行耗时：").append(String.format("%.1f", pathResult.getEstimatedTime())).append(" 分钟").append(System.lineSeparator());
        builder.append("途经点：").append(joinVertexNames(pathResult.getPathList())).append(System.lineSeparator());
        builder.append("----- 分段距离 -----").append(System.lineSeparator());
        appendSegmentDetails(builder, pathResult);
        builder.append("----- 分步导航 -----").append(System.lineSeparator());
        appendInstructions(builder, pathResult.getNaviInstructions());
        builder.append("==================================");
        return builder.toString();
    }

    private String joinVertexNames(List<Vertex> vertices) {
        StringJoiner joiner = new StringJoiner(" -> ");
        for (Vertex vertex : vertices) {
            joiner.add(vertex.getName());
        }
        return joiner.toString();
    }

    private void appendSegmentDetails(StringBuilder builder, PathResult pathResult) {
        List<Vertex> vertices = pathResult.getPathList();
        List<Double> distances = pathResult.getSegmentDistances();

        if (distances.isEmpty()) {
            builder.append("无分段距离（起点与终点相同）").append(System.lineSeparator());
            return;
        }

        for (int i = 1; i < vertices.size(); i++) {
            Vertex from = vertices.get(i - 1);
            Vertex to = vertices.get(i);
            double distance = distances.get(i - 1);
            builder.append(i)
                    .append(". ")
                    .append(from.getName())
                    .append(" -> ")
                    .append(to.getName())
                    .append("：")
                    .append(String.format("%.1f", distance))
                    .append(" 米")
                    .append(System.lineSeparator());
        }
    }

    private void appendInstructions(StringBuilder builder, List<String> instructions) {
        if (instructions.isEmpty()) {
            builder.append("无导航指引").append(System.lineSeparator());
            return;
        }

        for (int i = 0; i < instructions.size(); i++) {
            builder.append(i + 1)
                    .append(". ")
                    .append(instructions.get(i))
                    .append(System.lineSeparator());
        }
    }
}

