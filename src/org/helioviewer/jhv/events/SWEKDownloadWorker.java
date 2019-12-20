package org.helioviewer.jhv.events;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

import org.helioviewer.jhv.database.EventDatabase;

class SWEKDownloadWorker implements Runnable {

    private final SWEKSupplier supplier;
    private final long start;
    private final long end;
    private final List<SWEKParam> params;

    SWEKDownloadWorker(SWEKSupplier _supplier, long _start, long _end, List<SWEKParam> _params) {
        supplier = _supplier;
        start = _start;
        end = _end;
        params = _params;
    }

    void stopWorker() {
        //TBD
    }

    @Override
    public void run() {
        SWEKSource swekSource = supplier.getSource();
        boolean success = swekSource.getHandler().remote2db(supplier, start, end, params);
        if (success) {
            ArrayList<JHVAssociation> assocList = EventDatabase.associations2Program(start, end, supplier);
            ArrayList<JHVEvent> eventList = EventDatabase.events2Program(start, end, supplier, params);
            EventQueue.invokeLater(() -> {
                assocList.forEach(JHVEventCache::add);
                eventList.forEach(JHVEventCache::add);

                JHVEventCache.fireEventCacheChanged();
                SWEKDownloadManager.workerFinished(this);
            });
            EventDatabase.addDaterange2db(start, end, supplier);
        } else {
            EventQueue.invokeLater(() -> SWEKDownloadManager.workerForcedToStop(this));
        }
    }

    SWEKSupplier getSupplier() {
        return supplier;
    }

    long getStart() {
        return start;
    }

    long getEnd() {
        return end;
    }

}
