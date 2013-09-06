package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraPanAnimation;
import org.helioviewer.gl3d.camera.GL3DCameraZoomAnimation;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;

/**
 * Action that zooms in or out to fit the currently displayed image layers to
 * the displayed viewport. For 3D this results in a change in the
 * {@link GL3DCamera}'s distance to the sun.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DZoomFitAction extends ZoomFitAction {

    private static final long serialVersionUID = 1L;

    public GL3DZoomFitAction(boolean small) {
        super(small);
    }

    public void actionPerformed(ActionEvent e) {
        View view = LayersModel.getSingletonInstance().getActiveView();
        GL3DCamera camera = GL3DCameraSelectorModel.getInstance().getCurrentCamera();
        if (view != null) {
            Region region = view.getAdapter(MetaDataView.class).getMetaData().getPhysicalRegion();
            if (region != null) {
                double halfWidth = region.getWidth() / 2;
                double halfFOVRad = Math.toRadians(camera.getFOV() / 2.0);
                double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);
                distance = -distance - camera.getZTranslation();
                Log.debug("GL3DZoomFitAction: Distance = " + distance + " Existing Distance: " + camera.getZTranslation());
                camera.addCameraAnimation(new GL3DCameraZoomAnimation(distance, 500));
                camera.addCameraAnimation(new GL3DCameraPanAnimation(camera.getTranslation().copy().negate()));
            }
        }
    }

}
