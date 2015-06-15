package org.helioviewer.jhv.layers;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import javax.swing.Timer;

import org.helioviewer.base.datetime.ImmutableDateTime;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.renderable.components.RenderableImageLayer;
import org.helioviewer.viewmodel.view.AbstractView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.MovieView.AnimationMode;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

public class Layers {

    private static AbstractView activeView;
    private static final ArrayList<AbstractView> layers = new ArrayList<AbstractView>();

    /**
     * Returns the view at a given position within the stack of layers.
     *
     * @param index
     *            Position within the stack of layers
     * @return View at given position
     */
    private static AbstractView getLayer(int index) {
        try {
            return layers.get(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns number of layers
     *
     * @return Number of layers
     * @see #getNumberOfVisibleLayer
     */
    public static int getNumLayers() {
        return layers.size();
    }

    /**
     * Return the view associated with the active Layer
     *
     * @return View associated with the active Layer
     */
    public static AbstractView getActiveView() {
        return activeView;
    }

    public static void setActiveView(AbstractView view) {
        activeView = view;
        setMasterMovie(view);
        fireActiveLayerChanged(view);
    }

    private static MovieView masterView;
    private static NextFrameCandidateChooser nextFrameCandidateChooser = new NextFrameCandidateLoopChooser();
    private static FrameChooser frameChooser = new RelativeFrameChooser();

    private static final Timer frameTimer = new Timer(1000 / 20, new FrameListener());

    private static class FrameListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (masterView != null)
                setFrame(frameChooser.moveToNextFrame(masterView.getCurrentFrameNumber()));
        }
    }

    private static void setMasterMovie(AbstractView view) {
        if (view instanceof MovieView)
            masterView = (MovieView) view;
        else
            masterView = null;
        nextFrameCandidateChooser.setMaxFrame();

        MoviePanel.getSingletonInstance().setActiveMovie(masterView);
    }

    // accessed from reader thread, tbd
    public static boolean isMoviePlaying() {
        return frameTimer.isRunning();
    }

    public static void playMovies() {
        if (masterView != null) {
            frameTimer.restart();
            MoviePanel.playStateChanged(true);
        }
    }

    public static void pauseMovies() {
        if (masterView != null) {
            frameTimer.stop();
            MoviePanel.playStateChanged(false);
        }
    }

    public static void setFrame(int frame) {
        if (masterView != null) {
            ImmutableDateTime dateTime = masterView.getFrameDateTime(frame);
            if (dateTime == null) // out of range
                return;

            for (AbstractView movieView : layers) {
                if (movieView instanceof MovieView) {
                    ((MovieView) movieView).setCurrentFrame(dateTime);
                }
            }
            MoviePanel.getSingletonInstance().setFrameSlider(frame);
        }
    }

    private static ImmutableDateTime getStartDateImmutable(AbstractView view) {
        ImmutableDateTime result = null;

        if (view instanceof MovieView) {
            result = ((MovieView) view).getFrameDateTime(0);
        } else {
            result = view.getMetaData().getDateObs();
        }
        return result;
    }

    private static ImmutableDateTime getEndDateImmutable(AbstractView view) {
        ImmutableDateTime result = null;

        if (view instanceof MovieView) {
            MovieView tmv = (MovieView) view;
            int lastFrame = tmv.getMaximumFrameNumber();
            result = tmv.getFrameDateTime(lastFrame);
        } else {
            result = view.getMetaData().getDateObs();
        }
        return result;
    }

    private static ImmutableDateTime getStartDateImmutable(int idx) {
        return getStartDateImmutable(getLayer(idx));
    }

    private static ImmutableDateTime getEndDateImmutable(int idx) {
        return getEndDateImmutable(getLayer(idx));
    }

    /**
     * Return the timestamp of the first available image data of the layer in
     * question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return timestamp of the first available image data, null if no
     *         information available
     */
    public static Date getStartDate(AbstractView view) {
        Date result = null;
        ImmutableDateTime date = getStartDateImmutable(view);

        if (date != null)
            result = date.getTime();
        return result;
    }

    /**
     * Return the timestamp of the last available image data of the layer in
     * question
     *
     * @param view
     *            - View that can be associated with the layer in question
     * @return timestamp of the last available image data, null if no
     *         information available
     */
    public static Date getEndDate(AbstractView view) {
        Date result = null;
        ImmutableDateTime date = getEndDateImmutable(view);

        if (date != null)
            result = date.getTime();
        return result;
    }

    /**
     * Return the timestamp of the first available image data
     *
     * @return timestamp of the first available image data, null if no
     *         information available
     */
    public static Date getFirstDate() {
        ImmutableDateTime earliest = null;

        int size = layers.size();
        for (int idx = 0; idx < size; idx++) {
            ImmutableDateTime start = getStartDateImmutable(idx);
            if (start == null) {
                continue;
            }
            if (earliest == null || start.compareTo(earliest) < 0) {
                earliest = start;
            }
        }
        return earliest == null ? null : earliest.getTime();
    }

    /**
     * Return the timestamp of the last available image data
     *
     * @return timestamp of the last available image data, null if no
     *         information available
     */
    public static Date getLastDate() {
        ImmutableDateTime latest = null;

        int size = layers.size();
        for (int idx = 0; idx < size; idx++) {
            ImmutableDateTime end = getEndDateImmutable(idx);
            if (end == null) {
                continue;
            }
            if (latest == null || end.compareTo(latest) > 0) {
                latest = end;
            }
        }
        return latest == null ? null : latest.getTime();
    }

    public static void addLayer(AbstractView view) {
        if (view == null)
            return;

        view.setImageLayer(new RenderableImageLayer(view));
        layers.add(view);

        fireLayerAdded(view);
        setActiveView(view);
    }

    // special
    public static void addLayerFromThread(AbstractView view) {
        EventQueue.invokeLater(new Runnable() {
            private AbstractView theView;

            @Override
            public void run() {
                addLayer(theView);
            }

            public Runnable init(AbstractView _view) {
                theView = _view;
                return this;
            }
        }.init(view));
    }

    /**
     * Check if the given index is valid, given the current state of the
     * ViewChain
     *
     * @param idx
     *            - index of the layer in question
     * @return true if the index is valid
     */
    private static boolean isValidIndex(int idx) {
        if (idx >= 0 && idx < layers.size()) {
            return true;
        }
        return false;
    }

    /**
     * Calculate a new activeLayer after the old Layer has been deleted
     *
     * @param oldActiveLayerIdx
     *            - index of old active, but deleted, layer
     * @return the index of the new active layer to choose, or -1 if no suitable
     *         new layer can be found
     */
    private static int determineNewActiveLayer(int oldActiveLayerIdx) {
        int candidate = oldActiveLayerIdx;
        if (!isValidIndex(candidate)) {
            candidate = layers.size() - 1;
        }

        return candidate;
    }

    /**
     * Remove the layer in question
     *
     * @param view
     *            - View that can be associated with the layer in question
     */
    public static void removeLayer(AbstractView view) {
        int index = layers.indexOf(view);

        layers.remove(view);
        if (view instanceof JHVJP2View) {
            ((JHVJP2View) view).abolish();
        }

        setActiveView(getLayer(determineNewActiveLayer(index)));
    }

    public static void removeLayer(int idx) {
        removeLayer(getLayer(idx));
    }

    private static void fireLayerAdded(AbstractView view) {
        for (LayersListener ll : layerListeners) {
            ll.layerAdded(view);
        }
    }

    private static void fireActiveLayerChanged(AbstractView view) {
        for (LayersListener ll : layerListeners) {
            ll.activeLayerChanged(view);
        }
    }

    private static final HashSet<LayersListener> layerListeners = new HashSet<LayersListener>();

    public static void addLayersListener(LayersListener layerListener) {
        layerListeners.add(layerListener);
    }

    public static void removeLayersListener(LayersListener layerListener) {
        layerListeners.remove(layerListener);
    }

    public static void setDesiredRelativeSpeed(int fps) {
        frameTimer.setDelay(1000 / fps);
    }

    public static void setAnimationMode(AnimationMode mode) {
        switch (mode) {
        case LOOP:
            nextFrameCandidateChooser = new NextFrameCandidateLoopChooser();
            break;
        case STOP:
            nextFrameCandidateChooser = new NextFrameCandidateStopChooser();
            break;
        case SWING:
            nextFrameCandidateChooser = new NextFrameCandidateSwingChooser();
            break;
        }
        nextFrameCandidateChooser.setMaxFrame();
    }

    private static abstract class NextFrameCandidateChooser {
        protected int maxFrame;

        protected void setMaxFrame() {
            if (masterView == null)
                maxFrame = 0;
            else
                maxFrame = masterView.getMaximumFrameNumber();
        }

        protected void resetStartTime(int frame) {
            if (frameChooser instanceof AbsoluteFrameChooser) {
                ((AbsoluteFrameChooser) frameChooser).resetStartTime(frame);
            }
        }

        public abstract int getNextCandidate(int lastCandidate);
    }

    private static class NextFrameCandidateLoopChooser extends NextFrameCandidateChooser {
        @Override
        public int getNextCandidate(int lastCandidate) {
            if (++lastCandidate > maxFrame) {
                System.gc();
                resetStartTime(0);
                return 0;
            }
            return lastCandidate;
        }
    }

    private static class NextFrameCandidateStopChooser extends NextFrameCandidateChooser {
        @Override
        public int getNextCandidate(int lastCandidate) {
            if (++lastCandidate > maxFrame) {
                pauseMovies();
                resetStartTime(0);
                return 0;
            }
            return lastCandidate;
        }
    }

    private static class NextFrameCandidateSwingChooser extends NextFrameCandidateChooser {
        private int currentDirection = 1;

        @Override
        public int getNextCandidate(int lastCandidate) {
            lastCandidate += currentDirection;
            if (lastCandidate < 0 && currentDirection == -1) {
                currentDirection = 1;
                resetStartTime(0);
                return 1;
            } else if (lastCandidate > maxFrame && currentDirection == 1) {
                currentDirection = -1;
                resetStartTime(maxFrame);
                return maxFrame - 1;
            }

            return lastCandidate;
        }
    }

    private interface FrameChooser {
        public int moveToNextFrame(int frameNumber);
    }

    private static class RelativeFrameChooser implements FrameChooser {
        @Override
        public int moveToNextFrame(int frame) {
            return nextFrameCandidateChooser.getNextCandidate(frame);
        }
    }

    private static class AbsoluteFrameChooser implements FrameChooser {

        private long[] obsMillis;
        private long absoluteStartTime;
        private long systemStartTime;

        public AbsoluteFrameChooser(long[] _obsMillis) {
            obsMillis = _obsMillis;
        }

        public void resetStartTime(int frame) {
            absoluteStartTime = obsMillis[frame];
            systemStartTime = System.currentTimeMillis();
        }

        @Override
        public int moveToNextFrame(int frame) {
            int lastCandidate, nextCandidate = frame;
            long lastDiff, nextDiff = -Long.MAX_VALUE;

            do {
                lastCandidate = nextCandidate;
                nextCandidate = nextFrameCandidateChooser.getNextCandidate(nextCandidate);

                lastDiff = nextDiff;
                nextDiff = Math.abs(obsMillis[nextCandidate] - absoluteStartTime) - (System.currentTimeMillis() - systemStartTime);
            } while (nextDiff < 0);

            if (-lastDiff < nextDiff) {
                return lastCandidate;
                // return lastDiff;
            } else {
                return nextCandidate;
                // return nextDiff;
            }
        }
    }

}
