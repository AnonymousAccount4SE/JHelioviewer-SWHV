package org.helioviewer.jhv.camera;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;
import org.helioviewer.jhv.gui.dialogs.TextDialog;

@SuppressWarnings("serial")
public class CameraOptionsPanel extends SmallPanel implements PositionLoadFire {

    private enum CameraMode {
        OBSERVER, EARTH, EXPERT
    }

    private static final double FOVAngleDefault = 0.8;
    private double FOVAngle = FOVAngleDefault * Math.PI / 180.;

    private final CameraOptionPanelExpert expertOptionPanel;
    private CameraOptionPanel currentOptionPanel;

    private static final String explanation = "Observer: view from observer.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "Earth: view from Earth.\nCamera time defined by timestamps of the master layer.\n\n" +
                                              "Other: view from selected object.\nCamera time defined by timestamps of the master layer, unless " +
                                              "\"Use master layer timestamps\" is off. In that case, camera time is interpolated in the configured time interval.";

    public CameraOptionsPanel() {
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;

        JPanel radio = new JPanel(new FlowLayout(FlowLayout.LEADING));

        JRadioButton observerItem = new JRadioButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeCamera(CameraMode.OBSERVER);
            }
        });
        observerItem.setText("Observer View");
        observerItem.setSelected(true);
        radio.add(observerItem);

        JRadioButton earthItem = new JRadioButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeCamera(CameraMode.EARTH);
            }
        });
        earthItem.setText("Earth View");
        radio.add(earthItem);

        JRadioButton expertItem = new JRadioButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changeCamera(CameraMode.EXPERT);
            }
        });
        expertItem.setText("Other View");
        radio.add(expertItem);

        ButtonGroup group = new ButtonGroup();
        group.add(observerItem);
        group.add(earthItem);
        group.add(expertItem);

        add(radio, c);
        c.gridx = 1;
        c.weightx = 0;

        JButton infoButton = new JButton(new AbstractAction() {
            {
                putValue(SHORT_DESCRIPTION, "Show viewpoint info");
                putValue(SMALL_ICON, IconBank.getIcon(JHVIcon.INFO));
            }

            @Override
            public void actionPerformed(ActionEvent arg0) {
                TextDialog td = new TextDialog("Viewpoint options information", explanation);
                td.showDialog();
            }
        });
        infoButton.setBorder(null);
        infoButton.setText(null);
        infoButton.setBorderPainted(false);
        infoButton.setFocusPainted(false);
        infoButton.setContentAreaFilled(false);
        add(infoButton, c);

        // fov
        double min = 0, max = 180;

        JPanel fovPanel = new JPanel();
        fovPanel.setLayout(new BoxLayout(fovPanel, BoxLayout.LINE_AXIS));
        fovPanel.add(new JLabel("FOV angle"));

        JSpinner fovSpinner = new JSpinner();
        fovSpinner.setModel(new SpinnerNumberModel(Double.valueOf(FOVAngleDefault), Double.valueOf(min), Double.valueOf(max), Double.valueOf(0.01)));
        fovSpinner.addChangeListener(e -> {
            FOVAngle = (Double) fovSpinner.getValue() * Math.PI / 180.;
            Displayer.display();
        });

        JFormattedTextField f = ((JSpinner.DefaultEditor) fovSpinner.getEditor()).getTextField();
        f.setFormatterFactory(new TerminatedFormatterFactory("%.2f", "\u00B0", min, max));

        WheelSupport.installMouseWheelSupport(fovSpinner);
        fovPanel.add(fovSpinner);

        fovSpinner.setMaximumSize(new Dimension(6, 22));
        fovPanel.add(Box.createHorizontalGlue());

        c.weightx = 1;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 1;
        add(fovPanel, c);

        setSmall();

        PositionLoad positionLoad = new PositionLoad(this);
        ((UpdateViewpoint.UpdateViewpointExpert) UpdateViewpoint.updateExpert).setPositionLoad(positionLoad);
        expertOptionPanel = new CameraOptionPanelExpert(positionLoad);
    }

    public double getFOVAngle() {
        return FOVAngle;
    }

    private void switchOptionsPanel(CameraOptionPanel newOptionPanel) {
        if (currentOptionPanel == newOptionPanel)
            return;

        if (currentOptionPanel != null) {
            currentOptionPanel.deactivate();
            remove(currentOptionPanel);
        }

        if (newOptionPanel != null) {
            newOptionPanel.activate();
            newOptionPanel.syncWithLayer();

            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = 2;
            add(newOptionPanel, c);
        }
        currentOptionPanel = newOptionPanel;
        revalidate();
    }

    private void changeCamera(CameraMode mode) {
        UpdateViewpoint update;
        CameraOptionPanel panel = null;

        switch (mode) {
            case EXPERT:
                update = UpdateViewpoint.updateExpert;
                panel = expertOptionPanel;
            break;
            case EARTH:
                update = UpdateViewpoint.updateEarth;
            break;
            default:
                update = UpdateViewpoint.updateObserver;
        }
        Displayer.setViewpointUpdate(update);
        Displayer.getCamera().reset();

        switchOptionsPanel(panel);
    }

    @Override
    public void fireLoaded(String state) {
        expertOptionPanel.fireLoaded(state);
        Displayer.getCamera().refresh();
    }

}
