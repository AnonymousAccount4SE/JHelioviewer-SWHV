package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.Rectangle;

public class SubImage {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    // minimum 1 pixel, fit into rectangle
    public SubImage(int x, int y, int w, int h, Rectangle r) {
        w = Math.min(Math.max(w, 1), r.width);
        h = Math.min(Math.max(h, 1), r.height);
        x = Math.min(Math.max(x, 0), r.width - 1);
        y = Math.min(Math.max(y, 0), r.height - 1);

        w = Math.min(w, r.width - x);
        h = Math.min(h, r.height - y);

        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SubImage) {
            SubImage s = (SubImage) o;
            return x == s.x && y == s.y && width == s.width && height == s.height;
        }
        return false;
    }

    @Override
    public String toString() {
        return "[x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
    }

}
