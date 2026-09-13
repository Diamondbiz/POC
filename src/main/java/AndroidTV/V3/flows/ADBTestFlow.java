package AndroidTV.V3.flows;

import AndroidTV.V3.config.TestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.pages.ADBLiveMosaicPage;
import AndroidTV.V3.pages.ADBLoginPage;
import AndroidTV.V3.pages.ADBOTPPage;
import AndroidTV.V3.services.KeypadStateService;

import java.util.Map;

/**
 * Master orchestrator for all flows.
 */
public class ADBTestFlow {

    private final DeviceController device;
    private final ADBLoginPage loginPage;
    private final ADBOTPPage otpPage;
    private final ADBLiveMosaicPage liveMosaicPage;
    private final XmlParser parser;

    private final String xmlFolderPath;
    private final String testStartTime;

    private static final Map<String, int[]> KEYPAD_POSITIONS = ADBLoginFlow.KEYPAD_POSITIONS;
    private static final Map<String, int[]> KEY_BOUNDS_ON_SCREEN = ADBLoginFlow.KEY_BOUNDS_ON_SCREEN;

    public ADBTestFlow(String deviceUDID, String xmlFolderPath, String testStartTime) throws Exception {
        this.device = new DeviceController(deviceUDID);
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;

        this.parser = new XmlParser();

        String perSessionFolder = TestConfig.CURRENT_SCREEN_DIR + "/Keypad digits state_" + testStartTime;
        KeypadStateService keypadStateService = new KeypadStateService(
                device, parser, xmlFolderPath, testStartTime,
                TestConfig.KEYPAD_REF_SELECTED_DIR, perSessionFolder, KEY_BOUNDS_ON_SCREEN);

        this.loginPage = new ADBLoginPage(device, parser, xmlFolderPath, testStartTime, keypadStateService);
        this.otpPage = new ADBOTPPage(device, parser, xmlFolderPath, testStartTime, keypadStateService);
        this.liveMosaicPage = new ADBLiveMosaicPage(device, parser, xmlFolderPath, testStartTime);
    }

    public void openAppAndWaitForLogin() throws Exception {
        device.launchApp();
        if (!loginPage.waitForLoginScreen(30000)) {
            throw new RuntimeException("Login screen did not load");
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
            throw new RuntimeException("OTP screen did not load");
        }
    }

    public void loginWithValidPhoneNumber(String phoneNumber) throws Exception {
        openAppAndWaitForLogin();
        enterPhoneNumber(phoneNumber);
        pressConnect();
        waitForOTP();
    }

    public void forceStopApp() throws Exception {
        device.forceStopApp("il.net.hot.hot");
    }

    public void validateLiveMosaicScreen() throws Exception {
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        if (!liveMosaicPage.isLiveMosaicScreenPresent(xml)) {
            throw new RuntimeException("Live Mosaic screen elements are missing");
        }
    }

    public DeviceController getDevice() { return device; }
    public XmlParser getParser() { return parser; }
    public ADBLoginPage getLoginPage() { return loginPage; }
    public ADBOTPPage getOTPPage() { return otpPage; }
    public ADBLiveMosaicPage getLiveMosaicPage() { return liveMosaicPage; }
}