package org.helioviewer.jhv.renderable.components;

import java.awt.Color;
import java.awt.Component;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.helioviewer.jhv.base.astronomy.Position;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.math.Mat4;
import org.helioviewer.jhv.base.math.Quat;
import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Viewport;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLHelper;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.components.RenderableGridOptionsPanel.GridChoiceType;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableGrid extends AbstractRenderable {

    // height of text in solar radii
    private static final float textScale = 0.08f;
    private static final int SUBDIVISIONS = 90;
    private static final Color firstColor = Color.RED;
    private static final Color secondColor = Color.GREEN;

    private float lonstepDegrees = 15f;
    private float latstepDegrees = 20f;

    private boolean showAxes = true;
    private boolean showLabels = true;

    private final Component optionsPanel;
    private static final String name = "Grid";

    public RenderableGrid() {
        optionsPanel = new RenderableGridOptionsPanel(this);
        setVisible(true);

        makeLatLabels();
        makeLonLabels();
        makeRadialLabels();
    }

    private int positionBufferID;
    private int colorBufferID;
    private GridChoiceType gridChoice = GridChoiceType.VIEWPOINT;

    @Override
    public void render(Camera camera, Viewport vp, GL2 gl) {
        if (!isVisible[vp.idx])
            return;

        if (showAxes)
            drawAxes(gl);

        Mat4 cameraMatrix;
        switch (gridChoice) {
        case VIEWPOINT:
            cameraMatrix = camera.getViewpoint().orientation.toMatrix();
            break;
        case HCI:
            cameraMatrix = Mat4.identity();
            break;
        case STONYHURST:
            Position.Latitudinal p = Sun.getEarth(Layers.getLastUpdatedTimestamp().milli);
            Quat orientation = new Quat(0, p.lon);
            cameraMatrix = orientation.toMatrix();
            break;
        default:
            cameraMatrix = Mat4.identity();
            break;
        }

        GLHelper.lineWidth(gl, 0.25);

        gl.glPushMatrix();
        gl.glMultMatrixd(cameraMatrix.transpose().m, 0);
        {
            int pixelsPerSolarRadius = (int) (textScale * vp.height / (2 * camera.getWidth()));
            if (showLabels) {
                // cameraWidth changes ever so slightly with distance to Sun
                drawText(gl, pixelsPerSolarRadius);
            }
            drawCircles(gl, pixelsPerSolarRadius);
        }
        gl.glPopMatrix();
        drawEarthCircles(gl);
    }

    private void drawAxes(GL2 gl) {
        GLHelper.lineWidth(gl, 1);

        gl.glBegin(GL2.GL_LINES);
        {
            gl.glColor3f(0, 0, 1);
            gl.glVertex3f(0, -1.2f, 0);
            gl.glVertex3f(0, -1, 0);
            gl.glColor3f(1, 0, 0);
            gl.glVertex3f(0, 1.2f, 0);
            gl.glVertex3f(0, 1, 0);
        }
        gl.glEnd();
    }

    private void drawEarthCircles(GL2 gl) {
        gl.glColor3f(1, 1, 0);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glVertexPointer(2, GL2.GL_FLOAT, 0, 0);
        {
            Position.Latitudinal p = Sun.getEarth(Layers.getLastUpdatedTimestamp().milli);
            {
                gl.glPushMatrix();
                Quat longitudeRotation = new Quat(0, p.lon + Math.PI / 2);
                longitudeRotation.conjugate();
                gl.glMultMatrixd(longitudeRotation.toMatrix().m, 0);
                gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                gl.glPopMatrix();

                gl.glPushMatrix();
                Quat latitudeRotation = new Quat(p.lat + Math.PI / 2, p.lon);
                latitudeRotation.conjugate();
                gl.glMultMatrixd(latitudeRotation.toMatrix().m, 0);
                gl.glRotatef((float) (-p.lat), 0, 0, 1);
                gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                gl.glPopMatrix();
            }
        }
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private static final float END_RADIUS = 30;
    private static final float START_RADIUS = 2;
    private static final float[] R_LABEL_POS = { 2, 7, 12, 22 };

    private static float STEP_DEGREES = 15;

    private void drawRadialGrid(GL2 gl, int size) {

        gl.glColor3f(1, 1, 1);
        gl.glPushMatrix();
        {
            gl.glRotatef(90, 0, 0, 1);
            {
                gl.glPushMatrix();
                gl.glScalef(1, 1, 1);
                for (float i = START_RADIUS; i <= END_RADIUS; i++) {
                    if (i % 10 == 0) {
                        GLHelper.lineWidth(gl, 0.5);
                    }
                    else {
                        GLHelper.lineWidth(gl, 0.25);
                    }
                    gl.glScalef(i / (i - 1), i / (i - 1), i / (i - 1));
                    gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                }
                gl.glPopMatrix();
            }
            GLHelper.lineWidth(gl, 0.25);
            {
                gl.glPushMatrix();

                for (float i = 0; i < 360; i += STEP_DEGREES) {
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3f(START_RADIUS, 0, 0);
                    gl.glVertex3f(END_RADIUS, 0, 0);
                    gl.glEnd();
                    gl.glRotatef(STEP_DEGREES, 0, 0, 1);
                }
                gl.glPopMatrix();
            }
        }
        gl.glPopMatrix();
        if (this.showLabels) {
            drawRadialGridText(gl, size);
        }

    }

    private void drawRadialGridText(GL2 gl, int size) {
        gl.glDisable(GL2.GL_CULL_FACE);
        for (float rsize : R_LABEL_POS) {
            TextRenderer renderer = GLText.getRenderer((int) (rsize * size));
            float textScaleFactor = textScale / renderer.getFont().getSize2D();
            renderer.begin3DRendering();
            for (int i = 0; i < radialLabels.size(); ++i) {
                GridLabel label = radialLabels.get(i);
                renderer.draw3D(label.txt, rsize * label.x, rsize * label.y, 0, rsize * textScaleFactor);
            }
            renderer.end3DRendering();
        }
        gl.glEnable(GL2.GL_CULL_FACE);

    }

    private void drawCircles(GL2 gl, int size) {
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glVertexPointer(2, GL2.GL_FLOAT, 0, 0);
        {
            drawRadialGrid(gl, size);
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBufferID);
            gl.glColorPointer(3, GL2.GL_FLOAT, 0, 0);
            gl.glRotatef(90, 0, 1, 0);

            gl.glPushMatrix();
            {
                float rotation = 0;
                while (rotation <= 180) {
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, SUBDIVISIONS / 4, SUBDIVISIONS / 2 + 1);
                    gl.glRotatef(lonstepDegrees, 0, 1, 0);
                    rotation += lonstepDegrees;
                }
            }
            gl.glPopMatrix();

            gl.glPushMatrix();
            {
                float rotation = 0;
                rotation -= lonstepDegrees;
                gl.glRotatef(-lonstepDegrees, 0, 1, 0);

                while (rotation >= -180) {
                    gl.glDrawArrays(GL2.GL_LINE_STRIP, SUBDIVISIONS / 4, SUBDIVISIONS / 2 + 1);
                    gl.glRotatef(-lonstepDegrees, 0, 1, 0);
                    rotation -= lonstepDegrees;
                }
            }
            gl.glPopMatrix();

            gl.glPushMatrix();
            {
                float scale, rotation = 0;
                gl.glRotatef(90, 1, 0, 0);

                gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                while (rotation < 90) {
                    gl.glPushMatrix();
                    {
                        gl.glTranslatef(0, 0, (float) Math.sin(Math.PI / 180. * rotation));
                        scale = (float) Math.cos(Math.PI / 180. * rotation);
                        gl.glScalef(scale, scale, scale);
                        gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                    }
                    gl.glPopMatrix();
                    rotation += latstepDegrees;
                }

                rotation = latstepDegrees;
                while (rotation < 90) {
                    gl.glPushMatrix();
                    {
                        gl.glTranslatef(0, 0, -(float) Math.sin(Math.PI / 180. * rotation));
                        scale = (float) Math.cos(Math.PI / 180. * rotation);
                        gl.glScalef(scale, scale, scale);
                        gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
                    }
                    gl.glPopMatrix();
                    rotation += latstepDegrees;
                }
                gl.glDrawArrays(GL2.GL_LINE_LOOP, 0, SUBDIVISIONS);
            }
            gl.glPopMatrix();

            gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
        }
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
    }

    private static String formatStrip(double v) {
        String txt = String.format("%.1f", v);
        if (txt.endsWith("0")) {
            txt = txt.substring(0, txt.length() - 2);
        }
        return txt;
    }

    private static class GridLabel {
        protected String txt;
        protected float x;
        protected float y;
        protected float theta;

        protected GridLabel(String _txt, float _x, float _y, float _theta) {
            txt = _txt;
            x = _x;
            y = _y;
            theta = _theta;
        }
    }

    private final ArrayList<GridLabel> latLabels = new ArrayList<GridLabel>();
    private final ArrayList<GridLabel> lonLabels = new ArrayList<GridLabel>();
    private final ArrayList<GridLabel> radialLabels = new ArrayList<GridLabel>();

    private void makeRadialLabels() {
        double size = Sun.Radius;
        radialLabels.clear();

        for (double phi = 0; phi < 360; phi += STEP_DEGREES) {
            double angle = phi * Math.PI / 180.;
            String txt = formatStrip(phi);
            radialLabels.add(new GridLabel(txt, (float) (Math.sin(-angle) * size), (float) (Math.cos(-angle) * size), 0));
        }
    }

    private void makeLatLabels() {
        double size = Sun.Radius * 1.1;
        // adjust for font size in horizontal and vertical direction (centering the text approximately)
        float horizontalAdjustment = textScale / 2f;
        float verticalAdjustment = textScale / 3f;

        latLabels.clear();

        for (double phi = 0; phi <= 90; phi += latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = formatStrip(phi);

            latLabels.add(new GridLabel(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            if (phi != 90) {
                latLabels.add(new GridLabel(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            }
        }
        for (double phi = -latstepDegrees; phi >= -90; phi -= latstepDegrees) {
            double angle = (90 - phi) * Math.PI / 180.;
            String txt = formatStrip(phi);

            latLabels.add(new GridLabel(txt, (float) (Math.sin(angle) * size), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            if (phi != -90) {
                latLabels.add(new GridLabel(txt, (float) (-Math.sin(angle) * size - horizontalAdjustment), (float) (Math.cos(angle) * size - verticalAdjustment), 0));
            }
        }
    }

    private void makeLonLabels() {
        double size = Sun.Radius * 1.05;

        lonLabels.clear();

        for (double theta = 0; theta <= 180.; theta += lonstepDegrees) {
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = formatStrip(theta);
            lonLabels.add(new GridLabel(txt, (float) (Math.cos(angle) * size), (float) (Math.sin(angle) * size), (float) theta));
        }
        for (double theta = -lonstepDegrees; theta > -180.; theta -= lonstepDegrees) {
            double angle = (90 - theta) * Math.PI / 180.;
            String txt = formatStrip(theta);
            lonLabels.add(new GridLabel(txt, (float) (Math.cos(angle) * size), (float) (Math.sin(angle) * size), (float) theta));
        }
    }

    private void drawText(GL2 gl, int size) {
        TextRenderer renderer = GLText.getRenderer(size);
        // the scale factor has to be divided by the current font size
        float textScaleFactor = textScale / renderer.getFont().getSize2D();

        renderer.begin3DRendering();

        gl.glDisable(GL2.GL_CULL_FACE);
        for (int i = 0; i < latLabels.size(); ++i) {
            GridLabel label = latLabels.get(i);
            renderer.draw3D(label.txt, label.x, label.y, 0, textScaleFactor);
        }
        renderer.flush();
        gl.glEnable(GL2.GL_CULL_FACE);

        for (int i = 0; i < lonLabels.size(); ++i) {
            gl.glPushMatrix();
            {
                GridLabel label = lonLabels.get(i);
                gl.glTranslatef(label.x, 0, label.y);
                gl.glRotatef(label.theta, 0, 1, 0);

                renderer.draw3D(label.txt, 0, 0, 0, textScaleFactor);
                renderer.flush();
            }
            gl.glPopMatrix();
        }
        renderer.end3DRendering();
    }

    @Override
    public void init(GL2 gl) {
        FloatBuffer positionBuffer = FloatBuffer.allocate((SUBDIVISIONS + 1) * 2);
        FloatBuffer colorBuffer = FloatBuffer.allocate((SUBDIVISIONS + 1) * 3);

        for (int i = 0; i <= SUBDIVISIONS; i++) {
            positionBuffer.put((float) Math.cos(2 * Math.PI * i / SUBDIVISIONS));
            positionBuffer.put((float) Math.sin(2 * Math.PI * i / SUBDIVISIONS));
            if (i % 2 == 0) {
                colorBuffer.put(firstColor.getRed() / 255f);
                colorBuffer.put(firstColor.getGreen() / 255f);
                colorBuffer.put(firstColor.getBlue() / 255f);
            } else {
                colorBuffer.put(secondColor.getRed() / 255f);
                colorBuffer.put(secondColor.getGreen() / 255f);
                colorBuffer.put(secondColor.getBlue() / 255f);
            }
        }

        positionBuffer.flip();
        colorBuffer.flip();
        int positionBufferSize = positionBuffer.capacity();
        int colorBufferSize = colorBuffer.capacity();

        positionBufferID = generate(gl);
        colorBufferID = generate(gl);

        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, positionBufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, positionBufferSize * Buffers.SIZEOF_FLOAT, positionBuffer, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, colorBufferID);
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, colorBufferSize * Buffers.SIZEOF_FLOAT, colorBuffer, GL2.GL_STATIC_DRAW);
    }

    private int generate(GL2 gl) {
        int[] tmpId = new int[1];
        gl.glGenBuffers(1, tmpId, 0);
        return tmpId[0];
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return optionsPanel;
    }

    @Override
    public String getName() {
        return name;
    }

    public double getLonstepDegrees() {
        return lonstepDegrees;
    }

    public void setLonstepDegrees(double _lonstepDegrees) {
        lonstepDegrees = (float) _lonstepDegrees;
        makeLonLabels();
    }

    public double getLatstepDegrees() {
        return latstepDegrees;
    }

    public void setLatstepDegrees(double _latstepDegrees) {
        latstepDegrees = (float) _latstepDegrees;
        makeLatLabels();
    }

    public void showLabels(boolean show) {
        showLabels = show;
    }

    public void showAxes(boolean show) {
        showAxes = show;
    }

    @Override
    public String getTimeString() {
        return null;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }

    @Override
    public void dispose(GL2 gl) {
        gl.glDeleteBuffers(1, new int[] { positionBufferID }, 0);
        gl.glDeleteBuffers(1, new int[] { colorBufferID }, 0);
    }

    public void setCoordinates(GridChoiceType _gridChoice) {
        gridChoice = _gridChoice;
    }

}
