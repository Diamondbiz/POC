package AndroidTV.V3.Tests.User.ScreensAssertion;

import AndroidTV.V3.config.TestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.ScreenState;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.flows.LoginFlow;
import AndroidTV.V3.flows.Preconditions;
import AndroidTV.V3.profiles.LiveMosaicScreenProfile;
import AndroidTV.V3.services.OtpService;
import AndroidTV.V3.services.SsidService;
import AndroidTV.V3.utils.AssertionRunner;
import AndroidTV.V3.utils.TestLogger;
import AndroidTV.V3.validators.ScreenAssertionResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LiveMosaicAssertionTest {

    public static void main(String[] args) {
        new LiveMosaicAssertionTest().run();
    }

    public void run() {
        String testStartTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        TestLogger.init("LiveMosaicAssertion");

        try {
            TestLogger.log("═══════════════════════════════════════════════════");
            TestLogger.log("📱 LIVE MOSAIC ASSERTION TEST");
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

            TestLogger.logStep("3", "Ensuring device reaches Live Mosaic screen");
            pre.ensureLoggedIn(phone, TestConfig.REGULAR_OTP);

            TestLogger.logStep("4", "Running assertion for Live Mosaic screen");
            AssertionRunner runner = new AssertionRunner(
                    device, parser,
                    TestConfig.XML_DIR,
                    TestConfig.LOGS_DIR,
                    testStartTime);

            ScreenAssertionResult result = runner.runAssertion(
                    LiveMosaicScreenProfile.get(),
                    TestConfig.CURRENT_SCREEN_DIR,
                    TestConfig.FAIL_DIR,
                    TestConfig.SCREEN_MARKER_TIMEOUT_MS);

            printSummary(result);

            if (!result.isOverallPassed()) {
                TestLogger.logError("❌ LIVE MOSAIC ASSERTION FAILED");
                System.exit(1);
            }
            TestLogger.logSuccess("✅ LIVE MOSAIC ASSERTION PASSED");

        } catch (Exception e) {
            TestLogger.logError("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            TestLogger.close();
        }
    }

    private void printSummary(ScreenAssertionResult result) {
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
        TestLogger.log("═══════════════════════════════════════");
    }
}