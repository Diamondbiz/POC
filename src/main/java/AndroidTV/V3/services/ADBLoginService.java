package AndroidTV.V3.services;

import AndroidTV.V3.flows.ADBLoginFlow;

/**
 * Business logic layer for login operations.
 * Test classes call this service, NOT the flow or pages directly.
 */
public class ADBLoginService {

    private ADBLoginFlow loginFlow;

    public ADBLoginService(String deviceUDID, String xmlFolderPath, String testStartTime) throws Exception {
        this.loginFlow = new ADBLoginFlow(deviceUDID, xmlFolderPath, testStartTime);
    }

    /**
     * Opens the app and waits for the login screen.
     */
    public void openAppAndWaitForLogin() throws Exception {
        loginFlow.openAppAndWaitForLogin();
    }

    /**
     * Enters a phone number using DPAD navigation.
     */
    public void enterPhoneNumber(String phoneNumber) throws Exception {
        loginFlow.enterPhoneNumber(phoneNumber);
    }

    /**
     * Presses the "התחבר" button.
     */
    public void pressConnect() throws Exception {
        loginFlow.pressConnect();
    }

    /**
     * Waits for the OTP screen to load.
     */
    public void waitForOTP() throws Exception {
        loginFlow.waitForOTP();
    }

    /**
     * Performs the complete login with a valid phone number.
     */
    public void loginWithValidPhoneNumber(String phoneNumber) throws Exception {
        loginFlow.loginWithValidPhoneNumber(phoneNumber);
    }

    /**
     * Gets the phone number currently in the field.
     */
    public String getEnteredPhoneNumber() throws Exception {
        return loginFlow.getEnteredPhoneNumber();
    }

    /**
     * Force stops the app.
     */
    public void forceStopApp() throws Exception {
        loginFlow.forceStopApp("il.net.hot.hot");
    }
}