package AndroidTV.V3.pages;

import AndroidTV.V3.core.ADBDeviceController;
import AndroidTV.V3.core.ADBXmlParser;
import AndroidTV.V3.services.KeypadStateService;
import AndroidTV.V3.utils.TestLogger;

import java.util.Map;

/**
 * Page Object for the OTP screen.
 */
public class ADBOTPPage {

    private final ADBDeviceController device;
    private final ADBXmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;
    private final KeypadStateService keypadStateService;

    public ADBOTPPage(ADBDeviceController device,
                      ADBXmlParser parser,
                      String xmlFolderPath,
                      String testStartTime,
                      KeypadStateService keypadStateService) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
        this.keypadStateService = keypadStateService;
    }

    public boolean waitForOTPScreen(int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            if (parser.isAllOTPElementsPresent(xml)) {
                return true;
            }
            Thread.sleep(1000);
        }
        return false;
    }

    public boolean isOTPScreenPresent() throws Exception {
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        return parser.isAllOTPElementsPresent(xml);
    }

    public void enterOTP(Map<String, int[]> keypadPositions, String otp) throws Exception {
        TestLogger.log("📱 Entering OTP: " + otp);

        String detected = keypadStateService.detectSelectedDigit();

        int[] startPos;
        if (detected == null || detected.isEmpty()) {
            TestLogger.logWarning("   ⚠️ Could not detect selected digit — defaulting to '0'");
            startPos = keypadPositions.get("0");
        } else {
            startPos = keypadPositions.get(detected);
            if (startPos == null) {
                TestLogger.logWarning("   ⚠️ Detected '" + detected +
                        "' not in keypad map — defaulting to '0'");
                startPos = keypadPositions.get("0");
            } else {
                TestLogger.log("   🎯 Using starting digit: " + detected);
            }
        }

        int currentRow = startPos[0];
        int currentCol = startPos[1];

        for (char digitChar : otp.toCharArray()) {
            String digit = String.valueOf(digitChar);
            int[] targetPos = keypadPositions.get(digit);
            if (targetPos == null) continue;

            int targetRow = targetPos[0];
            int targetCol = targetPos[1];

            while (currentRow > targetRow) { device.pressDpadUp(); currentRow--; }
            while (currentRow < targetRow) { device.pressDpadDown(); currentRow++; }
            while (currentCol > targetCol) { device.pressDpadLeft(); currentCol--; }
            while (currentCol < targetCol) { device.pressDpadRight(); currentCol++; }

            device.pressCenter();
            Thread.sleep(200);
        }

        TestLogger.logSuccess("✅ OTP entered: " + otp);
    }

    public void pressVerify() throws Exception {
        device.pressCenter();
        Thread.sleep(3000);
    }

    public java.io.File takeScreenshot(String savePath) throws Exception {
        return device.takeScreenshot(savePath, testStartTime);
    }
}