package cmsp.tool.box.datamodel;

import cmsp.tool.box.enums.DiscoveryReportTypes;

import java.io.File;
import java.util.HashMap;

public class DiscoveryProject {

    private final File projectDirectory;
    private final HashMap<String, File> projectFiles;
    private final String projectID;
    private final DiscoveryReportTypes projectType;

    /**
     * Discovery Project object. Indicates the type of project and associated result files.
     *
     * @param projectID ID string for project - most commonly project folder name
     * @param type Type of discovery project
     * @param directory Directory of project result files
     * @param exports Array of result export files
     */
    public DiscoveryProject(String projectID, DiscoveryReportTypes type, File directory, File[] exports) {

        this.projectID = projectID;
        this.projectDirectory = directory;
        this.projectType = type;
        projectFiles = new HashMap<>();

        // Categorize result files associated with discovery project.
        // TODO - implement for other discovery project types.
        switch (projectType) {
            case PROTPD -> detectPdExportFiles(exports);
        }
    }

    /**
     * Categorize result files associated with ProteomeDiscoverer project.
     *
     * @param exports
     */
    private void detectPdExportFiles(File[] exports) {

        for (File export : exports) {

            String[] components = export.getName().split("_");
            String type = components[components.length - 1];

            // TODO - implement remaining export types.
            switch (type) {
                case "Proteins.txt" -> projectFiles.put("Proteins", export);
                case "PeptideGroups.txt" -> projectFiles.put("Peptides", export);
                case "PSMs.txt" -> projectFiles.put("PSMs", export);
                case "StudyInformation.txt" -> projectFiles.put("Study", export);
            }
        }
    }

    public HashMap<String, File> getProjectFiles() {
        return projectFiles;
    }

    public String getProjectID() {
        return projectID;
    }
}
