package AndroidTV.V3.profiles;

import static AndroidTV.V3.profiles.ADBElementType.RESOURCE_ID;
import static AndroidTV.V3.profiles.ADBElementType.TEXT_PRESENT_ANYWHERE;

/**
 * Screen profile for the HOT Login (Phone) screen.
 */
public class ADBLoginScreenProfile {

    public static ADBScreenProfile get() {
        return ADBScreenProfile.builder("LoginScreen")
                .marker(RESOURCE_ID, "txtUserCellPhone")

                .element("topScreenText",    TEXT_PRESENT_ANYWHERE, "להפעלת האפליקציה")
                .element("phoneField",       RESOURCE_ID,            "txtUserCellPhone")
                .element("phoneFieldLabel",  TEXT_PRESENT_ANYWHERE, "מספר נייד (הרשום במנוי)")
                .element("numericKeypad",    RESOURCE_ID,            "mod_keyboard")
                .element("connectButton",    RESOURCE_ID,            "dvbtnConnect")
                .element("bottomScreenText", TEXT_PRESENT_ANYWHERE, "עוד לא מנוי HOT")
                .element("hotIcon",          TEXT_PRESENT_ANYWHERE, "HOT")

                .build();
    }
}