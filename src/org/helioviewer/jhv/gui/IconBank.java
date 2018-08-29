package org.helioviewer.jhv.gui;

import java.awt.GraphicsEnvironment;
import java.awt.Transparency;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.ImageIcon;

import com.jidesoft.icons.IconsFactory;

public class IconBank {

    private static final ImageIcon blank = new ImageIcon(GraphicsEnvironment.getLocalGraphicsEnvironment().
            getDefaultScreenDevice().getDefaultConfiguration().
            createCompatibleImage(32, 32, Transparency.OPAQUE));

    // The enum has all the icons, you supply these enums to the getIcon method
    public enum JHVIcon {
        CLOSED_HAND("ClosedHand.gif"), CLOSED_HAND_MAC("drag.png"), HVLOGO_SMALL("hvImage_160x160.png");

        final String fname;

        private JHVIcon(String _fname) {
            fname = _fname;
        }

    }

    @Nullable
    public static ImageIcon getIcon(JHVIcon icon) {
        return IconsFactory.getImageIcon(IconBank.class, "/images/" + icon.fname);
    }

    @Nonnull
    public static ImageIcon getBlank() {
        return blank;
    }

}
