package cmsp.tool.box.enums;

public enum SequenceTableHeaders {

    A7200(new String[] {"Vial", "Method Path", "Method File", "Data Path", "Data File", "Type"}),
    FUSION(new String[] {"Sample Type", "File Name", "Path", "Instrument Method", "Position", "Inj Vol", "Comment"}),
    ECLIPSE(new String[] {"Sample Type", "File Name", "Path", "Instrument Method", "Position", "Inj Vol", "Comment"}),
    QEXACTIVE(new String[] {"Sample Type", "File Name", "Path", "Instrument Method", "Position", "Inj Vol", "Comment"});

    private final String[] headers;

    SequenceTableHeaders(String[] headers) {
        this.headers = headers;
    }

    public String[] getHeaders() {
        return headers;
    }
}
