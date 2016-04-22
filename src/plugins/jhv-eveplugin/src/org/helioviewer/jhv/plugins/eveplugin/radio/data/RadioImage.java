package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Rectangle;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ResolutionSetting;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;

public class RadioImage {

    private Interval imageTimeInterval;
    private Interval imageFreqInterval;
    private Interval visibleImageTimeInterval;
    private Interval visibleImageFreqInterval;
    private int frameInJPX;
    private ResolutionSet resolutioSet;
    private final List<ResolutionSetting> resolutionSettings;
    private ResolutionSetting lastUsedResolutionSetting;
    private final long radioImageID;
    private boolean isDownloading;
    private Rectangle lastDataSize;

    public RadioImage(long radioImageID, Interval timeInterval, Interval freqInterval, int frameInJPX, ResolutionSet rs, List<ResolutionSetting> resolutionSettings, boolean isDownloading) {
        super();
        imageTimeInterval = timeInterval;
        imageFreqInterval = freqInterval;
        visibleImageFreqInterval = freqInterval;
        visibleImageTimeInterval = timeInterval;
        this.frameInJPX = frameInJPX;
        resolutioSet = rs;
        this.resolutionSettings = resolutionSettings;
        this.radioImageID = radioImageID;
        this.isDownloading = isDownloading;
    }

    /**
     * Gives the size of the latest data received
     *
     * @return Rectangle with the size of the latest received data for this
     *         RadioImage
     */
    public Rectangle getLastDataSize() {
        // Log.trace("get last data size for image id : " + radioImageID);
        return lastDataSize;
    }

    /**
     * Sets the size of the latest received data.
     *
     * @param lastDataSize
     *            The size of the last data download
     */
    public void setLastDataSize(Rectangle lastDataSize) {
        // Log.trace("Set last data size for image id : " + radioImageID);
        this.lastDataSize = lastDataSize;
    }

    /**
     * Indicates if this radio image is downloading.
     *
     * @return True if the radio image is downloading, false if not
     */
    public boolean isDownloading() {
        return isDownloading;
    }

    /**
     * Sets whether the radio image is downloading
     *
     * @param isDownloading
     *            True if the radio image is downloading, false if not
     */
    public void setDownloading(boolean isDownloading) {
        this.isDownloading = isDownloading;
    }

    public long getRadioImageID() {
        return radioImageID;
    }

    public ResolutionSetting getLastUsedResolutionSetting() {
        return lastUsedResolutionSetting;
    }

    public void setLastUsedResolutionSetting(ResolutionSetting resolutionSetting) {
        lastUsedResolutionSetting = resolutionSetting;
    }

    public Interval getTimeInterval() {
        return imageTimeInterval;
    }

    public Interval getVisibleImageTimeInterval() {
        return visibleImageTimeInterval;
    }

    public Interval getVisibleImageFreqInterval() {
        return visibleImageFreqInterval;
    }

    public void setTimeInterval(Interval timeInterval) {
        imageTimeInterval = timeInterval;
    }

    public Interval getFreqInterval() {
        return imageFreqInterval;
    }

    public void setFreqInterval(Interval freqInterval) {
        imageFreqInterval = freqInterval;
    }

    public int getFrameInJPX() {
        return frameInJPX;
    }

    public void setFrameInJPX(int frameInJPX) {
        this.frameInJPX = frameInJPX;
    }

    public ResolutionSet getResolutioSet() {
        return resolutioSet;
    }

    public void setResolutioSet(ResolutionSet resolutioSet) {
        this.resolutioSet = resolutioSet;
    }

    public ResolutionSetting defineBestResolutionSetting(double ratioX, double ratioY) {
        ResolutionSetting currentBest = null;
        int highestLevel = 0;
        if (ratioX != 0 && ratioY != 0) {
            for (ResolutionSetting rs : resolutionSettings) {
                if (rs.getxRatio() < ratioX || rs.getyRatio() < ratioY) {
                    if (rs.getResolutionLevel() > highestLevel) {
                        highestLevel = rs.getResolutionLevel();
                        currentBest = rs;
                    }
                }
                if (currentBest == null && rs.getResolutionLevel() == 0) {
                    currentBest = rs;
                }
            }
        } else {
            return resolutionSettings.get(resolutionSettings.size() - 1);
        }
        return currentBest;
    }

