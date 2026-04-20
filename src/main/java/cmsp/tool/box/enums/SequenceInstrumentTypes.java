package cmsp.tool.box.enums;

public enum SequenceInstrumentTypes {

    A6495C("Sample", "DoubleBlank", "Blank", "QC", "QC", "Cal"),
    A7200("Sample", "DoubleBlank", "Blank", "QC", "QC", "Cal"),
    S5500("Sample", "DoubleBlank", "Blank", "QC", "QC", "Cal"),
    S6500("Sample", "DoubleBlank", "Blank", "QC", "QC", "Cal"),
    ECLIPSE("Unknown", "Blank", "Blank", "QC", "QC", "Std Bracket"),
    FUSION("Unknown", "Blank", "Blank", "QC", "QC", "Std Bracket"),
    QEXACTIVE("Unknown", "Blank", "Blank", "QC", "QC", "Std Bracket");

    private final String sample;
    private final String blank;
    private final String neg;
    private final String sst;
    private final String qc;
    private final String cal;

    SequenceInstrumentTypes(String sample, String blank, String neg, String sst, String qc, String cal) {
        this.sample = sample;
        this.blank = blank;
        this.neg = neg;
        this.sst = sst;
        this.qc = qc;
        this.cal = cal;
    }

    public String getBlank() {
        return blank;
    }

    public String getCal() {
        return cal;
    }

    public String getNeg() {
        return neg;
    }

    public String getQc() {
        return qc;
    }

    public String getSample() {
        return sample;
    }

    public String getSst() {
        return sst;
    }

    public String getType(String type) {
        return switch (type) {
            case "Sample" -> getSample();
            case "Blank" -> getBlank();
            case "Neg" -> getNeg();
            case "SST" -> getSst();
            case "QC" -> getQc();
            case "Cal" -> getCal();
            default -> "";
        };
    }
}
