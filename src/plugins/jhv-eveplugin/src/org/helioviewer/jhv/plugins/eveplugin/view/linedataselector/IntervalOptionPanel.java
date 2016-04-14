package org.helioviewer.jhv.plugins.eveplugin.view.linedataselector;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.plugins.eveplugin.draw.DrawController;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimeIntervalLockModel;
import org.helioviewer.jhv.plugins.eveplugin.draw.TimingListener;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class IntervalOptionPanel extends JPanel implements ActionListener, LayersListener, TimingListener, LineDataSelectorModelListener {

    private final JComboBox zoomComboBox;
    private final JToggleButton periodFromLayersButton;
    private boolean selectedIndexSetByProgram;
    private Interval selectedIntervalByZoombox = null;
    private final DrawController drawController;

    private enum ZOOM {
        CUSTOM, All, Year, Month, Day, Hour, Carrington, Movie
    };

    public IntervalOptionPanel() {
        drawController = DrawController.getSingletonInstance();
        drawController.addTimingListener(this);
        LineDataSelectorModel.getSingletonInstance().addLineDataSelectorModelListener(this);

        zoomComboBox = new JComboBox(new DefaultComboBoxModel());
        fillZoomComboBox();
        zoomComboBox.addActionListener(this);
        zoomComboBox.setEnabled(false);

        periodFromLayersButton = new JToggleButton(IconBank.getIcon(JHVIcon.MOVIE_UNLINK));
        periodFromLayersButton.setToolTipText("Synchronize movie and time series display");
        periodFromLayersButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        periodFromLayersButton.setEnabled(Layers.getActiveView() != null);
        periodFromLayersButton.addActionListener(this);

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        add(zoomComboBox);
        add(periodFromLayersButton);

        Layers.addLayersListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == periodFromLayersButton) {
            TimeIntervalLockModel.getInstance().setLocked(periodFromLayersButton.isSelected());
            if (periodFromLayersButton.isSelected()) {
                periodFromLayersButton.setIcon(IconBank.getIcon(JHVIcon.MOVIE_LINK));
                periodFromLayersButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            } else {
                periodFromLayersButton.setIcon(IconBank.getIcon(JHVIcon.MOVIE_UNLINK));
                periodFromLayersButton.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
            }
        } else if (e.getSource().equals(zoomComboBox)) {
            final ZoomComboboxItem item = (ZoomComboboxItem) zoomComboBox.getSelectedItem();
            selectedIntervalByZoombox = null;

            if (item != null && !selectedIndexSetByProgram) {
                selectedIntervalByZoombox = zoomTo(item.getZoom(), item.getNumber());
            } else {
                if (selectedIndexSetByProgram) {
                    selectedIndexSetByProgram = false;
                }
            }
        }
    }

    @Override
    public void downloadStartded(LineDataSelectorElement element) {
    }

    @Override
    public void downloadFinished(LineDataSelectorElement element) {
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        zoomComboBox.setEnabled(true);
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        if (LineDataSelectorModel.getSingletonInstance().getNumberOfAvailableLineData() == 0) {
            zoomComboBox.setEnabled(false);
        }
    }

    @Override
    public void lineDataUpdated(LineDataSelectorElement element) {
    }

    private void addCarringtonRotationToModel(final DefaultComboBoxModel model, final int numberOfRotations) {
        model.addElement(new ZoomComboboxItem(ZOOM.Carrington, numberOfRotations));
    }

    private void addMovieToModel(DefaultComboBoxModel model) {
        // TODO Auto-generated method stub
        model.addElement(new ZoomComboboxItem(ZOOM.Movie, 0));
    }

    private boolean addElementToModel(final DefaultComboBoxModel model, final int calendarValue, final ZOOM zoom) {
        model.addElement(new ZoomComboboxItem(zoom, calendarValue));
        return true;
    }

    private void fillZoomComboBox() {
        final DefaultComboBoxModel model = (DefaultComboBoxModel) zoomComboBox.getModel();
        model.removeAllElements();
        model.addElement(new ZoomComboboxItem(ZOOM.CUSTOM, 0));
        model.addElement(new ZoomComboboxItem(ZOOM.All, 0));
        addMovieToModel(model);
        addElementToModel(model, 1, ZOOM.Year);
        addElementToModel(model, 6, ZOOM.Month);
        addElementToModel(model, 3, ZOOM.Month);
        addCarringtonRotationToModel(model, 1);

        addElementToModel(model, 7, ZOOM.Day);
        addElementToModel(model, 3, ZOOM.Day);
        addElementToModel(model, 12, ZOOM.Hour);
        addElementToModel(model, 6, ZOOM.Hour);
        addElementToModel(model, 1, ZOOM.Hour);
    }

    private static class ZoomComboboxItem {

        private final ZOOM zoom;
        private final int number;

        public ZoomComboboxItem(final ZOOM zoom, final int number) {
            this.zoom = zoom;
            this.number = number;
        }

        public ZOOM getZoom() {
            return zoom;
        }

        public int getNumber() {
            return number;
        }

        @Override
        public String toString() {
            final String plural = number > 1 ? "s" : "";

            switch (zoom) {
            case All:
                return "Maximum interval";
            case Hour:
                return Integer.toString(number) + " hour" + plural;
            case Day:
                return Integer.toString(number) + " day" + plural;
            case Month:
                return Integer.toString(number) + " month" + plural;
            case Year:
                return Integer.toString(number) + " year" + plural;
            case Carrington:
                return "Carrington rotation" + plural;
            case Movie:
                return "Movie interval";
            default:
                break;
            }

            return "Custom interval";
        }
    }

    @Override
    public void layerAdded(View view) {
    }

    @Override
    public void activeLayerChanged(View view) {
        periodFromLayersButton.setEnabled(view != null);
    }

    @Override
    public void availableIntervalChanged() {
    }

    @Override
    public void selectedIntervalChanged(boolean keepFullValueRange) {
        Interval newInterval = DrawController.getSingletonInstance().getSelectedInterval();
        if (selectedIntervalByZoombox != null && newInterval != null) {
            if (!selectedIntervalByZoombox.equals(newInterval)) {
                try {
                    selectedIndexSetByProgram = true;
                    zoomComboBox.setSelectedIndex(0);
                } catch (final IllegalArgumentException ex) {
                }
            }
        }
    }

    private Interval zoomTo(final ZOOM zoom, final long value) {
        Interval selectedInterval = drawController.getSelectedInterval();
        Interval availableInterval = drawController.getAvailableInterval();

        Interval newInterval;
        switch (zoom) {
        case All:
            newInterval = availableInterval;
            break;
        case Day:
            newInterval = computeZoomInterval(selectedInterval, Calendar.DAY_OF_MONTH, value);
            break;
        case Hour:
            newInterval = computeZoomInterval(selectedInterval, Calendar.HOUR, value);
            break;
        case Month:
            newInterval = computeZoomInterval(selectedInterval, Calendar.MONTH, value);
            break;
        case Year:
            newInterval = computeZoomInterval(selectedInterval, Calendar.YEAR, value);
            break;
        case Carrington:
            newInterval = computeCarringtonInterval(selectedInterval, value);
            break;
        case Movie:
            newInterval = computeMovieInterval();
            break;
        case CUSTOM:
        default:
            newInterval = selectedInterval;
        }
        return drawController.setSelectedInterval(newInterval, true, true);
    }

    private Interval computeMovieInterval() {
        View view = Layers.getActiveView();
        if (view != null && view.isMultiFrame()) {
            return new Interval(view.getFirstTime().milli, view.getLastTime().milli);
        }
        long now = System.currentTimeMillis();
        return new Interval(now, now);
    }

    private Interval computeCarringtonInterval(Interval interval, long value) {
        return computeZoomForMilliSeconds(interval, value * 2356585920l);
    }

    private Interval computeZoomForMilliSeconds(final Interval interval, long differenceMilli) {
        long startDate = interval.start;
        Interval availableInterval = drawController.getAvailableInterval();
        long endDate = interval.end;
        long now = System.currentTimeMillis();
        final Date lastdataDate = DrawController.getSingletonInstance().getLastDateWithData();
        if (lastdataDate != null) {
            long lastdataDateTime = lastdataDate.getTime();
            if (endDate > lastdataDateTime) {
                endDate = lastdataDateTime;
            }
        } else if (endDate > now) {
            endDate = now;
        }

        startDate = endDate - differenceMilli;

        boolean sInAvailable = availableInterval.containsPointInclusive(startDate);
        boolean eInAvailable = availableInterval.containsPointInclusive(endDate);

        if (sInAvailable && eInAvailable) {
            return new Interval(startDate, endDate);
        }

        return new Interval(startDate, endDate);
    }

    private Interval computeZoomInterval(final Interval interval, final int calendarField, final long difference) {
        return computeZoomForMilliSeconds(interval, differenceInMilliseconds(calendarField, difference));
    }

    private Long differenceInMilliseconds(final int calendarField, final long value) {
        switch (calendarField) {
        case Calendar.YEAR:
            return value * 365 * 24 * 60 * 60 * 1000l;
        case Calendar.MONTH:
            return value * 30 * 24 * 60 * 60 * 1000l;
        case Calendar.DAY_OF_MONTH:
        case Calendar.DAY_OF_WEEK:
        case Calendar.DAY_OF_WEEK_IN_MONTH:
        case Calendar.DAY_OF_YEAR:
            return value * 24 * 60 * 60 * 1000l;
        case Calendar.HOUR:
        case Calendar.HOUR_OF_DAY:
            return value * 60 * 60 * 1000l;
        case Calendar.MINUTE:
            return value * 60 * 1000l;
        case Calendar.SECOND:
            return value * 1000l;
        case Calendar.MILLISECOND:
            return value * 1l;
        default:
            return null;
        }
    }

}
