package org.helioviewer.jhv.gui.dialogs;

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.XMLUtils;
import org.helioviewer.jhv.gui.JHVFrame;
import org.helioviewer.jhv.gui.components.base.WrappedTable;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.log.Log;
import org.helioviewer.jhv.metadata.HelioviewerMetaData;
import org.helioviewer.jhv.metadata.MetaData;
import org.helioviewer.jhv.time.TimeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import com.jidesoft.swing.JideSplitPane;
import com.jidesoft.swing.SearchableUtils;

@SuppressWarnings("serial")
public class MetaDataDialog extends StandardDialog implements ShowableDialog {

    private final JideSplitPane content = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
    private final JButton exportFitsButton = new JButton("Export FITS Header as XML");

    private final WrappedTable fitsTable = new WrappedTable();
    private final JTextArea basicArea = new JTextArea();
    private final JTextArea hvArea = new JTextArea();

    public MetaDataDialog() {
        super(JHVFrame.getFrame(), "Image Information");

        setInitFocusedComponent(fitsTable);
        SearchableUtils.installSearchable(fitsTable);

        basicArea.setEditable(false);
        hvArea.setEditable(false);
        hvArea.setLineWrap(true);
        hvArea.setWrapStyleWord(true);

        content.add(new JScrollPane(basicArea));
        content.add(new JScrollPane(fitsTable));
        content.add(new JScrollPane(hvArea));
    }

    @Override
    public ButtonPanel createButtonPanel() {
        AbstractAction close = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        setDefaultAction(close);
        setDefaultCancelAction(close);

        JButton button = new JButton(close);
        button.setText("Close");

        ButtonPanel panel = new ButtonPanel();
        panel.add(button, ButtonPanel.AFFIRMATIVE_BUTTON);
        panel.add(exportFitsButton, ButtonPanel.OTHER_BUTTON);

        return panel;
    }

    @Override
    public JComponent createContentPanel() {
        return content;
    }

    @Nullable
    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public void showDialog() {
        pack();
        content.setDividerLocation(1, 600);
        setLocationRelativeTo(JHVFrame.getFrame());
        setVisible(true);
    }

    public void setMetaData(ImageLayer layer) {
        hvArea.setText("");
        lastNodeSeen = null;
        exportFitsButton.setEnabled(false);

        DefaultTableModel fitsModel = new DefaultTableModel(new Object[0][0], new Object[]{"FITS Keyword", "Value"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        fitsTable.setModel(fitsModel);
        fitsTable.setRowSorter(new TableRowSorter<>(fitsModel));
        fitsTable.getColumnModel().getColumn(1).setCellRenderer(new WrappedTable.WrappedTextRenderer());

        MetaData metaData = layer.getMetaData();
        if (!(metaData instanceof HelioviewerMetaData)) {
            basicArea.setText("No Helioviewer metadata available");
            return;
        }

        HelioviewerMetaData m = (HelioviewerMetaData) metaData;
        basicArea.setText("Observatory: " + m.getObservatory() + '\n' +
                "Instrument: " + m.getInstrument() + '\n' +
                "Detector: " + m.getDetector() + '\n' +
                "Measurement: " + m.getMeasurement() + '\n' +
                "Observation Date: " + m.getViewpoint().time);
        basicArea.setCaretPosition(0);

        try {
            String xml = layer.getView().getXMLMetaData();
            Document doc = XMLUtils.parse(xml);

            // Send xml data to meta data dialog box
            StringBuilder hvSB = new StringBuilder();
            Node root = doc.getDocumentElement().getElementsByTagName("fits").item(0);
            if (root != null)
                readXMLData(fitsModel, hvSB, root);
            root = doc.getDocumentElement().getElementsByTagName("helioviewer").item(0);
            if (root != null)
                readXMLData(fitsModel, hvSB, root);
            hvArea.setText(hvSB.toString().trim());
            hvArea.setCaretPosition(0);

            String outFileName = JHVDirectory.EXPORTS.getPath() + m.getFullName().replace(' ', '_') + "__" + TimeUtils.formatFilename(m.getViewpoint().time.milli) + ".fits.xml";
            exportFitsButton.setEnabled(true);
            exportFitsButton.addActionListener(e -> {
                try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outFileName), StandardCharsets.UTF_8)) {
                    writer.write(xml, 0, xml.length());
                } catch (Exception ex) {
                    Log.error("Failed to write XML: " + ex);
                    return; // try with resources
                }
                JHVGlobals.displayNotification(outFileName);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String lastNodeSeen;

    private void readXMLData(DefaultTableModel fitsModel, StringBuilder hvSB, Node node) {
        // get element name and value
        String nodeName = node.getNodeName();
        String nodeValue = getElementValue(node);

        switch (nodeName) {
            case "fits":
            case "helioviewer":
                lastNodeSeen = nodeName;
                break;
            default:
                if ("fits".equals(lastNodeSeen))
                    fitsModel.addRow(new Object[]{nodeName, nodeValue});
                else
                    hvSB.append(nodeName).append(": ").append(nodeValue).append('\n');
                break;
        }

        // write the child nodes recursively
        NodeList children = node.getChildNodes();
        int len = children.getLength();
        for (int i = 0; i < len; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                readXMLData(fitsModel, hvSB, child);
            }
        }
    }

    /**
     * A method that gets the value of a node element.
     * <p>
     * If the node itself has children and no text value, an empty string is
     * returned. This is maybe an overkill for our purposes now, but takes into
     * account the possibility of nested tags.
     *
     * @param elem Node to read
     * @return value of the node
     */
    private static String getElementValue(Node elem) {
        if (elem != null && elem.hasChildNodes()) {
            for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() == Node.TEXT_NODE) {
                    return child.getNodeValue();
                }
            }
        }
        return "";
    }

}
