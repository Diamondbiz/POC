package AndroidTV.V3.services;

import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.XmlParser;
import AndroidTV.V3.utils.ImageComparator;
import AndroidTV.V3.utils.TestLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects which keypad key is currently selected.
 *
 * Two independent signals are always evaluated:
 *   - XML: finds the nested (selected) node and matches its bounds to a key.
 *   - Image: crops each key from a fresh screenshot and compares to reference images.
 *
 * Reconciliation (current policy — image primary):
 *   - If image yields a usable key → use it (log XML for comparison).
 *   - Else if XML yields a usable key → use it (log image failure).
 *   - Else → fall back to "0" with a loud warning.
 *
 * After the decision, the service also reports whether the detected digit
 * matches the expected fresh-state default ("0"). This is informational only —
 * navigation always proceeds from the detected position.
 */
public class KeypadStateService {

    private static final double SIMILARITY_THRESHOLD = 90.0;
    private static final String EXPECTED_DEFAULT = "0";

    private final DeviceController device;
    private final XmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;
    private final String referenceSelectedFolder;
    private final String perSessionFolder;
    private final Map<String, int[]> keyBoundsOnScreen;

    private static final String[] ALL_KEYS = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "Back", "Next"
    };

    public KeypadStateService(DeviceController device,
                              XmlParser parser,
                              String xmlFolderPath,
                              String testStartTime,
                              String referenceSelectedFolder,
                              String perSessionFolder,
                              Map<String, int[]> keyBoundsOnScreen) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
        this.referenceSelectedFolder = referenceSelectedFolder;
        this.perSessionFolder = perSessionFolder;
        this.keyBoundsOnScreen = keyBoundsOnScreen;
    }

    // ==================== PUBLIC API ====================

    /**
     * Detects which key is currently selected.
     * Always logs both signals, then reconciles and returns the decision.
     * Also prints whether the decision matches the expected default.
     */
    public String detectSelectedDigit() throws Exception {
        TestLogger.log("   🔍 Detecting selected digit...");

        String xmlValue = detectFromXml();
        String imageValue = detectFromImage();

        String decision = reconcile(xmlValue, imageValue);

        TestLogger.log("   🎯 Detected: " + decision);

        // ---- Default-expectation check (informational only) ----
        if (EXPECTED_DEFAULT.equals(decision)) {
            TestLogger.log("   ✅ Matches expected default (" + EXPECTED_DEFAULT + ")");
        } else if (isUsableKey(decision)) {
            TestLogger.logWarning("   ⚠️ Detected '" + decision +
                    "', but expected default is '" + EXPECTED_DEFAULT + "'");
            TestLogger.logWarning("      The keypad was NOT in its fresh state. " +
                    "The framework navigated from the detected position.");
        } else {
            TestLogger.logWarning("   ❌ No usable detection — fallback '" +
                    EXPECTED_DEFAULT + "' used");
        }

        return decision;
    }

    /**
     * Returns true if a string is a usable keypad key (0-9, Back, Next).
     */
    private boolean isUsableKey(String value) {
        if (value == null || value.isEmpty()) return false;
        if (value.equals("Back") || value.equals("Next")) return true;
        return value.length() == 1 && Character.isDigit(value.charAt(0));
    }

    // ==================== SIGNAL: XML ====================

    private String detectFromXml() throws Exception {
        try {
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            int keypadIdx = xml.indexOf("mod_keyboard_Container");
            if (keypadIdx == -1) {
                TestLogger.log("   XML signal:    (mod_keyboard_Container not found)");
                return "";
            }

            String section = xml.substring(keypadIdx);

            Pattern selectedPattern = Pattern.compile(
                    "<node[^>]*class=\"android\\.view\\.View\"[^>]*bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"[^>]*>" +
                            "(?:(?!<node[^>]*class=\"android\\.view\\.View\").)*?<node[^>]*class=\"android\\.widget\\.TextView\"",
                    Pattern.DOTALL);

            Matcher m = selectedPattern.matcher(section);
            while (m.find()) {
                int x1 = Integer.parseInt(m.group(1));
                int y1 = Integer.parseInt(m.group(2));

                String key = matchKeyByTopLeft(x1, y1);
                if (!key.isEmpty()) {
                    TestLogger.log("   XML signal:    " + key +
                            "  (matched nested node bounds at " + x1 + "," + y1 + ")");
                    return key;
                }
            }

            TestLogger.log("   XML signal:    (no nested node matched a known key)");
            return "";
        } catch (Exception e) {
            TestLogger.log("   XML signal:    (error: " + e.getMessage() + ")");
            return "";
        }
    }

    private String matchKeyByTopLeft(int x, int y) {
        int tolerance = 5;
        for (Map.Entry<String, int[]> e : keyBoundsOnScreen.entrySet()) {
            int[] b = e.getValue();
            if (Math.abs(b[0] - x) <= tolerance && Math.abs(b[1] - y) <= tolerance) {
                return e.getKey();
            }
        }
        return "";
    }

    // ==================== SIGNAL: IMAGE ====================

    private String detectFromImage() throws Exception {
        new File(perSessionFolder).mkdirs();
        File shot = device.takeScreenshot(perSessionFolder, testStartTime);
        String screenshotPath = shot.getAbsolutePath();

        for (String key : ALL_KEYS) {
            int[] bounds = keyBoundsOnScreen.get(key);
            if (bounds == null) continue;

            String cropPath = cropKeyFromScreenshot(screenshotPath, key, bounds);

            String refFileName = "keypad_" + key.toLowerCase() + ".png";
            String refPath = referenceSelectedFolder + "/" + refFileName;
            File refFile = new File(refPath);
            if (!refFile.exists()) continue;

            double similarity = ImageComparator.compare(cropPath, refPath);
            if (similarity >= SIMILARITY_THRESHOLD) {
                TestLogger.log("   Image signal:  " + key +
                        "  (" + String.format("%.2f", similarity) + "%)");
                return key;
            }
        }

        TestLogger.log("   Image signal:  (no match)");
        return "";
    }

    // ==================== RECONCILER ====================

    private String reconcile(String xmlValue, String imageValue) {
        boolean xmlOk = xmlValue != null && !xmlValue.isEmpty();
        boolean imgOk = imageValue != null && !imageValue.isEmpty();

        if (imgOk && xmlOk) {
            if (imageValue.equals(xmlValue)) {
                TestLogger.log("   ✅ Signals AGREE");
            } else {
                TestLogger.log("   ⚠️ Signals DISAGREE — trusting IMAGE: "
                        + imageValue + " (XML said " + xmlValue + ")");
            }
            return imageValue;
        }

        if (imgOk) {
            TestLogger.log("   ⚠️ XML failed — trusting IMAGE: " + imageValue);
            return imageValue;
        }

        if (xmlOk) {
            TestLogger.log("   ⚠️ Image failed — trusting XML: " + xmlValue);
            return xmlValue;
        }

        TestLogger.log("   ❌ No reliable detection — using fallback 0");
        return "0";
    }

    // ==================== HELPERS ====================

    private String cropKeyFromScreenshot(String screenshotPath, String key, int[] pos) throws Exception {
        int x = pos[0], y = pos[1], w = pos[2], h = pos[3];
        String cropPath = perSessionFolder + "/keypad_digit_" + key + "_" + testStartTime + ".png";

        String cmd = "convert '" + screenshotPath + "' -crop " +
                w + "x" + h + "+" + x + "+" + y + " +repage '" + cropPath + "'";
        runBash(cmd);
        return cropPath;
    }

    private int runBash(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (r.readLine() != null) { /* silent */ }
        }
        return process.waitFor();
    }
}