package sparqlguiwrapper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.SystemUtils;

public class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private QueryManager qm;
    private String ontologyFileName = null;
    private int port = 8080;
    private Server server = null;
    private String url = "about:blank";
    private final String configFileName = System.getProperty("user.home")
            + "/.config/sparql-gui-wrapper/config.json".replace("/", System.getProperty("file.separator"));

    private JTextField ontologyField;

    public MainWindow() {
        setResizable(false);
        setAlwaysOnTop(true);
        try {
            if (SystemUtils.IS_OS_LINUX)
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            else
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
        }

        setTitle("SPARQL GUI Wrapper");
        setSize(896, 132);

        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);

        JLabel ontologyLabel = new JLabel("<html>Inferred OWL ontology<br/>(RDF/XML format)</html>");
        ontologyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        ontologyLabel.setBounds(12, 58, 138, 34);
        ontologyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ontologyLabel.setFocusable(false);
        ontologyLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        getContentPane().add(ontologyLabel);

        ontologyField = new JTextField();
        ontologyField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ontologyField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectOntology();
            }
        });
        ontologyField.setEditable(false);
        ontologyField.setBackground(Color.WHITE);
        ontologyField.setBounds(167, 58, 620, 34);
        getContentPane().add(ontologyField);
        ontologyField.setColumns(10);

        JButton ontologyChooseButton = new JButton("Choose...");
        ontologyChooseButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        ontologyChooseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectOntology();
            }
        });
        ontologyChooseButton.setBounds(789, 58, 93, 34);
        getContentPane().add(ontologyChooseButton);

        JSpinner portField = new JSpinner();
        portField.setFont(new Font("SansSerif", Font.BOLD, 12));
        portField.setValue(port);
        ((SpinnerNumberModel) portField.getModel()).setMinimum(1025);
        ((SpinnerNumberModel) portField.getModel()).setMaximum(65535);
        NumberEditor ne_portField = new JSpinner.NumberEditor(portField, "#");
        portField.setEditor(ne_portField);
        portField.setBounds(80, 12, 70, 34);
        portField.getEditor().getComponent(0).setBackground(Color.WHITE);
        portField.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                port = (int) portField.getValue();
            }
        });
        getContentPane().add(portField);

        JLabel portLabel = new JLabel("<html>Port</html>");
        portLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        portLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        portLabel.setFocusable(false);
        portLabel.setAlignmentX(0.5f);
        portLabel.setBounds(12, 12, 52, 34);
        getContentPane().add(portLabel);

        JLabel statusLabel = new JLabel("<html></html>");
        statusLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                if (server == null)
                    return;
                try {
                    Desktop.getDesktop().browse(new URI(url));
                    setState(JFrame.ICONIFIED);
                } catch (IOException | URISyntaxException e) {
                    JOptionPane.showMessageDialog(null, "Go to " + url, "Open your browser",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        statusLabel.setBorder(null);
        statusLabel.setBounds(266, 12, 521, 34);
        getContentPane().add(statusLabel);

        JButton btnStart = new JButton("Start");
        btnStart.setVisible(false);
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                btnStart.setEnabled(false);
                portField.setEnabled(false);
                if (server == null) {
                    try {
                        server = new Server(port, qm);
                    } catch (IOException e) {
                        showErrorDialog(e.getMessage());
                        portField.setEnabled(true);
                    }
                    btnStart.setText("Stop");
                    url = "http://localhost:" + port + "/";
                    statusLabel.setText("<html>Open the following URL in your browser:<br/><a href=\"" + url + "\">"
                            + url + "</a></html>");
                    statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    server.stop();
                    server = null;
                    portField.setEnabled(true);
                    statusLabel.setText("");
                    btnStart.setText("Start");
                    statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
                btnStart.setEnabled(true);
            }
        });
        btnStart.setBounds(167, 12, 93, 34);

        getContentPane().add(btnStart);

        JButton btnrefreshontology = new JButton("<html>Refresh<br/>ontology</html>");
        btnrefreshontology.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                btnrefreshontology.setEnabled(false);
                refreshOntology();
                btnrefreshontology.setEnabled(true);
            }
        });
        btnrefreshontology.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnrefreshontology.setBounds(789, 12, 93, 34);
        getContentPane().add(btnrefreshontology);

        setIconImage(new ImageIcon(MainWindow.class.getResource("/static/icon.png")).getImage());

        init();
        portField.setValue(port);
        btnStart.setVisible(true);

    }

    private void init() {
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent arg0) {
                writeConfigFile();
            }
        });
        qm = new QueryManager();
        readConfigFile();
        setOntologyFileName(ontologyFileName);
    }

    private void selectOntology() {
        JFileChooser chooser = new JFileChooser();
        if (ontologyFileName != null)
            chooser.setCurrentDirectory(new File(ontologyFileName).getParentFile());
        FileNameExtensionFilter filter = new FileNameExtensionFilter("OWL Ontologies (RDF/XML format)", "owl", "rdf",
                "xml");
        chooser.setFileFilter(filter);
        chooser.setAcceptAllFileFilterUsed(false);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            setOntologyFileName(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void setOntologyFileName(String ontologyFileName) {
        try {
            qm.setOntologyFileName(ontologyFileName);
            this.ontologyFileName = ontologyFileName;
        } catch (FileNotFoundException | IllegalArgumentException e) {
            showErrorDialog(e.getMessage());
            return;
        } finally {
            if (this.ontologyFileName == null)
                ontologyField.setText("[NOT SET]");
            else
                ontologyField.setText(this.ontologyFileName);
        }
    }

    private void refreshOntology() {
        try {
            qm.refreshOntology();
        } catch (FileNotFoundException e) {
            showErrorDialog(e.getMessage());
        }
    }

    private void showErrorDialog(String error) {
        JOptionPane.showMessageDialog(null, error, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {
            JFrame w = new MainWindow();
            w.setVisible(true);
        });
    }

    private void writeConfigFile() {
        File f;
        try {
            f = new File(configFileName);
            f.getParentFile().mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (ontologyFileName != null)
            b.add("ontologyFileName", ontologyFileName);
        else
            b.add("ontologyFileName", "");
        b.add("lastQuery", qm.getLastQuery());
        b.add("port", port);
        try (FileWriter file = new FileWriter(f)) {
            file.write(b.build().toString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readConfigFile() {
        try {
            String cfg = new String(Files.readAllBytes(Paths.get(configFileName)));
            JsonReader parser = Json.createReader(new StringReader(cfg));
            JsonObject o = parser.readObject();
            ontologyFileName = o.getString("ontologyFileName", "");
            if (ontologyFileName.isBlank())
                ontologyFileName = null;
            String lastQuery = o.getString("lastQuery", "");
            qm.setLastQuery(lastQuery);
            port = o.getInt("port", 8080);
        } catch (FileNotFoundException e) {
            // do nothing
        } catch (IOException | IllegalStateException e1) {
            e1.printStackTrace();
        }
    }

}
