package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.plugins.swek.config.SWEKEventType;
import org.helioviewer.jhv.plugins.swek.download.SWEKParam;
import org.helioviewer.jhv.plugins.swek.sources.SWEKDownloader;

public class HEKDownloader implements SWEKDownloader {

    @Override
    public void stopDownload() {
    }

    @Override
    public InputStream downloadData(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params, int page) {
        String urlString = createURL(eventType, startDate, endDate, params, page);
        try {
            DownloadStream ds = new DownloadStream(new URL(urlString), JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout());
            return ds.getInput();
        } catch (MalformedURLException e) {
            Log.error("Could not create URL from given string: " + urlString + " error : " + e);
            return null;
        } catch (IOException e) {
            Log.error("Could not create input stream for given URL: " + urlString + " error : " + e);
            return null;
        }

    }

    /**
     * Creates the download URL for the HEK.
     *
     * @param eventType
     *            the event type that should be downloaded
     * @param startDate
     *            the start date of the interval over which to download the
     *            event
     * @param endDate
     *            the end date of the interval over which to download the event
     * @param page
     *            the page that should be downloaded
     * @return the url represented as string
     */
    private String createURL(SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params, int page) {
        StringBuilder baseURL = new StringBuilder(HEKSourceProperties.getSingletonInstance().getHEKSourceProperties().getProperty("heksource.baseurl")).append("?");
        baseURL = appendCmd(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendType(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendEventType(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendEventCoorSys(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendX1X2Y1Y2(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendCosec(baseURL, eventType, startDate, endDate).append("&");
        baseURL = appendParams(baseURL, eventType, startDate, endDate, params).append("&");
        baseURL = appendEventStartTime(baseURL, eventType, startDate).append("&");
        baseURL = appendEventEndTime(baseURL, eventType, endDate).append("&");
        baseURL = appendPage(baseURL, page);
        return baseURL.toString();
    }

    /**
     * Appends the command to the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the command
     */
    private StringBuilder appendCmd(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("cmd=search");
    }

    /**
     * Appends the type to the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the type
     */
    private StringBuilder appendType(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("type=column");
    }

    /**
     * Appends the event type to the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the event type
     */
    private StringBuilder appendEventType(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("event_type=").append(HEKEventFactory.getHEKEvent(eventType.getEventName()).getAbbreviation());
    }

    /**
     * Appends the event coordinate system to the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the event coordinate system
     */
    private StringBuilder appendEventCoorSys(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("event_coordsys=").append(eventType.getCoordinateSystem());
    }

    /**
     * Appends the spatial region to the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with the spatial region
     */
    private StringBuilder appendX1X2Y1Y2(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        baseURL.append("x1=").append(eventType.getSpatialRegion().x1).append("&");
        baseURL.append("x2=").append(eventType.getSpatialRegion().x2).append("&");
        baseURL.append("y1=").append(eventType.getSpatialRegion().y1).append("&");
        baseURL.append("y2=").append(eventType.getSpatialRegion().y2).append("&");
        return baseURL;
    }

    /**
     * Append cosec to the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @return the current URL extended with cosec
     */
    private StringBuilder appendCosec(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate) {
        return baseURL.append("cosec=2");
    }

    /**
     * Appends params to the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @param endDate
     *            the end date
     * @param params
     * @return the current URL extended with the params
     */
    private StringBuilder appendParams(StringBuilder baseURL, SWEKEventType eventType, Date startDate, Date endDate, List<SWEKParam> params) {
        int paramCount = 0;

        for (SWEKParam param : params) {
            String encodedValue;
            try {
                encodedValue = URLEncoder.encode(param.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                encodedValue = param.getValue();
            }
            if (param.getParam().toLowerCase().equals("provider")) {
                baseURL.append("param").append(paramCount).append("=").append("frm_name").append("&").append("op").append(paramCount).append("=").append(param.getOperand().URLEncodedRepresentation()).append("&").append("value").append(paramCount).append("=").append(encodedValue).append("&");
            } else {
                baseURL.append("param").append(paramCount).append("=").append(param.getParam()).append("&").append("op").append(paramCount).append("=").append(param.getOperand().URLEncodedRepresentation()).append("&").append("value").append(paramCount).append("=").append(encodedValue).append("&");
            }
            paramCount++;
        }
        return baseURL;
    }

    /**
     * Appends the event start time to the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param startDate
     *            the start date
     * @return the current URL extended with the start time
     */
    private StringBuilder appendEventStartTime(StringBuilder baseURL, SWEKEventType eventType, Date startDate) {
        return baseURL.append("event_starttime=").append(formatDate(startDate));
    }

    /**
     * Appends the event end time to the given URL.
     *
     * @param baseURL
     *            the current URL
     * @param eventType
     *            the event type
     * @param endDate
     *            the end date
     * @return the current URL extended with the end time
     */
    private StringBuilder appendEventEndTime(StringBuilder baseURL, SWEKEventType eventType, Date endDate) {
        return baseURL.append("event_endtime=").append(formatDate(endDate));
    }

    /**
     * Formats a date in the yyyy-mm-ddThh:mm:ss format.
     *
     * @param date
     *            the date to format
     * @return the date in format yyyy-mm-ddThh:mm-ss
     */
    private String formatDate(Date date) {
        return TimeUtils.utcDateFormat.format(date);
    }

    private StringBuilder appendPage(StringBuilder baseURL, int page) {
        return baseURL.append("page=").append(page);
    }
}
