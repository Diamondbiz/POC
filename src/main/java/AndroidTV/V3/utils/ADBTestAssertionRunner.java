package AndroidTV.V3.utils;

import AndroidTV.V3.core.ADBDeviceController;
import AndroidTV.V3.core.ADBXmlParser;
import AndroidTV.V3.profiles.ADBScreenProfile;
import AndroidTV.V3.validators.ADBScreenAssertionResult;
import AndroidTV.V3.validators.ADBScreenValidator;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;

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
public class ADBTestAssertionRunner {

    private final ADBDeviceController device;
    private final ADBXmlParser parser;
    private final String xmlFolderPath;
    private final String logsFolderPath;
    private final String testStartTime;

    public ADBTestAssertionRunner(ADBDeviceController device,
                                  ADBXmlParser parser,
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
    public ADBScreenAssertionResult runAssertion(ADBScreenProfile profile,
                                                 String screenshotRoot,
                                                 String failRoot,
                                                 int markerTimeoutMs) throws Exception {

        ADBTestLogger.log("");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("🧪 RUNNING ASSERTION FOR: " + profile.getName());
        ADBTestLogger.log("═══════════════════════════════════════════════════");

        ADBScreenValidator validator = new ADBScreenValidator(
                device, parser, xmlFolderPath, testStartTime, screenshotRoot, failRoot);

        ADBScreenAssertionResult result;

        // 1. Wait for the marker
        boolean markerSeen = validator.waitUntilMarkerVisible(profile, markerTimeoutMs);

        if (!markerSeen) {
            // Short-circuit: screen never appeared.
            result = new ADBScreenAssertionResult(profile.getName());
            result.setMarkerPresent(false);
            result.setFailureReason("Marker '" + profile.getMarkerValue() +
                    "' did not appear within " + markerTimeoutMs + " ms");
            ADBTestLogger.logError("   ❌ Short-circuiting assertion — screen never loaded");
        } else {
            // 2. Full assertion pass
            result = validator.assertAll(profile);
        }

        // 3. Write JSON log
        String jsonPath = logsFolderPath + "/" + profile.getName() + "_" + testStartTime + ".json";
        ObjectNode json = buildJson(profile, result, screenshotRoot, failRoot);
        ADBArtifactReporter.writeJson(json, jsonPath);

        // 4. Print folder links
        String runFolder = screenshotRoot + "/" + profile.getName() + "_" + testStartTime;
        ADBArtifactReporter.printFolderLink("Screen folder", runFolder);
        ADBArtifactReporter.printFolderLink("Logs folder", logsFolderPath);

        ADBTestLogger.log("");
        ADBTestLogger.log("📌 Assertion result: " + result);
        ADBTestLogger.log("");

        return result;
    }

    // ==================== JSON BUILDER ====================

    private ObjectNode buildJson(ADBScreenProfile profile,
                                 ADBScreenAssertionResult result,
                                 String screenshotRoot,
                                 String failRoot) {

        ObjectNode root = ADBArtifactReporter.newJsonObject();

        root.put("testName", profile.getName());
        root.put("timestamp", testStartTime);
        root.put("markerPresent", result.isMarkerPresent());
        root.put("overallPassed", result.isOverallPassed());
        root.put("failureReason",
                result.getFailureReason() == null ? null : result.getFailureReason());

        // Elements
        ObjectNode elementsNode = ADBArtifactReporter.newJsonObject();
        for (java.util.Map.Entry<String, Boolean> e : result.getElements().entrySet()) {
            elementsNode.put(e.getKey(), e.getValue());
        }
        root.set("elements", elementsNode);

        // Crops
        ObjectNode cropsNode = ADBArtifactReporter.newJsonObject();
        for (java.util.Map.Entry<String, ADBScreenAssertionResult.CropResult> e
                : result.getCropResults().entrySet()) {
            ObjectNode one = ADBArtifactReporter.newJsonObject();
            one.put("passed", e.getValue().isPassed());
            one.put("similarity", e.getValue().getSimilarity());
            cropsNode.set(e.getKey(), one);
        }
        root.set("crops", cropsNode);

        // Summary counters
        ObjectNode summary = ADBArtifactReporter.newJsonObject();
        summary.put("elementsFound", result.getElementsFoundCount());
        summary.put("elementsTotal", result.getElementsTotalCount());
        summary.put("cropsPassed", result.getCropsPassedCount());
        summary.put("cropsTotal", result.getCropsTotalCount());
        root.set("summary", summary);

        // Artifacts
        ObjectNode artifacts = ADBArtifactReporter.newJsonObject();
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