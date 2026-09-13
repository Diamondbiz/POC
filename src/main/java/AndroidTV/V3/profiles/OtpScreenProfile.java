package AndroidTV.V3.profiles;

import static AndroidTV.V3.profiles.ElementType.RESOURCE_ID;
import static AndroidTV.V3.profiles.ElementType.TEXT_PRESENT_ANYWHERE;

/**
 * Screen profile for the OTP screen.
 */
public class OtpScreenProfile {

    public static ScreenProfile get() {
        return ScreenProfile.builder("OTPScreen")
                .marker(RESOURCE_ID, "txtToken")

                .element("otpInputField", RESOURCE_ID,            "txtToken")
                .element("verifyButton",  RESOURCE_ID,            "dvbtnConnectToken")
                .element("resendLink",    RESOURCE_ID,            "mod_PopupLogin_ResendTokenLink")
                .element("otpLabel",      TEXT_PRESENT_ANYWHERE, "הזן קוד אימות שנשלח לנייד")

                .build();
    }
}