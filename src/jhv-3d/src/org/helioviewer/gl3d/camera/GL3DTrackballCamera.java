package org.helioviewer.gl3d.camera;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.HeliocentricCartesianCoordinateSystem;

/**
 * The trackball camera provides a trackball rotation behavior (
 * {@link GL3DTrackballRotationInteraction}) when in rotation mode. It is
 * currently the default camera.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DTrackballCamera extends GL3DCamera {
    public static final double DEFAULT_CAMERA_DISTANCE = 12 * Constants.SunRadius;

    private GL3DRay lastMouseRay;

    // protected CoordinateSystem viewSpaceCoordinateSystem = new
    // HEECoordinateSystem();
    protected CoordinateSystem viewSpaceCoordinateSystem = new HeliocentricCartesianCoordinateSystem();
    // protected CoordinateSystem viewSpaceCoordinateSystem = new
    // HEEQCoordinateSystem(new Date());
    // protected CoordinateSystem viewSpaceCoordinateSystem = new
    // SolarSphereCoordinateSystem();

    private GL3DTrackballRotationInteraction rotationInteraction;
    private GL3DPanInteraction panInteraction;
    private GL3DZoomBoxInteraction zoomBoxInteraction;

    protected GL3DSceneGraphView sceneGraphView;

    protected GL3DInteraction currentInteraction;

    public GL3DTrackballCamera(GL3DSceneGraphView sceneGraphView) {
        this.sceneGraphView = sceneGraphView;
        this.rotationInteraction = new GL3DTrackballRotationInteraction(this, sceneGraphView);
        this.panInteraction = new GL3DPanInteraction(this, sceneGraphView);
        this.zoomBoxInteraction = new GL3DZoomBoxInteraction(this, sceneGraphView);

        this.currentInteraction = this.rotationInteraction;
    }

    public void applyCamera(GL3DState state) {
        // ((HEEQCoordinateSystem)this.viewSpaceCoordinateSystem).setObservationDate(state.getCurrentObservationDate());
        super.applyCamera(state);
    }

    public void setSceneGraphView(GL3DSceneGraphView sceneGraphView) {
        this.sceneGraphView = sceneGraphView;
    }

    public void reset() {
        this.currentInteraction.reset(this);
    }

    public double getDistanceToSunSurface() {
        return -this.getCameraTransformation().translation().z;
    }

    public GL3DInteraction getPanInteraction() {
        return this.panInteraction;
    }

    public GL3DInteraction getRotateInteraction() {
        return this.rotationInteraction;
    }

    public GL3DInteraction getCurrentInteraction() {
        return this.currentInteraction;
    }

    public void setCurrentInteraction(GL3DInteraction currentInteraction) {
        this.currentInteraction = currentInteraction;
    }

    public GL3DInteraction getZoomInteraction() {
        return this.zoomBoxInteraction;
    }

    public GL3DRay getLastMouseRay() {
        return lastMouseRay;
    }

    public CoordinateSystem getViewSpaceCoordinateSystem() {
        return this.viewSpaceCoordinateSystem;
    }

    public GL3DMat4d getVM() {
        GL3DMat4d c = this.getCameraTransformation().copy();
        return c;
    }

    public String getName() {
        return "Trackball";
    }
}