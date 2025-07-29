package cmsp.tool.box.gui;

import cmsp.tool.box.Launcher;
import cmsp.tool.box.datamodel.DiscoveryProject;
import cmsp.tool.box.datamodel.DiscoveryResult;

import cmsp.tool.box.enums.DiscoveryPathwayCodes;
import cmsp.tool.box.enums.DiscoveryReportTypes;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static cmsp.tool.box.utils.FileLocatorUtils.locateRScriptPath;

public class DiscoveryReportsPdController {

    public CheckBox doseCheckBox;
    public CheckBox enablePathwayCheckBox;
    public ChoiceBox<String> experimentTypeBox;
    public CheckBox goCheckBox;
    public CheckBox gseaCheckBox;
    public CheckBox keggCheckBox;
    public CheckBox meshCheckBox;
    public Button nextButton2;
    public CheckBox oraCheckBox;
    public ChoiceBox<String> organismChoiceBox;
    public ProgressBar progressBar;
    public ChoiceBox<String> projectIdBox1;
    public ChoiceBox<String> projectIdBox2;
    public TextField projectNumberBox;
    public TableView<Map<Integer, String>> projectReportTable;
    public TableView<Map<Integer, String>> projectSampleTable;
    public TextField pvalueTextField;
    public CheckBox reactCheckBox;
    public Label updateLabel;
    public CheckBox wikiCheckBox;
    private List<DiscoveryProject> discoveryProjects;
    private DiscoveryReportsMainController mainController;
    private HashMap<String, HashMap<String, List<DiscoveryResult>>> projectMap;
    private String projectPath;
    private DiscoveryReportTypes projectType;
    private Path rscriptPath;
    private String selectedProject;

    /**
     * Disable all buttons associated with pathway analysis
     */
    private void disablePathwayAnalysis() {

        enablePathwayCheckBox.setDisable(true);
        organismChoiceBox.setDisable(true);
        oraCheckBox.setDisable(true);
        gseaCheckBox.setDisable(true);
        goCheckBox.setDisable(true);
        keggCheckBox.setDisable(true);
        reactCheckBox.setDisable(true);
        wikiCheckBox.setDisable(true);
        doseCheckBox.setDisable(true);
        meshCheckBox.setDisable(true);

        projectSampleTable.setDisable(false);
    }

    /**
     * Reset and display data associated with project study information in tableview object.
     * TODO - only shows for qual experiments presently, add quant experiments
     */
    private void displayProjectTable() {
        projectReportTable.getColumns().clear();
        projectReportTable.getItems().clear();
        projectReportTable.refresh();
        projectReportTable.getColumns().addAll(getReportTableColumns());
        projectReportTable.getItems().addAll(getReportTableData());
    }

    /**
     * Enable all buttons and boxes associated with pathway analysis.
     * Only used if experiment is quantitative
     */
    private void enablePathwayAnalysis() {

        // TODO - move organisms to enumerator class, add missing organism.
        // TODO - Evaluate if options need to be surrounded by quotes
        String[] organisms = {"\"Homo sapiens\"", "\"Mus musculus\"", "\"Rattus norvegicus\""};
        organismChoiceBox.getItems().clear();
        organismChoiceBox.getItems().addAll(organisms);

        enablePathwayCheckBox.setDisable(false);
        pvalueTextField.setDisable(true);
        organismChoiceBox.setDisable(true);
        oraCheckBox.setDisable(true);
        gseaCheckBox.setDisable(true);
        goCheckBox.setDisable(true);
        keggCheckBox.setDisable(true);
        reactCheckBox.setDisable(true);
        projectSampleTable.setDisable(true);
    }

    /**
     * Handle action for enable pathway checkbox
     */
    public void enablePathwayChoices() {
        boolean selection = !enablePathwayCheckBox.isSelected();
        organismChoiceBox.setDisable(selection);
        oraCheckBox.setDisable(selection);
        gseaCheckBox.setDisable(selection);
        goCheckBox.setDisable(selection);
        keggCheckBox.setDisable(selection);
        reactCheckBox.setDisable(selection);
        pvalueTextField.setDisable(selection);
    }

    /**
     * For each project report, format displayed table columns and data representations
     */
    private void formatProjectData() {
        // For each project in list
        for (Map.Entry<String, HashMap<String, List<DiscoveryResult>>> project : projectMap.entrySet()) {
            // For each report in project
            for (Map.Entry<String, List<DiscoveryResult>> result : project.getValue().entrySet()) {
                formatProteinReport(result.getValue(), project.getValue().get("Study"));
            }
        }
    }

