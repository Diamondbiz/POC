package AndroidTV.V3.flows;

import AndroidTV.V3.config.TestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.pages.LoginPage;
import AndroidTV.V3.pages.OtpPage;
import AndroidTV.V3.services.KeypadStateService;

import java.util.HashMap;
import java.util.Map;

/**
 * Orchestrates the login flow.
 */
public class ADBLoginFlow {

    private final DeviceController device;
    private final LoginPage loginPage;
    private final OtpPage otpPage;
    private final String xmlFolderPath;
    private final String testStartTime;

    /**
     * Keypad layout for navigation:
     *   [1][2][3]
     *   [4][5][6]
     *   [7][8][9]
     *   [Back][0][Next]
     */
    public static final Map<String, int[]> KEYPAD_POSITIONS = new HashMap<>();
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

    /**
     * Bounds of each key on the keypad screen (x, y, w, h).
     * Used by KeypadStateService to identify the selected key.
     */
    public static final Map<String, int[]> KEY_BOUNDS_ON_SCREEN = new HashMap<>();
    static {
        KEY_BOUNDS_ON_SCREEN.put("1",    new int[]{1389, 352, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("2",    new int[]{1473, 352, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("3",    new int[]{1557, 352, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("4",    new int[]{1389, 424, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("5",    new int[]{1473, 424, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("6",    new int[]{1557, 424, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("7",    new int[]{1389, 496, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("8",    new int[]{1473, 496, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("9",    new int[]{1557, 496, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("0",    new int[]{1473, 568, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("Back", new int[]{1389, 568, 79, 68});
        KEY_BOUNDS_ON_SCREEN.put("Next", new int[]{1557, 568, 79, 68});
    }

    public ADBLoginFlow(String deviceUDID, String xmlFolderPath, String testStartTime) throws Exception {
        this.device = new DeviceController(deviceUDID);
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;

        XmlParser parser = new XmlParser();

        String perSessionFolder = TestConfig.CURRENT_SCREEN_DIR + "/Keypad digits state_" + testStartTime;
        KeypadStateService keypadStateService = new KeypadStateService(
                device, parser, xmlFolderPath, testStartTime,
                TestConfig.KEYPAD_REF_SELECTED_DIR, perSessionFolder, KEY_BOUNDS_ON_SCREEN);

        this.loginPage = new LoginPage(device, parser, xmlFolderPath, testStartTime, keypadStateService);
        this.otpPage = new OtpPage(device, parser, xmlFolderPath, testStartTime, keypadStateService);
    }

    public void openAppAndWaitForLogin() throws Exception {
        device.launchApp();
        if (!loginPage.waitForLoginScreen(30000)) {
            throw new RuntimeException("Login screen did not load within 30 seconds");
        }
    }

    public void enterPhoneNumber(String phoneNumber) throws Exception {
        loginPage.enterPhoneNumber(KEYPAD_POSITIONS, phoneNumber);
    }

    public void pressConnect() throws Exception {
        loginPage.pressConnect();
    }

    public void waitForOTP() throws Exception {
        if (!otpPage.waitForOTPScreen(30000)) {
            throw new RuntimeException("OTP screen did not load within 30 seconds");
        }
    }

    public void loginWithValidPhoneNumber(String phoneNumber) throws Exception {
        openAppAndWaitForLogin();
        enterPhoneNumber(phoneNumber);
        pressConnect();
        waitForOTP();
    }

    public String getEnteredPhoneNumber() throws Exception {
        return loginPage.getEnteredPhoneNumber();
    }

    public void forceStopApp(String packageName) throws Exception {
        device.forceStopApp(packageName);
    }

    public OtpPage getOtpPage() {
        return otpPage;
    }
}