package cmsp.tool.box.tasks;

import cmsp.tool.box.datamodel.TargetedDetailEntry;
import cmsp.tool.box.datamodel.TargetedMoleculeEntry;
import cmsp.tool.box.datamodel.TargetedRatioEntry;
import cmsp.tool.box.enums.ErrorTypes;
import cmsp.tool.box.enums.TargetedDetailsTypes;
import cmsp.tool.box.enums.TargetedMoleculeTypes;
import cmsp.tool.box.enums.TargetedSampleTypes;
import cmsp.tool.box.gui.TargetedReportsController;
import cmsp.tool.box.utils.MathUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Main processing task for targeted report formatting.
 */
public class TargetedReportsTask {

    static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(TargetedReportsController.class);
    private final Integer accThreshold;
    private final Path databasePath;
    private final Path documentDirectory;
    private final Path documentPath;
    private final Integer lodThreshold;
    private final Integer loqThreshold;
    private final Path quantitationPath;
    private final Path ratioResultPath;
    private final Path replicatePath;
    private List<String> errorMessages;
    private List<TargetedDetailEntry> replicateDetails;
    private List<LinkedHashMap<String, String>> resultAccuracy;
    private List<TargetedMoleculeEntry> resultMolecules;
    private List<LinkedHashMap<String, String>> resultPrecision;
    private List<TargetedRatioEntry> resultRatios;
    private List<LinkedHashMap<String, String>> resultSamples;


    /**
     * Constructor class.
     *
     * @param databasePath Path to application database
     * @param documentPath Path to Skyline document
     * @param lodThreshold Integer threshold for limit of detection
     * @param loqThreshold Integer threshold for limit of quant
     * @param accThreshold Integer threshold for calibrator accuracy
     */
    public TargetedReportsTask(Path databasePath, Path documentPath, Integer lodThreshold, Integer loqThreshold, Integer accThreshold) {

        this.databasePath = databasePath;
        this.documentPath = documentPath;
        this.documentDirectory = documentPath.getParent();
        this.lodThreshold = lodThreshold;
        this.loqThreshold = loqThreshold;
        this.accThreshold = accThreshold;

        this.quantitationPath = documentDirectory.resolve("Molecule Quantification.csv");
        this.ratioResultPath = documentDirectory.resolve("Molecule Ratio Results.csv");
        this.replicatePath = documentDirectory.resolve("Replicate Details.csv");

        errorMessages = new ArrayList<>();
    }

    /**
     * Get error message from stack trace for display in error message pop-up window.
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
     * Format quantitation limits style. Replace internal standard values with null and report with 3-significant figures.
     */
    private void adjustDilutionQuantLimits() {

        // Filter Unknown results and get average dilution factor
        List<TargetedRatioEntry> unknownRatios = resultRatios.stream().filter(entry -> entry.getSampleType().equals(TargetedSampleTypes.UNKNOWN.getLabel())).toList();
        ArrayList<Double> dilutionFactors = new ArrayList<>(unknownRatios.stream().map(TargetedRatioEntry::getDilutionFactor).toList());
        Double factorCorrection = MathUtils.calculateAverage(dilutionFactors);

        // Adjust limits and round to 3 decimal places
        for (TargetedMoleculeEntry molecule : resultMolecules) {

            if (molecule.getInternalStandard()) {
                molecule.setLimitOfDetection(null);
                molecule.setLowerLimitOfQuant(null);
                molecule.setUpperLimitOfQuant(null);
                molecule.setCalibrationCurve(null);
                molecule.setCalibrationFit(null);
                molecule.setConcentrationMultiplier(null);
                molecule.setUnit(null);
            } else {
                molecule.setLimitOfDetection(Double.parseDouble(String.format("%.3f", molecule.getLimitOfDetection() * factorCorrection)));
                molecule.setLowerLimitOfQuant(Double.parseDouble(String.format("%.3f", molecule.getLowerLimitOfQuant() * factorCorrection)));
                molecule.setUpperLimitOfQuant(Double.parseDouble(String.format("%.3f", molecule.getUpperLimitOfQuant() * factorCorrection)));
            }
        }
    }

    /**
     * Adjust expected concentration by concentration multipliers. This is not explicitly done by Skyline.
     */
    private void adjustExpectedConcentration() {

        for (TargetedRatioEntry entry : resultRatios) {

            if (entry.getExpectedConcentration() != null) {

                Double multiplier = resultMolecules.stream().filter(mol -> mol.getName().equals(entry.getMoleculeName())).map(TargetedMoleculeEntry::getConcentrationMultiplier).toList().get(0);

                if (multiplier != null) {
                    entry.setExpectedConcentration(entry.getExpectedConcentration() * multiplier);
                }
            }
        }
    }