    /**
     * Format discovery project report to restrict which columns are shown, column names, and data representations
     *
     * @param report Discovery project report, such as proteins, peptides, etc.
     * @param study Study meta data report
     */
    private void formatProteinReport(List<DiscoveryResult> report, List<DiscoveryResult> study) {

        // Representation of proteins adjusted for UniProt database entries
        Boolean uniprotDB = isUniprotDB(report);

        // For each row in report table
        for (DiscoveryResult row : report) {

            // Make new row (LinkedHashMap) for formatted results
            LinkedHashMap<String, String> oldRow = row.getMap();
            LinkedHashMap<String, String> newRow = new LinkedHashMap<>();

            // If protein is containment, indicate in protein name.
            String contaminant = (row.isPdContaminant()) ? "(Contaminant DB) " : "";

            // For each cell value in row
            for (Map.Entry<String, String> entry : oldRow.entrySet()) {

                // TODO - could this be changed to switch function?
                // Protein information (name, species, etc.) stored in Description column
                if (entry.getKey().equals("Description")) {
                    // If uniprotDB, parse description to get protein name, species, and sequence variant
                    // Description format: ProteinName OS=OrganismName OX=OrganismIdentifier[ GN=GeneName] PE=ProteinExistence SV=SequenceVersion
                    if (uniprotDB) {
                        String s = entry.getValue();
                        newRow.put("Protein Name", contaminant + s.split("OS=")[0].trim());
                        newRow.put("Species", s.substring(s.indexOf("OS=") + 3, s.indexOf("OX=")).trim());
                        newRow.put("Sequence Variant", entry.getValue().split("SV=")[1].trim());
                    } else {
                        newRow.put(entry.getKey(), contaminant + entry.getValue());
                    }

                    // Update PSMs column name to from workflow identifier (Alphabetic code) to sample name.
                    // Sample name to workflow ID stored in Study information report
                } else if (entry.getKey().contains("# PSMs (by Search Engine):")) {
                    String tmp = entry.getKey().replace("# PSMs (by Search Engine):", "").trim();
                    tmp = tmp.split(" ")[0];
                    String label = tmp.replaceAll("\\d", "");
                    String sampleName = study.stream().filter(e -> e.getPdLabel().equals(label)).map(DiscoveryResult::getPdSampleIdentifier).toList().get(0);
                    newRow.put("# PSMs: " + sampleName, entry.getValue());

                    // Update PSMs XCorr column name to from workflow identifier (Alphabetic code) to sample name.
                    // Sample name to workflow ID stored in Study information report
                } else if (entry.getKey().contains("PSM XCorr (by Search Engine):")) {
                    String tmp = entry.getKey().replace("PSM XCorr (by Search Engine):", "").trim();
                    tmp = tmp.split(" ")[0];
                    String label = tmp.replaceAll("\\d", "");
                    String sampleName = study.stream().filter(e -> e.getPdLabel().equals(label)).map(DiscoveryResult::getPdSampleIdentifier).toList().get(0);
                    newRow.put("PSM XCorr: " + sampleName, entry.getValue());

                    // Update PSMs Confidence column name to from workflow identifier (Alphabetic code) to sample name.
                    // Sample name to workflow ID stored in Study information report
                } else if (entry.getKey().contains("PSM Confidence (by Search Engine):")) {
                    String tmp = entry.getKey().replace("PSM Confidence (by Search Engine):", "").trim();
                    tmp = tmp.split(" ")[0];
                    String label = tmp.replaceAll("\\d", "");
                    String sampleName = study.stream().filter(e -> e.getPdLabel().equals(label)).map(DiscoveryResult::getPdSampleIdentifier).toList().get(0);
                    String value = (entry.getValue().equals("n/a")) ? "" : entry.getValue();
                    newRow.put("PSM Confidence: " + sampleName, value);

                    // Update column header name and format p-value in scientific format
                } else if (entry.getKey().contains("Abundance Ratio Adj. P-Value:")) {
                    if (!entry.getValue().isEmpty()) {
                        newRow.put(entry.getKey().replace("Abundance Ratio Adj. P-Value:", "Adj. P-Value:"), String.format("%1.3e", Double.parseDouble(entry.getValue())));
                    } else {
                        newRow.put(entry.getKey().replace("Abundance Ratio Adj. P-Value:", "Adj. P-Value:"), entry.getValue());
                    }

                    // Change column header name
                } else if (entry.getKey().contains("Abundance Ratio:")) {
                    newRow.put(entry.getKey().replace("Abundance Ratio:", "Fold-Change:"), entry.getValue());

                    // Format file name entry
                } else if (entry.getKey().equals("File Name")) {
                    File tmpFile = new File(entry.getValue());
                    newRow.put(entry.getKey(), tmpFile.getName());

                    // Remove indicated columns
                } else if (!removeProteinColumn(entry.getKey())) {
                    newRow.put(entry.getKey(), entry.getValue());
                }
            }

            row.setResultsMap(newRow);
        }
    }

    /**
     * Convert coverage percent to color RGB value
     *
     * @param coverage Protein coverage percentage
     */
    public String getCoverageColor(double coverage) {

        Color zeroCov = new Color(255, 255, 255);
        Color maxCov = new Color(115, 194, 251);

        float r = (float) (coverage * maxCov.getRed() + (1 - coverage) * zeroCov.getRed());
        float g = (float) (coverage * maxCov.getGreen() + (1 - coverage) * zeroCov.getGreen());
        float b = (float) (coverage * maxCov.getBlue() + (1 - coverage) * zeroCov.getBlue());

        return String.format("#%02x%02x%02x", Math.round(r), Math.round(g), Math.round(b));
    }

