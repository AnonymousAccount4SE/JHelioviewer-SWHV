package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.base.Range;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.EVEState;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.GraphDimensionListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.RangeListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.YAxis;
import org.helioviewer.jhv.plugins.eveplugin.radio.gui.RadioImagePane;
import org.helioviewer.jhv.plugins.eveplugin.radio.gui.RadioOptionsPanel;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ColorLookupModel;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ColorLookupModelListener;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.DrawableAreaMap;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.NoDataConfig;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.PlotConfig;
import org.helioviewer.jhv.plugins.eveplugin.radio.model.ResolutionSetting;
import org.helioviewer.jhv.plugins.eveplugin.settings.EVESettings;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModel;
import org.helioviewer.jhv.viewmodel.metadata.XMLMetaDataContainer;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image.ReaderMode;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ImageCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;

/**
 * The radio data manager manages all the downloaded data for radio
 * spectrograms.
 *
 * It receives all of its input from the radio downloader as listener of the
 * radio downloader.
 *
 * @author Bram.Bourgoignie@oma.be
 *
 */
public class RadioDataManager implements ColorLookupModelListener, LineDataSelectorElement, TimingListener, GraphDimensionListener, RangeListener {

    /** The singleton instance of the class. */
    private static RadioDataManager instance;

    /** The instance of downloader */
    private RadioDownloader downloader;

    /** The instance of the line data selector model */
    private LineDataSelectorModel lineDataSelectorModel;

    /** Instance of the radio image cache */
    private RadioImageCache cache;

    /** Instance of eve state */
    private EVEState eveState;

    /** Is there a request for data busy */
    private boolean requestForDataBusy;

    private final DrawController drawController;
    private Map<Long, BufferedImage> bufferedImages;
    private final YAxis yAxis;
    private final RadioImagePane radioImagePane;
    private Map<Long, PlotConfig> plotConfigList;
    /** Map containing per download id a list of no data configurations */
    private List<NoDataConfig> noDataConfigList;

    private Map<Long, RadioImage> radioImages;
    private boolean isDownloading;
    private boolean isVisible;

    /**
     * private constructor used when the instance is requested for the first
     * time.
     *
     */
    private RadioDataManager() {
        ColorLookupModel.getInstance().addFilterModelListener(this);
        drawController = EVEPlugin.dc;
        drawController.addGraphDimensionListener(this);
        drawController.addTimingListener(this);
        bufferedImages = new HashMap<Long, BufferedImage>();
        yAxis = new YAxis(new Range(), "Mhz", false);
        radioImagePane = new RadioImagePane();
        radioImagePane.setYAxis(yAxis);
        plotConfigList = new HashMap<Long, PlotConfig>();
        noDataConfigList = new ArrayList<NoDataConfig>();
        isVisible = true;
        isDownloading = false;
    }

    /**
     * Access to the singleton instance of the radio data manager
     *
     * @return The instance of the radio data manager
     */
    public static RadioDataManager getSingletonInstance() {
        if (instance == null) {
            instance = new RadioDataManager();
            instance.init();
        }
        return instance;
    }

    private void init() {
        lineDataSelectorModel = LineDataSelectorModel.getSingletonInstance();
        cache = RadioImageCache.getInstance();
        eveState = EVEState.getSingletonInstance();
        requestForDataBusy = false;
        downloader = RadioDownloader.getSingletonInstance();
    }

