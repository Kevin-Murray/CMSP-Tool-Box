
package cmsp.tool.box.gui;

import cmsp.tool.box.enums.ErrorTypes;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

/**
 * Controller class for error page.
 */
public class ErrorPageController {


    @FXML public Button closeButton;
    @FXML private TextArea errorMessageField;

    /**
     * Handle close button click from error page.
     */
    @FXML
    void closeButtonClicked() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Set error message with input message.
     *
     * @param error Specified error type
     */
    @FXML
    public void setErrorMessage(ErrorTypes error) {
        errorMessageField.setText(error.getErrorMessage());
    }

    /**
     * Set error message with input message.
     *
     * @param string Specified error message
     */
    @FXML
    public void setErrorMessage(String string) {
        errorMessageField.setText(string);
    }

    /**
     * Copy error message to clipboard.
     */
    @FXML
    public void copyButtonClicked(ActionEvent actionEvent) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(errorMessageField.getText());
        clipboard.setContent(content);
    }
}
