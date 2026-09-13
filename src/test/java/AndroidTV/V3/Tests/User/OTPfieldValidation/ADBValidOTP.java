package AndroidTV.V3.Tests.User.OTPfieldValidation;

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

public class ADBValidOTP {

    private String testStartTime;
    private final String deviceUDID = "192.168.1.10:5555";

    // Keypad layout for phone number
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

    // OTP Keypad layout (positions relative to keypad starting at [0,0])
    private static final Map<String, int[]> OTP_KEYPAD_POSITIONS = new HashMap<>();
    static {
        OTP_KEYPAD_POSITIONS.put("1", new int[]{0, 0});
        OTP_KEYPAD_POSITIONS.put("2", new int[]{0, 1});
        OTP_KEYPAD_POSITIONS.put("3", new int[]{0, 2});
        OTP_KEYPAD_POSITIONS.put("4", new int[]{1, 0});
        OTP_KEYPAD_POSITIONS.put("5", new int[]{1, 1});
        OTP_KEYPAD_POSITIONS.put("6", new int[]{1, 2});
        OTP_KEYPAD_POSITIONS.put("7", new int[]{2, 0});
        OTP_KEYPAD_POSITIONS.put("8", new int[]{2, 1});
        OTP_KEYPAD_POSITIONS.put("9", new int[]{2, 2});
        OTP_KEYPAD_POSITIONS.put("0", new int[]{3, 1});
    }

    // Output folders
    private final String expectedScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Regular user/Full login screen/Valid Phone Number.png";
    private final String currentScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Current screen";
    private final String passFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Pass";
    private final String failFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Fail";
    private final String xmlFolderPath = "/Users/Johnny/IdeaProjects/POC/xml";

    public static void main(String[] args) {
        new ADBValidOTP().runTest();
    }

