package org.helioviewer.jhv.plugins.swek.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.Timer;

import org.helioviewer.jhv.data.datatype.event.SWEKEventType;
import org.helioviewer.jhv.gui.ComponentUtils.SmallPanel;
import org.helioviewer.jhv.plugins.swek.download.SWEKDownloadManager;
import org.helioviewer.jhv.plugins.swek.model.EventTypePanelModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModel;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelEventType;
import org.helioviewer.jhv.plugins.swek.model.SWEKTreeModelListener;

/**
 * Panel display one event type.
 * 
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 * 
 */
@SuppressWarnings("serial")
public class EventPanel extends JPanel implements MouseListener, SWEKTreeModelListener, ActionListener {

    /** The event type for which the event panel is created */
    private final SWEKEventType eventType;

    /** Tree containing the event type and its sources. */
    private final JTree eventTypeTree;

    /** The model for this panel */
    private final EventTypePanelModel eventPanelModel;

    /** The label holding the loading text */
    private final JLabel loadingLabel;

    /** The timer handling the loading animation */
    private final Timer loadingTimer;

    private int loadingStep;

    /**
     * Creates a event panel for a certain eventType.
     */
    public EventPanel(SWEKEventType eventType) {
        this.eventType = eventType;
        SWEKTreeModel.getSingletonInstance().addSWEKTreeModelListener(this);
        eventPanelModel = new EventTypePanelModel(new SWEKTreeModelEventType(this.eventType));
        eventPanelModel.addEventPanelModelListener(SWEKDownloadManager.getSingletonInstance());
        loadingTimer = new Timer(500, this);
        loadingStep = 0;

        setLayout(new BorderLayout());
        eventTypeTree = new JTree(eventPanelModel);
        eventTypeTree.setShowsRootHandles(true);
        eventTypeTree.setSelectionModel(null);
        eventTypeTree.addMouseListener(this);
        eventTypeTree.addTreeExpansionListener(eventPanelModel);
        eventTypeTree.setCellRenderer(new SWEKEventTreeRenderer());

        // workaround for Win HiDpi
        if (System.getProperty("jhv.os").equals("windows")) {
            eventTypeTree.setRowHeight(new JCheckBox("J").getPreferredSize().height);
        }

        add(eventTypeTree, BorderLayout.CENTER);
        final FilterDialog filterDialog = new FilterDialog(eventType);

        SmallPanel filterPanel = new SmallPanel();
        filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.Y_AXIS));
        filterPanel.setOpaque(true);
        filterPanel.setBackground(Color.WHITE);

        if (eventType.containsFilter()) {
            JButton filterButton = new JButton("Filter");
            filterButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    filterDialog.setVisible(true);
                }
            });
            filterButton.addMouseListener(new MouseListener() {

                @Override
                public void mouseReleased(MouseEvent arg0) {
                }

                @Override
                public void mousePressed(MouseEvent arg0) {
                    Point pressedLocation = arg0.getLocationOnScreen();
                    Point windowLocation = new Point(pressedLocation.x, pressedLocation.y - filterDialog.getSize().height);
                    filterDialog.setLocation(windowLocation);
                }

                @Override
                public void mouseExited(MouseEvent arg0) {
                }

                @Override
                public void mouseEntered(MouseEvent arg0) {
                }

                @Override
                public void mouseClicked(MouseEvent arg0) {
                }
            });

            filterPanel.add(filterButton);

        }
        loadingLabel = new JLabel();
        filterPanel.add(loadingLabel);
        filterPanel.setSmall();
        add(filterPanel, BorderLayout.LINE_END);
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        int clickedOnRow = eventTypeTree.getRowForLocation(e.getX(), e.getY());
        eventPanelModel.rowClicked(clickedOnRow);
        eventTypeTree.revalidate();
        eventTypeTree.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void expansionChanged() {
    }

    @Override
    public void startedDownloadingEventType(SWEKEventType eventType) {
        if (eventType.equals(this.eventType) && !loadingTimer.isRunning()) {
            loadingLabel.setText("Loading   ");
            loadingStep = 0;
            loadingTimer.start();
        }
    }

    @Override
    public void stoppedDownloadingEventType(SWEKEventType eventType) {
        if (eventType.equals(this.eventType) && loadingTimer.isRunning()) {
            loadingTimer.stop();
            loadingLabel.setText("          ");
            loadingStep = 0;
        }
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        switch (loadingStep) {
        case 0:
            loadingLabel.setText("Loading   ");
            break;
        case 1:
            loadingLabel.setText("Loading.  ");
            break;
        case 2:
            loadingLabel.setText("Loading.. ");
            break;
        case 3:
            loadingLabel.setText("Loading...");
            loadingStep = -1;
            break;
        default:
            break;
        }
        loadingStep++;
    }

}
