package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.base.BufferUtils;
import org.helioviewer.jhv.base.FloatArray;
import org.helioviewer.jhv.log.Log;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;

public class GLSLLine {

    private int[] vboAttribRefs;
    private final int[] vboAttribLens = { 3, 3, 3, 1, 4 };

    private final VBO[] vbos = new VBO[5];
    private VBO ivbo;
    private boolean hasPoints = false;
    private boolean inited = false;

    public void setData(GL2 gl, FloatBuffer points, FloatBuffer colors) {
        hasPoints = false;
        int plen = points.limit() / 3;
        if (plen != colors.limit() / 4 || plen < 2) {
            Log.error("Something is wrong with the vertices or colors from this GLLine");
            return;
        }
        setBufferData(gl, points, colors, plen);
        hasPoints = true;
    }

    public void setData(GL2 gl, FloatArray points, FloatArray colors) {
        hasPoints = false;
        int plen = points.length() / 3;
        if (plen != colors.length() / 4 || plen < 2) {
            Log.error("Something is wrong with the vertices or colors from this GLLine");
            return;
        }
        setBufferData(gl, points.toArray(), colors.toArray(), plen);
        hasPoints = true;
    }

    public void render(GL2 gl, double aspect, double thickness) {
        if (!hasPoints)
            return;

        GLSLLineShader.line.bind(gl);
        GLSLLineShader.line.setAspect(aspect);
        GLSLLineShader.line.setThickness(thickness);
        GLSLLineShader.line.bindParams(gl);

        bindVBOs(gl);
        gl.glDrawElements(GL2.GL_TRIANGLES, ivbo.bufferSize, GL2.GL_UNSIGNED_INT, 0);
        unbindVBOs(gl);

        GLSLShader.unbind(gl);
    }