    public void runTest() {
        testStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ADBTestLogger.init("ADBValidOTP");

        try {
            ADBTestLogger.log("═══════════════════════════════════════════════════");
            ADBTestLogger.log("📱 VALID OTP TEST - PURE ADB");
            ADBTestLogger.log("═══════════════════════════════════════════════════");

            ADBTestLogger.logStep("1", "Connecting to device via ADB");
            connectDevice();

            ADBTestLogger.logStep("2", "Printing current SSID and validating against router list");
            printAndValidateSSID();

            ADBTestLogger.logStep("3", "Pressing HOME and opening HOT app");
            pressHome();
            launchApp();

            ADBTestLogger.logStep("4", "Waiting for Login screen to load (immediate polling)");
            waitForAppToLoad();

            ADBTestLogger.logStep("5", "Entering valid phone number");
            String expectedPhoneNumber = "0543501323";
            enterPhoneNumber(expectedPhoneNumber);

            ADBTestLogger.logStep("6", "Taking screenshot and comparing to expected 'Valid Phone Number' screen");
            File currentScreenshot = takeScreenshotFile();
            validateValidPhoneNumberScreen(currentScreenshot);

            ADBTestLogger.logStep("7", "Verifying 'התחבר' button is selected (white highlighted) via CROP");
            verifyConnectButtonSelected(currentScreenshot);

            ADBTestLogger.logStep("8", "Pressing 'התחבר' button");
            pressConnectButton();

            ADBTestLogger.logStep("9", "Validating OTP screen elements");
            validateOTPScreen();

            ADBTestLogger.logStep("10", "Entering valid OTP: 123456");
            enterOTP("123456");

            ADBTestLogger.logStep("11", "Checking if 'התחבר' button is highlighted on OTP screen");
            checkOTPConnectButtonHighlighted();

            ADBTestLogger.logStep("12", "Pressing 'התחבר' button on OTP screen");
            pressOTPConnectButton();

            ADBTestLogger.logStep("13", "Waiting for Live Mosaic screen to load");
            validateLiveMosaicScreen();

            ADBTestLogger.logSuccess("✅ VALID OTP TEST PASSED! Live Mosaic screen loaded successfully!");
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
                ADBTestLogger.logSuccess("✅ SSID matched. Valid phone number confirmed for this router");
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
        ADBTestLogger.logSuccess("✅ HOT icon clicked. Starting immediate polling...");
    }

    private void pressDpad(int keyCode) throws Exception {
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", String.valueOf(keyCode));
        Thread.sleep(150);
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

        ADBTestLogger.log("📸 Current screenshot saved to: " + new File(localPath).getAbsolutePath());
        ADBTestLogger.log("   🔗 file://" + new File(localPath).getAbsolutePath());

        return new File(localPath);
    }

    private void takeScreenshot(String name) {
        try {
            String remotePath = "/sdcard/screen_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);

            String localPath = failFolderPath + "/" + name + "_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "pull", remotePath, localPath);

            ADBTestLogger.log("📸 Screenshot saved to: " + new File(localPath).getAbsolutePath());
            ADBTestLogger.log("   🔗 file://" + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            ADBTestLogger.logWarning("⚠️ Could not take screenshot: " + e.getMessage());
        }
    }

    private void savePageSource(String name) {
        try {
            String remoteDumpPath = "/sdcard/dump_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "shell", "uiautomator", "dump", remoteDumpPath);
            String localPath = failFolderPath + "/" + name + "_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localPath);
            ADBTestLogger.log("📄 Page Source: " + new File(localPath).getAbsolutePath());
            ADBTestLogger.log("   🔗 file://" + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            ADBTestLogger.logWarning("⚠️ Could not save page source: " + e.getMessage());
        }
    }

    // ==================== WAIT FOR LOGIN SCREEN ====================

    private void waitForAppToLoad() throws Exception {
        ADBTestLogger.log("⏳ Starting immediate polling for Login screen...");

        long startTime = System.currentTimeMillis();
        long timeout = 30000;

        while (System.currentTimeMillis() - startTime < timeout) {
            String pageSource = getScreenXml();
            if (pageSource.contains("txtUserCellPhone") &&
                    pageSource.contains("dvbtnConnect") &&
                    pageSource.contains("mod_keyboard")) {

                ADBTestLogger.logSuccess("✅ Login screen loaded successfully in " + (System.currentTimeMillis() - startTime) + " ms");
                printLoginScreenElements(pageSource);
                return;
            }
            Thread.sleep(1000);
        }

        throw new RuntimeException("App failed to load within " + timeout + " seconds");
    }

    private void printLoginScreenElements(String xml) {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("📋 PHONE SCREEN - FOUND ELEMENTS");
        ADBTestLogger.log("═══════════════════════════════════════════════════");

        if (xml.contains("txtUserCellPhone")) {
            ADBTestLogger.logSuccess("   ✅ Phone number field: txtUserCellPhone");
        } else {
            ADBTestLogger.logError("   ❌ Phone number field: txtUserCellPhone");
        }

        if (xml.contains("dvbtnConnect")) {
            ADBTestLogger.logSuccess("   ✅ Connect button: dvbtnConnect");
        } else {
            ADBTestLogger.logError("   ❌ Connect button: dvbtnConnect");
        }

        if (xml.contains("mod_keyboard")) {
            ADBTestLogger.logSuccess("   ✅ Numeric keypad: mod_keyboard");
        } else {
            ADBTestLogger.logError("   ❌ Numeric keypad: mod_keyboard");
        }

        if (xml.contains("מספר נייד (הרשום במנוי)")) {
            ADBTestLogger.logSuccess("   ✅ Phone field label: מספר נייד (הרשום במנוי)");
        } else {
            ADBTestLogger.logError("   ❌ Phone field label: מספר נייד (הרשום במנוי)");
        }

        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("");
    }

    // ==================== ENTER PHONE NUMBER ====================

    private void enterPhoneNumber(String phoneNumber) throws Exception {
        ADBTestLogger.log("📱 Entering phone number: " + phoneNumber);
        ADBTestLogger.log("   Using DPAD navigation on keypad");

        int currentRow = 1;
        int currentCol = 1;

        for (char digitChar : phoneNumber.toCharArray()) {
            String digit = String.valueOf(digitChar);
            int[] targetPos = KEYPAD_POSITIONS.get(digit);
            if (targetPos == null) {
                ADBTestLogger.logWarning("⚠️ Unknown digit: " + digit + ", skipping...");
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

        ADBTestLogger.logSuccess("✅ Phone number entered successfully: " + phoneNumber);
    }

    // ==================== VALIDATE PHONE NUMBER SCREEN ====================

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
        ADBTestLogger.log("📊 Image Comparison Result:");
        ADBTestLogger.log("   ├─ Total Pixels: " + totalPixels);
        ADBTestLogger.log("   ├─ Mismatched Pixels: " + mismatchedPixels);
        ADBTestLogger.log("   └─ Similarity: " + String.format("%.2f", similarity) + "%");

        if (similarity >= 95.0) {
            ADBTestLogger.logSuccess("✅ Current screen matches expected 'Valid Phone Number' screen!");
            saveToPassFolder(currentScreenshot);
        } else {
            ADBTestLogger.logError("❌ Current screen DOES NOT match expected 'Valid Phone Number' screen!");
            saveToFailFolder(currentScreenshot);
            throw new RuntimeException("Valid Phone Number screen mismatch! Similarity: " + similarity + "%");
        }
    }

    // ==================== VERIFY CONNECT BUTTON SELECTED ====================

    private void verifyConnectButtonSelected(File currentScreenshot) throws IOException {
        ADBTestLogger.log("🔍 Verifying 'התחבר' button is selected (white highlighted)...");

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
        ADBTestLogger.log("   📊 Cropped button area: " + cropWidth + "x" + cropHeight);
        ADBTestLogger.log("   📊 Total pixels: " + totalPixels + ", White pixels: " + whiteCount);
        ADBTestLogger.log("   📊 White percentage: " + String.format("%.2f", whitePercentage) + "%");

        if (whitePercentage >= 30.0) {
            ADBTestLogger.logSuccess("✅ 'התחבר' button is selected (white highlighted)!");
        } else {
            ADBTestLogger.logError("❌ 'התחבר' button is NOT selected!");
            throw new RuntimeException("'התחבר' button is not selected!");
        }
    }

    // ==================== PRESS CONNECT BUTTON ====================

    private void pressConnectButton() throws Exception {
        ADBTestLogger.log("🔘 Pressing 'התחבר' button...");
        for (int i = 0; i < 5; i++) {
            pressDpad(20); // DOWN
        }
        pressDpad(23); // CENTER
        ADBTestLogger.logSuccess("✅ 'התחבר' button pressed");
        Thread.sleep(3000);
    }

    // ==================== OTP SCREEN VALIDATION ====================

    private void validateOTPScreen() throws Exception {
        ADBTestLogger.log("⏳ Waiting for OTP screen to load...");

        long startTime = System.currentTimeMillis();
        long timeout = 30000;

        while (System.currentTimeMillis() - startTime < timeout) {
            String pageSource = getScreenXml();

            if (pageSource.contains("txtToken") &&
                    pageSource.contains("dvbtnConnectToken") &&
                    pageSource.contains("mod_PopupLogin_ResendTokenLink")) {

                ADBTestLogger.logSuccess("✅ OTP screen loaded successfully!");
                printOTPScreenElements(pageSource);
                return;
            }
            Thread.sleep(1000);
        }

        throw new RuntimeException("OTP screen failed to load within " + timeout + " seconds");
    }

    private void printOTPScreenElements(String xml) {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("📋 OTP SCREEN - FOUND ELEMENTS");
        ADBTestLogger.log("═══════════════════════════════════════════════════");

        if (xml.contains("txtToken")) {
            ADBTestLogger.logSuccess("   ✅ OTP input field: txtToken");
        } else {
            ADBTestLogger.logError("   ❌ OTP input field: txtToken");
        }

        if (xml.contains("dvbtnConnectToken")) {
            ADBTestLogger.logSuccess("   ✅ Verify button: dvbtnConnectToken");
        } else {
            ADBTestLogger.logError("   ❌ Verify button: dvbtnConnectToken");
        }

        if (xml.contains("mod_PopupLogin_ResendTokenLink")) {
            ADBTestLogger.logSuccess("   ✅ Resend link: mod_PopupLogin_ResendTokenLink");
        } else {
            ADBTestLogger.logError("   ❌ Resend link: mod_PopupLogin_ResendTokenLink");
        }

        if (xml.contains("הזן קוד אימות שנשלח לנייד")) {
            ADBTestLogger.logSuccess("   ✅ OTP label: הזן קוד אימות שנשלח לנייד");
        } else {
            ADBTestLogger.logError("   ❌ OTP label: הזן קוד אימות שנשלח לנייד");
        }

        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("");
    }

    // ==================== ENTER OTP (FIXED) ====================

    private void enterOTP(String otp) throws Exception {
        ADBTestLogger.log("📱 Entering OTP: " + otp);
        ADBTestLogger.log("   Using DPAD navigation on keypad");
        ADBTestLogger.log("   Starting from OTP text field, pressing DOWN to enter keypad at position [0,0]");

        // Step 1: Press DOWN once to move from OTP text field to the "1" key on the keypad
        // This is our starting position at [0,0]
        pressDpad(20); // DOWN
        ADBTestLogger.log("   ✅ Entered keypad at position [0,0] (digit 1)");

        // Track current position on the keypad
        int currentRow = 0;
        int currentCol = 0;

        // Enter each digit of the OTP
        for (char digitChar : otp.toCharArray()) {
            String digit = String.valueOf(digitChar);
            int[] targetPos = OTP_KEYPAD_POSITIONS.get(digit);

            if (targetPos == null) {
                ADBTestLogger.logWarning("⚠️ Unknown digit: " + digit + ", skipping...");
                continue;
            }

            int targetRow = targetPos[0];
            int targetCol = targetPos[1];

            ADBTestLogger.log("   📍 Navigating to digit " + digit + " at position [" + targetRow + "," + targetCol + "]");

            // Navigate to the target digit
            while (currentRow > targetRow) {
                pressDpad(19); // UP
                currentRow--;
                Thread.sleep(100);
            }
            while (currentRow < targetRow) {
                pressDpad(20); // DOWN
                currentRow++;
                Thread.sleep(100);
            }

            while (currentCol > targetCol) {
                pressDpad(21); // LEFT
                currentCol--;
                Thread.sleep(100);
            }
            while (currentCol < targetCol) {
                pressDpad(22); // RIGHT
                currentCol++;
                Thread.sleep(100);
            }

            // Press the digit
            pressDpad(23); // CENTER
            Thread.sleep(200);

            ADBTestLogger.log("   ✅ Pressed digit: " + digit + " at position [" + currentRow + "," + currentCol + "]");
        }

        // Verify OTP was entered by checking the screen
        String pageSource = getScreenXml();
        if (pageSource.contains("txtToken") && pageSource.contains(otp)) {
            ADBTestLogger.logSuccess("✅ OTP entered successfully: " + otp + " (verified in UI)");
        } else {
            ADBTestLogger.logWarning("⚠️ OTP entry could not be verified in UI - continuing...");
        }
    }

    // ==================== CHECK OTP CONNECT BUTTON HIGHLIGHTED ====================

    private void checkOTPConnectButtonHighlighted() throws Exception {
        ADBTestLogger.log("🔍 Checking if 'התחבר' button is highlighted on OTP screen...");

        // Navigate to the Connect button
        ADBTestLogger.log("   Navigating to 'התחבר' button...");
        // Press DOWN twice to reach the button from the keypad
        for (int i = 0; i < 2; i++) {
            pressDpad(20); // DOWN
            Thread.sleep(100);
        }

        // Get page source to check focus/selection
        String pageSource = getScreenXml();

        // Crop and analyze the connect button from screenshot
        String remotePath = "/sdcard/otp_connect_check_" + testStartTime + ".png";
        runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);

        String localPath = currentScreenPath + "/OTP_ConnectButton_Check_" + testStartTime + ".png";
        runCommand("adb", "-s", deviceUDID, "pull", remotePath, localPath);

        // Analyze the cropped button
        File screenshot = new File(localPath);
        BufferedImage fullImage = ImageIO.read(screenshot);

        // Crop the button area based on XML bounds: [383,449][1068,565]
        int cropX = 383;
        int cropY = 449;
        int cropWidth = 1068 - 383;
        int cropHeight = 565 - 449;

        BufferedImage croppedButton = fullImage.getSubimage(cropX, cropY, cropWidth, cropHeight);

        // Save cropped button for reference
        String croppedPath = currentScreenPath + "/OTP_ConnectButton_Cropped_" + testStartTime + ".png";
        ImageIO.write(croppedButton, "png", new File(croppedPath));

        // Count white pixels (highlighted button appears white)
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

        ADBTestLogger.log("   📊 Cropped button area: " + cropWidth + "x" + cropHeight);
        ADBTestLogger.log("   📊 Total pixels: " + totalPixels + ", White pixels: " + whiteCount);
        ADBTestLogger.log("   📊 White percentage: " + String.format("%.2f", whitePercentage) + "%");
        ADBTestLogger.log("📸 Cropped button saved to: " + croppedPath);
        ADBTestLogger.log("   🔗 file://" + croppedPath);

        // Check XML for focus/selection attributes
        boolean isFocused = pageSource.contains("focused=\"true\"") ||
                pageSource.contains("selected=\"true\"") ||
                pageSource.contains("highlighted=\"true\"");

        // Also check visual highlight via white percentage
        boolean isVisuallyHighlighted = whitePercentage >= 30.0;

        if (isFocused && isVisuallyHighlighted) {
            ADBTestLogger.logSuccess("✅ 'התחבר' button IS highlighted and in focus! (XML + Visual)");
        } else if (isFocused) {
            ADBTestLogger.logWarning("⚠️ 'התחבר' button is focused but NOT visually highlighted");
            ADBTestLogger.logWarning("   White percentage: " + String.format("%.2f", whitePercentage) + "% (expected > 30%)");
        } else if (isVisuallyHighlighted) {
            ADBTestLogger.logWarning("⚠️ 'התחבר' button is visually highlighted but NOT in focus");
        } else {
            ADBTestLogger.logError("❌ 'התחבר' button is NOT highlighted (neither XML nor visual)");
            ADBTestLogger.log("   📊 White percentage: " + String.format("%.2f", whitePercentage) + "%");
            throw new RuntimeException("'התחבר' button is not highlighted on OTP screen!");
        }

        ADBTestLogger.log("📸 Full screenshot saved to: " + new File(localPath).getAbsolutePath());
        ADBTestLogger.log("   🔗 file://" + new File(localPath).getAbsolutePath());
    }

    // ==================== PRESS OTP CONNECT BUTTON ====================

    private void pressOTPConnectButton() throws Exception {
        ADBTestLogger.log("🔘 Pressing 'התחבר' button on OTP screen...");
        // Make sure we're on the button (press DOWN twice if needed)
        for (int i = 0; i < 2; i++) {
            pressDpad(20); // DOWN
            Thread.sleep(100);
        }
        pressDpad(23); // CENTER to click
        ADBTestLogger.logSuccess("✅ 'התחבר' button pressed");
        Thread.sleep(5000); // Wait for transition
    }

    // ==================== VALIDATE LIVE MOSAIC SCREEN ====================

    private void validateLiveMosaicScreen() throws Exception {
        ADBTestLogger.log("⏳ Waiting for Live Mosaic screen to load...");

        long startTime = System.currentTimeMillis();
        long timeout = 30000;
        String pageSource = null;

        String[] expectedElements = {
                "משודר עכשיו",
                "הכל",
                "אפליקציות",
                "מועדפים",
                "סרטים",
                "סדרות ובידור",
                "אקטואליה חדשות ושפות",
                "ילדים",
                "ספורט",
                "מוזיקה",
                "תעודה טבע ופנאי",
                "רדיו"
        };

        while (System.currentTimeMillis() - startTime < timeout) {
            pageSource = getScreenXml();

            // Check if we're still on OTP screen (failed to transition)
            if (pageSource.contains("txtToken") && pageSource.contains("dvbtnConnectToken")) {
                ADBTestLogger.logWarning("⚠️ Still on OTP screen - transition may have failed");
                // Try pressing the button again
                pressOTPConnectButton();
                continue;
            }

            boolean allFound = true;
            for (String element : expectedElements) {
                if (!pageSource.contains(element)) {
                    allFound = false;
                    break;
                }
            }

            if (allFound) {
                ADBTestLogger.logSuccess("✅ Live Mosaic screen loaded successfully in " +
                        (System.currentTimeMillis() - startTime) + " ms");

                printLiveMosaicElements(pageSource, expectedElements);

                // Save success files
                takeScreenshot("live_mosaic_success");
                savePageSource("live_mosaic_success");

                return;
            }
            Thread.sleep(1000);
        }

        // Timeout - screen not loaded
        if (pageSource != null) {
            ADBTestLogger.logError("❌ Live Mosaic screen validation timed out after " + timeout + " ms");
            logMissingLiveMosaicElements(pageSource, expectedElements);
            takeScreenshot("live_mosaic_timeout");
            savePageSource("live_mosaic_timeout");
        }

        throw new RuntimeException("Live Mosaic screen failed to load within " + timeout + " seconds");
    }

    // ==================== PRINT LIVE MOSAIC ELEMENTS ====================

    private void printLiveMosaicElements(String xml, String[] expectedElements) {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("📋 LIVE MOSAIC SCREEN - FOUND ELEMENTS");
        ADBTestLogger.log("═══════════════════════════════════════════════════");

        for (String element : expectedElements) {
            if (xml.contains(element)) {
                ADBTestLogger.logSuccess("   ✅ " + element);
            } else {
                ADBTestLogger.logError("   ❌ " + element + " (MISSING)");
            }
        }

        // Check for additional unique elements
        if (xml.contains("mod_LiveMosaic")) {
            ADBTestLogger.logSuccess("   ✅ Live Mosaic module: mod_LiveMosaic");
        } else {
            ADBTestLogger.logError("   ❌ Live Mosaic module: mod_LiveMosaic (MISSING)");
        }

        if (xml.contains("mod_LiveMosaic_NavCategories")) {
            ADBTestLogger.logSuccess("   ✅ Navigation categories: mod_LiveMosaic_NavCategories");
        } else {
            ADBTestLogger.logError("   ❌ Navigation categories: mod_LiveMosaic_NavCategories (MISSING)");
        }

        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("");
    }

    // ==================== LOG MISSING LIVE MOSAIC ELEMENTS ====================

    private void logMissingLiveMosaicElements(String xml, String[] expectedElements) {
        ADBTestLogger.log("");
        ADBTestLogger.log("⚠️ Missing Live Mosaic Elements:");
        for (String element : expectedElements) {
            if (!xml.contains(element)) {
                ADBTestLogger.logError("   ❌ " + element);
            }
        }
    }

    // ==================== SAVE METHODS ====================

    private void saveToPassFolder(File screenshot) {
        try {
            File dir = new File(passFolderPath);
            if (!dir.exists()) dir.mkdirs();
            String fileName = "ValidPhoneNumberADB_PASS_" + testStartTime + ".png";
            FileUtils.copyFile(screenshot, new File(dir, fileName));
            ADBTestLogger.log("📸 PASS screenshot saved to: " + new File(dir, fileName).getAbsolutePath());
            ADBTestLogger.log("   🔗 file://" + new File(dir, fileName).getAbsolutePath());
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
            ADBTestLogger.log("   🔗 file://" + new File(dir, fileName).getAbsolutePath());
        } catch (IOException e) {
            ADBTestLogger.logError("❌ Could not save fail screenshot: " + e.getMessage());
        }
    }

    // ==================== REPORTING ====================

    private void printResultsSummary(boolean passed) {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══ TEST RESULTS SUMMARY ═══");
        ADBTestLogger.log("📌 Status: " + (passed ? "✅ PASSED" : "❌ FAILED"));
        ADBTestLogger.log("🕐 Timestamp: " + testStartTime);
    }
}