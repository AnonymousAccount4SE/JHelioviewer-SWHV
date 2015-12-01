package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import kdu_jni.Jp2_palette;
import kdu_jni.Jp2_family_src;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_istream_ref;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.base.Region;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.Viewpoint;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.filters.lut.DefaultTable;
import org.helioviewer.jhv.gui.filters.lut.LUT;
import org.helioviewer.jhv.io.APIResponseDump;
import org.helioviewer.jhv.viewmodel.imagecache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.imagecache.LocalImageCacheStatus;
import org.helioviewer.jhv.viewmodel.imagecache.RemoteImageCacheStatus;
import org.helioviewer.jhv.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.jhv.viewmodel.view.ViewROI;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduEngine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;

/**
 * This class can open JPEG2000 images. Modified to improve the JPIP
 * communication.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Juan Pablo
 */
public class JP2Image {

    /** An array of the file extensions this class currently supports */
    private static final String[] SUPPORTED_EXTENSIONS = { ".JP2", ".JPX" };

    /** This is the URI that uniquely identifies the image. */
    private final URI uri;

    /** This is the URI from whch the whole file can be downloaded via http */
    private final URI downloadURI;

    /** This is the object in which all transmitted data is stored */
    private JHV_Kdu_cache cacheReader;
    private Kdu_cache cacheRender;

    /** The number of composition layers for the image. */
    private int frameCount;

    private int numLUTs = 0;
    private int[] builtinLUT = null;

    /** An object with all the resolution layer information. */
    protected ResolutionSet resolutionSet;
    private int resolutionSetCompositionLayer = -1;

    /**
     * This is a little tricky variable to specify that the file contains
     * multiple frames
     */
    private boolean isJpx = false;

    protected MetaData[] metaDataList;

    private JPIPSocket socket;

    /**
     * The number of output components (should be the number of 8 bits
     * channels). Currently only value of 1 and 3 are supported (corresponding
     * to grayscale and RGB images).
     */
    private int numComponents;

    private ImageCacheStatus imageCacheStatus;

    // Reader
    public enum ReaderMode {
        NEVERFIRE, ONLYFIREONCOMPLETE, ALWAYSFIREONNEWDATA, SIGNAL_RENDER_ONCE
    }

    private J2KReader reader;
    private ReaderMode readerMode = ReaderMode.ALWAYSFIREONNEWDATA;

    /**
     * Constructor
     *
     * <p>
     * To open an image an URI must be given and this should be made unique. All
     * initialization for this object is done in the constructor or in methods
     * called by the constructor. Either the constructor throws an exception or
     * the image was opened successfully.
     *
     * @param newUri
     *            URI representing the location of the image
     * @param downloadURI
     *            In case the file should be downloaded to the local filesystem,
     *            use this URI as the source.
     * @throws IOException
     * @throws JHV_KduException
     */
    public JP2Image(URI _uri, URI _downloadURI) throws Exception {
        uri = _uri;
        downloadURI = _downloadURI;

        String name = uri.getPath().toUpperCase();
        boolean supported = false;
        for (String ext : SUPPORTED_EXTENSIONS)
            if (name.endsWith(ext))
                supported = true;
        if (!supported)
            throw new JHV_KduException("File extension not supported");

        isJpx = name.endsWith(".JPX");

        try {
            String scheme = uri.getScheme().toUpperCase();
            if (scheme.equals("JPIP")) {
                cacheReader = new JHV_Kdu_cache();
                cacheRender = new Kdu_cache();
                cacheRender.Attach_to(cacheReader);
                // cache.Set_preferred_memory_limit(60 * 1024 * 1024);
                initRemote(cacheReader);
            } else if (scheme.equals("FILE")) {
                // nothing
            } else
                throw new JHV_KduException(scheme + " scheme not supported!");

            KakaduEngine kdu = new KakaduEngine(cacheReader, uri, null);

            createKakaduMachinery(kdu.getJpxSource(), kdu.getCompositor());
            configureLUT(kdu.getJpxSource());

            metaDataList = new MetaData[frameCount];
            KakaduUtils.cacheMetaData(kdu.getFamilySrc(), metaDataList);

            kdu.destroy();
        } catch (KduException ex) {
            ex.printStackTrace();
            throw new JHV_KduException("Failed to create Kakadu machinery: " + ex.getMessage(), ex);
        }
    }

