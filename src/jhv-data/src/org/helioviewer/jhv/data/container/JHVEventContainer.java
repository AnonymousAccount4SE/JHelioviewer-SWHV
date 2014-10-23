package org.helioviewer.jhv.data.container;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.logging.Logger;

import org.helioviewer.jhv.data.container.cache.JHVEventCache;
import org.helioviewer.jhv.data.container.cache.JHVEventHandlerCache;
import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventType;
import org.helioviewer.jhv.data.lock.JHVEventContainerLocks;

public class JHVEventContainer {

    /** Singleton instance */
    private static JHVEventContainer singletonInstance;

    /** The handlers of requests */
    private final List<JHVEventContainerRequestHandler> requestHandlers;

    /** the event cache */
    private final JHVEventCache eventCache;

    /** the event handler cache */
    private final JHVEventHandlerCache eventHandlerCache;

    private Long requestID;

    /**
     * Private constructor.
     */
    private JHVEventContainer() {
        requestHandlers = new ArrayList<JHVEventContainerRequestHandler>();
        eventHandlerCache = JHVEventHandlerCache.getSingletonInstance();
        eventCache = JHVEventCache.getSingletonInstance();
        requestID = 0L;
    }

    /**
     * Gets the singleton instance of the JHVEventContainer
     * 
     * @return the singleton instance
     */
    public static JHVEventContainer getSingletonInstance() {
        if (singletonInstance == null) {
            singletonInstance = new JHVEventContainer();
        }
        return singletonInstance;
    }

    /**
     * Register a JHV event container request handler.
     * 
     * 
     * @param handler
     *            the handler to register
     */
    public void registerHandler(JHVEventContainerRequestHandler handler) {
        synchronized (JHVEventContainerLocks.requestHandlerLock) {
            requestHandlers.add(handler);
        }
    }

    /**
     * Removes the JHV event container request handler.
     * 
     * @param handler
     *            the handler to remove
     */
    public void removeHandler(JHVEventContainerRequestHandler handler) {
        synchronized (JHVEventContainerLocks.requestHandlerLock) {
            requestHandlers.remove(handler);
        }
    }

