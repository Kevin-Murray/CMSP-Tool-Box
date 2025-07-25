package cmsp.tool.box.enums;

/**
 * Enumerator class for Discovery Report tab names and order
 */
public enum DiscoveryReportTabs {

    PROTEINS("Proteins", "Protein Report", 1),
    PEPTIDES("Peptides", "Peptide Report", 2),
    PSMS("PSMs", "PSM Report", 3),
    STUDY("Study", "Study Information", 4),
    ORAGOBP("ORAGOBP", "GO Biological Process", 1),
    ORAGOMF("ORAGOMF", "GO Molecular Function", 2),
    ORAGOCC("ORAGOCC", "GO Cellular Compartment", 3),
    ORAKEGG("ORAKEGG", "KEGG Pathways", 4),
    ORAREACTOME("ORAREACTOME", "Reactome Pathways", 5),
    GSEGOBP("GSEGOBP", "GO Biological Process", 1),
    GSEGOMF("GSEGOMF", "GO Molecular Function", 2),
    GSEGOCC("GSEGOCC", "GO Cellular Compartment", 3),
    GSEKEGG("GSEKEGG", "KEGG Pathways", 4),
    GSEREACTOME("GSEREACTOME", "Reactome Pathways", 5);

    private final int index;
    private final String report;
    private final String tabTitle;

    /**
     * Discovery report tab information. Matches report code to report title and tab order.
     *
     * @param report   Report code
     * @param tabTitle Tab title
     * @param index    Order index number
     */
    DiscoveryReportTabs(String report, String tabTitle, int index) {
        this.report = report;
        this.tabTitle = tabTitle;
        this.index = index;
    }

    /**
     * Get associated DiscoveryReportTabs object from report code
     *
     * @param reportName Report code
     * @return Matching DiscoveryReportTabs object
     */
    public static DiscoveryReportTabs fromImportName(String reportName) {
        for (DiscoveryReportTabs tab : DiscoveryReportTabs.values()) {
            if (tab.getReport().equals(reportName)) {
                return tab;
            }
        }
        return null;
    }

    public int getIndex() {
        return index;
    }

    public String getReport() {
        return report;
    }

    public String getTabTitle() {
        return tabTitle;
    }
}
