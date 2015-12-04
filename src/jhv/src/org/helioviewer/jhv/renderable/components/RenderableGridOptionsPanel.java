package org.helioviewer.jhv.renderable.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.display.Displayer;
import org.helioviewer.jhv.gui.ComponentUtils;
import org.helioviewer.jhv.gui.components.base.TerminatedFormatterFactory;
import org.helioviewer.jhv.gui.components.base.WheelSupport;

@SuppressWarnings("serial")
public class RenderableGridOptionsPanel extends ComponentUtils.SmallPanel {

    enum GridChoiceType {
        VIEWPOINT("Viewpoint grid"), STONYHURST("Stonyhurst grid"), HCI("HCI grid");

        private final String display;

        private GridChoiceType(String s) {
            display = s;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    private static final double min = 5, max = 90;

    private JSpinner gridResolutionXSpinner;
    private JSpinner gridResolutionYSpinner;
    JComboBox gridChoiceBox;
    RenderableGrid grid;

    public RenderableGridOptionsPanel(RenderableGrid renderableGrid) {
        grid = renderableGrid;
        createGridResolutionX(grid);
        createGridResolutionY(grid);

        setLayout(new GridBagLayout());

        GridBagConstraints c0 = new GridBagConstraints();
        c0.fill = GridBagConstraints.HORIZONTAL;
        c0.weightx = 1.;
        c0.weighty = 1.;

        c0.gridy = 0;

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.EAST;
        JCheckBox axes = new JCheckBox("Solar axes", true);
        axes.setHorizontalTextPosition(SwingConstants.LEFT);
        axes.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                grid.showAxes(e.getStateChange() == ItemEvent.SELECTED);
                Displayer.display();
            }
        });
        add(axes, c0);

        c0.gridx = 3;
        c0.anchor = GridBagConstraints.EAST;
        JCheckBox labels = new JCheckBox("Grid labels", true);
        labels.setHorizontalTextPosition(SwingConstants.LEFT);
        labels.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                grid.showLabels(e.getStateChange() == ItemEvent.SELECTED);
                Displayer.display();
            }
        });
        add(labels, c0);

        c0.gridy = 1;

        c0.gridx = 0;
        c0.anchor = GridBagConstraints.EAST;
        add(new JLabel("Longitude", JLabel.RIGHT), c0);

        JFormattedTextField fx = ((JSpinner.DefaultEditor) gridResolutionXSpinner.getEditor()).getTextField();
        fx.setFormatterFactory(new TerminatedFormatterFactory("%.1f", "\u00B0", min, max));

        c0.gridx = 1;
        c0.anchor = GridBagConstraints.WEST;
        add(gridResolutionXSpinner, c0);

        c0.gridx = 2;
        c0.anchor = GridBagConstraints.EAST;
        add(new JLabel("Latitude", JLabel.RIGHT), c0);

        JFormattedTextField fy = ((JSpinner.DefaultEditor) gridResolutionYSpinner.getEditor()).getTextField();
        fy.setFormatterFactory(new TerminatedFormatterFactory("%.1f", "\u00B0", min, max));

        c0.gridx = 3;
        c0.anchor = GridBagConstraints.WEST;
        add(gridResolutionYSpinner, c0);

        //TBD
        c0.gridy = 2;
        c0.gridx = 0;
        c0.anchor = GridBagConstraints.EAST;
        add(new JLabel("Grid type", JLabel.RIGHT), c0);
        c0.gridx = 1;
        c0.anchor = GridBagConstraints.WEST;
        createGridChoiceBox(renderableGrid);
        add(gridChoiceBox, c0);

        setSmall();
    }

    public void createGridChoiceBox(RenderableGrid renderableGrid) {
        gridChoiceBox = new JComboBox();
        gridChoiceBox.setModel(new DefaultComboBoxModel(GridChoiceType.values()));
        gridChoiceBox.setToolTipText("Choose grid options");
        gridChoiceBox.setSelectedItem(GridChoiceType.VIEWPOINT);
        gridChoiceBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GridChoiceType t = (GridChoiceType) gridChoiceBox.getSelectedItem();
                grid.setCoordinates(t);
                Displayer.display();
            }
        });
    }

    public void createGridResolutionX(RenderableGrid renderableGrid) {
        gridResolutionXSpinner = new JSpinner();
        gridResolutionXSpinner.setModel(new SpinnerNumberModel(Double.valueOf(renderableGrid.getLonstepDegrees()), Double.valueOf(min), Double.valueOf(max), Double.valueOf(0.1)));
        gridResolutionXSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                grid.setLonstepDegrees((Double) gridResolutionXSpinner.getValue());
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(gridResolutionXSpinner);
    }

    public void createGridResolutionY(RenderableGrid renderableGrid) {
        gridResolutionYSpinner = new JSpinner();
        gridResolutionYSpinner.setModel(new SpinnerNumberModel(Double.valueOf(renderableGrid.getLatstepDegrees()), Double.valueOf(min), Double.valueOf(max), Double.valueOf(0.1)));
        gridResolutionYSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                grid.setLatstepDegrees((Double) gridResolutionYSpinner.getValue());
                Displayer.display();
            }
        });
        WheelSupport.installMouseWheelSupport(gridResolutionYSpinner);
    }

}
