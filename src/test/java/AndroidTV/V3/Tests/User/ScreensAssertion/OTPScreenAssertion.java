package AndroidTV.V3.Tests.User.ScreensAssertion;

import AndroidTV.V3.config.ADBTestConfig;
import AndroidTV.V3.core.ADBDeviceController;
import AndroidTV.V3.core.ADBScreenState;
import AndroidTV.V3.core.ADBXmlParser;
import AndroidTV.V3.flows.ADBLoginFlow;
import AndroidTV.V3.flows.ADBPreconditions;
import AndroidTV.V3.profiles.ADBOTPScreenProfile;
import AndroidTV.V3.services.ADBOTPService;
import AndroidTV.V3.services.SSIDService;
import AndroidTV.V3.utils.ADBTestAssertionRunner;
import AndroidTV.V3.utils.ADBTestLogger;
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

        ADBTestLogger.init("OTPScreenAssertion");

        try {
            ADBTestLogger.log("═══════════════════════════════════════════════════");
            ADBTestLogger.log("📱 OTP SCREEN ASSERTION TEST");
            ADBTestLogger.log("═══════════════════════════════════════════════════");

            ADBTestLogger.logStep("1", "Connecting to device");
            ADBDeviceController device = new ADBDeviceController(ADBTestConfig.DEVICE_UDID);
            device.connect();

            ADBXmlParser parser = new ADBXmlParser();
            ADBScreenState state = new ADBScreenState(
                    device, parser,
                    ADBTestConfig.XML_DIR, testStartTime,
                    ADBTestConfig.HOT_PACKAGE);

            ADBLoginFlow loginFlow = new ADBLoginFlow(
                    ADBTestConfig.DEVICE_UDID,
                    ADBTestConfig.XML_DIR,
                    testStartTime);

            ADBOTPService otpService = new ADBOTPService(
                    ADBTestConfig.DEVICE_UDID,
                    ADBTestConfig.XML_DIR,
                    testStartTime);

            SSIDService ssidService = new SSIDService(device, ADBTestConfig.XML_DIR, testStartTime);

            ADBPreconditions pre = new ADBPreconditions(
                    device, parser, state,
                    loginFlow, otpService,
                    ADBTestConfig.XML_DIR, testStartTime,
                    ADBTestConfig.HOT_PACKAGE);

            ADBTestLogger.logStep("2", "Router / phone number check");
            ssidService.logRouterCheck();

            String phone = ssidService.getPhoneNumberForCurrentSSID();
            ADBTestLogger.log("   📱 Test will use phone: " + phone);

            ADBTestLogger.logStep("3", "Ensuring device reaches OTP screen");
            pre.ensureOnOtpScreen(phone);

            ADBTestLogger.logStep("4", "Running assertion for OTP screen");
            ADBTestAssertionRunner runner = new ADBTestAssertionRunner(
                    device, parser,
                    ADBTestConfig.XML_DIR,
                    ADBTestConfig.LOGS_DIR,
                    testStartTime);

            ADBScreenAssertionResult result = runner.runAssertion(
                    ADBOTPScreenProfile.get(),
                    ADBTestConfig.CURRENT_SCREEN_DIR,
                    ADBTestConfig.FAIL_DIR,
                    ADBTestConfig.SCREEN_MARKER_TIMEOUT_MS);

            printSummary(result);

            if (!result.isOverallPassed()) {
                ADBTestLogger.logError("❌ OTP SCREEN ASSERTION FAILED");
                System.exit(1);
            }
            ADBTestLogger.logSuccess("✅ OTP SCREEN ASSERTION PASSED");

        } catch (Exception e) {
            ADBTestLogger.logError("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            ADBTestLogger.close();
        }
    }

    private void printSummary(ADBScreenAssertionResult result) {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══ FINAL TEST SUMMARY ═══");
        ADBTestLogger.log("📌 Screen:        " + result.getScreenName());
        ADBTestLogger.log("📌 Marker:        " + (result.isMarkerPresent() ? "✅ present" : "❌ missing"));
        ADBTestLogger.log("📋 Elements:      " + result.getElementsFoundCount() + "/" + result.getElementsTotalCount());
        ADBTestLogger.log("📋 Crops:         " + result.getCropsPassedCount() + "/" + result.getCropsTotalCount());
        ADBTestLogger.log("📌 Overall:       " + (result.isOverallPassed() ? "✅ PASS" : "❌ FAIL"));
        if (result.getFailureReason() != null) {
            ADBTestLogger.log("   ⚠️ Reason:      " + result.getFailureReason());
        }
        ADBTestLogger.log("═══════════════════════════════════════");
    }
}