package org.helioviewer.jhv.io;

import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.dialogs.observation.ImageDataPanel;
import org.helioviewer.jhv.gui.dialogs.observation.ObservationDialog;
import org.helioviewer.jhv.threads.JHVWorker;

public class SetupTimeTask extends JHVWorker<Long, Void> {

    private final String observatory;
    private final String instrument;
    private final String detector;
    private final String measurement;

    // Sets the latest available image (or now if fails) to the end time and the start 24h earlier.
    public SetupTimeTask(String _observatory, String _instrument, String _detector, String _measurement) {
        observatory = _observatory;
        instrument = _instrument;
        detector = _detector;
        measurement = _measurement;
        setThreadName("MAIN--SetupTime");
    }

    @Override
    protected Long backgroundWork() {
        return APIRequestManager.getLatestImageDate(observatory, instrument, detector, measurement, true);
    }

    @Override
    public void done() {
        try {
            ImageDataPanel idp = ObservationDialog.getInstance().getObservationImagePane();

            Date endDate = new Date(get());
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTime(endDate);

            gregorianCalendar.add(GregorianCalendar.SECOND, idp.getCadence());
            idp.setEndDate(gregorianCalendar.getTime(), false);

            gregorianCalendar.add(GregorianCalendar.DAY_OF_MONTH, -1);
            idp.setStartDate(gregorianCalendar.getTime(), false);

            if (Boolean.parseBoolean(Settings.getSingletonInstance().getProperty("startup.loadmovie")))
                idp.loadRemote();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
