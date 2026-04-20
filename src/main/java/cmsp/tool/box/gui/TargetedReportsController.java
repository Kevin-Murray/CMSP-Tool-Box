package cmsp.tool.box.gui;

import cmsp.tool.box.Launcher;
import cmsp.tool.box.datamodel.TargetedDetailEntry;
import cmsp.tool.box.datamodel.TargetedMoleculeEntry;
import cmsp.tool.box.enums.ErrorTypes;
import cmsp.tool.box.tasks.TargetedReportsTask;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Controller class for Targeted Reports module.
 * Handles all interactions of user with report settings and displaying report output.
 */
public class TargetedReportsController {

    static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(TargetedReportsController.class);
    private int accThreshold;
    @FXML private TableView<Map<Integer, String>> accuracyTable;
    @FXML private Spinner<Integer> accuracyThresholdSpinner;
    @FXML private Spinner<Integer> blankThresholdSpinner;
    private Path databasePath;
    private Path documentPath;
    @FXML private TableView<TargetedDetailEntry> fileDetailsTable;
    private int lodThreshold;
    private int loqThreshold;
    @FXML private Spinner<Integer> loqThresholdSpinner;
    @FXML private TableView<TargetedMoleculeEntry> moleculeTable;
    @FXML private TableView<Map<Integer, String>> precisionTable;
    private Preferences prefs;
    private TargetedReportsTask reportsTask;
    @FXML private TableView<Map<Integer, String>> sampleTable;
    @FXML private TextField skylineDocumentFile;
    private Workbook workbook;

    /**
     * Get error message stack trace for error message display.
     *
     * @param e Exception
     * @return Error message string
     */
    public static String getStackTraceString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Create excel workbook object for final report.
     *
     * @throws IOException
     */
    public void createExcelWorkbook() throws IOException {

        workbook = new XSSFWorkbook();

        Sheet sheetSummary = workbook.createSheet("Result Summary");
        Sheet sheetFit = workbook.createSheet("Calibration Fit");
        Sheet sheetAccuracy = workbook.createSheet("Experimental Accuracy");
        Sheet sheetPrecision = workbook.createSheet("Experimental Precision");
        Sheet sheetDetails = workbook.createSheet("Raw Data Details");
        Sheet sheetLegend = workbook.createSheet("Color Legend");

        writeXLSXTable(sheetSummary, sampleTable, "Result Summary");
        writeXLSXTable(sheetFit, moleculeTable, "Calibration Fit");
        writeXLSXTable(sheetAccuracy, accuracyTable, "Experimental Accuracy");
        writeXLSXTable(sheetPrecision, precisionTable, "Experimental Precision");
        writeXLSXTable(sheetDetails, fileDetailsTable, "Raw Data Details");

        writeXLSXTableLegend(sheetLegend);
    }

