package org.helioviewer.viewmodel.view.fitsview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

import org.helioviewer.base.math.Interval;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.viewmodel.imagedata.ColorMask;
import org.helioviewer.viewmodel.imagedata.ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.metadata.MetaDataConstructor;
import org.helioviewer.viewmodel.metadata.ObserverMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.viewport.StaticViewport;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.StaticViewportImageSize;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSizeAdapter;

/**
 * Implementation of ImageInfoView for FITS images.
 *
 * <p>
 * For further informations about the behavior of this view,
 * {@link ImageInfoView} is a good start to get into the concept.
 *
 * @author Andreas Hoelzl
 * */
public class JHVFITSView extends AbstractView implements RegionView, SubimageDataView, ImageInfoView, MetaDataView {

    protected Viewport viewport;
    protected Region region;
    protected FITSImage fits;
    protected ImageData subImageData;
    protected MetaData m;
    private final URI uri;
    private final Interval<Date> range;

    /**
     * Constructor which loads a fits image from a given URI.
     *
     * @param uri
     *            Specifies the location of the FITS file.
     * @throws IOException
     *             when an error occurred during reading the fits file.
     * */
    public JHVFITSView(URI uri, Interval<Date> range) throws IOException {

        this.uri = uri;
        this.range = range;
        if (!uri.getScheme().equalsIgnoreCase("file"))
            throw new IOException("FITS does not support the " + uri.getScheme() + " protocol");

        try {
            fits = new FITSImage(uri.toURL().toString());
        } catch (Exception e) {
            throw new IOException("FITS image data cannot be accessed.");
        }

        initFITSImageView();
    }

    /**
     * Constructor which uses a given fits image.
     *
     * @param fits
     *            FITSImage object which contains the image data
     * @param uri
     *            Specifies the location of the FITS file.
     * */
    public JHVFITSView(FITSImage fits, URI uri, Interval<Date> range) {
        this.uri = uri;
        this.fits = fits;
        this.range = range;
        initFITSImageView();

        initFITSImageView();
    }

    /**
     * Initializes global variables.
     */
    private void initFITSImageView() {
        m = MetaDataConstructor.getMetaData(fits);

        BufferedImage bi = fits.getImage(0, 0, fits.getPixelHeight(), fits.getPixelWidth());
        if (bi.getColorModel().getPixelSize() <= 8) {
            subImageData = new SingleChannelByte8ImageData(bi, new ColorMask());
        } else if (bi.getColorModel().getPixelSize() <= 16) {
            subImageData = new SingleChannelShortImageData(bi.getColorModel().getPixelSize(), bi, new ColorMask());
        } else {
            subImageData = new ARGBInt32ImageData(bi, new ColorMask());
        }
        subImageData.setMETADATA(m);

        region = StaticRegion.createAdaptedRegion(m.getPhysicalLowerLeft().getX(), m.getPhysicalLowerLeft().getY(), m.getPhysicalImageSize().getX(), m.getPhysicalImageSize().getY());
        viewport = StaticViewport.createAdaptedViewport(100, 100);
    }

    /**
     * Updates the sub image depending on the current region.
     *
     * @param event
     *            Event that belongs to the request.
     * */
    private void updateImageData(ChangeEvent event) {
        Region r = region;
        m = getMetaData();

        double imageMeterPerPixel = m.getPhysicalImageWidth() / fits.getPixelWidth();
        long imageWidth = Math.round(r.getWidth() / imageMeterPerPixel);
        long imageHeight = Math.round(r.getHeight() / imageMeterPerPixel);

        Vector2dInt imagePostion = ViewHelper.calculateInnerViewportOffset(r, m.getPhysicalRegion(), new ViewportImageSizeAdapter(new StaticViewportImageSize(fits.getPixelWidth(), fits.getPixelHeight())));

        BufferedImage bi = fits.getImage(imagePostion.getX(), imagePostion.getY(), (int) imageHeight, (int) imageWidth);
        if (bi.getColorModel().getPixelSize() <= 8) {
            subImageData = new SingleChannelByte8ImageData(bi, new ColorMask());
        } else if (bi.getColorModel().getPixelSize() <= 16) {
            subImageData = new SingleChannelShortImageData(bi.getColorModel().getPixelSize(), bi, new ColorMask());
        } else {
            subImageData = new ARGBInt32ImageData(bi, new ColorMask());
        }

        event.addReason(new SubImageDataChangedReason(this));
        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     * */
    public boolean setViewport(Viewport v, ChangeEvent event) {
        // check if viewport has changed
        if (viewport != null && v != null && viewport.getWidth() == v.getWidth() && viewport.getHeight() == v.getHeight())
            return false;

        viewport = v;
        event.addReason(new ViewportChangedReason(this, v));
        notifyViewListeners(event);

        return true;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public Region getRegion() {

        return region;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public boolean setRegion(Region r, ChangeEvent event) {
        // check if region has changed
        if ((region == r) || (region != null && r != null && region.getCornerX() == r.getCornerX() && region.getCornerY() == r.getCornerY() && region.getWidth() == r.getWidth() && region.getHeight() == r.getHeight()))
            return false;

        region = r;
        event.addReason(new RegionChangedReason(this, r));
        updateImageData(event);

        return true;
    }

    /**
     * Returns the header information as XML string.
     *
     * @return XML string including all header information.
     * */
    public String getHeaderAsXML() {
        return fits.getHeaderAsXML();
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public MetaData getMetaData() {
        return m;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public ImageData getSubimageData() {
        return subImageData;
    }

    /**
     * Returns the FITS image managed by this class.
     *
     * @return FITS image.
     */
    public FITSImage getFITSImage() {
        return fits;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public String getName() {
        if (m instanceof ObserverMetaData) {
            ObserverMetaData observerMetaData = (ObserverMetaData) m;
            return observerMetaData.getFullName();
        } else {
            String name = uri.getPath();
            return name.substring(name.lastIndexOf('/') + 1, name.lastIndexOf('.'));
        }
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public URI getUri() {
        return uri;
    }

    /**
     * {@inheritDoc}
     * */
    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public URI getDownloadURI() {
        return uri;
    }

    @Override
    public Interval<Date> getDateRange() {
        return this.range;
    }

}
