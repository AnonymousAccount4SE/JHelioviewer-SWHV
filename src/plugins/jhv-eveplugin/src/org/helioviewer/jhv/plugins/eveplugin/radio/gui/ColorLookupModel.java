package org.helioviewer.jhv.plugins.eveplugin.radio.gui;

import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.HashSet;

import org.helioviewer.jhv.gui.filters.lut.LUT;

public class ColorLookupModel {

    private static ColorLookupModel instance;

    private LUT lut;

    private IndexColorModel indexColorModel;

    private static final HashSet<ColorLookupModelListener> listeners = new HashSet<ColorLookupModelListener>();

    private ColorLookupModel() {
        lut = LUT.getStandardList().get("Rainbow 2");
        indexColorModel = createIndexColorModelFromLUT(lut);
    }

    public static ColorLookupModel getInstance() {
        if (instance == null) {
            instance = new ColorLookupModel();
        }
        return instance;
    }

    public void addFilterModelListener(ColorLookupModelListener listener) {
        listeners.add(listener);
    }

    public void removeFilterModelListener(ColorLookupModelListener listener) {
        listeners.remove(listener);
    }

    public void setLUT(LUT newLUT) {
        lut = newLUT;
        indexColorModel = createIndexColorModelFromLUT(lut);
        fireLUTChanged();
    }

    private void fireLUTChanged() {
        for (ColorLookupModelListener l : listeners) {
            l.colorLUTChanged();
        }
    }

    public IndexColorModel getColorModel() {
        return indexColorModel;
    }

    public LUT getLut() {
        return lut;
    }

    private IndexColorModel createIndexColorModelFromLUT(LUT lut2) {
        return new IndexColorModel(8, lut2.getLut8().length, lut2.getLut8(), 0, false, -1, DataBuffer.TYPE_BYTE);
    }

}
