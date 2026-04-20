package cmsp.tool.box.gui;

import cmsp.tool.box.datamodel.SequenceSample;
import cmsp.tool.box.datamodel.SequenceSettings;
import cmsp.tool.box.enums.SequenceInstrumentTypes;
import cmsp.tool.box.enums.SequenceTableHeaders;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.util.*;

public class SequenceBuilderSampleController {

    public TextField groupNumTextField;
    public Button importButton;
    public TextField longFileTextField;
    public TextField maxCharacterTextField;
    public TextField prefixTextField;
    public TextField sampleNumTextField;
    public TextArea sampleTextArea;

    private ArrayList<SequenceSample> sequenceSamples;
    private ArrayList<ArrayList<SequenceSample>> batchList;
    private SequenceBuilderMainController mainController;
    private SequenceSettings settings;

    private int blankRuns;
    private int negRuns;
    private int calRuns;
    private int sstRuns;
    private int qcRuns;
    private int totalSampleSize;

    public void actionSamplePaste(KeyEvent keyEvent) {

        if ((keyEvent.isControlDown() && keyEvent.getCode() == KeyCode.V) ||
                (keyEvent.getCode() == KeyCode.ENTER) || (keyEvent.getCode() == KeyCode.DELETE) ||
                (keyEvent.getCode() == KeyCode.BACK_SPACE))  {

            List<CharSequence> entries = sampleTextArea.getParagraphs();
            List<String> samples = new ArrayList<>();
            List<String> groups = new ArrayList<>();

            for (CharSequence charSequence : entries) {

                String entry = charSequence.toString().trim();

                if (!entry.isEmpty()) {
                    samples.add(entry);
                    String[] nameGrp = entry.split("\\t");
                    groups.add((nameGrp.length > 1) ? nameGrp[1] : null);
                }
            }

            sampleNumTextField.setText(String.valueOf(samples.size()));

            if (!samples.isEmpty()) {
                String longest = Collections.max(samples, Comparator.comparing(String::length));
                int length = settings.getPrefix().length() + 5 + longest.length();
                longFileTextField.setText(String.valueOf(length));

                importButton.setDisable(false);
            } else {
                importButton.setDisable(true);
            }

            groups.removeIf(Objects::isNull);

            if (groups.isEmpty()) {
                groupNumTextField.setText("N/A");
            } else {
                HashSet<String> uniqueGroups = new HashSet<>(groups);
                groupNumTextField.setText(String.valueOf(uniqueGroups.size()));
            }
        }
    }

    public void initialize() {
        this.importButton.setDisable(true);

        this.sequenceSamples = new ArrayList<>();
        this.batchList = new ArrayList<>();
    }

    public void setMainController(SequenceBuilderMainController mainController) {
        this.mainController = mainController;
    }

    public void actionCreateSequenceButton() {

        List<CharSequence> entries = sampleTextArea.getParagraphs();
        Integer startPosition = settings.getSampleStartPosition();

        for (int i = 0; i < entries.size(); i++) {

            String entry = entries.get(i).toString().trim();
            if (!entry.isEmpty()) {
                String[] nameGrp = entry.split("\\t");
                String grp = (nameGrp.length > 1) ? nameGrp[1] : "";
                sequenceSamples.add(new SequenceSample(i, nameGrp[0], grp, getInstrumentType("Sample"), startPosition));
                startPosition = startPosition + 1;
            }
        }

        totalSampleSize = sequenceSamples.size();
        randomizeSampleList();
        createBatchSequence(1);
    }

