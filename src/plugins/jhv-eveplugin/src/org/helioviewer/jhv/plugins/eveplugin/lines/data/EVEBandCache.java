package org.helioviewer.jhv.plugins.eveplugin.lines.data;

import java.awt.Rectangle;
import java.util.HashMap;

import org.helioviewer.jhv.base.interval.Interval;

/**
 *
 * @author Stephan Pagel
 * */
public class EVEBandCache {

    // //////////////////////////////////////////////////////////////////////////////
    // Definitions
    // //////////////////////////////////////////////////////////////////////////////

    private final HashMap<Band, EVECache> cacheMap = new HashMap<Band, EVECache>();

    // //////////////////////////////////////////////////////////////////////////////
    // Methods
    // //////////////////////////////////////////////////////////////////////////////

    public void add(Band band, float[] values, long[] dates) {
        EVECache cache = cacheMap.get(band);
        if (cache == null) {
            cache = new EVECache();
            cacheMap.put(band, cache);
        }

        cache.add(values, dates);
    }

    public EVEValues getValuesInInterval(final Band band, final Interval interval, Rectangle plotArea) {
        EVECache cache = cacheMap.get(band);
        if (cache == null) {
            return new EVEValues();
        }

        return cache.getValuesInInterval(interval, plotArea);

    }

    public boolean hasDataInInterval(Band band, Interval selectedInterval) {
        EVECache cache = cacheMap.get(band);
        if (cache == null) {
            return false;
        } else {
            return cache.hasDataInInterval(selectedInterval);
        }
    }

}
