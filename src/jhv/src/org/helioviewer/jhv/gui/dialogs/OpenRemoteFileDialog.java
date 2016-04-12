package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.text.BadLocationException;
import javax.swing.tree.TreePath;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.message.Message;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.DynamicModel;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;
import org.helioviewer.jhv.io.LoadURIDownloadTask;
import org.helioviewer.jhv.io.LoadURITask;

/**
 * Dialog that is used to open user defined JPIP images.
 *
 * @author Stephan Pagel
 * @author Andreas Hoelzl
 */
@SuppressWarnings("serial")
public class OpenRemoteFileDialog extends JDialog implements ShowableDialog, ActionListener {

    // whether the advanced or the normal options are currently displayed
    private boolean advancedOptions = false;
    private static JTextField inputAddress = new JTextField();
    private static JTextField imageAddress = new JTextField();
    private final JLabel secondLabel = new JLabel("Remote Image Path: ");
    private final JButton buttonOpen = new JButton(" Open ");
    private final JButton buttonCancel = new JButton(" Cancel ");
    private final JButton refresh = new JButton(" Connect ");
    private final JButton buttonShow = new JButton(" Advanced Options ");
    private static DynamicModel treeModel;
    private static JTree tree;
    private static String chosenFile = "/";
    private final JPanel connectPanel = new JPanel(new BorderLayout());
    private JScrollPane scrollPane = new JScrollPane();
    private static JCheckBox fromJPIP = new JCheckBox("Download From HTTP");