    private void createBatchSequence(int batchNum) {

        ArrayList<SequenceSample> batchSequence = new ArrayList<>();

        int totalSampleNumber = settings.getTotalSampleNumber(totalSampleSize);
        int qcFrequency = (settings.getQcAnalyzeFrequency() == 0) ? settings.getQcSampleNum() : 10000000;
        int sstFrequency = (settings.getSstAnalyzeFrequency() == 0) ? settings.getSstSampleNum() : 1000000;

        blankRuns = 1;
        negRuns = 1;
        calRuns = 1;
        sstRuns = 1;
        qcRuns = 1;


        // Initialize batch with injections specified for beginning of sequence.
        if (settings.getCalRun()) {
            batchSequence.addAll(makeCalSampleList());
        }

        if (settings.getLibRun()) {
            batchSequence.addAll(makeLibSampleList());
        }

        if(settings.getSstRun() && settings.runSstAtTime("Beginning")) {
            batchSequence.addAll(makeSstSampleList());
        }

        if(settings.getQcRun() && settings.runQcAtTime("Beginning")) {
            batchSequence.addAll(makeQcSampleList());
        }

        // If running SST or QC samples at regular intervals, start sequence with samples. Update counter.
        int injCounter = 1;

        if (settings.getSstAnalyzeFrequency() == 0 && settings.getSstRun()) {
            batchSequence.addAll(makeSstSampleList());
            if (settings.isCountInjection()) {
                injCounter = injCounter + settings.getSstLevels();
            }
        }

        if (settings.getQcAnalyzeFrequency() == 0 && settings.getQcRun()) {
            batchSequence.addAll(makeQcSampleList());
            if (settings.isCountInjection()) {
                injCounter = injCounter + settings.getQcTotalSample();
            }
        }

        // Add real samples, QC, and SST to batch sequence in specified intervals.
        int qcCounter = 1;
        int sstCounter = 1;
        while (totalSampleNumber > 0 && !sequenceSamples.isEmpty()) {
            if (injCounter > totalSampleNumber) { // Break sequence larger than specified
                break;
            } else if (sstCounter > sstFrequency && settings.getSstRun()) { // Add SST at interval, reset counter
                batchSequence.addAll(makeSstSampleList());
                if (settings.isCountInjection()) {
                    injCounter = injCounter + settings.getSstLevels();
                }
                sstCounter = 1;
            } else if (qcCounter > qcFrequency && settings.getQcRun()) { // Add QC at interval, reset counter
                batchSequence.addAll(makeQcSampleList());
                if (settings.isCountInjection()) {
                    injCounter = injCounter + settings.getQcTotalSample();
                }
                qcCounter = 1;
            } else { // Add sample to sequence, increment counters
                batchSequence.add(sequenceSamples.get(0));
                sequenceSamples.remove(0);
                injCounter = injCounter + 1;
                qcCounter = qcCounter + 1;
                sstCounter = sstCounter + 1;
            }
        }

        // Augment batch with injections specified for middle of sequence.
        int middleIndex = getBatchListMiddleIndex(batchSequence);

        if (settings.getQcRun() && settings.runQcAtTime("Middle")) {
            batchSequence.addAll(middleIndex, makeQcSampleList());
        }

        if (settings.getSstRun() && settings.runSstAtTime("Middle")) {
            batchSequence.addAll(middleIndex, makeSstSampleList());
        }

        if (settings.getCalRun() && settings.runCalAtTime("Middle")) {
            batchSequence.addAll(middleIndex, makeCalSampleList());
        }

        if (settings.getLibRun() && settings.runLibAtTime("Middle")) {
            batchSequence.addAll(middleIndex, makeLibSampleList());
        }

        // Finalize batch with injections specified for end of sequence.
        // If running SST or QC samples at regular intervals, end sequence with samples.
        if (settings.getSstAnalyzeFrequency() == 0 && settings.getSstRun()) {
            batchSequence.addAll(makeSstSampleList());
        }

        if (settings.getQcAnalyzeFrequency() == 0 && settings.getQcRun()) {
            batchSequence.addAll(makeQcSampleList());
        }

        if (settings.getCalRun() && settings.runCalAtTime("End")) {
            batchSequence.addAll(makeCalSampleList());
        }

        if (settings.getLibRun() && settings.runLibAtTime("End")) {
            batchSequence.addAll(makeLibSampleList());
        }

        if(settings.getSstRun() && settings.runSstAtTime("End")) {
            batchSequence.addAll(makeSstSampleList());
        }

        if(settings.getQcRun() && settings.runQcAtTime("End")) {
            batchSequence.addAll(makeQcSampleList());
        }

        if(settings.getInstrument().equals("Thermo QExactive")) {
            batchSequence.add(new SequenceSample("Stop", getInstrumentType("Blank"), settings.getBlankStartPosition()));
        }

        // Add batch id and injection number for each sample in batch;
        for (int i = 0; i < batchSequence.size(); i++) {
            batchSequence.get(i).setBatch(batchNum);
            batchSequence.get(i).setInjectionNumber(i + 1);
        }

        // Add batch sequence to map
        batchList.add(batchSequence);

        // If there are still elements in sample list, create next batch of samples.
        if (!sequenceSamples.isEmpty()) {
            createBatchSequence(batchNum+1);
        } else {
            finalizeBatchSequence();
            createBatchTables();
        }
    }