    /**
     * Inform the RadioDataManager about the new data that was received.
     *
     *
     * @param data
     *            The data received
     * @param imageID
     *            The ID of the image for which data was received
     * @param dataSize
     *            The height and width of the data
     */
    public void dataForImage(byte[] byteData, long imageID, Rectangle dataSize, Rectangle providedRegion, int resolutionHeight) {
        if (radioImages != null && isVisible) {
            RadioImage image = radioImages.get(imageID);
            if (image != null) {
                image.setLastDataSize(dataSize);
                if (image.getVisibleImageFreqInterval() != null && image.getVisibleImageTimeInterval() != null) {
                    Interval dataFrequencyInterval = defineDataFrequencyInterval(image.getFreqInterval(), providedRegion, resolutionHeight);
                    BufferedImage newImage = createBufferedImage(dataSize.width, dataSize.height, byteData);
                    bufferedImages.put(imageID, newImage);
                    radioImagePane.setIntervalTooBig(false);
                    DrawableAreaMap dam = getDrawableAreaMap(image.getVisibleImageTimeInterval().start, image.getVisibleImageTimeInterval().end, dataFrequencyInterval.start, dataFrequencyInterval.end, image.getFreqInterval().start, image.getFreqInterval().end, dataSize);
                    PlotConfig pc = new PlotConfig(newImage, dam, isVisible, imageID);
                    plotConfigList.put(imageID, pc);
                    fireDrawNewBufferedImage();
                }
            }
        }
    }

    /**
     * Removes the given download request data. The data is removed from the
     * line selector model. The images are removed from the cache.
     *
     * The listeners are informed about the removal of the download request
     * data.
     *
     * @param drd
     *            The download request data that should be removed
     */
    private void removeRadioData() {
        if (radioImages != null) {

            for (long imageID : radioImages.keySet()) {
                cache.remove(imageID);
                bufferedImages.remove(imageID);
            }
            noDataConfigList = new ArrayList<NoDataConfig>();
            plotConfigList = new HashMap<Long, PlotConfig>();
            drawController.removeDrawableElement(radioImagePane);
            lineDataSelectorModel.removeLineData(this);
            radioImages = null;
        }
    }

    /**
     * Changed the visibility of the download request data. All radio data
     * manager listeners are informed.
     *
     *
     * @param drd
     *            The download request data for which the visiblility is changed
     */
    public void radioDataVisibilityChanged() {
        for (PlotConfig pc : plotConfigList.values()) {
            pc.setVisible(isVisible);
        }
        for (NoDataConfig ndc : noDataConfigList) {
            ndc.setVisible(isVisible);
        }
        if (isVisible) {
            drawController.updateDrawableElement(radioImagePane, true);
        } else {
            drawController.removeDrawableElement(radioImagePane);
        }

        lineDataSelectorModel.lineDataElementUpdated(this);
    }

    private void requestForData(long xStart, long xEnd, double yStart, double yEnd, double xRatio, double yRatio) {
        RequestConfig requestConfig = new RequestConfig(xStart, xEnd, yStart, yEnd, xRatio, yRatio);
        handleRequestConfig(requestConfig, xStart, xEnd, yStart, yEnd);
    }

    /**
     * Informs the radio data manager about an image, identified by imageID from
     * download identified by downloadID, finished its downloading?
     *
     *
     * @param imageID
     *            The image id of the image that finished downloading
     * @param downloadID
     *            The download id of the batch of which the image was part
     */
    public void finishedDownloadingImage(long imageID) {
        if (radioImages != null) {
            RadioImage image = radioImages.get(imageID);
            if (image != null) {
                image.setDownloading(false);
            }
            boolean isImDownloading = false;
            for (RadioImage im : radioImages.values()) {
                if (im.isDownloading()) {
                    isImDownloading = true;
                    break;
                }
            }
            if (!isImDownloading) {
                if (isDownloading) {
                    isDownloading = false;
                    lineDataSelectorModel.downloadFinished(this);
                }
            }
        }
    }

    public void intervalTooBig(long requestedStartTime, long requestedEndTime) {
        radioImages = new HashMap<Long, RadioImage>();
        lineDataSelectorModel.addLineData(this);
        intervalTooBig();
        downloadRequestAnswered(new Interval(requestedStartTime, requestedEndTime));
    }

    public void newJPXFilesDownloaded(List<DownloadedJPXData> jpxFiles, long requestedStartTime, long requestedEndTime) {
        radioImages = new HashMap<Long, RadioImage>();
        isDownloading = true;
        lineDataSelectorModel.addLineData(this);
        if (!jpxFiles.isEmpty()) {
            for (DownloadedJPXData djd : jpxFiles) {
                handleDownloadedJPXData(djd, Double.NaN, Double.NaN);
            }
        }
        defineMaxBounds();
        downloadRequestAnswered(new Interval(requestedStartTime, requestedEndTime));
    }

