package AndroidTV.V3.config;

/**
 * Central configuration for all ADB UI tests.
 * Single source of truth for:
 *   - device identity
 *   - app identity
 *   - timeouts
 *   - test data
 *   - all file paths (absolute)
 *   - image comparison thresholds
 */
public final class ADBTestConfig {

    private ADBTestConfig() {
        // constants only — do not instantiate
    }

    // ==================== DEVICE ====================
    public static final String DEVICE_UDID = "192.168.1.165:5555";
    public static final String HOT_PACKAGE = "il.net.hot.hot";

    // ==================== TIMEOUTS (ms) ====================
    public static final int APP_LOAD_TIMEOUT = 60000;
    public static final int OTP_LOAD_TIMEOUT = 30000;
    public static final int SCREEN_MARKER_TIMEOUT_MS = 60000;
    public static final int LOGIN_SCREEN_TIMEOUT_MS = 30000;
    public static final int FOREGROUND_WAIT_TIMEOUT_MS = 15000;
    public static final int DEFAULT_POLL_INTERVAL_MS = 1000;
    public static final int KEYPAD_NAVIGATION_DELAY = 150;
    public static final int AFTER_CLICK_DELAY = 3000;
    public static final int DEFAULT_WAIT = 1000;

    // ==================== TEST DATA ====================
    public static final String REGULAR_PHONE_NUMBER = "0543501323";
    public static final String REGULAR_OTP = "123456";

    // ==================== PROJECT ROOT & DERIVED PATHS ====================
    public static final String PROJECT_ROOT = "/Users/Johnny/IdeaProjects/POC";

    public static final String SCREENS_DIR         = PROJECT_ROOT + "/Screens";
    public static final String CURRENT_SCREEN_DIR  = SCREENS_DIR + "/Current screen";
    public static final String FAIL_DIR            = SCREENS_DIR + "/Fail";
    public static final String PASS_DIR            = SCREENS_DIR + "/Pass";
    public static final String EXPECTED_DIR        = SCREENS_DIR + "/Expected";

    public static final String XML_DIR  = PROJECT_ROOT + "/xml";
    public static final String LOGS_DIR = PROJECT_ROOT + "/logs";

    // ==================== KEYPAD REFERENCES ====================
    public static final String KEYPAD_REF_SELECTED_DIR =
            EXPECTED_DIR + "/Keypad References/Key selected";

    // ==================== IMAGE COMPARISON ====================
    public static final double SIMILARITY_THRESHOLD = 95.0;
    public static final int PIXEL_TOLERANCE = 10;
}