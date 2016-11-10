package org.helioviewer.jhv.plugins.pfssplugin;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.helioviewer.jhv.base.plugin.interfaces.Plugin;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssCache;
import org.helioviewer.jhv.threads.JHVThread;

public class PfssPlugin implements Plugin {

    private static final PfssCache pfssCache = new PfssCache();
    private static final PfssRenderable renderable = new PfssRenderable();

    private static final BlockingQueue<Runnable> newLoadBlockingQueue = new ArrayBlockingQueue<>(1);
    public static final ExecutorService pfssNewLoadPool = new ThreadPoolExecutor(0, 1, 10L, TimeUnit.MINUTES, newLoadBlockingQueue, new JHVThread.NamedThreadFactory("PFSS NewLoad"), new ThreadPoolExecutor.DiscardPolicy());

    public static final ScheduledExecutorService pfssReaperPool = new ScheduledThreadPoolExecutor(1, new JHVThread.NamedThreadFactory("PFSS Reaper"), new ThreadPoolExecutor.DiscardPolicy());

    private static final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(1024);
    public static final ExecutorService pfssDataPool = new ThreadPoolExecutor(0, 5, 10L, TimeUnit.MINUTES, blockingQueue, new JHVThread.NamedThreadFactory("PFSS DataLoad"), new ThreadPoolExecutor.DiscardPolicy());

    public static PfssCache getPfsscache() {
        return pfssCache;
    }

    @Override
    public void installPlugin() {
        Layers.addTimespanListener(renderable);
        ImageViewerGui.getRenderableContainer().addRenderable(renderable);
    }

    @Override
    public void uninstallPlugin() {
        ImageViewerGui.getRenderableContainer().removeRenderable(renderable);
        Layers.removeTimespanListener(renderable);
    }

    @Override
    public String getDescription() {
        return "This plugin visualizes PFSS model data";
    }

    @Override
    public String getName() {
        return "PFSS Plugin " + "$Rev$";
    }

    @Override
    public String getAboutLicenseText() {
        String description = "<p>The plugin uses the <a href=\"http://heasarc.gsfc.nasa.gov/docs/heasarc/fits/java/v1.0/\">FITS in Java</a> Library, licensed under a <a href=\"https://www.gnu.org/licenses/old-licenses/gpl-1.0-standalone.html\">GPL License</a>.";
        return description + "<p>The plugin uses the <a href=\"http://www.bzip.org\">Bzip2</a> Library, licensed under the <a href=\"http://opensource.org/licenses/bsd-license.php\">BSD License</a>.";
    }

    @Override
    public void setState(String state) {
    }

    @Override
    public String getState() {
        return null;
    }

}
