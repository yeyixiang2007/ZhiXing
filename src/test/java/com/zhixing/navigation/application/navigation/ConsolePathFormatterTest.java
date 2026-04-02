package com.zhixing.navigation.application.navigation;

import com.zhixing.navigation.domain.model.PathResult;
import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.Vertex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class ConsolePathFormatterTest {

    @Test
    void shouldFormatPathResultWithUnifiedTemplate() {
        Vertex start = new Vertex("A", "East Gate", PlaceType.GATE, 0, 0, "");
        Vertex mid = new Vertex("B", "Library", PlaceType.LIBRARY, 1, 1, "");
        Vertex end = new Vertex("C", "Teaching Building A", PlaceType.TEACHING_BUILDING, 2, 2, "");

        PathResult result = new PathResult(
                start,
                end,
                Arrays.asList(start, mid, end),
                300.0,
                4.0,
                Arrays.asList(120.0, 180.0),
                Arrays.asList("第1步：从 East Gate 前往 Library，步行约 120 米。", "第2步：从 Library 前往 Teaching Building A，步行约 180 米。", "已到达目的地：Teaching Building A。")
        );

        ConsolePathFormatter formatter = new ConsolePathFormatter();
        String output = formatter.format(result);

        Assertions.assertTrue(output.contains("路径规划结果"));
        Assertions.assertTrue(output.contains("总距离：300.0 米"));
        Assertions.assertTrue(output.contains("预计步行耗时：4.0 分钟"));
        Assertions.assertTrue(output.contains("East Gate -> Library -> Teaching Building A"));
        Assertions.assertTrue(output.contains("分步导航"));
    }
}

