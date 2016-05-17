package org.helioviewer.jhv.io;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.EventDispatchQueue;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.fitsview.FITSView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ImageCallisto;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2ViewCallisto;
import org.helioviewer.jhv.viewmodel.view.simpleimageview.SimpleImageView;

/**
 * This class provides methods to download files from a server.
 *
 * Most of the methods only will work with the current Helioviewer server
 * because they modify links and requests that they will fit with the API.
 *
 * @author Stephan Pagel
 * @author Andre Dau
 * @author Helge Dietert
 */
public class APIRequestManager {
    /**
     * Returns the date of the latest image available from the server
     *
     * @param observatory
     *            observatory of the requested image.
     * @param instrument
     *            instrument of the requested image.
     * @param detector
     *            detector of the requested image.
     * @param measurement
     *            measurement of the requested image.
     * @param message
     *            display error message
     * @return time stamp of the latest available image on the server
     * @throws IOException
     * @throws MalformedURLException
     */
    public static long getLatestImageDate(String observatory, String instrument, String detector, String measurement, boolean message) {
        long timestamp = System.currentTimeMillis();
        boolean readDate = false;
        View view = null;

        try {
            String server = Settings.getSingletonInstance().getProperty("API.jp2images.path");
            view = loadImage(server, observatory, instrument, detector, measurement, timestamp, message);
            if (view != null) {
                timestamp = view.getFirstTime().milli;
                readDate = true;

                EventQueue.invokeLater(new Runnable() {
                    private View view;

                    @Override
                    public void run() {
                        view.abolish();
                    }

                    public Runnable init(View _view) {
                        view = _view;
                        return this;
                    }
                }.init(view));
            } else {
                Log.error("APIRequestManager.getLatestImageDate() > Could not load latest image. Use current date as initial end date.", new Exception());
            }
        } catch (MalformedURLException e) {
            Log.error("APIRequestManager.getLatestImageDate() > Malformed jpip request url. Use current date as initial end date.", e);
        } catch (IOException e) {
            Log.error("APIRequestManager.getLatestImageDate() > Error while opening stream. Use current date as initial end date.", e);
        }

        if (readDate) {
            return timestamp;
        } else {
            return System.currentTimeMillis() - 2 * TimeUtils.DAY_IN_MILLIS;
        }
    }

