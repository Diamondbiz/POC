package AndroidTV.V3.validators;

import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.XmlParser;

/**
 * Validates the Login (Phone) screen elements.
 */
public class ADBLoginValidator {

    private final DeviceController device;
    private final XmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;

    public ADBLoginValidator(DeviceController device, XmlParser parser, String xmlFolderPath, String testStartTime) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
    }

    /**
     * Returns true if all login screen elements are present in the current XML.
     */
    public boolean isLoginScreenPresent() throws Exception {
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        return parser.isAllLoginScreenElementsPresent(xml);
    }

    /**
     * Asserts that the login screen is present.
     * @throws AssertionError if login screen elements are missing.
     */
    public void assertLoginScreenPresent() throws Exception {
        if (!isLoginScreenPresent()) {
            throw new AssertionError("❌ Login screen elements are MISSING!");
        }
        System.out.println("✅ Login screen elements are present.");
    }

    /**
     * Asserts that the login screen is NOT present (used for negative tests).
     * @throws AssertionError if login screen elements are found.
     */
    public void assertLoginScreenNotPresent() throws Exception {
        if (isLoginScreenPresent()) {
            throw new AssertionError("❌ Login screen elements are PRESENT (should be missing)!");
        }
        System.out.println("✅ Login screen elements are correctly MISSING.");
    }

    /**
     * Verifies that the entered phone number matches the expected value.
     */
    public void assertPhoneNumberMatches(String expectedNumber) throws Exception {
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        String actualNumber = parser.extractTextByResourceId(xml, "txtUserCellPhone");

        if (!actualNumber.equals(expectedNumber)) {
            throw new AssertionError("❌ Phone number mismatch! Expected: '" + expectedNumber + "', Actual: '" + actualNumber + "'");
        }
        System.out.println("✅ Phone number verified: " + actualNumber);
    }
}