package AndroidTV.V3.core;

/**
 * Read-only screen state detector.
 * Answers "where is the device right now?" — never performs side effects.
 */
public class ADBScreenState {

    private final ADBDeviceController device;
    private final ADBXmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;
    private final String appPackage;

    public ADBScreenState(ADBDeviceController device,
                          ADBXmlParser parser,
                          String xmlFolderPath,
                          String testStartTime,
                          String appPackage) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
        this.appPackage = appPackage;
    }

    // ==================== APP-LEVEL ====================

    /**
     * True if the app process is running.
     */
    public boolean isAppRunning() throws Exception {
        return device.isAppRunning(appPackage);
    }

    /**
     * True if dumpsys reports our app as the foreground package.
     * (Frequently wrong on Android TV — prefer isAppForegroundReliable.)
     */
    public boolean isAppForeground() throws Exception {
        String fg = device.getForegroundPackage();
        return fg != null && fg.equals(appPackage);
    }

    /**
     * Two-signal foreground check:
     *   1) dumpsys window says our app is focused, OR
     *   2) the current UI dump's root package is our app.
     *
     * Signal 2 is the ground truth on Android TV, where dumpsys often
     * misreports the launcher as focused even when our app is visibly drawn.
     */
    public boolean isAppForegroundReliable() throws Exception {
        // Signal 1: dumpsys
        String fg = device.getForegroundPackage();
        if (fg != null && fg.equals(appPackage)) {
            return true;
        }

        // Signal 2: UI dump root package
        try {
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            String rootPkg = parser.getRootPackage(xml);
            return rootPkg != null && rootPkg.equals(appPackage);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * True if the app is running AND reliably foreground.
     */
    public boolean isAppActive() throws Exception {
        return isAppRunning() && isAppForegroundReliable();
    }

    // ==================== SCREEN-LEVEL ====================

    private String getXml() throws Exception {
        return device.getScreenXml(xmlFolderPath, testStartTime);
    }

    /**
     * True if the login screen markers are present.
     */
    public boolean isOnLoginScreen() throws Exception {
        if (!isAppActive()) return false;
        String xml = getXml();
        return parser.isAllLoginScreenElementsPresent(xml);
    }

    /**
     * True if the OTP screen markers are present.
     */
    public boolean isOnOtpScreen() throws Exception {
        if (!isAppActive()) return false;
        String xml = getXml();
        return parser.isAllOTPElementsPresent(xml);
    }

    /**
     * True if the app is running and we are past login/OTP.
     */
    public boolean isLoggedIn() throws Exception {
        if (!isAppActive()) return false;
        if (isOnLoginScreen()) return false;
        if (isOnOtpScreen()) return false;
        return true;
    }

    /**
     * Generic check: is the given resource id present in the current XML?
     */
    public boolean isOnScreen(String resourceIdMarker) throws Exception {
        if (!isAppActive()) return false;
        String xml = getXml();
        return parser.containsResourceId(xml, resourceIdMarker);
    }

    // ==================== HUMAN-READABLE LABEL ====================

    /**
     * Returns a short label describing the current screen state (for logging).
     */
    public String getCurrentScreenLabel() throws Exception {
        if (!isAppRunning()) return "APP_NOT_RUNNING";
        if (!isAppForegroundReliable()) return "APP_BACKGROUND";

        String xml = getXml();
        if (parser.isAllLoginScreenElementsPresent(xml)) return "LOGIN_SCREEN";
        if (parser.isAllOTPElementsPresent(xml)) return "OTP_SCREEN";
        if (parser.containsText(xml, "משודר עכשיו")) return "LIVE_MOSAIC_SCREEN";

        return "UNKNOWN_SCREEN";
    }
}