    /**
     * Export report workbook to local file.
     *
     * @param actionEvent Mouseclick. Not used.
     */
    public void exportButtonClick(ActionEvent actionEvent) {

        Stage stage = new Stage();

        File defaultDirectory = new File(documentPath.getParent().toString());
        String defaultName = documentPath.getFileName().toString().split("\\.")[0] + "_report.xlsx";

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report As...");
        fileChooser.setInitialDirectory(defaultDirectory);
        fileChooser.setInitialFileName(defaultName);

        FileChooser.ExtensionFilter extFilter =
                new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx");
        fileChooser.getExtensionFilters().add(extFilter);

        File outputFile = fileChooser.showSaveDialog(stage);

        // Write to file
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            workbook.write(out);
            out.close();
            workbook.close();
        } catch (IOException e) {
            this.skylineDocumentFile.setText("");
            this.documentPath = null;
            showErrorMessage(getStackTraceString(e));
        }
    }

    /**
     * Submit selected Skyline document for report formatting.
     * Export necessary reports and format them for visualization.
     */
    public void generateResultTables() {

        this.lodThreshold = blankThresholdSpinner.getValue();
        this.loqThreshold = loqThresholdSpinner.getValue();
        this.accThreshold = accuracyThresholdSpinner.getValue();

        // Submit user parameters to new task.
        reportsTask = new TargetedReportsTask(databasePath, documentPath, lodThreshold, loqThreshold, accThreshold);

        // Export results and format output for TableView
        // Catch unhandled error messages
        try {
            reportsTask.run();
        } catch (Exception e) {
            this.skylineDocumentFile.setText("");
            this.documentPath = null;
            showErrorMessage(getStackTraceString(e));
            return;
        }

        // Handled error message recorded.
        if (reportsTask.hasErrorMessage()) {
            this.skylineDocumentFile.setText("");
            this.documentPath = null;
            showErrorMessage(reportsTask.getErrorMessages());
            return;
        }

        // Check if Skyline exports are valid and contain necessary components.
        ErrorTypes error = reportsTask.validDocument();
        if (error != null) {
            this.skylineDocumentFile.setText("");
            this.documentPath = null;
            showErrorMessage(error);
            return;
        }

        // Generate tables for visualization and export.
        // Print unhandled error messages
        try {
            reportsTask.generateReports();
        } catch (Exception e) {
            this.skylineDocumentFile.setText("");
            this.documentPath = null;
            showErrorMessage(getStackTraceString(e));
            return;
        }

        // Sample Outcomes Table
        sampleTable.getColumns().clear();
        sampleTable.getItems().clear();
        sampleTable.refresh();
        sampleTable.getColumns().addAll(reportsTask.makeSampleTable());
        sampleTable.getItems().addAll(reportsTask.getSampleTable());

        // Calibration Fit Table
        moleculeTable.getColumns().clear();
        moleculeTable.getItems().clear();
        moleculeTable.refresh();
        moleculeTable.getColumns().addAll(reportsTask.makeMoleculeTable());
        moleculeTable.getItems().addAll(reportsTask.getTargetedMoleculeEntry());

        // Experimental Accuracy Table
        accuracyTable.getColumns().clear();
        accuracyTable.getItems().clear();
        accuracyTable.refresh();
        accuracyTable.getColumns().addAll(reportsTask.makeAccuracyTable());
        accuracyTable.getItems().addAll(reportsTask.getAccuracyTable());

        // Experimental Precision Table
        precisionTable.getItems().clear();
        precisionTable.getColumns().clear();
        precisionTable.refresh();
        if (!reportsTask.precisionTableEmpty()) {
            precisionTable.getColumns().addAll(reportsTask.makePrecisionTable());
            precisionTable.getItems().addAll(reportsTask.getPrecisionTable());
        }

        // Replicate Details Table
        fileDetailsTable.getColumns().clear();
        fileDetailsTable.getItems().clear();
        fileDetailsTable.refresh();
        fileDetailsTable.getColumns().addAll(reportsTask.makeReplicateDetailTable());
        fileDetailsTable.getItems().addAll(reportsTask.getReplicateDetails());

        // Create initial workbook object.
        try {
            createExcelWorkbook();
        } catch (IOException e) {
            this.skylineDocumentFile.setText("");
            this.documentPath = null;
            showErrorMessage(getStackTraceString(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * Return cell fill color based on table type, column header and value.
     *
     * @param tableType Table type.
     * @param header    Header of column
     * @param value     Cell value
     * @param rowIndex  Row index
     * @return Foreground fill color of specified cell.
     */
    private XSSFColor getCellColor(String tableType, String header, String value, int rowIndex) {

        String colorString;

        if (tableType.equals("Result Summary")) {
            colorString = reportsTask.getSampleColor(header, value, rowIndex);
        } else {

            if (value == null) {
                colorString = ((rowIndex % 2) == 0) ? "FFFFFF" : "F6F6F6";
            } else if (value.equals("NOT USED")) {
                colorString = ((rowIndex % 2) == 0) ? "D9D9D9" : "BFBFBF";
            } else if (header.contains("Accuracy")) {
                double acc = Math.abs(100 - Double.parseDouble(value.split("%")[0]));
                if (acc < accThreshold) {
                    colorString = ((rowIndex % 2) == 0) ? "D9F2D0" : "B4E5A2";
                } else if (acc > accThreshold & acc < accThreshold * 2) {
                    colorString = ((rowIndex % 2) == 0) ? "FFF5C9" : "FFEB9C";
                } else {
                    colorString = ((rowIndex % 2) == 0) ? "FFD1D8" : "FFB6C1";
                }
            } else if (header.contains("RSD")) {
                double acc = Double.parseDouble(value.split("%")[0]);
                if (acc < 15.0) {
                    colorString = ((rowIndex % 2) == 0) ? "D9F2D0" : "B4E5A2";
                } else if (acc > 15.0 & acc < 15.0 * 2) {
                    colorString = ((rowIndex % 2) == 0) ? "FFF5C9" : "FFEB9C";
                } else {
                    colorString = ((rowIndex % 2) == 0) ? "FFD1D8" : "FFB6C1";
                }
            } else {
                colorString = ((rowIndex % 2) == 0) ? "FFFFFF" : "F6F6F6";
            }
        }

        // Convert color string to hexcode
        try {
            return new XSSFColor(Hex.decodeHex(colorString), null);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get user preference (database path) if they have used application before.
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
     * Initialize module.
     * Set spinner values.
     */
    public void initialize() {

        getPreferences();

        // Limit of Detection signal-to-noise spinner.
        SpinnerValueFactory<Integer> valueFactoryBlank = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10);
        valueFactoryBlank.setValue(3);
        blankThresholdSpinner.setValueFactory(valueFactoryBlank);
        blankThresholdSpinner.editorProperty().get().setAlignment(Pos.CENTER);

        // Limit of Detection signal-to-noise spinner.
        SpinnerValueFactory<Integer> valueFactoryLOQ = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20);
        valueFactoryLOQ.setValue(5);
        loqThresholdSpinner.setValueFactory(valueFactoryLOQ);
        loqThresholdSpinner.editorProperty().get().setAlignment(Pos.CENTER);

        // Limit of Quantitation minimum accuracy spinner.
        SpinnerValueFactory<Integer> valueFactoryAccuracy = new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 30);
        valueFactoryAccuracy.setValue(20);

        // Insert percent sign in to spinner text field.
        valueFactoryAccuracy.setConverter(new StringConverter<>() {

            @Override
            public Integer fromString(String s) {
                String integer = s.replaceAll("%", "").trim();
                return Integer.valueOf(integer);
            }

            @Override
            public String toString(Integer integer) {
                return integer.toString() + "%";
            }
        });

        accuracyThresholdSpinner.setValueFactory(valueFactoryAccuracy);
        accuracyThresholdSpinner.editorProperty().get().setAlignment(Pos.CENTER);

        moleculeTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        moleculeTable.getSelectionModel().setCellSelectionEnabled(true);

        accuracyTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        accuracyTable.getSelectionModel().setCellSelectionEnabled(true);

        precisionTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        precisionTable.getSelectionModel().setCellSelectionEnabled(true);

        sampleTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        sampleTable.getSelectionModel().setCellSelectionEnabled(true);

        fileDetailsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        fileDetailsTable.getSelectionModel().setCellSelectionEnabled(true);
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
     * Handle application set-up menu.
     * Currently only used to designate directory of QC databases.
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

            // Launch pop-up window.
            Stage stage = new Stage();
            stage.setTitle("AutoQC Set Up...");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);

            stage.showAndWait();

            if (!Files.exists(controller.getDatabaseFolder())) {
                showErrorMessage(ErrorTypes.DATABASE);
            } else {
                // Update database location and set application defaults.
                this.databasePath = controller.getDatabaseFolder();
                setPreferences();
            }
        } catch (Exception e) {
            // TODO - better error handling.
            e.printStackTrace();
        }
    }

    /**
     * Handles user selecting Skyline document to format.
     * Submits path to export necessary files and format results tables.
     *
     * @param event
     */
    public void openDocumentClick(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // Initialize file chooser with Skyline extension.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Skyline Document");
        ExtensionFilter ex1 = new ExtensionFilter("Skyline Document", "*.sky");
        fileChooser.getExtensionFilters().addAll(ex1);

        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            this.skylineDocumentFile.setText(selectedFile.getName());
            this.documentPath = Paths.get(selectedFile.getPath());
        } else {
            this.skylineDocumentFile.setText("");
            this.documentPath = null;
        }

        // Submit Skyline document path for exports.
        if (this.documentPath != null) {

            if (this.databasePath == null || this.databasePath.toString().isEmpty()) {
                showErrorMessage(ErrorTypes.DATABASE);
                this.skylineDocumentFile.setText("");
                this.documentPath = null;
            } else {
                // Start new thread for processing. Allows GUI to update during processing
                Platform.runLater(this::generateResultTables);
            }
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
     * Write submitted table to Excel sheet.
     *
     * @param sheet     Specified sheet
     * @param tableView Results table
     * @param tableType Table type for color formatting
     * @param <T>       Ignore
     */
    private <T> void writeXLSXTable(Sheet sheet, TableView<T> tableView, String tableType) {

        // Header row style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        try {
            XSSFColor color = new XSSFColor(Hex.decodeHex("DAE7F5"), null);
            headerStyle.setFillForegroundColor(color);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.DOUBLE);
        headerStyle.setRightBorderColor(IndexedColors.GREY_40_PERCENT.index);
        headerStyle.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.index);

        List<String> headers = new ArrayList<>();

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.setHeight((short) (25 * 20));
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            TableColumn<T, ?> column = tableView.getColumns().get(i);
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(column.getText());
            headers.add(column.getText());
            cell.setCellStyle(headerStyle);
        }

        // Populate data rows
        for (int i = 0; i < tableView.getItems().size(); i++) {
            Row dataRow = sheet.createRow(i + 1);
            for (int j = 0; j < tableView.getColumns().size(); j++) {
                TableColumn<T, ?> column = tableView.getColumns().get(j);
                Object cellValue = column.getCellData(i);
                Cell cell = dataRow.createCell(j);
                String value = (cellValue == null) ? null : cellValue.toString();
                boolean accuracy = false;
                if (value != null) {
                    if (isNumeric(value)) {
                        cell.setCellValue(Double.parseDouble(value));
                    } else {
                        if (value.contains("%")) {
                            try {
                                cell.setCellValue(Double.parseDouble(value.split("%")[0]) / 100.0);
                                accuracy = true;
                            } catch (NumberFormatException e) {
                                cell.setCellValue(value);
                            }
                        } else {
                            cell.setCellValue(value);
                        }
                    }
                }

                CellStyle style = workbook.createCellStyle();
                if (accuracy) {
                    DataFormat dataFormat = workbook.createDataFormat();
                    style.setDataFormat(dataFormat.getFormat("0.0%"));
                }
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                XSSFColor color = getCellColor(tableType, headers.get(j), value, i);
                style.setFillForegroundColor(color);
                style.setBorderRight(BorderStyle.THIN);
                style.setBorderBottom(BorderStyle.THIN);
                style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.index);
                style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.index);
                cell.setCellStyle(style);
            }
        }

        // Adjust column widths
        for (int i = 0; i < tableView.getColumns().size(); i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) > 15000) {
                sheet.setColumnWidth(i, 15000);
            } else {
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 500);
            }
        }
    }

    /**
     * Create Color Legend sheet
     *
     * @param sheet Legend sheet
     */
    private void writeXLSXTableLegend(Sheet sheet) {

        // Header row style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        try {
            XSSFColor color = new XSSFColor(Hex.decodeHex("DAE7F5"), null);
            headerStyle.setFillForegroundColor(color);
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);

        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setAlignment(HorizontalAlignment.CENTER);
        textStyle.setBorderRight(BorderStyle.THIN);
        textStyle.setBorderBottom(BorderStyle.THIN);

        CellStyle grey1Style = workbook.createCellStyle();
        CellStyle grey2Style = workbook.createCellStyle();
        CellStyle red1Style = workbook.createCellStyle();
        CellStyle red2Style = workbook.createCellStyle();
        CellStyle yellow1Style = workbook.createCellStyle();
        CellStyle yellow2Style = workbook.createCellStyle();
        CellStyle green1Style = workbook.createCellStyle();
        CellStyle green2Style = workbook.createCellStyle();

        grey1Style.setBorderBottom(BorderStyle.THIN);
        grey2Style.setBorderBottom(BorderStyle.THIN);
        green1Style.setBorderBottom(BorderStyle.THIN);
        green2Style.setBorderBottom(BorderStyle.THIN);
        red1Style.setBorderBottom(BorderStyle.THIN);
        red2Style.setBorderBottom(BorderStyle.THIN);
        yellow1Style.setBorderBottom(BorderStyle.THIN);
        yellow2Style.setBorderBottom(BorderStyle.THIN);
        red1Style.setBorderBottom(BorderStyle.THIN);
        red2Style.setBorderBottom(BorderStyle.THIN);

        grey2Style.setBorderRight(BorderStyle.THIN);
        red2Style.setBorderRight(BorderStyle.THIN);
        yellow2Style.setBorderRight(BorderStyle.THIN);
        green2Style.setBorderRight(BorderStyle.THIN);

        try {

            XSSFColor colorGrey1 = new XSSFColor(Hex.decodeHex("D9D9D9"), null);
            XSSFColor colorGrey2 = new XSSFColor(Hex.decodeHex("BFBFBF"), null);
            XSSFColor colorRed1 = new XSSFColor(Hex.decodeHex("FFD1D8"), null);
            XSSFColor colorRed2 = new XSSFColor(Hex.decodeHex("FFB6C1"), null);
            XSSFColor colorYellow1 = new XSSFColor(Hex.decodeHex("FFF5C9"), null);
            XSSFColor colorYellow2 = new XSSFColor(Hex.decodeHex("FFEB9C"), null);
            XSSFColor colorGreen1 = new XSSFColor(Hex.decodeHex("D9F2D0"), null);
            XSSFColor colorGreen2 = new XSSFColor(Hex.decodeHex("B4E5A2"), null);

            grey1Style.setFillForegroundColor(colorGrey1);
            grey1Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            grey2Style.setFillForegroundColor(colorGrey2);
            grey2Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            red1Style.setFillForegroundColor(colorRed1);
            red1Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            red2Style.setFillForegroundColor(colorRed2);
            red2Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            yellow1Style.setFillForegroundColor(colorYellow1);
            yellow1Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            yellow2Style.setFillForegroundColor(colorYellow2);
            yellow2Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            green1Style.setFillForegroundColor(colorGreen1);
            green1Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            green2Style.setFillForegroundColor(colorGreen2);
            green2Style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }

        // Result Summary
        Row row0 = sheet.createRow(0);
        row0.setHeight((short) (25 * 20));
        Cell cell00 = row0.createCell(0);
        cell00.setCellValue("Result Summary");
        cell00.setCellStyle(headerStyle);
        Cell cell01 = row0.createCell(1);
        cell01.setCellStyle(headerStyle);
        Cell cell02 = row0.createCell(2);
        cell02.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        Row row1 = sheet.createRow(1);
        Cell cell10 = row1.createCell(0);
        cell10.setCellValue("Concentration below limit of detection (LOD)");
        cell10.setCellStyle(textStyle);
        Cell cell11 = row1.createCell(1);
        cell11.setCellStyle(red1Style);
        Cell cell12 = row1.createCell(2);
        cell12.setCellStyle(red2Style);

        Row row2 = sheet.createRow(2);
        Cell cell20 = row2.createCell(0);
        cell20.setCellValue("Concentration outside quantitation limits (LLOQ/ULOQ)");
        cell20.setCellStyle(textStyle);
        Cell cell21 = row2.createCell(1);
        cell21.setCellStyle(yellow1Style);
        Cell cell22 = row2.createCell(2);
        cell22.setCellStyle(yellow2Style);

        sheet.createRow(3);

        // Experimental Accuracy and Precision
        Row row4 = sheet.createRow(4);
        row4.setHeight((short) (25 * 20));
        Cell cell40 = row4.createCell(0);
        cell40.setCellValue("Experimental Accuracy / Precision");
        cell40.setCellStyle(headerStyle);
        Cell cell41 = row4.createCell(1);
        cell41.setCellStyle(headerStyle);
        Cell cell42 = row4.createCell(2);
        cell42.setCellStyle(headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 2));

        Row row5 = sheet.createRow(5);
        Cell cell50 = row5.createCell(0);
        cell50.setCellValue("Standard not used in calibration curve");
        cell50.setCellStyle(textStyle);
        Cell cell51 = row5.createCell(1);
        cell51.setCellStyle(grey1Style);
        Cell cell52 = row5.createCell(2);
        cell52.setCellStyle(grey2Style);

        Row row6 = sheet.createRow(6);
        Cell cell60 = row6.createCell(0);
        cell60.setCellValue("Accuracy < " + accThreshold + "%   RSD < 15%");
        cell60.setCellStyle(textStyle);
        Cell cell61 = row6.createCell(1);
        cell61.setCellStyle(green1Style);
        Cell cell62 = row6.createCell(2);
        cell62.setCellStyle(green2Style);

        Row row7 = sheet.createRow(7);
        Cell cell70 = row7.createCell(0);
        cell70.setCellValue("Accuracy < " + accThreshold * 2 + "%   RSD < 30%");
        cell70.setCellStyle(textStyle);
        Cell cell71 = row7.createCell(1);
        cell71.setCellStyle(yellow1Style);
        Cell cell72 = row7.createCell(2);
        cell72.setCellStyle(yellow2Style);

        Row row8 = sheet.createRow(8);
        Cell cell80 = row8.createCell(0);
        cell80.setCellValue("Accuracy > " + accThreshold * 2 + "%   RSD > 30%");
        cell80.setCellStyle(textStyle);
        Cell cell81 = row8.createCell(1);
        cell81.setCellStyle(red1Style);
        Cell cell82 = row8.createCell(2);
        cell82.setCellStyle(red2Style);

        sheet.setColumnWidth(0, 60 * 256);
        sheet.setColumnWidth(1, 10 * 256);
        sheet.setColumnWidth(2, 10 * 256);
    }
}