    private void finalizeBatchSequence() {

        if (batchList.size() > 1) {
            for (int i = 0; i < batchList.size(); i++){
                ArrayList<SequenceSample> resetBatch = resetSampleVialPosition(batchList.get(i));
                batchList.set(i, updateFileNames(resetBatch, true));
            }
        } else {
            batchList.set(0, updateFileNames(batchList.get(0), false));
        }
    }

    private ArrayList<SequenceSample> updateFileNames(ArrayList<SequenceSample> batch, Boolean includeBatchNum) {
        for (SequenceSample sample : batch) {
            sample.setFileName(settings.getPrefix(), includeBatchNum);
            sample.setVialPosition(settings.convertIntToPosition(sample.getVialIndex()));
        }
        return batch;
    }

    private ArrayList<SequenceSample> resetSampleVialPosition(ArrayList<SequenceSample> batchList) {

        List<SequenceSample> sortedSamples = batchList.stream().filter(entry -> entry.getType().equals(getInstrumentType("Sample"))).sorted(Comparator.comparing(SequenceSample::getVialIndex)).toList();
        TreeMap<Integer, Integer> order = new TreeMap<>();

        for (int i = 0; i < sortedSamples.size(); i++) {
            order.put(sortedSamples.get(i).getVialIndex(), i);
        }

        for (SequenceSample entry : batchList) {
            if (entry.getType().equals(getInstrumentType("Sample"))) {
                int index = order.get(entry.getVialIndex()) + settings.getSampleStartPosition();
                entry.setVialIndex(index);
            }
        }

        return batchList;
    }

    private void createBatchTables() {

        LinkedHashMap<String, ArrayList<TableColumn<Map<Integer, String>, String>>> tableHeaderMap = new LinkedHashMap<>();
        LinkedHashMap<String, ObservableList<Map<Integer, String>>> tableDataMap = new LinkedHashMap<>();

        ArrayList<SequenceSample> masterList = new ArrayList<>();

        int batchNum = 1;
        for (ArrayList<SequenceSample> batch : batchList) {
            masterList.addAll(batch);
            ObservableList<Map<Integer, String>> displayData = FXCollections.observableArrayList();
            for (SequenceSample sample : batch) {
                displayData.add(sample.makeSampleMap(settings.getInstrument()));
            }
            tableHeaderMap.put("Batch" + batchNum, makeBatchTableColumns());
            tableDataMap.put("Batch" + batchNum, displayData);
            batchNum = batchNum + 1;
        }

        // Sort Preparation Details by Batch Number than Sample ID
        Comparator<SequenceSample> primaryComparator = Comparator.comparing(SequenceSample::getBatch);
        List<SequenceSample> sampleList = masterList.stream().filter(e -> e.getType().equals(getInstrumentType("Sample"))).sorted(primaryComparator.thenComparing(SequenceSample::getId)).toList();

        ObservableList<Map<Integer, String>> displayData = FXCollections.observableArrayList();
        for (SequenceSample sample : sampleList) {
            displayData.add(sample.makePrepMap());
        }
        tableHeaderMap.put("Preparation_Details", makePrepTableColumns());
        tableDataMap.put("Preparation_Details", displayData);

        mainController.setExpPrefix(settings.getPrefix());
        mainController.setInstrument(settings.getInstrument());
        mainController.setBatchData(tableHeaderMap, tableDataMap);
        mainController.updateBatchTable();

        // Close discovery processing wizard window
        Stage stage = (Stage) importButton.getScene().getWindow();
        stage.close();

    }

