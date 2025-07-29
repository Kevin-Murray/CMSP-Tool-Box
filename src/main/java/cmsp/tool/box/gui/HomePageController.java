package cmsp.tool.box.gui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Controller class for application homepage.
 * Handles switching between module stages.
 */
public class HomePageController {

    private Stage stage;
    private Scene scene;
    private Parent root;

    /**
     * Change stage to Quick QC module
     *
     * @param event MouseEvent mouse click
     * @throws IOException
     */
    public void launchModule_QQC(MouseEvent event) throws IOException {
        
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/cmsp/tool/box/QuickQCPageGui.fxml"));
        root = fxmlLoader.load();

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/cmsp/tool/box/styleGuide.css")).toString());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Change stage to Targeted Reports module.
     *
     * @param event MouseEvent mouse click
     * @throws IOException
     */
    public void launchModule_targetedReports(MouseEvent event) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/cmsp/tool/box/TargetedReportsPage.fxml"));
        root = fxmlLoader.load();

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/cmsp/tool/box/styleGuide.css")).toString());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Change stage to Discovery Reports module.
     *
     * @param event MouseEvent mouse click
     * @throws IOException
     */
    public void launchModule_discoverReports(MouseEvent event) throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/cmsp/tool/box/DiscoveryReportsMainPage.fxml"));
        root = fxmlLoader.load();

        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/cmsp/tool/box/styleGuide.css")).toString());
        stage.setScene(scene);
        stage.show();
    }
}
