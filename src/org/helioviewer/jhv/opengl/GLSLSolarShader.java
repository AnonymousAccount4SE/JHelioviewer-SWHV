package org.helioviewer.jhv.opengl;

import org.helioviewer.jhv.math.Quat;
import org.helioviewer.jhv.math.Vec2;

import com.jogamp.opengl.GL2;

public class GLSLSolarShader extends GLSLShader {

    public static final GLSLSolarShader sphere = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarSphere.frag", false);
    public static final GLSLSolarShader ortho = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarOrtho.frag", true);
    public static final GLSLSolarShader lati = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLati.frag", true);
    public static final GLSLSolarShader polar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarPolar.frag", true);
    public static final GLSLSolarShader logpolar = new GLSLSolarShader("/glsl/solar.vert", "/glsl/solarLogPolar.frag", true);

    private final boolean hasCommon;

    private int isDiffRef;

    private int hgltRef;
    private int gridRef;
    private int hgltDiffRef;
    private int gridDiffRef;

    private int crvalRef;
    private int crotaQuatRef;
    private int crotaRef;
    private int crotaDiffRef;

    private int deltaTRef;

    private int sectorRef;
    private int radiiRef;
    private int polarRadiiRef;
    private int cutOffDirectionRef;
    private int cutOffValueRef;

    private int slitRef;
    private int brightRef;
    private int colorRef;
    private int sharpenRef;
    private int enhancedRef;
    private int calculateDepthRef;

    private int rectRef;
    private int diffRectRef;
    private int viewportRef;
    private int viewportOffsetRef;

    private int cameraTransformationInverseRef;
    private int cameraDifferenceRef;

    private final int[] isDiff = new int[1];

    private final float[] hglt = new float[1];
    private final float[] grid = new float[2];
    private final float[] crota = new float[3];
    private final float[] hgltDiff = new float[1];
    private final float[] gridDiff = new float[2];
    private final float[] crotaDiff = new float[3];

    private final int[] enhanced = new int[1];
    private final int[] calculateDepth = new int[1];

    private final float[] floatArr = new float[8];

    private GLSLSolarShader(String vertex, String fragment, boolean _hasCommon) {
        super(vertex, fragment);
        hasCommon = _hasCommon;
    }

    public static void init(GL2 gl) {
        sphere._init(gl, sphere.hasCommon);
        ortho._init(gl, ortho.hasCommon);
        lati._init(gl, lati.hasCommon);
        polar._init(gl, polar.hasCommon);
        logpolar._init(gl, logpolar.hasCommon);
    }

    @Override
    protected void initUniforms(GL2 gl, int id) {
        isDiffRef = gl.glGetUniformLocation(id, "isdifference");

        hgltRef = gl.glGetUniformLocation(id, "hglt");
        gridRef = gl.glGetUniformLocation(id, "grid");
        hgltDiffRef = gl.glGetUniformLocation(id, "hgltDiff");
        gridDiffRef = gl.glGetUniformLocation(id, "gridDiff");

        crvalRef = gl.glGetUniformLocation(id, "crval");
        crotaQuatRef = gl.glGetUniformLocation(id, "crotaQuat");
        crotaRef = gl.glGetUniformLocation(id, "crota");
        crotaDiffRef = gl.glGetUniformLocation(id, "crotaDiff");

        deltaTRef = gl.glGetUniformLocation(id, "deltaT");

        sectorRef = gl.glGetUniformLocation(id, "sector");
        radiiRef = gl.glGetUniformLocation(id, "radii");
        polarRadiiRef = gl.glGetUniformLocation(id, "polarRadii");
        cutOffDirectionRef = gl.glGetUniformLocation(id, "cutOffDirection");
        cutOffValueRef = gl.glGetUniformLocation(id, "cutOffValue");

        sharpenRef = gl.glGetUniformLocation(id, "sharpen");
        slitRef = gl.glGetUniformLocation(id, "slit");
        brightRef = gl.glGetUniformLocation(id, "brightness");
        colorRef = gl.glGetUniformLocation(id, "color");
        enhancedRef = gl.glGetUniformLocation(id, "enhanced");
        calculateDepthRef = gl.glGetUniformLocation(id, "calculateDepth");

        rectRef = gl.glGetUniformLocation(id, "rect");
        diffRectRef = gl.glGetUniformLocation(id, "differencerect");
        viewportRef = gl.glGetUniformLocation(id, "viewport");
        viewportOffsetRef = gl.glGetUniformLocation(id, "viewportOffset");

        cameraTransformationInverseRef = gl.glGetUniformLocation(id, "cameraTransformationInverse");
        cameraDifferenceRef = gl.glGetUniformLocation(id, "cameraDifference");

        if (hasCommon) {
            setTextureUnit(gl, id, "image", GLTexture.Unit.ZERO);
            setTextureUnit(gl, id, "lut", GLTexture.Unit.ONE);
            setTextureUnit(gl, id, "diffImage", GLTexture.Unit.TWO);
        }
    }