    /**
     * Calculate molecule limit of detection (LOD) using measured blank concentrations and signal-to-noise threshold.
     */
    private void calculateMoleculeLOD() {

        // Filter blank results
        List<TargetedRatioEntry> blankRatios = resultRatios.stream().filter(entry -> entry.getSampleType().equals(TargetedSampleTypes.BLANK.getLabel())).toList();

        // For each molecule, independently calculate the limit of detection.
        for (TargetedMoleculeEntry resultMolecule : resultMolecules) {

            String molecule = resultMolecule.getName();

            ArrayList<Double> blankResults = new ArrayList<>(blankRatios.stream().filter(entry -> entry.getMoleculeName().equals(molecule)).map(TargetedRatioEntry::getMeasuredConcentration).filter(Objects::nonNull).toList());

            // If no results available or computed lod negative, replace with zero.
            if (blankResults.isEmpty()) {
                resultMolecule.setLimitOfDetection(0.0);
            } else {
                double lod = MathUtils.calculateAverage(blankResults) * lodThreshold;
                resultMolecule.setLimitOfDetection(Math.max(0.0, lod));
            }
        }
    }

    /**
     * Calculate limit of quantitation for each molecule. Limit of quantitation defined by accuracy and signal-to-noise
     * threshold.
     */
    private void calculateMoleculeLOQ() {

        // Expected concentrations do not explicitly account for concentration multipliers
        adjustExpectedConcentration();

        // Filter calibration standard results
        List<TargetedRatioEntry> standardRatios = resultRatios.stream().filter(entry -> entry.getSampleType().equals(TargetedSampleTypes.STANDARD.getLabel())).toList();

        for (TargetedMoleculeEntry resultMolecule : resultMolecules) {

            String molecule = resultMolecule.getName();

            // Calculate LOQ value from LOD value and user specified threshold.
            Double LOQ = resultMolecule.getLimitOfDetection() / lodThreshold * loqThreshold;

            List<Double> standardResults = standardRatios.stream().filter(entry -> entry.getMoleculeName().equals(molecule)).map(TargetedRatioEntry::getExpectedConcentration).toList();
            List<Double> standardAccuracy = standardRatios.stream().filter(entry -> entry.getMoleculeName().equals(molecule)).map(TargetedRatioEntry::getAccuracy).toList();

            if (standardAccuracy.isEmpty()) {
                resultMolecule.setLowerLimitOfQuant(0.0);
                resultMolecule.setUpperLimitOfQuant(0.0);
                continue;
            }

            Double minResult = Collections.max(standardResults);
            Double maxResult = Collections.min(standardResults);

            for (int i = 0; i < standardResults.size(); i++) {

                if (standardAccuracy.get(i) != null) {
                    if (standardResults.get(i) < minResult & standardResults.get(i) > LOQ & Math.abs(100.0 - standardAccuracy.get(i)) < accThreshold) {
                        minResult = standardResults.get(i);
                    }

                    if (standardResults.get(i) > maxResult & standardResults.get(i) > LOQ & Math.abs(100.0 - standardAccuracy.get(i)) < accThreshold) {
                        maxResult = standardResults.get(i);
                    }
                }
            }

            resultMolecule.setLowerLimitOfQuant(minResult);
            resultMolecule.setUpperLimitOfQuant(maxResult);
        }
    }

    /**
     * Confirm document file exists.
     *
     * @return True if document exists
     */
    private Boolean documentExists() {

        return new File(String.valueOf(documentPath)).exists();
    }

