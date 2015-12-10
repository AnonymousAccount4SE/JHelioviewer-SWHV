package org.helioviewer.jhv.data.guielements;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.RowFilter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.data.datatype.event.JHVEventParameter;
import org.helioviewer.jhv.data.guielements.model.EmptyRowFilter;
import org.helioviewer.jhv.data.guielements.model.ParameterTableModel;

/**
 * Represents a panel with a table containing all the parameters from the given
 * list.
 *
 * @author Bram Bourgoignie (Bram.Bourgoignie@oma.be)
 *
 */
@SuppressWarnings({ "serial" })
public class ParameterTablePanel extends JPanel {

    /** the table */
    private final JTable table;
    /** the model for the table */
    private final ParameterTableModel parameterModel;
    /** checkbox indicating null value should be visible */
    private final JCheckBox nullValue;

    private final TableRowSorter<ParameterTableModel> sorter;

    /**
     * Creates a table panel for the given parameters.
     *
     * @param parameters
     *            the parameters
     */
    public ParameterTablePanel(Collection<JHVEventParameter> parameters) {
        super();
        setLayout(new BorderLayout());
        parameterModel = new ParameterTableModel(parameters);
        table = new JTable(parameterModel) {
            /*
             * @Override public boolean getScrollableTracksViewportWidth() {
             * return getPreferredSize().width < getParent().getWidth(); }
             */
        };
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setCellRenderer(new WrappedTextCellRenderer());
        table.getColumnModel().getColumn(0).setResizable(false);
        table.getColumnModel().getColumn(0).setMaxWidth(180);
        table.setPreferredScrollableViewportSize(new Dimension(table.getWidth(), 150));
        // table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        sorter = new TableRowSorter<ParameterTableModel>(parameterModel);
        table.setRowSorter(sorter);

        URLTextRenderer renderer = new URLTextRenderer();
        table.setDefaultRenderer(String.class, renderer);
        table.addMouseMotionListener(renderer);
        table.addMouseListener(renderer);

        nullValue = new JCheckBox();
        JPanel nullValuePanel = new JPanel();
        JLabel nullValueLabel = new JLabel("Show Empty Parameters");
        nullValuePanel.add(nullValue);
        nullValuePanel.add(nullValueLabel);
        RowFilter<ParameterTableModel, Integer> rf = null;
        // If current expression doesn't parse, don't update.
        try {
            rf = new EmptyRowFilter();
        } catch (java.util.regex.PatternSyntaxException ex) {
            return;
        }
        sorter.setRowFilter(rf);

        nullValue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nullValue.isSelected()) {
                    sorter.setRowFilter(null);
                } else {
                    RowFilter<ParameterTableModel, Integer> rf = null;
                    // If current expression doesn't parse, don't update.
                    try {
                        rf = new EmptyRowFilter();
                    } catch (java.util.regex.PatternSyntaxException ex) {
                        return;
                    }
                    sorter.setRowFilter(rf);
                }

            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(nullValuePanel, BorderLayout.PAGE_END);
    }

    private static class WrappedTextCellRenderer extends JTextArea implements TableCellRenderer {
        public WrappedTextCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((String) value);
            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            if (table.getRowHeight(row) != getPreferredSize().height) {
                table.setRowHeight(row, getPreferredSize().height);
            }
            return this;
        }
    }
}