    /**
     * Sends an request to the server to compute where the nearest image is
     * located on the server. The address of the file will be returned.
     *
     * @param observatory
     *            observatory of the requested image.
     * @param instrument
     *            instrument of the requested image.
     * @param detector
     *            detector of the requested image.
     * @param measurement
     *            measurement of the requested image.
     * @param startTime
     *            time if the requested image.
     * @param message
     *            display error message.
     * @return view of the nearest image file on the server.
     * @throws MalformedURLException
     * @throws IOException
     */
    private static View loadImage(String server, String observatory, String instrument, String detector, String measurement, long startTime, boolean message) throws IOException {
        String fileRequest = server + "?action=getJP2Image&observatory=" + observatory +
                                      "&instrument=" + instrument +
                                      "&detector=" + detector +
                                      "&measurement=" + measurement +
                                      "&date=" + TimeUtils.apiDateFormat.format(startTime) + "&json=true";
        String jpipRequest = fileRequest + "&jpip=true";

        // get URL from server where file with image series is located
        try {
            return requestData(new URL(jpipRequest), new URI(fileRequest), message);
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                Log.debug("APIRequestManager.loadImage() > Error will be thrown", e);
                throw new IOException("Unknown Host: " + e.getMessage());
            } else {
                Log.debug("APIRequestManager.loadImage() > Error will be thrown", e);
                throw new IOException("Error in the server communication:" + e.getMessage());
            }
        } catch (URISyntaxException e) {
            Log.error("Error creating jpip request", e);
        }
        return null;
    }

    /**
     * Sends an request to the server to compute where the image series is
     * located on the server. The address of the file will be returned.
     *
     * @param observatory
     *            observatory of the requested image series.
     * @param instrument
     *            instrument of the requested image series.
     * @param detector
     *            detector of the requested image series.
     * @param measurement
     *            measurement of the requested image series.
     * @param startTime
     *            start time of the requested image series.
     * @param endTime
     *            end time of the requested image series.
     * @param cadence
     *            cadence between to images of the image series.
     * @param message
     *            display error message.
     * @return view of the file which represents the image series on the server.
     * @throws MalformedURLException
     * @throws IOException
     */
    private static View loadImageSeries(String server, String observatory, String instrument, String detector, String measurement, long startTime, long endTime, String cadence, boolean message) throws IOException {
        String fileRequest = server + "?action=getJPX&observatory=" + observatory +
                                      "&instrument=" + instrument +
                                      "&detector=" + detector +
                                      "&measurement=" + measurement +
                                      "&startTime=" + TimeUtils.apiDateFormat.format(startTime) +
                                      "&endTime=" + TimeUtils.apiDateFormat.format(endTime);
        if (cadence != null) {
            fileRequest += "&cadence=" + cadence;
        }
        String jpipRequest = fileRequest + "&jpip=true&verbose=true&linked=true";
        // Log.debug("APIRequestManager.loadImageSeries() > request url: " + jpipRequest);

        // get URL from server where file with image series is located
        try {
            return requestData(new URL(jpipRequest), new URI(fileRequest), message);
        } catch (IOException e) {
            if (e instanceof UnknownHostException) {
                Log.debug("APIRequestManager.loadImageSeries() > Error will be thrown", e);
                throw new IOException("Unknown Host: " + e.getMessage());
            } else {
                Log.debug("APIRequestManager.loadImageSeries() > Error will be thrown", e);
                throw new IOException("Error in the server communication:" + e.getMessage());
            }
        } catch (URISyntaxException e) {
            Log.error("Error creating jpip request", e);
        }

        return null;
    }

    /**
     * Sends an request to the server to compute where the image series is
     * located on the server together with meta information like timestamps for
     * the frames.
     * <p>
     * After processing the request it will if the server gives a sufficient
     * reply, i.e. "uri" is set it will try to load the result with
     * {@link #newLoad(URI, URI, boolean)}. It will display and log any further
     * message from the server.
     * <p>
     * Returns the corresponding View for the file.
     *
     * @param jpipRequest
     *            The http request url which is sent to the server
     * @param downloadUri
     *            the http uri from which the whole file can be downloaded
     * @param errorMessage
     *            display error message
     * @return The View corresponding to the file whose location was returned by
     *         the server
     */
    private static View requestData(URL jpipRequest, URI downloadUri, boolean errorMessage) throws IOException {
        try {
            APIResponse response = new APIResponse(new DownloadStream(jpipRequest).getInput());

            // Could we handle the answer from the server
            if (!response.hasData()) {
                Log.error("Could not understand server answer from " + jpipRequest);
                if (errorMessage) {
                    Message.err("Invalid Server reply", "The server data could not be parsed.", false);
                }
                return null;
            }
            // Just some error from the server
            String error = response.getString("error");
            if (error != null) {
                Log.error("Data query returned error: " + error);
                if (errorMessage) {
                    Message.err("Error getting the data", Message.formatMessageString(error), false);
                }
                return null;
            }

            // Try to load
            if (response.getURI() != null) {
                // The server wants to load us the data
                String message = response.getString("message");
                if (message != null && errorMessage) {
                    Message.warn("Warning", Message.formatMessageString(message));
                }
                APIResponseDump.getSingletonInstance().putResponse(response);
                return loadView(response.getURI(), downloadUri);
            } else {
                // We did not get a reply to load data or no reply at all
                String message = response.getString("message");
                if (message != null) {
                    Log.error("No data to load returned from " + jpipRequest);
                    Log.error("Server message: " + message);
                    if (errorMessage) {
                        Message.err("Server could not return data", Message.formatMessageString(message), false);
                    }
                } else {
                    Log.error("Did not find uri in reponse to " + jpipRequest);
                    if (errorMessage) {
                        Message.err("No data source response", "While quering the data source, the server did not provide an answer.", false);
                    }
                }
            }
        } catch (SocketTimeoutException e) {
            Log.error("Socket timeout while requesting jpip url", e);
            Message.err("Socket timeout", "Socket timeout while requesting jpip url", false);
        }
        return null;
    }

    /**
     * Method does remote opening. If image series, file is downloaded. If
     * single frame, file is opened via JPIP on delphi.nascom.nasa.gov:8090.
     *
     * @param cadence
     *            cadence between two frames (null for single images).
     * @param startTime
     *            start time of the requested image
     * @param endTime
     *            end time of the requested image (empty for single images).
     * @param observatory
     *            observatory of the requested image
     * @param instrument
     *            instrument of the requested image
     * @param detector
     *            detector of the requested image.
     * @param measurement
     *            measurement of the requested image.
     * @param message
     *            display error message
     * @return new view
     * @throws IOException
     */
    public static View requestAndOpenRemoteFile(String server, String cadence, long startTime, long endTime, String observatory, String instrument, String detector, String measurement, boolean message) throws IOException {
        if (server == null) // use default
            server = Settings.getSingletonInstance().getProperty("API.jp2images.path");

        if (startTime == endTime) {
            return loadImage(server, observatory, instrument, detector, measurement, startTime, message);
        } else {
            return loadImageSeries(server, observatory, instrument, detector, measurement, startTime, endTime, cadence, message);
        }
    }

    /**
     * Loads a new image located at the given URI.
     *
     * <p>
     * Depending on the file type, a different implementation of the View is
     * chosen. If there is no implementation available for the given type, an
     * exception is thrown.
     *
     * @param uri
     *            URI representing the location of the image
     * @param downloadURI
     *            URI from which the whole file can be downloaded
     * @return View containing the image
     * @throws IOException
     *             if anything went wrong (e.g. type not supported, image not
     *             found, etc.)
     */
    public static View loadView(URI uri, URI downloadURI) throws IOException {
        if (uri == null || uri.getScheme() == null) {
            throw new IOException("Invalid URI");
        }

        try {
            String down = downloadURI.toString().toLowerCase(Locale.ENGLISH);
            if (down.endsWith(".fits") || down.endsWith(".fts")) {
                return new FITSView(uri);
            } else if (down.endsWith(".png") || down.endsWith(".jpg") || down.endsWith(".jpeg")) {
                 return new SimpleImageView(uri);
            } else {
                JP2Image jp2Image;
                if (down.contains("callisto"))
                    jp2Image = new JP2ImageCallisto(uri, downloadURI);
                else
                    jp2Image = new JP2Image(uri, downloadURI);
                return EventDispatchQueue.invokeAndWait(new AllocateJP2View(jp2Image));
            }
        } catch (InterruptedException e) {
            // nothing
        } catch (Exception e) {
            Log.debug("APIRequestManager.loadView(\"" + uri + "\", \"" + downloadURI + "\") ", e);
            throw new IOException(e.getMessage());
        }
        return null;
    }

    private static class AllocateJP2View implements Callable<JP2View> {
        private final AtomicReference<JP2Image> refJP2Image = new AtomicReference<JP2Image>();

        public AllocateJP2View(JP2Image jp2Image) {
            refJP2Image.set(jp2Image);
        }

        @Override
        public JP2View call() {
            JP2View view;
            JP2Image jp2Image = refJP2Image.get();
            if (jp2Image instanceof JP2ImageCallisto) {
                view = new JP2ViewCallisto();
            } else {
                view = new JP2View();
            }
            view.setJP2Image(jp2Image);

            return view;
        }
    }

}