    /**
     * Export specified Skyline report to CSV.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void exportDocumentResults() throws IOException, InterruptedException {

        Runtime rt = Runtime.getRuntime();

        // Export ratio results
        String command1 = databasePath.resolve("SkylineRunner.exe") +
                " --in=" + documentPath +
                " --report-add=" + databasePath.resolve("skyline_report_molecule-ratio-result.skyr") +
                " --report-conflict-resolution=overwrite" +
                " --report-name=\"Molecule Ratio Results\"" +
                " --report-file=\"" + ratioResultPath + "\"";

        Process pr1 = rt.exec(command1);
        pr1.getOutputStream().close();
        pr1.waitFor();

        // Export molecule quantification
        String command2 = databasePath.resolve("SkylineRunner.exe") +
                " --in=" + documentPath +
                " --report-add=" + databasePath.resolve("skyline_report_molecule-quantification.skyr") +
                " --report-conflict-resolution=overwrite" +
                " --report-name=\"Molecule Quantification\"" +
                " --report-file=\"" + quantitationPath + "\"";

        Process pr2 = rt.exec(command2);
        pr2.getOutputStream().close();
        pr2.waitFor();

        // Export replicate details
        String command3 = databasePath.resolve("SkylineRunner.exe") +
                " --in=" + documentPath +
                " --report-add=" + databasePath.resolve("skyline_report_replicate-details.skyr") +
                " --report-conflict-resolution=overwrite" +
                " --report-name=\"Replicate Details\"" +
                " --report-file=\"" + replicatePath + "\"";

        Process pr3 = rt.exec(command3);
        pr3.getOutputStream().close();
        pr3.waitFor();
    }

    /**
     * Confirm document exports were created properly.
     *
     * @return True if both exports exist
     */
    private Boolean exportsExists() {

        return (new File(String.valueOf(ratioResultPath)).exists() &
                new File(String.valueOf(documentPath)).exists());
    }

    /**
     * Format accuracy results into table.
     */
    private void formatAccuracyTable() {

        List<TargetedRatioEntry> accuracyEntries = resultRatios.stream().filter(e ->
                e.getSampleType().equals(TargetedSampleTypes.STANDARD.getLabel()) || e.getSampleType().equals(TargetedSampleTypes.QC.getLabel())).toList();
        //List<TargetedRatioEntry> accuracyEntries = resultRatios.stream().filter(e -> e.getAccuracy() != null).toList();
        accuracyEntries = accuracyEntries.stream().filter(e -> !e.getInternalStandard()).toList();

        List<String> replicates = accuracyEntries.stream().map(TargetedRatioEntry::getSampleName).toList();
        Set<String> uniqueReplicates = new TreeSet<>(replicates);

        for (String replicate : uniqueReplicates) {

            List<TargetedRatioEntry> replicateEntries = accuracyEntries.stream().filter(e -> e.getSampleName().equals(replicate)).toList();

            LinkedHashMap<String, String> replicateRow = new LinkedHashMap<>();
            replicateRow.put("Sample Name", replicate);
            replicateRow.put("Sample Type", replicateEntries.get(0).getSampleType());

            for (TargetedRatioEntry e : replicateEntries) {

                String molecule = e.getMoleculeName();

                if (e.getExcludedFromCalibration() && e.getSampleType().equals(TargetedSampleTypes.STANDARD.getLabel())) {
                    replicateRow.put(molecule + " (Expected Concentration)", "NOT USED");
                    replicateRow.put(molecule + " (Measured Concentration)", "NOT USED");
                    replicateRow.put(molecule + " (Accuracy)", "NOT USED");
                } else if (e.getAccuracy() == null || e.getMeasuredConcentration() == null) {
                    replicateRow.put(molecule + " (Expected Concentration)", "NOT USED");
                    replicateRow.put(molecule + " (Measured Concentration)", "NOT USED");
                    replicateRow.put(molecule + " (Accuracy)", "NOT USED");
                } else {
                    replicateRow.put(molecule + " (Expected Concentration)", String.format("%.3f", e.getExpectedConcentration()));
                    replicateRow.put(molecule + " (Measured Concentration)", String.format("%.3f", e.getMeasuredConcentration()));
                    replicateRow.put(molecule + " (Accuracy)", e.getAccuracy() + "%");
                }
            }

            resultAccuracy.add(replicateRow);
        }
    }