    /**
     * Request the JHVEventContainer for events from a specific date. The events
     * will be send to the given handler. Events already available will directly
     * be send to the handler. Events becoming available will also be send to
     * the handler in the future.
     * 
     * @param date
     *            the date to send events for
     * @param handler
     *            the handler to send events to
     */
    public void requestForDate(final Date date, final JHVEventHandler handler) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Long requestID = nextDownloadID();
                Long previousRequestID = eventHandlerCache.add(handler, date, requestID);
                Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events = eventCache.get(date);
                handler.newEventsReceived(events);
                if (previousRequestID != null) {
                    removeIntervalForRequestID(previousRequestID);
                }
                requestEvents(date, requestID);
            }
        });
    }

    /**
     * Request the JHVEventContainer for events from a specific list of dates.
     * The events will be send to the given handler. Events already available
     * will directly be send to the handler. Events becoming available will also
     * be send to the handler in the future.
     * 
     * @param dateList
     *            the list of dates to send events for
     * @param handler
     *            the handler to send events to
     */
    public void requestForDateList(final List<Date> dateList, final JHVEventHandler handler) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                // Logger.getLogger(JHVEventContainer.class.getName()).info("Request for date list :");
                for (Date date : dateList) {
                    requestForDate(date, handler);
                }
            }
        });
    }

    /**
     * Request the JHVEventContainer for events from a specific time interval.
     * The events will be send to the given handler. Events already available
     * will directly be send to the handler. Events becoming available will also
     * be send to the handler in the future.
     * 
     * @param startDate
     *            the start date of the interval
     * @param endDate
     *            the end date of the interval
     * @param handler
     *            the handler
     */
    public void requestForInterval(final Date startDate, final Date endDate, final JHVEventHandler handler) {
        // Logger.getLogger(JHVEventContainer.class.getName()).info("Request for interval : ["
        // + startDate + "," + endDate + "]");
        // Logger.getLogger(JHVEventContainer.class.getName()).info("handler : "
        // + handler);
        // Thread.dumpStack();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (startDate != null && endDate != null) {
                    Long requestID = nextDownloadID();
                    Long previousRequestID = eventHandlerCache.add(handler, startDate, endDate, requestID);
                    Map<String, NavigableMap<Date, NavigableMap<Date, List<JHVEvent>>>> events = eventCache.get(startDate, endDate);
                    // AssociationsPrinter.print(events);
                    handler.newEventsReceived(events);
                    if (previousRequestID != null) {
                        removeIntervalForRequestID(previousRequestID);
                    }
                    requestEvents(startDate, endDate, requestID);
                }
            }
        });
    }

    /**
     * Add an event to the event cache.
     * 
     * @param event
     *            the event to add to the event cache
     */
    public void addEvent(final JHVEvent event) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                eventCache.add(event);
            }

        });

    }

    /**
     * Indicates to the JHVEventContainer that a download was finished. This
     * must be called the event request handlers in order to propagate the
     * downloaded events to the event handlers.
     */
    public void finishedDownload() {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                fireEventCacheChanged();
            }
        });
    }

    /**
     * Removes the events of the given eventType from the event cache.
     * 
     * @param eventType
     *            the event type to remove from the cache.
     */
    public void removeEvents(final JHVEventType eventType) {
        Logger.getLogger(JHVEventContainer.class.getName()).severe("remove events called");
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                Logger.getLogger(JHVEventContainer.class.getName()).severe("remove events executed");
                eventCache.removeEventType(eventType);
                fireEventCacheChanged();
            }
        });
    }

    /**
     * Request data from the request handlers for a date.
     * 
     * @param date
     *            the date for which to request the data
     */
    private void requestEvents(Date date, Long requestID2) {
        synchronized (JHVEventContainerLocks.requestHandlerLock) {
            for (JHVEventContainerRequestHandler handler : requestHandlers) {
                handler.handleRequestForDate(date, requestID2);
            }
        }
    }

    /**
     * Request data from the request handlers over an interval.
     * 
     * @param startDate
     *            the start of the interval
     * @param endDate
     *            the end of the interval
     * @param requestID2
     */
    private void requestEvents(Date startDate, Date endDate, Long requestID2) {
        synchronized (JHVEventContainerLocks.requestHandlerLock) {
            for (JHVEventContainerRequestHandler handler : requestHandlers) {
                handler.handleRequestForInterval(startDate, endDate, requestID);
            }
        }
    }

    /**
     * Notify the interested JHVEventhandler of about the cache that was
     * changed.
     * 
     * @param date
     *            the date for which the cache was changed.
     */
    private void fireEventCacheChanged() {
        /**
         * List<JHVEventHandler> jhvEventHandlers =
         * eventHandlerCache.getJHVEventHandlersForDate(date); for
         * (JHVEventHandler handler : jhvEventHandlers) {
         * handler.cacheUpdated(); }
         */
        // Logger.getLogger(JHVEventContainer.class.getName()).severe("event cache changed");
        // Thread.dumpStack();
        Set<JHVEventHandler> handlers = eventHandlerCache.getAllJHVEventHandlers();
        synchronized (JHVEventContainerLocks.eventHandlerCacheLock) {
            for (JHVEventHandler handler : handlers) {
                handler.cacheUpdated();
            }
        }
    }

    private void removeIntervalForRequestID(Long previousRequestID) {
        Set<JHVEventHandler> handlers = eventHandlerCache.getAllJHVEventHandlers();
        synchronized (JHVEventContainerLocks.eventHandlerCacheLock) {
            for (JHVEventContainerRequestHandler handler : requestHandlers) {
                handler.removeRequestID(requestID);
            }
        }
    }

    private Long nextDownloadID() {
        synchronized (requestID) {
            return requestID++;
        }
    }

}
