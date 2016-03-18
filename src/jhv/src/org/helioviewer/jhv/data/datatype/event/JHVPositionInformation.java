package org.helioviewer.jhv.data.datatype.event;

import java.util.List;

import org.helioviewer.jhv.base.math.Vec3;

/**
 * Interface defining the position of a JHVEvent.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
public interface JHVPositionInformation {

    /**
     * Gets a list with coordinates defining the bounding box of the event.
     *
     * @return a list with coordinates defining the bounding box
     */
    public abstract List<Vec3> getBoundBox();

    /**
     * Gets the central point of the event.
     *
     * @return the central point.
     */
    public abstract Vec3 centralPoint();

    /**
     * Gets the bound coordinates. Finer grained than the bound box.
     *
     * @return the bound coordinates
     */
    public abstract List<Vec3> getBoundCC();

}
