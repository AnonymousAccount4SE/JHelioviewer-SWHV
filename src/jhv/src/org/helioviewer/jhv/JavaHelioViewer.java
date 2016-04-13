package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JComponent;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.astronomy.Sun;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.logging.LogSettings;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.base.plugin.controller.PluginManager;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.resourceloader.SystemProperties;
import org.helioviewer.jhv.threads.JHVExecutor;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduMessageSystem;

/**
 * This class starts the applications.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Markus Langenberg
 * @author Stephan Pagel
 * @author Andre Dau
 * @author Helge Dietert
 *
 */
public class JavaHelioViewer {

    public static void main(String[] args) {
        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLineProcessor.getUsageMessage());
            return;
        }
        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();

        // Save command line arguments
        CommandLineProcessor.setArguments(args);

        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());

        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Save current default locale to user.locale
        System.setProperty("user.locale", Locale.getDefault().toString());

        // Per default, the us locale should be used
        Locale.setDefault(Locale.US);

        // init log
        LogSettings.init("/settings/log4j.initial.properties", JHVDirectory.SETTINGS.getPath() + "log4j.properties", JHVDirectory.LOGS.getPath(), CommandLineProcessor.isOptionSet("--use-existing-log-time-stamp"));

        // Information log message
        StringBuilder argString = new StringBuilder();
        for (int i = 0; i < args.length; ++i) {
            argString.append(" ").append(args[i]);
        }
        Log.info("JHelioviewer started with command-line options:" + argString);

        // This attempts to create the necessary directories for the application
        Log.info("Create directories...");
        JHVGlobals.createDirs();

        // Save the log settings. Must be done AFTER the directories are created
        LogSettings.getSingletonInstance().update();

        // Read the version and revision from the JAR metafile
        JHVGlobals.determineVersionAndRevision();

        Log.info("Initializing JHelioviewer");
        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been
        // initialized
        Log.info("Load settings");
        Settings.getSingletonInstance().load();

        // Set the platform system properties
        SystemProperties.setPlatform();
        Log.info("OS: " + System.getProperty("jhv.os") + " - arch: " + System.getProperty("jhv.arch") + " - java arch: " + System.getProperty("jhv.java.arch"));

        Log.debug("Instantiate Kakadu engine");
        try {
            JHVLoader.copyKDULibs();
            KakaduMessageSystem engine = new KakaduMessageSystem();
            engine.startKduMessageSystem();
        } catch (Exception e) {
            Message.err("Failed to setup Kakadu", e.getMessage(), true);
            return;
        }

        FileUtils.deleteDir(JHVDirectory.PLUGINSCACHE.getFile());
        JHVDirectory.PLUGINSCACHE.getFile().mkdirs();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                JHVExecutor.setSwingWorkersExecutorService(10);

                TimeUtils.getSingletonInstance(); // instantiate class
                Sun.getSingletonInstance();

                Settings.getSingletonInstance().setLookAndFeelEverywhere(null, null); // for Windows and testing
                UIGlobals.getSingletonInstance().setUIFont(UIGlobals.UIFont);

                Log.info("Start main window");
                ExitHooks.attach();
                ImageViewerGui.prepareGui();
                ImageViewerGui.loadImagesAtStartup();

                DataSources.getSingletonInstance(); // query server for data

                Log.info("Load plugin settings");
                PluginManager.getSingletonInstance().loadSettings(JHVDirectory.PLUGINS.getPath());

                try {
                    if (theArgs.length != 0 && theArgs[0].equals("--exclude-plugins")) {
                        Log.info("Do not load plugins");
                    } else if (theArgs.length != 0 && theArgs[0].equals("--remote-plugins")) {
                        Log.info("Load remote plugins -- not recommended");
                        JHVLoader.loadRemotePlugins(theArgs);
                    } else {
                        Log.info("Load bundled plugins");
                        JHVLoader.loadBundledPlugin("EVEPlugin.jar");
                        JHVLoader.loadBundledPlugin("SWEKPlugin.jar");
                        JHVLoader.loadBundledPlugin("PfssPlugin.jar");
                        JHVLoader.loadBundledPlugin("SWHVHEKPlugin.jar");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                // after loading plugins fix the minimum width of left pane
                JComponent leftScrollPane = ImageViewerGui.getLeftScrollPane();
                leftScrollPane.setMinimumSize(new Dimension(leftScrollPane.getPreferredSize().width + ImageViewerGui.SIDE_PANEL_WIDTH_EXTRA, -1));
                ImageViewerGui.getMainFrame().pack();

                try {
                    JHVUpdate update = new JHVUpdate(false);
                    update.check();
                } catch (Exception e) {
                    // Should never happen
                    Log.error("Error retrieving internal update URL", e);
                }
            }

            private String[] theArgs;

            public Runnable init(String[] _args) {
                theArgs = _args;
                return this;
            }

        }.init(args));
    }

}