    /**
     * Create table column array for visualization in main discovery gui.
     *
     * @param tableData List of row of discovery project report
     */
    public ArrayList<TableColumn<Map<Integer, String>, String>> getFormattedColumns(List<DiscoveryResult> tableData) {

        ArrayList<TableColumn<Map<Integer, String>, String>> columns = new ArrayList<>();
        String[] keys = tableData.get(0).getColumnHeaders().toArray(new String[0]);

        // For each report key (column), make a new table column object.
        for (int i = 0; i < keys.length; i++) {

            String name = keys[i];
            Integer index = i;

            TableColumn<Map<Integer, String>, String> tableColumn = new TableColumn<>(name);
            tableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(index)));
            setColumnFormatting(tableColumn, name);
            columns.add(tableColumn);
        }
        return columns;
    }

    /**
     * Create table data for visualization in main discovery gui.
     *
     * @param tableData List of row of discovery project report
     */
    public ObservableList<Map<Integer, String>> getFormattedTableData(List<DiscoveryResult> tableData) {

        ObservableList<Map<Integer, String>> displayData = FXCollections.observableArrayList();

        // Get number of columns and key set.
        DiscoveryResult entry = tableData.get(0);
        int col = entry.size();
        String[] keys = entry.getColumnHeaders().toArray(new String[0]);

        // Add each data entry into mapped table row object.
        for (DiscoveryResult row : tableData) {
            // Table rows are hash maps.
            Map<Integer, String> dataRow = new HashMap<>();
            for (int i = 0; i < col; i++) {
                dataRow.put(i, row.getValue(keys[i]));
            }
            displayData.add(dataRow);
        }
        return displayData;
    }

    /**
     * From the input project directory, detect the number of ProteomeDiscoverer projects, and associated result export
     * to each project for report formatting
     *
     * @param directory User input directory that contain ProteomeDiscoverer project(s)
     */
    private void getPdProjectFiles(File directory) {

        // Detect proteome discoverer projects in directory
        File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdresult"));
        List<String> projects = new ArrayList<>();

        // Each detected proteome discoverer results is treated as independent project
        if (files != null) {
            for (File file : files) {
                projects.add(file.getName().replace(".pdResult", ""));
            }
        } else {
            //TODO - error handling
            // TODO - pop-up window that no projects detected, return to reports selection window
        }

        // For each project, find and categorize result exports
        if (!projects.isEmpty()) {
            for (String project : projects) {
                // Use underscore to account for duplicated file name increments produced by PD
                File[] exports = directory.listFiles((dir, name) -> name.contains(project + "_") & name.toLowerCase().endsWith(".txt"));
                if (exports == null) {
                    continue;
                }
                if (exports.length != 0) {
                    discoveryProjects.add(new DiscoveryProject(project, projectType, directory, exports));
                }
            }
        } else {
            //TODO - error handling
            // TODO - pop-up window that no projects detected, return to reports selection window
            // TODO - error handling if no exports found
        }
    }

    /**
     * Get global path of user workstation RScript.exe
     * If path specified in users preferences, use that. If no preference specified, use most recent version of R
     * TODO - error handling if no R path detected
     */
    private String getRPath() {
        if (rscriptPath == null) {
            return locateRScriptPath();
        } else {
            if (rscriptPath.toString().isEmpty()) {
                return locateRScriptPath();
            } else {
                return rscriptPath.toString();
            }
        }
    }

    /**
     * Create temporary file of software R file for script execution
     * TODO - evaluate if this is the best way to do this...
     */
    private String getRScriptPath() {
        // Create a temporary file to hold the script content
        Path tempScriptFile;
        try {
            // Relative path to pathway analysis Rscript
            InputStream script = getClass().getResourceAsStream("/cmsp/tool/box/R/pathwayAnalysis.R");
            tempScriptFile = Files.createTempFile("rscript_", ".R");
            Files.copy(script, tempScriptFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
            // TODO - more robust error handling
        }
        return tempScriptFile.toString();
    }

    /**
     * Update Rscript exe path
     */
    public void setRScriptPath(Path rscriptPath) {
        this.rscriptPath = rscriptPath;
    }

    /**
     * Make table column array for discovery project report exports tableview. Only includes Report Field categorization
     * and file name.
     */
    private ArrayList<TableColumn<Map<Integer, String>, String>> getReportTableColumns() {

        ArrayList<TableColumn<Map<Integer, String>, String>> columns = new ArrayList<>();
        String[] columnNames = {"Report Field", "File Name"};

        for (int i = 0; i < columnNames.length; i++) {
            int index = i;
            TableColumn<Map<Integer, String>, String> tableColumn = new TableColumn<>(columnNames[i]);
            tableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(index)));
            if (columnNames[i].equals("Report Field")) {
                tableColumn.setStyle("-fx-alignment: CENTER");
            }
            columns.add(tableColumn);
        }
        return columns;
    }

    /**
     * Make table data array for discovery project report exports tableview. Only includes Report Field categorization
     * and file name.
     */
    private ObservableList<Map<Integer, String>> getReportTableData() {

        ObservableList<Map<Integer, String>> tableData = FXCollections.observableArrayList();
        String projectID = projectIdBox1.getSelectionModel().getSelectedItem();
        DiscoveryProject project = discoveryProjects.stream().filter(e -> e.getProjectID().equals(projectID)).toList().get(0);

        // For each discovery project report export, show category and file name as row
        HashMap<String, File> exportFiles = project.getProjectFiles();
        for (Map.Entry<String, File> entry : exportFiles.entrySet()) {
            Map<Integer, String> dataRow = new HashMap<>();
            dataRow.put(0, entry.getKey());
            dataRow.put(1, entry.getValue().getName());
            tableData.add(dataRow);
        }
        return tableData;
    }

    /**
     * Make table columns for project meta data tableview.
     */
    private ArrayList<TableColumn<Map<Integer, String>, String>> getSampleTableColumns() {

        ArrayList<TableColumn<Map<Integer, String>, String>> columns = new ArrayList<>();
        String[] columnNames = {"PD Workflow ID", "File ID", "Sample Name"};

        for (int i = 0; i < columnNames.length; i++) {
            int index = i;
            TableColumn<Map<Integer, String>, String> tableColumn = new TableColumn<>(columnNames[i]);
            tableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(index)));
            if (!columnNames[i].equals("Sample Name")) {
                tableColumn.setStyle("-fx-alignment: CENTER");
            }
            columns.add(tableColumn);
        }
        return columns;
    }

    /**
     * Make table data for project meta data tableview.
     */
    private ObservableList<Map<Integer, String>> getSampleTableData() {

        ObservableList<Map<Integer, String>> tableData = FXCollections.observableArrayList();

        HashMap<String, List<DiscoveryResult>> project = projectMap.get(selectedProject);
        List<DiscoveryResult> studyInformation = project.get("Study");
        List<String> label = studyInformation.stream().map(DiscoveryResult::getPdLabel).toList();
        List<String> sample = studyInformation.stream().map(DiscoveryResult::getPdSample).toList();
        List<String> sampleID = studyInformation.stream().map(DiscoveryResult::getPdSampleIdentifier).toList();

        for (int i = 0; i < label.size(); i++) {
            Map<Integer, String> dataRow = new HashMap<>();
            dataRow.put(0, label.get(i));
            dataRow.put(1, sample.get(i));
            dataRow.put(2, sampleID.get(i));
            tableData.add(dataRow);
        }
        return tableData;
    }

    /**
     * Initialize controller.
     */
    public void initialize() {
        projectMap = new HashMap<>();
    }

    /**
     * Evaluate if Discovery project result utilized Uniprot database
     */
    private Boolean isUniprotDB(List<DiscoveryResult> report) {
        return !report.stream().map(DiscoveryResult::isUniprotDatabase).toList().contains(false);
    }

    /**
     * Evaluate if column text will be left justified
     *
     * @param key Column header string
     */
    public boolean leftJustifiedColumn(String key) {

        String[] patterns = {"Protein Name", "Description", "Biological Process", "Cellular Component", "Molecular Function", "Gene ID",
                "Pfam IDs", "Reactome Pathway Accessions", "Reactome Pathways", "WikiPathway Accessions", "WikiPathways", "File Name"};

        for (String pattern : patterns) {
            if (key.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles (first) next button click on Proteome Discoverer project selection window.
     */
    public void nextButton1Clicked(ActionEvent actionEvent) {

        // TODO - handling if multiple projects are to be processed
        // TODO - handle merging of multiple projects.
        List<DiscoveryProject> project = discoveryProjects.stream().filter(e -> e.getProjectID().equals(selectedProject)).toList();

        try {
            // Get second discovery processing window
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/cmsp/tool/box/DiscoveryReportsPdPage2.fxml"));
            Parent root = fxmlLoader.load();

            // Set project, path, type, and main controller
            DiscoveryReportsPdController controller = fxmlLoader.getController();
            controller.setProject(project);
            controller.setProjectDetails(projectPath, projectType);
            controller.setMainController(mainController);

            // Replace existing scene with new scene.
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/cmsp/tool/box/styleGuide.css")).toString());
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            // TODO - better error handling.
            e.printStackTrace();
        }
    }

    /**
     * Handles (second) next button click as part of Proteome Discoverer project processing window
     */
    public void nextButton2Clicked(ActionEvent actionEvent) {

        // Run pathway analysis or pass project data to main controller for tableview visualization
        if (enablePathwayCheckBox.isSelected()) {
            runPathwayAnalysis(actionEvent);
        } else {
            submitProjectData();
        }
    }

    /**
     * Handles (first) previous button click as part of Proteome Discovery project selection window
     */
    public void previousButton1Clicked(ActionEvent actionEvent) {

        try {
            // Get FXML of project type selection window
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("DiscoveryReportsSelection.fxml"));
            Parent root = fxmlLoader.load();

            // Update controller class with current database location.
            DiscoveryReportsSelectionController controller = fxmlLoader.getController();
            controller.setMainController(mainController);

            // Replace current stage with discovery selection stage
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setTitle("Project Set up...");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            // TODO - more robust error handling
            System.out.println(e.getMessage());
        }
    }

    /**
     * Handles (second) previous button click as part of Proteome Discoverer processing window
     */
    public void previousButton2Clicked(ActionEvent actionEvent) {

        try {
            // Get project report view window
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("DiscoveryReportsPdPage1.fxml"));
            Parent root = fxmlLoader.load();

            // Set controller and parameters
            DiscoveryReportsPdController controller = fxmlLoader.getController();
            controller.setProjectPath(projectType, projectPath);
            controller.setMainController(mainController);

            // Replace current scene
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/cmsp/tool/box/styleGuide.css")).toString());
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            // TODO - error handling
        }
    }

    /**
     * Read tab-delimited results export and format to list of discovery results.
     *
     * @param resultFile Discovery export result file
     */
    private List<DiscoveryResult> readResultTXT(File resultFile) {

        List<DiscoveryResult> resultTable = new ArrayList<>();

        // Create an instance of BufferedReader
        try (BufferedReader br = Files.newBufferedReader(resultFile.toPath(), StandardCharsets.UTF_8)) {

            String line = br.readLine();
            line = line.replace("\"", "");
            String[] header = line.split("\\t");
            line = br.readLine();

            // Loop through database line by line.
            while (line != null) {
                // TODO - evaluate a more efficient way to handle commas in comment field.
                // Parse strings.
                String[] attributes = line.split("\\t");
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = attributes[i].replace("\"", "");
                }

                // Make data entry object and add to list.
                DiscoveryResult row = new DiscoveryResult(header, attributes);
                resultTable.add(row);

                line = br.readLine();
            }

        } catch (IOException ioe) {
            // TODO - Error handling
            ioe.printStackTrace();
        }
        return resultTable;
    }

    /**
     * Determine if column should be removed from final report.
     *
     * @param key Column header string
     */
    private boolean removeProteinColumn(String key) {

        String[] removePatterns = {"Checked", "Protein FDR Confidence", "Confidence", "Contaminant", "Found in Sample",
                "Abundance Ratio Variability", "Abundances (Grouped):", "Abundance Ratio P-Value:"};

        for (String pattern : removePatterns) {
            if (key.startsWith(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Run pathway analysis using Rscript for selected project.
     * Uses pathway analysis parameters to create command to execute Rscript using command line interface
     */
    public void runPathwayAnalysis(ActionEvent event) {

        Scene scene = ((Node) event.getSource()).getScene();
        scene.setCursor(Cursor.WAIT); // Make cursor spin
        nextButton2.setDisable(true);

        // Reset progress bar
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0.0);

        // R and Script executable paths
        String rPath = getRPath();
        String scriptPath = getRScriptPath();

        // TODO - only processes first project, need to update if multiple projects analyzed
        String inputPath = this.discoveryProjects.get(0).getProjectFiles().get("Proteins").toString();
        String organism = organismChoiceBox.getValue();
        String pvalue = pvalueTextField.getText();

        // Filter pathway analysis codes to user selection
        List<DiscoveryPathwayCodes> pathwayCodes = Arrays.stream(DiscoveryPathwayCodes.values()).toList();

        // If ORA selected, make new project for ORA results
        if (oraCheckBox.isSelected()) {
            String project = selectedProject + "_" + "ORA-PathwayReport";
            HashMap<String, List<DiscoveryResult>> newMap = new HashMap<>();
            projectMap.put(project, newMap);
        } else {
            pathwayCodes = pathwayCodes.stream().filter(e -> !e.getType().equals("ORA")).toList();
        }

        // If GSEA selected, make new project for GSEA results
        if (gseaCheckBox.isSelected()) {
            String project = selectedProject + "_" + "GSEA-PathwayReport";
            HashMap<String, List<DiscoveryResult>> newMap = new HashMap<>();
            projectMap.put(project, newMap);
        } else {
            pathwayCodes = pathwayCodes.stream().filter(e -> !e.getType().equals("GSEA")).toList();
        }

        // Filter gene set parameters
        if (!goCheckBox.isSelected()) {
            pathwayCodes = pathwayCodes.stream().filter(e -> !e.getGeneSet().equals("GO")).toList();
        }
        if (!keggCheckBox.isSelected()) {
            pathwayCodes = pathwayCodes.stream().filter(e -> !e.getGeneSet().equals("KEGG")).toList();
        }
        if (!reactCheckBox.isSelected()) {
            pathwayCodes = pathwayCodes.stream().filter(e -> !e.getGeneSet().equals("REACTOME")).toList();
        }

        // Make a new background task
        List<DiscoveryPathwayCodes> codes = pathwayCodes;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {

                // For progress bar
                int steps = codes.size();
                int i = 1;

                // Loop through each pathway analysis code, submit for Rscript command line execution
                for (DiscoveryPathwayCodes code : codes) {
                    Path outputPath = null;
                    try {
                        outputPath = Files.createTempFile("pathwayAnalysis_", ".txt");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                        // TODO - more robust error handling
                    }

                    // TODO - can this collapsed into one line?
                    List<String> exec = new ArrayList<>();
                    exec.add(rPath);
                    exec.add(scriptPath);
                    exec.add(inputPath);
                    exec.add(outputPath.toString());
                    exec.add(organism);
                    exec.add(pvalue);
                    exec.add(code.getCode());

                    // Submit for processing, replace existing file if present
                    File output = new File(outputPath.toString());
                    try {
                        if (output.exists()) {
                            Files.deleteIfExists(output.toPath());
                        }

                        ProcessBuilder pb = new ProcessBuilder(exec);
                        pb.redirectErrorStream(true); //redirect STD ERR to STD OUT
                        Process process = pb.start();

                        // Print Rscript stream to standard out for debugging purposes
                        try (final BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                            String line = null;
                            while ((line = br.readLine()) != null) {
                                System.out.println("std-out-line: " + line);
                            }
                        }
                        int outputVal = process.waitFor();
                        System.out.format("outputVal: %d\n", outputVal);

                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                        // TODO - more robust error handling
                    }

                    // Read output from Rscript processing into project report object
                    if (output.exists()) {
                        String project = selectedProject + "_" + code.getType() + "-PathwayReport";
                        projectMap.get(project).put(code.toString(), readResultTXT(output));
                        try {
                            // Remove temporary files
                            Files.deleteIfExists(output.toPath());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                            // TODO - more robust error handling
                        }
                    }

                    // Progress bar update
                    int step = i;
                    Platform.runLater(() -> {
                        updateProgress(step, steps);
                    });
                    i = i + 1;
                }
                return null;
            }
        };

        // This method allows us to handle any Exceptions thrown by the task
        task.setOnFailed(wse -> wse.getSource().getException().printStackTrace());

        // If completed successfully, delete temporary script and submit project for main page visualization
        task.setOnSucceeded(wse -> {
            try {
                Files.deleteIfExists(Paths.get(scriptPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
                // TODO - more robust error handling
            }
            submitProjectData();
        });

        // Bind progress bar property and start background task
        progressBar.progressProperty().bind(task.progressProperty());
        Thread myThread = new Thread(task);
        myThread.start();
    }


    /**
     * Tableview cell formatting for main discovery gui
     *
     * @param tableColumn table column object
     * @param key report header name
     */
    private void setColumnFormatting(TableColumn<Map<Integer, String>, String> tableColumn, String key) {

        tableColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(final String item, boolean empty) {

                super.updateItem(item, empty);
                if (item != null) {
                    setText(item);
                    // Default cell fill, alternative fill color based on row even-odd index
                    if ((getIndex() % 2) == 0) {
                        setStyle("-fx-background-color: #FFFFFF");
                    } else {
                        setStyle("-fx-background-color: #F6F6F6");
                    }

                    // Highlight contaminant proteins, alternate fill based on row even-odd index
                    if (item.contains("Contaminant DB") || item.contains("(*)")) {
                        if ((getIndex() % 2) == 0) {
                            setStyle("-fx-background-color: #FFCDD4");
                        } else {
                            setStyle("-fx-background-color: #FFB6C1");
                        }
                    }

                    // Protein coverage fill gradient based on percentage
                    if (key.contains("Coverage")) {
                        if (item.isEmpty()) {
                            setStyle("-fx-background-color: #D9D9D9");
                        } else {
                            double coverage = Double.parseDouble(item) / 100.0;
                            setStyle("-fx-background-color: " + getCoverageColor(coverage));
                        }
                    }

                    // Quantitation variability fill coloring
                    if (key.contains("Abundance Ratio Variability") || key.contains("Abundances (Grouped) CV")) {
                        if (item.isEmpty()) {
                            setStyle("-fx-background-color: #D9D9D9");
                        } else {
                            double var = Double.parseDouble(item);
                            if (var > 50) {
                                setStyle("-fx-background-color: #E35B5C");
                            } else if (var > 40) {
                                setStyle("-fx-background-color: #EA8082");
                            } else if (var > 30) {
                                setStyle("-fx-background-color: #F2AAAB");
                            } else if (var > 20) {
                                setStyle("-fx-background-color: #FFFFCC");
                            }
                        }
                    }

                    // Number of PSMs fill coloring, alternate fill color based on row even-odd index
                    if (key.contains("# PSMs:")) {
                        if (item.isEmpty()) {
                            if ((getIndex() % 2) == 0) {
                                setStyle("-fx-background-color: #D9D9D9");
                            } else {
                                setStyle("-fx-background-color: #BFBFBF");
                            }
                        } else {
                            int psm = Integer.parseInt(item);
                            if (psm < 3) {
                                if ((getIndex() % 2) == 0) {
                                    setStyle("-fx-background-color: #FFD1D8");
                                } else {
                                    setStyle("-fx-background-color: #FFB6C1");
                                }
                            } else if (psm < 5) {
                                if ((getIndex() % 2) == 0) {
                                    setStyle("-fx-background-color: #FFF5C9");
                                } else {
                                    setStyle("-fx-background-color: #FFEB9C");
                                }
                            } else {
                                if ((getIndex() % 2) == 0) {
                                    setStyle("-fx-background-color: #D9F2D0");
                                } else {
                                    setStyle("-fx-background-color: #B4E5A2");
                                }
                            }
                        }
                    }

                    // Fold change color gradient
                    if (key.contains("Fold-Change:")) {
                        if (item.isEmpty()) {
                            setStyle("-fx-background-color: #D9D9D9");
                        } else {
                            double ratio = Double.parseDouble(item);
                            if (ratio < 0.1) {
                                setStyle("-fx-background-color: #4156A5; -fx-text-fill: yellow");
                            } else if (ratio < 0.13) {
                                setStyle("-fx-background-color: #6778B7");
                            } else if (ratio < 0.17) {
                                setStyle("-fx-background-color: #8D9AC9");
                            } else if (ratio < 0.25) {
                                setStyle("-fx-background-color: #B3BCDB");
                            } else if (ratio < 0.5) {
                                setStyle("-fx-background-color: #D9DEED");
                            } else if (ratio > 0.5 && ratio < 2.00) {
                                // Nothing
                            } else if (ratio < 4.0) {
                                setStyle("-fx-background-color: #FBD5D6");
                            } else if (ratio < 6.0) {
                                setStyle("-fx-background-color: #F7ABAC");
                            } else if (ratio < 8.0) {
                                setStyle("-fx-background-color: #F28082");
                            } else if (ratio < 10.0) {
                                setStyle("-fx-background-color: #EE5658");
                            } else if (ratio > 10.0) {
                                setStyle("-fx-background-color: #E92B2E; -fx-text-fill: yellow");
                            }
                        }
                    }

                    // P-value color gradient
                    if (key.contains("Adj. P-Value:")) {
                        if (item.isEmpty()) {
                            setStyle("-fx-background-color: #D9D9D9");
                        } else {
                            double pval = Double.parseDouble(item);
                            if (pval < 0.0001) {
                                setStyle("-fx-background-color: #31A354; -fx-text-fill: yellow");
                            } else if (pval < 0.001) {
                                setStyle("-fx-background-color: #78C679");
                            } else if (pval < 0.01) {
                                setStyle("-fx-background-color: #C2E699");
                            } else if (pval < 0.05) {
                                setStyle("-fx-background-color: #FFFFCC");
                            }
                        }
                    }

                    // Highlight cell with quan information, alternate based on row even-odd index
                    if (key.contains("Quan Info")) {
                        if (!item.isEmpty()) {
                            if ((getIndex() % 2) == 0) {
                                setStyle("-fx-background-color: #FFF5C9");
                            } else {
                                setStyle("-fx-background-color: #FFEB9C");
                            }
                        }
                    }

                    // Highlight row with multiple protein groups, alternate based on row even-odd index
                    if (key.contains("Protein Groups")) {
                        if (!item.isEmpty()) {
                            int num = Integer.parseInt(item);
                            if (num > 1) {
                                if ((getIndex() % 2) == 0) {
                                    setStyle("-fx-background-color: #FFF5C9");
                                } else {
                                    setStyle("-fx-background-color: #FFEB9C");
                                }
                            }
                        }
                    }

                    // Text alignment
                    if (!leftJustifiedColumn(key)) {
                        setStyle(getStyle() + "; -fx-alignment: CENTER");
                    }
                }
            }
        });
    }

    /**
     * Set main controller object. Used to pass objects between controllers.
     *
     * @param controller Main discovery gui controller
     */
    public void setMainController(DiscoveryReportsMainController controller) {
        mainController = controller;
    }

    /**
     * Set project list as part of project-report selection window.
     *
     * @param projects List of detected projects
     */
    public void setProject(List<DiscoveryProject> projects) {

        this.discoveryProjects = projects;

        ObservableList<String> projectChoices = FXCollections.observableArrayList();
        projectChoices.addAll(projects.stream().map(DiscoveryProject::getProjectID).toList());
        projectIdBox2.getItems().addAll(projectChoices);

        for (DiscoveryProject project : projects) {
            setProject(project);
        }
        projectIdBox2.setValue(projects.get(0).getProjectID());
    }

    /**
     * Use project files to define new Discovery Result object entry. Read exports and format each detected report
     * into Discovery Result hashmap object.
     *
     * @param project Detected discovery project (export paths)
     */
    public void setProject(DiscoveryProject project) {

        HashMap<String, List<DiscoveryResult>> projectResults = new HashMap<>();

        for (Map.Entry<String, File> result : project.getProjectFiles().entrySet()) {
            projectResults.put(result.getKey(), readResultTXT(result.getValue()));
        }
        projectMap.put(project.getProjectID(), projectResults);
    }

    /**
     * Set project list choices to choicebox. Set selected project as first project from list.
     */
    private void setProjectChoices() {

        ObservableList<String> projectChoices = FXCollections.observableArrayList();
        List<String> projectIDs = discoveryProjects.stream().map(DiscoveryProject::getProjectID).toList();

        selectedProject = projectIDs.get(0);
        projectChoices.addAll(projectIDs);
        projectIdBox1.getItems().addAll(projectChoices);
        projectIdBox1.setValue(selectedProject);
    }

    /**
     * Determine whether selected project is qualitative or quantitative. Update displayed items and choices respectively.
     *
     * @param projectPath Project name
     * @param type Type of discovery project
     */
    public void setProjectDetails(String projectPath, DiscoveryReportTypes type) {

        this.projectPath = projectPath;
        this.projectType = type;
        selectedProject = projectIdBox2.getSelectionModel().getSelectedItem();

        // Quantitative projects will have p-value column in headers
        HashMap<String, List<DiscoveryResult>> project = projectMap.get(selectedProject);
        List<DiscoveryResult> proteins = project.get("Proteins");
        Pattern p = Pattern.compile("P-Value");

        // Look for pattern in report headers
        boolean qual = true;
        for (String header : proteins.get(0).getColumnHeaders()) {
            if (p.matcher(header).find()) {
                qual = false;
                break;
            }
        }

        // Reset project study information tableview
        projectSampleTable.getColumns().clear();
        projectSampleTable.getItems().clear();
        projectSampleTable.refresh();

        // If a qualitative experiment, update project study information table, otherwise, enable pathway analysis features
        if (qual) {
            experimentTypeBox.setValue("Qual");
            disablePathwayAnalysis();
            projectSampleTable.getColumns().addAll(getSampleTableColumns());
            projectSampleTable.getItems().addAll(getSampleTableData());
        } else {
            experimentTypeBox.setValue("Quant");
            enablePathwayAnalysis();
        }
    }

    /**
     * From selected project path, find associated project files and update selections in project-report tableview.
     *
     * @param type Type of discovery project
     * @param projectPath Project directory or path
     */
    public void setProjectPath(DiscoveryReportTypes type, String projectPath) {

        this.discoveryProjects = new ArrayList<>();
        this.projectPath = projectPath;
        this.projectType = type;

        File directory = new File(projectPath);

        if (directory.isDirectory()) {
            getPdProjectFiles(directory);
        } else {
            //TODO - error handling
        }

        if (!discoveryProjects.isEmpty()) {
            projectNumberBox.setText(String.valueOf(discoveryProjects.size()));
            setProjectChoices();
            displayProjectTable();
        } else {
            //TODO - error handling
        }
    }

    /**
     * Submit selected project(s) to be visualized in main discovery gui tableview tabs.
     */
    public void submitProjectData() {

        // Format project reports to conform with style properties of final report
        formatProjectData();

        // Each project, and associated project reports, will be stored as LinkedHashMap of table columns and data.
        try {
            LinkedHashMap<String, LinkedHashMap<String, ArrayList<TableColumn<Map<Integer, String>, String>>>> projectTabColumns = new LinkedHashMap<>();
            LinkedHashMap<String, LinkedHashMap<String, ObservableList<Map<Integer, String>>>> projectTabData = new LinkedHashMap<>();

            // For each project in list
            for (Map.Entry<String, HashMap<String, List<DiscoveryResult>>> project : projectMap.entrySet()) {

                LinkedHashMap<String, ArrayList<TableColumn<Map<Integer, String>, String>>> projectReportColumns = new LinkedHashMap<>();
                LinkedHashMap<String, ObservableList<Map<Integer, String>>> projectReportData = new LinkedHashMap<>();

                // For each report associated with project
                for (Map.Entry<String, List<DiscoveryResult>> result : project.getValue().entrySet()) {

                    // Make table columns and data
                    ArrayList<TableColumn<Map<Integer, String>, String>> reportColumns = getFormattedColumns(result.getValue());
                    ObservableList<Map<Integer, String>> reportData = getFormattedTableData(result.getValue());
                    projectReportColumns.put(result.getKey(), reportColumns);
                    projectReportData.put(result.getKey(), reportData);
                }
                projectTabColumns.put(project.getKey(), projectReportColumns);
                projectTabData.put(project.getKey(), projectReportData);
            }

            // Set all relevant project information in main controller object for main discovery page visualization
            List<String> projects = new ArrayList<>(projectMap.keySet());
            mainController.setProjectController(this);
            mainController.setProjectChoiceBox(projects, selectedProject);
            mainController.setProjectTable(projectTabColumns, projectTabData);
            mainController.updateProjectTable();
            mainController.setProjectPath(projectPath, projectType);

            // Close discovery processing wizard window
            Stage stage = (Stage) projectIdBox2.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            // TODO - better error handling.
            e.printStackTrace();
        }
    }

    /**
     * Handle action event - change in selected project
     */
    public void updateProjectChoice(ActionEvent actionEvent) {

        String projectID = projectIdBox1.getSelectionModel().getSelectedItem();

        if (projectID == null) {
            return;
        }

        if (!Objects.equals(projectID, selectedProject)) {
            selectedProject = projectID;
            displayProjectTable();
        }
    }
}