    public static void dispose(GL2 gl) {
        sphere._dispose(gl);
        ortho._dispose(gl);
        lati._dispose(gl);
        polar._dispose(gl);
        logpolar._dispose(gl);
    }

    public void bindMatrix(GL2 gl, float[] matrix) {
        gl.glUniformMatrix4fv(cameraTransformationInverseRef, 1, false, matrix, 0);
    }

    public void bindCameraDifference(GL2 gl, Quat quat, Quat quatDiff) {
        quat.setFloatArray(floatArr, 0);
        quatDiff.setFloatArray(floatArr, 4);
        gl.glUniform4fv(cameraDifferenceRef, 2, floatArr, 0);
    }

    public void bindCRVAL(GL2 gl, Vec2 vec, Vec2 vecDiff) {
        floatArr[0] = (float) vec.x;
        floatArr[1] = (float) vec.y;
        floatArr[2] = (float) vecDiff.x;
        floatArr[3] = (float) vecDiff.y;
        gl.glUniform2fv(crvalRef, 2, floatArr, 0);
    }

    public void bindCROTAQuat(GL2 gl, Quat quat, Quat quatDiff) {
        quat.setFloatArray(floatArr, 0);
        quatDiff.setFloatArray(floatArr, 4);
        gl.glUniform4fv(crotaQuatRef, 2, floatArr, 0);
    }

    public void bindDeltaT(GL2 gl, double deltaT, double deltaTDiff) {
        floatArr[0] = (float) deltaT;
        floatArr[1] = (float) deltaTDiff;
        gl.glUniform1fv(deltaTRef, 2, floatArr, 0);
    }

    public void bindRect(GL2 gl, double xOffset, double yOffset, double xScale, double yScale) {
        floatArr[0] = (float) xOffset;
        floatArr[1] = (float) yOffset;
        floatArr[2] = (float) xScale;
        floatArr[3] = (float) yScale;
        gl.glUniform4fv(rectRef, 1, floatArr, 0);
    }

    public void bindDiffRect(GL2 gl, double diffXOffset, double diffYOffset, double diffXScale, double diffYScale) {
        floatArr[0] = (float) diffXOffset;
        floatArr[1] = (float) diffYOffset;
        floatArr[2] = (float) diffXScale;
        floatArr[3] = (float) diffYScale;
        gl.glUniform4fv(diffRectRef, 1, floatArr, 0);
    }

    public void bindColor(GL2 gl, float red, float green, float blue, double alpha, double blend) {
        floatArr[0] = (float) (red * alpha);
        floatArr[1] = (float) (green * alpha);
        floatArr[2] = (float) (blue * alpha);
        floatArr[3] = (float) (alpha * blend); // http://amindforeverprogramming.blogspot.be/2013/07/why-alpha-premultiplied-colour-blending.html
        gl.glUniform4fv(colorRef, 1, floatArr, 0);
    }

    public void bindSlit(GL2 gl, double left, double right) {
        floatArr[0] = (float) left;
        floatArr[1] = (float) right;
        gl.glUniform1fv(slitRef, 2, floatArr, 0);
    }

