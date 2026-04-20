package cmsp.tool.box.gui;

import cmsp.tool.box.Launcher;
import cmsp.tool.box.datamodel.SequenceSettings;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SequenceBuilderParametersController {

    // FXML objects
    public Rectangle batchBox;
    public RadioButton batchDistEqualRadio;
    public RadioButton batchDistMaxRadio;
    public Label batchLabel;
    public Spinner<Integer> batchNSpinner;
    public ChoiceBox<String> batchNumberChoice;
    public Label blankACalLabel;
    public Spinner<Integer> blankAfterCalSpinner;
    public Spinner<Integer> blankAfterQcSpinner;
    public Label blankBCalLabel;
    public Spinner<Integer> blankBeforeCalSpinner;
    public Spinner<Integer> blankBeforeQcSpinner;
    public CheckBox blankCheckBox;
    public ChoiceBox<String> calAnalyzeChoice;
    public Spinner<Integer> calBottomLevelSpinner;
    public Rectangle calBox;
    public CheckBox calCheckBox;
    public Spinner<Integer> calTopLevelSpinner;
    public TextField expTextField;
    public ChoiceBox<String> expTypeChoiceBox;
    public Button importButton;
    public ChoiceBox<String> instrumentChoiceBox;
    public ChoiceBox<String> libAnalyzeChoice;
    public Rectangle libBox;
    public CheckBox libCheckBox;
    public ChoiceBox<String> libSchemaChoice;
    public Label negACalLabel;
    public Spinner<Integer> negAfterCalSpinner;
    public Spinner<Integer> negAfterQcSpinner;
    public Label negBCalLabel;
    public Spinner<Integer> negBeforeCalSpinner;
    public Spinner<Integer> negBeforeQcSpinner;
    public Rectangle negBox;
    public CheckBox negCheckBox;
    public TextField piTextField;
    public ChoiceBox<String> qcAnalyzeChoice;
    public Rectangle qcBox;
    public CheckBox qcCheckBox;
    public Button qcLevelNamesButton;
    public Spinner<Integer> qcLevelsSpinner;
    public Spinner<Integer> qcNSpinner;
    public TextField researchTextField;
    public Button resetButton;
    public ChoiceBox<String> sampleAnalyzeChoice;
    public Rectangle sampleBox;
    public Label sampleLabel;
    public ChoiceBox<String> sampleRandomizeChoice;
    public Rectangle solventBox;
    public ChoiceBox<String> sstAnalyze;
    public Rectangle sstBox;
    public CheckBox sstCheckBox;
    public Button sstLevelNamesButton;
    public Spinner<Integer> sstNSpinner;
    public Spinner<Integer> sstNumLevels;
    public ChoiceBox<String> startPosition;

    // Class objects
    private SequenceBuilderMainController mainController;

    public void actionCalCheckBox() {
        calBox.setVisible(!calCheckBox.isSelected());
    }

    public void actionExperimentTypeBox() {
        if (globalParametersSet()) {
            enableSequenceParameters();
            importButton.setDisable(false);
        } else {
            importButton.setDisable(true);
        }
    }

    public void actionImportButton(ActionEvent event) {

        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String formattedDate = currentDate.format(formatter);

        String pi = (piTextField.getText().isEmpty()) ? "cmsptc" : piTextField.getText();
        String exp = (expTextField.getText().isEmpty()) ? "00000" : expTextField.getText();

        String prefix = (researchTextField.getText().isEmpty()) ? pi + "_" + exp + "_" + formattedDate :
                pi + "_" + researchTextField.getText() + "_" + exp + "_" + formattedDate;

        SequenceSettings settings = new SequenceSettings(prefix, expTypeChoiceBox.getValue(), instrumentChoiceBox.getValue(),
                startPosition.getValue(), batchNumberChoice.getValue(), batchNSpinner.getValue(), batchDistMaxRadio.isSelected(),
                sampleRandomizeChoice.getValue(), sampleAnalyzeChoice.getValue(), calCheckBox.isSelected(), calTopLevelSpinner.getValue(),
                calBottomLevelSpinner.getValue(), calAnalyzeChoice.getValue(), libCheckBox.isSelected(), libSchemaChoice.getValue(),
                libAnalyzeChoice.getValue(), qcCheckBox.isSelected(), qcLevelsSpinner.getValue(), qcAnalyzeChoice.getValue(), qcNSpinner.getValue(),
                sstCheckBox.isSelected(), sstNumLevels.getValue(), sstAnalyze.getValue(), sstNSpinner.getValue(), negCheckBox.isSelected(),
                negBeforeCalSpinner.getValue(), negAfterCalSpinner.getValue(), negBeforeQcSpinner.getValue(), negAfterQcSpinner.getValue(),
                blankCheckBox.isSelected(), blankBeforeCalSpinner.getValue(), blankAfterCalSpinner.getValue(), blankBeforeQcSpinner.getValue(),
                blankAfterQcSpinner.getValue());

        try {
            // Get window design for setting page.
            FXMLLoader fxmlLoader = new FXMLLoader(Launcher.class.getResource("SequenceBuilderImport.fxml"));
            Parent root = fxmlLoader.load();

            // Update controller class with current database location.
            SequenceBuilderSampleController controller = fxmlLoader.getController();
            controller.setMainController(mainController);
            controller.setSequenceSettings(settings);


            // Launch pop-up window.
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/cmsp/tool/box/styleGuide.css")).toString());
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            // TODO - better error handling.
            e.printStackTrace();
        }
    }

    public void actionInstrumentChoiceBox() {

        if (instrumentChoiceBox.getSelectionModel().getSelectedItem().isEmpty()) {
            return;
        }

        String[] OPT1 = {"RA1", "RB1", "RC1", "RD1", "RE1", "BA1", "BB1", "BC1", "BD1", "BE1", "GA1", "GB1", "GC1", "GD1", "GE1"};
        String[] OPT2 = {"P1-A1", "P1-B1", "P1-C1", "P2-A1", "P2-B1", "P2-C1", "P3-A1", "P3-B1", "P3-C1", "P4-A1", "P4-B1", "P4-C1"};
        String[] OPT3 = {"1", "11", "21", "31", "41", "51"};

        String instrument = instrumentChoiceBox.getSelectionModel().getSelectedItem();
        String[] options = switch (instrument) {
            case "Thermo Eclipse", "Thermo Fusion", "Thermo QExactive" -> OPT1;
            case "Agilent 6495C" -> OPT2;
            default -> OPT3;
        };

        startPosition.getItems().clear();
        startPosition.getItems().addAll(new ArrayList<>(List.of(options)));

        if (globalParametersSet()) {
            enableSequenceParameters();
            importButton.setDisable(false);
        } else {
            importButton.setDisable(true);
        }
    }

    public void actionLibCheckBox() {
        libBox.setVisible(!libCheckBox.isSelected());
    }

    public void actionNegCheckBox() {
        negBox.setVisible(!negCheckBox.isSelected());
    }

    public void actionQcCheckBox() {
        qcBox.setVisible(!qcCheckBox.isSelected());
    }

    public void actionSampleSpacingBox() {

        if (qcAnalyzeChoice.getSelectionModel().isEmpty() || sstAnalyze.getSelectionModel().isEmpty()) {
            return;
        }

        qcNSpinner.setDisable(!qcAnalyzeChoice.getSelectionModel().getSelectedItem().contains("N "));
        sstNSpinner.setDisable(!sstAnalyze.getSelectionModel().getSelectedItem().contains("N "));
    }

    public void actionSolventCheckBox() {
        solventBox.setVisible(!blankCheckBox.isSelected());
    }

    public void actionSstCheckBox() {
        sstBox.setVisible(!sstCheckBox.isSelected());
    }

    public void actionStartPositionBox() {
        if (globalParametersSet()) {
            enableSequenceParameters();
            importButton.setDisable(false);
        } else {
            importButton.setDisable(true);
        }
    }

    private void enableSequenceParameters() {

        batchLabel.setDisable(false);
        sampleLabel.setDisable(false);
        qcCheckBox.setDisable(false);
        sstCheckBox.setDisable(false);
        negCheckBox.setDisable(false);
        blankCheckBox.setDisable(false);

        qcCheckBox.setSelected(true);
        sstCheckBox.setSelected(true);
        negCheckBox.setSelected(true);
        blankCheckBox.setSelected(true);

        batchBox.setVisible(false);
        sampleBox.setVisible(false);
        qcBox.setVisible(false);
        sstBox.setVisible(false);
        negBox.setVisible(false);
        solventBox.setVisible(false);

        if (expTypeChoiceBox.getSelectionModel().getSelectedItem().equals("Targeted")) {
            setTargetedParameters();
        } else {
            setDiscoveryParameters();
        }
    }

    private Boolean globalParametersSet() {
        return !expTypeChoiceBox.getSelectionModel().isEmpty() &
                !instrumentChoiceBox.getSelectionModel().isEmpty() &
                !startPosition.getSelectionModel().isEmpty();
    }

    /**
     * Initialize GUI.
     * Set instrument, position, and analysis type choiceBoxes
     */
    public void initialize() {

        // Mandatory parameters
        String[] inst = {"Agilent 6495C", "Agilent 7200", "Sciex 5500", "Sciex 6500", "Thermo Eclipse", "Thermo Fusion", "Thermo QExactive"};
        instrumentChoiceBox.getItems().addAll(new ArrayList<>(List.of(inst)));

        String[] type = {"Discovery", "Targeted"};
        expTypeChoiceBox.getItems().addAll(new ArrayList<>(List.of(type)));

        // Batch parameters
        String[] batchInj = {"Samples", "Injections"};
        batchNumberChoice.getItems().addAll(new ArrayList<>(List.of(batchInj)));
        batchNumberChoice.getSelectionModel().select(0);

        SpinnerValueFactory<Integer> valueFactoryBatchN = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 300);
        valueFactoryBatchN.setValue(50);
        batchNSpinner.setValueFactory(valueFactoryBatchN);
        batchNSpinner.editorProperty().get().setAlignment(Pos.CENTER);
        batchNSpinner.setEditable(true);

        ToggleGroup toggleGroup = new ToggleGroup();
        batchDistEqualRadio.setToggleGroup(toggleGroup);
        batchDistMaxRadio.setToggleGroup(toggleGroup);
        batchDistEqualRadio.setSelected(true);

        // Sample parameters
        String[] sampleR = {"Do not randomize", "Order", "Group"};
        sampleRandomizeChoice.getItems().addAll(new ArrayList<>(List.of(sampleR)));
        sampleRandomizeChoice.getSelectionModel().select(1);

        String[] sampleA = {"Singlet", "Doublet", "Triplet"};
        sampleAnalyzeChoice.getItems().addAll(new ArrayList<>(List.of(sampleA)));
        sampleAnalyzeChoice.getSelectionModel().select(0);

        // Calibrator parameters
        SpinnerValueFactory<Integer> valueFactoryCalTopLevel = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30);
        valueFactoryCalTopLevel.setValue(1);
        SpinnerValueFactory<Integer> valueFactoryCalBtmLevel = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30);
        valueFactoryCalBtmLevel.setValue(10);
        calTopLevelSpinner.setValueFactory(valueFactoryCalTopLevel);
        calBottomLevelSpinner.setValueFactory(valueFactoryCalBtmLevel);
        calTopLevelSpinner.editorProperty().get().setAlignment(Pos.CENTER);
        calBottomLevelSpinner.editorProperty().get().setAlignment(Pos.CENTER);

        String[] sequenceStart = {"Beginning Only", "Beginning-End", "Beginning-Middle-End"};
        calAnalyzeChoice.getItems().addAll(new ArrayList<>(List.of(sequenceStart)));
        calAnalyzeChoice.getSelectionModel().select(0);

        // Library parameters
        String[] librarySchema = {"Metabolomics DIA - 4 windows - pos/neg", "Proteomics GPF Library"};
        libSchemaChoice.getItems().addAll(new ArrayList<>(List.of(librarySchema)));
        libSchemaChoice.getSelectionModel().select(0);
        libAnalyzeChoice.getItems().addAll(sequenceStart);
        libAnalyzeChoice.getSelectionModel().select(0);

        // QC parameters
        SpinnerValueFactory<Integer> valueFactoryQcLevel = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5);
        qcLevelsSpinner.setValueFactory(valueFactoryQcLevel);
        qcLevelsSpinner.editorProperty().get().setAlignment(Pos.CENTER);

        String[] sampleDist = {"Every N samples", "Beginning-End", "Middle-End", "Beginning-Middle-End"};
        qcAnalyzeChoice.getItems().addAll(new ArrayList<>(List.of(sampleDist)));
        qcAnalyzeChoice.getSelectionModel().select(0);

        SpinnerValueFactory<Integer> valueFactoryQcN = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50);
        valueFactoryQcN.setValue(5);
        qcNSpinner.setValueFactory(valueFactoryQcN);
        qcNSpinner.editorProperty().get().setAlignment(Pos.CENTER);

        // SST parameters
        SpinnerValueFactory<Integer> valueFactorySstLevel = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5);
        sstNumLevels.setValueFactory(valueFactorySstLevel);
        sstNumLevels.editorProperty().get().setAlignment(Pos.CENTER);

        sstAnalyze.getItems().addAll(new ArrayList<>(List.of(sampleDist)));
        sstAnalyze.getSelectionModel().select(0);

        SpinnerValueFactory<Integer> valueFactorySstN = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50);
        valueFactorySstN.setValue(10);
        sstNSpinner.setValueFactory(valueFactorySstN);
        sstNSpinner.editorProperty().get().setAlignment(Pos.CENTER);

        // Negative control parameters
        SpinnerValueFactory<Integer> valueFactoryNegCalBefore = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10);
        SpinnerValueFactory<Integer> valueFactoryNegCalAfter = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10);
        SpinnerValueFactory<Integer> valueFactoryNegQcBefore = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10);
        SpinnerValueFactory<Integer> valueFactoryNegQcAfter = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10);
        valueFactoryNegCalBefore.setValue(1);
        valueFactoryNegCalAfter.setValue(0);
        valueFactoryNegQcBefore.setValue(1);
        valueFactoryNegQcAfter.setValue(0);

        negBeforeCalSpinner.setValueFactory(valueFactoryNegCalBefore);
        negAfterCalSpinner.setValueFactory(valueFactoryNegCalAfter);
        negBeforeQcSpinner.setValueFactory(valueFactoryNegQcBefore);
        negAfterQcSpinner.setValueFactory(valueFactoryNegQcAfter);
        negBeforeCalSpinner.editorProperty().get().setAlignment(Pos.CENTER);
        negAfterCalSpinner.editorProperty().get().setAlignment(Pos.CENTER);
        negBeforeQcSpinner.editorProperty().get().setAlignment(Pos.CENTER);
        negAfterQcSpinner.editorProperty().get().setAlignment(Pos.CENTER);

        // Solvent parameters
        SpinnerValueFactory<Integer> valueFactoryBlankCalBefore = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10);
        SpinnerValueFactory<Integer> valueFactoryBlankCalAfter = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10);
        SpinnerValueFactory<Integer> valueFactoryBlankQcBefore = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10);
        SpinnerValueFactory<Integer> valueFactoryBlankQcAfter = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10);
        valueFactoryBlankCalBefore.setValue(1);
        valueFactoryBlankCalAfter.setValue(3);
        valueFactoryBlankQcBefore.setValue(0);
        valueFactoryBlankQcAfter.setValue(1);

        blankBeforeCalSpinner.setValueFactory(valueFactoryBlankCalBefore);
        blankAfterCalSpinner.setValueFactory(valueFactoryBlankCalAfter);
        blankBeforeQcSpinner.setValueFactory(valueFactoryBlankQcBefore);
        blankAfterQcSpinner.setValueFactory(valueFactoryBlankQcAfter);
        blankBeforeCalSpinner.editorProperty().get().setAlignment(Pos.CENTER);
        blankAfterCalSpinner.editorProperty().get().setAlignment(Pos.CENTER);
        blankBeforeQcSpinner.editorProperty().get().setAlignment(Pos.CENTER);
        blankAfterQcSpinner.editorProperty().get().setAlignment(Pos.CENTER);

        importButton.setDisable(true);
    }

    public void setTargetedParameters() {

        calCheckBox.setDisable(false);
        calCheckBox.setSelected(true);
        calBox.setVisible(false);
        libCheckBox.setDisable(true);
        libCheckBox.setSelected(false);
        libBox.setVisible(true);

        calAnalyzeChoice.getSelectionModel().select(0);
        qcAnalyzeChoice.getSelectionModel().select(2);
        sstAnalyze.getSelectionModel().select(0);
        qcLevelsSpinner.getValueFactory().setValue(3);
        blankBeforeQcSpinner.getValueFactory().setValue(0);
        blankAfterQcSpinner.getValueFactory().setValue(1);
        negBeforeQcSpinner.getValueFactory().setValue(1);
        negAfterQcSpinner.getValueFactory().setValue(0);
        blankAfterCalSpinner.getValueFactory().setValue(3);
        negAfterCalSpinner.getValueFactory().setValue(0);

        blankBCalLabel.setText("Before Cal");
        blankACalLabel.setText("After Cal");
        negBCalLabel.setText("Before Cal");
        negACalLabel.setText("After Cal");
    }

    public void setDiscoveryParameters() {

        libCheckBox.setDisable(false);
        libCheckBox.setSelected(true);
        libBox.setVisible(false);
        calCheckBox.setDisable(true);
        calCheckBox.setSelected(false);
        calBox.setVisible(true);

        calAnalyzeChoice.getSelectionModel().select(0);
        qcAnalyzeChoice.getSelectionModel().select(0);
        sstAnalyze.getSelectionModel().select(1);
        qcLevelsSpinner.getValueFactory().setValue(1);
        blankBeforeQcSpinner.getValueFactory().setValue(0);
        blankAfterQcSpinner.getValueFactory().setValue(0);
        negBeforeQcSpinner.getValueFactory().setValue(0);
        negAfterQcSpinner.getValueFactory().setValue(0);
        blankAfterCalSpinner.getValueFactory().setValue(0);
        negAfterCalSpinner.getValueFactory().setValue(0);

        blankBCalLabel.setText("Before Lib");
        blankACalLabel.setText("After Lib");
        negBCalLabel.setText("Before Lib");
        negACalLabel.setText("After Lib");
    }


    public void setMainController(SequenceBuilderMainController mainController) {
        this.mainController = mainController;
    }
}
