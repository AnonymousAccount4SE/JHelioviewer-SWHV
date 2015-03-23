package org.helioviewer.jhv.plugin.renderable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.Border;

import org.helioviewer.jhv.display.Displayer;

public class RenderableContainerPanel extends JPanel {
    private static final int ROW_HEIGHT = 20;
    private static final int VISIBLEROW = 0;
    private static final int TITLEROW = 1;
    private static final int REMOVEROW = 2;
    public static final int NUMBEROFCOLUMNS = 3;

    public final JTable grid;
    private Component optionsPanel = new JPanel();
    GridBagConstraints gc = new GridBagConstraints();
    private final JPanel optionsPanelWrapper;

    public RenderableContainerPanel(RenderableContainer renderableContainer) {
        this.setLayout(new GridBagLayout());
        Border lineBorder = BorderFactory.createLineBorder(Color.black);
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.fill = GridBagConstraints.BOTH;
        grid = new JTable(renderableContainer);
        this.add(grid, gc);
        grid.setBorder(lineBorder);
        grid.setTableHeader(null);
        grid.setShowGrid(false);
        grid.setRowSelectionAllowed(true);
        grid.setColumnSelectionAllowed(false);
        grid.setIntercellSpacing(new Dimension(0, 0));

        grid.setRowHeight(ROW_HEIGHT);
        grid.setBackground(Color.white);
        grid.getColumnModel().getColumn(VISIBLEROW).setCellRenderer(new RenderableVisibleCellRenderer());
        grid.getColumnModel().getColumn(VISIBLEROW).setPreferredWidth(25);
        grid.getColumnModel().getColumn(VISIBLEROW).setMaxWidth(25);

        grid.getColumnModel().getColumn(TITLEROW).setCellRenderer(new RenderableCellRenderer());
        grid.getColumnModel().getColumn(REMOVEROW).setCellRenderer(new RenderableRemoveCellRenderer());
        grid.getColumnModel().getColumn(REMOVEROW).setPreferredWidth(25);
        grid.getColumnModel().getColumn(REMOVEROW).setMaxWidth(25);

        grid.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    handlePopup(e);
                }
            }

            /**
             * Handle with right-click menus
             *
             * @param e
             */
            public void handlePopup(MouseEvent e) {

            }

            /**
             * Handle with clicks on hide/show/remove layer icons
             */
            @Override
            public void mouseClicked(MouseEvent e) {

                int row = grid.rowAtPoint(new Point(e.getX(), e.getY()));
                int col = grid.columnAtPoint(new Point(e.getX(), e.getY()));
                RenderableContainer model = (RenderableContainer) grid.getModel();

                if (col == VISIBLEROW) {
                    Renderable renderable = (Renderable) renderableContainer.getValueAt(row, col);
                    renderable.setVisible(!renderable.isVisible());
                    model.fireTableCellUpdated(row, col);
                    Displayer.display();
                } else if (col == TITLEROW) {
                    Renderable renderable = (Renderable) model.getValueAt(row, col);
                    setOptionsPanel(renderable);
                } else if (col == REMOVEROW) {
                    model.removeRow(row);
                    Displayer.display();
                }
            }
        });

        this.optionsPanelWrapper = new JPanel();
        this.optionsPanelWrapper.setBorder(BorderFactory.createTitledBorder("Options"));
        optionsPanelWrapper.add(optionsPanel);
        gc.gridy = 1;
        add(optionsPanelWrapper, gc);

    }

    private void setOptionsPanel(Renderable renderable) {
        optionsPanelWrapper.remove(optionsPanel);
        optionsPanel = renderable.getOptionsPanel();
        optionsPanelWrapper.add(optionsPanel);
        this.getParent().revalidate();
        this.getParent().repaint();
    }

}
