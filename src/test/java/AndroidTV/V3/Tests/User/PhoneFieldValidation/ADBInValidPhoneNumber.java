package AndroidTV.V3.Tests.User.PhoneFieldValidation;

import AndroidTV.V3.config.ADBRouterConfig;
import AndroidTV.V3.Models.Router;
import AndroidTV.V3.utils.ADBTestLogger;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ADBInValidPhoneNumber {

    private String testStartTime;
    private final String deviceUDID = "192.168.1.10:5555";

    // Keypad layout: [1][2][3] / [4][5][6] / [7][8][9] / [ ][0][Next]
    private static final Map<String, int[]> KEYPAD_POSITIONS = new HashMap<>();
    static {
        KEYPAD_POSITIONS.put("1", new int[]{0, 0});
        KEYPAD_POSITIONS.put("2", new int[]{0, 1});
        KEYPAD_POSITIONS.put("3", new int[]{0, 2});
        KEYPAD_POSITIONS.put("4", new int[]{1, 0});
        KEYPAD_POSITIONS.put("5", new int[]{1, 1});
        KEYPAD_POSITIONS.put("6", new int[]{1, 2});
        KEYPAD_POSITIONS.put("7", new int[]{2, 0});
        KEYPAD_POSITIONS.put("8", new int[]{2, 1});
        KEYPAD_POSITIONS.put("9", new int[]{2, 2});
        KEYPAD_POSITIONS.put("0", new int[]{3, 1});
    }

    // Folder paths
    private final String currentScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Current screen";
    private final String passFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Pass";
    private final String failFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Fail";
    private final String xmlFolderPath = "/Users/Johnny/IdeaProjects/POC/xml";

    // Expected Error Screen (Invalid Phone Number with 710)
    private final String expectedErrorScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Regular user/Error messages/Invalid Phone number.png";

    public static void main(String[] args) {
        new ADBInValidPhoneNumber().runTest();
    }

    public void runTest() {
        testStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ADBTestLogger.init("ADBInValidPhoneNumber");

        try {
            ADBTestLogger.log("═══════════════════════════════════════════════════");
            ADBTestLogger.log("🚫 INVALID PHONE NUMBER TEST - PURE ADB");
            ADBTestLogger.log("═══════════════════════════════════════════════════");

            ADBTestLogger.logStep("1", "Connecting to device via ADB");
            connectDevice();

            ADBTestLogger.logStep("2", "Printing current SSID and validating against router list");
            printAndValidateSSID();

            ADBTestLogger.logStep("3", "Pressing HOME and opening HOT app");
            pressHome();
            launchApp();

            ADBTestLogger.logStep("4", "Waiting for Login screen to load");
            waitForAppToLoad();

            ADBTestLogger.logStep("5", "Entering INVALID phone number: 0543501322");
            String invalidPhoneNumber = "0543501322";
            enterPhoneNumber(invalidPhoneNumber);

            ADBTestLogger.logStep("6", "Logging phone number in field (NO FAILURE EXPECTED)");
            String actualPhone = getPhoneNumberFromField();
            ADBTestLogger.log("   📋 Phone number in field: '" + actualPhone + "'");
            // Do NOT fail here! It's okay if it doesn't match the valid phone number.
            ADBTestLogger.log("   ℹ️ (This is expected to be different from valid number 0543501323)");

            ADBTestLogger.logStep("7", "Pressing 'התחבר' (OK) - button is already focused");
            pressOkButton();
            Thread.sleep(3000);

            ADBTestLogger.logStep("8", "Waiting for INVALID ERROR SCREEN (710)");
            waitForErrorScreen();

            ADBTestLogger.logStep("9", "Printing all elements found on error screen");
            printErrorScreenElements();

            ADBTestLogger.logStep("10", "Taking screenshot and comparing to expected Error screen");
            File currentScreenshot = takeScreenshotFile();
            validateErrorScreen(currentScreenshot);

            ADBTestLogger.logStep("11", "Checking for OTP screen (should NOT appear)");
            if (isOTPScreenPresent()) {
                ADBTestLogger.logError("❌ USER ABUSE POLICY FAILED: OTP screen appeared with invalid phone number!");
                throw new RuntimeException("Test failed user abuse policy: OTP screen appeared with invalid number!");
            } else {
                ADBTestLogger.logSuccess("✅ No OTP screen appeared (correct behavior for invalid number)");
            }

            ADBTestLogger.logSuccess("✅ INVALID PHONE NUMBER TEST PASSED!");
            printResultsSummary(true);

        } catch (Exception e) {
            ADBTestLogger.logError("❌ Test failed: " + e.getMessage());
            takeScreenshot("test_failure");
            savePageSource("test_failure");
            printResultsSummary(false);
            System.exit(1);
        } finally {
            ADBTestLogger.close();
        }
    }

    // ==================== SSID VALIDATION ====================

    private void printAndValidateSSID() throws Exception {
        String ssid = getCurrentSSID();
        ADBTestLogger.log("   📶 Current SSID: " + ssid);

        Router router = ADBRouterConfig.getRouterBySSID(ssid);
        if (router != null) {
            ADBTestLogger.log("   ✅ Router found in config: " + router.getSsid());
            ADBTestLogger.log("   ✅ Valid phone number for this router: " + router.getValidPhoneNumber());
            if (router.getValidPhoneNumber().equals("0543501323")) {
                ADBTestLogger.logSuccess("✅ SSID matched. Router is QA BLUE-SKY 5Mhz");
            } else {
                ADBTestLogger.logWarning("⚠️ Router found but valid phone number differs");
            }
        } else {
            ADBTestLogger.logWarning("⚠️ Router SSID not found in config (case mismatch may exist): " + ssid);
        }
    }

    private String getCurrentSSID() throws Exception {
        String output = runCommand("adb", "-s", deviceUDID, "shell", "dumpsys", "wifi", "|", "grep", "mWifiInfo");
        Pattern pattern = Pattern.compile("SSID: \"([^\"]+)\"");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "Unknown";
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
        ADBTestLogger.log("   🔌 Connecting to device: " + deviceUDID);
        runCommand("adb", "connect", deviceUDID);
        ADBTestLogger.logSuccess("✅ Device connected");
    }

    private void pressHome() throws Exception {
        ADBTestLogger.log("   📱 Pressing HOME");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "3");
        Thread.sleep(1000);
    }

    private void launchApp() throws Exception {
        ADBTestLogger.log("   🚀 Clicking HOT icon (DPAD_CENTER)");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "23");
        ADBTestLogger.logSuccess("✅ HOT app launched");
        Thread.sleep(8000);
    }

    private void pressDpad(int keyCode) throws Exception {
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", String.valueOf(keyCode));
        Thread.sleep(100);
    }

    private void pressOkButton() throws Exception {
        ADBTestLogger.log("   ✅ 'התחבר' button is already focused. Pressing OK (DPAD_CENTER)...");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "23");
        ADBTestLogger.logSuccess("✅ OK pressed");
    }

    private String getScreenXml() throws Exception {
        String remoteDumpPath = "/sdcard/dump_" + testStartTime + ".xml";
        runCommand("adb", "-s", deviceUDID, "shell", "uiautomator", "dump", remoteDumpPath);
        String localDumpPath = xmlFolderPath + "/dump_" + testStartTime + ".xml";
        runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localDumpPath);
        return new String(Files.readAllBytes(Paths.get(localDumpPath)));
    }

    private File takeScreenshotFile() throws Exception {
        String remotePath = "/sdcard/screen_" + testStartTime + ".png";
        runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);
        String localPath = currentScreenPath + "/ADBInValidPhoneNumber_Current_" + testStartTime + ".png";
        runCommand("adb", "-s", deviceUDID, "pull", remotePath, localPath);
        return new File(localPath);
    }

    private void takeScreenshot(String name) {
        try {
            String remotePath = "/sdcard/screen_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);
            String localPath = failFolderPath + "/" + name + "_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "pull", remotePath, localPath);
            ADBTestLogger.log("📸 Screenshot saved to: " + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            ADBTestLogger.logWarning("⚠️ Could not take screenshot: " + e.getMessage());
        }
    }

    private void savePageSource(String name) {
        try {
            String remoteDumpPath = "/sdcard/dump_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "shell", "uiautomator", "dump", remoteDumpPath);
            String localPath = "xml/" + name + "_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localPath);
            ADBTestLogger.log("📄 Page Source: " + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            ADBTestLogger.logWarning("⚠️ Could not save page source: " + e.getMessage());
        }
    }

    // ==================== UI INTERACTION ====================

    private void waitForAppToLoad() throws Exception {
        ADBTestLogger.log("⏳ Waiting for Login screen to load...");
        long startTime = System.currentTimeMillis();
        long timeout = 60000;
        while (System.currentTimeMillis() - startTime < timeout) {
            String pageSource = getScreenXml();
            if (pageSource.contains("txtUserCellPhone") && pageSource.contains("dvbtnConnect") && pageSource.contains("mod_keyboard")) {
                ADBTestLogger.logSuccess("✅ Login screen loaded");
                return;
            }
            Thread.sleep(1000);
        }
        throw new RuntimeException("Login screen failed to load");
    }

    private void enterPhoneNumber(String phoneNumber) throws Exception {
        ADBTestLogger.log("📱 Entering phone number: " + phoneNumber);
        int currentRow = 1;
        int currentCol = 1;

        for (char digitChar : phoneNumber.toCharArray()) {
            String digit = String.valueOf(digitChar);
            int[] targetPos = KEYPAD_POSITIONS.get(digit);
            if (targetPos == null) continue;

            int targetRow = targetPos[0];
            int targetCol = targetPos[1];

            while (currentRow > targetRow) { pressDpad(19); currentRow--; }
            while (currentRow < targetRow) { pressDpad(20); currentRow++; }
            while (currentCol > targetCol) { pressDpad(21); currentCol--; }
            while (currentCol < targetCol) { pressDpad(22); currentCol++; }

            pressDpad(23);
            Thread.sleep(200);
        }
        ADBTestLogger.logSuccess("✅ Phone number entered: " + phoneNumber);
    }

    private String getPhoneNumberFromField() throws Exception {
        String xml = getScreenXml();
        Pattern pattern = Pattern.compile("resource-id=\"txtUserCellPhone\"[^>]*text=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).replace("|", "").trim();
        }
        return "";
    }

    private void waitForErrorScreen() throws Exception {
        ADBTestLogger.log("⏳ Waiting for error screen (710)...");
        long startTime = System.currentTimeMillis();
        long timeout = 30000;

        while (System.currentTimeMillis() - startTime < timeout) {
            String pageSource = getScreenXml();
            if (pageSource.contains("מספר הנייד שהוזן אינו תואם את הפרטים הקיימים במערכת") ||
                    pageSource.contains("710")) {
                ADBTestLogger.logSuccess("✅ Error screen detected with 710 error!");
                return;
            }
            Thread.sleep(1000);
        }
        throw new RuntimeException("Error screen (710) did not appear");
    }

    private void printErrorScreenElements() throws Exception {
        String xml = getScreenXml();
        ADBTestLogger.log("");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("📋 ERROR SCREEN - ELEMENTS FOUND");
        ADBTestLogger.log("═══════════════════════════════════════════════════");

        if (xml.contains("mod_PopupLogin_Error")) {
            ADBTestLogger.logSuccess("   ✅ Error element found: mod_PopupLogin_Error");
        } else {
            ADBTestLogger.logError("   ❌ Error element found: mod_PopupLogin_Error");
        }

        if (xml.contains("מספר הנייד שהוזן אינו תואם את הפרטים הקיימים במערכת")) {
            ADBTestLogger.logSuccess("   ✅ Error text found: מספר הנייד שהוזן אינו תואם את הפרטים הקיימים במערכת");
        } else {
            ADBTestLogger.logError("   ❌ Error text found: מספר הנייד שהוזן אינו תואם את הפרטים הקיימים במערכת");
        }

        if (xml.contains("710")) {
            ADBTestLogger.logSuccess("   ✅ Error code found: 710");
        } else {
            ADBTestLogger.logError("   ❌ Error code found: 710");
        }

        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("");
    }

    private boolean isOTPScreenPresent() throws Exception {
        String xml = getScreenXml();
        return xml.contains("txtToken") && xml.contains("dvbtnConnectToken");
    }

    private void validateErrorScreen(File currentScreenshot) throws Exception {
        File expectedFile = new File(expectedErrorScreenPath);
        if (!expectedFile.exists()) {
            ADBTestLogger.logWarning("⚠️ Expected error screenshot not found at: " + expectedErrorScreenPath);
            return;
        }

        BufferedImage expectedImage = ImageIO.read(expectedFile);
        BufferedImage currentImage = ImageIO.read(currentScreenshot);

        long mismatchedPixels = 0;
        long totalPixels = (long) currentImage.getWidth() * currentImage.getHeight();
        int tolerance = 10;

        for (int x = 0; x < currentImage.getWidth(); x++) {
            for (int y = 0; y < currentImage.getHeight(); y++) {
                int currentRGB = currentImage.getRGB(x, y);
                int expectedRGB = expectedImage.getRGB(x, y);
                int redDiff = Math.abs(((currentRGB >> 16) & 0xFF) - ((expectedRGB >> 16) & 0xFF));
                int greenDiff = Math.abs(((currentRGB >> 8) & 0xFF) - ((expectedRGB >> 8) & 0xFF));
                int blueDiff = Math.abs((currentRGB & 0xFF) - (expectedRGB & 0xFF));
                if (redDiff > tolerance || greenDiff > tolerance || blueDiff > tolerance) {
                    mismatchedPixels++;
                }
            }
        }

        double similarity = 100.0 - ((double) mismatchedPixels / totalPixels * 100);
        ADBTestLogger.log("📊 Error Screen Comparison: Similarity = " + String.format("%.2f", similarity) + "%");

        if (similarity >= 95.0) {
            ADBTestLogger.logSuccess("✅ Error screen matches expected (710)");
            saveToPassFolder(currentScreenshot);
        } else {
            ADBTestLogger.logError("❌ Error screen does NOT match expected!");
            saveToFailFolder(currentScreenshot);
            throw new RuntimeException("Error screen mismatch! Similarity: " + similarity + "%");
        }
    }

    // ==================== SAVE METHODS ====================

    private void saveToPassFolder(File screenshot) {
        try {
            File dir = new File(passFolderPath);
            if (!dir.exists()) dir.mkdirs();
            String fileName = "ADBInValidPhoneNumber_PASS_" + testStartTime + ".png";
            FileUtils.copyFile(screenshot, new File(dir, fileName));
            ADBTestLogger.log("📸 PASS screenshot saved to: " + new File(dir, fileName).getAbsolutePath());
        } catch (IOException e) {
            ADBTestLogger.logError("❌ Could not save pass screenshot: " + e.getMessage());
        }
    }

    private void saveToFailFolder(File screenshot) {
        try {
            File dir = new File(failFolderPath);
            if (!dir.exists()) dir.mkdirs();
            String fileName = "test_failure_" + testStartTime + ".png";
            FileUtils.copyFile(screenshot, new File(dir, fileName));
            ADBTestLogger.log("📸 FAIL screenshot saved to: " + new File(dir, fileName).getAbsolutePath());
        } catch (IOException e) {
            ADBTestLogger.logError("❌ Could not save fail screenshot: " + e.getMessage());
        }
    }

    private void printResultsSummary(boolean passed) {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══ TEST RESULTS SUMMARY ═══");
        ADBTestLogger.log("📌 Status: " + (passed ? "✅ PASSED" : "❌ FAILED"));
        ADBTestLogger.log("🕐 Timestamp: " + testStartTime);
    }
}