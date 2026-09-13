package AndroidTV.V3.core;

import java.io.File;

public abstract class ADBBasePage {

    protected ADBDeviceController device;
    protected ADBXmlParser parser;
    protected String xmlFolderPath;
    protected String testStartTime;

    public ADBBasePage(ADBDeviceController device, ADBXmlParser parser, String xmlFolderPath, String testStartTime) {
        this.device = device;
        this.parser = parser;
        this.xmlFolderPath = xmlFolderPath;
        this.testStartTime = testStartTime;
    }

    protected String getCurrentXml() throws Exception {
        return device.getScreenXml(xmlFolderPath, testStartTime);
    }

    protected boolean isElementPresent(String resourceId) throws Exception {
        String xml = getCurrentXml();
        return parser.containsResourceId(xml, resourceId);
    }

    protected boolean isTextPresent(String text) throws Exception {
        String xml = getCurrentXml();
        return parser.containsText(xml, text);
    }

    protected String getText(String resourceId) throws Exception {
        String xml = getCurrentXml();
        return parser.extractTextByResourceId(xml, resourceId);
    }

    protected File takeScreenshot(String savePath) throws Exception {
        return device.takeScreenshot(savePath, testStartTime);
    }

    protected void pressKey(int keyCode) throws Exception {
        device.pressKey(keyCode);
    }

    protected void pressDpadUp() throws Exception {
        device.pressDpadUp();
    }

    protected void pressDpadDown() throws Exception {
        device.pressDpadDown();
    }

    protected void pressDpadLeft() throws Exception {
        device.pressDpadLeft();
    }

    protected void pressDpadRight() throws Exception {
        device.pressDpadRight();
    }

    protected void pressCenter() throws Exception {
        device.pressCenter();
    }
}