    public boolean withinInterval(Interval intervalToBeIn, Interval freqIntervalToBeIn) {
        return intervalToBeIn.overlapsInclusive(imageTimeInterval) && freqIntervalToBeIn.overlaps(imageFreqInterval);
    }

    /**
     * Defines the visible interval for this image based on the given visible
     * time and frequency. If the given visible interval start or end (both time
     * and frequency) lies within the time and frequency interval of this image.
     * The part of this image that is visible is defined.
     *
     * @param visibleXStart
     *            The start time of the visible time interval
     * @param visibleXEnd
     *            The end time of the visible time interval
     * @param visibleYStart
     *            The start frequency of the visible interval
     * @param visibleYEnd
     *            The end frequency of the visible interval
     */
    public void setVisibleIntervals(long visibleXStart, long visibleXEnd, int visibleYStart, int visibleYEnd) {
        if (imageTimeInterval.containsPointInclusive(visibleXStart) || imageTimeInterval.containsPointInclusive(visibleXEnd)) {

            long tempStartX = imageTimeInterval.squeeze(visibleXStart);
            long tempEndX = imageTimeInterval.squeeze(visibleXEnd);
            visibleImageTimeInterval = new Interval(tempStartX, tempEndX);
        } else {
            Interval tempInterval = new Interval(visibleXStart, visibleXEnd);
            if (tempInterval.containsPointInclusive(imageTimeInterval.start) || tempInterval.containsPointInclusive(imageTimeInterval.end)) {
                long tempStartX = tempInterval.squeeze(imageTimeInterval.start);
                long tempEndX = tempInterval.squeeze(imageTimeInterval.end);
                visibleImageTimeInterval = new Interval(tempStartX, tempEndX);
            } else {
                visibleImageTimeInterval = null;
            }

        }
        if (imageFreqInterval.containsPointInclusive(visibleYStart) || imageFreqInterval.containsPointInclusive(visibleYEnd)) {
            int tempStartY = (int) imageFreqInterval.squeeze(visibleYStart);
            int tempEndY = (int) imageFreqInterval.squeeze(visibleYEnd);
            visibleImageFreqInterval = new Interval(tempStartY, tempEndY);
        } else {
            visibleImageFreqInterval = null;
        }

    }

    /**
     * Defines the region of interest based on the visible time and frequency
     * interval. The region of interest is given in coordinates in the highest
     * resolution level.
     *
     * @return A Rectangle containing the region of interest of the visible time
     *         and frequency interface.
     */
    public Rectangle getROI() {
        if (visibleImageFreqInterval != null && visibleImageTimeInterval != null) {
            int maxImageWidth = resolutioSet.getResolutionLevel(0).width;
            int maxImageHeight = resolutioSet.getResolutionLevel(0).height;
            long imageTimesize = imageTimeInterval.end - imageTimeInterval.start;
            int imageFrequencySize = (int) (imageFreqInterval.end - imageFreqInterval.start);
            double timePerPix = 1.0 * imageTimesize / maxImageWidth;
            double freqPerPix = 1.0 * imageFrequencySize / maxImageHeight;

            int x0 = (int) Math.round((visibleImageTimeInterval.start - imageTimeInterval.start) / timePerPix);
            int y0 = (int) Math.round((imageFreqInterval.end - visibleImageFreqInterval.end) / freqPerPix);
            int width = (int) Math.round((visibleImageTimeInterval.end - visibleImageTimeInterval.start) / timePerPix);
            int height = (int) Math.round((visibleImageFreqInterval.end - visibleImageFreqInterval.start) / freqPerPix);
            return new Rectangle(x0, y0, width, height);
        } else {
            return null;
        }
    }

    public double getVisibleImagePercentage() {
        int maxImageWidth = resolutioSet.getResolutionLevel(0).width;
        int maxImageHeight = resolutioSet.getResolutionLevel(0).height;
        Rectangle roi = getROI();
        if (roi != null) {
            return roi.getWidth() * roi.getHeight() / (maxImageHeight * maxImageWidth);
        } else {
            return Double.NaN;
        }
    }
}
