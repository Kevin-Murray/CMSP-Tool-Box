package cmsp.tool.box.enums;

import javafx.scene.image.Image;

/**
 * Enumerator class of Discovery report types and associated information
 */
public enum DiscoveryReportTypes {

    PROTPD("Discovery Proteomics [Proteome Discoverer]", "discoveryProteomeDiscoverer.png", "Proteome Discoverer ≥ 3.2.0\nFormat exports from `Results Exporter` post-processing module.",
            "Proteome Discoverer Project Folder...", "Open Proteome Discoverer Project Folder...", null, null, "DiscoveryReportsPdPage1.fxml"),
    PROTPEAKS("Discovery Proteomics [PEAKS]", "discoveryPEAKSstudio.png", "PEAKS Studio ≥ 12.0.0\nFormat exports from PEAKS Studio project folder",
            "PEAKS Studio Project Folder...", "Open PEAKS Studio Project Folder", null, null, null),
    METASKY("Discovery Metabolomics [Skyline]", null, null, null, null, null, null, null),
    METAMZ("Discovery Metabolomics [MZmine]", null, null, null, null, null, null, null),
    BIOCRATES("Biocrates Targeted Profiling", null, null, null, null, null, null, null);

    private final String extName;
    private final String extension;
    private final String fxmlPage;
    private final String image;
    private final String info;
    private final String label;
    private final String message;
    private final String openMessage;

    /**
     * Discovery report type object that reflects this different types of data processing software.
     * Displays report type information in controller window during new report selection.
     *
     * @param label       Report type label to display in list
     * @param image       Report type image to display in window
     * @param message     Report type message to indicate compatible software version and restrictions
     * @param info        Report type file selector info message
     * @param openMessage Report type file selector window title
     * @param extName     Report type file extension filter title
     * @param extension   Report type file extension filter
     * @param fxmlPage    Report type associated FXML wizard gui.
     */
    DiscoveryReportTypes(String label, String image, String message, String info, String openMessage, String extName, String extension, String fxmlPage) {
        this.label = label;
        this.image = image;
        this.message = message;
        this.info = info;
        this.openMessage = openMessage;
        this.extName = extName;
        this.extension = extension;
        this.fxmlPage = fxmlPage;
    }

    /**
     * Get Enum type from export header string. Match by import name field.
     *
     * @param label Discovery report type label to match
     * @return Enum type of match.
     */
    public static DiscoveryReportTypes fromImportName(String label) {
        for (DiscoveryReportTypes type : DiscoveryReportTypes.values()) {
            if (type.getLabel().equals(label)) {
                return type;
            }
        }
        return null;
    }

    public String getExtension() {
        return extension;
    }

    public String getExtensionName() {
        return this.extName;
    }

    public String getFxmlPage() {
        return this.fxmlPage;
    }

    /**
     * Get associated discovery report type image from resources.
     */
    public Image getImage() {
        return new Image("cmsp/tool/box/icons/" + this.image);
    }

    public String getInfo() {
        return this.info;
    }

    public String getLabel() {
        return this.label;
    }

    public String getMessage() {
        return this.message;
    }

    public String getOpenMessage() {
        return openMessage;
    }
}
