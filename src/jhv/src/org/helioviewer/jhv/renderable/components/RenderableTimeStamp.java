package org.helioviewer.jhv.renderable.components;

import java.awt.Component;

import org.helioviewer.jhv.camera.GL3DViewport;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.GLText;
import org.helioviewer.jhv.renderable.gui.AbstractRenderable;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;

public class RenderableTimeStamp extends AbstractRenderable {

    private static final double vpScale = 0.035;
    private static final String name = "Timestamp";

    @Override
    public void render(GL2 gl, GL3DViewport vp) {
    }

    @Override
    public void renderFloat(GL2 gl, GL3DViewport vp) {
        if (!isVisible[vp.getIndex()])
            return;

        String text = Layers.getLastUpdatedTimestamp().toString();
        if (Displayer.multiview) {
            RenderableImageLayer im = ImageViewerGui.getRenderableContainer().getViewportRenderableImageLayer(vp.getIndex());
            if (im != null) {
                text = im.getTimeString();
            }
        }

        int delta = (int) (vp.getHeight() * 0.01);
        TextRenderer renderer = GLText.getRenderer((int) (vp.getHeight() * vpScale));
        renderer.beginRendering(vp.getWidth(), vp.getHeight(), true);
        renderer.draw(text, delta, delta);
        renderer.endRendering();
    }

    @Override
    public void init(GL2 gl) {
    }

    @Override
    public void remove(GL2 gl) {
        dispose(gl);
    }

    @Override
    public Component getOptionsPanel() {
        return null;
    }

    @Override
    public String getName() {
        return name;
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
    }

    @Override
    public void renderMiniview(GL2 gl, GL3DViewport vp) {
    }

}
