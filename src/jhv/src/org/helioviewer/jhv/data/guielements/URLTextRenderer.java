package org.helioviewer.jhv.data.guielements;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.helioviewer.jhv.base.Regex;

public class URLTextRenderer extends DefaultTableCellRenderer implements MouseListener, MouseMotionListener {

    private int row = -1;
    private int col = -1;
    private boolean isRollover;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        String str = "";
        if (value != null) {
            str = value.toString();
        }

        if (isRolloverCell(table, row, column) && isValueURL(value)) {
            setText("<html><u><font color='blue'>" + str);
        } else if (isValueURL(value)) {
            setText("<html><font color='blue'>" + str);
        } else {
            setText(str);
        }
        return this;
    }

    private boolean isValueURL(Object value) {
        if (value instanceof String) {
            String strValue = (String) value;
            strValue = strValue.trim();
            strValue = strValue.toLowerCase();
            return Regex.HTTPpattern.matcher(strValue).matches();
        } else {
            return false;
        }
    }

    protected boolean isRolloverCell(JTable table, int row, int column) {
        return !table.isEditing() && this.row == row && col == column && isRollover;
    }

    private boolean isURLColumn(JTable table, int col, int row) {
        return isValueURL(table.getValueAt(row, col));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        JTable table = (JTable) e.getComponent();
        int prevRow = row;
        int prevCol = col;
        boolean prevRollover = isRollover;

        Point pt = e.getPoint();
        row = table.rowAtPoint(pt);
        col = table.columnAtPoint(pt);
        if (row < 0 || col < 0)
            return;

        isRollover = isURLColumn(table, col, row);
        if (row == prevRow && col == prevCol && isRollover == prevRollover || !isRollover && !prevRollover)
            return;

        Rectangle repaintRect;
        if (isRollover) {
            Rectangle r = table.getCellRect(row, col, false);
            repaintRect = prevRollover ? r.union(table.getCellRect(prevRow, prevCol, false)) : r;
            table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            repaintRect = table.getCellRect(prevRow, prevCol, false);
            table.setCursor(Cursor.getDefaultCursor());
        }
        table.repaint(repaintRect);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (row < 0 || col < 0)
            return;

        JTable table = (JTable) e.getComponent();
        if (isURLColumn(table, col, row)) {
            table.repaint(table.getCellRect(row, col, false));
            row = -1;
            col = -1;
            isRollover = false;
            table.setCursor(Cursor.getDefaultCursor());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        JTable table = (JTable) e.getComponent();

        Point pt = e.getPoint();
        int ccol = table.columnAtPoint(pt);
        int crow = table.rowAtPoint(pt);
        if (ccol < 0 || crow < 0)
            return;

        if (isURLColumn(table, ccol, crow)) {
            String value = (String) table.getValueAt(crow, ccol);
            try {
                URL url = new URL(value);
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(url.toURI());
                }
            } catch (URISyntaxException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

}
