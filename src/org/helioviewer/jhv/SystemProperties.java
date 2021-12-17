package org.helioviewer.jhv;

import javax.swing.JOptionPane;

public class SystemProperties {

    // Reads the builtin Java properties to determine the platform and set simplified properties used by JHV
    static void setPlatform() {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        if (os == null || arch == null) {
            Log2.error("Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
            return;
        }

        os = os.toLowerCase();
        arch = arch.toLowerCase();

        if (arch.contains("x86_64") || arch.contains("amd64"))
            System.setProperty("jhv.arch", "x86-64");
        else {
            Log2.error("Platform > Please install Java 64-bit to run JHelioviewer.");
            JOptionPane optionPane = new JOptionPane();
            optionPane.setMessage("Please install Java 64-bit to run JHelioviewer.");
            optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
            optionPane.setOptions(new String[]{"Quit JHelioviewer"});
            optionPane.createDialog(null, "JHelioviewer: Java 64-bit required").setVisible(true);
            System.exit(1);
        }

        if (os.contains("windows"))
            System.setProperty("jhv.os", "windows");
        else if (os.contains("linux"))
            System.setProperty("jhv.os", "linux");
        else if (os.contains("mac os x"))
            System.setProperty("jhv.os", "mac");
        else
            Log2.error("Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
    }

}