    public void newAdditionalDataDownloaded(List<DownloadedJPXData> jpxFiles, double ratioX, double ratioY) {
        if (radioImages != null) {
            boolean oldDownloading = isDownloading;
            isDownloading = true;
            if (!oldDownloading) {
                lineDataSelectorModel.downloadStarted(this);
            }
            for (DownloadedJPXData djd : jpxFiles) {
                handleDownloadedJPXData(djd, ratioX, ratioY);
            }
            defineMaxBounds();
        }
    }

    public void newNoData(List<Interval> noDataList) {
        if (!eveState.isMouseTimeIntervalDragging() && !eveState.isMouseValueIntervalDragging() && noDataList.size() > 0) {
            radioImagePane.setIntervalTooBig(false);
            for (Interval noData : noDataList) {
                DrawableAreaMap dam = getDrawableAreaMap(noData.start, noData.end);
                noDataConfigList.add(new NoDataConfig(noData, dam, isVisible));
            }
            fireDrawNewBufferedImage();
        }
    }

    public void removeSpectrograms() {
        removeRadioData();
    }

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged() {
        requestData();
    }

    @Override
    public void graphDimensionChanged() {
        requestData();
    }

    @Override
    public void rangeChanged() {
        requestData();
    }

    private void requestData() {
        Rectangle displaySize = drawController.getGraphArea();
        double xRatio = drawController.selectedAxis.getRatio(displaySize.width);
        double yRatio = 1.0 * (yAxis.getSelectedRange().max - yAxis.getSelectedRange().min) / displaySize.getHeight();
        Interval selectedInterval = drawController.getSelectedInterval();
        requestData(selectedInterval.start, selectedInterval.end, yAxis.getSelectedRange().min, yAxis.getSelectedRange().max, xRatio, yRatio);
    }

    /**
     * Define the maximum bound of the frequency interval. The y-value model is
     * updated.
     *
     * @param downloadID
     *            The download ID for which the bounds are defined
     * @param plotIdentifier
     *            The plot identifier for which the bound are defined
     */
    private void defineMaxBounds() {
        Interval maxFrequencyInterval = new Interval(20, 400);
        if (radioImages != null) {
            if (!radioImages.isEmpty()) {
                long localMinFrequency = -1;
                long localMaxFrequency = -1;
                boolean first = true;
                for (RadioImage image : radioImages.values()) {
                    if (first) {
                        localMinFrequency = image.getFreqInterval().start;
                        localMaxFrequency = image.getFreqInterval().end;
                        first = false;
                    } else {
                        if (image.getFreqInterval().start < localMinFrequency) {
                            localMinFrequency = (int) image.getFreqInterval().start;
                        }
                        if (image.getFreqInterval().end > localMaxFrequency) {
                            localMaxFrequency = (int) image.getFreqInterval().end;
                        }
                    }
                }
                maxFrequencyInterval = new Interval(localMinFrequency, localMaxFrequency);
            } else {
                maxFrequencyInterval = new Interval(0, 0);
            }
        }
        yAxis.setSelectedRange(new Range(maxFrequencyInterval.start, maxFrequencyInterval.end));
    }

    private Interval defineDataFrequencyInterval(Interval freqInterval, Rectangle providedRegion, int resolutionHeight) {
        double ratio = (freqInterval.end - freqInterval.start) / (double) resolutionHeight;
        double start = freqInterval.end - providedRegion.getY() * ratio;
        double end = start - providedRegion.getHeight() * ratio;
        return new Interval((int) start, (int) end);
    }

