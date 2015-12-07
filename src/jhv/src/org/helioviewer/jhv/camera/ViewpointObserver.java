package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

class ViewpointObserver extends Viewpoint {

    @Override
    void update(JHVDate date) {
        time = date;

        View view = Layers.getActiveView();
        if (view == null) {
            Position.Q p = Sun.getEarthQuat(time);
            orientation = p.q;
            distance = p.rad;
        } else {
            MetaData m = view.getMetaData(time);
            orientation = m.getRotationObs();
            distance = m.getDistanceObs();
        }
    }

    @Override
    CameraOptionPanel getOptionPanel() {
        return null;
    }

}
