package AndroidTV.V3.pages;

import AndroidTV.V3.core.DeviceController;
import AndroidTV.V3.core.XmlParser;

/**
 * Page Object for the Live Mosaic screen (after successful login).
 */
public class ADBLiveMosaicPage {

    private final DeviceController device;
    private final XmlParser parser;
    private final String xmlFolderPath;
    private final String testStartTime;

    public ADBLiveMosaicPage(DeviceController device, XmlParser parser, String xmlFolderPath, String testStartTime) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
    }

    /**
     * Checks if the Live Mosaic screen elements are present in the XML.
     */
    public boolean isLiveMosaicScreenPresent(String xml) {
        // Based on your XML, these are key Live Mosaic elements
        return parser.containsResourceId(xml, "mainContainer") &&
                parser.containsText(xml, "משודר עכשיו") &&
                parser.containsResourceId(xml, "mod_LiveMosaic_Main");
    }

    /**
     * Waits for the Live Mosaic screen to load.
     */
    public boolean waitForLiveMosaicScreen(int timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            String xml = device.getScreenXml(xmlFolderPath, testStartTime);
            if (isLiveMosaicScreenPresent(xml)) {
                return true;
            }
            Thread.sleep(1000);
        }
        return false;
    }

    /**
     * Takes a screenshot of the Live Mosaic screen.
     */
    public java.io.File takeScreenshot(String savePath) throws Exception {
        return device.takeScreenshot(savePath, testStartTime);
    }
}