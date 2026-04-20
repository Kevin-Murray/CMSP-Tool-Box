package cmsp.tool.box.gui;

import cmsp.tool.box.Launcher;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SequenceBuilderMainController {

    public TabPane tabPane;

    private String expPrefix;
    private String instrument;
    private LinkedHashMap<String, ArrayList<TableColumn<Map<Integer, String>, String>>> batchHeaderMap;
    private LinkedHashMap<String, ObservableList<Map<Integer, String>>> batchDataMap;

    /**
     * Initialize GUI.
     * Launch directly into sequence parameters page
     */
    public void initialize() {
        Platform.runLater(this::openSequenceBuilderParametersWindow);
    }

    /**
     * Handle action request for a new report.
     */
    public void actionNewReport() {
        Platform.runLater(this::openSequenceBuilderParametersWindow);
    }

    /**
     * Launch new window to collect sequence parameters
     */
    public void openSequenceBuilderParametersWindow() {

        try {
            // Get window design for setting page.
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("SequenceBuilderSettings.fxml"));
            Parent root = fxmlLoader.load();

            // Update controller class with current database location.
            SequenceBuilderParametersController controller = fxmlLoader.getController();
            controller.setMainController(this);

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
     * Set sequence table header and table data maps from sequence parameters wizard.
     *
     * @param batchHeaderMap Map of table column array
     * @param batchDataMap Map of table data lists
     */
    public void setBatchData(LinkedHashMap<String, ArrayList<TableColumn<Map<Integer, String>, String>>> batchHeaderMap, LinkedHashMap<String, ObservableList<Map<Integer, String>>> batchDataMap) {
        this.batchHeaderMap = batchHeaderMap;
        this.batchDataMap = batchDataMap;
    }

    /**
     * Set experiment file name prefix.
     */
    public void setExpPrefix(String prefix) {
        this.expPrefix = prefix;
    }

    /**
     * Set experiment instrument selection.
     */
    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    /**
     * Update tableview with created sequence data.
     */
    public void updateBatchTable() {

        if (batchHeaderMap == null || batchDataMap == null) {
            return;
        }

        // Clear existing results
        tabPane.getTabs().clear();
        List<Tab> tabList = new ArrayList<>();

        // For each report in selected project data, create tab and populate tableview with data.
        List<String> batchNames = new ArrayList<>(batchHeaderMap.keySet());
        for (String batch : batchNames) {

            // Get data for tableview
            ArrayList<TableColumn<Map<Integer, String>, String>> reportColumns = batchHeaderMap.get(batch);
            ObservableList<Map<Integer, String>> reportData = batchDataMap.get(batch);

            // Populate tableview
            TableView<Map<Integer, String>> reportTable = new TableView<>();
            reportTable.getColumns().addAll(reportColumns);
            reportTable.getItems().addAll(reportData);
            reportTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

            // Create tab and add tableview
            Tab reportTab = new Tab(batch);
            reportTab.setContent(reportTable);
            tabList.add(reportTab); // Tab index is for order of tabs
        }

        // Remove unused tab levels to achieve desired organization.
        tabPane.getTabs().addAll(tabList);
    }

    /**
     * Export all tableview into CSV file in user selected directory.
     */
    public void exportBatchSequence() {

        // Get user selected output directory
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose Directory to Save Batch Sequences...");
        File outDir = dirChooser.showDialog(new Stage());

        // Loop through and export each tableview.
        Set<String> keys = batchHeaderMap.keySet();
        for (String key : keys) {

            Path outPath = Paths.get(String.valueOf(outDir), expPrefix + "_" + key + ".csv");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(String.valueOf(outPath)))) {

                // Thermo sequence files require a special string at the start of the file.
                if (instrument.contains("Thermo") && !key.equals("Preparation_Details")) {
                    writer.write("Bracket Type=4");
                    writer.newLine();
                }

                // Write header row
                ArrayList<TableColumn<Map<Integer, String>, String>> headerColumns = batchHeaderMap.get(key);
                ArrayList<String> header = new ArrayList<>();
                for (TableColumn<Map<Integer, String>, String> column : headerColumns) {
                    header.add(column.getText());
                }
                writer.write(String.join(",", header));
                writer.newLine();

                // Write data rows
                List<Map<Integer, String>> dataList = batchDataMap.get(key);
                for (Map<Integer, String> dataRow : dataList) {
                    writer.write(String.join(",", dataRow.values()));
                    writer.newLine();
                }

            } catch (IOException e) {
                // TODO - more robust error handling
                System.err.println("Error writing CSV file: " + e.getMessage());
            }
        }

        // Alert user to successful export
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sequence Builder");
        alert.setHeaderText("Export Complete - " + batchHeaderMap.size() + " Files Generated");
        alert.setContentText("Export Path: " + outDir);
        alert.showAndWait();
    }
}
