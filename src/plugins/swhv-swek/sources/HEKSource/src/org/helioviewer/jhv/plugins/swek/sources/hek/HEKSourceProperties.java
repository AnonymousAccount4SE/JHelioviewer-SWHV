package org.helioviewer.jhv.plugins.swek.sources.hek;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.helioviewer.jhv.base.logging.Log;

/**
 * Gives access to the HEK source properties
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
public class HEKSourceProperties {
    private static HEKSourceProperties singletonInstance;

    /** The HEK source properties */
    private final Properties hekSourceProperties;

    /**
     * Private default constructor.
     */
    private HEKSourceProperties() {
        hekSourceProperties = new Properties();
        loadProperties();
    }

    /**
     * Gets the singleton instance of the HEK source properties
     * 
     * @return the HEK source properties
     */
    public static HEKSourceProperties getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new HEKSourceProperties();
        }
        return singletonInstance;
    }

    /**
     * Gets the HEK source properties.
     * 
     * @return the hek source properties
     */
    public Properties getHEKSourceProperties() {
        return hekSourceProperties;
    }

    /**
     * Loads the overall hek source settings.
     */
    private void loadProperties() {
        InputStream defaultPropStream = HEKSourceProperties.class.getResourceAsStream("/heksource.properties");
        try {
            hekSourceProperties.load(defaultPropStream);
            defaultPropStream.close();
        } catch (IOException ex) {
            Log.error("Could not load the hek settings " + ex);
        }
    }

}
