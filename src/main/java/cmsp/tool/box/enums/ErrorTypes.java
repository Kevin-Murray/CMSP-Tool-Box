
package cmsp.tool.box.enums;

/**
 * Error types enum. Details the error message for commonly encountered issues.
 */
public enum ErrorTypes {

    DATABASE("Database path not set properly.\nPlease set in - Files > Set Up...\n\nSelect `QuickQA` folder in CBS_cmsp_results_2024 back-up drive."),
    RANGE("No QC entries in date range.\nPlease adjust the range and try again."),
    LOG("Log Number does not match expected format '00000'"),
    DATE( "Date not properly set."),
    TIME("Time field not properly set."),
    TIMEDAY("Time not properly set. Select AM or PM."),
    INSTRUMENT("Instrument not properly set."),
    CONFIGURATION("Configuration not properly set."),
    MATRIX("Matrix not properly set."),
    TYPE("Event Type not properly set."),

    TARGETBLANK("No replicates with `Blank` sample type detected in Skyline document.\n\nPlease revise and resubmit."),
    TARGETUNIQUE("Duplicate molecule names detected. Ensure all Skyline molecule names are unique, including internal standards.\n\nPlease revise and resubmit");

    private final String errorMessage;

    /**
     * Constructor class.
     *
     * @param errorMessage String of error message
     */
    ErrorTypes(String errorMessage) {

        this.errorMessage = errorMessage;
    }

    /**
     * Get error message string.
     *
     * @return String of error message
     */
    public String getErrorMessage() {

        return this.errorMessage;
    }
}