    /**
     * Function handles a request configuration.
     *
     * @param requestConfig
     *            The request configuration to be handled
     * @param xStart
     *            The start date of the currently visible time interval
     * @param xEnd
     *            The end date of the currently visible time interval
     * @param yStart
     *            The start value of the currently visible frequency interval
     * @param yEnd
     *            The end value of the currently visible frequency interval
     */
    private void handleRequestConfig(RequestConfig requestConfig, long xStart, long xEnd, double yStart, double yEnd) {
        if (requestConfig.xEnd - requestConfig.xStart > EVESettings.MAXIMUM_INTERVAL_RANGE_MILLI_SEC_REQ) {
            intervalTooBig();
        } else {
            RadioImageCacheResult result = cache.getRadioImageCacheResultForInterval(requestConfig.xStart, requestConfig.xEnd, 24L * 60 * 60 * 1000);
            if (radioImages != null) {
                downloader.requestAndOpenIntervals(result.getMissingInterval(), requestConfig.xRatio, requestConfig.yRatio);
            } else {
                Log.trace("drd is null");
            }

            if (radioImages != null) {
                for (long imageID : result.getToRemove()) {
                    plotConfigList.remove(imageID);
                }
                // Log.trace("Size of available images : " +
                // result.getAvailableData().size());
                for (DownloadedJPXData jpxData : result.getAvailableData()) {
                    handleAvailableData(jpxData, xStart, xEnd, yStart, yEnd);
                }
            }
        }

    }

    /**
     * Handles new available data send by the radio downloader
     *
     * @param xStart
     *            The start time of the currently visible time interval
     * @param xEnd
     *            The end time of the currently visible time interval
     * @param yStart
     *            The start frequency of the currently visible frequency
     *            interval
     * @param yEnd
     *            The end frequency of the currently visible frequency interval
     * @param jpxData
     *            The downloaded jpx data that should be handled
     * @param plotIdentifier
     *            The identifier of the plot
     * @param drd
     *            The download request data
     *
     */
    private void handleAvailableData(DownloadedJPXData jpxData, long xStart, long xEnd, double yStart, double yEnd) {
        RadioImage ri = radioImages.get(jpxData.getImageID());
        if (ri != null) {
            ri.setVisibleIntervals(xStart, xEnd, (int) Math.floor(yStart), (int) Math.ceil(yEnd));
            if (ri.getVisibleImageFreqInterval() != null && ri.getVisibleImageTimeInterval() != null) {
                Interval visibleDateInterval = ri.getVisibleImageTimeInterval();
                if (visibleDateInterval.start != visibleDateInterval.end) {
                    JP2ViewCallisto jp2View = jpxData.getView();
                    if (jp2View != null) {
                        JP2ImageCallisto image = jp2View.getJP2Image();
                        image.setRegion(ri.getROI());
                        jp2View.render(null, null, defineFactor(ri.getVisibleImagePercentage()));
                    }
                }
            }
        }
    }

    /**
     * Handles newly downloaded jpx-data.
     *
     * @param djd
     *            The jpx-data to be handled
     * @param drd
     *            The corresponding download request data
     * @param downloadID
     *            The download identifier for which the data was downloaded
     */
    private void handleDownloadedJPXData(DownloadedJPXData djd, double ratioX, double ratioY) {
        JP2ViewCallisto jp2View = djd.getView();
        if (jp2View != null) {
            JP2ImageCallisto image = jp2View.getJP2Image();
            image.setReaderMode(ReaderMode.ONLYFIREONCOMPLETE);
            ResolutionSet rs = image.getResolutionSet(0);
            int maximumFrameNumber = image.getMaximumFrameNumber();
            LineDataSelectorModel.getSingletonInstance().downloadStarted(this);

            XMLMetaDataContainer hvMetaData = new XMLMetaDataContainer();
            for (int i = 0; i <= maximumFrameNumber; i++) {
                try {
                    hvMetaData.parseXML(image.getXML(i));
                    double freqStart = hvMetaData.tryGetDouble("STARTFRQ");
                    double freqEnd = hvMetaData.tryGetDouble("END-FREQ");
                    long start = JHVDate.parseDateTime(hvMetaData.get("DATE-OBS")).milli;
                    long end = JHVDate.parseDateTime(hvMetaData.get("DATE-END")).milli;
                    hvMetaData.destroyXML();

                    Interval fi = new Interval((int) Math.round(freqStart), (int) Math.round(freqEnd));

                    List<ResolutionSetting> resolutionSettings = new ArrayList<ResolutionSetting>();
                    Interval dateInterval = new Interval(start, end);
                    for (int j = 0; j <= rs.getMaxResolutionLevels(); j++) {
                        ResolutionLevel res = rs.getResolutionLevel(j);
                        ResolutionSetting tempResSet = new ResolutionSetting((end - start) / (double) res.width, (freqEnd - freqStart) / res.height, j, res.width, res.height, res.discardLayers);
                        resolutionSettings.add(tempResSet);
                    }

                    RadioImage tempRs = new RadioImage(djd.getImageID(), dateInterval, fi, i, rs, resolutionSettings, true);

                    ResolutionSetting lastUsedResolutionSetting = null;
                    if (Double.isNaN(ratioX) && Double.isNaN(ratioY)) {
                        int highestLevel = -1;
                        for (ResolutionSetting rst : resolutionSettings) {
                            if (rst.getResolutionLevel() > highestLevel) {
                                highestLevel = rst.getResolutionLevel();
                                lastUsedResolutionSetting = rst;
                            }
                        }
                    } else {
                        lastUsedResolutionSetting = tempRs.defineBestResolutionSetting(ratioX, ratioY);
                    }
                    tempRs.setLastUsedResolutionSetting(lastUsedResolutionSetting);
                    image.setRegion(tempRs.getROI());
                    jp2View.render(null, null, defineFactor(tempRs.getVisibleImagePercentage()));
                    radioImages.put(tempRs.getRadioImageID(), tempRs);
                } catch (Exception e) {
                    Log.error("Some of the metadata could not be read, aborting...");
                    return;
                }
            }
        }
    }

