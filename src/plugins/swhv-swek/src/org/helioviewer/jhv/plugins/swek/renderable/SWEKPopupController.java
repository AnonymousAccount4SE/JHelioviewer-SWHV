package org.helioviewer.jhv.plugins.swek.renderable;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.base.math.Vec3;
import org.helioviewer.jhv.base.scale.GridScale;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.camera.CameraHelper;
import org.helioviewer.jhv.data.container.JHVEventContainer;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVPositionInformation;
import org.helioviewer.jhv.data.datatype.event.JHVRelatedEvents;
import org.helioviewer.jhv.data.guielements.SWEKEventInformationDialog;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.gui.controller.InputControllerPlugin;
import org.helioviewer.jhv.layers.TimeListener;
import org.helioviewer.jhv.opengl.GLHelper;

public class SWEKPopupController implements MouseListener, MouseMotionListener, InputControllerPlugin, TimeListener {

    private static final Cursor helpCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private static final int xOffset = 12;
    private static final int yOffset = 12;

    private static Component component;
    private static Camera camera;

    private static Cursor lastCursor;

    static JHVRelatedEvents mouseOverJHVEvent = null;
    static Point mouseOverPosition = null;
    long currentTime;

    @Override
    public void setComponent(Component _component) {
        component = _component;
    }

    @Override
    public void setCamera(Camera _camera) {
        camera = _camera;
    }

    @Override
    public void timeChanged(JHVDate date) {
        currentTime = date.milli;
    }

    private Point calcWindowPosition(Point p, int hekWidth, int hekHeight) {
        int yCoord = 0;
        boolean yCoordInMiddle = false;

        int compWidth = component.getWidth();
        int compHeight = component.getHeight();
        int compLocX = component.getLocationOnScreen().x;
        int compLocY = component.getLocationOnScreen().y;

        if (p.y + hekHeight + yOffset < compHeight) {
            yCoord = p.y + compLocY + yOffset;
        } else {
            yCoord = p.y + compLocY - hekHeight - yOffset;
            if (yCoord < compLocY) {
                yCoord = compLocY + compHeight - hekHeight;
                if (yCoord < compLocY) {
                    yCoord = compLocY;
                }
                yCoordInMiddle = true;
            }
        }

        int xCoord = 0;
        if (p.x + hekWidth + xOffset < compWidth) {
            xCoord = p.x + compLocX + xOffset;
        } else {
            xCoord = p.x + compLocX - hekWidth - xOffset;
            if (xCoord < compLocX && !yCoordInMiddle) {
                xCoord = compLocX + compWidth - hekWidth;
            }
        }

        return new Point(xCoord, yCoord);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (mouseOverJHVEvent != null) {
            SWEKEventInformationDialog hekPopUp = new SWEKEventInformationDialog(mouseOverJHVEvent, mouseOverJHVEvent.getClosestTo(currentTime));
            hekPopUp.pack();
            hekPopUp.setLocation(calcWindowPosition(GLHelper.GL2AWTPoint(mouseOverPosition.x, mouseOverPosition.y), hekPopUp.getWidth(), hekPopUp.getHeight()));
            hekPopUp.setVisible(true);

            component.setCursor(helpCursor);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseOverPosition = null;
        mouseOverJHVEvent = null;
        JHVEventContainer.highlight(null);
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        ArrayList<JHVRelatedEvents> eventsToDraw = SWEKData.getSingletonInstance().getActiveEvents(currentTime);
        if (eventsToDraw.isEmpty())
            return;

        mouseOverJHVEvent = null;
        mouseOverPosition = null;
        Vec3 pt = null;
        Vec3 hitpoint = null;

        Viewport vp = Displayer.getActiveViewport();
        for (JHVRelatedEvents evtr : eventsToDraw) {
            JHVEvent evt = evtr.getClosestTo(currentTime);
            JHVPositionInformation pi = evt.getPositionInformation();
            if (pi == null)
                continue;

            if (Displayer.mode == Displayer.DisplayMode.ORTHO) {
                if (evt.getName() == "Coronal Mass Ejection") { // interned
                    double principalAngle = Math.toRadians(SWEKData.readCMEPrincipalAngleDegree(evt));
                    double speed = SWEKData.readCMESpeed(evt);
                    double distSun = 2.4;
                    distSun += speed * (currentTime - evt.start) / Sun.RadiusMeter;

                    Position.Q p = pi.getEarthPosition();
                    hitpoint = p.orientation.rotateInverseVector(getHitPointPlane(e, vp));
                    pt = p.orientation.rotateInverseVector(new Vec3(distSun * Math.cos(principalAngle), distSun * Math.sin(principalAngle), 0));
                } else {
                    hitpoint = getHitPoint(e, vp);
                    pt = pi.centralPoint();
                }

                if (pt != null && hitpoint != null) {
                    double deltaX = Math.abs(hitpoint.x - pt.x);
                    double deltaY = Math.abs(hitpoint.y - pt.y);
                    double deltaZ = Math.abs(hitpoint.z - pt.z);
                    if (deltaX < 0.08 && deltaZ < 0.08 && deltaY < 0.08) {
                        mouseOverJHVEvent = evtr;
                        mouseOverPosition = e.getPoint();
                        break;
                    }
                }
            } else {
                Vec2 tf = null;
                Vec2 mousepos = null;
                if (evt.getName() == "Coronal Mass Ejection") { // interned
                    if (Displayer.mode == Displayer.DisplayMode.LOGPOLAR || Displayer.mode == Displayer.DisplayMode.POLAR) {
                        double principalAngle = SWEKData.readCMEPrincipalAngleDegree(evt) - 90;
                        double speed = SWEKData.readCMESpeed(evt);
                        double distSun = 2.4;
                        distSun += speed * (currentTime - evt.start) / Sun.RadiusMeter;
                        GridScale scale = GridScale.current;
                        tf = new Vec2(scale.getXValueInv(principalAngle), scale.getYValueInv(distSun));
                        mousepos = scale.mouseToGridInv(e.getPoint(), vp, camera);
                    }
                } else {
                    hitpoint = getHitPoint(e, vp);
                    pt = pi.centralPoint();
                    pt = camera.getViewpoint().orientation.rotateVector(pt);
                    GridScale scale = GridScale.current;
                    tf = scale.transform(pt);
                    mousepos = scale.mouseToGridInv(e.getPoint(), vp, camera);
                }

                if (tf != null && mousepos != null) {
                    double deltaX = Math.abs(tf.x - mousepos.x);
                    double deltaY = Math.abs(tf.y - mousepos.y);
                    if (deltaX < 0.02 && deltaY < 0.02) {
                        mouseOverJHVEvent = evtr;
                        mouseOverPosition = e.getPoint();
                        break;
                    }
                }
            }
        }

        JHVEventContainer.highlight(mouseOverJHVEvent);
        if (helpCursor != component.getCursor())
            lastCursor = component.getCursor();

        if (mouseOverJHVEvent != null) {
            component.setCursor(helpCursor);
        } else {
            component.setCursor(lastCursor);
        }
    }

    private Vec3 getHitPointPlane(MouseEvent e, Viewport vp) {
        Point p = e.getPoint();
        return CameraHelper.getVectorFromPlane(camera, vp, p.x, p.y, Quat.ZERO, true);
    }

    private Vec3 getHitPoint(MouseEvent e, Viewport vp) {
        Point p = e.getPoint();
        Vec3 hp = CameraHelper.getVectorFromSphere(camera, vp, p.x, p.y, camera.getViewpoint().orientation, true);
        if (hp != null)
            hp.y = -hp.y;
        return hp;
    }

}
