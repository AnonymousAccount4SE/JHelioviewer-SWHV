package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import org.helioviewer.jhv.plugins.eveplugin.radio.gui.RadioOptionsPanel;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;

public class DownloadRequestData implements LineDataSelectorElement {

    private Map<Long, RadioImage> radioImages;
    private boolean isDownloading;
    private boolean isVisible;

    private RadioDataManager radioDataManager;

    public DownloadRequestData() {
        radioDataManager = RadioDataManager.getSingletonInstance();
        radioImages = new HashMap<Long, RadioImage>();
        isVisible = true;
    }

    public DownloadRequestData(Map<Long, RadioImage> radioImages) {
        this.radioImages = radioImages;
        isVisible = true;
    }

    public void addRadioImage(RadioImage radioImage) {
        radioImages.put(radioImage.getRadioImageID(), radioImage);
    }

    public Map<Long, RadioImage> getRadioImages() {
        return radioImages;
    }

    public void setRadioImages(Map<Long, RadioImage> radioImages) {
        this.radioImages = radioImages;
    }

    @Override
    public void removeLineData() {
        radioDataManager.removeDownloadRequestData();
    }

    @Override
    public void setVisibility(boolean visible) {
        isVisible = visible;
        radioDataManager.downloadRequestDataVisibilityChanged(this);
    }

    @Override
    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public String getName() {
        return "Callisto radiogram";
    }

    @Override
    public Color getDataColor() {
        return null;
    }

    @Override
    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean isDownloading) {
        this.isDownloading = isDownloading;
    }

    /**
     * Merges the download request data in this download request data. If the
     * plot identifier and the download identifier are the same, the extra radio
     * images are added to the radio images of this download request data. The
     * downloading status and visibility status of this download request data is
     * kept.
     *
     * @param downloadRequestData
     *            The download request data to be merged in this
     */
    public void mergeDownloadRequestData(DownloadRequestData downloadRequestData) {
        radioImages.putAll(downloadRequestData.getRadioImages());
    }

    @Override
    public Component getOptionsPanel() {
        return new RadioOptionsPanel();
    }

    @Override
    public boolean hasData() {
        // TODO maybe implement this too.
        return true;
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

}
