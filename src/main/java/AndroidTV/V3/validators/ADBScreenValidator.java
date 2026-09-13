package AndroidTV.V3.validators;

import AndroidTV.V3.core.ADBDeviceController;
import AndroidTV.V3.core.ADBXmlParser;
import AndroidTV.V3.profiles.ADBCropExpectation;
import AndroidTV.V3.profiles.ADBElementExpectation;
import AndroidTV.V3.profiles.ADBScreenProfile;
import AndroidTV.V3.utils.ADBArtifactReporter;
import AndroidTV.V3.utils.ADBCropUtil;
import AndroidTV.V3.utils.ADBImageComparator;
import AndroidTV.V3.utils.ADBTestLogger;

import java.io.File;

/**
 * Generic screen validator. Consumes ANY ADBScreenProfile and produces an
 * ADBScreenAssertionResult. Uses soft assertions throughout — never throws on
 * element or crop mismatches. Only logs and records.
 *
 * Screen-agnostic. Reusable by any test class.
 */
public class ADBScreenValidator {

    private final ADBDeviceController device;
    private final ADBXmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;
    private final String screenshotRootFolder;    // e.g. "Screens/Current screen"
    private final String failRootFolder;          // e.g. "Screens/Fail"

    public ADBScreenValidator(ADBDeviceController device,
                              ADBXmlParser parser,
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
    public boolean waitUntilMarkerVisible(ADBScreenProfile profile, int timeoutMs) throws Exception {
        ADBTestLogger.log("⏳ Waiting for screen marker: " + profile.getMarkerValue());
        long start = System.currentTimeMillis();
        long lastLog = 0;

        while (System.currentTimeMillis() - start < timeoutMs) {
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            if (profile.isMarkerPresentIn(xml, parser)) {
                ADBTestLogger.logSuccess("   ✅ Marker appeared after " +
                        (System.currentTimeMillis() - start) + " ms");
                return true;
            }
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed - lastLog >= 2000) {
                ADBTestLogger.log("   … still waiting (" + elapsed + " ms)");
                lastLog = elapsed;
            }
            Thread.sleep(1000);
        }
        ADBTestLogger.logError("   ❌ Marker did not appear within " + timeoutMs + " ms");
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
    public ADBScreenAssertionResult assertAll(ADBScreenProfile profile) throws Exception {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("🔍 ASSERTING SCREEN: " + profile.getName());
        ADBTestLogger.log("═══════════════════════════════════════════════════");

        ADBScreenAssertionResult result = new ADBScreenAssertionResult(profile.getName());

        // 1. Marker
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        boolean markerPresent = profile.isMarkerPresentIn(xml, parser);
        result.setMarkerPresent(markerPresent);

        if (!markerPresent) {
            result.setFailureReason("Marker '" + profile.getMarkerValue() + "' not present in XML");
            ADBTestLogger.logError("   ❌ Marker not present: " + profile.getMarkerValue());
            return result;
        }
        ADBTestLogger.logSuccess("   ✅ Marker present: " + profile.getMarkerValue());

        // 2. Elements
        ADBTestLogger.log("");
        ADBTestLogger.log("   ─── Elements ───");
        for (ADBElementExpectation e : profile.getElements()) {
            boolean found = checkElement(xml, e);
            result.putElement(e.getName(), found);
            if (found) {
                ADBTestLogger.logSuccess("      ✅ " + e.getName());
            } else {
                ADBTestLogger.logError("      ❌ " + e.getName() + " (missing: " + e.getValue() + ")");
            }
        }

        // 3. Screenshot
        ADBTestLogger.log("");
        ADBTestLogger.log("   ─── Screenshot ───");
        String runFolder = screenshotRootFolder + "/" + profile.getName() + "_" + testStartTime;
        new File(runFolder).mkdirs();

        File shot = device.takeScreenshot(runFolder, testStartTime);
        String fullScreenshotPath = shot.getAbsolutePath();

        ADBArtifactReporter.printFileLink("Screenshot", fullScreenshotPath);

        // 4. Crops
        if (!profile.getCrops().isEmpty()) {
            ADBTestLogger.log("");
            ADBTestLogger.log("   ─── Crops ───");
            String cropFolder = runFolder + "/crops";
            new File(cropFolder).mkdirs();
            String failCropFolder = failRootFolder + "/" + profile.getName() + "_" + testStartTime;
            new File(failCropFolder).mkdirs();

            for (ADBCropExpectation c : profile.getCrops()) {
                String cropOutputPath = cropFolder + "/" + c.getName() + "_" + testStartTime + ".png";
                String cropped = ADBCropUtil.crop(fullScreenshotPath, cropOutputPath, c.getBounds());
                if (cropped == null) {
                    result.putCrop(c.getName(), false, 0.0);
                    ADBTestLogger.logError("      ❌ " + c.getName() + " — crop failed");
                    continue;
                }

                double similarity = ADBImageComparator.compare(cropped, c.getExpectedBaselinePath());
                boolean passed = similarity >= ADBImageComparator.DEFAULT_SIMILARITY_THRESHOLD;
                result.putCrop(c.getName(), passed, similarity);

                if (passed) {
                    ADBTestLogger.logSuccess("      ✅ " + c.getName() +
                            " (" + String.format("%.2f", similarity) + "%)");
                } else {
                    ADBTestLogger.logError("      ❌ " + c.getName() +
                            " (" + String.format("%.2f", similarity) + "%)");
                    String failCopy = failCropFolder + "/" + c.getName() + "_FAIL_" + testStartTime + ".png";
                    try {
                        java.nio.file.Files.copy(
                                new File(cropped).toPath(),
                                new File(failCopy).toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        ADBArtifactReporter.printFileLink("Failed crop", failCopy);
                    } catch (Exception ex) {
                        ADBTestLogger.logWarning("      ⚠️ Could not copy failed crop: " + ex.getMessage());
                    }
                }
            }
        }

        // 5. Summary
        ADBTestLogger.log("");
        ADBTestLogger.log("   ─── Summary ───");
        ADBTestLogger.log("      Marker:    " + (markerPresent ? "✅" : "❌"));
        ADBTestLogger.log("      Elements:  " + result.getElementsFoundCount() + "/" + result.getElementsTotalCount());
        ADBTestLogger.log("      Crops:     " + result.getCropsPassedCount() + "/" + result.getCropsTotalCount());
        ADBTestLogger.log("      Overall:   " + (result.isOverallPassed() ? "✅ PASS" : "❌ FAIL"));
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("");

        return result;
    }

    // ==================== ELEMENT CHECK ====================

    private boolean checkElement(String xml, ADBElementExpectation e) {
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