package cmsp.tool.box.datamodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DiscoveryResult {

    private LinkedHashMap<String, String> resultsMap;

    /**
     * Discovery Result object. Store result table as LinkedHashMap.
     * Access result entries using column headers as keys.
     *
     * @param header Array of result table headers
     * @param row Array of result table row entries
     */
    public DiscoveryResult(String[] header, String[] row) {

        this.resultsMap = new LinkedHashMap<>();
        addItems(header, row);
    }

    /**
     * Put each value into designated bucket.
     *
     * @param header Column names from results table
     * @param items  Variable values
     */
    public void addItems(String[] header, String[] items) {

        for (int i = 0; i < items.length; i++) {
            resultsMap.put(header[i], items[i]);
        }
    }

    public List<String> getColumnHeaders() {
        return new ArrayList<>(resultsMap.keySet());
    }

    public LinkedHashMap<String, String> getMap() {
        return resultsMap;
    }

    public String getPdGeneSymbol() {
        return resultsMap.get("Gene Symbol");
    }

    public String getPdLabel() {
        return resultsMap.get("CF: Label");
    }

    public String getPdSample() {
        return resultsMap.get("Sample");
    }

    public String getPdSampleIdentifier() {
        return resultsMap.get("Sample Identifier");
    }

    public String getValue(String key) {
        return resultsMap.get(key);
    }

    public Boolean isPdContaminant() {
        return Boolean.parseBoolean(resultsMap.get("Contaminant"));
    }

    /**
     * Evaluate if protein database is from Uniprot. Database origin determined from protein description column -
     * Uniprot protein entries should include OS (origin species) and SV (sequence variant) information.
     * TODO - need to evaluate how robust this is.
     *
     * @return Boolean true if protein database was downloaded from Uniprot.
     */
    public Boolean isUniprotDatabase() {
        if (resultsMap.containsKey("Description")) {
            return resultsMap.get("Description").contains("OS=") &&
                    resultsMap.get("Description").contains("SV=");
        }
        return false;
    }

    public void setResultsMap(LinkedHashMap<String, String> resultsMap) {
        this.resultsMap = resultsMap;
    }

    public int size() {
        return resultsMap.size();
    }
}
