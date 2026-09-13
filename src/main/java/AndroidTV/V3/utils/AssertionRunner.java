package AndroidTV.V3.utils;

import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.profiles.ScreenProfile;
import AndroidTV.V3.validators.ScreenAssertionResult;
import AndroidTV.V3.validators.ScreenValidator;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Test-facing assertion runner.
 *
 * Combines:
 *   1. ADBScreenValidator (wait + assert)
 *   2. JSON logging via Jackson (through ADBArtifactReporter)
 *   3. Console file/folder links
 *
 * Usage (from a test):
 *   ADBTestAssertionRunner runner = new ADBTestAssertionRunner(
 *       device, parser, xmlFolder, logsFolder, testStartTime);
 *   ADBScreenAssertionResult r = runner.runAssertion(
 *       ADBLoginScreenProfile.get(),
 *       "Screens/Current screen",
 *       "Screens/Fail",
 *       60000);
 *
 * The runner does NOT throw on assertion failure. It returns a result.
 * The test decides whether to fail.
 *
 * If the screen marker never appears (timeout), the runner short-circuits:
 * it produces a marker-missing result without running element/crop checks.
 */
public class AssertionRunner {

    private final DeviceController device;
    private final XmlParser parser;
    private final String xmlFolderPath;
    private final String logsFolderPath;
    private final String testStartTime;

    public AssertionRunner(DeviceController device,
                           XmlParser parser,
                           String xmlFolderPath,
                           String logsFolderPath,
                           String testStartTime) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.logsFolderPath = logsFolderPath;
        this.testStartTime = testStartTime;
    }

    /**
     * Runs the full assertion pipeline for one screen profile.
     *
     * @param profile            the screen profile
     * @param screenshotRoot     e.g. "Screens/Current screen"
     * @param failRoot           e.g. "Screens/Fail"
     * @param markerTimeoutMs    max time to wait for the screen to load
     * @return                   assertion result (never null)
     */
    public ScreenAssertionResult runAssertion(ScreenProfile profile,
                                              String screenshotRoot,
                                              String failRoot,
                                              int markerTimeoutMs) throws Exception {

        TestLogger.log("");
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("🧪 RUNNING ASSERTION FOR: " + profile.getName());
        TestLogger.log("═══════════════════════════════════════════════════");

        ScreenValidator validator = new ScreenValidator(
                device, parser, xmlFolderPath, testStartTime, screenshotRoot, failRoot);

        ScreenAssertionResult result;

        // 1. Wait for the marker
        boolean markerSeen = validator.waitUntilMarkerVisible(profile, markerTimeoutMs);

        if (!markerSeen) {
            // Short-circuit: screen never appeared.
            result = new ScreenAssertionResult(profile.getName());
            result.setMarkerPresent(false);
            result.setFailureReason("Marker '" + profile.getMarkerValue() +
                    "' did not appear within " + markerTimeoutMs + " ms");
            TestLogger.logError("   ❌ Short-circuiting assertion — screen never loaded");
        } else {
            // 2. Full assertion pass
            result = validator.assertAll(profile);
        }

        // 3. Write JSON log
        String jsonPath = logsFolderPath + "/" + profile.getName() + "_" + testStartTime + ".json";
        ObjectNode json = buildJson(profile, result, screenshotRoot, failRoot);
        ArtifactReporter.writeJson(json, jsonPath);

        // 4. Print folder links
        String runFolder = screenshotRoot + "/" + profile.getName() + "_" + testStartTime;
        ArtifactReporter.printFolderLink("Screen folder", runFolder);
        ArtifactReporter.printFolderLink("Logs folder", logsFolderPath);

        TestLogger.log("");
        TestLogger.log("📌 Assertion result: " + result);
        TestLogger.log("");

        return result;
    }

    // ==================== JSON BUILDER ====================

    private ObjectNode buildJson(ScreenProfile profile,
                                 ScreenAssertionResult result,
                                 String screenshotRoot,
                                 String failRoot) {

        ObjectNode root = ArtifactReporter.newJsonObject();

        root.put("testName", profile.getName());
        root.put("timestamp", testStartTime);
        root.put("markerPresent", result.isMarkerPresent());
        root.put("overallPassed", result.isOverallPassed());
        root.put("failureReason",
                result.getFailureReason() == null ? null : result.getFailureReason());

        // Elements
        ObjectNode elementsNode = ArtifactReporter.newJsonObject();
        for (java.util.Map.Entry<String, Boolean> e : result.getElements().entrySet()) {
            elementsNode.put(e.getKey(), e.getValue());
        }
        root.set("elements", elementsNode);

        // Crops
        ObjectNode cropsNode = ArtifactReporter.newJsonObject();
        for (java.util.Map.Entry<String, ScreenAssertionResult.CropResult> e
                : result.getCropResults().entrySet()) {
            ObjectNode one = ArtifactReporter.newJsonObject();
            one.put("passed", e.getValue().isPassed());
            one.put("similarity", e.getValue().getSimilarity());
            cropsNode.set(e.getKey(), one);
        }
        root.set("crops", cropsNode);

        // Summary counters
        ObjectNode summary = ArtifactReporter.newJsonObject();
        summary.put("elementsFound", result.getElementsFoundCount());
        summary.put("elementsTotal", result.getElementsTotalCount());
        summary.put("cropsPassed", result.getCropsPassedCount());
        summary.put("cropsTotal", result.getCropsTotalCount());
        root.set("summary", summary);

        // Artifacts
        ObjectNode artifacts = ArtifactReporter.newJsonObject();
        String runFolder = screenshotRoot + "/" + profile.getName() + "_" + testStartTime;
        String failFolder = failRoot + "/" + profile.getName() + "_" + testStartTime;
        artifacts.put("screenFolder", runFolder);
        artifacts.put("cropsFolder", runFolder + "/crops");
        artifacts.put("failFolder", failFolder);
        artifacts.put("logsFolder", logsFolderPath);
        root.set("artifacts", artifacts);

        return root;
    }
}