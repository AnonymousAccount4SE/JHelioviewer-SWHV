package org.helioviewer.jhv.timelines.band;

import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.io.JSONUtils;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.threads.JHVWorker;
import org.helioviewer.jhv.time.TimeUtils;
import org.helioviewer.jhv.timelines.Timelines;
import org.helioviewer.jhv.timelines.TimelineSettings;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.ArrayListMultimap;

public class BandDataProvider {

    private static final ArrayListMultimap<Band, BandDownloadTask> workerMap = ArrayListMultimap.create();

    public static void loadBandTypes() {
        JHVGlobals.getExecutorService().execute(new BandTypeTask());
    }

    public static void loadBand(JSONObject jo) {
        JHVGlobals.getExecutorService().execute(new BandLoadTask(jo));
    }

    static void addDownloads(Band band, List<Interval> intervals) {
        for (Interval interval : intervals) {
            BandDownloadTask worker = new BandDownloadTask(band, interval.start, interval.end);
            JHVGlobals.getExecutorService().submit(worker);
            workerMap.put(band, worker);
        }
    }

    static void stopDownloads(Band band) {
        workerMap.get(band).forEach(worker -> worker.cancel(true));
        workerMap.removeAll(band);
    }

    static boolean isDownloadActive(Band band) {
        for (BandDownloadTask worker : workerMap.get(band)) {
            if (!worker.isDone())
                return true;
        }
        return false;
    }

    private static class BandDownloadTask extends JHVWorker<BandResponse, Void> {

        private final Band band;
        private final long startTime;
        private final long endTime;

        BandDownloadTask(Band _band, long _startTime, long _endTime) {
            band = _band;
            startTime = _startTime;
            endTime = _endTime;
            Timelines.getLayers().downloadStarted(band);
            setThreadName("EVE--BandDownload");
        }

        @Nullable
        @Override
        protected BandResponse backgroundWork() {
            try {
                BandType type = band.getBandType();
                String request = type.getBaseURL() + "start_date=" + TimeUtils.formatDate(startTime) + "&end_date=" + TimeUtils.formatDate(endTime) +
                        "&timeline=" + type.getName();
                return new BandResponse(JSONUtils.get(request));
            } catch (Exception e) {
                Log.error("Error downloading band", e);
            }
            return null;
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                try {
                    BandResponse r = get();
                    if (r != null) {
                        if (!r.bandName.equals(band.getBandType().getName()))
                            throw new Exception("Expected " + band.getBandType().getName() + ", got " + r.bandName);
                        band.addToCache(r.values, r.dates);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Timelines.getLayers().downloadFinished(band);
        }

    }

    private static class BandLoadTask extends JHVWorker<BandResponse, Void> {

        private final JSONObject jo;

        BandLoadTask(JSONObject _jo) {
            jo = _jo;
        }

        @Nullable
        @Override
        protected BandResponse backgroundWork() {
            try {
                return new BandResponse(jo);
            } catch (Exception e) {
                Log.error("Error loading band", e);
            }
            return null;
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                try {
                    BandResponse r = get();
                    if (r != null) {
                        Band band = new Band(r.bandType == null ? BandType.getBandType(r.bandName) : r.bandType);
                        band.addToCache(r.values, r.dates);
                        Timelines.getLayers().add(band);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static class BandTypeTask extends JHVWorker<JSONArray, Void> {

        BandTypeTask() {
            setThreadName("EVE--LoadSources");
        }

        @Nullable
        @Override
        protected JSONArray backgroundWork() {
            try {
                return JSONUtils.get(TimelineSettings.baseURL).getJSONArray("objects");
            } catch (Exception e) {
                Log.error("Error downloading band types", e);
            }
            return null;
        }

        @Override
        protected void done() {
            if (!isCancelled()) {
                try {
                    JSONArray ja = get();
                    if (ja != null) {
                        BandType.loadBandTypes(ja);
                        Timelines.td.getObservationPanel().setupDatasets();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
