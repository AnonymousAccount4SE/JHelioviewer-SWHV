package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

/**
 * Action that resets the view transformation of the current camera
 * to its default settings
 */
@SuppressWarnings("serial")
public class ResetCameraAction extends AbstractAction {

    public ResetCameraAction(boolean small, boolean useIcon) {
        super("Reset Camera", useIcon ? IconBank.getIcon(JHVIcon.RESET) : null);
        putValue(SHORT_DESCRIPTION, "Reset camera position to default");
        //putValue(MNEMONIC_KEY, KeyEvent.VK_R);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Displayer.getCamera().reset();
    }

}
