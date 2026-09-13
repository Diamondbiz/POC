package AndroidTV.V3.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Crops a PNG file using ImageMagick via bash.
 * Bounds are given as (x1, y1, x2, y2) and converted to ImageMagick's WxH+X+Y format.
 *
 * Reusable by any validator that needs a crop of a screenshot region.
 */
public class CropUtil {

    /**
     * Crops a source PNG into a destination PNG.
     *
     * @param sourcePath absolute path of the source screenshot
     * @param outputPath absolute path of the output crop (parent folders must exist or will be created)
     * @param x1 left
     * @param y1 top
     * @param x2 right
     * @param y2 bottom
     * @return the output path if the crop succeeded, or null on failure
     */
    public static String crop(String sourcePath, String outputPath,
                              int x1, int y1, int x2, int y2) throws Exception {
        int width = x2 - x1;
        int height = y2 - y1;

        if (width <= 0 || height <= 0) {
            TestLogger.logWarning("CropUtil: invalid bounds " +
                    x1 + "," + y1 + " → " + x2 + "," + y2);
            return null;
        }

        File src = new File(sourcePath);
        if (!src.exists()) {
            TestLogger.logWarning("CropUtil: source not found: " + sourcePath);
            return null;
        }

        File out = new File(outputPath);
        if (out.getParentFile() != null) {
            out.getParentFile().mkdirs();
        }

        // ImageMagick 7: prefer "magick" over deprecated "convert"
        String cmd = "magick '" + sourcePath + "' -crop " +
                width + "x" + height + "+" + x1 + "+" + y1 +
                " +repage '" + outputPath + "'";

        int exitCode = runBash(cmd);

        // Fallback for systems where only "convert" is available
        if (exitCode != 0) {
            String legacyCmd = "convert '" + sourcePath + "' -crop " +
                    width + "x" + height + "+" + x1 + "+" + y1 +
                    " +repage '" + outputPath + "'";
            exitCode = runBash(legacyCmd);
        }

        if (exitCode != 0 || !out.exists()) {
            TestLogger.logWarning("CropUtil: crop failed for " + outputPath);
            return null;
        }

        return outputPath;
    }

    /**
     * Convenience: crops using bounds array [x1, y1, x2, y2].
     */
    public static String crop(String sourcePath, String outputPath, int[] bounds) throws Exception {
        if (bounds == null || bounds.length != 4) {
            TestLogger.logWarning("CropUtil: bounds must be [x1,y1,x2,y2]");
            return null;
        }
        return crop(sourcePath, outputPath, bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    private static int runBash(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Silent — we don't want to spam the console with every crop.
            }
        }
        return process.waitFor();
    }
}