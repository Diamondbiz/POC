package AndroidTV.V3.validators;

import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.profiles.CropExpectation;
import AndroidTV.V3.profiles.ElementExpectation;
import AndroidTV.V3.profiles.ScreenProfile;
import AndroidTV.V3.utils.ArtifactReporter;
import AndroidTV.V3.utils.CropUtil;
import AndroidTV.V3.utils.ImageComparator;
import AndroidTV.V3.utils.TestLogger;

import java.io.File;

/**
 * Generic screen validator. Consumes ANY ADBScreenProfile and produces an
 * ADBScreenAssertionResult. Uses soft assertions throughout — never throws on
 * element or crop mismatches. Only logs and records.
 *
 * Screen-agnostic. Reusable by any test class.
 */
public class ADBScreenValidator {

    private final DeviceController device;
    private final XmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;
    private final String screenshotRootFolder;    // e.g. "Screens/Current screen"
    private final String failRootFolder;          // e.g. "Screens/Fail"

    public ADBScreenValidator(DeviceController device,
                              XmlParser parser,
                              String xmlFolderPath,
                              String testStartTime,
                              String screenshotRootFolder,
                              String failRootFolder) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
        this.screenshotRootFolder = screenshotRootFolder;
        this.failRootFolder = failRootFolder;
    }

    // ==================== WAIT ====================

    /**
     * Polls the UI XML until the profile's marker appears or timeout.
     */
    public boolean waitUntilMarkerVisible(ScreenProfile profile, int timeoutMs) throws Exception {
        TestLogger.log("⏳ Waiting for screen marker: " + profile.getMarkerValue());
        long start = System.currentTimeMillis();
        long lastLog = 0;

        while (System.currentTimeMillis() - start < timeoutMs) {
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            if (profile.isMarkerPresentIn(xml, parser)) {
                TestLogger.logSuccess("   ✅ Marker appeared after " +
                        (System.currentTimeMillis() - start) + " ms");
                return true;
            }
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed - lastLog >= 2000) {
                TestLogger.log("   … still waiting (" + elapsed + " ms)");
                lastLog = elapsed;
            }
            Thread.sleep(1000);
        }
        TestLogger.logError("   ❌ Marker did not appear within " + timeoutMs + " ms");
        return false;
    }

    // ==================== ASSERT ALL ====================

    /**
     * Full assertion pass:
     *  - checks marker
     *  - checks each element
     *  - takes a screenshot
     *  - crops + compares each crop
     * Never throws. Returns a result.
     */
    public ADBScreenAssertionResult assertAll(ScreenProfile profile) throws Exception {
        TestLogger.log("");
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("🔍 ASSERTING SCREEN: " + profile.getName());
        TestLogger.log("═══════════════════════════════════════════════════");

        ADBScreenAssertionResult result = new ADBScreenAssertionResult(profile.getName());

        // 1. Marker
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        boolean markerPresent = profile.isMarkerPresentIn(xml, parser);
        result.setMarkerPresent(markerPresent);

        if (!markerPresent) {
            result.setFailureReason("Marker '" + profile.getMarkerValue() + "' not present in XML");
            TestLogger.logError("   ❌ Marker not present: " + profile.getMarkerValue());
            return result;
        }
        TestLogger.logSuccess("   ✅ Marker present: " + profile.getMarkerValue());

        // 2. Elements
        TestLogger.log("");
        TestLogger.log("   ─── Elements ───");
        for (ElementExpectation e : profile.getElements()) {
            boolean found = checkElement(xml, e);
            result.putElement(e.getName(), found);
            if (found) {
                TestLogger.logSuccess("      ✅ " + e.getName());
            } else {
                TestLogger.logError("      ❌ " + e.getName() + " (missing: " + e.getValue() + ")");
            }
        }

        // 3. Screenshot
        TestLogger.log("");
        TestLogger.log("   ─── Screenshot ───");
        String runFolder = screenshotRootFolder + "/" + profile.getName() + "_" + testStartTime;
        new File(runFolder).mkdirs();

        File shot = device.takeScreenshot(runFolder, testStartTime);
        String fullScreenshotPath = shot.getAbsolutePath();

        ArtifactReporter.printFileLink("Screenshot", fullScreenshotPath);

        // 4. Crops
        if (!profile.getCrops().isEmpty()) {
            TestLogger.log("");
            TestLogger.log("   ─── Crops ───");
            String cropFolder = runFolder + "/crops";
            new File(cropFolder).mkdirs();
            String failCropFolder = failRootFolder + "/" + profile.getName() + "_" + testStartTime;
            new File(failCropFolder).mkdirs();

            for (CropExpectation c : profile.getCrops()) {
                String cropOutputPath = cropFolder + "/" + c.getName() + "_" + testStartTime + ".png";
                String cropped = CropUtil.crop(fullScreenshotPath, cropOutputPath, c.getBounds());
                if (cropped == null) {
                    result.putCrop(c.getName(), false, 0.0);
                    TestLogger.logError("      ❌ " + c.getName() + " — crop failed");
                    continue;
                }

                double similarity = ImageComparator.compare(cropped, c.getExpectedBaselinePath());
                boolean passed = similarity >= ImageComparator.DEFAULT_SIMILARITY_THRESHOLD;
                result.putCrop(c.getName(), passed, similarity);

                if (passed) {
                    TestLogger.logSuccess("      ✅ " + c.getName() +
                            " (" + String.format("%.2f", similarity) + "%)");
                } else {
                    TestLogger.logError("      ❌ " + c.getName() +
                            " (" + String.format("%.2f", similarity) + "%)");
                    String failCopy = failCropFolder + "/" + c.getName() + "_FAIL_" + testStartTime + ".png";
                    try {
                        java.nio.file.Files.copy(
                                new File(cropped).toPath(),
                                new File(failCopy).toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        ArtifactReporter.printFileLink("Failed crop", failCopy);
                    } catch (Exception ex) {
                        TestLogger.logWarning("      ⚠️ Could not copy failed crop: " + ex.getMessage());
                    }
                }
            }
        }

        // 5. Summary
        TestLogger.log("");
        TestLogger.log("   ─── Summary ───");
        TestLogger.log("      Marker:    " + (markerPresent ? "✅" : "❌"));
        TestLogger.log("      Elements:  " + result.getElementsFoundCount() + "/" + result.getElementsTotalCount());
        TestLogger.log("      Crops:     " + result.getCropsPassedCount() + "/" + result.getCropsTotalCount());
        TestLogger.log("      Overall:   " + (result.isOverallPassed() ? "✅ PASS" : "❌ FAIL"));
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("");

        return result;
    }

    // ==================== ELEMENT CHECK ====================

    private boolean checkElement(String xml, ElementExpectation e) {
        switch (e.getType()) {
            case RESOURCE_ID:
                return parser.containsResourceId(xml, e.getValue());
            case TEXT:
                return xml.contains("text=\"" + e.getValue() + "\"");
            case TEXT_PRESENT_ANYWHERE:
                return xml.contains(e.getValue());
            default:
                return false;
        }
    }
}