    /**
     * Compute experimental precision from replicates with identical expected concentrations. All Quality Control samples
     * without an expected concentration will be grouped together.
     */
    private void formatPrecisionTable() {

        String[] types = {TargetedSampleTypes.STANDARD.getLabel(), TargetedSampleTypes.QC.getLabel()};
        List<String> molecules = resultMolecules.stream().filter(e -> !e.getInternalStandard()).map(TargetedMoleculeEntry::getName).toList();

        // For Standard (Calibrators) and QC samples, get unique concentrations and compute relative standard deviation
        for (String type : types) {

            List<TargetedRatioEntry> typeRatios = resultRatios.stream().filter(e -> e.getSampleType().equals(type)).toList();
            List<Double> concList = typeRatios.stream().map(TargetedRatioEntry::getExpectedConcentration).filter(Objects::nonNull).toList();

            List<Double> uniqueConc = new ArrayList<>();

            if (concList.size() == 0) {
                uniqueConc.add(null);
            } else {
                uniqueConc.addAll(new TreeSet<>(typeRatios.stream().map(TargetedRatioEntry::getExpectedConcentration).toList()));
            }

            // Index sample groups, e.g. - QC1, QC2, etc...
            int i = 1;

            // Loop through each unique concentration value, including null.
            for (Double conc : uniqueConc) {

                boolean valid = false;

                LinkedHashMap<String, String> replicateRow = new LinkedHashMap<>();
                replicateRow.put("Sample Group", TargetedSampleTypes.getEnumByString(type) + i);
                replicateRow.put("Sample Type", type);

                i = i + 1;

                for (String molecule : molecules) {

                    List<TargetedRatioEntry> ratios;

                    if (conc == null) {
                        ratios = typeRatios.stream().filter(e -> e.getMoleculeName().equals(molecule) && e.getExpectedConcentration() == null).toList();
                    } else {
                        ratios = typeRatios.stream().filter(e -> e.getMoleculeName().equals(molecule) && e.getExpectedConcentration().equals(conc)).toList();
                    }

                    // If there is more than one sample with expected concentration, compute variance.
                    if (ratios.size() > 1) {

                        valid = true;

                        List<String> names = ratios.stream().map(TargetedRatioEntry::getSampleName).toList();
                        ArrayList<Double> values = new ArrayList<>(ratios.stream().map(TargetedRatioEntry::getMeasuredConcentration).filter(Objects::nonNull).toList());
                        Double mean = MathUtils.calculateAverage(values);

                        Double rsd = MathUtils.calculateRelativeStandardDeviation(values);
                        Double sd = MathUtils.calculateStandardDeviation(values);
                        String nameString = String.join(" / ", names);

                        replicateRow.put("N Replicates", String.valueOf(names.size()));
                        replicateRow.put("Sample Names", nameString);
                        replicateRow.put(molecule + " (Average Concentration)", String.format("%.3f", mean));
                        replicateRow.put(molecule + " (Observed Std Dev)", String.format("%.3f", sd));
                        replicateRow.put(molecule + " (RSD)", String.format("%.1f", rsd) + "%");
                    }
                }

                if (valid) {
                    resultPrecision.add(replicateRow);
                }
            }
        }
    }

    /**
     * Format ratio results into table. Replace values less than limit of detection with LOD.
     */
    private void formatSampleTable() {

        List<TargetedRatioEntry> sampleEntries = resultRatios.stream().filter(e -> e.getSampleType().equals(TargetedSampleTypes.UNKNOWN.getLabel())).toList();
        sampleEntries = sampleEntries.stream().filter(e -> !e.getInternalStandard()).toList();

        List<String> replicates = sampleEntries.stream().map(TargetedRatioEntry::getSampleName).toList();
        Set<String> uniqueReplicates = new TreeSet<>(replicates);
        for (String replicate : uniqueReplicates) {

            List<TargetedRatioEntry> replicateEntries = sampleEntries.stream().filter(e -> e.getSampleName().equals(replicate)).toList();

            LinkedHashMap<String, String> replicateRow = new LinkedHashMap<>();
            replicateRow.put("Sample Name", replicate);

            for (TargetedRatioEntry e : replicateEntries) {

                String mol = e.getMoleculeName();
                Double conc = e.getMeasuredConcentration();

                TargetedMoleculeEntry molecule = resultMolecules.stream().filter(m -> m.getName().equals(mol)).toList().get(0);

                if (conc == null) {
                    replicateRow.put(mol, "<LOD");
                } else if (conc < 0.0) {
                    replicateRow.put(mol, "<LOD");
                } else if (conc < molecule.getLimitOfDetection()) {
                    replicateRow.put(mol, "<LOD");
                } else {
                    replicateRow.put(mol, String.format("%.3f", conc));
                }
            }

            resultSamples.add(replicateRow);
        }
    }

    /**
     * Format results into exportable reports.
     */
    public void generateReports() {

        this.resultPrecision = new ArrayList<>();
        formatPrecisionTable();

        setMoleculeUnits();
        calculateMoleculeLOD();
        calculateMoleculeLOQ();
        adjustDilutionQuantLimits();

        this.resultAccuracy = new ArrayList<>();
        formatAccuracyTable();

        this.resultSamples = new ArrayList<>();
        formatSampleTable();
    }

