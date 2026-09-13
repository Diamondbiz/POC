package AndroidTV.V3.Tests.User.PhoneFieldValidation;

import AndroidTV.V3.config.ADBRouterConfig;
import AndroidTV.V3.Models.Router;
import AndroidTV.V3.utils.TestLogger;
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

public class ADBValidPhoneNumber {

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

    // Output folders
    private final String expectedScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Regular user/Full login screen/Valid Phone Number.png";
    private final String currentScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Current screen";
    private final String passFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Pass";
    private final String failFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Fail";
    private final String xmlFolderPath = "/Users/Johnny/IdeaProjects/POC/xml";

    public static void main(String[] args) {
        new ADBValidPhoneNumber().runTest();
    }

    public void runTest() {
        testStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        TestLogger.init("ADBValidPhoneNumber");

        try {
            TestLogger.log("═══════════════════════════════════════════════════");
            TestLogger.log("📱 VALID PHONE NUMBER TEST - PURE ADB");
            TestLogger.log("═══════════════════════════════════════════════════");

            TestLogger.logStep("1", "Connecting to device via ADB");
            connectDevice();

            TestLogger.logStep("2", "Printing current SSID and validating against router list");
            printAndValidateSSID();

            TestLogger.logStep("3", "Pressing HOME and opening HOT app");
            pressHome();
            launchApp();

            TestLogger.logStep("4", "Waiting for Login screen to load (immediate polling)");
            waitForAppToLoad();

            TestLogger.logStep("5", "Entering valid phone number");
            String expectedPhoneNumber = "0543501323";
            enterPhoneNumber(expectedPhoneNumber);

            TestLogger.logStep("6", "Taking screenshot and comparing to expected 'Valid Phone Number' screen");
            File currentScreenshot = takeScreenshotFile();
            validateValidPhoneNumberScreen(currentScreenshot);

            TestLogger.logStep("7", "Verifying 'התחבר' button is selected (white highlighted) via CROP");
            verifyConnectButtonSelected(currentScreenshot);

            TestLogger.logStep("8", "Pressing 'התחבר' button");
            pressConnectButton();

            TestLogger.logStep("9", "Validating OTP screen elements and printing them");
            validateOTPScreen();

            TestLogger.logSuccess("✅ VALID PHONE NUMBER TEST PASSED! OTP screen loaded successfully!");
            printResultsSummary(true);

        } catch (Exception e) {
            TestLogger.logError("❌ Test failed: " + e.getMessage());
            takeScreenshot("test_failure");
            savePageSource("test_failure");
            printResultsSummary(false);
            System.exit(1);
        } finally {
            TestLogger.close();
        }
    }

    // ==================== SSID VALIDATION ====================

    private void printAndValidateSSID() throws Exception {
        String ssid = getCurrentSSID();
        TestLogger.log("   📶 Current SSID: " + ssid);

        Router router = ADBRouterConfig.getRouterBySSID(ssid);
        if (router != null) {
            TestLogger.log("   ✅ Router found in config: " + router.getSsid());
            TestLogger.log("   ✅ Valid phone number for this router: " + router.getValidPhoneNumber());
            if (router.getValidPhoneNumber().equals("0543501323")) {
                TestLogger.logSuccess("✅ SSID matched. Valid phone number confirmed for this router");
            } else {
                TestLogger.logWarning("⚠️ Router found but valid phone number differs");
            }
        } else {
            TestLogger.logWarning("⚠️ Router SSID not found in config (case mismatch may exist): " + ssid);
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
        TestLogger.log("   🔌 Connecting to device: " + deviceUDID);
        runCommand("adb", "connect", deviceUDID);
        TestLogger.logSuccess("✅ Device connected");
    }

    private void pressHome() throws Exception {
        TestLogger.log("   📱 Pressing HOME");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "3");
        Thread.sleep(1000);
    }

