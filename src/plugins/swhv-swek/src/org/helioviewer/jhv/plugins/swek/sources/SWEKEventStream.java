package org.helioviewer.jhv.plugins.swek.sources;

import org.helioviewer.jhv.data.datatype.event.JHVAssociation;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;

public interface SWEKEventStream {
    /**
     * Has the stream events left.
     *
     * @return True if the stream still has events left, false if not
     */
    public abstract boolean hasEvents();

    /**
     * Gets the next available event if any.
     *
     * @return The next event or null if the stream has no events.hasEvents
     */
    public abstract JHVEvent next();

    /**
     * Indicates if the download was sufficient or not
     *
     * @return true if an extra download is needed, false if not.
     */
    public abstract boolean additionalDownloadNeeded();

    public abstract JHVAssociation nextAssociation();

    boolean hasAssociations();
}
