package org.helioviewer.jhv.base.plugin.controller;

import java.net.URI;

import org.helioviewer.jhv.base.plugin.interfaces.Container;
import org.helioviewer.jhv.base.plugin.interfaces.Plugin;

/**
 * The basic class which manages the interface between JHV and the contained
 * plugin. It manages the current status of the corresponding plug-in.
 * 
 * @author Stephan Pagel
 */
public class PluginContainer implements Container {

    private final Plugin plugin;
    private final URI pluginLocation;
    private boolean pluginActive;

    /**
     * Default constructor.
     * 
     * @param plugin
     *            Plug-in to control.
     * @param pluginLocation
     *            Location of the associated file of the plug-in.
     * @param active
     *            Status if plug-in is already activated (installed) or not.
     */
    public PluginContainer(Plugin plugin, URI pluginLocation, boolean active) {
        this.plugin = plugin;
        this.pluginLocation = pluginLocation;
        this.pluginActive = active;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.plugin.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return this.plugin.getDescription();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isActive() {
        return this.pluginActive;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the new active status is the same as the current status the method
     * will do nothing. Otherwise it installs the plug-in if the new status is
     * true or uninstalls the plug-in if the new status is false.
     */
    public void setActive(boolean active) {
        if (this.pluginActive == active)
            return;

        this.pluginActive = active;
        if (active)
            plugin.installPlugin();
        else
            plugin.uninstallPlugin();
    }

    /**
     * {@inheritDoc}
     */
    public void changeSettings() {
        PluginSettings.getSingletonInstance().pluginSettingsToXML(this);
    }

    /**
     * Returns the plug-in which is controlled by this container.
     * 
     * @return the plug-in which is controlled by this container.
     */
    public Plugin getPlugin() {
        return this.plugin;
    }

    /**
     * Returns the location of the file where the controlled plug-in is from.
     * 
     * @return the location of the file where the controlled plug-in is from.
     */
    public URI getPluginLocation() {
        return this.pluginLocation;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return this.plugin.getName();
    }

}
