package org.helioviewer.jhv.plugins;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

public class PluginManager {

    private static final HashSet<Plugin> plugins = new HashSet<>();

    public static Set<Plugin> getPlugins() {
        return Collections.unmodifiableSet(plugins);
    }

    public static void addPlugin(Plugin plugin) {
        plugins.add(plugin);
        if (plugin.isActive())
            plugin.install();
    }

    public static void loadState(JSONObject jo) {
        for (String name : jo.keySet()) {
            for (Plugin plugin : plugins) {
                if (name.equals(plugin.toString())) {
                    JSONObject po = jo.optJSONObject(name);
                    if (po != null) {
                        plugin.loadState(po);
                    }
                }
            }
        }
    }

    public static void saveState(JSONObject jo) {
        for (Plugin plugin : plugins) {
            JSONObject po = new JSONObject();
            plugin.saveState(po);
            jo.put(plugin.toString(), po);
        }
    }

}
