package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;

class ViewpointEarth extends Viewpoint {

    @Override
    void update(JHVDate date) {
        time = date;

        Position.L p = Sun.getEarth(time.milli);
        orientation = new Quat(p.lat, p.lon);
        distance = p.rad;
    }

    @Override
    CameraOptionPanel getOptionPanel() {
        return null;
    }

}
