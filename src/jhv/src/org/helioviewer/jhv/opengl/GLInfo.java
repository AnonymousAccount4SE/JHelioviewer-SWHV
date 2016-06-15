package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.base.logging.Log;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.glu.GLU;

public class GLInfo {

    public static final int GLSAMPLES = 4;

    public static int[] pixelScale = new int[] { 1, 1 };
    public static float[] pixelScaleFloat = new float[] { 1f, 1f };

    static int maxTextureSize;

    private static boolean first = true;

    public static void update(GL2 gl) {
        if (first) {
            first = false;

            Log.debug("GLInfo > Version string: " + gl.glGetString(GL2.GL_VERSION));
            Log.debug("GLInfo > Extensions: " + gl.glGetString(GL2.GL_EXTENSIONS));

            if (!gl.isExtensionAvailable("GL_VERSION_2_1")) {
                String err = "OpenGL 2.1 not supported. JHelioviewer is not able to run.";
                Log.error("GLInfo > " + err);
                throw new GLException(err);
            }

            int[] out = new int[] { 0 };
            gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, out, 0);
            maxTextureSize = out[0];
            Log.debug("GLInfo > max texture size: " + out[0]);
        } else
            Log.debug("GLInfo.update()");
    }

    public static void updatePixelScale(ScalableSurface surface) {
        surface.getCurrentSurfaceScale(pixelScaleFloat);
        pixelScale[0] = (int) pixelScaleFloat[0];
        pixelScale[1] = (int) pixelScaleFloat[1];
    }

    public static boolean checkGLErrors(GL2 gl, String message) {
        GLU glu = new GLU();
        int glErrorCode, errors = 0;

        while ((glErrorCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
            Log.error("GL Error (" + glErrorCode + "): " + glu.gluErrorString(glErrorCode) + " - @" + message);
            errors++;
        }
        return errors != 0;
    }

}
