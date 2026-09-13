package AndroidTV.V3.services;

import AndroidTV.V3.Models.Router;
import AndroidTV.V3.config.RouterConfig;
import AndroidTV.V3.config.TestConfig;
import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.utils.TestLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the current Wi-Fi SSID and the phone number to use for the current router.
 *
 * Fallback policy (Choice A — static):
 *   - If SSID is in RouterConfig and has a non-empty validPhoneNumber → use it.
 *   - Otherwise → fall back to TestConfig.REGULAR_PHONE_NUMBER.
 *
 * Also provides a small log block for test start, plus a mismatch warning for callers
 * that pass a custom phone number differing from the router's expected one.
 */
public class SsidService {

    /** Simple holder for router info. */
    public static class RouterInfo {
        public final String ssid;
        public final boolean routerFound;
        public final String expectedPhone;
        public final String phoneToUse;
        public final String status;

        public RouterInfo(String ssid, boolean routerFound, String expectedPhone,
                          String phoneToUse, String status) {
            this.ssid = ssid;
            this.routerFound = routerFound;
            this.expectedPhone = expectedPhone;
            this.phoneToUse = phoneToUse;
            this.status = status;
        }
    }

    private final DeviceController device;
    private final String xmlFolderPath;
    private final String testStartTime;

    public SsidService(DeviceController device, String xmlFolderPath, String testStartTime) {
        this.device = device;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
    }

    // ==================== SSID ====================

    /**
     * Reads the current Wi-Fi SSID from the device. Returns "Unknown" if not found.
     */
    public String getCurrentSSID() throws Exception {
        String output = runAdbShell("dumpsys wifi");

        Pattern p = Pattern.compile("SSID:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(output);
        if (m.find()) {
            return m.group(1);
        }
        return "Unknown";
    }

    /**
     * Internal helper: runs "adb -s <udid> shell <shellCommand>" and returns stdout.
     */
    private String runAdbShell(String shellCommand) throws Exception {
        String udid = TestConfig.DEVICE_UDID;

        ProcessBuilder pb = new ProcessBuilder("adb", "-s", udid, "shell", shellCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader reader =
                     new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        process.waitFor();
        return sb.toString();
    }

    // ==================== ROUTER INFO ====================

    /**
     * Resolves the current SSID, looks up the router config, and decides which
     * phone number the test should use.
     */
    public RouterInfo getCurrentRouterInfo() throws Exception {
        String ssid = getCurrentSSID();
        Router router = RouterConfig.getRouterBySSID(ssid);

        if (router == null) {
            return new RouterInfo(
                    ssid, false, "",
                    TestConfig.REGULAR_PHONE_NUMBER,
                    "FALLBACK_ROUTER_NOT_FOUND");
        }

        String expected = router.getValidPhoneNumber();
        if (expected == null || expected.trim().isEmpty()) {
            return new RouterInfo(
                    ssid, true, "",
                    TestConfig.REGULAR_PHONE_NUMBER,
                    "FALLBACK_NO_PHONE_DEFINED");
        }

        return new RouterInfo(
                ssid, true, expected,
                expected,
                "MATCH");
    }

    /**
     * Convenience: returns the phone number the test should use for the current SSID.
     */
    public String getPhoneNumberForCurrentSSID() throws Exception {
        return getCurrentRouterInfo().phoneToUse;
    }

    // ==================== LOGGING ====================

    /**
     * Prints the compact router-check block at test start.
     */
    public void logRouterCheck() throws Exception {
        RouterInfo info = getCurrentRouterInfo();

        TestLogger.log("");
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("📶 ROUTER & PHONE NUMBER CHECK");
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("   Current SSID:       " + info.ssid);
        TestLogger.log("   Expected phone:     " +
                (info.expectedPhone.isEmpty() ? "(not defined)" : info.expectedPhone));
        TestLogger.log("   Test will use:      " + info.phoneToUse);

        switch (info.status) {
            case "MATCH":
                TestLogger.logSuccess("   Status:             ✅ MATCH — using the router's configured phone number");
                break;
            case "FALLBACK_ROUTER_NOT_FOUND":
                TestLogger.logWarning("   Status:             ⚠️ FALLBACK — SSID not in RouterConfig");
                TestLogger.logWarning("                       Using default REGULAR_PHONE_NUMBER");
                break;
            case "FALLBACK_NO_PHONE_DEFINED":
                TestLogger.logWarning("   Status:             ⚠️ FALLBACK — router found but no validPhoneNumber set");
                TestLogger.logWarning("                       Using default REGULAR_PHONE_NUMBER");
                break;
            default:
                TestLogger.logWarning("   Status:             ⚠️ Unknown status");
        }

        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("");
    }

    /**
     * Prints a warning if the caller passes a phone number that differs from the
     * router's expected one. Does not fail.
     */
    public void logPhoneNumberOverrideWarning(String insertedPhone) throws Exception {
        RouterInfo info = getCurrentRouterInfo();
        if (info.expectedPhone.isEmpty()) return;
        if (info.expectedPhone.equals(insertedPhone)) return;

        TestLogger.log("");
        TestLogger.logWarning("⚠️ PHONE NUMBER MISMATCH");
        TestLogger.logWarning("   Router expects: " + info.expectedPhone);
        TestLogger.logWarning("   You are using:  " + insertedPhone);
        TestLogger.logWarning("   Proceeding anyway — verify this is intentional.");
        TestLogger.log("");
    }
}