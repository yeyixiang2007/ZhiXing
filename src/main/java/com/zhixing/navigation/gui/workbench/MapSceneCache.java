package com.zhixing.navigation.gui.workbench;

import java.awt.image.BufferedImage;

final class MapSceneCache {
    private BufferedImage sceneImage;
    private boolean dirty;

    MapSceneCache() {
        this.sceneImage = null;
        this.dirty = true;
    }

    BufferedImage image() {
        return sceneImage;
    }

    void markDirty() {
        dirty = true;
    }

    boolean isDirty() {
        return dirty;
    }

    boolean ensureSize(int width, int height) {
        int safeWidth = Math.max(width, 1);
        int safeHeight = Math.max(height, 1);
        if (sceneImage != null && sceneImage.getWidth() == safeWidth && sceneImage.getHeight() == safeHeight) {
            return false;
        }
        sceneImage = new BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_ARGB);
        dirty = true;
        return true;
    }

    void markClean() {
        dirty = false;
    }
}
