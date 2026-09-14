package AndroidTV.V3.Tests.User;

import AndroidTV.V3.config.TestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.ScreenState;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.flows.LoginFlow;
import AndroidTV.V3.flows.Preconditions;
import AndroidTV.V3.profiles.LiveMosaicScreenProfile;
import AndroidTV.V3.profiles.LoginScreenProfile;
import AndroidTV.V3.profiles.OtpScreenProfile;
import AndroidTV.V3.services.KeypadStateService;
import AndroidTV.V3.services.OtpService;
import AndroidTV.V3.services.SsidService;
import AndroidTV.V3.utils.AssertionRunner;
import AndroidTV.V3.utils.TestLogger;
import AndroidTV.V3.validators.ScreenAssertionResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * End-to-end full login test.
 *
 * Flow:
 *   1. Connect to device
 *   2. Router / phone number check
 *   3. Cold start: force-stop → pm clear → launch
 *   4. Assert Login screen (elements + keypad state)
 *   5. Enter phone number → press Connect
 *   6. Assert OTP screen (elements + keypad state)
 *   7. Enter OTP → press Verify
 *   8. Assert Live Mosaic screen (elements + crops)
 *   9. Combined summary
 *
 * Soft assertions throughout — every screen is asserted even if an earlier
 * one partially fails, so the run produces complete diagnostics.
 */
public class FullLoginTest {

    private static final String EXPECTED_DEFAULT_DIGIT = "0";

    // Results collected across the three screens
    private ScreenAssertionResult loginResult;
    private ScreenAssertionResult otpResult;
    private ScreenAssertionResult liveMosaicResult;

    private String loginKeypadState;
    private String otpKeypadState;

    public static void main(String[] args) {
        new FullLoginTest().run();
    }

