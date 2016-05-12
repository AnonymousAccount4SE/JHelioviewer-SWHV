package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.base.time.JHVDate;
import org.helioviewer.jhv.base.time.TimeUtils;
import org.helioviewer.jhv.gui.components.base.JSeparatorComboBox;
import org.helioviewer.jhv.gui.components.base.TimeTextField;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarDatePicker;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarEvent;
import org.helioviewer.jhv.gui.components.calendar.JHVCalendarListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.viewmodel.view.View;

@SuppressWarnings("serial")
public class CameraOptionPanelExpert extends CameraOptionPanel implements LayersListener {

    private final JLabel loadedLabel;

    private JPanel addBeginDatePanel;
    private JHVCalendarDatePicker beginDatePicker;
    private TimeTextField beginTimePicker;

    private JPanel addEndDatePanel;
    private JHVCalendarDatePicker endDatePicker;
    private TimeTextField endTimePicker;

    private JPanel buttonPanel;

    private final JCheckBox exactDateCheckBox;

    private final PositionLoad positionLoad;

    CameraOptionPanelExpert(PositionLoad _positionLoad) {
        positionLoad = _positionLoad;

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);

        JPanel loadedLabelPanel = new JPanel();
        loadedLabelPanel.setLayout(new BoxLayout(loadedLabelPanel, BoxLayout.LINE_AXIS));

        loadedLabel = new JLabel("Status: Not loaded");
        loadedLabelPanel.add(loadedLabel);
        c.gridy = 1;
        add(loadedLabelPanel, c);
        c.gridy = 2;
        add(new JSeparator(SwingConstants.HORIZONTAL), c);
        c.gridy = 3;

        addObjectCombobox(c);
        exactDateCheckBox = new JCheckBox("Use master layer timestamps", true);
        c.gridy = 4;
        add(exactDateCheckBox, c);
        c.gridy = 5;
        addBeginDatePanel(c);
        c.gridy = 6;
        addEndDatePanel(c);
        addBeginDatePanel.setVisible(false);
        addEndDatePanel.setVisible(false);
        c.gridy = 7;

