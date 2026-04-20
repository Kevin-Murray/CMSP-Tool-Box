package cmsp.tool.box.datamodel;

import java.util.HashMap;
import java.util.Map;

public class SequenceSample {

    private Integer id;
    private String name;
    private String group;
    private String type;
    private Integer vialIndex;
    private String vialPosition;
    private Integer batch;
    private Integer injectionNumber;
    private String fileName;

    public SequenceSample(Integer id, String name, String group, String type, Integer vialIndex) {
        this.id = id;
        this.name = name;
        this.group = group;
        this.type = type;
        this.vialIndex = vialIndex;
    }

    public SequenceSample(String name, String type, Integer vialIndex) {
        this.name = name;
        this.type = type;
        this.vialIndex = vialIndex;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public void setFileName(String prefix, Boolean includeBatch) {
        this.fileName = (includeBatch) ? prefix + "_B" + String.format("%02d", batch) + "_" + name : prefix + "_" + name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getVialIndex() {
        return vialIndex;
    }

    public void setVialIndex(Integer index) {
        this.vialIndex = index;
    }

    public String getVialPosition() {
        return vialPosition;
    }

    public void setVialPosition(String position) {
        this.vialPosition = position;
    }

    public void setBatch(int number) {
        this.batch = number;
    }

    public int getBatch() {
        return this.batch;
    }

    public void setInjectionNumber(int number) {
        this.injectionNumber = number;
    }

    public int getId() {
        return this.id;
    }

    public int getInjectionNumber() {
        return this.injectionNumber;
    }

    public Map<Integer, String> makeSampleMap(String instrument) {

        return switch (instrument) {
            case "Agilent 6495C" -> null;
            case "Agilent 7200" -> makeA7200Map();
            case "Sciex 5500" -> null;
            case "Sciex 6500" -> null;
            case "Thermo Fusion" -> makeThermoMap();
            case "Thermo Eclipse" -> makeThermoMap();
            case "Thermo QExactive" -> makeThermoMap();
            default -> null;
        };
    }

    private Map<Integer, String> makeThermoMap() {

        Map<Integer, String> sampleMap = new HashMap<>();
        sampleMap.put(0, type); // Sample Type
        sampleMap.put(1, fileName); // File Name
        sampleMap.put(2, ""); // Path
        sampleMap.put(3, ""); // Instrument Method
        sampleMap.put(4, vialPosition); // Position
        sampleMap.put(5, "1"); // Inj Vol
        sampleMap.put(6, ""); // Comment
        return sampleMap;
    }

    private Map<Integer, String> makeA7200Map() {

        Map<Integer, String> sampleMap = new HashMap<>();
        sampleMap.put(0, vialPosition); // Vial
        sampleMap.put(1, ""); // Method Path
        sampleMap.put(2, ""); // Method File
        sampleMap.put(3, ""); // Data Path
        sampleMap.put(4, fileName); // Data File
        sampleMap.put(5, type); // Type
        return sampleMap;
    }

    public Map<Integer, String> makePrepMap() {
        Map<Integer, String> sampleMap = new HashMap<>();
        sampleMap.put(0, String.valueOf(id));
        sampleMap.put(1, name);
        sampleMap.put(2, group);
        sampleMap.put(3, "Batch " + batch);
        sampleMap.put(4, vialPosition);
        sampleMap.put(5, String.valueOf(injectionNumber));
        sampleMap.put(6, fileName);
        return sampleMap;
    }
}
