package org.helioviewer.jhv.events.gui.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helioviewer.jhv.events.SWEKParam;
import org.helioviewer.jhv.events.SWEKParameter;
import org.helioviewer.jhv.events.SWEKSupplier;

public class FilterManager {

    private static final Map<SWEKSupplier, Map<SWEKParameter, List<SWEKParam>>> filters = new HashMap<>();
    private static final ArrayList<FilterManagerListener> listeners = new ArrayList<>();

    public static void addListener(FilterManagerListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    static void addFilter(SWEKSupplier supplier, SWEKParameter parameter, SWEKParam filter) {
        Map<SWEKParameter, List<SWEKParam>> filteredParameterPerEventType = getFilter(supplier);
        filters.put(supplier, filteredParameterPerEventType);

        filteredParameterPerEventType.putIfAbsent(parameter, new ArrayList<>());
        filteredParameterPerEventType.get(parameter).add(filter);
    }

    static void removeFilters(SWEKSupplier supplier) {
        filters.remove(supplier);
    }

    static void fireFilters(SWEKSupplier supplier) {
        listeners.forEach(listener -> listener.filtersChanged(supplier));
    }

    public static Map<SWEKParameter, List<SWEKParam>> getFilter(SWEKSupplier supplier) {
        Map<SWEKParameter, List<SWEKParam>> filter = filters.get(supplier);
        return filter == null ? new HashMap<>() : filter;
    }
/*
    public static boolean isFiltered(SWEKSupplier supplier, SWEKParameter parameter) {
        return filters.containsKey(supplier) && filters.get(supplier).containsKey(parameter);
    }
*/
}