    /**
     * Initializes the Jp2_family_src for a remote file. (JPIP comms
     * happen here).
     *
     * @throws JHV_KduException
     * @throws IOException
     */
    private void initRemote(JHV_Kdu_cache cache) throws JHV_KduException {
        // Create the JPIP-socket necessary for communications
        JPIPResponse res;
        socket = new JPIPSocket();

        try {
            // Connect to the JPIP server and add the first response to cache
            res = (JPIPResponse) socket.connect(uri);
            cache.addJPIPResponseData(res, null);

            // Download the necessary initial data
            boolean initialDataLoaded = false;
            int numTries = 0;

            do {
                try {
                    KakaduUtils.downloadInitialData(socket, cache);
                    initialDataLoaded = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    numTries++;
                    socket.close();
                    socket = new JPIPSocket();
                    socket.connect(uri);
                }
            } while (!initialDataLoaded && numTries < 5);
        } catch (SocketTimeoutException e) {
            throw new JHV_KduException("Timeout while communicating with the server:" + System.getProperty("line.separator") + e.getMessage(), e);
        } catch (IOException e) {
            throw new JHV_KduException("Error in the server communication:" + System.getProperty("line.separator") + e.getMessage(), e);
        } finally {
            Timer timer = new Timer("WaitForCloseSocket");
            timer.schedule(new TimerTask() {
                @Override
                public synchronized void run() {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.error(">> JP2Image.initRemote() > Error closing socket.", e);
                        }
                        socket = null;
                    }
                }
            }, 5000);
        }
    }

    private void configureLUT(Jpx_source jpxSrc) throws KduException {
        Jpx_codestream_source stream = jpxSrc.Access_codestream(0);
        if (!stream.Exists()) {
            throw new KduException(">> stream doesn't exist");
        }

        Jp2_palette palette = stream.Access_palette();

        numLUTs = palette.Get_num_luts();
        if (numLUTs == 0)
            return;

        int[] lut = new int[palette.Get_num_entries()];
        float[] red = new float[lut.length];
        float[] green = new float[lut.length];
        float[] blue = new float[lut.length];

        palette.Get_lut(0, red);
        palette.Get_lut(1, green);
        palette.Get_lut(2, blue);

        for (int i = 0; i < lut.length; i++) {
            lut[i] = 0xFF000000 | ((int) ((red[i] + 0.5f) * 0xFF) << 16) | ((int) ((green[i] + 0.5f) * 0xFF) << 8) | ((int) ((blue[i] + 0.5f) * 0xFF));
        }
        builtinLUT = lut;
    }

    /**
     * Creates the Kakadu objects and sets all the data-members in this object.
     *
     * @throws JHV_KduException
     */
    private void createKakaduMachinery(Jpx_source jpxSrc, Kdu_region_compositor compositor) throws KduException {
        // I create references here so the GC doesn't try to collect the
        // Kdu_dims obj
        Kdu_dims ref1 = new Kdu_dims(), ref2 = new Kdu_dims();

        // A layer must be added to determine the image parameters
        compositor.Add_ilayer(0, ref1, ref2);
        {
            // Retrieve the number of composition layers
            {
                int[] tempVar = new int[1];
                jpxSrc.Count_compositing_layers(tempVar);
                frameCount = tempVar[0];
            }

            Kdu_codestream stream = compositor.Access_codestream(compositor.Get_next_istream(new Kdu_istream_ref(), false, true));
            if (!stream.Exists()) {
                throw new KduException(">> stream doesn't exist");
            }

            // Retrieve the number of components
            {
                // Since it gets tricky here I am just grabbing a bunch of
                // values
                // and taking the max of them. It is acceptable to think
                // that an
                // image is color when its not monochromatic, but not the
                // other way
                // around... so this is just playing it safe.
                Kdu_channel_mapping cmap = new Kdu_channel_mapping();
                cmap.Configure(stream);

                int maxComponents = MathUtils.max(cmap.Get_num_channels(), cmap.Get_num_colour_channels(), stream.Get_num_components(true), stream.Get_num_components(false));

                // numComponents = maxComponents == 1 ? 1 : 3;
                numComponents = maxComponents; // With new file formats we
                // may have 2 components

                cmap.Clear();
                cmap.Native_destroy();
                cmap = null;
            }
            // Cleanup
            stream = null;
        }
        updateResolutionSet(compositor, 0);
        // Remove the layer that was added
        compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);
    }

    private KakaduEngine kduRender;

    Kdu_region_compositor getCompositor(Kdu_thread_env threadEnv) throws KduException, IOException {
        if (kduRender == null) {
            Thread.currentThread().setName("Render " + getName(0));
            kduRender = new KakaduEngine(cacheRender, uri, threadEnv);
        }
        return kduRender.getCompositor();
    }

    void destroyEngine() throws KduException {
        if (kduRender != null) {
            kduRender.destroy();
            kduRender = null;
        }
    }

    protected void startReader(JP2View view) {
        if (cacheReader != null) { // remote
            imageCacheStatus = new RemoteImageCacheStatus(getMaximumFrameNumber());
            try {
                reader = new J2KReader(view, this);
                reader.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            imageCacheStatus = new LocalImageCacheStatus(getMaximumFrameNumber());
        }
    }

    protected void signalReader(JP2ImageParameter params) {
        if (reader != null)
            reader.signalReader(params);
    }

    private JP2ImageParameter oldImageViewParams;

    // Recalculates the image parameters used within the jp2-package
    // Reader signals only for CURRENTFRAME*
    protected JP2ImageParameter calculateParameter(Camera camera, Viewport vp, Viewpoint v, int frameNumber, boolean fromReader) {
        MetaData m = metaDataList[frameNumber];
        Region mr = m.getPhysicalRegion();
        Region r = ViewROI.updateROI(camera, vp, v, m);

        double ratio = 2 * camera.getWidth() / vp.height;
        int totalHeight = (int) (mr.height / ratio);

        ResolutionLevel res = resolutionSet.getNextResolutionLevel(totalHeight, totalHeight);
        int viewportImageWidth = res.getResolutionBounds().width;
        int viewportImageHeight = res.getResolutionBounds().height;

        double currentMeterPerPixel = mr.width / viewportImageWidth;
        int imageWidth = (int) Math.round(r.width / currentMeterPerPixel);
        int imageHeight = (int) Math.round(r.height / currentMeterPerPixel);

        int imagePositionX = +(int) Math.round((r.ulx - mr.ulx) / mr.width * viewportImageWidth);
        int imagePositionY = -(int) Math.round((r.uly - mr.uly) / mr.height * viewportImageHeight);

        SubImage subImage = new SubImage(imagePositionX, imagePositionY, imageWidth, imageHeight, res.getResolutionBounds());

        JP2ImageParameter imageViewParams = new JP2ImageParameter(this, v, subImage, res, frameNumber);

        boolean viewChanged = oldImageViewParams == null ||
                              !(imageViewParams.subImage.equals(oldImageViewParams.subImage) &&
                                imageViewParams.resolution.equals(oldImageViewParams.resolution));
        // ping reader
        if (viewChanged) {
            signalReader(imageViewParams);
        }

        // if (!fromReader && jp2Image.getImageCacheStatus().getImageStatus(frameNumber) == CacheStatus.COMPLETE && newImageViewParams.equals(oldImageViewParams)) {
        //    Displayer.display();
        //    return null;
        //}

        oldImageViewParams = imageViewParams;

        return imageViewParams;
    }

    /**
     * Sets the reader mode.
     *
     * <p>
     * The options are:
     * <ul>
     * <li>NEVERFIRE: The reader basically is disabled and never fires a
     * ChangeEvent.</li>
     * <li>ONLYFIREONCOMPLETE: The reader only fires a ChangeEvent, when the
     * current frame is loaded completely.</li>
     * <li>ALWAYSFIREONNEWDATA: Whenever new data is received, the reader fires
     * a ChangeEvent. This is the default value.</li>
     * </ul>
     *
     * @param readerMode
     * @see #getReaderMode()
     */
    public void setReaderMode(ReaderMode _readerMode) {
        readerMode = _readerMode;
    }

    /**
     * Returns the reader mode.
     *
     * @return Current reader mode.
     * @see #setReaderMode(ReaderMode)
     */
    protected ReaderMode getReaderMode() {
        return readerMode;
    }

    /**
     * Returns whether the image contains multiple frames.
     *
     * A image consisting of multiple frames is also called a 'movie'.
     *
     * @return True, if the image contains multiple frames, false otherwise
     */
    protected boolean isMultiFrame() {
        return isJpx && frameCount > 1;
    }

    /**
     * Returns the URI representing the location of the image.
     *
     * @return URI representing the location of the image.
     */
    protected URI getURI() {
        return uri;
    }

    /**
     * Returns the download uri the image.
     *
     * This is the uri from which the whole file can be downloaded and stored
     * locally
     *
     * @return download uri
     */
    public URI getDownloadURI() {
        return downloadURI;
    }

    protected String getName(int frame) {
        MetaData metaData = metaDataList[frame];
        if (metaData instanceof ObserverMetaData) {
            return ((ObserverMetaData) metaData).getFullName();
        } else {
            String name = getURI().getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
    }

    /**
     * Returns the socket, if in remote mode.
     *
     * The socket is returned only one time. After calling this function for the
     * first time, it will always return null.
     *
     * @return Socket connected to the server
     */
    protected JPIPSocket getSocket() {
        if (socket == null)
            return null;

        JPIPSocket output = socket;
        socket = null;
        return output;
    }

    // Returns the number of output components
    protected int getNumComponents() {
        return numComponents;
    }

    public int getMaximumFrameNumber() {
        return frameCount - 1;
    }

    /**
     * Gets the ResolutionSet object that contains the Resolution level
     * information.
     */
    public ResolutionSet getResolutionSet() {
        return resolutionSet;
    }

    private volatile boolean isAbolished = false;

    // if instance was built before cancelling
    @Override
    protected void finalize() {
        if (!isAbolished) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    abolish();
                }
            });
        }
    }

    /**
     * Closes the image out. Destroys all objects and performs cleanup
     * operations. I use the 'abolish' name to distinguish it from what the
     * Kakadu library uses.
     */
    protected void abolish() {
        isAbolished = true;

        if (reader != null) {
            reader.abolish();
            reader = null;
        }

        APIResponseDump.getSingletonInstance().removeResponse(uri);

        try {
            destroyEngine();

            if (cacheRender != null) {
                cacheRender.Close();
                cacheRender.Native_destroy();
            }
            if (cacheReader != null) {
                cacheReader.Close();
                cacheReader.Native_destroy();
            }
        } catch (KduException ex) {
            ex.printStackTrace();
        } finally {
            cacheRender = null;
            cacheReader = null;
        }
    }

    protected boolean updateResolutionSet(Kdu_region_compositor compositor, int compositionLayerCurrentlyInUse) throws KduException {
        if (resolutionSetCompositionLayer == compositionLayerCurrentlyInUse)
            return false;

        Kdu_codestream stream = compositor.Access_codestream(compositor.Get_next_istream(new Kdu_istream_ref(), false, true));
        if (!stream.Exists()) {
            throw new KduException(">> stream doesn't exist " + compositionLayerCurrentlyInUse);
        }

        resolutionSetCompositionLayer = compositionLayerCurrentlyInUse;

        int maxDWT = stream.Get_min_dwt_levels();

        compositor.Set_scale(false, false, false, 1.0f);
        Kdu_dims dims = new Kdu_dims();
        if (!compositor.Get_total_composition_dims(dims))
            return false;

        if (resolutionSet != null) {
            int pixelWidth = resolutionSet.getResolutionLevel(0).getResolutionBounds().width;
            int pixelHeight = resolutionSet.getResolutionLevel(0).getResolutionBounds().height;

            Kdu_coords size = dims.Access_size();
            if (size.Get_x() == pixelWidth && size.Get_y() == pixelHeight)
                return false;
        }

        resolutionSet = new ResolutionSet(maxDWT + 1);
        resolutionSet.addResolutionLevel(0, KakaduUtils.kdu_dimsToRect(dims));

        for (int i = 1; i <= maxDWT; i++) {
            compositor.Set_scale(false, false, false, 1.0f / (1 << i));
            dims = new Kdu_dims();
            if (!compositor.Get_total_composition_dims(dims))
                break;
            resolutionSet.addResolutionLevel(i, KakaduUtils.kdu_dimsToRect(dims));
        }

        return false;
    }

    /**
     * Deactivates the internal color lookup table for the given composition
     * layer.
     *
     * It is not allowed to call this function for a layer which is not loaded
     * yet.
     *
     * @param numLayer
     *            composition layer to deactivate internal color lookup for
     */
    /*
     * in preservation - not needed void deactivateColorLookupTable(int
     * numLayer) throws KduException { for (int i = 0; i < numLUTs; i++) {
     * jpxSrc.Access_layer(numLayer).Access_channels().Set_colour_mapping(i, 0,
     * -1, numLayer); } }
     */

    // Returns the cache reference
    protected JHV_Kdu_cache getCacheRef() {
        return cacheReader;
    }

    protected ImageCacheStatus getImageCacheStatus() {
        return imageCacheStatus;
    }

    // Returns the built-in color lookup table.
    protected int[] getBuiltinLUT() {
        return builtinLUT;
    }

    protected LUT getAssociatedLUT() {
        MetaData metaData = metaDataList[0];
        if (metaData instanceof HelioviewerMetaData) {
            String colorKey = DefaultTable.getSingletonInstance().getColorTable((HelioviewerMetaData) metaData);
            if (colorKey != null) {
                return LUT.getStandardList().get(colorKey);
            }
        }
        return null;
    }

    // very slow
    public String getXML(int boxNumber) {
        String xml = null;
        try {
            KakaduEngine kduTmp = new KakaduEngine(cacheReader, uri, null);
            xml = KakaduUtils.getXml(kduTmp.getFamilySrc(), boxNumber);
            kduTmp.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return xml;
    }

}
