package com.zhixing.navigation.gui.workbench;

import com.zhixing.navigation.domain.model.PlaceType;
import com.zhixing.navigation.domain.model.RoadType;

import java.awt.Color;

final class MapConfig {
    static final double MIN_ZOOM = 0.50;
    static final double MAX_ZOOM = 4.0;
    static final double ZOOM_STEP = 1.12;
    static final int VIEW_PADDING = 40;
    static final int WORLD_PADDING = 20;
    static final double GRID_SNAP_SIZE = 20.0;
    static final int GRID_SNAP_PIXELS = 10;
    static final int SNAP_VERTEX_PIXELS = 14;
    static final int AXIS_ALIGN_PIXELS = 10;
    static final double MOVE_HIT_THRESHOLD_PIXELS = 20.0;
    static final int PAN_VISIBLE_MIN_PIXELS = 36;
    static final double PAN_OVERSCROLL_FACTOR = 0.35;
    static final int FLASH_CYCLE_LIMIT = 8;
    static final int FLASH_INTERVAL_MS = 180;

    private MapConfig() {
    }

    static Color withOpacity(Color color, float opacity) {
        int alpha = Math.round(255 * Math.max(0.0f, Math.min(1.0f, opacity)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    static Color roadColor(RoadType roadType) {
        if (roadType == RoadType.MAIN_ROAD) {
            return new Color(46, 95, 165);
        }
        if (roadType == RoadType.STAIRS) {
            return new Color(115, 94, 164);
        }
        return new Color(128, 140, 154);
    }

    static float roadWidth(RoadType roadType) {
        if (roadType == RoadType.MAIN_ROAD) {
            return 4.8f;
        }
        if (roadType == RoadType.STAIRS) {
            return 3.4f;
        }
        return 2.9f;
    }

    static Color placeColor(PlaceType placeType) {
        if (placeType == PlaceType.GATE) {
            return new Color(40, 118, 209);
        }
        if (placeType == PlaceType.LIBRARY) {
            return new Color(31, 154, 111);
        }
        if (placeType == PlaceType.CANTEEN) {
            return new Color(219, 150, 53);
        }
        if (placeType == PlaceType.TEACHING_BUILDING) {
            return new Color(84, 104, 207);
        }
        if (placeType == PlaceType.DORMITORY) {
            return new Color(49, 150, 178);
        }
        if (placeType == PlaceType.OFFICE) {
            return new Color(204, 104, 88);
        }
        if (placeType == PlaceType.SPORTS_CENTER) {
            return new Color(66, 169, 99);
        }
        return new Color(116, 128, 144);
    }
}
