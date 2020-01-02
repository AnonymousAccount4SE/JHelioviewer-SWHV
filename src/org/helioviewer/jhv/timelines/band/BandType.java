package org.helioviewer.jhv.timelines.band;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class BandType {

    private static final HashMap<String, List<BandType>> groups = new HashMap<>();

    static void loadBandTypes(JSONArray ja) {
        int len = ja.length();
        for (int i = 0; i < len; i++) {
            new BandType(ja.getJSONObject(i)); // will register BandType in the band groups
        }    
    }

    @Nonnull
    static BandType getBandType(String name) {
        for (List<BandType> list : groups.values()) {
            for (BandType bt : list)
                if (bt.name.equals(name))
                    return bt;
        }
        return new BandType(new JSONObject());
    }

    @Nonnull
    public static List<BandType> getBandTypes(String group) {
        List<BandType> list = groups.get(group);
        return list == null ? new ArrayList<>() : Collections.unmodifiableList(list);
    }

    @Nonnull
    public static String[] getGroups() {
        return groups.keySet().toArray(new String[0]);
    }

    private final String name;
    private final String baseURL;
    private final String label;
    private final String unitLabel;
    private final String[] warnLabels;
    private final double[] warnLevels;
    private final double min;
    private final double max;
    private final String scale;
    private final String bandCacheType;

    private final JSONObject json;

    BandType(JSONObject jo) {
        json = jo;

        name = jo.optString("name", "Unknown");
        String group = jo.optString("group", "Unknown");
        baseURL = jo.optString("baseUrl", "");
        label = jo.optString("label", "Unknown");

        String ul = jo.optString("unitLabel", "unknown");
        if ("".equals(ul)) // crashes ChartDrawGraphPane.drawVerticalLabels
            ul = " ";
        unitLabel = ul;

        JSONArray range = jo.optJSONArray("range");
        if (range != null) {
            min = range.optDouble(0, 0);
            max = range.optDouble(1, 1);
        } else {
            min = 0;
            max = 1;
        }

        scale = jo.optString("scale", "linear");

        JSONArray warn = jo.optJSONArray("warnLevels");
        if (warn != null) {
            int len = warn.length();
            warnLabels = new String[len];
            warnLevels = new double[len];
            for (int i = 0; i < len; i++) {
                JSONObject o = warn.getJSONObject(i);
                warnLabels[i] = o.getString("warnLabel");
                warnLevels[i] = o.getDouble("warnValue");
            }
        } else {
            warnLabels = new String[0];
            warnLevels = new double[0];
        }

        bandCacheType = jo.optString("bandCacheType", "BandCacheMinute");

        groups.computeIfAbsent(group, k -> new ArrayList<>()).add(this);
    }

    void serialize(JSONObject jo) {
        jo.put("bandType", json);
    }

    String getName() {
        return name;
    }

    String getBandCacheType() {
        return bandCacheType;
    }

    String getScale() {
        return scale;
    }

    String getUnitLabel() {
        return unitLabel;
    }

    @Nonnull
    String[] getWarnLabels() {
        return warnLabels;
    }

    @Nonnull
    double[] getWarnLevels() {
        return warnLevels;
    }

    double getMin() {
        return min;
    }

    double getMax() {
        return max;
    }

    String getBaseURL() {
        return baseURL;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BandType))
            return false;
        BandType t = (BandType) o;
        return name.equals(t.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return label;
    }

}
