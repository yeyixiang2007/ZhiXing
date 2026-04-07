package com.zhixing.navigation.gui.workbench;

import java.awt.image.BufferedImage;

final class MapSceneCache {
    private BufferedImage sceneImage;
    private boolean dirty;
    private int logicalWidth;
    private int logicalHeight;
    private double scaleX;
    private double scaleY;

    MapSceneCache() {
        this.sceneImage = null;
        this.dirty = true;
        this.logicalWidth = 0;
        this.logicalHeight = 0;
        this.scaleX = 1.0;
        this.scaleY = 1.0;
    }

    BufferedImage image() {
        return sceneImage;
    }

    double scaleX() {
        return scaleX;
    }

    double scaleY() {
        return scaleY;
    }

    void markDirty() {
        dirty = true;
    }

    boolean isDirty() {
        return dirty;
    }

    boolean ensureSize(int width, int height, double deviceScaleX, double deviceScaleY) {
        int safeWidth = Math.max(width, 1);
        int safeHeight = Math.max(height, 1);
        double safeScaleX = sanitizeScale(deviceScaleX);
        double safeScaleY = sanitizeScale(deviceScaleY);
        int pixelWidth = Math.max(1, (int) Math.ceil(safeWidth * safeScaleX));
        int pixelHeight = Math.max(1, (int) Math.ceil(safeHeight * safeScaleY));

        if (sceneImage != null
                && logicalWidth == safeWidth
                && logicalHeight == safeHeight
                && sceneImage.getWidth() == pixelWidth
                && sceneImage.getHeight() == pixelHeight
                && nearlyEquals(scaleX, safeScaleX)
                && nearlyEquals(scaleY, safeScaleY)) {
            return false;
        }

        logicalWidth = safeWidth;
        logicalHeight = safeHeight;
        scaleX = safeScaleX;
        scaleY = safeScaleY;
        sceneImage = new BufferedImage(pixelWidth, pixelHeight, BufferedImage.TYPE_INT_ARGB);
        dirty = true;
        return true;
    }

    void markClean() {
        dirty = false;
    }

    private static double sanitizeScale(double scale) {
        if (Double.isNaN(scale) || Double.isInfinite(scale) || scale <= 0) {
            return 1.0;
        }
        return scale;
    }

    private static boolean nearlyEquals(double left, double right) {
        return Math.abs(left - right) < 0.001;
    }
}
