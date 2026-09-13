package AndroidTV.V3.flows;
import AndroidTV.V3.services.OtpService;
import AndroidTV.V3.config.TestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.ScreenState;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.utils.TestLogger;

/**
 * State enforcer layer.
 * Every enforcer checks first (read-only), then acts only if needed.
 * Never asserts. Throws only when target state cannot be reached.
 */
public class Preconditions {

    private final DeviceController device;
    private final XmlParser parser;
    private final ScreenState state;
    private final LoginFlow loginFlow;
    private final OtpService otpService;
    private final String xmlFolderPath;
    private final String testStartTime;
    private final String appPackage;

    public Preconditions(DeviceController device,
                         XmlParser parser,
                         ScreenState state,
                         LoginFlow loginFlow,
                         OtpService otpService,
                         String xmlFolderPath,
                         String testStartTime,
                         String appPackage) {
        this.device = device;
        this.parser = parser;
        this.state = state;
        this.loginFlow = loginFlow;
        this.otpService = otpService;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
        this.appPackage = appPackage;
    }

    // ==================== APP-LEVEL ====================

    /**
     * Ensures the app process is running AND reliably foreground.
     */
    public void ensureAppRunning() throws Exception {
        TestLogger.log("🔧 ensureAppRunning");

        boolean running = state.isAppRunning();
        boolean foreground = running && state.isAppForegroundReliable();

        if (running && foreground) {
            TestLogger.log("   ↪ Already running and foreground — skipping");
            return;
        }

        if (running) {
            TestLogger.log("   ↪ Process alive but not foreground — bringing to front");
        } else {
            TestLogger.log("   ↪ Not running — launching");
        }

        device.bringAppToForeground(appPackage);

        if (!waitForAppForegroundReliable(TestConfig.FOREGROUND_WAIT_TIMEOUT_MS)) {
            throw new RuntimeException("App did not reach foreground within " +
                    TestConfig.FOREGROUND_WAIT_TIMEOUT_MS + " ms");
        }

        TestLogger.log("   ↪ App is now foreground");
    }

    /**
     * Force-stops the app process.
     */
    public void ensureAppStopped() throws Exception {
        TestLogger.log("🔧 ensureAppStopped");
        if (!state.isAppRunning()) {
            TestLogger.log("   ↪ Already stopped — skipping");
            return;
        }
        TestLogger.log("   ↪ Running — force-stopping");
        device.forceStopApp(appPackage);
        Thread.sleep(1000);
        if (state.isAppRunning()) {
            throw new RuntimeException("App still running after force-stop");
        }
    }

    /**
     * Force-stops then relaunches.
     */
    public void ensureAppRestarted() throws Exception {
        TestLogger.log("🔧 ensureAppRestarted");
        ensureAppStopped();
        device.bringAppToForeground(appPackage);
        if (!waitForAppForegroundReliable(TestConfig.FOREGROUND_WAIT_TIMEOUT_MS)) {
            throw new RuntimeException("App did not reach foreground after restart");
        }
    }

    // ==================== SCREEN-LEVEL ====================

    /**
     * Ensures the device is on the login screen.
     */
    public void ensureOnLoginScreen() throws Exception {
        TestLogger.log("🔧 ensureOnLoginScreen");

        ensureAppRunning();

        if (state.isOnLoginScreen()) {
            TestLogger.log("   ↪ Already on login screen — skipping");
            return;
        }

        if (state.isLoggedIn()) {
            throw new RuntimeException(
                    "Device is currently logged in. ensureLoggedOut() is not yet implemented.");
        }

        if (waitForScreen("txtUserCellPhone", TestConfig.LOGIN_SCREEN_TIMEOUT_MS)) {
            TestLogger.log("   ↪ Login screen appeared after waiting");
            return;
        }

        // Diagnostics before failing
        TestLogger.logError("   ❌ Login screen did not appear — capturing diagnostics");
        try {
            device.takeScreenshot(TestConfig.FAIL_DIR, testStartTime);
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            TestLogger.log("   Foreground package (dumpsys): " + device.getForegroundPackage());
            TestLogger.log("   Root package (UI dump):       " + parser.getRootPackage(xml));
            TestLogger.log("   Screen label:                 " + state.getCurrentScreenLabel());
            TestLogger.log("   XML length: " + xml.length());
        } catch (Exception e) {
            TestLogger.logWarning("   ⚠️ Diagnostic capture failed: " + e.getMessage());
        }

        throw new RuntimeException("Could not reach login screen within " +
                TestConfig.LOGIN_SCREEN_TIMEOUT_MS + " ms.");
    }

