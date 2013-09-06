package org.helioviewer.gl3d.plugin;

import java.util.List;

/**
 * A plugin Configuration provides a list of available plugins.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public interface GL3DPluginConfiguration {
    public List<GL3DModelPlugin> findPlugins();
}
