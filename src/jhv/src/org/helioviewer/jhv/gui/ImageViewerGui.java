package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.AbstractList;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.helioviewer.base.message.Message;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.components.MainContentPanel;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.StatusPanel;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.components.statusplugins.FramerateStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.PositionStatusPanel;
import org.helioviewer.jhv.gui.components.statusplugins.ZoomStatusPanel;
import org.helioviewer.jhv.gui.controller.InputController;
import org.helioviewer.jhv.gui.dialogs.observation.ImageDataPanel;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.gui.filters.FiltersPanel;
import org.helioviewer.jhv.io.APIRequestManager;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.io.FileDownloader;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.renderable.components.RenderableCamera;
import org.helioviewer.jhv.renderable.components.RenderableGrid;
import org.helioviewer.jhv.renderable.components.RenderableGridType;
import org.helioviewer.jhv.renderable.components.RenderableSolarAxes;
import org.helioviewer.jhv.renderable.components.RenderableSolarAxesType;
import org.helioviewer.jhv.renderable.components.RenderableTimeStamp;
import org.helioviewer.jhv.renderable.components.RenderableTimeStampType;
import org.helioviewer.jhv.renderable.gui.RenderableContainer;
import org.helioviewer.jhv.renderable.gui.RenderableContainerPanel;
import org.helioviewer.viewmodel.view.ComponentView;

/**
 * A class that sets up the graphical user interface.
 *
 * @author caplins
 * @author Benjamin Wamsler
 * @author Alen Agheksanterian
 * @author Stephan Pagel
 * @author Markus Langenberg
 * @author Andre Dau
 *
 */
public class ImageViewerGui {

    public static final int SIDE_PANEL_WIDTH = 320;

    private static ImageViewerGui instance;

    private static JFrame mainFrame;
    private static JSplitPane midSplitPane;
    private static JScrollPane leftScrollPane;

    private static SideContentPane leftPane;
    private static FiltersPanel filtersPanel;

    private static ComponentView componentView;
    private static InputController inputController;
    private static MainContentPanel mainContentPanel;

    private static ImageDataPanel imageObservationPanel;
    private static ObservationDialog observationDialog;

    private static ZoomStatusPanel zoomStatus;
    private static FramerateStatusPanel framerateStatus;

    private static RenderableContainer renderableContainer;
    private static RenderableCamera renderableCamera;

    private ImageViewerGui() {
    }

    public static ImageViewerGui getSingletonInstance() {
        if (instance == null) {
            prepareGui();
            instance = new ImageViewerGui();
        }
        return instance;
    }

