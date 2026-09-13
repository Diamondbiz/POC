package AndroidTV.V3.services;

import AndroidTV.V3.Models.Router;
import AndroidTV.V3.config.ADBRouterConfig;
import AndroidTV.V3.config.ADBTestConfig;
import AndroidTV.V3.core.ADBDeviceController;
import AndroidTV.V3.utils.ADBTestLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the current Wi-Fi SSID and the phone number to use for the current router.
 *
 * Fallback policy (Choice A — static):
 *   - If SSID is in ADBRouterConfig and has a non-empty validPhoneNumber → use it.
 *   - Otherwise → fall back to ADBTestConfig.REGULAR_PHONE_NUMBER.
 *
 * Also provides a small log block for test start, plus a mismatch warning for callers
 * that pass a custom phone number differing from the router's expected one.
 */
public class SSIDService {

    /** Simple holder for router info. */
    public static class RouterInfo {
        public final String ssid;
        public final boolean routerFound;
        public final String expectedPhone;   // empty if unknown
        public final String phoneToUse;      // always non-empty
        public final String status;          // "MATCH", "FALLBACK_ROUTER_NOT_FOUND",
        // "FALLBACK_NO_PHONE_DEFINED"

        public RouterInfo(String ssid, boolean routerFound, String expectedPhone,
                          String phoneToUse, String status) {
            this.ssid = ssid;
            this.routerFound = routerFound;
            this.expectedPhone = expectedPhone;
            this.phoneToUse = phoneToUse;
            this.status = status;
        }
    }

    private final ADBDeviceController device;
    private final String xmlFolderPath;
    private final String testStartTime;

    public SSIDService(ADBDeviceController device, String xmlFolderPath, String testStartTime) {
        this.device = device;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
    }

    // ==================== SSID ====================

    /**
     * Reads the current Wi-Fi SSID from the device. Returns "Unknown" if not found.
     *
     * Runs "dumpsys wifi" and filters in Java (avoids shell quoting issues).
     */
    public String getCurrentSSID() throws Exception {
        // We need a raw command runner here. Use ADBDeviceController's public methods
        // via a temporary XML dump is not viable; instead we leverage the device's
        // generic exec path. ADBDeviceController currently doesn't expose a raw
        // "runCommand" — so we shell out to adb directly via a short bash fallback.
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
     * Bypasses the strict runCommand because some shell commands return non-zero
     * but are still informative (e.g. grep).
     */
    private String runAdbShell(String shellCommand) throws Exception {
        // We can't access ADBDeviceController's private runCommand.
        // Execute adb directly here. The UDID comes from the device instance,
        // so we re-resolve it from ADBTestConfig for consistency.
        String udid = ADBTestConfig.DEVICE_UDID;

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
        process.waitFor(); // ignore exit code — we only care about content
        return sb.toString();
    }

    // ==================== ROUTER INFO ====================

    /**
     * Resolves the current SSID, looks up the router config, and decides which
     * phone number the test should use.
     */
    public RouterInfo getCurrentRouterInfo() throws Exception {
        String ssid = getCurrentSSID();
        Router router = ADBRouterConfig.getRouterBySSID(ssid);

        if (router == null) {
            return new RouterInfo(
                    ssid, false, "",
                    ADBTestConfig.REGULAR_PHONE_NUMBER,
                    "FALLBACK_ROUTER_NOT_FOUND");
        }

        String expected = router.getValidPhoneNumber();
        if (expected == null || expected.trim().isEmpty()) {
            return new RouterInfo(
                    ssid, true, "",
                    ADBTestConfig.REGULAR_PHONE_NUMBER,
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

        ADBTestLogger.log("");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("📶 ROUTER & PHONE NUMBER CHECK");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("   Current SSID:       " + info.ssid);
        ADBTestLogger.log("   Expected phone:     " +
                (info.expectedPhone.isEmpty() ? "(not defined)" : info.expectedPhone));
        ADBTestLogger.log("   Test will use:      " + info.phoneToUse);

        switch (info.status) {
            case "MATCH":
                ADBTestLogger.logSuccess("   Status:             ✅ MATCH — using the router's configured phone number");
                break;
            case "FALLBACK_ROUTER_NOT_FOUND":
                ADBTestLogger.logWarning("   Status:             ⚠️ FALLBACK — SSID not in ADBRouterConfig");
                ADBTestLogger.logWarning("                       Using default REGULAR_PHONE_NUMBER");
                break;
            case "FALLBACK_NO_PHONE_DEFINED":
                ADBTestLogger.logWarning("   Status:             ⚠️ FALLBACK — router found but no validPhoneNumber set");
                ADBTestLogger.logWarning("                       Using default REGULAR_PHONE_NUMBER");
                break;
            default:
                ADBTestLogger.logWarning("   Status:             ⚠️ Unknown status");
        }

        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("");
    }

    /**
     * Prints a warning if the caller passes a phone number that differs from the
     * router's expected one. Does not fail. Intended for future use when tests
     * accept a custom phone number.
     */
    public void logPhoneNumberOverrideWarning(String insertedPhone) throws Exception {
        RouterInfo info = getCurrentRouterInfo();
        if (info.expectedPhone.isEmpty()) {
            return; // nothing to compare against
        }
        if (info.expectedPhone.equals(insertedPhone)) {
            return; // matches — no warning
        }

        ADBTestLogger.log("");
        ADBTestLogger.logWarning("⚠️ PHONE NUMBER MISMATCH");
        ADBTestLogger.logWarning("   Router expects: " + info.expectedPhone);
        ADBTestLogger.logWarning("   You are using:  " + insertedPhone);
        ADBTestLogger.logWarning("   Proceeding anyway — verify this is intentional.");
        ADBTestLogger.log("");
    }
}