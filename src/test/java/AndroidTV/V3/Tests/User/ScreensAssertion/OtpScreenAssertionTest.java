package AndroidTV.V3.Tests.User.ScreensAssertion;

import AndroidTV.V3.config.TestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.ScreenState;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.flows.LoginFlow;
import AndroidTV.V3.flows.Preconditions;
import AndroidTV.V3.profiles.OtpScreenProfile;
import AndroidTV.V3.services.KeypadStateService;
import AndroidTV.V3.services.OtpService;
import AndroidTV.V3.services.SsidService;
import AndroidTV.V3.utils.AssertionRunner;
import AndroidTV.V3.utils.TestLogger;
import AndroidTV.V3.validators.ScreenAssertionResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OtpScreenAssertionTest {

    private static final String EXPECTED_DEFAULT_DIGIT = "0";

    public static void main(String[] args) {
        new OtpScreenAssertionTest().run();
    }

    public void run() {
        String testStartTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        TestLogger.init("OTPScreenAssertion");

        String keypadDecision = null;

        try {
            TestLogger.log("═══════════════════════════════════════════════════");
            TestLogger.log("📱 OTP SCREEN ASSERTION TEST");
            TestLogger.log("═══════════════════════════════════════════════════");

            TestLogger.logStep("1", "Connecting to device");
            DeviceController device = new DeviceController(TestConfig.DEVICE_UDID);
            device.connect();

            XmlParser parser = new XmlParser();
            ScreenState state = new ScreenState(
                    device, parser,
                    TestConfig.XML_DIR, testStartTime,
                    TestConfig.HOT_PACKAGE);

            LoginFlow loginFlow = new LoginFlow(
                    TestConfig.DEVICE_UDID,
                    TestConfig.XML_DIR,
                    testStartTime);

            OtpService otpService = new OtpService(
                    TestConfig.DEVICE_UDID,
                    TestConfig.XML_DIR,
                    testStartTime);

            SsidService ssidService = new SsidService(device, TestConfig.XML_DIR, testStartTime);

            Preconditions pre = new Preconditions(
                    device, parser, state,
                    loginFlow, otpService,
                    TestConfig.XML_DIR, testStartTime,
                    TestConfig.HOT_PACKAGE);

            TestLogger.logStep("2", "Router / phone number check");
            ssidService.logRouterCheck();

            String phone = ssidService.getPhoneNumberForCurrentSSID();
            TestLogger.log("   📱 Test will use phone: " + phone);

            TestLogger.logStep("3", "Ensuring device reaches OTP screen");
            pre.ensureOnOtpScreen(phone);

            // ---------- Read-only keypad detection ----------
            TestLogger.logStep("4", "Reading keypad state on OTP screen (read-only)");
            String perSessionFolder = TestConfig.CURRENT_SCREEN_DIR + "/Keypad digits state_" + testStartTime;
            KeypadStateService keypadStateService = new KeypadStateService(
                    device, parser, TestConfig.XML_DIR, testStartTime,
                    TestConfig.KEYPAD_REF_SELECTED_DIR, perSessionFolder,
                    LoginFlow.KEY_BOUNDS_ON_SCREEN);
            keypadDecision = keypadStateService.detectSelectedDigit();
            TestLogger.log("   OTP screen keypad state: " + keypadDecision);

            TestLogger.logStep("5", "Running assertion for OTP screen");
            AssertionRunner runner = new AssertionRunner(
                    device, parser,
                    TestConfig.XML_DIR,
                    TestConfig.LOGS_DIR,
                    testStartTime);

            ScreenAssertionResult result = runner.runAssertion(
                    OtpScreenProfile.get(),
                    TestConfig.CURRENT_SCREEN_DIR,
                    TestConfig.FAIL_DIR,
                    TestConfig.SCREEN_MARKER_TIMEOUT_MS);

            printSummary(result, keypadDecision);

            if (!result.isOverallPassed()) {
                TestLogger.logError("❌ OTP SCREEN ASSERTION FAILED");
                System.exit(1);
            }
            TestLogger.logSuccess("✅ OTP SCREEN ASSERTION PASSED");

        } catch (Exception e) {
            TestLogger.logError("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            TestLogger.close();
        }
    }

    private void printSummary(ScreenAssertionResult result, String keypadDecision) {
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
        TestLogger.log("═══ KEYPAD STATUS (OTP screen) ═══");
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