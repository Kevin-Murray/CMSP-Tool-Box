
package cmsp.tool.box.gui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static cmsp.tool.box.utils.FileLocatorUtils.*;

/**
 * Controller class for application set-up page.
 */
public class SetUpPageController {


    @FXML private Button cancelButton;
    @FXML private Button openButton;
    @FXML private TextField pathField;
    @FXML private TextField rscriptPathField;

    /**
     * Handle cancel button click - Close window.
     */
    @FXML
    void cancelButtonClicked() {

        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Handle open button click - Close window.
     */
    @FXML
    void openButtonClicked() {

        Stage stage = (Stage) openButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Handles path selector button clicked.
     * Allow user to navigate to location of QC databases.
     */
    @FXML
    void pathSelectorButtonClicked() {

        // Initiate new DirectoryChooser object.
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open Folder");
        File file = directoryChooser.showDialog(null);

        if(file != null) pathField.setText(file.getAbsolutePath());
    }

    /**
     * Handles path selector button clicked.
     * Allow user to navigate to location of QC databases.
     */
    @FXML
    void rscriptSelectorButtonClicked() {

        // Initiate new DirectoryChooser object.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Rscript Executable Path...");
        FileChooser.ExtensionFilter ex1 = new FileChooser.ExtensionFilter("Executable", "*.exe");
        fileChooser.getExtensionFilters().addAll(ex1);

        File file = fileChooser.showOpenDialog(null);

        if(file != null) pathField.setText(file.getAbsolutePath());
    }

    /**
     * Get selected database directory path.
     */
    public Path getDatabaseFolder() {

        return Paths.get(pathField.getText());
    }

    /**
     * Get R Script executable path
     */
    public Path getRscriptPath() {

        return Paths.get(rscriptPathField.getText());
    }

    /**
     * Set controller database location field.
     */
    public void setDatabaseFolder(Path databaseFolder) {

        if(databaseFolder != null) {
            this.pathField.setText(databaseFolder.toString());
        }
    }

    /**
     * Set R Script executable path
     */
    public void setRscriptPath(Path path) {

        if(path != null) {
            this.rscriptPathField.setText(path.toString());
        } else {
            this.rscriptPathField.setText(locateRScriptPath());
        }
    }
}
