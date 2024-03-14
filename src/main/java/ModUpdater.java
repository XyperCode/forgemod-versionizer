import com.formdev.flatlaf.FlatDarkLaf;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.*;

public class ModUpdater extends JFrame implements KeyListener {
    private JTextField versionTxt;
    private File inputFile;
    private File outputFile;
    private JButton startButton;

    public ModUpdater() throws HeadlessException {
        super("Forge Mod Versionizer");

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(500, 165);

        JPanel contentPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        this.buildContent(contentPane);
        this.buildButtons(contentPane);

        this.setContentPane(contentPane);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);

        this.validateVersion();
    }

    private void validateVersion() {
        this.startButton.setEnabled(!versionTxt.getText().isEmpty());
    }

    public static void main(String[] args) throws InterruptedException, InvocationTargetException {
        FlatDarkLaf.setup();

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new ModUpdater();
            frame.setVisible(true);
        });
    }

    private void buildButtons(Container frame) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        this.startButton = new JButton("Create Updated File");
        this.startButton.addActionListener(e -> updateMod(versionTxt.getText()));
        panel.add(this.startButton);
        panel.setVisible(true);
        frame.add(panel);
    }

    private void buildContent(Container frame) {
        //Create and populate the panel.
        GridLayout layout = new GridLayout(3, 2, 5, 5);
        JPanel panel = new JPanel(layout);
        JLabel versionLbl = new JLabel("Minimum forge version:", JLabel.TRAILING);
        panel.add(versionLbl);

        this.versionTxt = new JTextField();
        versionLbl.setLabelFor(versionTxt);
        this.versionTxt.addKeyListener(this);
        panel.add(this.versionTxt);

        JLabel inputFileLbl = new JLabel("Input file:", JLabel.TRAILING);
        inputFileLbl.setToolTipText("Select the mod that you want to change the forge version for.");
        panel.add(inputFileLbl);

        JButton inputFileBtn = createInputButton();
        panel.add(inputFileBtn);

        JLabel outputFileLbl = new JLabel("Output file:", JLabel.TRAILING);
        outputFileLbl.setToolTipText("Select where you want to save the new mod.");
        panel.add(outputFileLbl);

        JButton outputFileBtn = createOutputButton();
        panel.add(outputFileBtn);

        //Add the panel to the frame.
        frame.add(panel);
    }

    private JButton createOutputButton() {
        JButton outputFileBtn = new JButton("Select File");
        outputFileBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                this.setOutputFile(selectedFile);

                outputFileBtn.setText("File selected: " + selectedFile.getName());
            }
        });
        outputFileBtn.setMaximumSize(new Dimension(200, 30));
        outputFileBtn.setPreferredSize(new Dimension(200, 30));
        return outputFileBtn;
    }

    private JButton createInputButton() {
        JButton inputFileBtn = new JButton("Select File");
        inputFileBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                this.setInputFile(selectedFile);

                inputFileBtn.setText("File selected: " + selectedFile.getName());
            }
        });
        inputFileBtn.setMaximumSize(new Dimension(240, 15));
        inputFileBtn.setPreferredSize(new Dimension(240, 15));
        return inputFileBtn;
    }

    private void setInputFile(File selectedFile) {
        inputFile = selectedFile;
    }

    private void setOutputFile(File selectedFile) {
        outputFile = selectedFile;
    }

    @SuppressWarnings("unchecked")
    private void updateMod(String newVersion) {
        File inputFile = this.getInputFile();
        File outputFile = this.getOutputFile();

        try (JarFile jarFile = new JarFile(inputFile);
             JarOutputStream tempJar = new JarOutputStream(new FileOutputStream(outputFile))) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            // Iterate over the entries in the original JAR file
            for (JarEntry entry : Collections.list(jarFile.entries())) {
                try (InputStream entryStream = jarFile.getInputStream(entry)) {
                    // If the entry is the mod's dependency file, modify it
                    if (entry.getName().equals("META-INF/mods.toml")) {
                        Toml toml = new Toml().read(entryStream);
                        Map<String, Object> tomlMap = toml.toMap();
                        Map<String, Object> dependenciesData = (Map<String, Object>) tomlMap.get("dependencies");
                        Set<String> strings = dependenciesData.keySet();
                        for (String string : strings) {
                            var modData = (List<Map<String, Object>>) dependenciesData.get(string);
                            for (Map<String, Object> mod : modData) {
                                String forgeData = (String) mod.get("modId");
                                if (forgeData.equals("forge")) {
                                    mod.put("versionRange", "[" + newVersion + ",)");
                                }
                            }
                        }

                        TomlWriter tomlWriter = new TomlWriter();
                        String dependencies = tomlWriter.write(tomlMap);

                        // Add the modified entry to the new JAR file
                        tempJar.putNextEntry(new JarEntry(entry.getName()));
                        tempJar.write(dependencies.getBytes());
                    } else {
                        // Add the original entry to the new JAR file
                        tempJar.putNextEntry(entry);
                        while ((bytesRead = entryStream.read(buffer)) != -1) {
                            tempJar.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "IO Exception: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(null, "New mod file created successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    public File getInputFile() {
        return inputFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        this.validateVersion();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        this.validateVersion();
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