    private double defineFactor(double visibleImagePercentage) {
        if (!Double.isNaN(visibleImagePercentage)) {
            if (visibleImagePercentage <= 0.03125) {
                return 1;
            } else if (visibleImagePercentage <= 0.0625) {
                return 0.5;
            } else if (visibleImagePercentage <= 0.125) {
                return 0.25;
            } else if (visibleImagePercentage <= 0.25) {
                return 0.125;
            } else if (visibleImagePercentage <= 0.5) {
                return 0.0625;
            } else {
                return 0.03125;
            }
        } else {
            return 0;
        }
    }

    public void noDataInDownloadInterval(Interval requestInterval) {
        radioImages = new HashMap<Long, RadioImage>();
        lineDataSelectorModel.addLineData(this);
        downloadRequestAnswered(requestInterval);
    }

    public Collection<PlotConfig> getPlotConfigurations() {
        return plotConfigList.values();
    }

    /**
     * Creates a collection of no data configurations for a specific plot
     * identifier
     *
     * @return A collection containing all the no data configurations.
     */
    public Collection<NoDataConfig> getNoDataConfigurations() {
        return noDataConfigList;
    }

    private void downloadRequestAnswered(Interval timeInterval) {
        requestData();
    }

    private void intervalTooBig() {
        plotConfigList = new HashMap<Long, PlotConfig>();
        radioImagePane.setIntervalTooBig(true);
        drawController.updateDrawableElement(radioImagePane, true);
    }

    private void requestData(long xStart, long xEnd, double yStart, double yEnd, double xRatio, double yRatio) {
        if (radioImages != null && isVisible) {
            requestForData(xStart, xEnd, yStart, yEnd, xRatio, yRatio);
            updateNoDataConfig();
        }
    }

    private void fireDrawNewBufferedImage() {
        radioImagePane.setYAxis(yAxis);
        drawController.updateDrawableElement(radioImagePane, true);
    }

