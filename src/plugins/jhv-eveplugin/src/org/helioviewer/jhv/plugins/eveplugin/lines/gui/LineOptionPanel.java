package org.helioviewer.jhv.plugins.eveplugin.lines.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.plugins.eveplugin.lines.data.Band;
import org.helioviewer.jhv.plugins.eveplugin.lines.model.EVEDrawController;

@SuppressWarnings("serial")
public class LineOptionPanel extends JPanel {

    private final Band band;

    private static final String[] options = { "Left", "Right" };

    public LineOptionPanel(Band _band) {
        band = _band;

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        JButton pickColor = new JButton("Line color");
        pickColor.setMargin(new Insets(0, 0, 0, 0));
        pickColor.setToolTipText("Change the color of the current line");
        pickColor.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Color newColor = JColorChooser.showDialog(ImageViewerGui.getMainFrame(), "Choose Line Color", band.getGraphColor());
                if (newColor != null) {
                    band.setGraphColor(newColor);
                }
            }
        });
        add(pickColor, c);

        JLabel yAxis = new JLabel("Y-axis");
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        add(yAxis, c);

        JComboBox changeAxis = new JComboBox(options);
        changeAxis.setToolTipText("Switch the axis");
        changeAxis.setSelectedIndex(EVEDrawController.getSingletonInstance().getAxisLocation(band));
        changeAxis.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EVEDrawController.getSingletonInstance().changeAxis(band);
            }
        });
        changeAxis.setEnabled(EVEDrawController.getSingletonInstance().canChangeAxis(band));

        c.anchor = GridBagConstraints.EAST;
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 1;
        c.fill = GridBagConstraints.NONE;
        add(changeAxis, c);
    }

}
