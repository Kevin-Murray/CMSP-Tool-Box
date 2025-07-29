package cmsp.tool.box.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for automatic detection of specified files on local workstation.
 */
public class FileLocatorUtils {

    /**
     * Determine if input R software version is newer than previous version.
     *
     * @param version1 Previous version
     * @param version2 Input version
     * @return true if input is newer than previous
     */
    public static boolean isNewerRVersion(File version1, File version2) {

        String v1 = version1.getName();
        String v2 = version2.getName();

        v1 = v1.replace("R-", "");
        v2 = v2.replace("R-", "");

        String[] parts1 = v1.split("\\."); // Splits by dot
        String[] parts2 = v2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);

        // Loop through version components -- format: Major.Minor.Patch
        for (int i = 0; i < length; i++) {
            int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (part1 < part2) {
                return true;
            }

            if (part1 > part2) {
                return false;
            }

            // If version components same, loop to next version component
        }
        return false; // Versions are the same
    }

    /**
     * Locate path of R Script executable on local workstation.
     *
     * @return Absolute string path to detected location
     */
    public static String locateRScriptPath() {

        // Default installation path of R
        File parent = new File("C:\\Program Files\\R");

        // If R not installed, or not installed in default location - return empty string
        if (!parent.isDirectory()) {
            return "";
        }

        // List all files in default directory, find files starting with R prefix
        File[] files = parent.listFiles();
        List<File> rFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("R-")) {
                    rFiles.add(file);
                }
            }
        }

        // If multiple files detected, select latest version
        Path rPath = null;
        if (!rFiles.isEmpty()) {
            if (rFiles.size() > 1) {
                File latestVersion = rFiles.get(0);
                for (int i = 1; i < files.length; i++) {
                    if (isNewerRVersion(latestVersion, rFiles.get(i))) {
                        latestVersion = rFiles.get(i);
                    }
                }
                rPath = latestVersion.toPath();
            } else {
                rPath = rFiles.get(0).toPath();
            }
        }

        // Resolve R Script executable path
        Path exePath = (rPath != null) ? rPath.resolve("bin\\Rscript.exe") : null;

        // If R Script executable path exists, return absolute path.
        if (exePath != null) {
            return (Files.exists(exePath)) ? exePath.toString() : "";
        } else {
            return "";
        }
    }
}
