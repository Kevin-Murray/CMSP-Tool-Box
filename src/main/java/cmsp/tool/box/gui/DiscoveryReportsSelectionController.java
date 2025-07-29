package cmsp.tool.box.gui;

import cmsp.tool.box.Launcher;
import cmsp.tool.box.enums.DiscoveryReportTypes;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Controller class for the Discovery Report import wizard - report type and project path selector
 */
public class DiscoveryReportsSelectionController {

    private final ObservableList<String> reportTypes = FXCollections.observableArrayList();

    public ImageView discoveryImage;
    public Label discoveryLabel;
    public ListView<String> discoveryLists;
    public TextArea discoveryMessage;
    public Button nextButton;
    public Button previousButton;
    public TextField projectPath;
    public Button searchButton;
    private DiscoveryReportsMainController mainController;
    private DiscoveryReportTypes reportType;
    private Path rscriptPath;

    /**
     * Initialize window with default selections
     */
    public void initialize() {

        discoveryImage.setPreserveRatio(true);

        // Add Report Types to List
        DiscoveryReportTypes[] reports = DiscoveryReportTypes.class.getEnumConstants();
        for (DiscoveryReportTypes report : reports) {
            reportTypes.add(report.getLabel());
        }
        discoveryLists.setItems(reportTypes);

        // Listen for Changes in List
        discoveryLists.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            reportType = DiscoveryReportTypes.fromImportName(newValue);
            if (reportType != null) {
                projectPath.setText("");
                nextButton.setDisable(true);
                searchButton.setDisable(false);
                discoveryImage.setImage(reportType.getImage());
                discoveryMessage.setText(reportType.getMessage());
                discoveryLabel.setText(reportType.getInfo());
            }
        });
    }

    /**
     * Handle action event - next button clicked to process selected project type
     */
    public void nextButtonClicked(ActionEvent actionEvent) {

        try {
            // Load appropriate FXML for selected project type
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource(reportType.getFxmlPage()));
            Parent root = fxmlLoader.load();

            // Set appropriate controller for report type
            // TODO - currently hard-coded to Proteome Discoverer
            DiscoveryReportsPdController controller = fxmlLoader.getController();
            controller.setProjectPath(reportType, projectPath.getText());
            controller.setMainController(mainController);
            controller.setRScriptPath(rscriptPath);

            // Update scene
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
     * Handle action event -- open project click. Open directory or file chooser dialogue window for project selection.
     */
    public void openDocumentClick(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        File selectedFile;
        if (reportType.getExtension() == null) {
            // Initialize directory chooser with open message
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle(reportType.getOpenMessage());
            selectedFile = directoryChooser.showDialog(stage);
        } else {
            // Initialize file chooser with Report Type extension.
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle(reportType.getOpenMessage());
            FileChooser.ExtensionFilter ex1 = new FileChooser.ExtensionFilter(reportType.getExtensionName(), reportType.getExtension());
            fileChooser.getExtensionFilters().addAll(ex1);
            selectedFile = fileChooser.showOpenDialog(stage);
        }

        if (selectedFile != null) {
            projectPath.setText(String.valueOf(selectedFile));
            nextButton.setDisable(false);
        }
    }

    /**
     * Set Main discovery page gui controller for object passing.
     */
    public void setMainController(DiscoveryReportsMainController controller) {
        mainController = controller;
    }

    /**
     * Set R Script executable absolute path
     */
    public void setRScriptPath(Path rscriptPath) {
        this.rscriptPath = rscriptPath;
    }
}
