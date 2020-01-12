package org.helioviewer.jhv.view.j2k;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.Nullable;

import kdu_jni.KduException;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_quality_limiter;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.imagedata.ImageBuffer;
import org.helioviewer.jhv.imagedata.SubImage;
import org.helioviewer.jhv.view.j2k.image.DecodeParams;

import org.lwjgl.system.MemoryUtil;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

//import com.google.common.math.StatsAccumulator;
//import com.google.common.base.Stopwatch;

class J2KDecoder implements Runnable {

    // Maximum of samples to process per rendering iteration
    private static final int MAX_RENDER_SAMPLES = 256 * 1024;
    private static final int[] firstComponent = {0};
    private static final Kdu_quality_limiter qualityLow = new Kdu_quality_limiter(2f / 256);
    private static final Kdu_quality_limiter qualityHigh = new Kdu_quality_limiter(1f / 256);

    private static final ThreadLocal<Cache<DecodeParams, ImageBuffer>> decodeCache = ThreadLocal.withInitial(() -> CacheBuilder.newBuilder().softValues().build());
    private static final ThreadLocal<Kdu_thread_env> localThread = ThreadLocal.withInitial(J2KDecoder::createThreadEnv);

    private final DecodeParams decodeParams;

    //private final Stopwatch sw = Stopwatch.createUnstarted();
    //private static final ThreadLocal<StatsAccumulator> localAcc = ThreadLocal.withInitial(StatsAccumulator::new);

    J2KDecoder(DecodeParams _decodeParams) {
        decodeParams = _decodeParams;
    }

    private ImageBuffer decodeLayer(DecodeParams params) throws KduException {
        ImageBuffer imageBuffer = decodeCache.get().getIfPresent(decodeParams);
        if (imageBuffer != null)
            return imageBuffer;

        //sw.reset().start();

        SubImage subImage = params.subImage;
        int frame = params.frame;
        int numComponents = params.view.getNumComponents(frame);

        Kdu_region_compositor compositor = createCompositor(params.view, params.factor < 1 ? qualityLow : qualityHigh);

        Kdu_dims empty = new Kdu_dims();
        if (numComponents < 3) {
            // alpha tbd
            compositor.Add_primitive_ilayer(frame, firstComponent, Kdu_global.KDU_WANT_CODESTREAM_COMPONENTS, empty, empty);
        } else {
            compositor.Add_ilayer(frame, empty, empty);
        }

        compositor.Set_scale(false, false, false, 1f / (1 << params.resolution.level), (float) params.factor);

        Kdu_dims requestedRegion = new Kdu_dims();
        requestedRegion.From_u32(subImage.x, subImage.y, subImage.width, subImage.height);
        compositor.Set_buffer_surface(requestedRegion);

        Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(empty, true); // modifies empty
        Kdu_dims actualRegion = compositorBuf.Get_rendering_region();

        Kdu_coords actualPos = actualRegion.Access_pos();
        int actualX = actualPos.Get_x(), actualY = actualPos.Get_y();

        Kdu_coords actualSize = actualRegion.Access_size();
        int actualWidth = actualSize.Get_x(), actualHeight = actualSize.Get_y();

        int[] srcStride = new int[1];
        long addr = compositorBuf.Get_buf(srcStride, false);

        ImageBuffer.Format format = numComponents < 3 ? ImageBuffer.Format.Gray8 : ImageBuffer.Format.ARGB32;
        byte[] byteBuffer = new byte[actualWidth * actualHeight * format.bytes];

        Kdu_dims newRegion = new Kdu_dims();
        while (compositor.Process(MAX_RENDER_SAMPLES, newRegion)) {
            Kdu_coords newSize = newRegion.Access_size();
            int newWidth = newSize.Get_x();
            int newHeight = newSize.Get_y();
            if (newWidth * newHeight == 0)
                continue;

            Kdu_coords newOffset = newRegion.Access_pos();
            int newX = newOffset.Get_x() - actualX;
            int newY = newOffset.Get_y() - actualY;

            int dstIdx = newX + newY * actualWidth;
            int srcIdx = 0;

            if (numComponents < 3) {
                for (int row = 0; row < newHeight; row++, dstIdx += actualWidth, srcIdx += srcStride[0]) {
                    for (int col = 0; col < newWidth; ++col) {
                        byteBuffer[dstIdx + col] = MemoryUtil.memGetByte(addr + 4 * (srcIdx + col));
                    }
                }
            } else {
                for (int row = 0; row < newHeight; row++, dstIdx += actualWidth, srcIdx += srcStride[0]) {
                    for (int col = 0; col < newWidth; ++col) {
                        for (int idx = 0; idx < 4; ++idx)
                            byteBuffer[4 * (dstIdx + col) + idx] = MemoryUtil.memGetByte(addr + 4 * (srcIdx + col) + idx);
                    }
                }
            }
        }

        destroyCompositor(compositor);
/*
        StatsAccumulator acc = localAcc.get();
        acc.add(sw.elapsed().toNanos() / 1e9);
        if (acc.count() == params.view.getMaximumFrameNumber() + 1)
            System.out.println(">>> mean: " + acc.mean() + " stdvar: " + acc.sampleStandardDeviation());
*/
        imageBuffer = new ImageBuffer(actualWidth, actualHeight, format, ByteBuffer.wrap(byteBuffer).order(ByteOrder.nativeOrder()));
        if (decodeParams.complete) {
            decodeCache.get().put(decodeParams, imageBuffer);
        }
        return imageBuffer;
    }

    @Override
    public void run() {
        if (decodeParams == null) {
            abolish();
            return;
        }

        try {
            Thread.currentThread().setName("Decoder " + decodeParams.view.getName());
            ImageBuffer data = decodeLayer(decodeParams);
            decodeParams.view.setDataFromDecoder(decodeParams, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private static Kdu_thread_env createThreadEnv() {
        try {
            Kdu_thread_env kte = new Kdu_thread_env();
            kte.Create();
            int numThreads = Math.min(4, Kdu_global.Kdu_get_num_processors());
            for (int i = 1; i < numThreads; i++)
                kte.Add_thread();
            // System.out.println(">>>> Kdu_thread_env create " + kte);
            return kte;
        } catch (KduException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Kdu_region_compositor createCompositor(J2KView view, Kdu_quality_limiter quality) throws KduException {
        Kdu_region_compositor krc = new Kdu_region_compositor();
        // System.out.println(">>>> compositor create " + krc + " " + Thread.currentThread().getName());
        krc.Create(view.getSource().getJpxSource());
        krc.Set_surface_initialization_mode(false);
        krc.Set_quality_limiting(quality, -1, -1);
        krc.Set_thread_env(localThread.get(), null);
        return krc;
    }

    private static void destroyCompositor(Kdu_region_compositor krc) {
        try {
            // System.out.println(">>>> compositor destroy " + krc + " " + Thread.currentThread().getName());
            krc.Halt_processing();
            krc.Remove_ilayer(new Kdu_ilayer_ref(), true);
            krc.Set_thread_env(null, null);
            krc.Native_destroy();
        } catch (KduException e) {
            e.printStackTrace();
        }
    }

    private static void abolish() {
        try {
            Kdu_thread_env kte = localThread.get();
            if (kte != null) {
                kte.Destroy();
                localThread.set(null);
            }
            decodeCache.get().invalidateAll();
        } catch (KduException e) {
            e.printStackTrace();
        }
    }

}
