package AndroidTV.V3.Tests.User.ScreensAssertion;

import AndroidTV.V3.config.TestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.ScreenState;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.flows.ADBLoginFlow;
import AndroidTV.V3.flows.ADBPreconditions;
import AndroidTV.V3.profiles.ADBLoginScreenProfile;
import AndroidTV.V3.services.ADBOTPService;
import AndroidTV.V3.services.KeypadStateService;
import AndroidTV.V3.services.SSIDService;
import AndroidTV.V3.utils.AssertionRunner;
import AndroidTV.V3.utils.TestLogger;
import AndroidTV.V3.validators.ADBScreenAssertionResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoginScreenAssertion {

    private static final String EXPECTED_DEFAULT_DIGIT = "0";

    public static void main(String[] args) {
        new LoginScreenAssertion().run();
    }

    public void run() {
        String testStartTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        TestLogger.init("LoginScreenAssertion");

        String keypadDecision = null;

        try {
            TestLogger.log("═══════════════════════════════════════════════════");
            TestLogger.log("📱 LOGIN SCREEN ASSERTION TEST");
            TestLogger.log("═══════════════════════════════════════════════════");

            TestLogger.logStep("1", "Connecting to device");
            DeviceController device = new DeviceController(TestConfig.DEVICE_UDID);
            device.connect();

            XmlParser parser = new XmlParser();
            ScreenState state = new ScreenState(
                    device, parser,
                    TestConfig.XML_DIR, testStartTime,
                    TestConfig.HOT_PACKAGE);

            ADBLoginFlow loginFlow = new ADBLoginFlow(
                    TestConfig.DEVICE_UDID,
                    TestConfig.XML_DIR,
                    testStartTime);

            ADBOTPService otpService = new ADBOTPService(
                    TestConfig.DEVICE_UDID,
                    TestConfig.XML_DIR,
                    testStartTime);

            SSIDService ssidService = new SSIDService(device, TestConfig.XML_DIR, testStartTime);

            ADBPreconditions pre = new ADBPreconditions(
                    device, parser, state,
                    loginFlow, otpService,
                    TestConfig.XML_DIR, testStartTime,
                    TestConfig.HOT_PACKAGE);

            TestLogger.logStep("2", "Router / phone number check");
            ssidService.logRouterCheck();

            TestLogger.logStep("3", "Ensuring app is running and on the login screen");
            pre.ensureAppRunning();
            pre.ensureOnLoginScreen();

            TestLogger.logStep("4", "Reading keypad default selection (read-only)");
            String perSessionFolder = TestConfig.CURRENT_SCREEN_DIR + "/Keypad digits state_" + testStartTime;
            KeypadStateService keypadStateService = new KeypadStateService(
                    device, parser, TestConfig.XML_DIR, testStartTime,
                    TestConfig.KEYPAD_REF_SELECTED_DIR, perSessionFolder,
                    ADBLoginFlow.KEY_BOUNDS_ON_SCREEN);
            keypadDecision = keypadStateService.detectSelectedDigit();
            TestLogger.log("   Login screen keypad default: " + keypadDecision);

            TestLogger.logStep("5", "Running assertion for Login screen");
            AssertionRunner runner = new AssertionRunner(
                    device, parser,
                    TestConfig.XML_DIR,
                    TestConfig.LOGS_DIR,
                    testStartTime);

            ADBScreenAssertionResult result = runner.runAssertion(
                    ADBLoginScreenProfile.get(),
                    TestConfig.CURRENT_SCREEN_DIR,
                    TestConfig.FAIL_DIR,
                    TestConfig.SCREEN_MARKER_TIMEOUT_MS);

            printSummary(result, keypadDecision);

            if (!result.isOverallPassed()) {
                TestLogger.logError("❌ LOGIN SCREEN ASSERTION FAILED");
                System.exit(1);
            }
            TestLogger.logSuccess("✅ LOGIN SCREEN ASSERTION PASSED");

        } catch (Exception e) {
            TestLogger.logError("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            TestLogger.close();
        }
    }

    private void printSummary(ADBScreenAssertionResult result, String keypadDecision) {
        TestLogger.log("");
        TestLogger.log("═══ FINAL TEST SUMMARY ═══");
        TestLogger.log("📌 Screen:        " + result.getScreenName());
        TestLogger.log("📌 Marker:        " + (result.isMarkerPresent() ? "✅ present" : "❌ missing"));
        TestLogger.log("📋 Elements:      " + result.getElementsFoundCount() + "/" + result.getElementsTotalCount());
        TestLogger.log("📋 Crops:         " + result.getCropsPassedCount() + "/" + result.getCropsTotalCount());
        TestLogger.log("📌 Overall:       " + (result.isOverallPassed() ? "✅ PASS" : "❌ FAIL"));
        if (result.getFailureReason() != null) {
            TestLogger.log("   ⚠️ Reason:      " + result.getFailureReason());
        }

        TestLogger.log("");
        TestLogger.log("═══ KEYPAD STATUS (Login screen) ═══");
        TestLogger.log("   Expected default: " + EXPECTED_DEFAULT_DIGIT);
        TestLogger.log("   Detected:         " + (keypadDecision == null ? "(none)" : keypadDecision));

        if (EXPECTED_DEFAULT_DIGIT.equals(keypadDecision)) {
            TestLogger.logSuccess("   ✅ Matches expected default");
        } else if (keypadDecision != null && !keypadDecision.isEmpty()) {
            TestLogger.logWarning("   ⚠️ Mismatch — the keypad was NOT in its fresh default state.");
            TestLogger.logWarning("      Framework navigated from the detected position.");
        } else {
            TestLogger.logWarning("   ❌ No detection available");
        }

        TestLogger.log("════════════════════════════════════");
    }
}