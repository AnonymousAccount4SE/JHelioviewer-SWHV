package org.helioviewer.jhv.camera.annotate;

import javax.annotation.Nullable;

import org.helioviewer.jhv.astronomy.Sun;
import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.scale.GridType;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.display.Display;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.math.Vec2;
import org.helioviewer.jhv.math.Vec3;
import org.helioviewer.jhv.opengl.GLHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

abstract class AbstractAnnotateable implements Annotateable {

    static final float[] activeColor = BufferUtils.colorRed;
    static final float[] dragColor = BufferUtils.colorYellow;
    static final float[] baseColor = BufferUtils.colorBlue;

    static final float lineWidth = 2;
    static final double radius = Sun.Radius * 1.01;
    private static final double lineWidthSR = Sun.Radius * 0.002;

    final Camera camera;

    Vec3 startPoint;
    Vec3 endPoint;

    Vec3 dragStartPoint;
    Vec3 dragEndPoint;

    AbstractAnnotateable(Camera _camera, JSONObject jo) {
        camera = _camera;
        if (jo != null) {
            JSONArray jaStart = jo.optJSONArray("startPoint");
            if (jaStart != null)
                startPoint = Vec3.fromJson(jaStart);

            JSONArray jaEnd = jo.optJSONArray("endPoint");
            if (jaEnd != null)
                endPoint = Vec3.fromJson(jaEnd);
        }
    }

    static Vec3 toSpherical(Vec3 p) {
        double len = p.length();
        return new Vec3(len, Math.acos(p.y / len), Math.atan2(p.x, p.z));
    }

    static Vec3 toCart(double y, double z) {
        return new Vec3(radius * Math.sin(y) * Math.sin(z),
                        radius * Math.cos(y),
                        radius * Math.sin(y) * Math.cos(z));
    }

    @Nullable
    Vec3 computePoint(int x, int y) {
        if (Display.mode == Display.DisplayMode.Orthographic) {
            return CameraHelper.getVectorFromSphere(camera, Display.getActiveViewport(), x, y, camera.getViewpoint().toQuat(), true);
        } else {
            return Display.mode.xform.transformInverse(Display.mode.scale.mouseToGrid(x, y, Display.getActiveViewport(), camera, GridType.Viewpoint));
        }
    }

    void interpolatedLineDraw(Viewport vp, GL2 gl, Vec3 p1s, Vec3 p2s, int subdivisions) {
        if (Display.mode == Display.DisplayMode.Orthographic) {
            gl.glBegin(GL2.GL_TRIANGLE_STRIP);

            for (double i = 0; i < subdivisions; i++) {
                double t = i / subdivisions;
                double y0 = (1 - t) * p1s.y + t * p2s.y;
                double z0 = (1 - t) * p1s.z + t * p2s.z;
                Vec3 p0 = toCart(y0, z0);

                t = (i + 1) / subdivisions;
                double y1 = (1 - t) * p1s.y + t * p2s.y;
                double z1 = (1 - t) * p1s.z + t * p2s.z;
                Vec3 p1 = toCart(y1, z1);

                Vec3 p1minusp0 = Vec3.subtract(p1, p0);
                Vec3 v = Vec3.cross(p0, p1minusp0);
                v.normalize();

                v.multiply(lineWidthSR);
                Vec3 p0plusv = Vec3.add(p0, v);
                p0plusv.normalize();
                gl.glVertex3f((float) p0plusv.x, (float) p0plusv.y, (float) p0plusv.z);
                Vec3 p0minusv = Vec3.subtract(p0, v);
                p0minusv.normalize();
                gl.glVertex3f((float) p0minusv.x, (float) p0minusv.y, (float) p0minusv.z);
                if (i == subdivisions - 1) {
                    Vec3 p1plusv = Vec3.add(p1, v);
                    p1plusv.normalize();
                    gl.glVertex3f((float) p1plusv.x, (float) p1plusv.y, (float) p1plusv.z);
                    Vec3 p1minusv = Vec3.subtract(p1, v);
                    p1minusv.normalize();
                    gl.glVertex3f((float) p1minusv.x, (float) p1minusv.y, (float) p1minusv.z);
                }
            }

            gl.glEnd();
        } else {
            gl.glBegin(GL2.GL_LINE_STRIP);

            Vec2 previous = null;
            for (double i = 0; i <= subdivisions; i++) {
                double t = i / subdivisions;
                double y0 = (1 - t) * p1s.y + t * p2s.y;
                double z0 = (1 - t) * p1s.z + t * p2s.z;
                Vec3 p0 = toCart(y0, z0);
                p0.y = -p0.y;
                previous = GLHelper.drawVertex(camera, vp, gl, p0, previous);
            }

            gl.glEnd();
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject jo = new JSONObject().put("type", getType());
        if (startPoint != null)
            jo.put("startPoint", startPoint.toJson());
        if (endPoint != null)
            jo.put("endPoint", endPoint.toJson());
        return jo;
    }

}