        addSyncButtons(c);
        buttonPanel.setVisible(false);
        exactDateCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                boolean selected = !exactDateCheckBox.isSelected();
                addBeginDatePanel.setVisible(selected);
                addEndDatePanel.setVisible(selected);
                buttonPanel.setVisible(selected);
                if (selected) {
                    setBeginTime(false);
                    setEndTime(true);
                }
            }
        });

        setSmall();
    }

    private void addSyncButtons(GridBagConstraints c) {
        JButton synchronizeWithLayersButton = new JButton("Sync");
        synchronizeWithLayersButton.setToolTipText("Fill selected layer dates");
        synchronizeWithLayersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayer();
            }
        });

        JButton synchronizeWithNowButton = new JButton("Now");
        synchronizeWithNowButton.setToolTipText("Fill twice current time");
        synchronizeWithNowButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncBothLayerNow();
            }
        });

        JButton synchronizeWithCurrentButton = new JButton("Current");
        synchronizeWithCurrentButton.setToolTipText("Fill twice selected layer time");
        synchronizeWithCurrentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncWithLayerCurrentTime();
            }
        });
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(0, 3));

        synchronizeWithLayersButton.getMaximumSize().width = 15;
        buttonPanel.add(synchronizeWithLayersButton);
        synchronizeWithCurrentButton.getMaximumSize().width = 15;
        buttonPanel.add(synchronizeWithCurrentButton);
        synchronizeWithNowButton.getMaximumSize().width = 15;
        buttonPanel.add(synchronizeWithNowButton);

        add(buttonPanel, c);
    }

    @Override
    void activate() {
        Layers.addLayersListener(this);
    }

    @Override
    void deactivate() {
        Layers.removeLayersListener(this);
    }

    @Override
    public void layerAdded(View view) {
    }

    @Override
    public void activeLayerChanged(View view) {
        if (view != null) {
            positionLoad.setBeginDate(view.getFirstTime().getDate(), false);
            positionLoad.setEndDate(view.getLastTime().getDate(), true);
            // Displayer.render();
        }
    }

    private void addObjectCombobox(GridBagConstraints c) {
        JSeparatorComboBox objectCombobox = new JSeparatorComboBox(SpaceObject.getObjectList().toArray());
        objectCombobox.setSelectedItem(SpaceObject.earth);
        objectCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    String object = ((SpaceObject) event.getItem()).getUrlName();
                    positionLoad.setObserver(object, true);
                    // Displayer.render();
                }
            }
        });
        add(objectCombobox, c);
    }

    private void addBeginDatePanel(GridBagConstraints c) {
        beginDatePicker = new JHVCalendarDatePicker();
        beginDatePicker.getTextField().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                beginDatePicker.checkDateStringInTextField();
                setBeginTime(true);
            }
        });
        beginTimePicker = new TimeTextField();
        addBeginDatePanel = new JPanel();
        addBeginDatePanel.setLayout(new BoxLayout(addBeginDatePanel, BoxLayout.LINE_AXIS));

        JLabel beginDateLabel = new JLabel("Begin", JLabel.RIGHT);
        beginDateLabel.setPreferredSize(new Dimension(40, 0));

        addBeginDatePanel.add(beginDateLabel);

        JPanel beginDatetimePanel = new JPanel();
        beginDatetimePanel.setLayout(new GridLayout(0, 2));
        beginDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setBeginTime(true);
            }
        });
        beginTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setBeginTime(true);
            }
        });
        addBeginDatePanel.add(beginDatePicker);
        addBeginDatePanel.add(beginTimePicker);
        addBeginDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));
        add(addBeginDatePanel, c);
    }

    private void setEndTime(boolean applyChanges) {
        Date dt = endTimePicker.getValue();
        Date end_date = new Date(endDatePicker.getDate().getTime() + dt.getTime());
        positionLoad.setEndDate(end_date, applyChanges);
        // Displayer.render();
    }

    private void setBeginTime(boolean applyChanges) {
        Date dt = beginTimePicker.getValue();
        Date begin_date = new Date(beginDatePicker.getDate().getTime() + dt.getTime());
        positionLoad.setBeginDate(begin_date, applyChanges);
        // Displayer.render();
    }

    @Override
    void syncWithLayer() {
        syncWithLayerBeginTime(false);
        syncWithLayerEndTime(true);
    }

    private void syncWithLayerBeginTime(boolean applyChanges) {
        View view = Layers.getActiveView();
        if (view == null)
            return;

        JHVDate startTime = view.getFirstTime();
        beginDatePicker.setDate(new Date(startTime.milli - startTime.milli % (60 * 60 * 24 * 1000)));
        beginTimePicker.setText(TimeUtils.timeDateFormat.format(startTime.getDate()));
        setBeginTime(applyChanges);
    }

    private void syncBothLayerNow() {
        Date nowDate = new Date(System.currentTimeMillis());
        Date syncDate = new Date(nowDate.getTime() - nowDate.getTime() % (60 * 60 * 24 * 1000));

        beginDatePicker.setDate(syncDate);
        beginTimePicker.setText(TimeUtils.timeDateFormat.format(nowDate));

        endDatePicker.setDate(syncDate);
        endTimePicker.setText(TimeUtils.timeDateFormat.format(nowDate));

        setBeginTime(false);
        setEndTime(true);
    }

    private void syncWithLayerCurrentTime() {
        Date currentDate = Layers.getLastUpdatedTimestamp().getDate();
        Date syncDate = new Date(currentDate.getTime() - currentDate.getTime() % (60 * 60 * 24 * 1000));

        endDatePicker.setDate(syncDate);
        endTimePicker.setText(TimeUtils.timeDateFormat.format(currentDate));

        beginDatePicker.setDate(syncDate);
        beginTimePicker.setText(TimeUtils.timeDateFormat.format(currentDate));

        setBeginTime(false);
        setEndTime(true);
    }

    private void syncWithLayerEndTime(boolean applyChanges) {
        View view = Layers.getActiveView();
        if (view == null)
            return;

        JHVDate endTime = view.getLastTime();
        endDatePicker.setDate(new Date(endTime.milli - endTime.milli % (60 * 60 * 24 * 1000)));
        endTimePicker.setText(TimeUtils.timeDateFormat.format(endTime.getDate()));
        setEndTime(applyChanges);
    }

    private void addEndDatePanel(GridBagConstraints c) {
        endDatePicker = new JHVCalendarDatePicker();
        endDatePicker.getTextField().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent arg0) {
            }

            @Override
            public void focusLost(FocusEvent arg0) {
                beginDatePicker.checkDateStringInTextField();
                setEndTime(true);
            }
        });
        endTimePicker = new TimeTextField();
        addEndDatePanel = new JPanel();
        addEndDatePanel.setLayout(new BoxLayout(addEndDatePanel, BoxLayout.LINE_AXIS));

        JLabel endDateLabel = new JLabel("End", JLabel.RIGHT);
        endDateLabel.setPreferredSize(new Dimension(40, 0));
        addEndDatePanel.add(endDateLabel);

        JPanel endDatetimePanel = new JPanel();
        endDatetimePanel.setLayout(new GridLayout(0, 2));
        endDatePicker.addJHVCalendarListener(new JHVCalendarListener() {
            @Override
            public void actionPerformed(JHVCalendarEvent e) {
                setEndTime(true);
            }
        });
        endTimePicker.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEndTime(true);
            }
        });
        addEndDatePanel.add(endDatePicker);
        addEndDatePanel.add(endTimePicker);
        addEndDatePanel.add(Box.createRigidArea(new Dimension(40, 0)));

        add(addEndDatePanel, c);
    }

    void fireLoaded(String state) {
        String htmlstart = "<html><body style='width: 200px'>";
        String htmlend = "</body></html>";
        loadedLabel.setText(htmlstart + "Status: " + state + htmlend);
    }

}
