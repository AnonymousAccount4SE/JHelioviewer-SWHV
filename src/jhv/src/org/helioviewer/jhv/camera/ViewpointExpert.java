package org.helioviewer.jhv.camera;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.view.View;

class ViewpointExpert extends Viewpoint {

    private double currentL = 0.;
    private double currentB = 0.;
    private double currentDistance = Sun.MeanEarthDistance;

    private final Camera camera;
    private final PositionLoad positionLoad = new PositionLoad(this);
    private final CameraOptionPanelExpert expertOptionPanel = new CameraOptionPanelExpert(positionLoad);

    ViewpointExpert(Camera _camera) {
        camera = _camera;
    }

    private JHVDate interpolate(JHVDate date) {
        if (positionLoad.isLoaded()) {
            long currentCameraTime;
            long tLayerStart = 0, tLayerEnd = 0;
            // Active layer times
            View view = Layers.getActiveView();
            if (view != null) {
                tLayerStart = Layers.getStartDate(view).milli;
                tLayerEnd = Layers.getEndDate(view).milli;
            }

            // camera times
            long tPositionStart = positionLoad.getStartTime();
            long tPositionEnd = positionLoad.getEndTime();

            if (tLayerEnd != tLayerStart) {
                currentCameraTime = (long) (tPositionStart + (tPositionEnd - tPositionStart) * (date.milli - tLayerStart) / (double) (tLayerEnd - tLayerStart));
            } else {
                currentCameraTime = tPositionEnd;
            }

            Position.L p = positionLoad.getInterpolatedPosition(currentCameraTime);
            if (p != null) {
                date = new JHVDate(p.milli);
                currentDistance = p.rad;
                currentL = p.lon;
                currentB = p.lat;
            }
        } else {
            Position.L p = Sun.getEarth(date.milli);
            currentDistance = p.rad;
            currentL = 0;
            currentB = p.lat;
        }

        return date;
    }

    void firePositionLoaded(String state) {
        expertOptionPanel.fireLoaded(state);
        camera.refresh();
    }

    @Override
    void update(JHVDate date) {
        time = interpolate(date);

        Position.L p = Sun.getEarth(time.milli);
        orientation = new Quat(currentB, -currentL + p.lon);
        distance = currentDistance;
    }

    @Override
    CameraOptionPanel getOptionPanel() {
        return expertOptionPanel;
    }

}
