package org.helioviewer.gl3d.model;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DGroup;
import org.helioviewer.gl3d.scenegraph.GL3DModel;
import org.helioviewer.gl3d.scenegraph.GL3DShape;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DArrow;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DText;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DSphere;
import org.helioviewer.gl3d.scenegraph.visuals.GL3DSunGrid;

/**
 * Grouping Object for all artificial objects, that is visual assistance objects
 * that do not represent any real data.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DArtificialObjects extends GL3DGroup {

    public GL3DArtificialObjects() {
        super("Artificial Objects");
        GL3DGroup indicatorArrows = new GL3DModel("Arrows", "Arrows indicating the viewspace axes");
        this.addNode(indicatorArrows);
        
        //GL3DSphere blackSphere = new GL3DSphere(0.990*Constants.SunRadius, 20,20, new GL3DVec4f(0.0f, 0.0f, 0.0f, 1.0f) );
        //this.addNode(blackSphere);
/*        GL3DShape xAxis = new GL3DArrow("X-Axis", Constants.SunRadius / 20, Constants.SunRadius, 32, new GL3DVec4f(1, 0, 0.5f, 0.2f));
        xAxis.modelView().rotate(Math.PI / 2, 0, 1, 0);
        indicatorArrows.addNode(xAxis);
        GL3DShape yAxis = new GL3DArrow("Y-Axis", Constants.SunRadius / 20, Constants.SunRadius, 32, new GL3DVec4f(0, 1, 0, 0.2f));
        yAxis.modelView().rotate(-Math.PI / 2, GL3DVec3d.XAxis);
        indicatorArrows.addNode(yAxis);
        GL3DShape zAxis = new GL3DArrow("Z-Axis", Constants.SunRadius / 20, Constants.SunRadius, 32, new GL3DVec4f(0, 0.5f, 1, 0.2f));
        indicatorArrows.addNode(zAxis);      */  
    }

    public void shapeDraw(GL3DState state) {
        // state.gl.glDisable(GL.GL_LIGHTING);
        // state.gl.glEnable(GL.GL_BLEND);
        // state.gl.glDisable(GL.GL_DEPTH_TEST);
        // state.gl.glDepthMask(false);
        super.shapeDraw(state);
        // state.gl.glDepthMask(true);
        // state.gl.glEnable(GL.GL_LIGHTING);
        // state.gl.glDisable(GL.GL_BLEND);
        // state.gl.glEnable(GL.GL_DEPTH_TEST);
    }
}