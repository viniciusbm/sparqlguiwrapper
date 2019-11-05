package sparqlguiwrapper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.SystemColor;
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
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
    private JButton btnStart;
    private final Component verticalGlue = Box.createVerticalGlue();
    private JLabel statusLabel;

    public MainWindow() {
        if (SystemUtils.IS_OS_WINDOWS)
            getContentPane().setBackground(SystemColor.control);
        setMinimumSize(new Dimension(600, 150));
        try {
            if (SystemUtils.IS_OS_LINUX)
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            else
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) {
        }

        setTitle("SPARQL GUI Wrapper");
        setSize(800, 150);

        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setIconImage(new ImageIcon(MainWindow.class.getResource("/static/icon.png")).getImage());

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        verticalGlue.setBackground(SystemColor.window);
        getContentPane().add(verticalGlue);

        JPanel panel = new JPanel();
        getContentPane().add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        JLabel portLabel = new JLabel("<html>Port</html>");
        portLabel.setMaximumSize(new Dimension(80, 17));
        portLabel.setPreferredSize(new Dimension(50, 17));
        portLabel.setMinimumSize(new Dimension(50, 30));
        panel.add(portLabel);
        portLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        portLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        portLabel.setFocusable(false);
        portLabel.setAlignmentX(0.5f);

        Filler filler_2 = new Filler(new Dimension(15, 0), new Dimension(50, 0), new Dimension(Short.MAX_VALUE, 0));
        filler_2.setMaximumSize(new Dimension(50, 0));
        filler_2.setPreferredSize(new Dimension(20, 0));
        panel.add(filler_2);

        JSpinner portField = new JSpinner();
        portField.setMaximumSize(new Dimension(32767, 40));
        panel.add(portField);
        portField.setFont(new Font("SansSerif", Font.BOLD, 12));
        portField.setValue(port);
        ((SpinnerNumberModel) portField.getModel()).setMinimum(1025);
        ((SpinnerNumberModel) portField.getModel()).setMaximum(65535);
        NumberEditor ne_portField = new JSpinner.NumberEditor(portField, "#");
        portField.setEditor(ne_portField);
        portField.getEditor().getComponent(0).setBackground(Color.WHITE);
        portField.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                port = (int) portField.getValue();
            }
        });
        portField.setValue(port);

        Filler filler0 = new Box.Filler(new Dimension(20, 0), new Dimension(30, 0), new Dimension(Short.MAX_VALUE, 0));
        panel.add(filler0);

        btnStart = new JButton("Start");
        btnStart.setPreferredSize(new Dimension(63, 44));
        btnStart.setMaximumSize(new Dimension(63, 44));
        panel.add(btnStart);
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
        btnStart.setVisible(false);

        Filler filler = new Box.Filler(new Dimension(10, 0), new Dimension(30, 0), new Dimension(100, 0));
        panel.add(filler);

        statusLabel = new JLabel("<html></html>");
        statusLabel.setSize(new Dimension(40, 17));
        statusLabel.setMinimumSize(new Dimension(40, 17));
        panel.add(statusLabel);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
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

        Filler filler_1 = new Box.Filler(new Dimension(2, 0), new Dimension(20, 0), new Dimension(Short.MAX_VALUE, 0));
        panel.add(filler_1);

        JButton btnrefreshontology = new JButton("<html>Refresh<br/>ontology</html>");
        btnrefreshontology.setMaximumSize(new Dimension(300, 44));
        btnrefreshontology.setPreferredSize(new Dimension(90, 44));
        btnrefreshontology.setMinimumSize(new Dimension(88, 44));
        panel.add(btnrefreshontology);
        btnrefreshontology.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                btnrefreshontology.setEnabled(false);
                refreshOntology();
                btnrefreshontology.setEnabled(true);
            }
        });
        btnrefreshontology.setFont(new Font("SansSerif", Font.BOLD, 11));

        Filler filler_6 = new Filler(new Dimension(2, 0), new Dimension(5, 0), new Dimension(20, 0));
        panel.add(filler_6);

        Component verticalGlue_1 = Box.createVerticalGlue();
        verticalGlue_1.setBackground(SystemColor.window);
        getContentPane().add(verticalGlue_1);

        JPanel panel_1 = new JPanel();
        getContentPane().add(panel_1);
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));

        Filler filler_5 = new Filler(new Dimension(1, 0), new Dimension(2, 0), new Dimension(10, 0));
        panel_1.add(filler_5);

        JLabel ontologyLabel = new JLabel("<html>Inferred OWL ontology<br/><small>(RDF/XML format)</small></html>");
        ontologyLabel.setPreferredSize(new Dimension(150, 44));
        ontologyLabel.setMinimumSize(new Dimension(53, 40));
        ontologyLabel.setMaximumSize(new Dimension(300, 60));
        panel_1.add(ontologyLabel);
        ontologyLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        ontologyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ontologyLabel.setFocusable(false);
        ontologyLabel.setHorizontalAlignment(SwingConstants.TRAILING);

        Filler filler_3 = new Filler(new Dimension(15, 0), new Dimension(20, 0), new Dimension(40, 0));
        panel_1.add(filler_3);

        ontologyField = new JTextField();
        ontologyField.setMaximumSize(new Dimension(2147483647, 30));
        panel_1.add(ontologyField);
        ontologyField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ontologyField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectOntology();
            }
        });
        ontologyField.setEditable(false);
        ontologyField.setBackground(Color.WHITE);
        ontologyField.setColumns(10);

        Filler filler_4 = new Filler(new Dimension(2, 0), new Dimension(5, 0), new Dimension(40, 0));
        panel_1.add(filler_4);

        JButton ontologyChooseButton = new JButton("Choose...");
        ontologyChooseButton.setPreferredSize(new Dimension(90, 27));
        ontologyChooseButton.setMaximumSize(new Dimension(86, 44));
        panel_1.add(ontologyChooseButton);
        ontologyChooseButton.setFont(new Font("SansSerif", Font.BOLD, 12));

        Filler filler_7 = new Filler(new Dimension(2, 0), new Dimension(5, 0), new Dimension(20, 0));
        panel_1.add(filler_7);

        Component verticalGlue_2 = Box.createVerticalGlue();
        verticalGlue_2.setBackground(SystemColor.window);
        getContentPane().add(verticalGlue_2);
        ontologyChooseButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectOntology();
            }
        });

        init();

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
            btnStart.setVisible(this.ontologyFileName != null);
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
        JOptionPane.showMessageDialog(this, error, "Error", JOptionPane.ERROR_MESSAGE);
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
            if (ontologyFileName.isEmpty() || !new File(ontologyFileName).exists())
                ontologyFileName = null;
            String lastQuery = o.getString("lastQuery", "");
            qm.setLastQuery(lastQuery);
            port = o.getInt("port", 8080);
        } catch (FileNotFoundException | NoSuchFileException e) {
            // do nothing
        } catch (IOException | IllegalStateException e1) {
            e1.printStackTrace();
        }
    }
}