    private BufferedImage createBufferedImage(int width, int height, byte[] data) {
        byte[] useData;
        if (width * height == data.length) {
            useData = data;
        } else {
            Log.error("Data array was too small; white image created");
            useData = new byte[width * height];
        }
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, ColorLookupModel.getInstance().getColorModel());
        DataBufferByte dataBuffer = new DataBufferByte(useData, width * height);
        Raster raster = Raster.createPackedRaster(dataBuffer, width, height, width, new int[] { 0xff }, new Point(0, 0));
        newImage.setData(raster);
        return newImage;
    }

    private void updateNoDataConfig() {
        for (NoDataConfig ndc : noDataConfigList) {
            DrawableAreaMap dam = getDrawableAreaMap(ndc.getDateInterval().start, ndc.getDateInterval().end);
            ndc.setDrawableAreaMap(dam);
        }
        fireDrawNewBufferedImage();
    }

    @Override
    public void colorLUTChanged() {
        ColorModel cm = ColorLookupModel.getInstance().getColorModel();
        Map<Long, BufferedImage> newBufferedImages = new HashMap<Long, BufferedImage>();
        for (Map.Entry<Long, BufferedImage> entry : bufferedImages.entrySet()) {
            long index = entry.getKey();
            BufferedImage old = entry.getValue();
            BufferedImage newIm = new BufferedImage(cm, old.getRaster(), false, null);
            newBufferedImages.put(index, newIm);
        }
        bufferedImages = newBufferedImages;
        updatePlotConfigurations();
        if (!getPlotConfigurations().isEmpty()) {
            fireDrawNewBufferedImage();
        }
    }

    private void updatePlotConfigurations() {
        for (Map.Entry<Long, PlotConfig> entry : plotConfigList.entrySet()) {
            long imageID = entry.getKey();
            PlotConfig tempPC = entry.getValue();
            if (bufferedImages.containsKey(imageID)) {
                tempPC.setBufferedImage(bufferedImages.get(imageID));
            }
        }
    }

    public YAxis getYAxis() {
        return yAxis;
    }

    @Override
    public void removeLineData() {
        removeRadioData();
    }

    @Override
    public void setVisibility(boolean visible) {
        isVisible = visible;
        radioDataVisibilityChanged();
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

    private DrawableAreaMap getDrawableAreaMap(long startDate, long endDate, long visualStartFrequency, long visualEndFrequency, long imageStartFrequency, long imageEndFrequency, Rectangle area) {
        int sourceX0 = defineXInSourceArea(startDate, startDate, endDate, area);
        int sourceY0 = 0;
        int sourceX1 = defineXInSourceArea(endDate, startDate, endDate, area);
        int sourceY1 = area.height;
        if (sourceY0 == sourceY1) {
            sourceY1 = sourceY0 + 1;
        }
        if (sourceX0 == sourceX1) {
            sourceX1 = sourceX0 + 1;
        }

        Rectangle plotArea = drawController.getPlotArea();
        int destX0 = drawController.selectedAxis.value2pixel(plotArea.x, plotArea.width, startDate);
        int destY0 = defineYInDestinationArea(visualStartFrequency, yAxis);
        int destX1 = drawController.selectedAxis.value2pixel(plotArea.x, plotArea.width, endDate);
        int destY1 = defineYInDestinationArea(visualEndFrequency, yAxis);
        return new DrawableAreaMap(sourceX0, sourceY0, sourceX1, sourceY1, destX0, destY0, destX1, destY1);
    }

    public DrawableAreaMap getDrawableAreaMap(long startDate, long endDate) {
        Rectangle plotArea = drawController.getPlotArea();
        int destX0 = drawController.selectedAxis.value2pixel(plotArea.x, plotArea.width, startDate);
        int destY0 = 0;
        int destX1 = drawController.selectedAxis.value2pixel(plotArea.x, plotArea.width, endDate);
        int destY1 = plotArea.height;
        return new DrawableAreaMap(0, 0, 0, 0, destX0, destY0, destX1, destY1);
    }

    private int defineYInDestinationArea(long frequencyToFind, YAxis _yAxis) {
        Rectangle displaySize = drawController.getPlotArea();
        return displaySize.height - (int) Math.floor((frequencyToFind - _yAxis.getSelectedRange().min) / (1.0 * (_yAxis.getSelectedRange().max - _yAxis.getSelectedRange().min) / displaySize.height));
    }

    private int defineXInSourceArea(long dateToFind, long startDateArea, long endDateArea, Rectangle area) {
        long timediff = dateToFind - startDateArea;
        long timeOfArea = endDateArea - startDateArea;
        return (int) Math.floor(timediff / (1.0 * (timeOfArea) / area.width));
    }

}
