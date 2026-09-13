package AndroidTV.V3.Tests.User;

import AndroidTV.V3.config.RouterConfig;
import AndroidTV.V3.Models.Router;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SwitchingWiFiTest {

    private String testStartTime;
    private final String deviceUDID = "192.168.1.10:5555";
    private final String appPackage = "com.hot.android.tv";

    // Remote control key constants from RCU Kyes.txt
    private static final int KEY_DPAD_LEFT = 21;
    private static final int KEY_DPAD_RIGHT = 22;
    private static final int KEY_DPAD_UP = 19;
    private static final int KEY_DPAD_DOWN = 20;
    private static final int KEY_DPAD_CENTER = 23;
    private static final int KEY_F8_HOT = 66;
    private static final int KEY_HOME = 3;

    // Output folders
    private final String passFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Pass";
    private final String failFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Fail";
    private final String xmlFolderPath = "/Users/Johnny/IdeaProjects/POC/xml";

    public static void main(String[] args) {
        new SwitchingWiFiTest().runTest();
    }

    public void runTest() {
        testStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        try {
            System.out.println("═══════════════════════════════════════════════════");
            System.out.println("📡 SWITCHING WIFI TEST - PURE ADB");
            System.out.println("═══════════════════════════════════════════════════");

            System.out.println("Step 1: Connecting to device via ADB");
            connectDevice();

            System.out.println("Step 2: Printing current SSID before switch");
            printCurrentSSID();

            System.out.println("Step 3: Press power button long press");
            longPressPowerButton();

            System.out.println("Step 4: Press the left button");
            pressDpad(KEY_DPAD_LEFT);

            System.out.println("Step 5: Press Ok button");
            pressDpad(KEY_DPAD_CENTER);

            System.out.println("Step 6: Wait 2 seconds");
            Thread.sleep(2000);

            System.out.println("Step 7: Press the down button");
            pressDpad(KEY_DPAD_DOWN);

            System.out.println("Step 8: Press the down button");
            pressDpad(KEY_DPAD_DOWN);

            System.out.println("Step 9: Press Ok button");
            pressDpad(KEY_DPAD_CENTER);

            System.out.println("Step 10: Press Ok button");
            pressDpad(KEY_DPAD_CENTER);

            System.out.println("Step 11: Wait until WiFi connection");
            waitForWiFiConnection();

            System.out.println("Step 12: Press home button");
            pressHome();

            System.out.println("Step 13: Make a new app session");
            startNewAppSession();

            System.out.println("Step 14: Verifying new WiFi connection");
            verifyWiFiConnection();

            System.out.println("✅ SWITCHING WIFI TEST PASSED!");
            printResultsSummary(true);

        } catch (Exception e) {
            System.out.println("❌ Test failed: " + e.getMessage());
            takeScreenshot("test_failure");
            savePageSource("test_failure");
            printResultsSummary(false);
            System.exit(1);
        }
    }

    // ==================== ADB COMMAND HELPERS ====================

    private String runCommand(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code " + exitCode + ": " + String.join(" ", command));
        }

        return output.toString();
    }

    private void connectDevice() throws Exception {
        System.out.println("   🔌 Connecting to device: " + deviceUDID);
        runCommand("adb", "connect", deviceUDID);
        Thread.sleep(2000);
        System.out.println("✅ Device connected");
    }

    private void pressDpad(int keyCode) throws Exception {
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", String.valueOf(keyCode));
        Thread.sleep(300);
    }

    private void longPressPowerButton() throws Exception {
        System.out.println("   🔘 Long pressing Power button...");
        // Use --longpress flag with KEYCODE_POWER
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "--longpress", "KEYCODE_POWER");
        Thread.sleep(3000);
        System.out.println("✅ Power button long pressed");
    }

    private void pressHome() throws Exception {
        System.out.println("   🏠 Pressing HOME button");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", String.valueOf(KEY_HOME));
        Thread.sleep(1000);
        System.out.println("✅ HOME button pressed");
    }

    private void startNewAppSession() throws Exception {
        System.out.println("   🚀 Starting new app session: " + appPackage);
        runCommand("adb", "-s", deviceUDID, "shell", "am", "force-stop", appPackage);
        Thread.sleep(2000);
        System.out.println("   Pressing HOT button (KEY_F8) to launch app");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", String.valueOf(KEY_F8_HOT));
        Thread.sleep(3000);
        System.out.println("✅ New app session started");
    }

    // ==================== WIFI HELPERS ====================

    private void printCurrentSSID() throws Exception {
        String ssid = getCurrentSSID();
        System.out.println("   📶 Current SSID: " + ssid);
    }

    private String getCurrentSSID() throws Exception {
        String output = runCommand("adb", "-s", deviceUDID, "shell", "dumpsys", "wifi");
        Pattern pattern = Pattern.compile("SSID: \"([^\"]+)\"");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown";
    }

    private void waitForWiFiConnection() throws Exception {
        System.out.println("   ⏳ Waiting for WiFi connection...");
        long startTime = System.currentTimeMillis();
        long timeout = 30000; // 30 seconds timeout
        boolean wifiConnected = false;

        while (System.currentTimeMillis() - startTime < timeout) {
            String ssid = getCurrentSSID();
            if (!ssid.equals("Unknown") && !ssid.isEmpty()) {
                System.out.println("   📶 WiFi connected to: " + ssid);
                wifiConnected = true;
                break;
            }
            System.out.println("   ⏳ Still waiting for WiFi connection...");
            Thread.sleep(2000);
        }

        if (!wifiConnected) {
            System.out.println("   ⚠️ WiFi connection timeout!");
        } else {
            System.out.println("✅ WiFi connection established");
        }
        Thread.sleep(3000);
    }

    private void verifyWiFiConnection() throws Exception {
        System.out.println("   🔍 Verifying WiFi connection after switching...");
        Thread.sleep(3000);

        String ssid = getCurrentSSID();
        System.out.println("   📶 New SSID: " + ssid);

        Router router = RouterConfig.getRouterBySSID(ssid);
        if (router != null) {
            System.out.println("✅ Connected to router: " + router.getSsid());
            System.out.println("   📍 MAC: " + router.getMacAddress());
            System.out.println("   📍 Type: " + router.getRouterType());
            System.out.println("   📍 Valid Phone: " + router.getValidPhoneNumber());
        } else {
            System.out.println("⚠️ Router SSID not found in config: " + ssid);
        }
    }

    // ==================== SCREENSHOT & XML HELPERS ====================

    private void takeScreenshot(String name) {
        try {
            String remotePath = "/sdcard/screen_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);

            String localPath = failFolderPath + "/" + name + "_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "pull", remotePath, localPath);

            System.out.println("📸 Screenshot saved to: " + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            System.out.println("⚠️ Could not take screenshot: " + e.getMessage());
        }
    }

    private void savePageSource(String name) {
        try {
            String remoteDumpPath = "/sdcard/dump_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "shell", "uiautomator", "dump", remoteDumpPath);
            String localPath = xmlFolderPath + "/" + name + "_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localPath);
            System.out.println("📄 Page Source: " + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            System.out.println("⚠️ Could not save page source: " + e.getMessage());
        }
    }

    // ==================== REPORTING ====================

    private void printResultsSummary(boolean passed) {
        System.out.println("");
        System.out.println("═══ TEST RESULTS SUMMARY ═══");
        System.out.println("📌 Status: " + (passed ? "✅ PASSED" : "❌ FAILED"));
        System.out.println("🕐 Timestamp: " + testStartTime);
    }
}