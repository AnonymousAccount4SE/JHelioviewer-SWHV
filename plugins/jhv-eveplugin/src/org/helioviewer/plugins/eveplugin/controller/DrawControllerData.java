package org.helioviewer.plugins.eveplugin.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.helioviewer.plugins.eveplugin.draw.DrawableElement;
import org.helioviewer.plugins.eveplugin.draw.DrawableType;
import org.helioviewer.plugins.eveplugin.draw.YAxisElement;

public class DrawControllerData {

    private int nrOfDrawableElements;
    private Set<YAxisElement> yAxisSet;
    private Map<DrawableType, List<DrawableElement>> drawableElements;
    private List<DrawControllerListener> listeners;

    public DrawControllerData() {
        this.nrOfDrawableElements = 0;
        this.drawableElements = new HashMap<DrawableType, List<DrawableElement>>();
        this.listeners = new ArrayList<DrawControllerListener>();
        this.yAxisSet = new HashSet<YAxisElement>();
    }

    public int getNrOfDrawableElements() {
        return nrOfDrawableElements;
    }

    public void setNrOfDrawableElements(int nrOfDrawableElements) {
        this.nrOfDrawableElements = nrOfDrawableElements;
    }

    public Set<YAxisElement> getyAxisSet() {
        synchronized (yAxisSet) {
            return yAxisSet;
        }
    }

    public void setyAxisSet(Set<YAxisElement> yAxisSet) {
        this.yAxisSet = yAxisSet;
    }

    public Map<DrawableType, List<DrawableElement>> getDrawableElements() {
        return drawableElements;
    }

    public void setDrawableElements(Map<DrawableType, List<DrawableElement>> drawableElements) {
        this.drawableElements = drawableElements;
    }

    public List<DrawControllerListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<DrawControllerListener> listeners) {
        this.listeners = listeners;
    }

    public void addDrawControllerListener(DrawControllerListener listener) {
        this.listeners.add(listener);
    }

    public void removeDrawControllerListener(DrawControllerListener listener) {
        this.listeners.remove(listener);

    }

    public void addDrawableElement(DrawableElement element) {
        List<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements == null) {
            elements = Collections.synchronizedList(new ArrayList<DrawableElement>());
            drawableElements.put(element.getDrawableElementType().getLevel(), elements);
        }
        synchronized (elements) {
            elements.add(element);
            synchronized (yAxisSet) {
                yAxisSet.add(element.getYAxisElement());
            }
            nrOfDrawableElements++;
        }
    }

    public void removeDrawableElement(DrawableElement element) {
        List<DrawableElement> elements = drawableElements.get(element.getDrawableElementType().getLevel());
        if (elements != null) {
            synchronized (elements) {
                elements.remove(element);
                nrOfDrawableElements--;
                if (elements.isEmpty()) {
                    synchronized (yAxisSet) {
                        yAxisSet.remove(element.getYAxisElement());
                    }
                }
            }
        }
    }
}
