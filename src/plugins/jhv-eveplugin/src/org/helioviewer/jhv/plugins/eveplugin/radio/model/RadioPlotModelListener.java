package org.helioviewer.jhv.plugins.eveplugin.radio.model;

import java.awt.image.BufferedImage;

public interface RadioPlotModelListener {
    public abstract void drawBufferedImage(BufferedImage image, DrawableAreaMap map);

    public abstract void removeDownloadRequestData();

    public abstract void changeVisibility();
}
