package org.helioviewer.jhv.plugins.eveplugin.view;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.interval.Interval;
import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.gui.dialogs.model.ObservationDialogDateModel;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.eveplugin.EVEPlugin;
import org.helioviewer.jhv.plugins.eveplugin.lines.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandColors;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandGroup;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandType;
import org.helioviewer.jhv.plugins.eveplugin.lines.BandTypeAPI;
import org.helioviewer.jhv.plugins.eveplugin.lines.DownloadController;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorElement;
import org.helioviewer.jhv.plugins.eveplugin.view.linedataselector.LineDataSelectorModelListener;

@SuppressWarnings("serial")
public class ObservationDialogUIPanel extends SimpleObservationDialogUIPanel implements LineDataSelectorModelListener {

    private final JComboBox comboBoxGroup;
    private final JComboBox comboBoxData;

    public ObservationDialogUIPanel() {
        JLabel labelGroup = new JLabel("Group", JLabel.RIGHT);
        JLabel labelData = new JLabel("Dataset", JLabel.RIGHT);

        comboBoxGroup = new JComboBox(new DefaultComboBoxModel());
        comboBoxData = new JComboBox(new DefaultComboBoxModel());
        JPanel dataPane = new JPanel();

        comboBoxGroup.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateGroupValues();
            }
        });

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        dataPane.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;

        dataPane.add(labelGroup, c);

        c.gridx = 1;
        c.gridy = 0;
        dataPane.add(comboBoxGroup, c);

        c.gridx = 0;
        c.gridy = 1;
        dataPane.add(labelData, c);

        c.gridx = 1;
        c.gridy = 1;
        dataPane.add(comboBoxData, c);

        container.add(dataPane, BorderLayout.CENTER);
        this.add(container);

        initGroups();
        EVEPlugin.ldsm.addLineDataSelectorModelListener(this);
    }

    private void initGroups() {
        DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxGroup.getModel();
        model.removeAllElements();

        List<BandGroup> groups = BandTypeAPI.getSingletonInstance().getOrderedGroups();
        for (BandGroup group : groups) {
            model.addElement(group);
        }
    }

    private void updateGroupValues() {
        final DefaultComboBoxModel model = (DefaultComboBoxModel) comboBoxData.getModel();
        final BandGroup selectedGroup = (BandGroup) comboBoxGroup.getSelectedItem();
        final BandType[] values = BandTypeAPI.getSingletonInstance().getBandTypes(selectedGroup);

        model.removeAllElements();

        for (BandType value : values) {
            if (!EVEPlugin.ldsm.containsBandType(value)) {
                model.addElement(value);
            }
        }

        if (model.getSize() > 0) {
            comboBoxData.setSelectedIndex(0);
        }
    }

    private void updateBandController() {
        BandType bandType = (BandType) comboBoxData.getSelectedItem();
        Band band = new Band(bandType);
        band.setDataColor(BandColors.getNextColor());
        DownloadController.getSingletonInstance().updateBand(band, EVEPlugin.dc.availableAxis.start, EVEPlugin.dc.availableAxis.end);
    }

    private void updateDrawController() {
        Interval interval = defineInterval(getDate());
        EVEPlugin.dc.setSelectedInterval(interval.start, interval.end);
    }

    private Interval defineInterval(Date date) {
        JHVDate start = Layers.getStartDate();
        JHVDate end = Layers.getEndDate();
        if (start != null && end != null) {
            Interval movieInterval = new Interval(Layers.getStartDate().milli, Layers.getEndDate().milli);

            if (movieInterval.containsPointInclusive(date.getTime())) {
                return movieInterval;
            }
        }
        GregorianCalendar gce = new GregorianCalendar();
        gce.clear();
        gce.setTime(date);
        gce.set(Calendar.HOUR, 0);
        gce.set(Calendar.MINUTE, 0);
        gce.set(Calendar.SECOND, 0);
        gce.set(Calendar.MILLISECOND, 0);
        gce.add(Calendar.DAY_OF_MONTH, 1);
        Date endDate = gce.getTime();

        if (endDate.after(new Date())) {
            gce.clear();
            gce.setTime(new Date());
            gce.set(Calendar.HOUR, 0);
            gce.set(Calendar.MINUTE, 0);
            gce.set(Calendar.SECOND, 0);
            gce.set(Calendar.MILLISECOND, 0);
            endDate = gce.getTime();
        }

        GregorianCalendar gcs = new GregorianCalendar();
        gcs.clear();
        gcs.setTime(endDate);
        gcs.set(Calendar.HOUR, 0);
        gcs.set(Calendar.MINUTE, 0);
        gcs.set(Calendar.SECOND, 0);
        gcs.set(Calendar.MILLISECOND, 0);
        gcs.add(Calendar.DAY_OF_MONTH, -2);
        Date startDate = gcs.getTime();

        return new Interval(startDate.getTime(), endDate.getTime());
    }

    @Override
    public boolean loadButtonPressed() {
        ObservationDialogDateModel.getInstance().setStartDate(getDate(), true);
        updateBandController();
        updateDrawController();
        return true;
    }

    @Override
    public void lineDataAdded(LineDataSelectorElement element) {
        updateGroupValues();
    }

    @Override
    public void lineDataRemoved(LineDataSelectorElement element) {
        updateGroupValues();
    }

}
