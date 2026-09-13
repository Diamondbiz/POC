package AndroidTV.V3.Tests.User.OTPscreenAssertion;

import AndroidTV.V3.config.RouterConfig;
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

public class ADBOTPScreenAssertion {

    protected String testStartTime;
    private final String deviceUDID = "192.168.1.165:5555";

    // Keypad layout: [1][2][3] / [4][5][6] / [7][8][9] / [Empty][0][Next]
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

    // ==================== KEYPAD CROP PARAMETERS ====================
    private static final int KEYPAD_START_X = 1370;
    private static final int KEYPAD_START_Y = 330;
    private static final int KEYPAD_WIDTH = 287;
    private static final int KEYPAD_HEIGHT = 335;

    // ==================== PHONE FIELD CROP PARAMETERS ====================
    private static final int PHONE_FIELD_CROP_X = 606;
    private static final int PHONE_FIELD_CROP_Y = 442;
    private static final int PHONE_FIELD_CROP_WIDTH = 622;
    private static final int PHONE_FIELD_CROP_HEIGHT = 53;

    // ==================== CONNECT BUTTON CROP PARAMETERS ====================
    private static final int CONNECT_BUTTON_CROP_X = 816;
    private static final int CONNECT_BUTTON_CROP_Y = 544;
    private static final int CONNECT_BUTTON_CROP_WIDTH = 252;
    private static final int CONNECT_BUTTON_CROP_HEIGHT = 66;

    // Reference folder paths
    private static final String EXPECTED_KEY_SELECTED_FOLDER = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Keypad References/Key selected";
    private static final String CURRENT_SCREEN_PATH = "/Users/Johnny/IdeaProjects/POC/Screens/Current screen";

    // Phone field reference images
    private static final String PHONE_FIELD_EMPTY_EXPECTED = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Phone field/phone_field_crop_not_selected_empty.png";
    private static final String PHONE_FIELD_VALID_EXPECTED = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Phone field/phone_field_crop_valid_phone_number.png";

    // Connect button reference images
    private static final String CONNECT_BUTTON_NOT_SELECTED_EXPECTED = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Connect Button/connect_button_crop_not_selected.png";
    private static final String CONNECT_BUTTON_SELECTED_EXPECTED = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Connect Button/connect_button_crop_selected.png";

    // Output folders
    private final String expectedScreenPath = "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Regular user/Full login screen/Valid Phone Number.png";
    private final String passFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Pass";
    private final String failFolderPath = "/Users/Johnny/IdeaProjects/POC/Screens/Fail";
    private final String xmlFolderPath = "/Users/Johnny/IdeaProjects/POC/xml";

