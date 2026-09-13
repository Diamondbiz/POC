package AndroidTV.V3.services;

import AndroidTV.V3.config.ADBTestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.flows.ADBLoginFlow;
import AndroidTV.V3.pages.ADBOTPPage;

import java.util.HashMap;
import java.util.Map;

/**
 * Business logic layer for OTP operations.
 */
public class ADBOTPService {

    private final DeviceController device;
    private final ADBOTPPage otpPage;
    private final XmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;

    /**
     * Keypad layout for OTP navigation.
     *   [1][2][3]
     *   [4][5][6]
     *   [7][8][9]
     *   [Back][0][Next]
     */
    private static final Map<String, int[]> KEYPAD_POSITIONS = new HashMap<>();
    static {
        KEYPAD_POSITIONS.put("1",    new int[]{0, 0});
        KEYPAD_POSITIONS.put("2",    new int[]{0, 1});
        KEYPAD_POSITIONS.put("3",    new int[]{0, 2});
        KEYPAD_POSITIONS.put("4",    new int[]{1, 0});
        KEYPAD_POSITIONS.put("5",    new int[]{1, 1});
        KEYPAD_POSITIONS.put("6",    new int[]{1, 2});
        KEYPAD_POSITIONS.put("7",    new int[]{2, 0});
        KEYPAD_POSITIONS.put("8",    new int[]{2, 1});
        KEYPAD_POSITIONS.put("9",    new int[]{2, 2});
        KEYPAD_POSITIONS.put("0",    new int[]{3, 1});
        KEYPAD_POSITIONS.put("Back", new int[]{3, 0});
        KEYPAD_POSITIONS.put("Next", new int[]{3, 2});
    }

    public ADBOTPService(String deviceUDID, String xmlFolderPath, String testStartTime) throws Exception {
        this.device = new DeviceController(deviceUDID);
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;

        this.parser = new XmlParser();

        String perSessionFolder = ADBTestConfig.CURRENT_SCREEN_DIR + "/Keypad digits state_" + testStartTime;
        KeypadStateService keypadStateService = new KeypadStateService(
                device, parser, xmlFolderPath, testStartTime,
                ADBTestConfig.KEYPAD_REF_SELECTED_DIR, perSessionFolder,
                ADBLoginFlow.KEY_BOUNDS_ON_SCREEN);

        this.otpPage = new ADBOTPPage(device, parser, xmlFolderPath, testStartTime, keypadStateService);
    }

    public void waitForOTP() throws Exception {
        if (!otpPage.waitForOTPScreen(30000)) {
            throw new RuntimeException("OTP screen did not load");
        }
    }

    public boolean isOTPScreenValid() throws Exception {
        return otpPage.isOTPScreenPresent();
    }

    public void enterOTP(String otp) throws Exception {
        otpPage.enterOTP(KEYPAD_POSITIONS, otp);
    }

    public void pressVerify() throws Exception {
        otpPage.pressVerify();
    }

    public java.io.File takeOTPScreenshot(String savePath) throws Exception {
        return otpPage.takeScreenshot(savePath);
    }
}