package AndroidTV.V3.pages;

import AndroidTV.V3.core.ADBDeviceController;
import AndroidTV.V3.core.ADBXmlParser;
import AndroidTV.V3.services.KeypadStateService;
import AndroidTV.V3.utils.TestLogger;

import java.util.Map;

/**
 * Page Object for the HOT Login (Phone) screen.
 */
public class ADBLoginPage {

    private final ADBDeviceController device;
    private final ADBXmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;
    private final KeypadStateService keypadStateService;

    public ADBLoginPage(ADBDeviceController device,
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

    public boolean waitForLoginScreen(int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            if (parser.isAllLoginScreenElementsPresent(xml)) {
                return true;
            }
            Thread.sleep(1000);
        }
        return false;
    }

    public void enterPhoneNumber(Map<String, int[]> keypadPositions, String phoneNumber) throws Exception {
        TestLogger.log("📱 Entering phone number: " + phoneNumber);

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

        for (char digitChar : phoneNumber.toCharArray()) {
            String digit = String.valueOf(digitChar);
            int[] targetPos = keypadPositions.get(digit);
            if (targetPos == null) {
                TestLogger.logWarning("⚠️ Unknown digit: " + digit + ", skipping...");
                continue;
            }

            int targetRow = targetPos[0];
            int targetCol = targetPos[1];

            while (currentRow > targetRow) { device.pressDpadUp(); currentRow--; }
            while (currentRow < targetRow) { device.pressDpadDown(); currentRow++; }
            while (currentCol > targetCol) { device.pressDpadLeft(); currentCol--; }
            while (currentCol < targetCol) { device.pressDpadRight(); currentCol++; }

            device.pressCenter();
            Thread.sleep(200);
        }

        TestLogger.logSuccess("✅ Phone number entered: " + phoneNumber);
    }

    public void pressConnect() throws Exception {
        for (int i = 0; i < 5; i++) {
            device.pressDpadDown();
        }
        device.pressCenter();
        Thread.sleep(3000);
    }

    public String getEnteredPhoneNumber() throws Exception {
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        return parser.extractTextByResourceId(xml, "txtUserCellPhone");
    }

    public boolean isLoginScreenPresent() throws Exception {
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        return parser.isAllLoginScreenElementsPresent(xml);
    }

    public java.io.File takeScreenshot(String savePath) throws Exception {
        return device.takeScreenshot(savePath, testStartTime);
    }
}