    private void launchApp() throws Exception {
        TestLogger.log("   🚀 Clicking HOT icon (DPAD_CENTER)");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "23");
        TestLogger.logSuccess("✅ HOT icon clicked. Starting immediate polling...");
    }

    private void pressDpad(int keyCode) throws Exception {
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", String.valueOf(keyCode));
        Thread.sleep(100);
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

        String localPath = currentScreenPath + "/ValidPhoneNumberADB_Current_" + testStartTime + ".png";
        runCommand("adb", "-s", deviceUDID, "pull", remotePath, localPath);

        TestLogger.log("📸 Current screenshot saved to: " + new File(localPath).getAbsolutePath());
        TestLogger.log("   🔗 file://" + new File(localPath).getAbsolutePath());

        return new File(localPath);
    }

    private void takeScreenshot(String name) {
        try {
            String remotePath = "/sdcard/screen_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);

            String localPath = failFolderPath + "/" + name + "_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "pull", remotePath, localPath);

            TestLogger.log("📸 Screenshot saved to: " + new File(localPath).getAbsolutePath());
            TestLogger.log("   🔗 file://" + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            TestLogger.logWarning("⚠️ Could not take screenshot: " + e.getMessage());
        }
    }

    private void savePageSource(String name) {
        try {
            String remoteDumpPath = "/sdcard/dump_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "shell", "uiautomator", "dump", remoteDumpPath);
            String localPath = "xml/" + name + "_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localPath);
            TestLogger.log("📄 Page Source: " + new File(localPath).getAbsolutePath());
            TestLogger.log("   🔗 file://" + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            TestLogger.logWarning("⚠️ Could not save page source: " + e.getMessage());
        }
    }

    // ==================== STEP 3: Wait for Login Screen (IMMEDIATE POLLING) ====================

    private void waitForAppToLoad() throws Exception {
        TestLogger.log("⏳ Starting immediate polling for Login screen...");

        long startTime = System.currentTimeMillis();
        long timeout = 30000;

        while (System.currentTimeMillis() - startTime < timeout) {
            String pageSource = getScreenXml();
            if (pageSource.contains("txtUserCellPhone") &&
                    pageSource.contains("dvbtnConnect") &&
                    pageSource.contains("mod_keyboard")) {

                TestLogger.logSuccess("✅ Login screen loaded successfully in " + (System.currentTimeMillis() - startTime) + " ms");

                // Print all found login elements
                printLoginScreenElements(pageSource);

                return;
            }
            Thread.sleep(1000);
        }

        throw new RuntimeException("App failed to load within " + timeout + " seconds");
    }

    // Print Login Screen Elements
    private void printLoginScreenElements(String xml) {
        TestLogger.log("");
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("📋 PHONE SCREEN - FOUND ELEMENTS");
        TestLogger.log("═══════════════════════════════════════════════════");

        if (xml.contains("txtUserCellPhone")) {
            TestLogger.logSuccess("   ✅ Phone number field: txtUserCellPhone");
        } else {
            TestLogger.logError("   ❌ Phone number field: txtUserCellPhone");
        }

        if (xml.contains("dvbtnConnect")) {
            TestLogger.logSuccess("   ✅ Connect button: dvbtnConnect");
        } else {
            TestLogger.logError("   ❌ Connect button: dvbtnConnect");
        }

        if (xml.contains("mod_keyboard")) {
            TestLogger.logSuccess("   ✅ Numeric keypad: mod_keyboard");
        } else {
            TestLogger.logError("   ❌ Numeric keypad: mod_keyboard");
        }

        if (xml.contains("מספר נייד (הרשום במנוי)")) {
            TestLogger.logSuccess("   ✅ Phone field label: מספר נייד (הרשום במנוי)");
        } else {
            TestLogger.logError("   ❌ Phone field label: מספר נייד (הרשום במנוי)");
        }

        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("");
    }

    // ==================== STEP 4: Enter Phone Number ====================

    private void enterPhoneNumber(String phoneNumber) throws Exception {
        TestLogger.log("📱 Entering phone number: " + phoneNumber);
        TestLogger.log("   Using DPAD navigation on keypad");

        int currentRow = 1;
        int currentCol = 1;

        for (char digitChar : phoneNumber.toCharArray()) {
            String digit = String.valueOf(digitChar);
            int[] targetPos = KEYPAD_POSITIONS.get(digit);
            if (targetPos == null) {
                TestLogger.logWarning("⚠️ Unknown digit: " + digit + ", skipping...");
                continue;
            }

            int targetRow = targetPos[0];
            int targetCol = targetPos[1];

            while (currentRow > targetRow) {
                pressDpad(19); // UP
                currentRow--;
            }
            while (currentRow < targetRow) {
                pressDpad(20); // DOWN
                currentRow++;
            }

            while (currentCol > targetCol) {
                pressDpad(21); // LEFT
                currentCol--;
            }
            while (currentCol < targetCol) {
                pressDpad(22); // RIGHT
                currentCol++;
            }

            pressDpad(23); // CENTER
            Thread.sleep(200);
        }

        TestLogger.logSuccess("✅ Phone number entered successfully: " + phoneNumber);
    }

    // ==================== STEP 5: Validate "Valid Phone Number" Screen ====================

    private void validateValidPhoneNumberScreen(File currentScreenshot) throws IOException {
        File expectedFile = new File(expectedScreenPath);
        if (!expectedFile.exists()) {
            throw new RuntimeException("Expected screen not found: " + expectedScreenPath);
        }

        BufferedImage expectedImage = ImageIO.read(expectedFile);
        BufferedImage currentImage = ImageIO.read(currentScreenshot);

        if (currentImage.getWidth() != expectedImage.getWidth() ||
                currentImage.getHeight() != expectedImage.getHeight()) {
            saveToFailFolder(currentScreenshot);
            throw new RuntimeException("Screen dimensions mismatch!");
        }

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
        TestLogger.log("📊 Image Comparison Result:");
        TestLogger.log("   ├─ Total Pixels: " + totalPixels);
        TestLogger.log("   ├─ Mismatched Pixels: " + mismatchedPixels);
        TestLogger.log("   └─ Similarity: " + String.format("%.2f", similarity) + "%");

        if (similarity >= 95.0) {
            TestLogger.logSuccess("✅ Current screen matches expected 'Valid Phone Number' screen!");
            saveToPassFolder(currentScreenshot);
        } else {
            TestLogger.logError("❌ Current screen DOES NOT match expected 'Valid Phone Number' screen!");
            saveToFailFolder(currentScreenshot);
            throw new RuntimeException("Valid Phone Number screen mismatch! Similarity: " + similarity + "%");
        }
    }

    // ==================== STEP 6: Verify "התחבר" Button Selected ====================

    private void verifyConnectButtonSelected(File currentScreenshot) throws IOException {
        TestLogger.log("🔍 Verifying 'התחבר' button is selected (white highlighted)...");

        BufferedImage fullImage = ImageIO.read(currentScreenshot);

        int cropX = 816;
        int cropY = 544;
        int cropWidth = 1068 - 816;
        int cropHeight = 610 - 544;

        BufferedImage croppedButton = fullImage.getSubimage(cropX, cropY, cropWidth, cropHeight);

        int whiteCount = 0;
        int totalPixels = cropWidth * cropHeight;

        for (int x = 0; x < cropWidth; x++) {
            for (int y = 0; y < cropHeight; y++) {
                int rgb = croppedButton.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                if (red > 200 && green > 200 && blue > 200) {
                    whiteCount++;
                }
            }
        }

        double whitePercentage = (double) whiteCount / totalPixels * 100;
        TestLogger.log("   📊 Cropped button area: " + cropWidth + "x" + cropHeight);
        TestLogger.log("   📊 Total pixels: " + totalPixels + ", White pixels: " + whiteCount);
        TestLogger.log("   📊 White percentage: " + String.format("%.2f", whitePercentage) + "%");

        if (whitePercentage >= 30.0) {
            TestLogger.logSuccess("✅ 'התחבר' button is selected (white highlighted)!");
        } else {
            TestLogger.logError("❌ 'התחבר' button is NOT selected!");
            throw new RuntimeException("'התחבר' button is not selected!");
        }
    }

    // ==================== STEP 7: Press "התחבר" Button ====================

    private void pressConnectButton() throws Exception {
        TestLogger.log("🔘 Pressing 'התחבר' button...");
        for (int i = 0; i < 5; i++) {
            pressDpad(20); // DOWN
        }
        pressDpad(23); // CENTER
        TestLogger.logSuccess("✅ 'התחבר' button pressed");
        Thread.sleep(3000);
    }

    // ==================== STEP 8: OTP Screen Validation ====================

    private void validateOTPScreen() throws Exception {
        TestLogger.log("⏳ Waiting for OTP screen to load...");

        long startTime = System.currentTimeMillis();
        long timeout = 30000;

        while (System.currentTimeMillis() - startTime < timeout) {
            String pageSource = getScreenXml();

            if (pageSource.contains("txtToken") &&
                    pageSource.contains("dvbtnConnectToken") &&
                    pageSource.contains("mod_PopupLogin_ResendTokenLink")) {

                TestLogger.logSuccess("✅ OTP screen loaded successfully!");

                // Print all found OTP elements
                printOTPScreenElements(pageSource);

                return;
            }

            Thread.sleep(1000);
        }

        throw new RuntimeException("OTP screen failed to load within " + timeout + " seconds");
    }

    // Print OTP Screen Elements
    private void printOTPScreenElements(String xml) {
        TestLogger.log("");
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("📋 OTP SCREEN - FOUND ELEMENTS");
        TestLogger.log("═══════════════════════════════════════════════════");

        if (xml.contains("txtToken")) {
            TestLogger.logSuccess("   ✅ OTP input field: txtToken");
        } else {
            TestLogger.logError("   ❌ OTP input field: txtToken");
        }

        if (xml.contains("dvbtnConnectToken")) {
            TestLogger.logSuccess("   ✅ Verify button: dvbtnConnectToken");
        } else {
            TestLogger.logError("   ❌ Verify button: dvbtnConnectToken");
        }

        if (xml.contains("mod_PopupLogin_ResendTokenLink")) {
            TestLogger.logSuccess("   ✅ Resend link: mod_PopupLogin_ResendTokenLink");
        } else {
            TestLogger.logError("   ❌ Resend link: mod_PopupLogin_ResendTokenLink");
        }

        if (xml.contains("הזן קוד אימות שנשלח לנייד")) {
            TestLogger.logSuccess("   ✅ OTP label: הזן קוד אימות שנשלח לנייד");
        } else {
            TestLogger.logError("   ❌ OTP label: הזן קוד אימות שנשלח לנייד");
        }

        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("");
    }

    // ==================== SAVE METHODS ====================

    private void saveToPassFolder(File screenshot) {
        try {
            File dir = new File(passFolderPath);
            if (!dir.exists()) dir.mkdirs();
            String fileName = "ValidPhoneNumberADB_PASS_" + testStartTime + ".png";
            FileUtils.copyFile(screenshot, new File(dir, fileName));
            TestLogger.log("📸 PASS screenshot saved to: " + new File(dir, fileName).getAbsolutePath());
            TestLogger.log("   🔗 file://" + new File(dir, fileName).getAbsolutePath());
        } catch (IOException e) {
            TestLogger.logError("❌ Could not save pass screenshot: " + e.getMessage());
        }
    }

    private void saveToFailFolder(File screenshot) {
        try {
            File dir = new File(failFolderPath);
            if (!dir.exists()) dir.mkdirs();
            String fileName = "test_failure_" + testStartTime + ".png";
            FileUtils.copyFile(screenshot, new File(dir, fileName));
            TestLogger.log("📸 FAIL screenshot saved to: " + new File(dir, fileName).getAbsolutePath());
            TestLogger.log("   🔗 file://" + new File(dir, fileName).getAbsolutePath());
        } catch (IOException e) {
            TestLogger.logError("❌ Could not save fail screenshot: " + e.getMessage());
        }
    }

    // ==================== REPORTING ====================

    private void printResultsSummary(boolean passed) {
        TestLogger.log("");
        TestLogger.log("═══ TEST RESULTS SUMMARY ═══");
        TestLogger.log("📌 Status: " + (passed ? "✅ PASSED" : "❌ FAILED"));
        TestLogger.log("🕐 Timestamp: " + testStartTime);
    }
}