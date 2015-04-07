package org.helioviewer.jhv.gui.controller;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.math.Vector2dInt;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.components.BasicImagePanel;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.metadata.ImageSizeMetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.viewmodel.viewport.Viewport;
import org.helioviewer.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * Collection of several zooming functions.
 *
 * <p>
 * This class provides several zooming functions. The controller is used by
 * several classes, such as {@link org.helioviewer.jhv.gui.actions.ZoomInAction}, {@link org.helioviewer.jhv.gui.actions.ZoomOutAction},
 * {@link org.helioviewer.jhv.gui.actions.ZoomFitAction},
 * {@link org.helioviewer.jhv.gui.actions.Zoom1to1Action} and
 * {@link org.helioviewer.jhv.gui.controller.MainImagePanelMouseController}.
 */
public class ZoomController {

    private BasicImagePanel panel = null;

    public static final double zoomFactorStep = Math.pow(2, 1.0 / (4.0));

    /**
     * Sets the panel on which the zoom controller should operate. Can be used
     * to zoom to the current mouse position within the specified panel.
     *
     * @param panel
     *            An ImagePanel
     */
    public void setImagePanel(BasicImagePanel panel) {
        this.panel = panel;
    }

    /**
     * Zooms in one step. A step mean scaling the current region of interest by
     * the square root of two
     */
    public void zoomIn(JHVJP2View topmostView) {
        zoom(topmostView, zoomFactorStep);
    }

    /**
     * Zooms out one step. A step mean scaling the current region of interest by
     * the square root of two
     */
    public void zoomOut(JHVJP2View topmostView) {
        zoom(topmostView, 1.0 / zoomFactorStep);
    }

    /**
     * Zooms in or out the desired number of steps. A step mean scaling the
     * current region of interest by the square root of two. To zoom in, steps
     * has to be greater than zero, to zoom out it has to be lesser than zero.
     *
     * @param steps
     *            Number of steps to zoom, the sign defines the direction.
     */
    public void zoomSteps(JHVJP2View topmostView, int steps) {
        zoom(topmostView, Math.pow(zoomFactorStep, steps));
    }

    /**
     * Zooms by scaling the current region by the given zoom factor. Uses a
     * heuristic to avoid zooming out TOO much!
     *
     * @param zoomFactor
     *            zoom factor to scale the current region with
     */
    public void zoom(JHVJP2View topmostView, double zoomFactor) {

        if (topmostView.getMetaData() != null) {

            // if zooming out, make sure that we do not get off too far
            if (zoomFactor < 1) {

                LayersModel layersModel = Displayer.getLayersModel();

                // loop over all layers to check if all layers would be getting
                // too small
                boolean tooSmall = true;
                for (int i = 0; i < layersModel.getNumLayers(); ++i) {
                    JHVJP2View view = layersModel.getLayer(i);
                    if (getZoom(view) * zoomFactor > 0.005) {
                        tooSmall = false;
                        break;
                    }
                }
                if (tooSmall) {
                    return;
                }
            }
            Region oldRegion = topmostView.getRegion();
            if (oldRegion == null) {
                return;
            }
            Vector2dDouble newSizeVector = Vector2dDouble.scale(oldRegion.getSize(), 1.0 / zoomFactor);

            Vector2dDouble newCorner = null;
            if (panel != null) {
                Vector2dInt mousePosition = panel.getInputController().getMousePosition();
                if (mousePosition != null) {
                    ViewportImageSize vis = ViewHelper.calculateViewportImageSize(oldRegion);
                    Vector2dInt vp = new Vector2dInt(GL3DState.get().getViewportWidth(), GL3DState.get().getViewportHeight());
                    Vector2dInt visOffset = vp.subtract(vis.getSizeVector()).scale(0.5);
                    Vector2dInt fixPointViewport = mousePosition.subtract(visOffset);
                    if (fixPointViewport.getX() >= 0 && fixPointViewport.getY() >= 0) {
                        Vector2dDouble fixPointOffset = ViewHelper.convertScreenToImageDisplacement(fixPointViewport.subtract(new Vector2dInt(0, vis.getHeight())), oldRegion, vis);
                        Vector2dDouble fixPoint = fixPointOffset.add(oldRegion.getLowerLeftCorner());
                        Vector2dDouble relativeFixPointOffset = fixPointOffset.invertedScale(oldRegion.getSize());
                        Vector2dDouble newFixPointOffset = newSizeVector.scale(relativeFixPointOffset);
                        newCorner = fixPoint.subtract(newFixPointOffset);
                    }
                }
            }

            if (newCorner == null) {
                newCorner = Vector2dDouble.add(oldRegion.getLowerLeftCorner(), Vector2dDouble.scale(Vector2dDouble.subtract(oldRegion.getSize(), newSizeVector), 0.5));
            }
            Region zoomedRegion = ViewHelper.cropRegionToImage(StaticRegion.createAdaptedRegion(newCorner, newSizeVector), topmostView.getMetaData());

            topmostView.setRegion(zoomedRegion, new ChangeEvent());
        }
    }

    public static double getZoom(JHVJP2View view, Region outerRegion) {
        if (view != null) {
            Region region = view.getRegion();
            if (outerRegion == null) {
                outerRegion = region;
            }

            region = view.getNewestRegion();

            if (region == null) {
                return 1.0;
            }

            double unitsPerPixel = ((ImageSizeMetaData) view.getMetaData()).getUnitsPerPixel();
            ViewportImageSize vis = ViewHelper.calculateViewportImageSize(outerRegion);
            Viewport layerViewport = ViewHelper.calculateInnerViewport(region, outerRegion, vis);
            Vector2dInt actualSize = layerViewport.getSize();
            double zoom = actualSize.getX() * unitsPerPixel / region.getSize().getX();
            return zoom;
        }
        return 1.0;
    }

    public static double getZoom(JHVJP2View view) {
        if (view != null) {
            return getZoom(view, null);
        }
        return 1.0;
    }

    public void zoom1to1(JHVJP2View topmostView, JHVJP2View activeView) {
        if (activeView != null && topmostView != null) {
            zoom(topmostView, 1.0 / getZoom(activeView, topmostView.getRegion()));
        }
    }

    /**
     * Zooms the image in such a way, that the whole region given by the
     * metaData fits exactly into the viewport.
     *
     * @param metaDataView
     *            MetaDataView of the layer which should be fit into the
     *            viewport
     * @param regionView
     *            RegionView which is used to set the new region
     */
    public void zoomFit(MetaDataView metaDataView, RegionView regionView) {
        if (metaDataView != null && regionView != null) {
            Region region = metaDataView.getMetaData().getPhysicalRegion();
            Vector2dDouble size = region.getSize();
            Vector2dDouble lowerLeft = region.getLowerLeftCorner();
            region = StaticRegion.createAdaptedRegion(lowerLeft, size);
            regionView.setRegion(region, new ChangeEvent());
        }
    }

    /**
     * Convenience method. Fits the active layer of the main view chain into the
     * main viewport.
     */
    public void zoomFit() {
        zoomFit(Displayer.getLayersModel().getActiveView(), Displayer.getLayersModel().getActiveView());
    }

}
