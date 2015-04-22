package org.helioviewer.plugins.eveplugin.lines.data;

import java.awt.Color;
import java.awt.Component;

import org.helioviewer.plugins.eveplugin.settings.BandType;
import org.helioviewer.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

/**
 *
 * @author Stephan Pagel
 * */
public class Band implements LineDataSelectorElement {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private final BandType bandType;

    private boolean isVisible = true;
    private Color graphColor = Color.BLACK;

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public Band(final BandType bandType) {
        this.bandType = bandType;
    }

    public final BandType getBandType() {
        return bandType;
    }

    public final String getTitle() {
        return bandType.getLabel();
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(final boolean visible) {
        isVisible = visible;
    }

    public void setGraphColor(final Color color) {
        graphColor = color;
    }

    public final Color getGraphColor() {
        return graphColor;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof Band)) {
            return false;
        }

        return bandType.equals(((Band) obj).bandType);
    }

    @Override
    public int hashCode() {
        return bandType.hashCode();
    }

    @Override
    public void removeLineData() {
        BandController.getSingletonInstance().removeBand(this);
    }

    @Override
    public void setVisibility(boolean visible) {
        this.setVisible(visible);
        BandController.getSingletonInstance().setBandVisibility(bandType, visible);
    }

    @Override
    public String getName() {
        return this.getTitle();
    }

    @Override
    public Color getDataColor() {
        return this.getGraphColor();
    }

    @Override
    public void setDataColor(Color c) {
        this.setGraphColor(c);

    }

    @Override
    public boolean isDownloading() {
        return DownloadController.getSingletonInstance().isDownloadActive(this);
    }

    @Override
    public boolean isAvailable() {
        return BandController.getSingletonInstance().isBandAvailable(this);
    }

    @Override
    public String getUnitLabel() {
        return bandType.getUnitLabel();
    }

    @Override
    public Component getOptionsPanel() {
        // TODO Auto-generated method stub
        return null;
    }
}
