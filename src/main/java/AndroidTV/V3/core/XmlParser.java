package AndroidTV.V3.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw XML from uiautomator dump to find elements and extract text.
 */
public class XmlParser {

    /**
     * Checks if the XML contains a specific resource ID.
     */
    public boolean containsResourceId(String xml, String resourceId) {
        return xml.contains("resource-id=\"" + resourceId + "\"");
    }

    /**
     * Checks if the XML contains a specific text.
     */
    public boolean containsText(String xml, String text) {
        return xml.contains(text);
    }

    /**
     * Extracts the "text" attribute of the node with the given resource ID.
     * Removes the trailing "|" cursor indicator.
     */
    public String extractTextByResourceId(String xml, String resourceId) {
        Pattern nodePattern = Pattern.compile("<node[^>]*resource-id=\"" + resourceId + "\"[^>]*>");
        Matcher nodeMatcher = nodePattern.matcher(xml);

        if (nodeMatcher.find()) {
            String node = nodeMatcher.group(0);
            Pattern textPattern = Pattern.compile("text=\"([^\"]*)\"");
            Matcher textMatcher = textPattern.matcher(node);
            if (textMatcher.find()) {
                return textMatcher.group(1).replace("|", "").trim();
            }
        }
        return "";
    }

    // ==================== ROOT PACKAGE ====================

    /**
     * Extracts the package name from the first "package=" attribute in the XML.
     */
    public String getRootPackage(String xml) {
        if (xml == null) return "";

        int idx = xml.indexOf("package=\"");
        if (idx == -1) return "";

        int start = idx + "package=\"".length();
        int end = xml.indexOf("\"", start);
        if (end == -1) return "";

        return xml.substring(start, end);
    }

    // ==================== SELECTED KEYPAD DIGIT ====================

    /**
     * Returns the currently selected digit (0-9) on the keypad, as detected
     * from the uiautomator XML.
     *
     * Handles three XML shapes:
     *   1) Flat:     <node text="5" selected="true" ... />
     *   2) Nested:   <node selected="true"><node text="5" /></node>
     *   3) Deeply nested: <node class="View"><node text="0" ... /></node>
     *      (0 is a special case where the selected attribute may be on a parent View)
     *
     * Returns "" if no selected digit can be determined.
     */
    public String getSelectedKeypadDigit(String xml) {
        if (xml == null) return "";

        // Focus on the keypad section if we can find it; otherwise scan all XML.
        int keypadStart = xml.indexOf("mod_keyboard");
        String section = (keypadStart >= 0) ? xml.substring(keypadStart) : xml;

        // Find any node with selected="true".
        Pattern selectedPattern = Pattern.compile("<node\\b[^>]*\\bselected=\"true\"[^>]*>");
        Matcher m = selectedPattern.matcher(section);

        while (m.find()) {
            String node = m.group(0);

            // Case 1: the selected node itself has text="X"
            String digit = extractDigitFromTextAttr(node);
            if (digit != null) return digit;

            // Case 2: the selected node contains a child node with text="X"
            // Find the end of this node's opening tag, then look for the next <node ... text="X" .../>
            int nodeStart = m.start();
            String afterNode = section.substring(nodeStart, Math.min(nodeStart + 400, section.length()));
            String childDigit = findFirstDigitInSubstring(afterNode, node.length());
            if (childDigit != null) return childDigit;
        }

        return "";
    }

    // ==================== SELECTED DIGIT HELPERS ====================

    /**
     * Extracts a single digit (0-9) from a text="X" attribute in the given snippet.
     * Returns null if not found.
     */
    private String extractDigitFromTextAttr(String snippet) {
        Pattern textPattern = Pattern.compile("text=\"([^\"]*)\"");
        Matcher m = textPattern.matcher(snippet);
        if (m.find()) {
            String value = m.group(1);
            if (value.length() == 1 && Character.isDigit(value.charAt(0))) {
                return value;
            }
        }
        return null;
    }

    /**
     * Looks for the first text="X" where X is a single digit, starting at
     * {@code fromIndex} characters into {@code snippet}. Used to find a child
     * digit inside a selected parent node.
     */
    private String findFirstDigitInSubstring(String snippet, int fromIndex) {
        if (fromIndex >= snippet.length()) return null;

        String tail = snippet.substring(fromIndex);
        Pattern textPattern = Pattern.compile("text=\"([^\"]*)\"");
        Matcher m = textPattern.matcher(tail);

        while (m.find()) {
            String value = m.group(1);
            if (value.length() == 1 && Character.isDigit(value.charAt(0))) {
                return value;
            }
        }
        return null;
    }

    // ==================== LOGIN SCREEN CHECKS ====================

    /**
     * Returns true if all required login screen elements are present.
     */
    public boolean isAllLoginScreenElementsPresent(String xml) {
        return containsResourceId(xml, "txtUserCellPhone") &&
                containsResourceId(xml, "dvbtnConnect") &&
                containsResourceId(xml, "mod_keyboard");
    }

    // ==================== OTP SCREEN CHECKS ====================

    /**
     * Returns true if all required OTP screen elements are present.
     */
    public boolean isAllOTPElementsPresent(String xml) {
        return containsResourceId(xml, "txtToken") &&
                containsResourceId(xml, "dvbtnConnectToken") &&
                containsResourceId(xml, "mod_PopupLogin_ResendTokenLink");
    }
}