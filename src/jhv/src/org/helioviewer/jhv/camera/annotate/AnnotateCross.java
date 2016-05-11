package org.helioviewer.jhv.camera.annotate;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;

import com.jogamp.opengl.GL2;

public class AnnotateCross extends AbstractAnnotateable {

    private Vec3 crossPoint;

    public AnnotateCross(Camera _camera) {
        super(_camera);
    }

    private void drawCross(Viewport vp, GL2 gl, Vec3 bp) {
        double delta = Math.PI * 2.5 / 180;
        Vec3 p1 = new Vec3(Sun.Radius, bp.y + delta, bp.z);
        Vec3 p2 = new Vec3(Sun.Radius, bp.y - delta, bp.z);
        Vec3 p3 = new Vec3(Sun.Radius, bp.y, bp.z + delta);
        Vec3 p4 = new Vec3(Sun.Radius, bp.y, bp.z - delta);
        interpolatedLineDraw(vp, gl, p1, p2, 2);
        interpolatedLineDraw(vp, gl, p3, p4, 2);
    }

    @Override
    public void render(Viewport vp, GL2 gl, boolean active) {
        if (crossPoint == null)
            return;

        gl.glLineWidth(lineWidth);

        if (active)
            gl.glColor3f(activeColor[0], activeColor[1], activeColor[2]);
        else
            gl.glColor3f(baseColor[0], baseColor[1], baseColor[2]);
        drawCross(vp, gl, toSpherical(crossPoint));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Vec3 pt = computePoint(e.getPoint());
        if (pt != null)
            crossPoint = pt;
    }

    @Override
    public boolean beingDragged() {
        return true;
    }

    @Override
    public boolean isDraggable() {
        return false;
    }

}