    public void bindBrightness(GL2 gl, double offset, double scale, double gamma) {
        floatArr[0] = (float) offset;
        floatArr[1] = (float) scale;
        floatArr[2] = (float) gamma;
        gl.glUniform3fv(brightRef, 1, floatArr, 0);
    }

    public void bindSharpen(GL2 gl, double weight, double pixelWidth, double pixelHeight) {
        floatArr[0] = (float) pixelWidth;
        floatArr[1] = (float) pixelHeight;
        floatArr[2] = -2 * (float) weight; // used for mix
        gl.glUniform3fv(sharpenRef, 1, floatArr, 0);
    }

    public void bindEnhanced(GL2 gl, boolean _enhanced) {
        enhanced[0] = _enhanced ? 1 : 0;
        gl.glUniform1iv(enhancedRef, 1, enhanced, 0);
    }

    public void bindCalculateDepth(GL2 gl, boolean _calculateDepth) {
        calculateDepth[0] = _calculateDepth ? 1 : 0;
        gl.glUniform1iv(calculateDepthRef, 1, calculateDepth, 0);
    }

    public void bindIsDiff(GL2 gl, int _isDiff) {
        isDiff[0] = _isDiff;
        gl.glUniform1iv(isDiffRef, 1, isDiff, 0);
    }

    public void bindViewport(GL2 gl, float offsetX, float offsetY, float width, float height) {
        floatArr[0] = offsetX;
        floatArr[1] = offsetY;
        gl.glUniform2fv(viewportOffsetRef, 1, floatArr, 0);
        floatArr[0] = width;
        floatArr[1] = height;
        floatArr[2] = height / width;
        gl.glUniform3fv(viewportRef, 1, floatArr, 0);
    }

    public void bindCutOffValue(GL2 gl, float val) {
        floatArr[0] = val;
        gl.glUniform1fv(cutOffValueRef, 1, floatArr, 0);
    }

    public void bindCutOffDirection(GL2 gl, float x, float y) {
        floatArr[0] = x;
        floatArr[1] = y;
        gl.glUniform2fv(cutOffDirectionRef, 1, floatArr, 0);
    }

    public void bindAngles(GL2 gl, float _hglt, float _crota, float scrota, float ccrota) {
        hglt[0] = _hglt;
        gl.glUniform1fv(hgltRef, 1, hglt, 0);
        crota[0] = _crota;
        crota[1] = scrota;
        crota[2] = ccrota;
        gl.glUniform1fv(crotaRef, 3, crota, 0);
    }

    public void bindAnglesDiff(GL2 gl, float _hglt, float _crota, float scrota, float ccrota) {
        hgltDiff[0] = _hglt;
        gl.glUniform1fv(hgltDiffRef, 1, hgltDiff, 0);
        crotaDiff[0] = _crota;
        crotaDiff[1] = scrota;
        crotaDiff[2] = ccrota;
        gl.glUniform1fv(crotaDiffRef, 3, crotaDiff, 0);
    }

    public void bindAnglesLatiGrid(GL2 gl, float lon, float lat) {
        grid[0] = lon;
        grid[1] = lat;
        gl.glUniform1fv(gridRef, 2, grid, 0);
    }

    public void bindAnglesLatiGridDiff(GL2 gl, float lon, float lat) {
        gridDiff[0] = lon;
        gridDiff[1] = lat;
        gl.glUniform1fv(gridDiffRef, 2, gridDiff, 0);
    }

    public void bindSector(GL2 gl, float sector0, float sector1) {
        floatArr[0] = sector0 == sector1 ? 0 : 1;
        floatArr[1] = sector0;
        floatArr[2] = sector1;
        gl.glUniform1fv(sectorRef, 3, floatArr, 0);
    }

    public void bindRadii(GL2 gl, float innerRadius, float outerRadius) {
        floatArr[0] = innerRadius;
        floatArr[1] = outerRadius;
        gl.glUniform1fv(radiiRef, 2, floatArr, 0);
    }

    public void bindPolarRadii(GL2 gl, double start, double stop) {
        floatArr[0] = (float) start;
        floatArr[1] = (float) stop;
        gl.glUniform1fv(polarRadiiRef, 2, floatArr, 0);
    }

}