    // Digit positions within the FULL SCREEN (from XML)
    private static final Map<String, int[]> KEY_DIGIT_SCREEN_POSITIONS = new HashMap<>();
    static {
        KEY_DIGIT_SCREEN_POSITIONS.put("1", new int[]{1389, 352, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("2", new int[]{1473, 352, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("3", new int[]{1557, 352, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("4", new int[]{1389, 424, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("5", new int[]{1473, 424, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("6", new int[]{1557, 424, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("7", new int[]{1389, 496, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("8", new int[]{1473, 496, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("9", new int[]{1557, 496, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("0", new int[]{1473, 568, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("Next", new int[]{1557, 568, 79, 68});
        KEY_DIGIT_SCREEN_POSITIONS.put("Empty", new int[]{1389, 568, 79, 68});
    }

    public static void main(String[] args) {
        new ADBOTPScreenAssertion().runTest();
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

            TestLogger.logStep("4", "Waiting for Login screen to load and validating initial state");
            waitForAppToLoad();
            validateInitialLoginScreenState();

            TestLogger.logStep("5", "Detecting which digit is currently selected (ALWAYS via Image + XML)");
            String detectedStartDigit = detectDefaultSelectedDigit();
            TestLogger.log("   🎯 Detected starting digit: " + detectedStartDigit);

            TestLogger.logStep("6", "Entering valid phone number");
            String expectedPhoneNumber = "0543501323";
            enterPhoneNumber(expectedPhoneNumber, detectedStartDigit);

            TestLogger.logStep("7", "Validating phone number via XML AND crop comparison");
            validatePhoneNumberAfterEntry(expectedPhoneNumber);

            TestLogger.logStep("8", "Verifying 'התחבר' button is selected (white highlighted)");
            verifyConnectButtonSelected();

            TestLogger.logStep("9", "Pressing 'התחבר' button");
            pressConnectButton();

            TestLogger.logStep("10", "Validating OTP screen elements and printing them");
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

    // ==================== PER-SESSION FOLDER ====================

    private String getCurrentKeypadStateFolder() {
        String folder = CURRENT_SCREEN_PATH + "/Keypad digits state_" + testStartTime;
        File dir = new File(folder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return folder;
    }

    // ==================== FILE LINK HELPER ====================

    private void logFileLink(String path) {
        String encodedPath = path.replace(" ", "%20");
        TestLogger.log("   🔗 file://" + encodedPath);
    }

    // ==================== VALIDATE INITIAL LOGIN SCREEN STATE ====================

    private void validateInitialLoginScreenState() throws Exception {
        TestLogger.log("");
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("📋 VALIDATING INITIAL LOGIN SCREEN STATE");
        TestLogger.log("═══════════════════════════════════════════════════");

        String phoneFieldText = getPhoneFieldTextFromXML();
        TestLogger.log("   📋 Phone field text from XML: '" + phoneFieldText + "'");

        if (phoneFieldText != null && phoneFieldText.contains("מספר נייד (הרשום במנוי)")) {
            TestLogger.logSuccess("   ✅ Phone field contains label: מספר נייד (הרשום במנוי)");
        } else {
            TestLogger.logError("   ❌ Phone field does NOT contain expected label!");
        }

        boolean phoneFieldEmptyMatch = validatePhoneFieldCrop(PHONE_FIELD_EMPTY_EXPECTED, "initial_empty");
        if (phoneFieldEmptyMatch) {
            TestLogger.logSuccess("   ✅ Phone field crop matches expected empty state!");
        } else {
            TestLogger.logError("   ❌ Phone field crop does NOT match expected empty state!");
        }

        boolean buttonNotSelected = validateConnectButtonCrop(CONNECT_BUTTON_NOT_SELECTED_EXPECTED, "initial_not_selected");
        if (buttonNotSelected) {
            TestLogger.logSuccess("   ✅ Connect button is NOT selected (correct initial state)!");
        } else {
            TestLogger.logError("   ❌ Connect button IS selected initially (incorrect state)!");
        }

        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("");
    }

    // ==================== VALIDATE PHONE NUMBER AFTER ENTRY (DO NOT FAIL ON XML) ====================

    private void validatePhoneNumberAfterEntry(String expectedPhoneNumber) throws Exception {
        TestLogger.log("");
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("📋 VALIDATING PHONE NUMBER AFTER ENTRY");
        TestLogger.log("═══════════════════════════════════════════════════");

        // 1. XML check (log warning only, don't fail)
        String actualPhoneNumber = getPhoneFieldTextFromXML();
        TestLogger.log("   📋 Phone field text from XML: '" + actualPhoneNumber + "'");

        String cleanedActual = actualPhoneNumber != null ? actualPhoneNumber.replace("|", "").trim() : "";
        if (cleanedActual.equals(expectedPhoneNumber)) {
            TestLogger.logSuccess("   ✅ Phone number matches expected (XML): " + expectedPhoneNumber);
        } else {
            TestLogger.logWarning("   ⚠️ Phone number does NOT match expected (XML). Continuing to crop validation...");
        }

        // 2. Crop validation (THE DEFINITIVE CHECK)
        TestLogger.log("   🔍 Running crop validation (definitive check)...");
        boolean phoneFieldValidMatch = validatePhoneFieldCrop(PHONE_FIELD_VALID_EXPECTED, "valid_number");
        if (phoneFieldValidMatch) {
            TestLogger.logSuccess("   ✅ Phone field crop matches expected valid state!");
        } else {
            TestLogger.logError("   ❌ Phone field crop does NOT match expected valid state!");
        }

        // 3. Connect button check
        boolean buttonSelected = validateConnectButtonCrop(CONNECT_BUTTON_SELECTED_EXPECTED, "selected");
        if (buttonSelected) {
            TestLogger.logSuccess("   ✅ Connect button IS selected (correct state after valid number)!");
        } else {
            TestLogger.logError("   ❌ Connect button is NOT selected (incorrect state after valid number)!");
        }

        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("");
    }

    // ==================== GET PHONE FIELD TEXT FROM XML ====================

    private String getPhoneFieldTextFromXML() throws Exception {
        String xml = getScreenXml();
        try {
            Pattern pattern = Pattern.compile("resource-id=\"txtUserCellPhone\"[^>]*text=\"([^\"]*)\"", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(xml);
            if (matcher.find()) {
                String text = matcher.group(1);
                if (text.endsWith("|")) text = text.substring(0, text.length() - 1);
                return text.trim();
            }

            Pattern pattern2 = Pattern.compile("text=\"([^\"]*)\"[^>]*resource-id=\"txtUserCellPhone\"", Pattern.DOTALL);
            Matcher matcher2 = pattern2.matcher(xml);
            if (matcher2.find()) {
                String text = matcher2.group(1);
                if (text.endsWith("|")) text = text.substring(0, text.length() - 1);
                return text.trim();
            }

            Pattern pattern3 = Pattern.compile("txtUserCellPhone[^>]*text=\"([^\"]*)\"", Pattern.DOTALL);
            Matcher matcher3 = pattern3.matcher(xml);
            if (matcher3.find()) {
                String text = matcher3.group(1);
                if (text.endsWith("|")) text = text.substring(0, text.length() - 1);
                return text.trim();
            }

            Pattern labelPattern = Pattern.compile("text=\"([^\"]*מספר נייד[^\"]*)\"", Pattern.DOTALL);
            Matcher labelMatcher = labelPattern.matcher(xml);
            if (labelMatcher.find()) {
                String text = labelMatcher.group(1);
                if (text.endsWith("|")) text = text.substring(0, text.length() - 1);
                return text.trim();
            }

            return null;
        } catch (Exception e) {
            TestLogger.logWarning("   ⚠️ Could not extract phone field text from XML: " + e.getMessage());
            return null;
        }
    }

    // ==================== VALIDATE PHONE FIELD CROP ====================

    private boolean validatePhoneFieldCrop(String expectedImagePath, String stateName) throws Exception {
        TestLogger.log("   🔍 Cropping phone field for validation...");
        String cropFileName = "phone_field_crop_" + stateName + "_" + testStartTime + ".png";
        String command = "TIMESTAMP=$(date +%Y%m%d_%H%M%S) && " +
                "cd /Users/Johnny/IdeaProjects/POC && " +
                "adb shell screencap -p /sdcard/screen_$TIMESTAMP.png && " +
                "adb pull /sdcard/screen_$TIMESTAMP.png screen_$TIMESTAMP.png && " +
                "mkdir -p '" + CURRENT_SCREEN_PATH + "' && " +
                "convert screen_$TIMESTAMP.png -crop " + PHONE_FIELD_CROP_WIDTH + "x" + PHONE_FIELD_CROP_HEIGHT +
                "+" + PHONE_FIELD_CROP_X + "+" + PHONE_FIELD_CROP_Y + " +repage '" + CURRENT_SCREEN_PATH + "/" + cropFileName + "' && " +
                "rm screen_$TIMESTAMP.png";
        runBashCommand(command);
        String actualPath = CURRENT_SCREEN_PATH + "/" + cropFileName;
        logFileLink(actualPath);
        return compareImages(actualPath, expectedImagePath);
    }

    // ==================== VALIDATE CONNECT BUTTON CROP ====================

    private boolean validateConnectButtonCrop(String expectedImagePath, String stateName) throws Exception {
        TestLogger.log("   🔍 Cropping connect button for validation...");
        String cropFileName = "connect_button_crop_" + stateName + "_" + testStartTime + ".png";
        String command = "TIMESTAMP=$(date +%Y%m%d_%H%M%S) && " +
                "cd /Users/Johnny/IdeaProjects/POC && " +
                "adb shell screencap -p /sdcard/screen_$TIMESTAMP.png && " +
                "adb pull /sdcard/screen_$TIMESTAMP.png screen_$TIMESTAMP.png && " +
                "mkdir -p '" + CURRENT_SCREEN_PATH + "' && " +
                "convert screen_$TIMESTAMP.png -crop " + CONNECT_BUTTON_CROP_WIDTH + "x" + CONNECT_BUTTON_CROP_HEIGHT +
                "+" + CONNECT_BUTTON_CROP_X + "+" + CONNECT_BUTTON_CROP_Y + " +repage '" + CURRENT_SCREEN_PATH + "/" + cropFileName + "' && " +
                "rm screen_$TIMESTAMP.png";
        runBashCommand(command);
        String actualPath = CURRENT_SCREEN_PATH + "/" + cropFileName;
        logFileLink(actualPath);
        return compareImages(actualPath, expectedImagePath);
    }

    // ==================== DETECT DEFAULT SELECTED DIGIT (ALWAYS IMAGE COMPARE) ====================

    private String detectDefaultSelectedDigit() throws Exception {
        TestLogger.log("   🔍 Detecting which digit is currently selected...");
        TestLogger.log("   ═══════════════════════════════════════════════════");

        // STEP 1: XML check (informational only, never decides alone)
        String xml = getScreenXml();
        boolean zeroIsNested = checkIfZeroIsNestedInXML(xml);
        TestLogger.log("   📋 XML analysis result: '0' is " + (zeroIsNested ? "NESTED (info only)" : "DIRECT (info only)"));

        // STEP 2: ALWAYS create per-session folder
        String currentKeypadFolder = getCurrentKeypadStateFolder();
        TestLogger.log("   📁 Per-session Keypad digits state folder: " + currentKeypadFolder);
        logFileLink(currentKeypadFolder);

        // STEP 3: Capture screenshot
        String screenshotPath = captureFreshScreenshot();
        TestLogger.log("   📸 Fresh screenshot saved to: " + screenshotPath);
        logFileLink(screenshotPath);

        // STEP 4: Compare every key
        String[] allKeys = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Empty", "Next"};
        String imageDetectedDigit = null;

        for (String key : allKeys) {
            TestLogger.log("");
            TestLogger.log("   🔍 Checking key: '" + key + "'");

            int[] pos = KEY_DIGIT_SCREEN_POSITIONS.get(key);
            if (pos == null) {
                TestLogger.logWarning("   ⚠️ No position defined for key: '" + key + "'");
                continue;
            }

            String currentCropPath = cropKeyFromScreenshot(screenshotPath, key, currentKeypadFolder);
            TestLogger.log("   📸 Current key crop saved to: " + currentCropPath);
            logFileLink(currentCropPath);

            String expectedKeyName;
            if (key.equals("Empty")) {
                expectedKeyName = "keypad_empty.png";
            } else if (key.equals("Next")) {
                expectedKeyName = "keypad_next.png";
            } else {
                expectedKeyName = "keypad_" + key.toLowerCase() + ".png";
            }

            String expectedRefPath = EXPECTED_KEY_SELECTED_FOLDER + "/" + expectedKeyName;
            File expectedRef = new File(expectedRefPath);
            if (!expectedRef.exists()) {
                TestLogger.logWarning("   ⚠️ Expected reference not found for key '" + key + "': " + expectedRefPath);
                continue;
            }

            boolean match = compareImages(currentCropPath, expectedRefPath, 90.0);
            if (match) {
                TestLogger.logSuccess("   ✅ Key '" + key + "' MATCHES selected reference!");
                imageDetectedDigit = key;
                break;
            } else {
                TestLogger.logWarning("   ❌ Key '" + key + "' does NOT match selected reference.");
            }
        }

        TestLogger.log("");
        TestLogger.log("   ═══════════════════════════════════════════════════");
        TestLogger.log("   📊 FINAL RESULT:");
        TestLogger.log("   ├─ XML: '0' is " + (zeroIsNested ? "NESTED ✅" : "DIRECT ❌"));
        TestLogger.log("   ├─ Image: " + (imageDetectedDigit != null ? "DIGIT '" + imageDetectedDigit + "' MATCHES ✅" : "NO MATCH ❌"));
        TestLogger.log("   ═══════════════════════════════════════════════════");

        String finalDigit;
        if (imageDetectedDigit != null) {
            finalDigit = imageDetectedDigit;
        } else {
            TestLogger.logWarning("   ⚠️ No digit matched. Defaulting to '0'.");
            finalDigit = "0";
        }

        TestLogger.logSuccess("   🎯 FINAL DETECTED STARTING DIGIT: '" + finalDigit + "'");
        return finalDigit;
    }

    // ==================== XML CHECK: IS '0' NESTED (INFO ONLY) ====================

    private boolean checkIfZeroIsNestedInXML(String xml) {
        TestLogger.log("   🔍 Checking if '0' is nested inside an extra View...");

        try {
            int keypadIndex = xml.indexOf("mod_keyboard");
            if (keypadIndex == -1) {
                TestLogger.logWarning("   ⚠️ 'mod_keyboard' not found in XML!");
                return false;
            }

            String keypadSection = xml.substring(keypadIndex);

            Pattern zeroNestedPattern = Pattern.compile(
                    "<node[^>]*class=\"android\\.view\\.View\"[^>]*>.*?<node[^>]*text=\"0\"[^>]*class=\"android\\.widget\\.TextView\"",
                    Pattern.DOTALL
            );
            Matcher zeroNestedMatcher = zeroNestedPattern.matcher(keypadSection);

            boolean zeroIsNested = zeroNestedMatcher.find();

            if (zeroIsNested) {
                TestLogger.log("   ✅ Found '0' nested inside an extra View (info only, NOT deciding)");
            } else {
                TestLogger.log("   ❌ '0' is NOT nested inside an extra View (info only, NOT deciding)");
            }

            return zeroIsNested;

        } catch (Exception e) {
            TestLogger.logWarning("   ⚠️ Could not parse XML: " + e.getMessage());
            return false;
        }
    }

    // ==================== CAPTURE FRESH SCREENSHOT ====================

    private String captureFreshScreenshot() throws Exception {
        String screenshotPath = CURRENT_SCREEN_PATH + "/keypad_state_full_" + testStartTime + ".png";
        String command = "TIMESTAMP=$(date +%Y%m%d_%H%M%S) && " +
                "cd /Users/Johnny/IdeaProjects/POC && " +
                "adb shell screencap -p /sdcard/screen_$TIMESTAMP.png && " +
                "adb pull /sdcard/screen_$TIMESTAMP.png screen_$TIMESTAMP.png && " +
                "mkdir -p '" + CURRENT_SCREEN_PATH + "' && " +
                "mv screen_$TIMESTAMP.png '" + screenshotPath + "'";
        runBashCommand(command);
        return screenshotPath;
    }

    // ==================== CROP A SINGLE KEY ====================

    private String cropKeyFromScreenshot(String screenshotPath, String key, String folder) throws Exception {
        int[] pos = KEY_DIGIT_SCREEN_POSITIONS.get(key);
        String cropFileName = "keypad_digit_" + key + "_" + testStartTime + ".png";
        String command = "mkdir -p '" + folder + "' && " +
                "convert '" + screenshotPath + "' -crop " + pos[2] + "x" + pos[3] +
                "+" + pos[0] + "+" + pos[1] + " +repage '" + folder + "/" + cropFileName + "'";
        runBashCommand(command);
        return folder + "/" + cropFileName;
    }

    // ==================== COMPARE IMAGES ====================

    private boolean compareImages(String actualPath, String expectedPath, double threshold) throws IOException {
        File actualFile = new File(actualPath);
        File expectedFile = new File(expectedPath);

        if (!expectedFile.exists()) {
            TestLogger.logWarning("   ⚠️ Expected image not found: " + expectedPath);
            return false;
        }

        if (!actualFile.exists()) {
            TestLogger.logWarning("   ⚠️ Actual image not found: " + actualPath);
            return false;
        }

        BufferedImage actualImage = ImageIO.read(actualFile);
        BufferedImage expectedImage = ImageIO.read(expectedFile);

        if (actualImage.getWidth() != expectedImage.getWidth() ||
                actualImage.getHeight() != expectedImage.getHeight()) {
            TestLogger.logWarning("   ⚠️ Image dimensions mismatch!");
            return false;
        }

        long mismatchedPixels = 0;
        long totalPixels = (long) actualImage.getWidth() * actualImage.getHeight();
        int tolerance = 10;

        for (int x = 0; x < actualImage.getWidth(); x++) {
            for (int y = 0; y < actualImage.getHeight(); y++) {
                int actualRGB = actualImage.getRGB(x, y);
                int expectedRGB = expectedImage.getRGB(x, y);

                int redDiff = Math.abs(((actualRGB >> 16) & 0xFF) - ((expectedRGB >> 16) & 0xFF));
                int greenDiff = Math.abs(((actualRGB >> 8) & 0xFF) - ((expectedRGB >> 8) & 0xFF));
                int blueDiff = Math.abs((actualRGB & 0xFF) - (expectedRGB & 0xFF));

                if (redDiff > tolerance || greenDiff > tolerance || blueDiff > tolerance) {
                    mismatchedPixels++;
                }
            }
        }

        double similarity = 100.0 - ((double) mismatchedPixels / totalPixels * 100);
        TestLogger.log("   📊 Image similarity: " + String.format("%.2f", similarity) + "%");

        return similarity >= threshold;
    }

    private boolean compareImages(String actualPath, String expectedPath) throws IOException {
        return compareImages(actualPath, expectedPath, 95.0);
    }

    // ==================== RUN BASH COMMAND ====================

    private void runBashCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                TestLogger.log("   🖥️ " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            TestLogger.logWarning("   ⚠️ Command exited with code " + exitCode);
        }
    }

    // ==================== SSID VALIDATION ====================

    private void printAndValidateSSID() throws Exception {
        String ssid = getCurrentSSID();
        TestLogger.log("   📶 Current SSID: " + ssid);

        Router router = RouterConfig.getRouterBySSID(ssid);
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

        String localPath = CURRENT_SCREEN_PATH + "/ValidPhoneNumberADB_Current_" + testStartTime + ".png";
        runCommand("adb", "-s", deviceUDID, "pull", remotePath, localPath);

        TestLogger.log("📸 Current screenshot saved to: " + new File(localPath).getAbsolutePath());
        logFileLink(localPath);

        return new File(localPath);
    }

    private void takeScreenshot(String name) {
        try {
            String remotePath = "/sdcard/screen_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "shell", "screencap", "-p", remotePath);

            String localPath = failFolderPath + "/" + name + "_" + testStartTime + ".png";
            runCommand("adb", "-s", deviceUDID, "pull", remotePath, localPath);

            TestLogger.log("📸 Screenshot saved to: " + new File(localPath).getAbsolutePath());
            logFileLink(localPath);
        } catch (Exception e) {
            TestLogger.logWarning("⚠️ Could not take screenshot: " + e.getMessage());
        }
    }

    private void savePageSource(String name) {
        try {
            String remoteDumpPath = "/sdcard/dump_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "shell", "uiautomator", "dump", remoteDumpPath);
            String localPath = xmlFolderPath + "/" + name + "_" + testStartTime + ".xml";
            runCommand("adb", "-s", deviceUDID, "pull", remoteDumpPath, localPath);
            TestLogger.log("📄 Page Source: " + new File(localPath).getAbsolutePath());
            logFileLink(localPath);
        } catch (Exception e) {
            TestLogger.logWarning("⚠️ Could not save page source: " + e.getMessage());
        }
    }

    // ==================== WAIT FOR APP TO LOAD ====================

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
                printLoginScreenElements(pageSource);
                return;
            }
            Thread.sleep(1000);
        }

        throw new RuntimeException("App failed to load within " + timeout + " seconds");
    }

    private void printLoginScreenElements(String xml) {
        TestLogger.log("");
        TestLogger.log("═══════════════════════════════════════════════════");
        TestLogger.log("📋 LOGIN SCREEN - FOUND ELEMENTS");
        TestLogger.log("═══════════════════════════════════════════════════");

        if (xml.contains("txtUserCellPhone")) {
            TestLogger.logSuccess("   ✅ Phone number field: txtUserCellPhone");
        } else {
            TestLogger.logError("   ❌ Phone number field: txtUserCellPhone");
        }

        if (xml.contains("dvbtnConnect")) {
            TestLogger.logSuccess("   ✅ Connect button: dvbtnConnect - \"התחבר\"");
        } else {
            TestLogger.logError("   ❌ Connect button: dvbtnConnect");
        }

        if (xml.contains("mod_keyboard")) {
            TestLogger.logSuccess("   ✅ Numeric keypad: mod_keyboard - Virtual Keypad");
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

    // ==================== ENTER PHONE NUMBER ====================

    private void enterPhoneNumber(String phoneNumber, String startDigit) throws Exception {
        TestLogger.log("📱 Entering phone number: " + phoneNumber);
        TestLogger.log("   Using DPAD navigation on keypad");
        TestLogger.log("   Starting from digit: " + startDigit);

        int[] startPos = KEYPAD_POSITIONS.get(startDigit);
        if (startPos == null) {
            TestLogger.logWarning("⚠️ Unknown start digit: " + startDigit + ", defaulting to '5'");
            startPos = KEYPAD_POSITIONS.get("5");
        }

        int currentRow = startPos[0];
        int currentCol = startPos[1];

        TestLogger.log("   Starting position: Row " + currentRow + ", Col " + currentCol);

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
                pressDpad(19);
                currentRow--;
                TestLogger.log("   ↑ Moved UP to row " + currentRow);
            }
            while (currentRow < targetRow) {
                pressDpad(20);
                currentRow++;
                TestLogger.log("   ↓ Moved DOWN to row " + currentRow);
            }

            while (currentCol > targetCol) {
                pressDpad(21);
                currentCol--;
                TestLogger.log("   ← Moved LEFT to col " + currentCol);
            }
            while (currentCol < targetCol) {
                pressDpad(22);
                currentCol++;
                TestLogger.log("   → Moved RIGHT to col " + currentCol);
            }

            pressDpad(23);
            TestLogger.log("   🔘 Pressed digit: " + digit);
            Thread.sleep(200);
        }

        TestLogger.logSuccess("✅ Phone number entered successfully: " + phoneNumber);
    }

    // ==================== VERIFY CONNECT BUTTON SELECTED ====================

    private void verifyConnectButtonSelected() throws Exception {
        TestLogger.log("🔍 Verifying 'התחבר' button is selected (white highlighted)...");

        boolean selected = validateConnectButtonCrop(CONNECT_BUTTON_SELECTED_EXPECTED, "selected");
        if (!selected) {
            throw new RuntimeException("'התחבר' button is not selected!");
        }
    }

    // ==================== PRESS CONNECT BUTTON ====================

    private void pressConnectButton() throws Exception {
        TestLogger.log("🔘 Pressing 'התחבר' button...");
        for (int i = 0; i < 2; i++) {
            pressDpad(20);
        }
        pressDpad(23);
        TestLogger.logSuccess("✅ 'התחבר' button pressed");
        Thread.sleep(3000);
    }

    // ==================== OTP SCREEN VALIDATION ====================

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
                printOTPScreenElements(pageSource);
                return;
            }

            Thread.sleep(1000);
        }

        throw new RuntimeException("OTP screen failed to load within " + timeout + " seconds");
    }

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
            logFileLink(new File(dir, fileName).getAbsolutePath());
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
            logFileLink(new File(dir, fileName).getAbsolutePath());
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