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

public class ADBInvalidPhoneScreenAssertion {

    private String testStartTime;
    private final String deviceUDID = "192.168.1.165:5555";
    private final Map<String, String> missingElements = new HashMap<>();

    // THE UNEXPECTED SCREEN (The Login screen we must NOT see)
    private final String unexpectedScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Unexpected/Full login screen/Full login screen.png";

    // Output folders
    private final String currentScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Current screen";
    private final String passFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Pass";
    private final String failFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Fail";
    private final String xmlFolderPath = "/Users/Johnny/IdeaProjects/POC/xml";

    public static void main(String[] args) {
        new ADBInvalidPhoneScreenAssertion().runTest();
    }

    public void runTest() {
        testStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        ADBTestLogger.init("ADBInvalidPhoneScreenAssertion");

        try {
            ADBTestLogger.log("═══════════════════════════════════════════════════");
            ADBTestLogger.log("🚫 INVALID PHONE SCREEN ASSERTION - PURE ADB (Negative)");
            ADBTestLogger.log("═══════════════════════════════════════════════════");

            ADBTestLogger.logStep("1", "Connecting to device via ADB");
            connectDevice();

            ADBTestLogger.logStep("2", "Force-stopping app and ensuring we are NOT on the Login screen");
            forceStopAppAndGoHome();

            ADBTestLogger.logStep("3", "Verifying all Login screen elements are MISSING");
            verifyLoginElementsMissing();

            ADBTestLogger.logStep("4", "Making current screen screenshot");
            File currentScreenshot = takeScreenshotFile();
            // Screenshot already saved to Current screen folder in takeScreenshotFile()

            ADBTestLogger.logStep("5", "Comparing current screen against the UNEXPECTED Login screen");
            compareToUnexpectedScreen(currentScreenshot);

            ADBTestLogger.logStep("6", "Printing results to console");
            printMissingElementsReport();

            ADBTestLogger.logSuccess("✅ INVALID PHONE SCREEN ASSERTION TEST PASSED!");
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

    private void forceStopAppAndGoHome() throws Exception {
        ADBTestLogger.log("🛑 Force-stopping the HOT app to clear all state...");
        runCommand("adb", "-s", deviceUDID, "shell", "am", "force-stop", "il.net.hot.hot");
        ADBTestLogger.logSuccess("✅ App process killed successfully");

        ADBTestLogger.log("   📱 Pressed HOME to ensure we are on the Streamer Home screen");
        runCommand("adb", "-s", deviceUDID, "shell", "input", "keyevent", "3");
        Thread.sleep(2000);
    }

    private String getScreenXml() throws Exception {
        String remoteDumpPath = "/sdcard/dump_" + testStartTime + ".xml";
        runCommand("adb", "-s", deviceUDID, "shell", "uiautomator", "dump", remoteDumpPath);

        String localDumpPath = xmlFolderPath + "/dump_" + testStartTime + ".xml";
        runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localDumpPath);

        return new String(Files.readAllBytes(Paths.get(localDumpPath)));
    }

    // Saves screenshot directly to Current screen folder and returns the file
    private File takeScreenshotFile() throws Exception {
        String remotePath = "/sdcard/screen_" + testStartTime + ".png";
        runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);

        String localPath = currentScreenPath + "/InvalidPhoneScreenAssertionADB_Current_" + testStartTime + ".png";
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
            String localPath = "xml/" + name + "_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localPath);
            ADBTestLogger.log("📄 Page Source: " + new File(localPath).getAbsolutePath());
            ADBTestLogger.log("   🔗 file://" + new File(localPath).getAbsolutePath());
        } catch (Exception e) {
            ADBTestLogger.logWarning("⚠️ Could not save page source: " + e.getMessage());
        }
    }

    // ==================== STEP 3: Verify Login elements MISSING ====================

    private void verifyLoginElementsMissing() throws Exception {
        ADBTestLogger.log("🔍 Checking that all Login screen elements are MISSING...");
        //Thread.sleep(10000); Activate the thread to simulate test failure
        String pageSource = getScreenXml();

        String[] loginElements = {
                "txtUserCellPhone",       // Phone number field
                "dvbtnConnect",           // Connect button
                "mod_keyboard",           // Numeric keypad
                "HOT",                    // HOT icon (logged only, NOT failing)
                "להפעלת האפליקציה",        // Top screen text
                "עוד לא מנוי HOT",         // Bottom screen text
                "מספר נייד (הרשום במנוי)"   // Phone field label
        };

        for (String element : loginElements) {
            if (element.equals("HOT")) {
                // LOG ONLY – DO NOT FAIL IF HOT ICON IS DETECTED
                if (pageSource.contains("HOT")) {
                    missingElements.put(element, "FOUND (Expected on Home screen – logging only, NOT failing)");
                    ADBTestLogger.log("   ℹ️ HOT icon detected (not failing, will be verified later in baseline comparison): " + element);
                } else {
                    missingElements.put(element, "MISSING (Correct)");
                    ADBTestLogger.logSuccess("✅ Element correctly MISSING: " + element);
                }
            } else if (element.equals("להפעלת האפליקציה")) {
                if (pageSource.contains("להפעלת האפליקציה")) {
                    missingElements.put(element, "FOUND (should be MISSING)");
                    ADBTestLogger.logError("❌ Element found (should be MISSING): " + element);
                } else {
                    missingElements.put(element, "MISSING (Correct)");
                    ADBTestLogger.logSuccess("✅ Element correctly MISSING: " + element);
                }
            } else if (element.equals("עוד לא מנוי HOT")) {
                if (pageSource.contains("עוד לא מנוי HOT")) {
                    missingElements.put(element, "FOUND (should be MISSING)");
                    ADBTestLogger.logError("❌ Element found (should be MISSING): " + element);
                } else {
                    missingElements.put(element, "MISSING (Correct)");
                    ADBTestLogger.logSuccess("✅ Element correctly MISSING: " + element);
                }
            } else if (element.equals("מספר נייד (הרשום במנוי)")) {
                if (pageSource.contains("מספר נייד (הרשום במנוי)")) {
                    missingElements.put(element, "FOUND (should be MISSING)");
                    ADBTestLogger.logError("❌ Element found (should be MISSING): " + element);
                } else {
                    missingElements.put(element, "MISSING (Correct)");
                    ADBTestLogger.logSuccess("✅ Element correctly MISSING: " + element);
                }
            } else {
                if (pageSource.contains(element)) {
                    missingElements.put(element, "FOUND (should be MISSING)");
                    ADBTestLogger.logError("❌ Element found (should be MISSING): " + element);
                } else {
                    missingElements.put(element, "MISSING (Correct)");
                    ADBTestLogger.logSuccess("✅ Element correctly MISSING: " + element);
                }
            }
        }

        // Assert: Only check the STRICT elements (NOT "HOT")
        boolean allStrictElementsMissing = true;
        for (Map.Entry<String, String> entry : missingElements.entrySet()) {
            if (entry.getKey().equals("HOT")) continue;
            if (entry.getValue().startsWith("FOUND")) {
                allStrictElementsMissing = false;
                break;
            }
        }

        if (!allStrictElementsMissing) {
            throw new RuntimeException("❌ Some strict Login screen elements were FOUND on Home screen!");
        } else {
            ADBTestLogger.logSuccess("✅ All strict Login screen elements are MISSING on Home screen!");
        }
    }

    // ==================== STEP 4: Compare against UNEXPECTED screen ====================

    private void compareToUnexpectedScreen(File currentScreenshot) throws IOException {
        try {
            File unexpectedFile = new File(unexpectedScreenPath);
            if (!unexpectedFile.exists()) {
                throw new RuntimeException("Unexpected screen baseline not found: " + unexpectedScreenPath);
            }
            BufferedImage unexpectedImage = ImageIO.read(unexpectedFile);
            BufferedImage currentImage = ImageIO.read(currentScreenshot);

            if (currentImage.getWidth() != unexpectedImage.getWidth() ||
                    currentImage.getHeight() != unexpectedImage.getHeight()) {
                ADBTestLogger.logSuccess("✅ Screens have different dimensions – this is NOT the unexpected Login screen!");
                saveToPassFolder(currentScreenshot);
                return;
            }

            long mismatchedPixels = 0;
            long totalPixels = (long) currentImage.getWidth() * currentImage.getHeight();
            int tolerance = 10;

            for (int x = 0; x < currentImage.getWidth(); x++) {
                for (int y = 0; y < currentImage.getHeight(); y++) {
                    int currentRGB = currentImage.getRGB(x, y);
                    int unexpectedRGB = unexpectedImage.getRGB(x, y);

                    int redDiff = Math.abs(((currentRGB >> 16) & 0xFF) - ((unexpectedRGB >> 16) & 0xFF));
                    int greenDiff = Math.abs(((currentRGB >> 8) & 0xFF) - ((unexpectedRGB >> 8) & 0xFF));
                    int blueDiff = Math.abs((currentRGB & 0xFF) - (unexpectedRGB & 0xFF));

                    if (redDiff > tolerance || greenDiff > tolerance || blueDiff > tolerance) {
                        mismatchedPixels++;
                    }
                }
            }

            double similarity = 100.0 - ((double) mismatchedPixels / totalPixels * 100);
            ADBTestLogger.log("📊 Image Comparison Result (vs UNEXPECTED Login screen):");
            ADBTestLogger.log("   └─ Similarity: " + String.format("%.2f", similarity) + "%");

            if (similarity >= 95.0) {
                ADBTestLogger.logError("❌ The UNEXPECTED Login screen was detected! Test FAILED.");
                saveToFailFolder(currentScreenshot);
                throw new RuntimeException("❌ The UNEXPECTED Login screen was detected!");
            } else {
                ADBTestLogger.logSuccess("✅ The current screen is NOT the Login screen. Test PASSED.");
                saveToPassFolder(currentScreenshot);
            }

        } catch (IOException e) {
            ADBTestLogger.logError("❌ Error during comparison: " + e.getMessage());
            throw new RuntimeException("Error during comparison: " + e.getMessage());
        }
    }

    // ==================== SAVE METHODS ====================

    private void saveToPassFolder(File screenshot) {
        try {
            File dir = new File(passFolderPath);
            if (!dir.exists()) dir.mkdirs();
            String fileName = "InvalidPhoneScreenAssertionADB_PASS_" + testStartTime + ".png";
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

    private void printMissingElementsReport() {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══ MISSING ELEMENTS REPORT ═══");
        for (Map.Entry<String, String> entry : missingElements.entrySet()) {
            if (entry.getValue().startsWith("FOUND")) {
                ADBTestLogger.log("   ℹ️ " + entry.getKey() + ": " + entry.getValue());
            } else {
                ADBTestLogger.logSuccess("   ✅ " + entry.getKey() + ": MISSING (Correct)");
            }
        }
    }

    private void printResultsSummary(boolean passed) {
        ADBTestLogger.log("");
        ADBTestLogger.log("═══ TEST RESULTS SUMMARY ═══");
        ADBTestLogger.log("📌 Status: " + (passed ? "✅ PASSED" : "❌ FAILED"));
        ADBTestLogger.log("🕐 Timestamp: " + testStartTime);
    }
}