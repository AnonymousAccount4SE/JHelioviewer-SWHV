package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.camera.Camera;
import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * Action to zoom such that the active layer fits completely in the viewport
 */
@SuppressWarnings("serial")
public class ZoomOneToOneAction extends AbstractAction {

    /**
     * @param small
     *            - if true, chooses a small (16x16), otherwise a large (24x24)
     *            icon for the action
     */
    public ZoomOneToOneAction(boolean small, boolean useIcon) {
        super("Zoom 1:1", useIcon ? (small ? IconBank.getIcon(JHVIcon.ZOOM_1TO1_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_1TO1)) : null);
        putValue(SHORT_DESCRIPTION, "Zoom to native resolution");
        putValue(MNEMONIC_KEY, KeyEvent.VK_Z);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        View view = Layers.getActiveView();
        if (view != null) {
            MetaData metaData = view.getImageLayer().getImageData().getMetaData();
            double imageFraction = Displayer.getViewport().height / (double) metaData.getPixelHeight();

            Camera camera = Displayer.getCamera();
            double fov = 2. * Math.atan2(0.5 * metaData.getPhysicalRegion().height * imageFraction, camera.getViewpoint().distance);
            camera.setCameraFOV(fov);

            Displayer.render(1);
        }
    }
}
