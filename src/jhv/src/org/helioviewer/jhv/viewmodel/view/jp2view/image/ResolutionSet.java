package org.helioviewer.jhv.viewmodel.view.jp2view.image;

import java.awt.Rectangle;

/**
 * A class describing the available resolution levels for a given image. It
 * supplies several simple methods to aid in selecting appropriate zoom levels.
 *
 * @author caplins
 */
public class ResolutionSet {

    /**
     * An array containing the available resolutions. The index's represent the
     * num of discardLayers
     */
    private final ResolutionLevel[] resolutions;

    /**
     * Constructor. Takes the number of resolution levels in the associated
     * image.
     *
     * @param _numResolutions
     */
    public ResolutionSet(int _numResolutions) {
        resolutions = new ResolutionLevel[_numResolutions];
    }

    /**
     * Adds a resolution level. Used while setting the object up. This method is
     * really the only place a ResolutionLevel object is ever created. NOTE:
     * Normally the resolution levels will always have an origin @ (0,0) and
     * hence a Dimension object would be enough to describe the layer bounds. In
     * some situations this might not be true though, and for extendability I
     * use a rectangle object instead of a Dimension object (even though I don't
     * think I ever explicitly use the origin).
     *
     * @param _discardLayer
     * @param _dims
     */
    public void addResolutionLevel(int _discardLayer, Rectangle _dims) {
        resolutions[_discardLayer] = new ResolutionLevel(_discardLayer, _dims);
    }

    public ResolutionLevel getResolutionLevel(int _index) {
        return resolutions[_index];
    }

    public ResolutionLevel getPreviousResolutionLevel(int w, int h) {
        int idx = 0;
        for (int i = 0; i < resolutions.length; i++) {
            idx = i;
            if (resolutions[i].dims.width <= w && resolutions[i].dims.height <= h)
                break;
        }
        return resolutions[idx];
    }

    public ResolutionLevel getNextResolutionLevel(int w, int h) {
        for (int i = 1; i < resolutions.length; ++i) {
            if (resolutions[i].dims.width < w || resolutions[i].dims.height < h)
                return resolutions[i - 1];
        }
        return resolutions[resolutions.length - 1];
    }

    public int getMaxResolutionLevels() {
        return resolutions.length - 1;
    }

    /**
     * A simple class describing a Resolution level for a given image. Note
     * though this class is public it's constructor is private. The
     * ResolutionSet object can be considered to be the ResolutionLevel
     * 'factory'. The ResolutionLevel object is also immutable.
     *
     * @author caplins
     *
     */
    public static class ResolutionLevel {

        /** The zoom level of the Resolution */
        private final int discardLayers;

        /** The bounds of the Resolution as a Rectangle */
        private final Rectangle dims;

        /** Private constructor. */
        private ResolutionLevel(int _discardLayers, Rectangle _dims) {
            if (_dims == null)
                throw new NullPointerException();
            discardLayers = _discardLayers;
            dims = (Rectangle) _dims.clone();
        }

        /** Returns the number of discardLayers */
        public int getZoomLevel() {
            return discardLayers;
        }

        /** Returns the zoom in percent */
        public float getZoomPercent() {
            return 1f / (1 << discardLayers);
        }

        /** Returns bounds of the resolution as a rectangle. */
        public Rectangle getResolutionBounds() {
            return (Rectangle) dims.clone();
        }

        /**
         * The equals method. Since these objects are immutable, can only be
         * instantiated in this class, and are not cloneable you can almost
         * certain do reference comparisons using the ==, but since I am
         * paranoid I overridden the equals method.
         */

        @Override
        public boolean equals(Object o) {
            if (o instanceof ResolutionLevel) {
                ResolutionLevel r = (ResolutionLevel) o;
                return discardLayers == r.discardLayers && dims.equals(r.dims);
            }
            return false;
        }

        /**
         * Clone is NOT supported.
         *
         * @throws CloneNotSupportedException
         */

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }

        @Override
        public String toString() {
            return "[[ZoomPercent=" + getZoomPercent() + "][ZoomLevel=" + discardLayers + "][ZoomDims=" + dims.toString() + "]]";
        }

    }

}