    /**
     * Get data for accuracy table.
     *
     * @return List of table data.
     */
    public ObservableList<Map<Integer, String>> getAccuracyTable() {

        ObservableList<Map<Integer, String>> tableData = FXCollections.observableArrayList();

        String[] keys = resultAccuracy.get(0).keySet().toArray(new String[0]);

        for (LinkedHashMap<String, String> resultMap : resultAccuracy) {

            Map<Integer, String> dataRow = new HashMap<>();

            for (int j = 0; j < resultMap.size(); j++) {
                dataRow.put(j, resultMap.get(keys[j]));
            }

            tableData.add(dataRow);
        }

        return tableData;
    }

    public String getErrorMessages() {
        return String.join("\n", errorMessages);
    }

    /**
     * Get table data for experimental precision results.
     *
     * @return Observable list of Mapped results.
     */
    public ObservableList<Map<Integer, String>> getPrecisionTable() {

        ObservableList<Map<Integer, String>> tableData = FXCollections.observableArrayList();

        String[] keys = resultPrecision.get(0).keySet().toArray(new String[0]);

        // For each row, add individual cell results
        for (LinkedHashMap<String, String> resultMap : resultPrecision) {

            Map<Integer, String> dataRow = new HashMap<>();

            for (int j = 0; j < resultMap.size(); j++) {
                dataRow.put(j, resultMap.get(keys[j]));
            }

            tableData.add(dataRow);
        }

        return tableData;
    }

    /**
     * Get replicate details lists.
     *
     * @return List of replicate entries.
     */
    public List<TargetedDetailEntry> getReplicateDetails() {

        return replicateDetails;
    }

    /**
     * Get sample color string for result summary table conditional formatting.
     *
     * @param header   Column header
     * @param value    Cell value
     * @param rowIndex Row Index
     * @return String of hexcode color value.
     */
    public String getSampleColor(String header, String value, int rowIndex) {

        double val;

        try {
            val = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            if (value.equals("<LOD")) {
                return ((rowIndex % 2) == 0) ? "FFD1D8" : "FFB6C1";
            } else {
                return ((rowIndex % 2) == 0) ? "FFFFFF" : "F6F6F6";
            }

        }

        //TargetedMoleculeEntry molecule = resultMolecules.stream().filter(e -> e.getName().equals(header)).toList().get(0);
        TargetedMoleculeEntry molecule = null;
        List<TargetedMoleculeEntry> moleculeList = resultMolecules.stream().filter(e -> e.getName().equals(header)).toList();
        if (!moleculeList.isEmpty()) {
            molecule = moleculeList.get(0);
        }

        if (molecule == null) {
            return ((rowIndex % 2) == 0) ? "FFFFFF" : "F6F6F6";
        }

        if (val < molecule.getLimitOfDetection()) {
            return ((rowIndex % 2) == 0) ? "FFD1D8" : "FFB6C1";
        } else if (val > molecule.getUpperLimitOfQuant() || val < molecule.getLowerLimitOfQuant()) {
            return ((rowIndex % 2) == 0) ? "FFF5C9" : "FFEB9C";
        } else {
            return ((rowIndex % 2) == 0) ? "FFFFFF" : "F6F6F6";
        }
    }

    /**
     * Get table data for result summary sheet.
     *
     * @return List of table data.
     */
    public ObservableList<Map<Integer, String>> getSampleTable() {

        ObservableList<Map<Integer, String>> tableData = FXCollections.observableArrayList();

        String[] keys = resultSamples.get(0).keySet().toArray(new String[0]);

        for (LinkedHashMap<String, String> resultSample : resultSamples) {

            Map<Integer, String> dataRow = new HashMap<>();

            for (int j = 0; j < resultSample.size(); j++) {
                dataRow.put(j, resultSample.get(keys[j]));
            }

            tableData.add(dataRow);
        }

        return tableData;
    }

    /**
     * Get list of targeted molecules.
     *
     * @return List of molecule entries.
     */
    public List<TargetedMoleculeEntry> getTargetedMoleculeEntry() {

        return resultMolecules;
    }

    public Boolean hasErrorMessage() {
        return !errorMessages.isEmpty();
    }

    /**
     * Make table columns for experimental accuracy table.
     *
     * @return List of table columns.
     */
    public ArrayList<TableColumn<Map<Integer, String>, String>> makeAccuracyTable() {

        ArrayList<TableColumn<Map<Integer, String>, String>> columns = new ArrayList<>();

        String[] keys = resultAccuracy.get(0).keySet().toArray(new String[0]);

        for (int i = 0; i < keys.length; i++) {

            String name = keys[i];
            Integer index = i;

            TableColumn<Map<Integer, String>, String> tableColumn = new TableColumn<>(name);
            tableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(index)));

