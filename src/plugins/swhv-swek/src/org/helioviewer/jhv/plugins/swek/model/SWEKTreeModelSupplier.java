package org.helioviewer.jhv.plugins.swek.model;

import org.helioviewer.jhv.data.datatype.event.SWEKSupplier;

/**
 * SWEKTree representation of a SWEKSupplier.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class SWEKTreeModelSupplier extends AbstractSWEKTreeModelElement {

    /** The SWEK supplier for this SWEK tree supplier */
    private SWEKSupplier swekSupplier;

    /**
     * Creates a SWEK tree supplier for the given SWEkSupplier.
     * 
     * @param swekSupplier
     *            The SWEK supplier for this SWEK tree supplier
     */
    public SWEKTreeModelSupplier(SWEKSupplier swekSupplier) {
        super(false);
        this.swekSupplier = swekSupplier;
    }

    /**
     * Gets the SWEK supplier.
     * 
     * @return The SWEK supplier
     */
    public SWEKSupplier getSwekSupplier() {
        return swekSupplier;
    }

    /**
     * Sets the SWEK supplier.
     * 
     * @param swekSupplier
     *            The SWEK supplier
     */
    public void setSwekSupplier(SWEKSupplier swekSupplier) {
        this.swekSupplier = swekSupplier;
    }

}
