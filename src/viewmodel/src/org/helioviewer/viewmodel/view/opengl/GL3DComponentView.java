package org.helioviewer.viewmodel.view.opengl;

import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.movie.MovieExport;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.DisplayListener;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIViewListenerDistributor;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.viewmodel.view.AbstractComponentView;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.viewmodel.view.opengl.shader.GLSLShader;

import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.awt.ImageUtil;

/**
 * The top-most View in the 3D View Chain. Lets the viewchain render to its
 * {@link GLCanvas}.
 *
 *
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 *
 */
public class GL3DComponentView extends AbstractComponentView implements GLEventListener, ComponentView, DisplayListener {

    private GLCanvas canvas;

    private Color backgroundColor = Color.BLACK;
    private boolean backGroundColorChanged = false;

    // screenshot & movie
    private ExportMovieDialog exportMovieDialog;
    private MovieExport export;
    private boolean exportMode = false;
    private boolean screenshotMode = false;
    private int previousScreenshot = -1;
    private File outputFile;

    @Override
    public void activate() {
        canvas.addGLEventListener(this);
        Displayer.addListener(this);
    }

    @Override
    public void deactivate() {
        Displayer.removeListener(this);
        canvas.removeGLEventListener(this);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();

        GLInfo.update((GLCanvas) drawable);

        GLSLShader.initShader(gl);
        GL3DState.create(gl);

        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glEnable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_POINT_SMOOTH);

        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glDepthFunc(GL2.GL_LEQUAL);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    private static void displayBody(GL2 gl, View v, int width, int height) {
        GL3DState state = GL3DState.get();
        GL3DCamera camera = ImageViewerGui.getSingletonInstance().getCameraView().getCurrentCamera();
        state.setActiveChamera(camera);

        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);

        gl.glDisable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        state.pushMV();
        camera.applyPerspective(state);
        camera.applyCamera(state);
        Displayer.getRenderablecontainer().render(state);
        camera.drawCamera(state);
        camera.resumePerspective(state);
        state.popMV();

        gl.glEnable(GL2.GL_BLEND);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        GL3DState.setUpdated(gl, width, height);
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        gl.glPushMatrix();
        displayBody(gl, view, width, height);
        gl.glPopMatrix();

        if (!postRenderers.isEmpty()) {
            gl.glPushMatrix();

            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();

            gl.glOrtho(0, width, 0, height, -1, 1);

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0.0f, height, 0.0f);
            gl.glScalef(1.0f, -1.0f, 1.0f);
            gl.glColor4f(1, 1, 1, 0);
            gl.glEnable(GL2.GL_TEXTURE_2D);

            //GLScreenRenderGraphics glRenderer = new GLScreenRenderGraphics(gl);
            //for (ScreenRenderer r : postRenderers) {
            //    r.render(glRenderer);
            //}
            gl.glPopMatrix();
        }

        if (exportMode || screenshotMode) {
            exportFrame();
        }
    }

    @Override
    public void setBackgroundColor(Color background) {
        backgroundColor = background;
        backGroundColorChanged = true;
    }

    @Override
    public void display() {
        canvas.repaint();
    }

    @Override
    public void viewChanged(View sender, ChangeEvent aEvent) {
        SubImageDataChangedReason sidReason = aEvent.getLastChangedReasonByType(SubImageDataChangedReason.class);
        if (sidReason != null) {
            Displayer.display();
        }

        UIViewListenerDistributor.getSingletonInstance().viewChanged(sender, aEvent);
    }

    private void exportFrame() {
        JHVJP2View v;
        JHVJP2View mv;

        if ((v = Displayer.getLayersModel().getActiveView()) == null || (mv = v) == null) {
            stopExport();
            return;
        }

        AWTGLReadBufferUtil rbu = new AWTGLReadBufferUtil(canvas.getGLProfile(), false);
        GL2 gl = canvas.getGL().getGL2();
        int width = canvas.getWidth();

        BufferedImage screenshot;

        if (exportMode) {
            int currentScreenshot = 1;
            int maxframeno = 1;
            if (mv instanceof JHVJPXView) {
                currentScreenshot = ((JHVJPXView) mv).getCurrentFrameNumber();
                maxframeno = ((JHVJPXView) mv).getMaximumFrameNumber();
            }

            screenshot = ImageUtil.createThumbnail(rbu.readPixelsToBufferedImage(gl, true), width);
            if (currentScreenshot != previousScreenshot) {
                export.writeImage(screenshot);
            }
            exportMovieDialog.setLabelText("Exporting frame " + (currentScreenshot + 1) + " / " + (maxframeno + 1));

            if ((!(mv instanceof JHVJPXView)) || (mv instanceof JHVJPXView && currentScreenshot < previousScreenshot)) {
                stopExport();
            }
            previousScreenshot = currentScreenshot;
        }

        if (screenshotMode) {
            screenshot = ImageUtil.createThumbnail(rbu.readPixelsToBufferedImage(gl, true), width);
            try {
                ImageIO.write(screenshot, "png", outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            screenshotMode = false;
        }
    }

    @Override
    public void startExport(ExportMovieDialog exportMovieDialog) {
        this.exportMovieDialog = exportMovieDialog;
        ImageViewerGui.getSingletonInstance().getLeftContentPane().setEnabled(false);
        View v = Displayer.getLayersModel().getActiveView();
        if (v instanceof JHVJPXView) {
            export = new MovieExport(canvas.getWidth(), canvas.getHeight());
            export.createProcess();
            exportMode = true;

            JHVJPXView jpxView = (JHVJPXView) v;
            jpxView.pauseMovie();
            jpxView.setCurrentFrame(0);
            jpxView.playMovie();
        } else {
            exportMovieDialog.fail();
            exportMovieDialog = null;
        }
    }

    private void stopExport() {
        View v = Displayer.getLayersModel().getActiveView();
        JHVJPXView jpxView = (JHVJPXView) v;

        exportMode = false;
        previousScreenshot = -1;
        export.finishProcess();

        JTextArea text = new JTextArea("Exported movie at: " + export.getFileName());
        text.setBackground(null);
        JOptionPane.showMessageDialog(ImageViewerGui.getSingletonInstance().getMainImagePanel(), text);

        ImageViewerGui.getSingletonInstance().getLeftContentPane().setEnabled(true);

        jpxView.pauseMovie();
        exportMovieDialog.reset();
        exportMovieDialog = null;
    }

    @Override
    public boolean saveScreenshot(String imageFormat, File outputFile) {
        this.outputFile = outputFile;
        screenshotMode = true;
        return true;
    }

    @Override
    public void setComponent(Component component) {
        canvas = (GLCanvas) component;
    }

    @Override
    protected void setViewSpecificImplementation(View newView, javax.swing.event.ChangeEvent changeEvent) {

    }

}
