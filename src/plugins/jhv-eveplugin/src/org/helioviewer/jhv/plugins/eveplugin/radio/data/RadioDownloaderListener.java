package org.helioviewer.jhv.plugins.eveplugin.radio.data;

import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.base.interval.Interval;

public interface RadioDownloaderListener {

    public abstract void intervalTooBig(Date requestedStartTime, Date requestedEndTime);

    public abstract void newJPXFilesDownloaded(List<DownloadedJPXData> jpxFiles, Date requestedStartTime, Date requestedEndTime);

    public abstract void newAdditionalDataDownloaded(List<DownloadedJPXData> jpxFiles, double ratioX, double ratioY);

    public abstract void newNoData(List<Interval<Date>> noDataList);

    /**
     * Instructs the radio downloader listener to remove all the spectrograms
     * for the plot identifier by the identifier.
     */
    public abstract void removeSpectrograms();

    public abstract void noDataInDownloadInterval(Interval<Date> requestInterval);

}
