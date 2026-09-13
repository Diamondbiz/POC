package AndroidTV.V3.validators;

import AndroidTV.V3.core.ADBDeviceController;
import AndroidTV.V3.core.ADBXmlParser;

/**
 * Validates the Live Mosaic screen elements.
 */
public class ADBLiveMosaicValidator {

    private final ADBDeviceController device;
    private final ADBXmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;

    public ADBLiveMosaicValidator(ADBDeviceController device, ADBXmlParser parser, String xmlFolderPath, String testStartTime) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
    }

    /**
     * Returns true if all Live Mosaic screen elements are present in the current XML.
     */
    public boolean isLiveMosaicScreenPresent() throws Exception {
        String xml = device.getScreenXml(xmlFolderPath, testStartTime);
        return parser.containsResourceId(xml, "mainContainer") &&
                parser.containsText(xml, "משודר עכשיו") &&
                parser.containsResourceId(xml, "mod_LiveMosaic_Main");
    }

    /**
     * Asserts that the Live Mosaic screen is present.
     * @throws AssertionError if Live Mosaic screen elements are missing.
     */
    public void assertLiveMosaicScreenPresent() throws Exception {
        if (!isLiveMosaicScreenPresent()) {
            throw new AssertionError("❌ Live Mosaic screen elements are MISSING!");
        }
        System.out.println("✅ Live Mosaic screen elements are present.");
    }

    /**
     * Asserts that the Live Mosaic screen is NOT present (used for negative tests).
     * @throws AssertionError if Live Mosaic screen elements are found.
     */
    public void assertLiveMosaicScreenNotPresent() throws Exception {
        if (isLiveMosaicScreenPresent()) {
            throw new AssertionError("❌ Live Mosaic screen elements are PRESENT (should be missing)!");
        }
        System.out.println("✅ Live Mosaic screen elements are correctly MISSING.");
    }
}