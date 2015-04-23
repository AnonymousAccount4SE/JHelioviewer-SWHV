package org.helioviewer.jhv.display;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.Timer;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventHighlightListener;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.statusplugins.FramerateStatusPanel;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugin.renderable.RenderableContainer;
import org.helioviewer.jhv.plugin.renderable.RenderableContainerPanel;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

public class Displayer implements JHVEventHighlightListener {

    private static final RenderableContainer renderableContainer = new RenderableContainer();
    private static final RenderableContainerPanel renderableContainerPanel = new RenderableContainerPanel(renderableContainer);
    private static final LayersModel layersModel = new LayersModel();

    private static final ArrayList<DisplayListener> listeners = new ArrayList<DisplayListener>();
    private static final ArrayList<RenderListener> renderListeners = new ArrayList<RenderListener>();
    private static final ArrayList<TimeListener> timeListeners = new ArrayList<TimeListener>();

    private static Date lastTimestamp;

    private static boolean torender = false;
    private static boolean todisplay = false;

    private final Timer timer = new Timer(1000 / 20, new MyListener());

    private static final FramerateStatusPanel framerateStatus = FramerateStatusPanel.getSingletonInstance();

    private Displayer() {
        timer.start();
    }

    public static void render() {
        torender = true;
    }

    public static void display() {
        todisplay = true;
    }

    private class MyListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (todisplay == true) {
                todisplay = false;
                for (final DisplayListener listener : listeners) {
                    listener.display();
                }
            }

            if (torender == true) {
                torender = false;
                for (final RenderListener renderListener : renderListeners) {
                    renderListener.render();
                }
            }
        }
    }

    public static void addListener(final DisplayListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(final DisplayListener listener) {
        listeners.remove(listener);
    }

    public static void removeListeners() {
        listeners.clear();
    }

    public static void addRenderListener(final RenderListener renderListener) {
        renderListeners.add(renderListener);
    }

    public static void removeRenderListener(final RenderListener renderListener) {
        renderListeners.remove(renderListener);
    }

    public static void addTimeListener(final TimeListener timeListener) {
        timeListeners.add(timeListener);
    }

    public static void addFirstTimeListener(final TimeListener timeListener) {
        timeListeners.add(0, timeListener);
    }

    public static void removeTimeListener(final TimeListener timeListener) {
        timeListeners.remove(timeListener);
    }

    public static void fireFrameChanged(JHVJP2View view, ImmutableDateTime dateTime) {
        int idx = layersModel.findView(view);
        if (idx != -1 /* layersModel.isValidIndex(idx) */) {
            // update timestamp labels
            Displayer.getRenderablecontainer().fireTimeUpdated(view.getImageLayer());

            if (idx == layersModel.getActiveLayer() && dateTime != null) {
                framerateStatus.updateFramerate(layersModel.getFPS(view));
                MoviePanel.setFrameSlider(view);

                lastTimestamp = dateTime.getTime();
                // fire TimeChanged
                for (final TimeListener listener : timeListeners) {
                    listener.timeChanged(lastTimestamp);
                }
            }
            display();
        }
    }

    public static Date getLastUpdatedTimestamp() {
        if (lastTimestamp == null) {
            Date lastDate = layersModel.getLastDate();
            if (lastDate != null) {
                lastTimestamp = layersModel.getLastDate();
                return lastTimestamp;
            }
            return null;
        } else {
            return lastTimestamp;
        }
    }

    @Override
    public void eventHightChanged(JHVEvent event) {
        Displayer.display();
    }

    public static RenderableContainer getRenderablecontainer() {
        return renderableContainer;
    }

    public static RenderableContainerPanel getRenderableContainerPanel() {
        return renderableContainerPanel;
    }

    public static LayersModel getLayersModel() {
        return layersModel;
    }

    private static final Displayer instance = new Displayer();

    public static Displayer getSingletonInstance() {
        return instance;

    }
}