    /**
     * Ensures the device is on the OTP screen.
     */
    public void ensureOnOtpScreen(String phoneNumber) throws Exception {
        TestLogger.log("🔧 ensureOnOtpScreen");

        ensureAppRunning();

        if (state.isOnOtpScreen()) {
            TestLogger.log("   ↪ Already on OTP screen — skipping");
            return;
        }

        if (state.isOnLoginScreen()) {
            TestLogger.log("   ↪ On login screen — entering phone number");
            loginFlow.enterPhoneNumber(phoneNumber);
            loginFlow.pressConnect();
            loginFlow.waitForOTP();
            if (!state.isOnOtpScreen()) {
                throw new RuntimeException("OTP screen did not appear after entering phone number");
            }
            return;
        }

        if (state.isLoggedIn()) {
            throw new RuntimeException(
                    "Device is currently logged in. Cannot reach OTP screen without logging out first. " +
                            "ensureLoggedOut() is not yet implemented.");
        }

        ensureOnLoginScreen();
        loginFlow.enterPhoneNumber(phoneNumber);
        loginFlow.pressConnect();
        loginFlow.waitForOTP();
        if (!state.isOnOtpScreen()) {
            throw new RuntimeException("OTP screen did not appear after entering phone number");
        }
    }

    /**
     * Ensures the user is fully logged in.
     */
    public void ensureLoggedIn(String phoneNumber, String otp) throws Exception {
        TestLogger.log("🔧 ensureLoggedIn");

        ensureAppRunning();

        if (state.isLoggedIn()) {
            TestLogger.log("   ↪ Already logged in — skipping");
            return;
        }

        ensureOnOtpScreen(phoneNumber);
        TestLogger.log("   ↪ On OTP screen — entering OTP code");
        otpService.enterOTP(otp);
        otpService.pressVerify();

        if (!waitUntilLoggedIn(30000)) {
            throw new RuntimeException("Login did not complete within 30s after entering OTP");
        }
        TestLogger.log("   ↪ Logged in successfully");
    }

    // ==================== GENERIC ====================

    public void ensureOnScreen(String markerResourceId, int timeoutMs) throws Exception {
        TestLogger.log("🔧 ensureOnScreen(" + markerResourceId + ")");

        if (state.isOnScreen(markerResourceId)) {
            TestLogger.log("   ↪ Already on screen — skipping");
            return;
        }

        if (waitForScreen(markerResourceId, timeoutMs)) {
            TestLogger.log("   ↪ Screen appeared after waiting");
            return;
        }

        throw new RuntimeException("Screen with marker '" + markerResourceId +
                "' did not appear within " + timeoutMs + "ms");
    }

    // ==================== HELPERS ====================

    private boolean waitForScreen(String resourceIdMarker, int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            if (parser.containsResourceId(xml, resourceIdMarker)) {
                return true;
            }
            Thread.sleep(1000);
        }
        return false;
    }

    private boolean waitForAppForegroundReliable(int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (state.isAppForegroundReliable()) {
                return true;
            }
            Thread.sleep(500);
        }
        return false;
    }

    private boolean waitUntilLoggedIn(int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (state.isLoggedIn()) return true;
            Thread.sleep(1000);
        }
        return false;
    }
}