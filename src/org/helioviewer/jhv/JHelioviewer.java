package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.UIGlobals;
import org.helioviewer.jhv.gui.UITimer;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.io.CommandLine;
import org.helioviewer.jhv.io.DataSources;
import org.helioviewer.jhv.io.SampClient;
import org.helioviewer.jhv.log.LogSettings;
import org.helioviewer.jhv.plugins.PluginManager;
import org.helioviewer.jhv.plugins.eve.EVEPlugin;
import org.helioviewer.jhv.plugins.pfss.PfssPlugin;
import org.helioviewer.jhv.plugins.swek.SWEKPlugin;

import java.lang.invoke.MethodHandles;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JHelioviewer {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    public static void main(String[] args) throws Exception {
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        System.setProperty("apple.awt.application.name", "JHelioviewer");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        if (isHeadless())
            throw new Exception("This application cannot run in a headless configuration.");

        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();
        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());
        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Save current default locale to user.locale
        System.setProperty("user.locale", Locale.getDefault().toString());
        // Per default, the us locale should be used
        Locale.setDefault(Locale.US);

        Log2.init();
        // Init log
        LogSettings.init("/settings/log4j.properties", JHVDirectory.LOGS.getPath());
        // Information log message
        LOGGER.log(Level.INFO, "JHelioviewer started with command-line options: " + String.join(" ", args));
        // System.setProperty("java.util.logging.manager", "org.apache.logging.julbridge.JULBridgeLogManager");

        // This attempts to create the necessary directories for the application
        JHVGlobals.createDirs();
        // Read the version and revision from the JAR metafile
        JHVGlobals.determineVersionAndRevision();
        Settings.load();
        // Set the platform system properties
        SystemProperties.setPlatform();
        System.setProperty("sun.awt.noerasebackground", "true");
        System.setProperty("org.sqlite.tmpdir", JHVGlobals.libCacheDir);
        System.setProperty("org.lwjgl.system.SharedLibraryExtractPath", JHVGlobals.libCacheDir);
        // System.setProperty("jsamp.nosystray", "true");
        // if (true) throw new RuntimeException("This is a Sentry test. Please ignore.");

        JHVInit.init();

        // Save command line arguments
        CommandLine.setArguments(args);
        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLine.getUsageMessage());
            return;
        }

        EventQueue.invokeLater(() -> {
            UIGlobals.setLaf();

            LOGGER.log(Level.INFO, "Start main window");
            ExitHooks.attach();
            JFrame frame = JHVFrame.prepare();

            try {
                if (args.length != 0 && args[0].equals("--exclude-plugins")) {
                    LOGGER.log(Level.INFO, "Do not load plugins");
                } else {
                    LOGGER.log(Level.INFO, "Load bundled plugins");
                    PluginManager.addPlugin(new EVEPlugin());
                    PluginManager.addPlugin(new SWEKPlugin());
                    PluginManager.addPlugin(new PfssPlugin());
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Plugin load error", e);
            }

            JComponent leftPane = JHVFrame.getLeftScrollPane();
            MoviePanel.setAdvanced(true);
            int moviePanelWidth = leftPane.getPreferredSize().width;
            MoviePanel.setAdvanced(false);
            leftPane.setMinimumSize(new Dimension(moviePanelWidth, -1));

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            UITimer.start();

            DataSources.loadSources();
            CommandLine.load();
            SampClient.init();

            new JHVUpdate(false).check();
        });
    }

    private static boolean isHeadless() {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }
        try {
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            return screens == null || screens.length == 0;
        } catch (HeadlessException e) {
            LOGGER.log(Level.SEVERE, "isHeadless", e);
            return true;
        }
    }

}