    private ArrayList<TableColumn<Map<Integer, String>, String>> makeBatchTableColumns() {

        ArrayList<TableColumn<Map<Integer, String>, String>> columns = new ArrayList<>();
        String[] keys = getInstrumentHeaders();

        // For each report key (column), make a new table column object.
        for (int i = 0; i < keys.length; i++) {

            String name = keys[i];
            Integer index = i;

            TableColumn<Map<Integer, String>, String> tableColumn = new TableColumn<>(name);
            tableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(index)));
            columns.add(tableColumn);
        }
        return columns;
    }

    private ArrayList<TableColumn<Map<Integer, String>, String>> makePrepTableColumns() {

        ArrayList<TableColumn<Map<Integer, String>, String>> columns = new ArrayList<>();
        String[] keys = {"Sample ID", "Sample Name", "Group", "Batch", "Vial Position", "Total Injection Number", "File Name"};

        // For each report key (column), make a new table column object.
        for (int i = 0; i < keys.length; i++) {

            String name = keys[i];
            Integer index = i;

            TableColumn<Map<Integer, String>, String> tableColumn = new TableColumn<>(name);
            tableColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().get(index)));
            columns.add(tableColumn);
        }
        return columns;
    }

    private int getBatchListMiddleIndex(ArrayList<SequenceSample> batchSequence) {

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < batchSequence.size(); i++) {
            if (batchSequence.get(i).getType().equals(getInstrumentType("Sample"))) {
                indices.add(i);
            }
        }
        return indices.get(indices.size() / 2);
    }

    private ArrayList<SequenceSample> makeSstSampleList() {

        ArrayList<SequenceSample> sstList = new ArrayList<>();

        int vialPosition = settings.getSstStartPosition();
        String name;
        for (int i = 1; i <= settings.getSstLevels(); i++) {
            name = "SST" + i + "_" + sstRuns;
            sstList.add(new SequenceSample(name, getInstrumentType("SST"), vialPosition));
            sstRuns = sstRuns + 1;
            vialPosition = vialPosition + 1;
        }
        return sstList;
    }

    private ArrayList<SequenceSample> makeQcSampleList() {

        ArrayList<SequenceSample> qcList = new ArrayList<>();

        if (settings.getBlankRun() && settings.getBlankNumBQc() != 0) {
            qcList.addAll(addBlankSample(settings.getBlankNumBQc()));
        }

        if (settings.getNegRun() && settings.getNegNumBQc() != 0) {
            qcList.addAll(addNegSample(settings.getNegNumBQc()));
        }

        int vialPosition = settings.getQcStartPosition();
        String name;
        for (int i = 1; i <= settings.getQcLevels(); i++) {
            name = "QC" + i + "_" + qcRuns;
            qcList.add(new SequenceSample(name, getInstrumentType("QC"), vialPosition));
            vialPosition = vialPosition + 1;
        }
        qcRuns = qcRuns + 1;

        if (settings.getBlankRun() && settings.getBlankNumAQc() != 0) {
            qcList.addAll(addBlankSample(settings.getBlankNumAQc()));
        }

        if (settings.getNegRun() && settings.getNegNumAQc() != 0) {
            qcList.addAll(addNegSample(settings.getNegNumAQc()));
        }

        return qcList;
    }

    private ArrayList<SequenceSample> makeCalSampleList() {

        ArrayList<SequenceSample> calList = new ArrayList<>();

        if (settings.getBlankRun() && settings.getBlankNumBCal() != 0) {
            calList.addAll(addBlankSample(settings.getBlankNumBCal()));
        }

        if (settings.getNegRun() && settings.getNegNumBCal() != 0) {
            calList.addAll(addNegSample(settings.getNegNumBCal()));
        }

        int vialPosition = settings.getCalStartPosition();
        String name;
        for (int j = settings.getCalBtmLevel(); j >= settings.getCalTopLevel(); j--) {
            name = "Cal" + String.format("%02d", j) + "_" + calRuns;
            calList.add(new SequenceSample(name, getInstrumentType("Cal"), vialPosition));
            vialPosition = vialPosition + 1;
        }
        calRuns = calRuns + 1;

        if (settings.getBlankRun() && settings.getBlankNumACal() != 0) {
            calList.addAll(addBlankSample(settings.getBlankNumACal()));
        }

        if (settings.getNegRun() && settings.getNegNumACal() != 0) {
            calList.addAll(addNegSample(settings.getNegNumACal()));
        }

        return calList;
    }

    private ArrayList<SequenceSample> makeLibSampleList() {

        ArrayList<SequenceSample> libList = new ArrayList<>();

        if (settings.getBlankRun() && settings.getBlankNumBCal() != 0) {
            libList.addAll(addBlankSample(settings.getBlankNumBCal()));
        }

        if (settings.getNegRun() && settings.getNegNumBCal() != 0) {
            libList.addAll(addNegSample(settings.getNegNumBCal()));
        }

        int vialPosition = settings.getCalStartPosition();
        String name;
        if (settings.isDiaLibraryExperiment()) {
            String[] polarity = {"p", "n"};
            String[] levels = {"070-310", "310-550", "550-790", "790-1030"};
            for (String p : polarity) {
                for (String l : levels) {
                    name = "DIA" + p + "_" + l;
                    libList.add(new SequenceSample(name, getInstrumentType("Cal"), vialPosition));
                    calRuns = calRuns + 1;
                }
                vialPosition = vialPosition + 1;
            }

        } else {

        }

        if (settings.getBlankRun() && settings.getBlankNumACal() != 0) {
            libList.addAll(addBlankSample(settings.getBlankNumACal()));
        }

        if (settings.getNegRun() && settings.getNegNumACal() != 0) {
            libList.addAll(addNegSample(settings.getNegNumACal()));
        }

        return libList;
    }

    private ArrayList<SequenceSample> addBlankSample(int num) {
        ArrayList<SequenceSample> blankList = new ArrayList<>();
        String name;
        for (int i = 1; i <= num; i++) {
            name = "Blank_" + String.format("%02d", blankRuns);
            blankList.add(new SequenceSample(name, getInstrumentType("Blank"), settings.getBlankStartPosition()));
            blankRuns = blankRuns + 1;
        }
        return blankList;
    }

    private ArrayList<SequenceSample> addNegSample(int num) {
        ArrayList<SequenceSample> negList = new ArrayList<>();
        String name;
        for (int i = 1; i <= num; i++) {
            name = "Neg_" + String.format("%02d", negRuns);
            negList.add(new SequenceSample(name, getInstrumentType("Neg"), settings.getNegStartPosition()));
            negRuns = negRuns + 1;
        }
        return negList;
    }

    private void randomizeSampleList() {

        switch (settings.getSampleRandomize()) {
            case "Order" -> randomizeSamplesByOrder();
            case "Group" -> randomizeSamplesByGroups();
        }
    }

    private void randomizeSamplesByOrder() {
        Collections.shuffle(sequenceSamples);
    }

    private void randomizeSamplesByGroups() {
    }

    public void setSequenceSettings(SequenceSettings settings) {
        this.settings = settings;
        this.prefixTextField.setText(settings.getPrefix());
    }

    public String getInstrumentType(String type) {

        return switch (settings.getInstrument()) {
            case "Agilent 6495C" -> SequenceInstrumentTypes.A6495C.getType(type);
            case "Agilent 7200" -> SequenceInstrumentTypes.A7200.getType(type);
            case "Sciex 5500" -> SequenceInstrumentTypes.S5500.getType(type);
            case "Sciex 6500" -> SequenceInstrumentTypes.S6500.getType(type);
            case "Thermo Eclipse" -> SequenceInstrumentTypes.ECLIPSE.getType(type);
            case "Thermo Fusion" -> SequenceInstrumentTypes.FUSION.getType(type);
            case "Thermo QExactive" -> SequenceInstrumentTypes.QEXACTIVE.getType(type);
            default -> "";
        };
    }

    public String[] getInstrumentHeaders() {

        return switch (settings.getInstrument()) {
            case "Agilent 7200" -> SequenceTableHeaders.A7200.getHeaders();
            case "Thermo Fusion" -> SequenceTableHeaders.FUSION.getHeaders();
            case "Thermo Eclipse" -> SequenceTableHeaders.ECLIPSE.getHeaders();
            case "Thermo QExactive" -> SequenceTableHeaders.QEXACTIVE.getHeaders();
            default -> new String[] {};
        };
    }
}
