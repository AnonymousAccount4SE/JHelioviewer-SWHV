package org.helioviewer.jhv.plugins;

// The basic class which manages the interface between JHV and the contained
// plugin. It manages the current status of the corresponding plug-in.
class PluginContainer {

    private final Plugin plugin;
    private final String jar;
    private boolean pluginActive;

    public PluginContainer(Plugin _plugin, String _jar, boolean _pluginActive) {
        plugin = _plugin;
        jar = _jar;
        pluginActive = _pluginActive;
    }

    public String getName() {
        return plugin.getName();
    }

    public String getDescription() {
        return plugin.getDescription();
    }

    public String getAboutLicenseText() {
        return plugin.getAboutLicenseText();
    }

    public boolean isActive() {
        return pluginActive;
    }

    public void setActive(boolean active) {
        if (pluginActive == active)
            return;

        pluginActive = active;
        PluginSettings.getSingletonInstance().save(this);
        if (active)
            plugin.installPlugin();
        else
            plugin.uninstallPlugin();
    }

    @Override
    public String toString() {
        return jar;
    }

}
