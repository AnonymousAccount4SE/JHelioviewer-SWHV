package org.helioviewer.jhv.data.guielements;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.helioviewer.jhv.data.datatype.event.JHVEvent;
import org.helioviewer.jhv.data.datatype.event.JHVEventRelation;
import org.helioviewer.jhv.data.datatype.event.comparator.JHVEventRelationComparator;
import org.helioviewer.jhv.data.guielements.listeners.DataCollapsiblePanelModelListener;
import org.helioviewer.jhv.data.guielements.model.DataCollapsiblePanelModel;
import org.helioviewer.jhv.gui.ImageViewerGui;

/**
 * Popup displaying informations about a HEK event.
 *
 * <p>
 * This panel is a JDialog, so that it can be displayed on top of an GLCanvas,
 * which is not possible for other swing components.
 *
 * <p>
 * For further informations about solar events, see
 * {@link org.helioviewer.jhv.solarevents}.
 *
 * @author Markus Langenberg
 * @author Malte Nuhn
 * @author Bram.Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
@SuppressWarnings({ "serial" })
public class SWEKEventInformationDialog extends JDialog implements WindowListener, DataCollapsiblePanelModelListener {

    private JPanel allTablePanel;

    private DataCollapsiblePanel standardParameters;
    // private DataCollapsiblePanel advancedParameters;
    private DataCollapsiblePanel allParameters;
    private DataCollapsiblePanel precedingEventsPanel;
    private DataCollapsiblePanel followingEventsPanel;
    private DataCollapsiblePanel otherRelatedEventsPanel;

    private int expandedPanels;

    private EventDescriptionPanel eventDescriptionPanel;

    private JHVEvent event;

    private Integer nrOfWindowsOpened;

    private final DataCollapsiblePanelModel model;

    /**
     *
     *
     * @param event
     */
    public SWEKEventInformationDialog(JHVEvent event) {
        super(ImageViewerGui.getMainFrame(), event.getJHVEventType().getEventType().getEventName());
        model = new DataCollapsiblePanelModel();
        model.addListener(this);
        initDialog(event);

    }

    public SWEKEventInformationDialog(JHVEvent event, SWEKEventInformationDialog parent, boolean modal) {
        super(parent, event.getJHVEventType().getEventType().getEventName(), modal);
        model = new DataCollapsiblePanelModel();
        model.addListener(this);
        initDialog(event);
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        decrementNrOfWindows();
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    private void initDialog(JHVEvent _event) {
        event = _event;
        nrOfWindowsOpened = 0;
        eventDescriptionPanel = new EventDescriptionPanel(event);

        initAllTablePanel();
        initParameterCollapsiblePanels();
        setCollapsiblePanels();

        setLayout(new GridBagLayout());

        GridBagConstraints eventDescriptionConstraint = new GridBagConstraints();
        eventDescriptionConstraint.gridx = 0;
        eventDescriptionConstraint.gridy = 0;
        eventDescriptionConstraint.weightx = 1;
        eventDescriptionConstraint.weighty = 0;
        eventDescriptionConstraint.anchor = GridBagConstraints.WEST;
        eventDescriptionConstraint.fill = GridBagConstraints.BOTH;

        add(eventDescriptionPanel, eventDescriptionConstraint);

        GridBagConstraints allTablePanelConstraint = new GridBagConstraints();
        allTablePanelConstraint.gridx = 0;
        allTablePanelConstraint.gridy = 1;
        allTablePanelConstraint.gridwidth = 1;
        allTablePanelConstraint.weightx = 1;
        allTablePanelConstraint.weighty = 1;
        allTablePanelConstraint.fill = GridBagConstraints.BOTH;
        // add(new JScrollPane(allTablePanel), allTablePanelConstraint);
        add(allTablePanel, allTablePanelConstraint);
    }

    /**
     * initialize the allTablePanel
     *
     */
    private void initAllTablePanel() {
        allTablePanel = new JPanel();
        // allTablePanel.setLayout(new BoxLayout(allTablePanel,
        // BoxLayout.Y_AXIS));
        allTablePanel.setLayout(new GridBagLayout());
    }

    /**
     * initialize collapsible panels
     */
    private void initParameterCollapsiblePanels() {
        ParameterTablePanel standardParameterPanel = new ParameterTablePanel(event.getVisibleEventParameters().values());
        expandedPanels = 1;
        standardParameters = new DataCollapsiblePanel("Standard Parameters", standardParameterPanel, true, model);

        // ParameterTablePanel advancedParameterPanel = new
        // ParameterTablePanel(event.getNonVisibleEventParameters().values());
        // advancedParameters = new DataCollapsiblePanel("Advanced Parameters",
        // advancedParameterPanel, false, model);

        ParameterTablePanel allEventsPanel = new ParameterTablePanel(event.getAllEventParameters().values());
        allParameters = new DataCollapsiblePanel("All Parameters", allEventsPanel, false, model);

        Map<String, JHVEventRelation> precedingEvents = event.getEventRelationShip().getPrecedingEvents();
        if (!precedingEvents.isEmpty() && notNullRelationShip(precedingEvents)) {
            precedingEventsPanel = createRelatedEventsCollapsiblePane("Preceding Events", precedingEvents);
        }

        Map<String, JHVEventRelation> nextEvents = event.getEventRelationShip().getNextEvents();
        if (!nextEvents.isEmpty() && notNullRelationShip(nextEvents)) {
            followingEventsPanel = createRelatedEventsCollapsiblePane("Following Events", nextEvents);
        }

        Map<String, JHVEventRelation> relatedEvents = event.getEventRelationShip().getRelatedEventsByRule();
        if (!relatedEvents.isEmpty() && notNullRelationShip(relatedEvents)) {
            otherRelatedEventsPanel = createRelatedEventsCollapsiblePane("Other Related Events", relatedEvents);
        }
    }

    private boolean notNullRelationShip(Map<String, JHVEventRelation> precedingEvents) {
        for (JHVEventRelation r : precedingEvents.values()) {
            if (r.getTheEvent() != null) {
                return true;
            }
        }
        return false;
    }

    private void setCollapsiblePanels() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1;
        gc.anchor = GridBagConstraints.NORTH;
        if (standardParameters.isExpanded()) {
            gc.weighty = 1;
        } else {
            gc.weighty = 0;
        }
        allTablePanel.add(standardParameters, gc);

        /*
         * gc.gridy = 1; if (advancedParameters.isExpanded()) { gc.weighty = 1;
         * } else { gc.weighty = 0; } allTablePanel.add(advancedParameters, gc);
         */

        gc.gridy = 1;
        if (allParameters.isExpanded()) {
            gc.weighty = 1;
        } else {
            gc.weighty = 0;
        }
        allTablePanel.add(allParameters, gc);

        int gridYPosition = 2;

        if (precedingEventsPanel != null) {
            gc.gridy = gridYPosition;
            if (precedingEventsPanel.isExpanded()) {
                gc.weighty = 1;
            } else {
                gc.weighty = 0;
            }
            allTablePanel.add(precedingEventsPanel, gc);
            gridYPosition++;
        }

        if (followingEventsPanel != null) {
            gc.gridy = gridYPosition;
            if (followingEventsPanel.isExpanded()) {
                gc.weighty = 1;
            } else {
                gc.weighty = 0;
            }
            allTablePanel.add(followingEventsPanel, gc);
            gridYPosition++;
        }

        if (otherRelatedEventsPanel != null) {
            gc.gridy = gridYPosition;
            if (otherRelatedEventsPanel.isExpanded()) {
                gc.weighty = 1;
            } else {
                gc.weighty = 0;
            }
            allTablePanel.add(otherRelatedEventsPanel, gc);
            gridYPosition++;
        }
    }

    private DataCollapsiblePanel createRelatedEventsCollapsiblePane(String relation, Map<String, JHVEventRelation> relations) {
        JPanel allPrecedingEvents = new JPanel();
        allPrecedingEvents.setLayout(new BoxLayout(allPrecedingEvents, BoxLayout.Y_AXIS));
        SortedSet<JHVEventRelation> sortedER = new TreeSet<JHVEventRelation>(new JHVEventRelationComparator());
        for (final JHVEventRelation er : relations.values()) {
            if (er.getTheEvent() != null) {
                sortedER.add(er);
            }
        }
        for (final JHVEventRelation er : sortedER) {
            if (er.getTheEvent() != null) {
                JPanel eventAndButtonPanel = new JPanel();

                JButton detailsButton = new JButton("Details");
                detailsButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (er.getTheEvent() != null) {
                            incrementNrOfWindows();
                            SWEKEventInformationDialog dialog = new SWEKEventInformationDialog(er.getTheEvent(), SWEKEventInformationDialog.this, false);
                            // dialog.setLocation();
                            dialog.addWindowListener(SWEKEventInformationDialog.this);
                            dialog.validate();
                            dialog.pack();
                            dialog.setVisible(true);
                        }
                    }
                });

                eventAndButtonPanel.setLayout(new GridBagLayout());
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = 0;
                c.gridy = 0;
                c.fill = GridBagConstraints.BOTH;
                c.anchor = GridBagConstraints.CENTER;
                c.weightx = 1;
                c.weighty = 1;
                eventAndButtonPanel.add(new EventDescriptionPanel(er.getTheEvent()), c);

                c.gridy = 1;
                c.fill = GridBagConstraints.NONE;
                c.weightx = 0;
                c.weighty = 0;
                c.anchor = GridBagConstraints.EAST;

                eventAndButtonPanel.add(detailsButton, c);
                allPrecedingEvents.add(eventAndButtonPanel);
            }
        }
        return new DataCollapsiblePanel(relation, new JScrollPane(allPrecedingEvents), false, model);
    }

    private void incrementNrOfWindows() {
        nrOfWindowsOpened++;
    }

    private void decrementNrOfWindows() {
        nrOfWindowsOpened--;
    }

    @Override
    public void repack() {
        int newExpandedPanels = nrOfExpandedPanels();
        // if (!(newExpandedPanels == 0 || (newExpandedPanels == 1 &&
        // expandedPanels == 0))) {
        // this.setPreferredSize(new Dimension(this.getWidth(),
        // this.getHeight()));
        // }
        allTablePanel.removeAll();
        setCollapsiblePanels();
        this.invalidate();
        this.validate();
        this.pack();
        expandedPanels = newExpandedPanels;
    }

    private int nrOfExpandedPanels() {
        int newExpandedPanels = 0;
        if (allParameters.isExpanded()) {
            newExpandedPanels++;
        }
        if (standardParameters.isExpanded()) {
            newExpandedPanels++;
        }
        /*
         * if (advancedParameters.isExpanded()) { newExpandedPanels++; }
         */
        if ((followingEventsPanel != null && followingEventsPanel.isExpanded())) {
            newExpandedPanels++;
        }
        if ((precedingEventsPanel != null && precedingEventsPanel.isExpanded())) {
            newExpandedPanels++;
        }
        if ((otherRelatedEventsPanel != null && otherRelatedEventsPanel.isExpanded())) {
            newExpandedPanels++;
        }
        return newExpandedPanels;
    }

}