    public void run() {
        String testStartTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        TestLogger.init("FullLoginTest");

        try {
            TestLogger.log("═══════════════════════════════════════════════════");
            TestLogger.log("📱 FULL LOGIN TEST — end-to-end");
            TestLogger.log("═══════════════════════════════════════════════════");

            // ==================== STEP 1: CONNECT ====================
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

            AssertionRunner runner = new AssertionRunner(
                    device, parser,
                    TestConfig.XML_DIR,
                    TestConfig.LOGS_DIR,
                    testStartTime);

            // ==================== STEP 2: ROUTER / PHONE CHECK ====================
            TestLogger.logStep("2", "Router / phone number check");
            ssidService.logRouterCheck();

            String phone = ssidService.getPhoneNumberForCurrentSSID();
            TestLogger.log("   📱 Test will use phone: " + phone);
            TestLogger.log("   🔢 Test will use OTP:   " + TestConfig.REGULAR_OTP);

            // ==================== STEP 3: COLD START ====================
            TestLogger.logStep("3", "Cold start — force-stop, clear data, relaunch");
            TestLogger.log("   🛑 Force-stopping app");
            device.forceStopApp(TestConfig.HOT_PACKAGE);
            TestLogger.log("   🧹 Clearing app data (logout)");
            device.clearAppData(TestConfig.HOT_PACKAGE);
            TestLogger.log("   ⏳ Waiting 1s after pm clear");
            Thread.sleep(1000);
            TestLogger.log("   🚀 Bringing app to foreground");
            device.bringAppToForeground(TestConfig.HOT_PACKAGE);

            // ==================== STEP 4: LOGIN SCREEN ====================
            TestLogger.logStep("4", "Login screen assertion");
            pre.ensureOnLoginScreen();

            String loginSessionFolder = TestConfig.CURRENT_SCREEN_DIR
                    + "/Keypad digits state_" + testStartTime + "_login";
            KeypadStateService loginKeypad = new KeypadStateService(
                    device, parser, TestConfig.XML_DIR, testStartTime,
                    TestConfig.KEYPAD_REF_SELECTED_DIR, loginSessionFolder,
                    LoginFlow.KEY_BOUNDS_ON_SCREEN);
            loginKeypadState = loginKeypad.detectSelectedDigit();
            TestLogger.log("   Login screen keypad state: " + loginKeypadState);

            loginResult = runner.runAssertion(
                    LoginScreenProfile.get(),
                    TestConfig.CURRENT_SCREEN_DIR,
                    TestConfig.FAIL_DIR,
                    TestConfig.SCREEN_MARKER_TIMEOUT_MS);

            // ==================== STEP 5: ENTER PHONE + CONNECT ====================
            TestLogger.logStep("5", "Entering phone number and pressing Connect");
            loginFlow.enterPhoneNumber(phone);
            loginFlow.pressConnect();
            loginFlow.waitForOTP();

            // ==================== STEP 6: OTP SCREEN ====================
            TestLogger.logStep("6", "OTP screen assertion");
            // OTP screen is already on screen at this point

            String otpSessionFolder = TestConfig.CURRENT_SCREEN_DIR
                    + "/Keypad digits state_" + testStartTime + "_otp";
            KeypadStateService otpKeypad = new KeypadStateService(
                    device, parser, TestConfig.XML_DIR, testStartTime,
                    TestConfig.KEYPAD_REF_SELECTED_DIR, otpSessionFolder,
                    LoginFlow.KEY_BOUNDS_ON_SCREEN);
            otpKeypadState = otpKeypad.detectSelectedDigit();
            TestLogger.log("   OTP screen keypad state: " + otpKeypadState);

            otpResult = runner.runAssertion(
                    OtpScreenProfile.get(),
                    TestConfig.CURRENT_SCREEN_DIR,
                    TestConfig.FAIL_DIR,
                    TestConfig.SCREEN_MARKER_TIMEOUT_MS);

            // ==================== STEP 7: ENTER OTP + VERIFY ====================
            TestLogger.logStep("7", "Entering OTP and pressing Verify");
            otpService.enterOTP(TestConfig.REGULAR_OTP);
            otpService.pressVerify();
            // Wait for login to complete — Live Mosaic marker
            boolean loginDone = waitForLiveMosaicMarker(device, testStartTime);
            if (!loginDone) {
                TestLogger.logWarning("   ⚠️ Live Mosaic marker did not appear after OTP entry");
            }

            // ==================== STEP 8: LIVE MOSAIC SCREEN ====================
            TestLogger.logStep("8", "Live Mosaic screen assertion");
            liveMosaicResult = runner.runAssertion(
                    LiveMosaicScreenProfile.get(),
                    TestConfig.CURRENT_SCREEN_DIR,
                    TestConfig.FAIL_DIR,
                    TestConfig.SCREEN_MARKER_TIMEOUT_MS);

            // ==================== STEP 9: COMBINED SUMMARY ====================
            TestLogger.logStep("9", "Combined summary");
            boolean overallPassed = printCombinedSummary(testStartTime);

            if (!overallPassed) {
                TestLogger.logError("❌ FULL LOGIN TEST FAILED");
                System.exit(1);
            }
            TestLogger.logSuccess("✅ FULL LOGIN TEST PASSED");

        } catch (Exception e) {
            TestLogger.logError("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            TestLogger.close();
        }
    }

    // ==================== WAIT FOR LIVE MOSAIC ====================

    /**
     * Polls the XML for the Live Mosaic marker until timeout.
     * Returns true if the marker appeared.
     */
    private boolean waitForLiveMosaicMarker(DeviceController device, String testStartTime) throws Exception {
        long start = System.currentTimeMillis();
        long timeout = 30000;
        String marker = "mod_LiveMosaic";

        TestLogger.log("   ⏳ Waiting for Live Mosaic marker: " + marker);
        while (System.currentTimeMillis() - start < timeout) {
            String xml = device.getScreenXml(TestConfig.XML_DIR, testStartTime);
            if (xml.contains(marker)) {
                TestLogger.logSuccess("   ✅ Marker appeared after "
                        + (System.currentTimeMillis() - start) + " ms");
                return true;
            }
            Thread.sleep(1000);
        }
        return false;
    }

    // ==================== COMBINED SUMMARY ====================

    private boolean printCombinedSummary(String testStartTime) {
        boolean loginOk = loginResult != null && loginResult.isOverallPassed();
        boolean otpOk = otpResult != null && otpResult.isOverallPassed();
        boolean liveOk = liveMosaicResult != null && liveMosaicResult.isOverallPassed();
        boolean allOk = loginOk && otpOk && liveOk;

        TestLogger.log("");
        TestLogger.log("╔═══════════════════════════════════════════════════╗");
        TestLogger.log("║           FULL LOGIN TEST — COMBINED SUMMARY      ║");
        TestLogger.log("╚═══════════════════════════════════════════════════╝");
        TestLogger.log("🕐 Timestamp: " + testStartTime);
        TestLogger.log("");

        // --- Login screen ---
        TestLogger.log("┌───────────────────────────────────────────────────┐");
        TestLogger.log("│ 1️⃣ LOGIN SCREEN                                   │");
        TestLogger.log("└───────────────────────────────────────────────────┘");
        if (loginResult != null) {
            TestLogger.log("   Marker:        " + (loginResult.isMarkerPresent() ? "✅ present" : "❌ missing"));
            TestLogger.log("   Elements:      " + loginResult.getElementsFoundCount()
                    + "/" + loginResult.getElementsTotalCount());
            TestLogger.log("   Crops:         " + loginResult.getCropsPassedCount()
                    + "/" + loginResult.getCropsTotalCount());
            TestLogger.log("   Status:        " + (loginOk ? "✅ PASS" : "❌ FAIL"));
        } else {
            TestLogger.log("   ⚠️ Not run");
        }
        TestLogger.log("   Keypad state:  " + (loginKeypadState == null ? "(none)" : loginKeypadState));
        if (EXPECTED_DEFAULT_DIGIT.equals(loginKeypadState)) {
            TestLogger.logSuccess("   ✅ Keypad matched expected default (0)");
        } else if (loginKeypadState != null && !loginKeypadState.isEmpty()) {
            TestLogger.logWarning("   ⚠️ Keypad was NOT in its fresh default state");
        } else {
            TestLogger.logWarning("   ❌ Keypad state not detected");
        }
        TestLogger.log("");

        // --- OTP screen ---
        TestLogger.log("┌───────────────────────────────────────────────────┐");
        TestLogger.log("│ 2️⃣ OTP SCREEN                                     │");
        TestLogger.log("└───────────────────────────────────────────────────┘");
        if (otpResult != null) {
            TestLogger.log("   Marker:        " + (otpResult.isMarkerPresent() ? "✅ present" : "❌ missing"));
            TestLogger.log("   Elements:      " + otpResult.getElementsFoundCount()
                    + "/" + otpResult.getElementsTotalCount());
            TestLogger.log("   Crops:         " + otpResult.getCropsPassedCount()
                    + "/" + otpResult.getCropsTotalCount());
            TestLogger.log("   Status:        " + (otpOk ? "✅ PASS" : "❌ FAIL"));
        } else {
            TestLogger.log("   ⚠️ Not run");
        }
        TestLogger.log("   Keypad state:  " + (otpKeypadState == null ? "(none)" : otpKeypadState));
        if (EXPECTED_DEFAULT_DIGIT.equals(otpKeypadState)) {
            TestLogger.logSuccess("   ✅ Keypad matched expected default (0)");
        } else if (otpKeypadState != null && !otpKeypadState.isEmpty()) {
            TestLogger.logWarning("   ⚠️ Keypad was NOT in its fresh default state");
        } else {
            TestLogger.logWarning("   ❌ Keypad state not detected");
        }
        TestLogger.log("");

        // --- Live Mosaic screen ---
        TestLogger.log("┌───────────────────────────────────────────────────┐");
        TestLogger.log("│ 3️⃣ LIVE MOSAIC SCREEN                             │");
        TestLogger.log("└───────────────────────────────────────────────────┘");
        if (liveMosaicResult != null) {
            TestLogger.log("   Marker:        " + (liveMosaicResult.isMarkerPresent() ? "✅ present" : "❌ missing"));
            TestLogger.log("   Elements:      " + liveMosaicResult.getElementsFoundCount()
                    + "/" + liveMosaicResult.getElementsTotalCount());
            TestLogger.log("   Crops:         " + liveMosaicResult.getCropsPassedCount()
                    + "/" + liveMosaicResult.getCropsTotalCount());
            TestLogger.log("   Status:        " + (liveOk ? "✅ PASS" : "❌ FAIL"));
        } else {
            TestLogger.log("   ⚠️ Not run");
        }
        TestLogger.log("");

        // --- Overall ---
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("📌 Overall:  " + (allOk ? "✅ PASS" : "❌ FAIL"));
        TestLogger.log("═══════════════════════════════════════════════════");

        return allOk;
    }
}