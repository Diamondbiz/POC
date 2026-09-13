package AndroidTV.V3.Tests.User.ScreensAssertion;

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

public class ADBPhoneScreenAssertion {

    private String testStartTime;
    private final String deviceUDID = "192.168.1.10:5555";
    private final Map<String, String> detectedElements = new HashMap<>();
    private boolean appLoaded = false;

    // Expected baseline path
    private final String expectedScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Regular user/Full login screen/Full login screen.png";

    // Folder paths
    private final String currentScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Current screen";
    private final String passFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Pass";
    private final String failFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Fail";
    private final String xmlFolderPath = "/Users/Johnny/IdeaProjects/POC/xml";

    public static void main(String[] args) {
        new ADBPhoneScreenAssertion().runTest();
    }

    public void runTest() {
        testStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ADBTestLogger.init("ADBPhoneScreenAssertion");

        try {
            ADBTestLogger.log("═══════════════════════════════════════════════════");
            ADBTestLogger.log("📱 PHONE SCREEN ASSERTION TEST - PURE ADB (Module 1)");
            ADBTestLogger.log("═══════════════════════════════════════════════════");

            ADBTestLogger.logStep("1", "Connecting to device via ADB");
            connectDevice();

            ADBTestLogger.logStep("2", "Pressing HOME button twice");
            pressHome();

            ADBTestLogger.logStep("3", "Clicking HOT icon to launch app");
            launchApp();

            ADBTestLogger.logStep("4", "Waiting for Login screen to load and validating elements");
            waitForAppToLoad();

            ADBTestLogger.logStep("5", "Detecting and validating all UI elements on Login Screen");
            detectAndValidateAllElements();

            ADBTestLogger.logStep("6", "Printing Login Screen element detection report");
            printElementReport();

            ADBTestLogger.logStep("7", "Asserting all required Login Screen elements are present");
            assertAllElementsPresent();

            ADBTestLogger.logStep("8", "Comparing Login Screen background to baseline image");
            validateLoginScreenBackground();

            ADBTestLogger.logSuccess("✅ PHONE SCREEN ASSERTION TEST PASSED!");
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
        ADBTestLogger.log("   📱 Pressing HOME (1/2)");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "3");
        Thread.sleep(1000);
        ADBTestLogger.log("   📱 Pressing HOME (2/2)");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "3");
        Thread.sleep(1000);
    }

    private void launchApp() throws Exception {
        ADBTestLogger.log("   🚀 Clicking HOT icon (DPAD_CENTER)");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "23");
        ADBTestLogger.logSuccess("✅ HOT icon clicked / App launching...");
        Thread.sleep(15000);
    }

    private String getScreenXml() throws Exception {
        String remoteDumpPath = "/sdcard/dump_" + testStartTime + ".xml";
        runCommand("adb", "-s", deviceUDID, "shell", "uiautomator", "dump", remoteDumpPath);

        String localDumpPath = xmlFolderPath + "/dump_" + testStartTime + ".xml";
        runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localDumpPath);

        return new String(Files.readAllBytes(Paths.get(localDumpPath)));
    }

    private void takeScreenshot(String name) {
        try {
            String remotePath = "/sdcard/screen_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);

            String localPath = "Screens/" + name + "_" + testStartTime + ".png";
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
            String localPath = "xml/" + name + "_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localPath);
            ADBTestLogger.log("📄 Page Source: " + new File(localPath).getAbsolutePath());
            ADBTestLogger.log("   🔗 file://" + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            ADBTestLogger.logWarning("⚠️ Could not save page source: " + e.getMessage());
        }
    }

    // ==================== UI ELEMENT DETECTION ====================

    private void waitForAppToLoad() throws Exception {
        ADBTestLogger.log("⏳ Waiting for app to load and validating login screen...");
        ADBTestLogger.log("");
        ADBTestLogger.log("┌─────────────────────────────────────────────────┐");
        ADBTestLogger.log("│ 🔍 VALIDATING LOGIN SCREEN ELEMENTS           │");
        ADBTestLogger.log("├─────────────────────────────────────────────────┤");

        long startTime = System.currentTimeMillis();
        long timeout = 60000;

        while (System.currentTimeMillis() - startTime < timeout) {
            String pageSource = getScreenXml();

            boolean phoneFieldFound = pageSource.contains("txtUserCellPhone");
            boolean phoneLabelFound = pageSource.contains("מספר נייד (הרשום במנוי)");
            boolean connectButtonFound = pageSource.contains("dvbtnConnect");
            boolean hotIconFound = pageSource.contains("HOT");
            boolean numericKeypadFound = pageSource.contains("mod_keyboard");
            boolean topTextFound = pageSource.contains("להפעלת האפליקציה");
            boolean bottomTextFound = pageSource.contains("עוד לא מנוי HOT");

            if (phoneFieldFound && phoneLabelFound && connectButtonFound && hotIconFound &&
                    numericKeypadFound && topTextFound && bottomTextFound) {
                appLoaded = true;
                ADBTestLogger.log("");
                ADBTestLogger.logSuccess("✅ All login screen elements validated successfully!");
                ADBTestLogger.log("└─────────────────────────────────────────────────┘");
                return;
            }

            Thread.sleep(1000);
        }

        throw new RuntimeException("App failed to load within " + timeout + " seconds");
    }

    private void detectAndValidateAllElements() throws Exception {
        ADBTestLogger.log("");
        ADBTestLogger.log("┌─────────────────────────────────────────────────┐");
        ADBTestLogger.log("│ 🔍 DETECTED LOGIN SCREEN UI ELEMENTS          │");
        ADBTestLogger.log("├─────────────────────────────────────────────────┤");

        String pageSource = getScreenXml();

        detectTopScreenText(pageSource);
        detectPhoneField(pageSource);
        detectPhoneFieldLabel(pageSource);
        detectNumericKeypad(pageSource);
        detectConnectButton(pageSource);
        detectBottomScreenText(pageSource);
        detectHOTIcon(pageSource);

        ADBTestLogger.log("└─────────────────────────────────────────────────┘");
        ADBTestLogger.log("");
    }

    private void detectTopScreenText(String pageSource) {
        ADBTestLogger.log("┃ 1️⃣ Top Screen Text");
        if (pageSource.contains("להפעלת האפליקציה")) {
            detectedElements.put("topScreenText", "FOUND");
            ADBTestLogger.log("┃    ✅ להפעלת האפליקציה אנא וודא שהמסך/סטרימר מחובר לרשת HOT בביתך");
            ADBTestLogger.logSuccess("Top screen text found");
        } else {
            detectedElements.put("topScreenText", "NOT FOUND");
            ADBTestLogger.logError("Top screen text not found");
        }
    }

    private void detectPhoneField(String pageSource) {
        ADBTestLogger.log("┃ 2️⃣ Phone Number Field");
        if (pageSource.contains("txtUserCellPhone")) {
            detectedElements.put("phoneField", "FOUND");
            ADBTestLogger.logSuccess("Phone field found");
        } else {
            detectedElements.put("phoneField", "NOT FOUND");
            ADBTestLogger.logError("Phone field not found");
        }
    }

    private void detectPhoneFieldLabel(String pageSource) {
        ADBTestLogger.log("┃ 3️⃣ Phone Field Label");
        if (pageSource.contains("מספר נייד (הרשום במנוי)")) {
            detectedElements.put("phoneFieldLabel", "FOUND");
            ADBTestLogger.logSuccess("Phone field label found");
        } else {
            detectedElements.put("phoneFieldLabel", "NOT FOUND");
            ADBTestLogger.logError("Phone field label not found");
        }
    }

    private void detectNumericKeypad(String pageSource) {
        ADBTestLogger.log("┃ 4️⃣ Numeric Keypad (1-9)");
        boolean allDigitsFound = true;
        for (int i = 1; i <= 9; i++) {
            if (!pageSource.contains("\"" + i + "\"")) {
                allDigitsFound = false;
                break;
            }
        }
        if (allDigitsFound) {
            detectedElements.put("numericKeypad", "FOUND");
            ADBTestLogger.logSuccess("Numeric keypad found");
        } else {
            detectedElements.put("numericKeypad", "NOT FOUND");
            ADBTestLogger.logError("Numeric keypad not found");
        }
    }

    private void detectConnectButton(String pageSource) {
        ADBTestLogger.log("┃ 5️⃣ Connect Button");
        if (pageSource.contains("dvbtnConnect")) {
            detectedElements.put("connectButton", "FOUND");
            ADBTestLogger.logSuccess("Connect button found");
        } else {
            detectedElements.put("connectButton", "NOT FOUND");
            ADBTestLogger.logError("Connect button not found");
        }
    }

    private void detectBottomScreenText(String pageSource) {
        ADBTestLogger.log("┃ 6️⃣ Bottom Screen Text");
        if (pageSource.contains("עוד לא מנוי HOT")) {
            detectedElements.put("bottomScreenText", "FOUND");
            ADBTestLogger.logSuccess("Bottom screen text found");
        } else {
            detectedElements.put("bottomScreenText", "NOT FOUND");
            ADBTestLogger.logError("Bottom screen text not found");
        }
    }

    private void detectHOTIcon(String pageSource) {
        ADBTestLogger.log("┃ 7️⃣ HOT Icon");
        if (pageSource.contains("HOT")) {
            detectedElements.put("hotIcon", "FOUND");
            ADBTestLogger.logSuccess("HOT icon found");
        } else {
            detectedElements.put("hotIcon", "NOT FOUND");
            ADBTestLogger.logWarning("HOT icon not found");
        }
    }

    // ==================== PRINT & ASSERT ====================

    private void printElementReport() {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("📋 LOGIN SCREEN ELEMENT DETECTION REPORT");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
        ADBTestLogger.log("");

        long foundCount = detectedElements.values().stream().filter(v -> v.equals("FOUND")).count();
        long totalCount = detectedElements.size();

        ADBTestLogger.log("📊 SUMMARY:");
        ADBTestLogger.log("   ├─ Elements Found: " + foundCount + "/" + totalCount);
        ADBTestLogger.log("   └─ Success Rate: " + (foundCount * 100 / totalCount) + "%");
        ADBTestLogger.log("");
        ADBTestLogger.log("═══════════════════════════════════════════════════");
    }

    private void assertAllElementsPresent() {
        ADBTestLogger.log("");
        ADBTestLogger.log("🔍 === ASSERTING ALL LOGIN SCREEN ELEMENTS ARE PRESENT ===");

        boolean allPresent = true;
        StringBuilder errors = new StringBuilder();

        String[] requiredElements = {
                "topScreenText", "phoneField", "phoneFieldLabel",
                "numericKeypad", "connectButton", "bottomScreenText"
        };

        for (String element : requiredElements) {
            String value = detectedElements.getOrDefault(element, "NOT FOUND");
            if (value.equals("NOT FOUND")) {
                allPresent = false;
                errors.append("   ❌ ").append(element).append(" is missing\n");
            } else {
                ADBTestLogger.log("   ✅ " + element + ": FOUND");
            }
        }

        if (allPresent) {
            ADBTestLogger.logSuccess("✅ All required login screen elements are present!");
        } else {
            ADBTestLogger.logError("❌ Missing login screen elements:");
            ADBTestLogger.logError(errors.toString());
            throw new RuntimeException("Required login screen elements missing:\n" + errors.toString());
        }
    }

    // ==================== VISUAL BASELINE COMPARISON ====================

    private void validateLoginScreenBackground() throws Exception {
        ADBTestLogger.log("🔍 Comparing Login Screen background to baseline image...");

        String remoteScreenshotPath = "/sdcard/screen_" + testStartTime + ".png";
        String localScreenshotPath = currentScreenPath + "/PhoneScreenAssertionADB_Current_" + testStartTime + ".png";

        runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remoteScreenshotPath);
        runCommand("adb", "-s", deviceUDID, "pull", remoteScreenshotPath, localScreenshotPath);

        File currentScreenshot = new File(localScreenshotPath);
        File expectedFile = new File(expectedScreenPath);

        if (!expectedFile.exists()) {
            throw new RuntimeException("Expected screen not found: " + expectedScreenPath);
        }

        BufferedImage currentImage = ImageIO.read(currentScreenshot);
        BufferedImage expectedImage = ImageIO.read(expectedFile);

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
            ADBTestLogger.logSuccess("✅ Login Screen background matches baseline (95%+ similarity)");
            saveToPassFolder(currentScreenshot);
        } else {
            ADBTestLogger.logError("❌ Login Screen background does NOT match baseline! Similarity: " + similarity + "%");
            saveToFailFolder(currentScreenshot);
            throw new RuntimeException("Login Screen visual mismatch! Similarity: " + similarity + "%");
        }
    }

    // ==================== SAVE FILES ====================

    private void saveToPassFolder(File screenshot) {
        try {
            File dir = new File(passFolderPath);
            if (!dir.exists()) dir.mkdirs();
            String fileName = "PhoneScreenAssertionADB_PASS_" + testStartTime + ".png";
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
            String fileName = "PhoneScreenAssertionADB_FAIL_" + testStartTime + ".png";
            FileUtils.copyFile(screenshot, new File(dir, fileName));
            ADBTestLogger.log("📸 FAIL screenshot saved to: " + new File(dir, fileName).getAbsolutePath());
            ADBTestLogger.log("   🔗 file://" + new File(dir, fileName).getAbsolutePath());
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