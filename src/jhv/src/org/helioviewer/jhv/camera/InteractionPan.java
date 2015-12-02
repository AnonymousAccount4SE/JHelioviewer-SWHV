package org.helioviewer.jhv.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vec2;
import org.helioviewer.jhv.display.Displayer;

public class InteractionPan extends Interaction {

    private Point lastMousePoint;

    public InteractionPan(Camera _camera) {
        super(_camera);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastMousePoint = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        int x = p.x - lastMousePoint.x;
        int y = p.y - lastMousePoint.y;
        double m = 2. * camera.getWidth() / Displayer.getActiveViewport().height;
        lastMousePoint = p;

        Vec2 pan = camera.getCurrentTranslation();
        camera.setCurrentTranslation(pan.x + x * m, pan.y - y * m);
        Displayer.render(0.5);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Displayer.render(1);
    }

}
