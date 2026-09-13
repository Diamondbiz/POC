package AndroidTV.V3.validators;

import AndroidTV.V3.core.ADBDeviceController;
import AndroidTV.V3.core.ADBXmlParser;

/**
 * Validates the OTP screen elements.
 */
public class ADBOTPValidator {

    private final ADBDeviceController device;
    private final ADBXmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;

    public ADBOTPValidator(ADBDeviceController device, ADBXmlParser parser, String xmlFolderPath, String testStartTime) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
    }

    /**
     * Returns true if all OTP screen elements are present in the current XML.
     */
    public boolean isOTPScreenPresent() throws Exception {
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        return parser.isAllOTPElementsPresent(xml);
    }

    /**
     * Asserts that the OTP screen is present.
     * @throws AssertionError if OTP screen elements are missing.
     */
    public void assertOTPScreenPresent() throws Exception {
        if (!isOTPScreenPresent()) {
            throw new AssertionError("❌ OTP screen elements are MISSING!");
        }
        System.out.println("✅ OTP screen elements are present.");
    }

    /**
     * Asserts that the OTP screen is NOT present (used for negative tests).
     * @throws AssertionError if OTP screen elements are found.
     */
    public void assertOTPScreenNotPresent() throws Exception {
        if (isOTPScreenPresent()) {
            throw new AssertionError("❌ OTP screen elements are PRESENT (should be missing)!");
        }
        System.out.println("✅ OTP screen elements are correctly MISSING.");
    }

    /**
     * Verifies that the entered OTP matches the expected value.
     */
    public void assertOTPMatches(String expectedOTP) throws Exception {
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        String actualOTP = parser.extractTextByResourceId(xml, "txtToken");

        if (!actualOTP.equals(expectedOTP)) {
            throw new AssertionError("❌ OTP mismatch! Expected: '" + expectedOTP + "', Actual: '" + actualOTP + "'");
        }
        System.out.println("✅ OTP verified: " + actualOTP);
    }
}