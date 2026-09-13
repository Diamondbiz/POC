package AndroidTV.V3.flows;
import AndroidTV.V3.services.ADBOTPService;
import AndroidTV.V3.config.ADBTestConfig;
import AndroidTV.V3.core.ADBDeviceController;
import AndroidTV.V3.core.ADBScreenState;
import AndroidTV.V3.core.ADBXmlParser;
import AndroidTV.V3.utils.ADBTestLogger;

/**
 * State enforcer layer.
 * Every enforcer checks first (read-only), then acts only if needed.
 * Never asserts. Throws only when target state cannot be reached.
 */
public class ADBPreconditions {

    private final ADBDeviceController device;
    private final ADBXmlParser parser;
    private final ADBScreenState state;
    private final ADBLoginFlow loginFlow;
    private final ADBOTPService otpService;
    private final String xmlFolderPath;
    private final String testStartTime;
    private final String appPackage;

    public ADBPreconditions(ADBDeviceController device,
                            ADBXmlParser parser,
                            ADBScreenState state,
                            ADBLoginFlow loginFlow,
                            ADBOTPService otpService,
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
        ADBTestLogger.log("🔧 ensureAppRunning");

        boolean running = state.isAppRunning();
        boolean foreground = running && state.isAppForegroundReliable();

        if (running && foreground) {
            ADBTestLogger.log("   ↪ Already running and foreground — skipping");
            return;
        }

        if (running) {
            ADBTestLogger.log("   ↪ Process alive but not foreground — bringing to front");
        } else {
            ADBTestLogger.log("   ↪ Not running — launching");
        }

        device.bringAppToForeground(appPackage);

        if (!waitForAppForegroundReliable(ADBTestConfig.FOREGROUND_WAIT_TIMEOUT_MS)) {
            throw new RuntimeException("App did not reach foreground within " +
                    ADBTestConfig.FOREGROUND_WAIT_TIMEOUT_MS + " ms");
        }

        ADBTestLogger.log("   ↪ App is now foreground");
    }

    /**
     * Force-stops the app process.
     */
    public void ensureAppStopped() throws Exception {
        ADBTestLogger.log("🔧 ensureAppStopped");
        if (!state.isAppRunning()) {
            ADBTestLogger.log("   ↪ Already stopped — skipping");
            return;
        }
        ADBTestLogger.log("   ↪ Running — force-stopping");
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
        ADBTestLogger.log("🔧 ensureAppRestarted");
        ensureAppStopped();
        device.bringAppToForeground(appPackage);
        if (!waitForAppForegroundReliable(ADBTestConfig.FOREGROUND_WAIT_TIMEOUT_MS)) {
            throw new RuntimeException("App did not reach foreground after restart");
        }
    }

    // ==================== SCREEN-LEVEL ====================

    /**
     * Ensures the device is on the login screen.
     */
    public void ensureOnLoginScreen() throws Exception {
        ADBTestLogger.log("🔧 ensureOnLoginScreen");

        ensureAppRunning();

        if (state.isOnLoginScreen()) {
            ADBTestLogger.log("   ↪ Already on login screen — skipping");
            return;
        }

        if (state.isLoggedIn()) {
            throw new RuntimeException(
                    "Device is currently logged in. ensureLoggedOut() is not yet implemented.");
        }

        if (waitForScreen("txtUserCellPhone", ADBTestConfig.LOGIN_SCREEN_TIMEOUT_MS)) {
            ADBTestLogger.log("   ↪ Login screen appeared after waiting");
            return;
        }

        // Diagnostics before failing
        ADBTestLogger.logError("   ❌ Login screen did not appear — capturing diagnostics");
        try {
            device.takeScreenshot(ADBTestConfig.FAIL_DIR, testStartTime);
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            ADBTestLogger.log("   Foreground package (dumpsys): " + device.getForegroundPackage());
            ADBTestLogger.log("   Root package (UI dump):       " + parser.getRootPackage(xml));
            ADBTestLogger.log("   Screen label:                 " + state.getCurrentScreenLabel());
            ADBTestLogger.log("   XML length: " + xml.length());
        } catch (Exception e) {
            ADBTestLogger.logWarning("   ⚠️ Diagnostic capture failed: " + e.getMessage());
        }

        throw new RuntimeException("Could not reach login screen within " +
                ADBTestConfig.LOGIN_SCREEN_TIMEOUT_MS + " ms.");
    }

    /**
     * Ensures the device is on the OTP screen.
     */
    public void ensureOnOtpScreen(String phoneNumber) throws Exception {
        ADBTestLogger.log("🔧 ensureOnOtpScreen");

        ensureAppRunning();

        if (state.isOnOtpScreen()) {
            ADBTestLogger.log("   ↪ Already on OTP screen — skipping");
            return;
        }

        if (state.isOnLoginScreen()) {
            ADBTestLogger.log("   ↪ On login screen — entering phone number");
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
        ADBTestLogger.log("🔧 ensureLoggedIn");

        ensureAppRunning();

        if (state.isLoggedIn()) {
            ADBTestLogger.log("   ↪ Already logged in — skipping");
            return;
        }

        ensureOnOtpScreen(phoneNumber);
        ADBTestLogger.log("   ↪ On OTP screen — entering OTP code");
        otpService.enterOTP(otp);
        otpService.pressVerify();

        if (!waitUntilLoggedIn(30000)) {
            throw new RuntimeException("Login did not complete within 30s after entering OTP");
        }
        ADBTestLogger.log("   ↪ Logged in successfully");
    }

    // ==================== GENERIC ====================

    public void ensureOnScreen(String markerResourceId, int timeoutMs) throws Exception {
        ADBTestLogger.log("🔧 ensureOnScreen(" + markerResourceId + ")");

        if (state.isOnScreen(markerResourceId)) {
            ADBTestLogger.log("   ↪ Already on screen — skipping");
            return;
        }

        if (waitForScreen(markerResourceId, timeoutMs)) {
            ADBTestLogger.log("   ↪ Screen appeared after waiting");
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