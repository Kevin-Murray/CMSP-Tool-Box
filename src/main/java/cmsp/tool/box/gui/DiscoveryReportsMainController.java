package cmsp.tool.box.gui;

import cmsp.tool.box.Launcher;
import cmsp.tool.box.enums.DiscoveryReportColors;
import cmsp.tool.box.enums.DiscoveryReportTabs;
import cmsp.tool.box.enums.DiscoveryReportTypes;
import cmsp.tool.box.enums.ErrorTypes;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * Controller class for main Discovery Report module page.
 */
public class DiscoveryReportsMainController {

    public Button editReportButton;
    public ProgressBar progressBar;
    public ChoiceBox<String> projectChoiceBox;
    public TabPane tabPane;

    private Path databasePath;
    private HashMap<DiscoveryReportColors, CellStyle> documentStyles;
    private Preferences prefs;
    private Object projectController;
    private String projectPath;
    private LinkedHashMap<String, LinkedHashMap<String, ArrayList<TableColumn<Map<Integer, String>, String>>>> projectTableColumns;
    private LinkedHashMap<String, LinkedHashMap<String, ObservableList<Map<Integer, String>>>> projectTableData;
    private DiscoveryReportTypes projectType;
    private Path rscriptPath;

    /**
     * Return list of indices that match input pattern string
     *
     * @param list    List of headers
     * @param pattern Header pattern to match
     */
    public static List<Integer> findAllIndexesWithPattern(List<String> list, String pattern) {
        List<Integer> indexes = new ArrayList<>();
        if (list != null && pattern != null) {
            Pattern compiledPattern = Pattern.compile(pattern);
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i) != null && compiledPattern.matcher(list.get(i)).find()) {
                    indexes.add(i);
                }
            }
        }
        return indexes;
    }

    /**
     * Evaluate if list of report headers contains column that matches input pattern.
     *
     * @param list    List of headers
     * @param pattern Pattern to match
     */
    public Boolean containsPattern(List<String> list, String pattern) {
        Pattern p = Pattern.compile(pattern);
        for (String s : list) {
            if (p.matcher(s).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create sheet row in Color legend tab
     *
     * @param sheet Color legend tab sheet
     * @param index Row index
     * @param color Cell fill color
     * @param label Row text label
     */
    private void createLegendRow(Sheet sheet, int index, DiscoveryReportColors color, String label) {

        Row row = sheet.createRow(index);
        row.setHeight((short) (25 * 20));
        Cell cell0 = row.createCell(0);
        cell0.setCellValue(label);
        cell0.setCellStyle(documentStyles.get(DiscoveryReportColors.defaultEvenCenter));
        Cell cell1 = row.createCell(1);
        cell1.setCellStyle(documentStyles.get(color));
        Cell cell2 = row.createCell(2);
        cell2.setCellStyle(documentStyles.get(color));
        sheet.addMergedRegion(new CellRangeAddress(index, index, 1, 2));
    }

    /**
     * Handle discovery report project edit action for ProteomeDiscoverer report type
     */
    private void editPDSettings() {

        try {
            // Get final discovery import gui page
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("/cmsp/tool/box/DiscoveryReportsPdPage2.fxml"));
            Parent root = fxmlLoader.load();
            fxmlLoader.setController(projectController);

            // Launch pop-up window.
            Stage stage = new Stage();
            stage.setTitle("Project Set up...");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.showAndWait();

        } catch (Exception e) {
            // TODO - more robust exception handling
            System.out.println(e.getMessage());
        }
    }

    /**
     * Handle action event to edit selected report processing parameters.
     */
    public void editReportButtonClick(ActionEvent actionEvent) {
        // TODO - handle other report types
        switch (projectType) {
            case PROTPD -> editPDSettings();
        }
    }

    /**
     * Handle action event to export current discovery report
     */
    public void exportButtonClick(ActionEvent event) {
        // Reset progress bar
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0.0);

        exportProject();
    }

    /**
     * Create color legend tab for selected workbook export
     */
    private void exportColorLegend(Workbook workbook) {

        Sheet sheet = workbook.createSheet("Color Legend");
        this.documentStyles = getDocumentStyles(workbook);

        // First row header
        Row row0 = sheet.createRow(0);
        row0.setHeight((short) (35 * 20));
        Cell cell00 = row0.createCell(0);
        cell00.setCellValue("Workbook Coloring Codes\n(Column) - Description");
        cell00.setCellStyle(documentStyles.get(DiscoveryReportColors.header));
        Cell cell01 = row0.createCell(1);
        cell01.setCellStyle(documentStyles.get(DiscoveryReportColors.header));
        Cell cell02 = row0.createCell(2);
        cell02.setCellStyle(documentStyles.get(DiscoveryReportColors.header));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        // Contaminant row highlighting legend
        Row row1 = sheet.createRow(1);
        row1.setHeight((short) (25 * 20));
        Cell cell10 = row1.createCell(0);
        cell10.setCellValue("(Accession:MW [kDa]) - Protein in Contaminant Database");
        cell10.setCellStyle(documentStyles.get(DiscoveryReportColors.defaultEvenCenter));
        Cell cell11 = row1.createCell(1);
        cell11.setCellStyle(documentStyles.get(DiscoveryReportColors.contaminantOddLeft));
        Cell cell12 = row1.createCell(2);
        cell12.setCellStyle(documentStyles.get(DiscoveryReportColors.contaminantOddLeft));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 2));

        // Protein coverage row legend
        Row row2 = sheet.createRow(2);
        row2.setHeight((short) (25 * 20));
        Cell cell20 = row2.createCell(0);
        cell20.setCellValue("(Coverage [%]) - Low to High Protein Coverage (Color Gradient)");
        cell20.setCellStyle(documentStyles.get(DiscoveryReportColors.defaultEvenCenter));
        Cell cell21 = row2.createCell(1);
        cell21.setCellStyle(documentStyles.get(DiscoveryReportColors.coverage1));
        Cell cell22 = row2.createCell(2);
        cell22.setCellStyle(documentStyles.get(DiscoveryReportColors.coverage7));

        // Add remaining color legend rows
        createLegendRow(sheet, 3, DiscoveryReportColors.valueGoodOdd, "(# PSMs:) - More than 5 PSMs detected");
        createLegendRow(sheet, 4, DiscoveryReportColors.valueMediumOdd, "(# PSMs:) - Less than 5 PSMs detected");
        createLegendRow(sheet, 5, DiscoveryReportColors.valueBadOdd, "(# PSMs:) - Less than 3 PSMs detected");
        createLegendRow(sheet, 6, DiscoveryReportColors.ratioHigh1, "(Fold-Change:) - Upregulation >= 2.0");
        createLegendRow(sheet, 7, DiscoveryReportColors.ratioHigh2, "(Fold-Change:) - Upregulation >= 4.0");
        createLegendRow(sheet, 8, DiscoveryReportColors.ratioHigh3, "(Fold-Change:) - Upregulation >= 6.0");
        createLegendRow(sheet, 9, DiscoveryReportColors.ratioHigh4, "(Fold-Change:) - Upregulation >= 8.0");
        createLegendRow(sheet, 10, DiscoveryReportColors.ratioHigh5, "(Fold-Change:) - Upregulation >= 10.0");
        createLegendRow(sheet, 11, DiscoveryReportColors.ratioLow1, "(Fold-Change:) - Downregulation <= 0.50");
        createLegendRow(sheet, 12, DiscoveryReportColors.ratioLow2, "(Fold-Change:) - Downregulation <= 0.25");
        createLegendRow(sheet, 13, DiscoveryReportColors.ratioLow3, "(Fold-Change:) - Downregulation <= 0.167");
        createLegendRow(sheet, 14, DiscoveryReportColors.ratioLow4, "(Fold-Change:) - Downregulation <= 0.125");
        createLegendRow(sheet, 15, DiscoveryReportColors.ratioLow5, "(Fold-Change:) - Downregulation <= 0.10");
        createLegendRow(sheet, 16, DiscoveryReportColors.pvalSig1, "(Adj. P-Value:) - Significant P-Value <= 0.05");
        createLegendRow(sheet, 17, DiscoveryReportColors.pvalSig2, "(Adj. P-Value:) - Significant P-Value <= 0.01");
        createLegendRow(sheet, 18, DiscoveryReportColors.pvalSig3, "(Adj. P-Value:) - Significant P-Value <= 0.001");
        createLegendRow(sheet, 19, DiscoveryReportColors.pvalSig4, "(Adj. P-Value:) - Significant P-Value <= 0.0001");
        createLegendRow(sheet, 20, DiscoveryReportColors.ratioHigh1, "(Abundances (Grouped) CV [%]) - Elevated Group CV% >= 20");
        createLegendRow(sheet, 21, DiscoveryReportColors.ratioHigh2, "(Abundances (Grouped) CV [%]) - Elevated Group CV% >= 40");
        createLegendRow(sheet, 22, DiscoveryReportColors.ratioHigh3, "(Abundances (Grouped) CV [%]) - Elevated Group CV% >= 60");
        createLegendRow(sheet, 23, DiscoveryReportColors.ratioHigh4, "(Abundances (Grouped) CV [%]) - Elevated Group CV% >= 80");
        createLegendRow(sheet, 24, DiscoveryReportColors.ratioHigh5, "(Abundances (Grouped) CV [%]) - Elevated Group CV% >= 100");
        createLegendRow(sheet, 25, DiscoveryReportColors.valueMediumOdd, "(Master Protein Accessions:Quan Info) - Peptide Assigned to Multiple Proteins Groups or Not Suitable for Quantitation");

        // Adjust column widths
        sheet.setColumnWidth(0, 110 * 256);
        sheet.setColumnWidth(1, 25 * 256);
        sheet.setColumnWidth(2, 25 * 256);
    }

    /**
     * Export selected discovery report table
     */
    public void exportProject() {

        Path outputDir = Paths.get(projectPath);

        // Total number of tasks
        int steps = tabPane.getTabs().size();

        // Export each tab as a background task. Update progress bar with each completed step.
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                File outputFile = outputDir.resolve(projectChoiceBox.getValue() + "-proteinReport.xlsx").toFile();
                Workbook workbook = new XSSFWorkbook();

                int i = 0;
                for (Tab tab : tabPane.getTabs()) {
                    exportReport(workbook, tab);
                    updateProgress(i, steps);
                    i++;
                }

                exportColorLegend(workbook);

                // Write to project reports to file
                try (FileOutputStream out = new FileOutputStream(outputFile)) {
                    workbook.write(out);
                    out.close();
                    workbook.close();
                } catch (IOException e) {
                    // TODO - more robust error handling
                    System.out.println(e.getMessage());
                }

                updateProgress(i++, steps);
                return null;
            }
        };

        // Handle any Exceptions thrown by the task
        task.setOnFailed(wse -> wse.getSource().getException().printStackTrace());

        // Task completed successfully.
        // TODO - Indicate to user that task is complete with a pop-up?
        task.setOnSucceeded(wse -> System.out.println("Done!"));

        // Bind UI values to the properties on the task
        progressBar.progressProperty().bind(task.progressProperty());

        // Start background thread
        new Thread(task).start();
    }

    /**
     * Create and export workbook sheet for tableview tab.
     */
    public void exportReport(Workbook workbook, Tab tab) {
        Sheet sheet = workbook.createSheet(tab.getText());
        sheet.createFreezePane(0, 1);
        TableView<Map<Integer, String>> tableView = (TableView<Map<Integer, String>>) tab.getContent();
        writeXLSXTable(workbook, sheet, tableView);
    }

    /**
     * Find index of headers list that match pattern
     * TODO - duplicate method? Compare to findAllIndexesWithPattern method
     *
     * @param list    List of headers
     * @param pattern Pattern to match
     * @return List of matching indices
     */
    public List<Integer> findIndex(List<String> list, String pattern) {

        Pattern p = Pattern.compile(pattern);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (p.matcher(list.get(i)).find()) {
                indices.add(i);
            }
        }
        return indices;
    }


    /**
     * Get index of boundary column. Boundary column will have a double border on their left side in the final exported
     * XLSX report.
     *
     * @param header List of headers
     */
    public List<Integer> getBoundaryColumnIndex(List<String> header) {

        List<Integer> boundaryIndex = new ArrayList<>();

        String[] patterns = {"^Coverage", "Fold-Change", "P-Value", "CV", "Biological Process", "Normalized", "# PSMs:", "PSM Confidence:", "PSM XCorr:",
                "Enriched Proteins", "Significant Outcomes", "Num. Significant Upregulated"};

        for (String pattern : patterns) {
            List<Integer> indices = findAllIndexesWithPattern(header, pattern);
            if (!indices.isEmpty()) {
                boundaryIndex.add(indices.get(0));
            }
        }
        return boundaryIndex;
    }

    /**
     * Create HashMap of cell styles for final report export to XLSX.
     *
     * @param workbook Report export
     */
    public HashMap<DiscoveryReportColors, CellStyle> getDocumentStyles(Workbook workbook) {

        HashMap<DiscoveryReportColors, CellStyle> documentStyles = new HashMap<>();

        for (DiscoveryReportColors color : DiscoveryReportColors.values()) {
            CellStyle style = workbook.createCellStyle();
            documentStyles.put(color, getStyle(workbook, style, color));
        }

        CellStyle style = workbook.createCellStyle();
        documentStyles.put(DiscoveryReportColors.header, getHeaderStyle(workbook, style, DiscoveryReportColors.header));
        CellStyle styleB = workbook.createCellStyle();
        documentStyles.put(DiscoveryReportColors.headerB, getHeaderStyle(workbook, styleB, DiscoveryReportColors.headerB));

        return documentStyles;
    }

    /**
     * Create cell style with desired header properties for the final report export to XLSX.
     *
     * @param workbook Report workbook
     * @param style    Cell style to add header properties.
     * @param color    Cell background fill color
     */
    public CellStyle getHeaderStyle(Workbook workbook, CellStyle style, DiscoveryReportColors color) {

        style = getStyle(workbook, style, color);
        style.setBorderBottom(BorderStyle.DOUBLE);
        style.setBottomBorderColor(IndexedColors.GREY_80_PERCENT.index);
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        style.setWrapText(true);
        style.setFont(headerFont);

        return style;
    }

    /**
     * Get user preference if they have used application before.
     */
    public void getPreferences() {

        // This will retrieve the node where the user preferences are stored.
        prefs = Preferences.userRoot().node(this.getClass().getName());
        String databasePath = prefs.get("TargetedReports.Database", null);

        if (databasePath != null) {
            this.databasePath = Paths.get(databasePath);
        }
    }

    /**
     * Create cell style that conforms to default properties and input background fill color.
     *
     * @param workbook Report workbook
     * @param style    Cell style to format
     * @param color    Background fill color
     */
    public CellStyle getStyle(Workbook workbook, CellStyle style, DiscoveryReportColors color) {

        style.setAlignment(color.getAlignment());
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(color.getFillColor());

        if (color.getFontColorCode() != null) {
            Font font = workbook.createFont();
            font.setColor(HSSFColor.HSSFColorPredefined.YELLOW.getIndex());
            style.setFont(font);
        }

        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.index);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.index);

        if (color.getBoundary()) {
            style.setBorderLeft(BorderStyle.DOUBLE);
            style.setLeftBorderColor(IndexedColors.GREY_80_PERCENT.index);
        }

        return style;
    }

    /**
     * Return stage to home page.
     *
     * @param event MouseEvent user clicked button
     * @throws IOException Unable to load home page
     */
    public void homeButtonClick(ActionEvent event) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/cmsp/tool/box/HomePage.fxml"));
        Parent root = fxmlLoader.load();

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/cmsp/tool/box/styleGuide.css")).toString());

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Initialize GUI.
     * Reads user preferences.
     */
    public void initialize() {
        getPreferences();
    }

    /**
     * Check if string is numeric
     *
     * @param str input string
     * @return Boolean true if number string
     */
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Determine if column cell text should be left justified.
     *
     * @param key Column header
     */
    public boolean leftJustifiedColumn(String key) {

        String[] patterns = {"Protein Name", "Description", "Biological Process", "Cellular Component", "Molecular Function", "Gene ID",
                "Pfam IDs", "Reactome Pathway Accessions", "Reactome Pathways", "WikiPathway Accessions", "WikiPathways", "File Name",
                "Core Enriched Proteins", "Upregulated Accessions", "Downregulated Accessions"};

        for (String pattern : patterns) {
            if (key.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handle application set-up menu.
     */
    @FXML
    protected void menuSetUpListener() {

        try {
            // Get window design for Set-up page.
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("SetUpPage.fxml"));
            Parent root = fxmlLoader.load();

            // Update controller class with current database location.
            SetUpPageController controller = fxmlLoader.<SetUpPageController>getController();
            controller.setDatabaseFolder(this.databasePath);
            controller.setRscriptPath(null);

            // Launch pop-up window.
            Stage stage = new Stage();
            stage.setTitle("CMSP Tool Box Set Up...");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.showAndWait();

            if (!Files.exists(controller.getDatabaseFolder())) {
                showErrorMessage(ErrorTypes.DATABASE);
            } else {
                // Update database location and set application defaults.
                this.databasePath = controller.getDatabaseFolder();
                this.rscriptPath = controller.getRscriptPath();
                setPreferences();
            }
        } catch (Exception e) {
            // TODO - better error handling.
            e.printStackTrace();
        }
    }

    /**
     * Handle action event for new report button click.
     * Launches Discovery Reports GUI wizard for user to select type of discovery experiment that are analyzing and
     * directory of associated files.
     */
    @FXML
    protected void newReportButtonClick(ActionEvent actionEvent) {

        try {
            // Get window design for Set-up page.
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("DiscoveryReportsSelection.fxml"));
            Parent root = fxmlLoader.load();

            // Update controller class with current database location.
            DiscoveryReportsSelectionController controller = fxmlLoader.getController();
            controller.setMainController(this);
            controller.setRScriptPath(this.rscriptPath);

            // Launch pop-up window.
            Stage stage = new Stage();
            stage.setTitle("Project Set up...");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.showAndWait();

        } catch (Exception e) {
            // TODO - better error handling.
            e.printStackTrace();
        }
    }

    /**
     * Set user database path preference.
     */
    public void setPreferences() {

        // This will define a node in which the preferences can be stored.
        prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.put("TargetedReports.Database", this.databasePath.toString());
    }

    /**
     * Populate project choice box with all processed projects for the Discovery GUI wizard. Set the selected project
     * as the first project from the processing.
     *
     * @param projects        List of Discovery Project names
     * @param selectedProject String of discovery project to show
     */
    public void setProjectChoiceBox(List<String> projects, String selectedProject) {
        ObservableList<String> projectIDs = FXCollections.observableArrayList();
        projectIDs.addAll(projects);
        projectChoiceBox.getItems().addAll(projectIDs);
        projectChoiceBox.setValue(selectedProject);
    }

    /**
     * Set variable controller object to controller used from the Discovery processing wizards gui, ProteomeDiscoverer
     * selection choice.
     */
    public void setProjectController(DiscoveryReportsPdController discoveryReportsPdController) {
        this.projectController = discoveryReportsPdController;
    }

    /**
     * Set path of selected discovery project and type of project.
     */
    public void setProjectPath(String projectPath, DiscoveryReportTypes type) {
        this.projectPath = projectPath;
        this.projectType = type;
    }

    /**
     * Set data (LinkedHashMap) to be visualized in main discovery reports tableview object. May include data for one
     * or more tabs to be viewed in GUI.
     *
     * @param projectTableColumns Contains column headers for each table tab
     * @param projectTableData    Contains table data for each table tab
     */
    public void setProjectTable(LinkedHashMap<String, LinkedHashMap<String, ArrayList<TableColumn<Map<Integer, String>, String>>>> projectTableColumns,
                                LinkedHashMap<String, LinkedHashMap<String, ObservableList<Map<Integer, String>>>> projectTableData) {
        this.projectTableColumns = projectTableColumns;
        this.projectTableData = projectTableData;
    }

    /**
     * Launches error window with input error message.
     */
    @FXML
    protected void showErrorMessage(ErrorTypes error) {

        try {
            // Get error page window design.
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("ErrorPage.fxml"));
            Parent root = fxmlLoader.load();

            // Initialize error window with message.
            ErrorPageController controller = fxmlLoader.getController();
            controller.setErrorMessage(error);

            // Launch pop-up window.
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.showAndWait();

        } catch (Exception e) {
            // TODO - better error handling.
            e.printStackTrace();
        }
    }

    /**
     * Launches error window with input error message.
     */
    @FXML
    protected void showErrorMessage(String string) {

        try {
            // Get error page window design.
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("ErrorPage.fxml"));
            Parent root = fxmlLoader.load();

            // Initialize error window with message.
            ErrorPageController controller = fxmlLoader.getController();
            controller.setErrorMessage(string);

            // Launch pop-up window.
            Stage stage = new Stage();
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setResizable(true);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.showAndWait();

        } catch (Exception e) {
            // TODO - better error handling.
            e.printStackTrace();
        }
    }

    /**
     * Update main discovery window tableview with currently selected project data. Create and populate multiple tabs
     * associated with each component of the data processing.
     */
    public void updateProjectTable() {

        if (projectTableColumns == null || projectTableData == null) {
            return;
        }

        // Clear existing results
        tabPane.getTabs().clear();
        editReportButton.setDisable(false);

        // Get data from selected projectID
        String projectID = projectChoiceBox.getSelectionModel().getSelectedItem();
        LinkedHashMap<String, ArrayList<TableColumn<Map<Integer, String>, String>>> projectColumn = projectTableColumns.get(projectID);
        LinkedHashMap<String, ObservableList<Map<Integer, String>>> projectData = projectTableData.get(projectID);

        // Create dummy list of tabs for tab order organization. Will drop unused entries later.
        List<Tab> tabList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tabList.add(null);
        }

        // For each report in selected project data, create tab and populate tableview with data.
        List<String> reportNames = new ArrayList<>(projectColumn.keySet());
        for (String report : reportNames) {

            // Get data
            ArrayList<TableColumn<Map<Integer, String>, String>> reportColumns = projectColumn.get(report);
            ObservableList<Map<Integer, String>> reportData = projectData.get(report);

            // Populate tableview
            TableView<Map<Integer, String>> reportTable = new TableView<>();
            reportTable.getColumns().addAll(reportColumns);
            reportTable.getItems().addAll(reportData);
            reportTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            // Create tab and add tableview
            DiscoveryReportTabs tabInfo = DiscoveryReportTabs.fromImportName(report);
            Tab reportTab = new Tab(tabInfo.getTabTitle());
            reportTab.setContent(reportTable);
            tabList.add(tabInfo.getIndex(), reportTab); // Tab index is for order of tabs
        }

        // Remove unused tab levels to achieve desired organization.
        tabList.removeAll(Collections.singleton(null));
        tabPane.getTabs().addAll(tabList);
    }

    /**
     * Write submitted table to Excel sheet.
     *
     * @param sheet     Specified sheet
     * @param tableView Results table
     * @param <T>       Ignore
     */
    private <T> void writeXLSXTable(Workbook workbook, Sheet sheet, TableView<T> tableView) {

        // Get all potential styles for the document
        HashMap<DiscoveryReportColors, CellStyle> documentStyles = getDocumentStyles(workbook);

        // Get column header names and compute number of characters per header name for automatic re-sizing
        List<String> headers = new ArrayList<>();
        List<Integer> headerSize = new ArrayList<>();
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            headers.add(tableView.getColumns().get(i).getText());
        }


        // Create header row and format boundary columns with appropriate borders.
        Row headerRow = sheet.createRow(0);
        headerRow.setHeight((short) -1);
        List<Integer> boundaryIndex = getBoundaryColumnIndex(headers);
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            TableColumn<T, ?> column = tableView.getColumns().get(i);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(column.getText());
            if (boundaryIndex.contains(i)) {
                cell.setCellStyle(documentStyles.get(DiscoveryReportColors.headerB));
            } else {
                cell.setCellStyle(documentStyles.get(DiscoveryReportColors.header));
            }
            headerSize.add(column.getText().length() + 2);
        }

        // List to track maximum number of characters in cell value for automatic resizing
        List<Integer> valueSize = new ArrayList<>();
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            valueSize.add(0);
        }

        // Populate data rows
        for (int i = 0; i < tableView.getItems().size(); i++) {
            Row dataRow = sheet.createRow(i + 1);
            for (int j = 0; j < tableView.getColumns().size(); j++) {
                TableColumn<T, ?> column = tableView.getColumns().get(j);
                String header = column.getText();
                Object cellValue = column.getCellData(i);
                Cell cell = dataRow.createCell(j);
                String value = (cellValue == null) ? null : cellValue.toString();

                if (value != null) {

                    // Update maximum number of characters for column - for automatic resizing
                    if (value.length() > valueSize.get(j)) {
                        valueSize.set(j, value.length() + 2);
                    }

                    // Dynamically adjust cell value if string or number
                    if (isNumeric(value)) {
                        cell.setCellValue(Double.parseDouble(value));
                    } else {
                        cell.setCellValue(value);
                    }

                    // If left justified column, adjust style
                    if (leftJustifiedColumn(header)) {
                        if (boundaryIndex.contains(j)) {
                            cell.setCellStyle(documentStyles.get(DiscoveryReportColors.defaultEvenLeftB));
                        } else {
                            cell.setCellStyle(documentStyles.get(DiscoveryReportColors.defaultEvenLeft));
                        }
                    } else {
                        if (boundaryIndex.contains(j)) {
                            cell.setCellStyle(documentStyles.get(DiscoveryReportColors.defaultOddCenterB));
                        } else {
                            cell.setCellStyle(documentStyles.get(DiscoveryReportColors.defaultOddCenter));
                        }
                    }
                }
            }
        }

        // Conditional formatting for rows containing contaminant proteins
        SheetConditionalFormatting sheetContam = sheet.getSheetConditionalFormatting();
        ConditionalFormattingRule contamEvenRowRule = sheetContam.createConditionalFormattingRule("AND(SEARCH(\"Contaminant DB\",$C2))");
        PatternFormatting contamEvenRowPattern = contamEvenRowRule.createPatternFormatting();
        contamEvenRowPattern.setFillBackgroundColor(DiscoveryReportColors.contaminantEvenCenter.getFillColor());
        contamEvenRowPattern.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
        CellRangeAddress[] contamRange = {new CellRangeAddress(1, sheet.getLastRowNum(), 0, 5)};
        sheetContam.addConditionalFormatting(contamRange, contamEvenRowRule);

        // Conditional formatting for coverage column - highlight shades of blue depending on coverage percent
        if (containsPattern(headers, "Coverage")) {

            List<Integer> columns = findIndex(headers, "Coverage");
            SheetConditionalFormatting sheetCov = sheet.getSheetConditionalFormatting();

            ConditionalFormattingRule covRowRule90 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "90");
            PatternFormatting covPattern90 = covRowRule90.createPatternFormatting();
            covPattern90.setFillBackgroundColor(DiscoveryReportColors.coverage9.getFillColor());
            covPattern90.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule covRowRule80 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "80");
            PatternFormatting covPattern80 = covRowRule80.createPatternFormatting();
            covPattern80.setFillBackgroundColor(DiscoveryReportColors.coverage8.getFillColor());
            covPattern80.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule covRowRule70 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "70");
            PatternFormatting covPattern70 = covRowRule70.createPatternFormatting();
            covPattern70.setFillBackgroundColor(DiscoveryReportColors.coverage7.getFillColor());
            covPattern70.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule covRowRule60 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "60");
            PatternFormatting covPattern60 = covRowRule60.createPatternFormatting();
            covPattern60.setFillBackgroundColor(DiscoveryReportColors.coverage6.getFillColor());
            covPattern60.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule covRowRule50 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "50");
            PatternFormatting covPattern50 = covRowRule50.createPatternFormatting();
            covPattern50.setFillBackgroundColor(DiscoveryReportColors.coverage5.getFillColor());
            covPattern50.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule covRowRule40 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "40");
            PatternFormatting covPattern40 = covRowRule40.createPatternFormatting();
            covPattern40.setFillBackgroundColor(DiscoveryReportColors.coverage4.getFillColor());
            covPattern40.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule covRowRule30 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "30");
            PatternFormatting covPattern30 = covRowRule30.createPatternFormatting();
            covPattern30.setFillBackgroundColor(DiscoveryReportColors.coverage3.getFillColor());
            covPattern30.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule covRowRule20 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "20");
            PatternFormatting covPattern20 = covRowRule20.createPatternFormatting();
            covPattern20.setFillBackgroundColor(DiscoveryReportColors.coverage2.getFillColor());
            covPattern20.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule covRowRule10 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "10");
            PatternFormatting covPattern10 = covRowRule10.createPatternFormatting();
            covPattern10.setFillBackgroundColor(DiscoveryReportColors.coverage1.getFillColor());
            covPattern10.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule covRowRule00 = sheetCov.createConditionalFormattingRule(ComparisonOperator.GT, "0");
            PatternFormatting covPattern00 = covRowRule00.createPatternFormatting();
            covPattern00.setFillBackgroundColor(DiscoveryReportColors.coverage0.getFillColor());
            covPattern00.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule[] covRules = {covRowRule90, covRowRule80, covRowRule70, covRowRule60, covRowRule50, covRowRule40, covRowRule30, covRowRule20, covRowRule10, covRowRule00};
            CellRangeAddress[] covRange = {new CellRangeAddress(1, sheet.getLastRowNum(), columns.get(0), columns.get(columns.size() - 1))};
            sheetCov.addConditionalFormatting(covRange, covRules);
        }

        // Conditional formatting for number of PSMs column - fill with red, yellow, or green depending on number of
        // observed PSMs
        if (containsPattern(headers, "# PSMs:")) {

            List<Integer> columns = findIndex(headers, "# PSMs:");
            SheetConditionalFormatting sheetPSM = sheet.getSheetConditionalFormatting();

            ConditionalFormattingRule psmRowRuleNull = sheetPSM.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"\"");
            PatternFormatting psmPatternNull = psmRowRuleNull.createPatternFormatting();
            psmPatternNull.setFillBackgroundColor(DiscoveryReportColors.missingEven.getFillColor());
            psmPatternNull.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule psmGoodRowRule = sheetPSM.createConditionalFormattingRule(ComparisonOperator.GT, "4");
            PatternFormatting psmGoodPattern = psmGoodRowRule.createPatternFormatting();
            psmGoodPattern.setFillBackgroundColor(DiscoveryReportColors.valueGoodOdd.getFillColor());
            psmGoodPattern.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule psmMediumRowRule = sheetPSM.createConditionalFormattingRule(ComparisonOperator.GT, "2");
            PatternFormatting psmMediumPattern = psmMediumRowRule.createPatternFormatting();
            psmMediumPattern.setFillBackgroundColor(DiscoveryReportColors.valueMediumOdd.getFillColor());
            psmMediumPattern.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule psmBadRowRule = sheetPSM.createConditionalFormattingRule(ComparisonOperator.GT, "0");
            PatternFormatting psmBadPattern = psmBadRowRule.createPatternFormatting();
            psmBadPattern.setFillBackgroundColor(DiscoveryReportColors.valueBadOdd.getFillColor());
            psmBadPattern.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule[] psmRules = {psmRowRuleNull, psmGoodRowRule, psmMediumRowRule, psmBadRowRule};
            CellRangeAddress[] psmRange = {new CellRangeAddress(1, sheet.getLastRowNum(), columns.get(0), columns.get(columns.size() - 1))};
            sheetPSM.addConditionalFormatting(psmRange, psmRules);

        }

        // Conditional formatting for p-value columns. Adjust fill color based on confidence in p-value
        if (containsPattern(headers, "P-Value")) {

            List<Integer> columns = findIndex(headers, "P-Value");
            SheetConditionalFormatting sheetPVal = sheet.getSheetConditionalFormatting();

            ConditionalFormattingRule pvalRowRuleNull = sheetPVal.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"\"");
            PatternFormatting ratioPatternNull = pvalRowRuleNull.createPatternFormatting();
            ratioPatternNull.setFillBackgroundColor(DiscoveryReportColors.missingEven.getFillColor());
            ratioPatternNull.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule pvalRowRule4 = sheetPVal.createConditionalFormattingRule(ComparisonOperator.LT, "0.0001");
            PatternFormatting pvalPattern4 = pvalRowRule4.createPatternFormatting();
            pvalPattern4.setFillBackgroundColor(DiscoveryReportColors.pvalSig4.getFillColor());
            pvalPattern4.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
            FontFormatting fontFmt = pvalRowRule4.createFontFormatting();
            fontFmt.setFontColorIndex(IndexedColors.YELLOW.index);

            ConditionalFormattingRule pvalRowRule3 = sheetPVal.createConditionalFormattingRule(ComparisonOperator.LT, "0.001");
            PatternFormatting pvalPattern3 = pvalRowRule3.createPatternFormatting();
            pvalPattern3.setFillBackgroundColor(DiscoveryReportColors.pvalSig3.getFillColor());
            pvalPattern3.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule pvalRowRule2 = sheetPVal.createConditionalFormattingRule(ComparisonOperator.LT, "0.01");
            PatternFormatting pvalPattern2 = pvalRowRule2.createPatternFormatting();
            pvalPattern2.setFillBackgroundColor(DiscoveryReportColors.pvalSig2.getFillColor());
            pvalPattern2.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule pvalRowRule1 = sheetPVal.createConditionalFormattingRule(ComparisonOperator.LT, "0.05");
            PatternFormatting pvalPattern1 = pvalRowRule1.createPatternFormatting();
            pvalPattern1.setFillBackgroundColor(DiscoveryReportColors.pvalSig2.getFillColor());
            pvalPattern1.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule[] pvalRules = {pvalRowRuleNull, pvalRowRule4, pvalRowRule3, pvalRowRule2, pvalRowRule1};
            CellRangeAddress[] pvalRange = {new CellRangeAddress(1, sheet.getLastRowNum(), columns.get(0), columns.get(columns.size() - 1))};
            sheetPVal.addConditionalFormatting(pvalRange, pvalRules);
        }

        // Conditional formatting for fold-change columns. Adjust fill color based on direction and size of fold-change.
        if (containsPattern(headers, "Fold-Change:")) {

            List<Integer> columns = findIndex(headers, "Fold-Change:");
            SheetConditionalFormatting sheetRatio = sheet.getSheetConditionalFormatting();

            ConditionalFormattingRule ratioRowRuleNull = sheetRatio.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"\"");
            PatternFormatting ratioPatternNull = ratioRowRuleNull.createPatternFormatting();
            ratioPatternNull.setFillBackgroundColor(DiscoveryReportColors.missingEven.getFillColor());
            ratioPatternNull.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleN5 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "0.10");
            PatternFormatting ratioPatternN5 = ratioRowRuleN5.createPatternFormatting();
            ratioPatternN5.setFillBackgroundColor(DiscoveryReportColors.ratioLow5.getFillColor());
            ratioPatternN5.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
            FontFormatting fontFmtN = ratioRowRuleN5.createFontFormatting();
            fontFmtN.setFontColorIndex(IndexedColors.YELLOW.index);

            ConditionalFormattingRule ratioRowRuleN4 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "0.13");
            PatternFormatting ratioPatternN4 = ratioRowRuleN4.createPatternFormatting();
            ratioPatternN4.setFillBackgroundColor(DiscoveryReportColors.ratioLow4.getFillColor());
            ratioPatternN4.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleN3 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "0.17");
            PatternFormatting ratioPatternN3 = ratioRowRuleN3.createPatternFormatting();
            ratioPatternN3.setFillBackgroundColor(DiscoveryReportColors.ratioLow3.getFillColor());
            ratioPatternN3.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleN2 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "0.25");
            PatternFormatting ratioPatternN2 = ratioRowRuleN2.createPatternFormatting();
            ratioPatternN2.setFillBackgroundColor(DiscoveryReportColors.ratioLow2.getFillColor());
            ratioPatternN2.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleN1 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "0.50");
            PatternFormatting ratioPatternN1 = ratioRowRuleN1.createPatternFormatting();
            ratioPatternN1.setFillBackgroundColor(DiscoveryReportColors.ratioLow1.getFillColor());
            ratioPatternN1.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleP5 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "10.0");
            PatternFormatting ratioPatternP5 = ratioRowRuleP5.createPatternFormatting();
            ratioPatternP5.setFillBackgroundColor(DiscoveryReportColors.ratioHigh5.getFillColor());
            ratioPatternP5.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
            FontFormatting fontFmtP = ratioRowRuleP5.createFontFormatting();
            fontFmtP.setFontColorIndex(IndexedColors.YELLOW.index);

            ConditionalFormattingRule ratioRowRuleP4 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "8.0");
            PatternFormatting ratioPatternP4 = ratioRowRuleP4.createPatternFormatting();
            ratioPatternP4.setFillBackgroundColor(DiscoveryReportColors.ratioHigh4.getFillColor());
            ratioPatternP4.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleP3 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "6.0");
            PatternFormatting ratioPatternP3 = ratioRowRuleP3.createPatternFormatting();
            ratioPatternP3.setFillBackgroundColor(DiscoveryReportColors.ratioHigh3.getFillColor());
            ratioPatternP3.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleP2 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "4.0");
            PatternFormatting ratioPatternP2 = ratioRowRuleP2.createPatternFormatting();
            ratioPatternP2.setFillBackgroundColor(DiscoveryReportColors.ratioHigh2.getFillColor());
            ratioPatternP2.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleP1 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "2.0");
            PatternFormatting ratioPatternP1 = ratioRowRuleP1.createPatternFormatting();
            ratioPatternP1.setFillBackgroundColor(DiscoveryReportColors.ratioHigh1.getFillColor());
            ratioPatternP1.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule[] ratioRules = {ratioRowRuleNull, ratioRowRuleN5, ratioRowRuleN4, ratioRowRuleN3, ratioRowRuleN2, ratioRowRuleN1, ratioRowRuleP5, ratioRowRuleP4, ratioRowRuleP3, ratioRowRuleP2, ratioRowRuleP1};
            CellRangeAddress[] ratioRange = {new CellRangeAddress(1, sheet.getLastRowNum(), columns.get(0), columns.get(columns.size() - 1))};
            sheetRatio.addConditionalFormatting(ratioRange, ratioRules);
        }

        // Conditional formatting for coefficient of variation columns. Adjust fill color based on magnitude of variation.
        if (containsPattern(headers, "CV")) {

            List<Integer> columns = findIndex(headers, "CV");
            SheetConditionalFormatting sheetCV = sheet.getSheetConditionalFormatting();

            ConditionalFormattingRule cvRowRuleNull = sheetCV.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"\"");
            PatternFormatting cvPatternNull = cvRowRuleNull.createPatternFormatting();
            cvPatternNull.setFillBackgroundColor(DiscoveryReportColors.missingEven.getFillColor());
            cvPatternNull.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule cvRowRuleP5 = sheetCV.createConditionalFormattingRule(ComparisonOperator.GT, "100");
            PatternFormatting cvPatternP5 = cvRowRuleP5.createPatternFormatting();
            cvPatternP5.setFillBackgroundColor(DiscoveryReportColors.ratioHigh5.getFillColor());
            cvPatternP5.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
            FontFormatting fontFmt = cvRowRuleP5.createFontFormatting();
            fontFmt.setFontColorIndex(IndexedColors.YELLOW.index);

            ConditionalFormattingRule cvRowRuleP4 = sheetCV.createConditionalFormattingRule(ComparisonOperator.GT, "80");
            PatternFormatting cvPatternP4 = cvRowRuleP4.createPatternFormatting();
            cvPatternP4.setFillBackgroundColor(DiscoveryReportColors.ratioHigh4.getFillColor());
            cvPatternP4.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule cvRowRuleP3 = sheetCV.createConditionalFormattingRule(ComparisonOperator.GT, "60");
            PatternFormatting cvPatternP3 = cvRowRuleP3.createPatternFormatting();
            cvPatternP3.setFillBackgroundColor(DiscoveryReportColors.ratioHigh3.getFillColor());
            cvPatternP3.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule cvRowRuleP2 = sheetCV.createConditionalFormattingRule(ComparisonOperator.GT, "40");
            PatternFormatting cvPatternP2 = cvRowRuleP2.createPatternFormatting();
            cvPatternP2.setFillBackgroundColor(DiscoveryReportColors.ratioHigh2.getFillColor());
            cvPatternP2.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule cvRowRuleP1 = sheetCV.createConditionalFormattingRule(ComparisonOperator.GT, "20");
            PatternFormatting cvPatternP1 = cvRowRuleP1.createPatternFormatting();
            cvPatternP1.setFillBackgroundColor(DiscoveryReportColors.ratioHigh1.getFillColor());
            cvPatternP1.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule[] cvRules = {cvRowRuleNull, cvRowRuleP5, cvRowRuleP4, cvRowRuleP3, cvRowRuleP2, cvRowRuleP1};
            CellRangeAddress[] cvRange = {new CellRangeAddress(1, sheet.getLastRowNum(), columns.get(0), columns.get(columns.size() - 1))};
            sheetCV.addConditionalFormatting(cvRange, cvRules);
        }

        // Conditional formatting for PSM confidence column. Adjust fill color based on confidence string.
        if (containsPattern(headers, "PSM Confidence")) {

            List<Integer> columns = findIndex(headers, "PSM Confidence");
            SheetConditionalFormatting sheetConf = sheet.getSheetConditionalFormatting();

            ConditionalFormattingRule confRowRuleNull = sheetConf.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"\"");
            PatternFormatting confPatternNull = confRowRuleNull.createPatternFormatting();
            confPatternNull.setFillBackgroundColor(DiscoveryReportColors.missingEven.getFillColor());
            confPatternNull.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule confRowRuleHigh = sheetConf.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"High\"");
            PatternFormatting confPatternHigh = confRowRuleHigh.createPatternFormatting();
            confPatternHigh.setFillBackgroundColor(DiscoveryReportColors.valueGoodOdd.getFillColor());
            confPatternHigh.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule confRowRuleMedium = sheetConf.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Medium\"");
            PatternFormatting confPatternMedium = confRowRuleMedium.createPatternFormatting();
            confPatternMedium.setFillBackgroundColor(DiscoveryReportColors.valueMediumOdd.getFillColor());
            confPatternMedium.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule confRowRuleLow = sheetConf.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"Low\"");
            PatternFormatting confPatternLow = confRowRuleLow.createPatternFormatting();
            confPatternLow.setFillBackgroundColor(DiscoveryReportColors.valueBadOdd.getFillColor());
            confPatternLow.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule[] confRules = {confRowRuleNull, confRowRuleHigh, confRowRuleMedium, confRowRuleLow};
            CellRangeAddress[] confRange = {new CellRangeAddress(1, sheet.getLastRowNum(), columns.get(0), columns.get(columns.size() - 1))};
            sheetConf.addConditionalFormatting(confRange, confRules);
        }

        // Conditional formatting for peptides with multiple protein groups. Adjust fill color of specified columns to
        // indicate peptide does not provide sufficient evidence for protein detection or quantitation.
        if (containsPattern(headers, "Protein Groups")) {

            List<Integer> columns = findIndex(headers, "Protein Groups");
            SheetConditionalFormatting sheetPG = sheet.getSheetConditionalFormatting();

            ConditionalFormattingRule pgRowRule1 = sheetPG.createConditionalFormattingRule("$F2 > 1");
            PatternFormatting pgPattern1 = pgRowRule1.createPatternFormatting();
            pgPattern1.setFillBackgroundColor(DiscoveryReportColors.valueMediumOdd.getFillColor());
            pgPattern1.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule[] pgRules = {pgRowRule1};
            CellRangeAddress[] pgRange = {new CellRangeAddress(1, sheet.getLastRowNum(), 0, columns.get(columns.size() - 1))};
            sheetPG.addConditionalFormatting(pgRange, pgRules);
        }

        // Conditional formatting for peptide rows based on Quan Info column. Adjust fill color to indicate peptide
        // is not suitable for quantification of associated protein(s)
        if (containsPattern(headers, "Quan Info")) {

            List<Integer> columns = findIndex(headers, "Quan Info");
            SheetConditionalFormatting sheetPG = sheet.getSheetConditionalFormatting();

            ConditionalFormattingRule pgRowRule1 = sheetPG.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"NotUnique\"");
            PatternFormatting pgPattern1 = pgRowRule1.createPatternFormatting();
            pgPattern1.setFillBackgroundColor(DiscoveryReportColors.valueMediumOdd.getFillColor());
            pgPattern1.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule[] pgRules = {pgRowRule1};
            CellRangeAddress[] pgRange = {new CellRangeAddress(1, sheet.getLastRowNum(), columns.get(0), columns.get(columns.size() - 1))};
            sheetPG.addConditionalFormatting(pgRange, pgRules);
        }

        // Conditional formatting for enrichment score column of pathway analysis. Adjust fill color based on string and
        // direction of pathway enrichment
        if (containsPattern(headers, "Enrichment Score:")) {

            List<Integer> columns = findIndex(headers, "Enrichment Score:");
            SheetConditionalFormatting sheetRatio = sheet.getSheetConditionalFormatting();

            ConditionalFormattingRule ratioRowRuleNull = sheetRatio.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"\"");
            PatternFormatting ratioPatternNull = ratioRowRuleNull.createPatternFormatting();
            ratioPatternNull.setFillBackgroundColor(DiscoveryReportColors.missingEven.getFillColor());
            ratioPatternNull.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleN5 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "-2.5");
            PatternFormatting ratioPatternN5 = ratioRowRuleN5.createPatternFormatting();
            ratioPatternN5.setFillBackgroundColor(DiscoveryReportColors.ratioLow5.getFillColor());
            ratioPatternN5.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
            FontFormatting fontFmtN = ratioRowRuleN5.createFontFormatting();
            fontFmtN.setFontColorIndex(IndexedColors.YELLOW.index);

            ConditionalFormattingRule ratioRowRuleN4 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "-2.0");
            PatternFormatting ratioPatternN4 = ratioRowRuleN4.createPatternFormatting();
            ratioPatternN4.setFillBackgroundColor(DiscoveryReportColors.ratioLow4.getFillColor());
            ratioPatternN4.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleN3 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "-1.75");
            PatternFormatting ratioPatternN3 = ratioRowRuleN3.createPatternFormatting();
            ratioPatternN3.setFillBackgroundColor(DiscoveryReportColors.ratioLow3.getFillColor());
            ratioPatternN3.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleN2 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "-1.5");
            PatternFormatting ratioPatternN2 = ratioRowRuleN2.createPatternFormatting();
            ratioPatternN2.setFillBackgroundColor(DiscoveryReportColors.ratioLow2.getFillColor());
            ratioPatternN2.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleN1 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.LT, "-1.25");
            PatternFormatting ratioPatternN1 = ratioRowRuleN1.createPatternFormatting();
            ratioPatternN1.setFillBackgroundColor(DiscoveryReportColors.ratioLow1.getFillColor());
            ratioPatternN1.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleP5 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "2.5");
            PatternFormatting ratioPatternP5 = ratioRowRuleP5.createPatternFormatting();
            ratioPatternP5.setFillBackgroundColor(DiscoveryReportColors.ratioHigh5.getFillColor());
            ratioPatternP5.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());
            FontFormatting fontFmtP = ratioRowRuleP5.createFontFormatting();
            fontFmtP.setFontColorIndex(IndexedColors.YELLOW.index);

            ConditionalFormattingRule ratioRowRuleP4 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "2.0");
            PatternFormatting ratioPatternP4 = ratioRowRuleP4.createPatternFormatting();
            ratioPatternP4.setFillBackgroundColor(DiscoveryReportColors.ratioHigh4.getFillColor());
            ratioPatternP4.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleP3 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "1.75");
            PatternFormatting ratioPatternP3 = ratioRowRuleP3.createPatternFormatting();
            ratioPatternP3.setFillBackgroundColor(DiscoveryReportColors.ratioHigh3.getFillColor());
            ratioPatternP3.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleP2 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "1.5");
            PatternFormatting ratioPatternP2 = ratioRowRuleP2.createPatternFormatting();
            ratioPatternP2.setFillBackgroundColor(DiscoveryReportColors.ratioHigh2.getFillColor());
            ratioPatternP2.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule ratioRowRuleP1 = sheetRatio.createConditionalFormattingRule(ComparisonOperator.GT, "1.25");
            PatternFormatting ratioPatternP1 = ratioRowRuleP1.createPatternFormatting();
            ratioPatternP1.setFillBackgroundColor(DiscoveryReportColors.ratioHigh1.getFillColor());
            ratioPatternP1.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

            ConditionalFormattingRule[] ratioRules = {ratioRowRuleNull, ratioRowRuleN5, ratioRowRuleN4, ratioRowRuleN3, ratioRowRuleN2, ratioRowRuleN1, ratioRowRuleP5, ratioRowRuleP4, ratioRowRuleP3, ratioRowRuleP2, ratioRowRuleP1};
            CellRangeAddress[] ratioRange = {new CellRangeAddress(1, sheet.getLastRowNum(), columns.get(0), columns.get(columns.size() - 1))};
            sheetRatio.addConditionalFormatting(ratioRange, ratioRules);
        }

        // Default conditional formatting for even and odd rows
        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
        ConditionalFormattingRule evenRowRule = sheetCF.createConditionalFormattingRule("MOD(ROW(),2)=0");
        PatternFormatting evenRowPattern = evenRowRule.createPatternFormatting();
        evenRowPattern.setFillBackgroundColor(DiscoveryReportColors.defaultEvenCenter.getFillColor());
        evenRowPattern.setFillBackgroundColor(FillPatternType.SOLID_FOREGROUND.getCode());

        ConditionalFormattingRule oddRowRule = sheetCF.createConditionalFormattingRule("MOD(ROW(),2)=1");
        PatternFormatting oddRowPattern = oddRowRule.createPatternFormatting();
        oddRowPattern.setFillBackgroundColor(DiscoveryReportColors.defaultOddCenter.getFillColor());
        oddRowPattern.setFillPattern(FillPatternType.SOLID_FOREGROUND.getCode());

        CellRangeAddress[] range = {new CellRangeAddress(1, sheet.getLastRowNum() - 1, 0, tableView.getColumns().size() - 1)};
        sheetCF.addConditionalFormatting(range, evenRowRule, oddRowRule);

        // Automatically adjust column widths based on column header size and max column cell values. Restrict columnns
        // from being too wide or too narrow.
        for (int i = 0; i < tableView.getColumns().size(); i++) {

            int cellWidth = valueSize.get(i);
            int headerWidth = headerSize.get(i);

            if (cellWidth > headerWidth) {
                if (cellWidth > 75) {
                    sheet.setColumnWidth(i, 75 * 256);
                } else {
                    sheet.setColumnWidth(i, cellWidth * 256);
                }
            } else if (headerWidth < 10) {
                sheet.setColumnWidth(i, 8 * 256);
            } else if (headerWidth < 20) {
                sheet.setColumnWidth(i, 12 * 256);
            } else {
                sheet.setColumnWidth(i, 22 * 256);
            }
        }
    }
}
