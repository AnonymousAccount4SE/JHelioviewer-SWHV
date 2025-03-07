package org.helioviewer.jhv.gui.components;

import java.awt.Desktop;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Platform;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.actions.CheckUpdateAction;
import org.helioviewer.jhv.gui.actions.ClearAnnotationsAction;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.actions.LoadStateAction;
import org.helioviewer.jhv.gui.actions.NewLayerAction;
import org.helioviewer.jhv.gui.actions.NewSoarAction;
import org.helioviewer.jhv.gui.actions.OpenLocalFileAction;
import org.helioviewer.jhv.gui.actions.OpenURLinBrowserAction;
import org.helioviewer.jhv.gui.actions.ResetCameraAction;
import org.helioviewer.jhv.gui.actions.ResetCameraAxisAction;
import org.helioviewer.jhv.gui.actions.SaveStateAction;
import org.helioviewer.jhv.gui.actions.SaveStateAsAction;
import org.helioviewer.jhv.gui.actions.ShowDialogAction;
import org.helioviewer.jhv.gui.actions.WindowMinimizeAction;
import org.helioviewer.jhv.gui.actions.WindowZoomAction;
import org.helioviewer.jhv.gui.actions.ZoomFitAction;
import org.helioviewer.jhv.gui.actions.ZoomInAction;
import org.helioviewer.jhv.gui.actions.ZoomOneToOneAction;
import org.helioviewer.jhv.gui.actions.ZoomOutAction;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.gui.dialogs.LogDialog;
import org.helioviewer.jhv.gui.dialogs.SettingsDialog;
import org.helioviewer.jhv.layers.MovieDisplay;

// Menu bar of the main window
@SuppressWarnings("serial")
public final class MenuBar extends JMenuBar {

    public MenuBar() {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(new NewLayerAction());
        fileMenu.add(new NewSoarAction());
        fileMenu.add(new OpenLocalFileAction());
        fileMenu.addSeparator();
        fileMenu.add(new LoadStateAction());
        fileMenu.add(new SaveStateAction());
        fileMenu.add(new SaveStateAsAction());

        ExitProgramAction exitAction = new ExitProgramAction();
        if (Platform.isMacOS()) {
            Desktop.getDesktop().setQuitHandler((e, response) -> exitAction.actionPerformed(null));
        } else {
            fileMenu.addSeparator();
            fileMenu.add(exitAction);
        }
        add(fileMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        viewMenu.add(new ZoomOneToOneAction());
        viewMenu.add(new ZoomFitAction());
        viewMenu.add(new ZoomInAction());
        viewMenu.add(new ZoomOutAction());
        viewMenu.addSeparator();
        viewMenu.add(new ResetCameraAxisAction());
        viewMenu.add(new ResetCameraAction());
        viewMenu.addSeparator();
        viewMenu.add(new ClearAnnotationsAction());

        JCheckBoxMenuItem white = new JCheckBoxMenuItem("Use White Background");
        white.addItemListener(e -> {
            JHVFrame.getGLListener().setWhiteBack(white.getState());
            MovieDisplay.display();
        });
        viewMenu.add(white);

        add(viewMenu);

        JMenu movieMenu = new JMenu("Movie");
        movieMenu.setMnemonic(KeyEvent.VK_M);
        movieMenu.add(MoviePanel.getPlayPauseAction());
        movieMenu.add(MoviePanel.getPreviousFrameAction());
        movieMenu.add(MoviePanel.getNextFrameAction());
        add(movieMenu);

        ShowDialogAction settingsAction = new ShowDialogAction("Settings...", new SettingsDialog());
        if (Platform.isMacOS()) {
            Desktop.getDesktop().setPreferencesHandler(e -> settingsAction.actionPerformed(null));
            JMenu windowMenu = new JMenu("Window");
            windowMenu.setMnemonic(KeyEvent.VK_W);
            windowMenu.add(new WindowMinimizeAction());
            windowMenu.add(new WindowZoomAction());
            add(windowMenu);
        } else {
            JMenu toolsMenu = new JMenu("Tools");
            toolsMenu.setMnemonic(KeyEvent.VK_T);
            toolsMenu.add(settingsAction);
            add(toolsMenu);
        }

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        ShowDialogAction aboutAction = new ShowDialogAction("About JHelioviewer...", new AboutDialog());
        if (Platform.isMacOS()) {
            Desktop.getDesktop().setAboutHandler(e -> aboutAction.actionPerformed(null));
        } else {
            helpMenu.add(aboutAction);
        }

        helpMenu.add(new OpenURLinBrowserAction("Open User Manual", JHVGlobals.documentationURL));
        helpMenu.add(new OpenURLinBrowserAction("Open Website", "https://www.jhelioviewer.org"));
        helpMenu.add(new CheckUpdateAction());
        helpMenu.addSeparator();
        helpMenu.add(new ShowDialogAction("Show Log...", new LogDialog()));
        helpMenu.add(new OpenURLinBrowserAction("Report Bug/Request Feature", JHVGlobals.bugURL));

        add(helpMenu);
    }

}
