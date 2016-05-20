package org.helioviewer.jhv.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.MainContentPanelPlugin;

/**
 * This panel acts as a container for the GUI elements which are shown in the
 * main area of the application. Usually it contains the main image area. Below
 * the main image area plug-ins are able to display their GUI components.
 * */
@SuppressWarnings("serial")
public class MainContentPanel extends JPanel implements ActionListener {

    private final LinkedList<MainContentPanelPlugin> pluginList = new LinkedList<MainContentPanelPlugin>();

    private final JSplitPane splitpane;
    private final JPanel pluginContainer;
    private final CollapsiblePane collapsiblePane;

    public MainContentPanel(Component mainComponent) {
        pluginContainer = new JPanel(new BorderLayout());
        collapsiblePane = new CollapsiblePane("Plugins", pluginContainer, true);
        collapsiblePane.toggleButton.addActionListener(this);

        // nest in a container to avoid crash of GLCanvas inside JSplitPane
        JPanel container = new JPanel(new BorderLayout());
        container.setMinimumSize(new Dimension());
        container.add(mainComponent, BorderLayout.CENTER);

        splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false);
        splitpane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 1));
        splitpane.setTopComponent(container);
        splitpane.setResizeWeight(0.66);
        splitpane.setOneTouchExpandable(false);
        splitpane.setDividerSize(0);

        setLayout(new BorderLayout());
        setMinimumSize(new Dimension());
        add(splitpane, BorderLayout.CENTER);
    }

    /**
     * Adds a plug-in and the associated GUI to the container. The GUI will be
     * displayed below the main component.
     *
     * @param plugin
     *            Plugin to be added to the container.
     * */
    public void addPlugin(MainContentPanelPlugin plugin) {
        if (plugin == null || pluginList.contains(plugin)) {
            return;
        }
        pluginList.add(plugin);
        updateLayout();
    }

    /**
     * Removes a plug-in and the associated GUI from the container.
     *
     * @param plugin
     *            Plugin to be removed from the container.
     * */
    public void removePlugin(MainContentPanelPlugin plugin) {
        if (pluginList.remove(plugin)) {
            updateLayout();
        }
    }

    /**
     * Updates the layout of the container and its sub components. Plug-ins will
     * be displayed, if available, in separated tabs below the main component
     * area. An split pane will be provided, if necessary, to readjust the
     * height of the components.
     * */
    private void updateLayout() {
        splitpane.remove(collapsiblePane);
        remove(collapsiblePane);
        splitpane.setDividerSize(0);

        if (pluginList.isEmpty()) {
            revalidate();
            repaint();
            return;
        }

        if (collapsiblePane.toggleButton.isSelected()) {
            pluginContainer.removeAll();

            if (pluginList.size() == 1 && pluginList.get(0).getVisualInterfaces().size() == 1) {
                pluginContainer.add(pluginList.get(0).getVisualInterfaces().get(0), BorderLayout.CENTER);
                collapsiblePane.setTitle(pluginList.get(0).getTabName());

                splitpane.setBottomComponent(collapsiblePane);
                splitpane.setDividerSize(ImageViewerGui.SPLIT_DIVIDER_SIZE);
            } else if (!(pluginList.size() == 1 && pluginList.get(0).getVisualInterfaces().size() == 0) && pluginList.size() > 0) {
                JTabbedPane tabbedPane = new JTabbedPane();

                for (MainContentPanelPlugin plugin : pluginList) {
                    for (JComponent component : plugin.getVisualInterfaces()) {
                        tabbedPane.addTab(plugin.getTabName(), component);
                    }
                }

                pluginContainer.add(tabbedPane, BorderLayout.CENTER);
                collapsiblePane.setTitle("Plugins");

                splitpane.setBottomComponent(collapsiblePane);
                splitpane.setDividerSize(ImageViewerGui.SPLIT_DIVIDER_SIZE);
            }
        } else {
            add(collapsiblePane, BorderLayout.PAGE_END);
        }
        revalidate();
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        updateLayout();
    }

}
