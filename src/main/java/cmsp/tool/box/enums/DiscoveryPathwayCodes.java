package cmsp.tool.box.enums;

/**
 * Enumerator class for Discovery pathway analysis.
 */
public enum DiscoveryPathwayCodes {

    // ORA - Over-representation analysis
    // GSEA - Gene set enrichment analysis
    // GO - Gene ontology; BP - Biological process; MF - Molecular function; CC - Cellular compartment
    // KEGG - Kyoto encyclopedia of genes and genomes
    ORAGOBP("ORA:GO-BP", "ORA", "GO"),
    ORAGOMF("ORA:GO-MF", "ORA", "GO"),
    ORAGOCC("ORA:GO-CC", "ORA", "GO"),
    ORAKEGG("ORA:KEGG", "ORA", "KEGG"),
    ORAREACTOME("ORA:REACTOME", "ORA", "REACTOME"),
    GSEGOBP("GSE:GO-BP", "GSEA", "GO"),
    GSEGOMF("GSE:GO-MF", "GSEA", "GO"),
    GSEGOCC("GSE:GO-CC", "GSEA", "GO"),
    GSEKEGG("GSE:KEGG", "GSEA", "KEGG"),
    GSEREACTOME("GSE:REACTOME", "GSEA", "REACTOME");

    private final String code;
    private final String type;
    private final String geneSet;

    /**
     * Pathway codes, classification (type), and associated gene sets.
     *
     * @param code String code - designator for Rscript (pathwayAnalysis.R) pathway analysis
     * @param type String type - indicates algorithm type of pathway analysis
     * @param geneSet String gene set - indicates the associated gene set / ontology pathway associations are made.
     */
    DiscoveryPathwayCodes(String code, String type, String geneSet) {
        this.code = code;
        this.type = type;
        this.geneSet = geneSet;
    }

    public String getCode() {
        return code;
    }

    public String getGeneSet() {
        return geneSet;
    }

    public String getType() {
        return type;
    }
}