    public static void prepareGui() {
        mainFrame = createMainFrame();

        JMenuBar menuBar = new MenuBar();
        mainFrame.setJMenuBar(menuBar);

        JPanel contentPanel = new JPanel(new BorderLayout());
        mainFrame.setContentPane(contentPanel);

        midSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
        midSplitPane.setBorder(null);
        midSplitPane.setOneTouchExpandable(false);
        midSplitPane.setDividerSize(6);
        contentPanel.add(midSplitPane, BorderLayout.CENTER);

        TopToolBar topToolBar = new TopToolBar();
        contentPanel.add(topToolBar, BorderLayout.PAGE_START);

        leftPane = new SideContentPane();
        // Movie control
        leftPane.add("Movie Controls", MoviePanel.getSingletonInstance(), true);

        // Layer control
        imageObservationPanel = new ImageDataPanel();
        observationDialog = new ObservationDialog(mainFrame);
        observationDialog.addUserInterface("Image data", imageObservationPanel);

        renderableContainer = new RenderableContainer();
        renderableContainer.addRenderable(new RenderableSolarAxes(new RenderableSolarAxesType("Solar axes")));
        renderableContainer.addRenderable(new RenderableGrid(new RenderableGridType("Grid")));
        renderableCamera = new RenderableCamera();
        renderableContainer.addRenderable(renderableCamera);
        renderableContainer.addRenderable(new RenderableTimeStamp(new RenderableTimeStampType("Timestamp")));

        RenderableContainerPanel renderableContainerPanel = new RenderableContainerPanel(renderableContainer);

        leftPane.add("Image Layers", renderableContainerPanel, true);
        leftScrollPane = new JScrollPane(leftPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setFocusable(false);
        leftScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        componentView = new ComponentView();
        Component canvas = componentView.getComponent();
        inputController = new InputController(canvas);
        mainContentPanel = new MainContentPanel(canvas);

        midSplitPane.setLeftComponent(leftScrollPane);
        midSplitPane.setRightComponent(mainContentPanel);

        // STATUS PANEL
        zoomStatus = new ZoomStatusPanel();
        framerateStatus = new FramerateStatusPanel();

        PositionStatusPanel positionStatus = new PositionStatusPanel();
        inputController.addPlugin(positionStatus);

        StatusPanel statusPanel = new StatusPanel(SIDE_PANEL_WIDTH + 20, 5);
        statusPanel.addPlugin(zoomStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(framerateStatus, StatusPanel.Alignment.LEFT);
        statusPanel.addPlugin(positionStatus, StatusPanel.Alignment.RIGHT);

        contentPanel.add(statusPanel, BorderLayout.PAGE_END);

        // extra widgets
        filtersPanel = new FiltersPanel();

        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);

        // force GLCanvas initialisation for pixel scale
        componentView.display();
    }

    private static JFrame createMainFrame() {
        JFrame frame = new JFrame(JHVGlobals.getProgramName());

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                ExitProgramAction exitAction = new ExitProgramAction();
                exitAction.actionPerformed(new ActionEvent(this, 0, ""));
            }
        });

        Dimension maxSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
        Dimension minSize = new Dimension(800, 600);
        minSize.width = Math.min(minSize.width, maxSize.width);
        minSize.height = Math.min(minSize.height, maxSize.height);

        frame.setMinimumSize(minSize);
        frame.setPreferredSize(new Dimension(maxSize.width - 100, maxSize.height - 100));
        enableFullScreen(frame);

        return frame;
    }

    private static void enableFullScreen(Window window) {
        if (System.getProperty("jhv.os").equals("mac")) {
            try {
                Class<?> fullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
                Method setWindowCanFullScreen = fullScreenUtilities.getMethod("setWindowCanFullScreen", Window.class, boolean.class);
                setWindowCanFullScreen.invoke(fullScreenUtilities, window, true);

                Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
                Method getSingletonApplication = applicationClass.getMethod("getApplication", (Class<?>[]) null);
                Object application = getSingletonApplication.invoke(applicationClass.newInstance());
                Method removeAboutMenuItem = applicationClass.getMethod("removeAboutMenuItem", (Class<?>[]) null);
                removeAboutMenuItem.invoke(application);

                System.setProperty("apple.laf.useScreenMenuBar", "true");
            } catch (Exception e) {
                throw new RuntimeException("FullScreen utilities not available", e);
            }
        }
    }

    public static void loadAtStart() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                loadImagesAtStartup();
            }
        }, "LoadImagesOnStartUp");
        thread.start();
    }

    /**
     * Loads the images which have to be displayed when the program starts.
     *
     * If there are any images defined in the command line, than this messages
     * tries to load this images. Otherwise it tries to load a default image
     * which is defined by the default entries of the observation panel.
     * */
    private static void loadImagesAtStartup() {
        // get values for different command line options
        AbstractList<URI> jpipUris = CommandLineProcessor.getJPIPOptionValues();
        AbstractList<URI> downloadAddresses = CommandLineProcessor.getDownloadOptionValues();
        AbstractList<URI> jpxUrls = CommandLineProcessor.getJPXOptionValues();

        // Do nothing if no resource is specified
        if (jpipUris.isEmpty() && downloadAddresses.isEmpty() && jpxUrls.isEmpty()) {
            return;
        }

        // -jpx
        for (URI jpxUrl : jpxUrls) {
            if (jpxUrl != null) {
                try {
                    LayersModel.addLayerFromThread(APIRequestManager.loadView(jpxUrl, jpxUrl));
                } catch (IOException e) {
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                }
            }
        }
        // -jpip
        for (URI jpipUri : jpipUris) {
            if (jpipUri != null) {
                try {
                    LayersModel.addLayerFromThread(APIRequestManager.loadView(jpipUri, jpipUri));
                } catch (IOException e) {
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                }
            }
        }
        // -download
        for (URI downloadAddress : downloadAddresses) {
            if (downloadAddress != null) {
                try {
                    FileDownloader fileDownloader = new FileDownloader();
                    File downloadFile = fileDownloader.getDefaultDownloadLocation(downloadAddress);
                    fileDownloader.get(downloadAddress, downloadFile);
                    URI uri = downloadFile.toURI();

                    LayersModel.addLayerFromThread(APIRequestManager.loadView(uri, uri));
                } catch (IOException e) {
                    Message.err("An error occured while opening the remote file!", e.getMessage(), false);
                }
            }
        }
    }

    /**
     * Toggles the visibility of the control panel on the left side.
     */
    public static void toggleShowSidePanel() {
        leftScrollPane.setVisible(!leftScrollPane.isVisible());

        int lastLocation = midSplitPane.getLastDividerLocation();
        if (lastLocation > 10) {
            midSplitPane.setDividerLocation(lastLocation);
        } else {
            midSplitPane.setDividerLocation(SIDE_PANEL_WIDTH);
        }
    }

    public static JFrame getMainFrame() {
        return mainFrame;
    }

    public static SideContentPane getLeftContentPane() {
        return leftPane;
    }

    public static MainContentPanel getMainContentPanel() {
        return mainContentPanel;
    }

    public static ObservationDialog getObservationDialog() {
        return observationDialog;
    }

    public static ImageDataPanel getObservationImagePane() {
        return imageObservationPanel;
    }

    public static ComponentView getComponentView() {
        return componentView;
    }

    public static InputController getInputController() {
        return inputController;
    }

    public static FiltersPanel getFiltersPanel() {
        return filtersPanel;
    }

    public static ZoomStatusPanel getZoomStatusPanel() {
        return zoomStatus;
    }

    public static FramerateStatusPanel getFramerateStatusPanel() {
        return framerateStatus;
    }

    public static RenderableCamera getRenderableCamera() {
        return renderableCamera;
    }

    public static RenderableContainer getRenderableContainer() {
        return renderableContainer;
    }

}
