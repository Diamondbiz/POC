package AndroidTV.V3.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles all low-level ADB commands.
 * This is the ONLY class that knows how to execute commands via ProcessBuilder.
 */
public class ADBDeviceController {

    private final String deviceUDID;

    public ADBDeviceController(String deviceUDID) {
        this.deviceUDID = deviceUDID;
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Executes an ADB command and returns the output.
     */
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
            throw new RuntimeException("ADB command failed (exit " + exitCode + "): " + String.join(" ", command));
        }
        return output.toString();
    }

    // ==================== DEVICE CONNECTION ====================

    public void connect() throws Exception {
        runCommand("adb", "connect", deviceUDID);
        System.out.println("✅ Connected to device: " + deviceUDID);
    }

    // ==================== KEY EVENTS ====================

    public void pressKey(int keyCode) throws Exception {
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", String.valueOf(keyCode));
        Thread.sleep(100);
    }

    public void pressHome() throws Exception {
        pressKey(ADBKeyCodes.HOME);
        Thread.sleep(500);
    }

    public void pressBack() throws Exception {
        pressKey(ADBKeyCodes.BACK);
        Thread.sleep(500);
    }

    public void pressDpadUp() throws Exception {
        pressKey(ADBKeyCodes.DPAD_UP);
    }

    public void pressDpadDown() throws Exception {
        pressKey(ADBKeyCodes.DPAD_DOWN);
    }

    public void pressDpadLeft() throws Exception {
        pressKey(ADBKeyCodes.DPAD_LEFT);
    }

    public void pressDpadRight() throws Exception {
        pressKey(ADBKeyCodes.DPAD_RIGHT);
    }

    public void pressCenter() throws Exception {
        pressKey(ADBKeyCodes.DPAD_CENTER);
    }

    // ==================== APP CONTROL ====================

    public void launchApp() throws Exception {
        pressHome();
        Thread.sleep(1000);
        pressCenter();  // HOT icon is auto-focused after HOME on this device
        System.out.println("🚀 HOT app launched");
        Thread.sleep(3000);
    }

    public void forceStopApp(String packageName) throws Exception {
        runCommand("adb", "-s", deviceUDID, "shell", "am", "force-stop", packageName);
        System.out.println("🛑 Force stopped: " + packageName);
    }

    // ==================== SCREEN CAPTURE ====================

    /**
     * Dumps the UI hierarchy XML and returns its content as a string.
     */
    public String getScreenXml(String xmlFolderPath, String testStartTime) throws Exception {
        String remoteDumpPath = "/sdcard/dump_" + testStartTime + ".xml";
        runCommand("adb", "-s", deviceUDID, "shell", "uiautomator", "dump", remoteDumpPath);

        String localDumpPath = xmlFolderPath + "/dump_" + testStartTime + ".xml";
        runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localDumpPath);

        return new String(Files.readAllBytes(Paths.get(localDumpPath)));
    }

    /**
     * Takes a screenshot and returns the local file.
     */
    public File takeScreenshot(String savePath, String testStartTime) throws Exception {
        String remotePath = "/sdcard/screen_" + testStartTime + ".png";
        runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);

        File localFile = new File(savePath + "/screen_" + testStartTime + ".png");
        runCommand("adb", "-s", deviceUDID, "pull", remotePath, localFile.getAbsolutePath());

        System.out.println("📸 Screenshot saved to: " + localFile.getAbsolutePath());
        return localFile;
    }

    // ==================== UTILITY ====================

    public String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    // ==================== APP / PROCESS STATE ====================

    /**
     * Returns true if the given package is currently running (has an active process).
     *
     * Uses "pidof <pkg> || true" so the shell command always exits 0.
     * If the process is running → output is the PID.
     * If the process is not running → output is empty.
     * Either way, runCommand won't throw.
     */
    public boolean isAppRunning(String packageName) throws Exception {
        String output = runCommand("adb", "-s", deviceUDID, "shell",
                "pidof", packageName, "||", "true");
        return output != null && !output.trim().isEmpty();
    }

    /**
     * Returns the package name of the currently focused (foreground) app, or "" if unknown.
     */
    public String getForegroundPackage() throws Exception {
        String output = runCommand("adb", "-s", deviceUDID, "shell", "dumpsys", "window", "windows");

        for (String line : output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("mCurrentFocus=") || trimmed.startsWith("mFocusedApp=")) {
                int u0Idx = trimmed.indexOf(" u0 ");
                if (u0Idx == -1) continue;

                String after = trimmed.substring(u0Idx + 4).trim();
                int slash = after.indexOf('/');
                if (slash > 0) {
                    String pkg = after.substring(0, slash);
                    pkg = pkg.replace("}", "").trim();
                    return pkg;
                }
            }
        }
        return "";
    }

    /**
     * Polls isAppRunning() until the package is running or the timeout expires.
     */
    public boolean waitForAppRunning(String packageName, int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (isAppRunning(packageName)) {
                return true;
            }
            Thread.sleep(500);
        }
        return false;
    }

    // ==================== FOREGROUND LAUNCH (Android TV) ====================

    /**
     * Brings the app to the foreground using monkey + LEANBACK_LAUNCHER category.
     * Works on Android TV apps (which use LEANBACK_LAUNCHER, not LAUNCHER).
     * Does not require knowing the main activity name.
     */
    public void bringAppToForeground(String packageName) throws Exception {
        runCommand("adb", "-s", deviceUDID, "shell", "monkey",
                "-p", packageName,
                "-c", "android.intent.category.LEANBACK_LAUNCHER",
                "1");
        System.out.println("🚀 Brought to foreground: " + packageName);
        Thread.sleep(1500);
    }

    /**
     * Polls until the given package is the foreground app, or timeout.
     */
    public boolean waitForAppForeground(String packageName, int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String fg = getForegroundPackage();
            if (fg != null && fg.equals(packageName)) {
                return true;
            }
            Thread.sleep(500);
        }
        return false;
    }
}