    private void bindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.bindArray(gl);
        }
        ivbo.bindArray(gl);
    }

    private void unbindVBOs(GL2 gl) {
        for (VBO vbo : vbos) {
            vbo.unbindArray(gl);
        }
        ivbo.unbindArray(gl);
    }

    private void initVBOs(GL2 gl) {
        for (int i = 0; i < vboAttribRefs.length; i++) {
            vbos[i] = VBO.gen_float_VBO(vboAttribRefs[i], vboAttribLens[i]);
            vbos[i].init(gl);
        }
        ivbo = VBO.gen_index_VBO();
        ivbo.init(gl);
    }

    private void disposeVBOs(GL2 gl) {
        for (int i = 0; i < vbos.length; i++) {
            if (vbos[i] != null) {
                vbos[i].dispose(gl);
                vbos[i] = null;
            }
        }
        if (ivbo != null) {
            ivbo.dispose(gl);
            ivbo = null;
        }
    }

    private static IntBuffer gen_indices(int plen) {
        IntBuffer indicesBuffer = BufferUtils.newIntBuffer(6 * (plen - 1));
        for (int j = 0; j <= 2 * (plen - 2); j += 2) {
            indicesBuffer.put(j);
            indicesBuffer.put(j + 1);
            indicesBuffer.put(j + 2);
            indicesBuffer.put(j + 2);
            indicesBuffer.put(j + 1);
            indicesBuffer.put(j + 3);
        }
        indicesBuffer.rewind();
        return indicesBuffer;
    }

    private static void addPoint(FloatBuffer to, FloatBuffer from, int start, int n) {
        for (int i = start; i < start + n; i++) {
            to.put(from.get(i));
        }
        for (int i = start; i < start + n; i++) {
            to.put(from.get(i));
        }
    }

    private void setBufferData(GL2 gl, FloatBuffer points, FloatBuffer colors, int plen) {
        FloatBuffer previousBuffer = BufferUtils.newFloatBuffer(3 * 2 * plen);
        FloatBuffer currentBuffer = BufferUtils.newFloatBuffer(3 * 2 * plen);
        FloatBuffer nextBuffer = BufferUtils.newFloatBuffer(3 * 2 * plen);
        FloatBuffer directionBuffer = BufferUtils.newFloatBuffer(2 * 2 * plen);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(4 * 2 * plen);

        int dir = -1;
        for (int i = 0; i < 2 * plen; i++) {
            directionBuffer.put(dir);
            directionBuffer.put(-dir);
        }

        addPoint(previousBuffer, points, 0, 3);
        addPoint(currentBuffer, points, 0, 3);
        addPoint(nextBuffer, points, 3, 3);
        addPoint(colorBuffer, colors, 0, 4);
        for (int i = 1; i < plen - 1; i++) {
            addPoint(previousBuffer, points, 3 * (i - 1), 3);
            addPoint(currentBuffer, points, 3 * i, 3);
            addPoint(nextBuffer, points, 3 * (i + 1), 3);
            addPoint(colorBuffer, colors, 4 * i, 4);
        }
        addPoint(previousBuffer, points, 3 * (plen - 2), 3);
        addPoint(currentBuffer, points, 3 * (plen - 1), 3);
        addPoint(nextBuffer, points, 3 * (plen - 1), 3);
        addPoint(colorBuffer, colors, 4 * (plen - 1), 4);

        previousBuffer.rewind();
        currentBuffer.rewind();
        nextBuffer.rewind();
        directionBuffer.rewind();
        colorBuffer.rewind();

        vbos[0].bindBufferData(gl, previousBuffer, Buffers.SIZEOF_FLOAT);
        vbos[1].bindBufferData(gl, currentBuffer, Buffers.SIZEOF_FLOAT);
        vbos[2].bindBufferData(gl, nextBuffer, Buffers.SIZEOF_FLOAT);
        vbos[3].bindBufferData(gl, directionBuffer, Buffers.SIZEOF_FLOAT);
        vbos[4].bindBufferData(gl, colorBuffer, Buffers.SIZEOF_FLOAT);

        IntBuffer indexBuffer = gen_indices(plen);
        ivbo.bindBufferData(gl, indexBuffer, Buffers.SIZEOF_INT);
    }

    private static void addPoint(FloatBuffer to, float[] from, int start, int n) {
        to.put(from, start, n);
        to.put(from, start, n);
    }

    private void setBufferData(GL2 gl, float[] points, float[] colors, int plen) {
        FloatBuffer previousBuffer = BufferUtils.newFloatBuffer(3 * 2 * plen);
        FloatBuffer currentBuffer = BufferUtils.newFloatBuffer(3 * 2 * plen);
        FloatBuffer nextBuffer = BufferUtils.newFloatBuffer(3 * 2 * plen);
        FloatBuffer directionBuffer = BufferUtils.newFloatBuffer(2 * 2 * plen);
        FloatBuffer colorBuffer = BufferUtils.newFloatBuffer(4 * 2 * plen);

        int dir = -1;
        for (int i = 0; i < 2 * plen; i++) {
            directionBuffer.put(dir);
            directionBuffer.put(-dir);
        }

        addPoint(previousBuffer, points, 0, 3);
        addPoint(currentBuffer, points, 0, 3);
        addPoint(nextBuffer, points, 3, 3);
        addPoint(colorBuffer, colors, 0, 4);
        for (int i = 1; i < plen - 1; i++) {
            addPoint(previousBuffer, points, 3 * (i - 1), 3);
            addPoint(currentBuffer, points, 3 * i, 3);
            addPoint(nextBuffer, points, 3 * (i + 1), 3);
            addPoint(colorBuffer, colors, 4 * i, 4);
        }
        addPoint(previousBuffer, points, 3 * (plen - 2), 3);
        addPoint(currentBuffer, points, 3 * (plen - 1), 3);
        addPoint(nextBuffer, points, 3 * (plen - 1), 3);
        addPoint(colorBuffer, colors, 4 * (plen - 1), 4);

        previousBuffer.rewind();
        currentBuffer.rewind();
        nextBuffer.rewind();
        directionBuffer.rewind();
        colorBuffer.rewind();

        vbos[0].bindBufferData(gl, previousBuffer, Buffers.SIZEOF_FLOAT);
        vbos[1].bindBufferData(gl, currentBuffer, Buffers.SIZEOF_FLOAT);
        vbos[2].bindBufferData(gl, nextBuffer, Buffers.SIZEOF_FLOAT);
        vbos[3].bindBufferData(gl, directionBuffer, Buffers.SIZEOF_FLOAT);
        vbos[4].bindBufferData(gl, colorBuffer, Buffers.SIZEOF_FLOAT);

        IntBuffer indexBuffer = gen_indices(plen);
        ivbo.bindBufferData(gl, indexBuffer, Buffers.SIZEOF_INT);
    }

    public void init(GL2 gl) {
        if (!inited) {
            vboAttribRefs = new int[] { GLSLLineShader.previousRef, GLSLLineShader.currentRef, GLSLLineShader.nextRef,
                    GLSLLineShader.directionRef, GLSLLineShader.colorRef };
            initVBOs(gl);
            inited = true;
        }
    }

    public void dispose(GL2 gl) {
        if (inited) {
            disposeVBOs(gl);
            inited = false;
        }
    }

}