    /**
     * The constructor that sets the fields and the dialog.
     */
    public OpenRemoteFileDialog() {
        super(ImageViewerGui.getMainFrame(), "Open Remote File", true);

        try {
            if (treeModel == null) {
                treeModel = new DynamicModel(Settings.getSingletonInstance().getProperty("/"));
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        MouseListener mouseListener;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        // the input text fields
        JPanel northContainer = new JPanel(new BorderLayout(7, 7));

        JPanel labelPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        JPanel fieldPanel = new JPanel(new GridLayout(0, 1, 8, 8));

        // the buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        inputAddress.setPreferredSize(new Dimension(250, 25));

        inputAddress.addActionListener(this);
        imageAddress.addActionListener(this);
        buttonOpen.addActionListener(this);
        buttonCancel.addActionListener(this);
        buttonShow.addActionListener(this);
        imageAddress.setPreferredSize(new Dimension(250, 25));
        buttonOpen.setPreferredSize(new Dimension(90, 25));
        refresh.setPreferredSize(new Dimension(90, 25));
        buttonCancel.setPreferredSize(new Dimension(90, 25));

        try {
            mouseListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int selRow = tree.getRowForLocation(e.getX(), e.getY());
                    TreePath treePath = tree.getPathForLocation(e.getX(), e.getY());
                    if (selRow != -1) {

                        String path = treePath.toString();
                        if (!path.contains(",")) {
                            buttonOpen.doClick();
                            return;
                        }

                        String parsed = path.substring(path.indexOf(","), path.lastIndexOf(']'));
                        parsed = parsed.replace(",", "");
                        parsed = parsed.replace(" ", "");

                        chosenFile = parsed;
                        if (e.getClickCount() >= 2) {
                            if (parsed.toLowerCase().endsWith(".jp2") || parsed.toLowerCase().endsWith(".jpx")) {
                                buttonOpen.doClick(100);
                            } else {
                            }
                        }
                    }
                }
            };

            if (tree == null) {
                tree = new JTree(treeModel);
                tree.addMouseListener(mouseListener);
            }

            fromJPIP.addActionListener(this);
            refresh.addActionListener(this);
            connectPanel.add(refresh, BorderLayout.EAST);
            connectPanel.add(fromJPIP, BorderLayout.WEST);

            scrollPane = new JScrollPane(tree);

            labelPanel.add(new JLabel("JPIP Server Address: "));

            labelPanel.add(secondLabel);

            fieldPanel.add(inputAddress);
            fieldPanel.add(imageAddress);

            buttonPanel.add(buttonShow);
            buttonPanel.add(buttonCancel);
            buttonPanel.add(buttonOpen);

            northContainer.add(labelPanel, BorderLayout.WEST);
            northContainer.add(fieldPanel, BorderLayout.CENTER);
            northContainer.add(connectPanel, BorderLayout.SOUTH);

            connectPanel.setVisible(false);
            panel.add(northContainer, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);
            scrollPane.setVisible(false);
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            fromJPIP.setSelected(false);
            inputAddress.setEnabled(true);
            imageAddress.setText(null);
            add(panel);
            this.setResizable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param arg
     *            the ActionEvent that occured what happens when the ActionEvent
     *            e is fired
     */
    @Override
    public void actionPerformed(ActionEvent arg) {
        /*
         * when the user wants to have more options the JTree is expanded and
         * the user can choose the files fromt he server he put into the
         * http-field
         */
        if (arg.getSource() == buttonShow) {
            show(arg);
        }
        /*
         * when the the refresh ("Connect") button is pressed a new JTree is
         * loaded
         */
        if (arg.getSource() == refresh) {
            refresh();
        }
        /*
         * the Cancel button is pressed
         */
        if (arg.getSource() == buttonCancel) {
            dispose();
        }
        /*
         * the selected file is streamed from the server
         */
        if (arg.getSource() == buttonOpen) {
            open();
        }
        /*
         * changes the download source from http to jpip or vice versa
         */
        if (arg.getSource() == fromJPIP) {
            changeSource();
        }
    }

    /**
     * changes the download source from http to jpip or vice versa
     *
     */
    private void changeSource() {
        if (fromJPIP.isSelected() == true) {
            inputAddress.setEnabled(false);
        } else {
            inputAddress.setEnabled(true);
        }
    }

    /**
     * when the user wants to have more options the JTree is expanded and the
     * user can choose the files fromt he server he put into the http-field
     *
     * @param arg
     *            the actionEvent that has occured
     *
     */
    private void show(ActionEvent arg) {
        this.setSize(this.getPreferredSize());
        if (advancedOptions == true) {
            connectPanel.setVisible(false);
            secondLabel.setText("Remote Image Path:");
            imageAddress.setText("");
            buttonShow.setText(" Advanced Options");
            inputAddress.setText(Settings.getSingletonInstance().getProperty("default.remote.path"));
            inputAddress.setEnabled(true);
            inputAddress.setEnabled(true);
            scrollPane.setVisible(false);

            this.setSize(this.getPreferredSize());
        } else {
            fromJPIP.setSelected(false);
            connectPanel.setVisible(true);
            secondLabel.setText("Concurrent HTTP Server:   ");

            imageAddress.setText(Settings.getSingletonInstance().getProperty("default.httpRemote.path"));
            inputAddress.setText(Settings.getSingletonInstance().getProperty("default.remote.path"));
            buttonShow.setText(" Basic Options");
            scrollPane.setVisible(true);
            inputAddress.setEnabled(true);
            this.setSize(this.getPreferredSize());
        }
        advancedOptions = !advancedOptions;
    }

    /**
     * When the refresh ("Connect") button is pressed a new JTree is loaded
     */
    private void refresh() {
        String http = imageAddress.getText();
        if (!http.endsWith("/"))
            http = http + "/";
        if (http.startsWith(" ")) {
            http = http.substring(1);
        }
        imageAddress.setText(http);

        /*
         * if the JPIP server and the HTTP Server are concurrent
         */
        try {
            URI urlHttpServer = new URI(imageAddress.getText());
            URI urlJpipServer = new URI(inputAddress.getText());

            if (urlHttpServer.getHost() == null) {
                Message.err("Invalid HTTP Server Address", "", false);
                return;
            }

            if (urlJpipServer.getHost() == null && fromJPIP.isSelected() == false) {
                Message.err("Invalid JPIP Server Address", "", false);
                return;
            }

            if (advancedOptions && urlHttpServer.getHost() != null && urlJpipServer.getHost() != null && !urlHttpServer.getHost().equals(urlJpipServer.getHost())) {
                Message.err("JPIP and HTTP address do not fit.", "", false);
                return;
            }
        } catch (URISyntaxException e) {
            Message.err("Invalid server address.", "", false);
            return;
        }

        try {
            String text = imageAddress.getText();
            if (!text.endsWith("/")) {
                text = text + "/";
            }

            treeModel = new DynamicModel(imageAddress.getText());
            tree.setModel(treeModel);

            tree.getParent().setSize(tree.getParent().getPreferredSize());

            Settings.getSingletonInstance().setProperty("default.httpRemote.path", text /*imageAddress.getText()*/);
            Settings.getSingletonInstance().setProperty("default.remote.path", text /*inputAddress.getText()*/);
            Settings.getSingletonInstance().save();
            tree.getParent().getParent().repaint();
        } catch (BadLocationException i) {
            Message.err("No .jp2 or .jpx on the server.", "", false);
        } catch (IOException i) {
            Message.err("The requested URL was not found or you have no access to it.", "", false);
        }
    }

    /**
     * downloads the selected file via http, stores it in the remote folder of
     * JHelioViewer and loads it locally from there
     */
    private void downloadFromHTTP() {
        if (tree.getLastSelectedPathComponent() != null) {
            if (!tree.getModel().isLeaf(tree.getLastSelectedPathComponent())) {
                tree.expandPath(tree.getSelectionPath());
                return;
            }
        }

        String srv = Settings.getSingletonInstance().getProperty("default.httpRemote.path");
        srv = srv.trim();
        if (srv.endsWith("/"))
            srv = srv.substring(0, srv.length() - 1);
        imageAddress.setText(srv);

        String img = chosenFile;
        img = img.trim();
        if (!img.startsWith("/"))
            img = "/" + img;

        setVisible(false);

        try {
            URI uri = new URI(srv + img);
            LoadURIDownloadTask uriTask = new LoadURIDownloadTask(uri, uri);
            JHVGlobals.getExecutorService().execute(uriTask);
            dispose();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * The selected file is streamed from the server.
     */
    private void open() {
        if (tree.getLastSelectedPathComponent() != null) {
            if (!tree.getModel().isLeaf(tree.getLastSelectedPathComponent())) {
                tree.expandPath(tree.getSelectionPath());
                return;
            }
        }
        if (fromJPIP.isSelected() == true) {
            downloadFromHTTP();
            return;
        }

        String srv = inputAddress.getText();
        if (advancedOptions) {
            srv = Settings.getSingletonInstance().getProperty("default.remote.path");
        }
        srv = srv.trim();

        if (srv.endsWith("/"))
            srv = srv.substring(0, srv.length() - 1);

        inputAddress.setText(srv);
        String img = "";

        if (advancedOptions == true) {
            img = chosenFile;
        } else {
            img = imageAddress.getText();
        }
        img = img.trim();
        if (!img.startsWith("/"))
            img = "/" + img;

        String httpPath;

        if (advancedOptions) {
            httpPath = Settings.getSingletonInstance().getProperty("default.httpRemote.path") + img;
        } else {
            httpPath = srv + img;
        }

        try {
            LoadURITask uriTask = new LoadURITask(new URI(srv + img), new URI(httpPath));
            JHVGlobals.getExecutorService().execute(uriTask);
            if (advancedOptions == false) {
                Settings.getSingletonInstance().setProperty("default.remote.path", inputAddress.getText());
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            setVisible(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showDialog() {
        pack();
        setSize(getPreferredSize());

        inputAddress.setText(Settings.getSingletonInstance().getProperty("default.remote.path"));

        setLocationRelativeTo(ImageViewerGui.getMainFrame());
        setVisible(true);
    }

    @Override
    public void init() {
    }

}
