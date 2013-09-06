package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraZoomAnimation;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;

/**
 * Action that zooms out, which increases the {@link GL3DCamera}'s distance to
 * the sun.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DZoomOutAction extends ZoomOutAction {

    private static final long serialVersionUID = 1L;

    public GL3DZoomOutAction(boolean small) {
        super(small);
    }

    public void actionPerformed(ActionEvent e) {
        GL3DCamera camera = GL3DCameraSelectorModel.getInstance().getCurrentCamera();

        double distance = -camera.getDistanceToSunSurface() / 2;
        GL3DCameraSelectorModel.getInstance().getCurrentCamera().addCameraAnimation(new GL3DCameraZoomAnimation(distance, 500));
    }

}