            // Conditional formatting
            tableColumn.setCellFactory(tc -> new TableCell<>() {

                @Override
                protected void updateItem(final String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setText(item);
                        if (item.equals("NOT USED")) {
                            if ((getIndex() % 2) == 0) {
                                setStyle("-fx-background-color: #D9D9D9");
                            } else {
                                setStyle("-fx-background-color: #BFBFBF");
                            }
                        } else {
                            if ((getIndex() % 2) == 0) {
                                setStyle("-fx-background-color: #FFFFFF");
                            } else {
                                setStyle("-fx-background-color: #F6F6F6");
                            }

                            if (keys[index].contains("Accuracy")) {

                                double acc = Math.abs(100 - Double.parseDouble(item.split("%")[0]));
                                if (acc <= accThreshold) {
                                    if ((getIndex() % 2) == 0) {
                                        setStyle("-fx-background-color: #D9F2D0");
                                    } else {
                                        setStyle("-fx-background-color: #B4E5A2");
                                    }
                                } else if (acc > accThreshold & acc < accThreshold * 2) {
                                    if ((getIndex() % 2) == 0) {
                                        setStyle("-fx-background-color: #FFF5C9");
                                    } else {
                                        setStyle("-fx-background-color: #FFEB9C");
                                    }
                                } else {
                                    if ((getIndex() % 2) == 0) {
                                        setStyle("-fx-background-color: #FFD1D8");
                                    } else {
                                        setStyle("-fx-background-color: #FFB6C1");
                                    }
                                }
                            }
                        }
                    }
                }
            });

            columns.add(tableColumn);
        }

        return columns;
    }

    /**
     * Make table columns for molecule table (Calibration fit).
     *
     * @return List of table columns.
     */
    public ArrayList<TableColumn<TargetedMoleculeEntry, String>> makeMoleculeTable() {

        List<String> columnNames = Arrays.stream(TargetedMoleculeTypes.values()).map(TargetedMoleculeTypes::getTableName).toList();
        List<String> varNames = Arrays.stream(TargetedMoleculeTypes.values()).map(TargetedMoleculeTypes::getVariableName).toList();

        ArrayList<TableColumn<TargetedMoleculeEntry, String>> tableColumns = new ArrayList<>();

        for (int i = 0; i < columnNames.size(); i++) {

            TableColumn<TargetedMoleculeEntry, String> tableColumn = new TableColumn<>(columnNames.get(i));
            tableColumn.setCellValueFactory(new PropertyValueFactory<>(varNames.get(i)));

            tableColumns.add(tableColumn);
        }

        return tableColumns;
    }

    /**
     * Make Precision Table columns.
     *
     * @return List of table columns.
     */
    public ArrayList<TableColumn<Map<Integer, String>, String>> makePrecisionTable() {

        ArrayList<TableColumn<Map<Integer, String>, String>> columns = new ArrayList<>();

        String[] keys = resultPrecision.get(0).keySet().toArray(new String[0]);

        for (int i = 0; i < keys.length; i++) {

            String name = keys[i];
            Integer index = i;

            TableColumn<Map<Integer, String>, String> tableColumn = new TableColumn<>(name);
            tableColumn.setMaxWidth(500);
            tableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(index)));

            // Conditional cell formatting
            tableColumn.setCellFactory(tc -> new TableCell<>() {

                @Override
                protected void updateItem(final String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        setText(item);

                        if (keys[index].contains("RSD")) {

                            double rsd = Double.parseDouble(item.split("%")[0]);
                            if (rsd <= 15.0) {
                                if ((getIndex() % 2) == 0) {
                                    setStyle("-fx-background-color: #D9F2D0");
                                } else {
                                    setStyle("-fx-background-color: #B4E5A2");
                                }
                            } else if (rsd > 15.0 & rsd < 15.0 * 2) {
                                if ((getIndex() % 2) == 0) {
                                    setStyle("-fx-background-color: #FFF5C9");
                                } else {
                                    setStyle("-fx-background-color: #FFEB9C");
                                }
                            } else {
                                if ((getIndex() % 2) == 0) {
                                    setStyle("-fx-background-color: #FFD1D8");
                                } else {
                                    setStyle("-fx-background-color: #FFB6C1");
                                }
                            }
                        } else {
                            setText(item);
                            if ((getIndex() % 2) == 0) {
                                setStyle("-fx-background-color: #FFFFFF");
                            } else {
                                setStyle("-fx-background-color: #F6F6F6");
                            }
                        }
                    }
                }
            });
            columns.add(tableColumn);
        }
        return columns;
    }

    /**
     * Make table columns for replicate details table.
     *
     * @return
     */
    public ArrayList<TableColumn<TargetedDetailEntry, String>> makeReplicateDetailTable() {

        List<String> columnNames = Arrays.stream(TargetedDetailsTypes.values()).map(TargetedDetailsTypes::getTableName).toList();
        List<String> varNames = Arrays.stream(TargetedDetailsTypes.values()).map(TargetedDetailsTypes::getVarName).toList();

        ArrayList<TableColumn<TargetedDetailEntry, String>> tableColumns = new ArrayList<>();

        for (int i = 0; i < columnNames.size(); i++) {

            TableColumn<TargetedDetailEntry, String> tableColumn = new TableColumn<>(columnNames.get(i));
            tableColumn.setCellValueFactory(new PropertyValueFactory<>(varNames.get(i)));

            tableColumns.add(tableColumn);
        }

        return tableColumns;
    }

    /**
     * Make table columns for results summary table.
     *
     * @return List of table columns.
     */
    public ArrayList<TableColumn<Map<Integer, String>, String>> makeSampleTable() {

        ArrayList<TableColumn<Map<Integer, String>, String>> columns = new ArrayList<>();

        String[] keys = resultSamples.get(0).keySet().toArray(new String[0]);

        for (int i = 0; i < keys.length; i++) {

            String name = keys[i];
            Integer index = i;

            TableColumn<Map<Integer, String>, String> tableColumn = new TableColumn<>(name);
            tableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(index)));

            // Conditional cell formatting.
            tableColumn.setCellFactory(tc -> new TableCell<>() {

                @Override
                protected void updateItem(final String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        if (item.equals("<LOD")) {
                            setText(item);
                            if ((getIndex() % 2) == 0) {
                                setStyle("-fx-background-color: #FFD1D8");
                            } else {
                                setStyle("-fx-background-color: #FFB6C1");
                            }
                        } else if (outsideLOQ(keys[index], item)) {
                            setText(item);
                            if ((getIndex() % 2) == 0) {
                                setStyle("-fx-background-color: #FFF5C9");
                            } else {
                                setStyle("-fx-background-color: #FFEB9C");
                            }
                        } else {
                            setText(item);
                            if ((getIndex() % 2) == 0) {
                                setStyle("-fx-background-color: #FFFFFF");
                            } else {
                                setStyle("-fx-background-color: #F6F6F6");
                            }
                        }
                    }
                }
            });

            columns.add(tableColumn);
        }

        return columns;
    }

    /**
     * Determine if value for molecule is outside limit of quantitation.
     *
     * @param molecule String of molecule name
     * @param value    String of cell value.
     * @return Boolean true if outside of range.
     */
    private Boolean outsideLOQ(String molecule, String value) {

        Double lloq = 0.0;
        Double uloq = 0.0;

        if (resultMolecules.stream().anyMatch(entry -> entry.getName().equals(molecule))) {
            lloq = resultMolecules.stream().filter(entry -> entry.getName().equals(molecule)).map(TargetedMoleculeEntry::getLowerLimitOfQuant).toList().get(0);
            uloq = resultMolecules.stream().filter(entry -> entry.getName().equals(molecule)).map(TargetedMoleculeEntry::getUpperLimitOfQuant).toList().get(0);
        } else {
            return false;
        }

        try {
            return Double.parseDouble(value) < lloq | Double.parseDouble(value) > uloq;
        } catch (NumberFormatException e) {
            return false;
        }

    }

    /**
     * Check if precision results list is empty.
     *
     * @return
     */
    public Boolean precisionTableEmpty() {
        return resultPrecision.isEmpty();
    }

    /**
     * Import molecule quantification Skyline export.
     *
     * @return List of molecule entries.
     */
    private List<TargetedMoleculeEntry> readQuantitationExport() {

        List<TargetedMoleculeEntry> dataEntries = new ArrayList<>();

        // Create an instance of BufferedReader
        try (BufferedReader br = Files.newBufferedReader(this.quantitationPath, StandardCharsets.US_ASCII)) {

            String line = br.readLine();
            line = line.replace("\"", "");
            String[] header = line.split(",");

            line = br.readLine();

            // Loop through database line by line.
            while (line != null) {

                // TODO - evaluate a more efficient way to handle commas in comment field.
                // Parse strings.
                String[] attributes = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = attributes[i].replace("\"", "");
                }

                // Make data entry object and add to list.
                TargetedMoleculeEntry entry = new TargetedMoleculeEntry(header, attributes);
                dataEntries.add(entry);

                line = br.readLine();
            }

        } catch (IOException ioe) {
            errorMessages.add(getStackTraceString(ioe));
        }

        return dataEntries;
    }

    /**
     * Read ratio results Skyline export.
     *
     * @return List of ration entries.
     */
    private List<TargetedRatioEntry> readRatioExport() {

        List<TargetedRatioEntry> dataEntries = new ArrayList<>();

        // Create an instance of BufferedReader
        try (BufferedReader br = Files.newBufferedReader(this.ratioResultPath, StandardCharsets.UTF_8)) {

            String line = br.readLine();
            line = line.replace("\"", "");
            String[] header = line.split(",");

            line = br.readLine();

            // Loop through database line by line.
            while (line != null) {

                // TODO - evaluate a more efficient way to handle commas in comment field.
                // Parse strings.
                String[] attributes = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = attributes[i].replace("\"", "");
                }

                // Make data entry object and add to list.
                TargetedRatioEntry entry = new TargetedRatioEntry(header, attributes);
                dataEntries.add(entry);

                line = br.readLine();
            }

        } catch (IOException ioe) {
            errorMessages.add(getStackTraceString(ioe));
        }

        return dataEntries;
    }

    /**
     * Read replicate details Skyline export.
     *
     * @return List of detail entries.
     */
    private List<TargetedDetailEntry> readReplicateDetailsExport() {

        List<TargetedDetailEntry> dataEntries = new ArrayList<>();

        // Create an instance of BufferedReader
        try (BufferedReader br = Files.newBufferedReader(this.replicatePath, StandardCharsets.US_ASCII)) {

            String line = br.readLine();
            line = line.replace("\"", "");
            String[] header = line.split(",");

            line = br.readLine();

            // Loop through database line by line.
            while (line != null) {

                // TODO - evaluate a more efficient way to handle commas in comment field.
                // Parse strings.
                String[] attributes = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i] = attributes[i].replace("\"", "");
                }

                // Make data entry object and add to list.
                TargetedDetailEntry entry = new TargetedDetailEntry(header, attributes);
                dataEntries.add(entry);

                line = br.readLine();
            }

        } catch (IOException ioe) {
            errorMessages.add(getStackTraceString(ioe));
        }

        return dataEntries;
    }

    /**
     * Run task. Using selected Skyline document, export results and read to object lists.
     */
    public void run() {

        // Export reports from Skyline document
        if (documentExists()) {
            try {
                exportDocumentResults();
            } catch (IOException | InterruptedException e) {
                errorMessages.add(getStackTraceString(e));
                throw new RuntimeException(e);
            }
        } else {
            errorMessages.add("Skyline Document not found");
            return;
        }

        // Read exported
        if (!exportsExists()) {
            errorMessages.add("Skyline exports not found");
            return;
        }

        this.resultMolecules = readQuantitationExport();
        this.resultRatios = readRatioExport();
        this.replicateDetails = readReplicateDetailsExport();
    }

    /**
     * Set molecule units from ration results quantification units.
     */
    private void setMoleculeUnits() {

        for (TargetedMoleculeEntry entry : resultMolecules) {

            String unit = resultRatios.stream().filter(e -> e.getMoleculeName().equals(entry.getName())).map(TargetedRatioEntry::getUnit).toList().get(0);
            entry.setUnit(unit);
        }
    }

    /**
     * Determine if Skyline document has all necessary components to computed report.
     * TODO - more checks.
     *
     * @return Boolean true if all conditions met.
     */
    public ErrorTypes validDocument() {

        // No blank entries in document, can't compute LOD
        List<TargetedRatioEntry> blankReplicates = resultRatios.stream().filter(e -> e.getSampleType().equals(TargetedSampleTypes.BLANK.getLabel())).toList();
        if (blankReplicates.isEmpty()) {
            return ErrorTypes.TARGETBLANK;
        }

        // Duplicate molecule names detected
        List<String> moleculeNames = resultMolecules.stream().map(TargetedMoleculeEntry::getName).toList();
        Set<String> uniqueNames = new TreeSet<>(moleculeNames);
        if (uniqueNames.size() < moleculeNames.size()) {
            return ErrorTypes.TARGETUNIQUE;
        }

        return null;
    }
}
