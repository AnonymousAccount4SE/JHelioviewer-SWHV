package org.helioviewer.jhv.camera;

import java.awt.Dimension;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;

public class GL3DViewport {

    private int w;
    private int h;
    private int x;
    private int y;
    private final int idx;
    private boolean isVisible = true;
    private GL3DCamera camera;
    private final boolean slave;
    private final boolean movieExport;

    public GL3DViewport(int _idx, int _x, int _y, int _w, int _h, GL3DCamera _camera, boolean _slave) {
        this(_idx, _x, _y, _w, _h, _camera, _slave, false, false);

    }

    private boolean active;

    public GL3DViewport(int _idx, int _x, int _y, int _w, int _h, GL3DCamera _camera, boolean _slave, boolean _movieExport, boolean _active) {

        idx = _idx;
        w = _w;
        h = _h;
        x = _x;
        y = _y;
        camera = _camera;
        slave = _slave;
        movieExport = _movieExport;
        active = _active;
    }

    public GL3DCamera getCamera() {
        return camera;
    }

    public void setCamera(GL3DCamera _camera) {
        camera = _camera;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public int getOffsetX() {
        if (slave)
            return x;
        return 0;
    }

    public int getOffsetY() {
        if (slave)
            return Displayer.getGLHeight() - h - y;
        return 0;
    }

    public Dimension getSize() {
        return new Dimension(w, h);
    }

    public void setSize(int _x, int _y, int _w, int _h) {
        w = _w;
        h = _h;
        x = _x;
        y = _y;
    }

    public void setSize(int _w, int _h) {
        w = _w;
        h = _h;
    }

    public void setOffset(int offsetX, int offsetY) {
        x = offsetX;
        y = offsetY;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean _isVisible) {
        isVisible = _isVisible;
    }

    @Override
    public String toString() {
        return "Offset: " + this.getOffsetX() + "," + this.getOffsetY() + " Size: " + this.getWidth() + "," + this.getHeight();
    }

    public int getIndex() {
        return idx;
    }

    public boolean isActive() {
        return active;
    }

    public void computeActive() {
        active = ImageViewerGui.getRenderableContainer().isViewportActive(idx);
    }
}
