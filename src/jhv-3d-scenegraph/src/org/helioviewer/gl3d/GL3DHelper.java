package org.helioviewer.gl3d;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.IllegalCoordinateVectorException;

/**
 * Helper class to convert WCS CoordinateVectors to mathematically used
 * coordinates.
 * 
 * TODO: let GL3DVec3d implement a CoordinateVector interface to get rid of this
 * class
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DHelper {

    public static GL3DVec3d toVec(CoordinateVector coordinate) {
        if (coordinate.getCoordinateSystem().getDimensions() != 3) {
            throw new IllegalCoordinateVectorException("Cannot Create GL3DVec3d from CoordinateVector with " + coordinate.getCoordinateSystem().getDimensions() + " dimensions");
        }
        GL3DVec3d vec = new GL3DVec3d();
        vec.x = coordinate.getValue(0);
        vec.y = coordinate.getValue(1);
        vec.z = coordinate.getValue(2);
        return vec;
    }

    public static CoordinateVector createCoordinate(GL3DVec3d vec, CoordinateSystem coordinateSystem) {
        if (coordinateSystem.getDimensions() != 3) {
            throw new IllegalCoordinateVectorException("Cannot Create CoordinateVector from GL3DVec3d with " + coordinateSystem.getDimensions() + " dimensions");
        }
        return coordinateSystem.createCoordinateVector(vec.x, vec.y, vec.z);
    }
}
