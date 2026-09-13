package AndroidTV.V3.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Pixel-by-pixel image comparison utility.
 * Compares two PNG files and returns a similarity percentage (0.0 - 100.0).
 *
 * Reusable by any validator that needs to compare a crop or full screen against a baseline.
 */
public class ImageComparator {

    public static final int DEFAULT_PIXEL_TOLERANCE = 10;
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 95.0;

    /**
     * Compares two images using the default pixel tolerance.
     *
     * @return similarity percentage (0.0 - 100.0). Returns 0.0 if:
     *         - either file doesn't exist
     *         - either image cannot be read
     *         - dimensions don't match
     */
    public static double compare(String actualPath, String expectedPath) throws IOException {
        return compare(actualPath, expectedPath, DEFAULT_PIXEL_TOLERANCE);
    }

    /**
     * Compares two images using a given per-channel pixel tolerance.
     *
     * @param tolerance max allowed difference per RGB channel (0-255) for a pixel to count as a match
     * @return similarity percentage (0.0 - 100.0)
     */
    public static double compare(String actualPath, String expectedPath, int tolerance) throws IOException {
        File actualFile = new File(actualPath);
        File expectedFile = new File(expectedPath);

        if (!actualFile.exists() || !expectedFile.exists()) {
            return 0.0;
        }

        BufferedImage actualImage = ImageIO.read(actualFile);
        BufferedImage expectedImage = ImageIO.read(expectedFile);

        if (actualImage == null || expectedImage == null) {
            return 0.0;
        }

        if (actualImage.getWidth() != expectedImage.getWidth() ||
                actualImage.getHeight() != expectedImage.getHeight()) {
            return 0.0;
        }

        long mismatched = 0;
        long total = (long) actualImage.getWidth() * actualImage.getHeight();

        for (int x = 0; x < actualImage.getWidth(); x++) {
            for (int y = 0; y < actualImage.getHeight(); y++) {
                int a = actualImage.getRGB(x, y);
                int e = expectedImage.getRGB(x, y);

                int rD = Math.abs(((a >> 16) & 0xFF) - ((e >> 16) & 0xFF));
                int gD = Math.abs(((a >> 8) & 0xFF) - ((e >> 8) & 0xFF));
                int bD = Math.abs((a & 0xFF) - (e & 0xFF));

                if (rD > tolerance || gD > tolerance || bD > tolerance) {
                    mismatched++;
                }
            }
        }

        return 100.0 - ((double) mismatched / total * 100);
    }

    /**
     * Returns true if similarity is at or above the given threshold.
     */
    public static boolean matches(String actualPath, String expectedPath, double threshold) throws IOException {
        return compare(actualPath, expectedPath) >= threshold;
    }

    /**
     * Convenience: checks whether two images have identical dimensions.
     */
    public static boolean dimensionsMatch(String actualPath, String expectedPath) throws IOException {
        File actualFile = new File(actualPath);
        File expectedFile = new File(expectedPath);
        if (!actualFile.exists() || !expectedFile.exists()) return false;

        BufferedImage a = ImageIO.read(actualFile);
        BufferedImage e = ImageIO.read(expectedFile);
        if (a == null || e == null) return false;

        return a.getWidth() == e.getWidth() && a.getHeight() == e.getHeight();
    }

    /**
     * Returns "WxH" for a given image, or "unknown" if unreadable.
     */
    public static String getDimensions(String imagePath) {
        try {
            BufferedImage img = ImageIO.read(new File(imagePath));
            if (img == null) return "unknown";
            return img.getWidth() + "x" + img.getHeight();
        } catch (IOException e) {
            return "unknown";
        }
    }
}