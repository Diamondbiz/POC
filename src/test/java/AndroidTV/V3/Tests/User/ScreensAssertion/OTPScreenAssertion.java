package AndroidTV.V3.Tests.User.ScreensAssertion;

import AndroidTV.V3.config.TestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.ScreenState;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.flows.ADBLoginFlow;
import AndroidTV.V3.flows.ADBPreconditions;
import AndroidTV.V3.profiles.OtpScreenProfile;
import AndroidTV.V3.services.ADBOTPService;
import AndroidTV.V3.services.SSIDService;
import AndroidTV.V3.utils.AssertionRunner;
import AndroidTV.V3.utils.TestLogger;
import AndroidTV.V3.validators.ADBScreenAssertionResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OTPScreenAssertion {

    public static void main(String[] args) {
        new OTPScreenAssertion().run();
    }

    public void run() {
        String testStartTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        TestLogger.init("OTPScreenAssertion");

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

            String phone = ssidService.getPhoneNumberForCurrentSSID();
            TestLogger.log("   📱 Test will use phone: " + phone);

            TestLogger.logStep("3", "Ensuring device reaches OTP screen");
            pre.ensureOnOtpScreen(phone);

            TestLogger.logStep("4", "Running assertion for OTP screen");
            AssertionRunner runner = new AssertionRunner(
                    device, parser,
                    TestConfig.XML_DIR,
                    TestConfig.LOGS_DIR,
                    testStartTime);

            ADBScreenAssertionResult result = runner.runAssertion(
                    OtpScreenProfile.get(),
                    TestConfig.CURRENT_SCREEN_DIR,
                    TestConfig.FAIL_DIR,
                    TestConfig.SCREEN_MARKER_TIMEOUT_MS);

            printSummary(result);

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

    private void printSummary(ADBScreenAssertionResult result) {
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