package AndroidTV.V3.profiles;

import static AndroidTV.V3.profiles.ElementType.RESOURCE_ID;
import static AndroidTV.V3.profiles.ElementType.TEXT;
import static AndroidTV.V3.profiles.ElementType.TEXT_PRESENT_ANYWHERE;

/**
 * Screen profile for the Live Mosaic screen.
 * Includes the fixed elements, all 11 genre tabs, and the crop checks.
 */
public class LiveMosaicScreenProfile {

    // ==================== CROP BASELINES ====================
    private static final String BASELINE_DIR =
            "/Users/Johnny/IdeaProjects/POC/Screens/Expected/Crops/LiveMosaicScreen";

    private static final String BASELINE_TITLE =
            BASELINE_DIR + "/Screen Title/MeshudarAhshav.png";

    private static final String BASELINE_GENRE_MENU =
            BASELINE_DIR + "/Genre Tabs/Genre tabs only one selected NOT active/GenreAllTabSelectedNOTactive.png";

    private static final String BASELINE_ALL_TAB =
            BASELINE_DIR + "/Genre Tabs/Single tab selected NOT active/TheAllTabIsSelectedNOTactive.png";

    private static final String BASELINE_SIDE_MENU =
            BASELINE_DIR + "/Side Menu/Close/MosaicSideMenuClosed.png";

    private static final String BASELINE_ICON =
            BASELINE_DIR + "/Side Menu/Close/LiveMosaicIconClosed.png";

    // ==================== CROP BOUNDS ====================
    private static final int[] BOUNDS_TITLE          = {1270, 80, 1460, 110};
    private static final int[] BOUNDS_GENRE_MENU     = {40, 550, 1820, 595};
    private static final int[] BOUNDS_ALL_TAB        = {1720, 545, 1815, 595};
    private static final int[] BOUNDS_SIDE_MENU      = {1808, 50, 1880, 1100};
    private static final int[] BOUNDS_LIVE_MOSAIC_ICON = {1800, 190, 1885, 270};

    // ==================== GENRE TABS ====================
    private static final String[] GENRE_TABS = {
            "הכל",
            "אפליקציות",
            "מועדפים",
            "סרטים",
            "סדרות ובידור",
            "אקטואליה חדשות ושפות",
            "ילדים",
            "ספורט",
            "מוזיקה",
            "תעודה טבע ופנאי",
            "רדיו"
    };

    public static ScreenProfile get() {
        ScreenProfile.Builder b = ScreenProfile.builder("LiveMosaicScreen")
                .marker(RESOURCE_ID, "mod_LiveMosaic")

                // Fixed elements
                .element("mod_LiveMosaic",                 RESOURCE_ID,            "mod_LiveMosaic")
                .element("mod_LiveMosaic_Synopsis",        RESOURCE_ID,            "mod_LiveMosaic_Synopsis")
                .element("mod_LiveMosaic_SynopsisLogo",    RESOURCE_ID,            "mod_LiveMosaic_SynopsisLogo")
                .element("mod_LiveMosaic_Icons",           RESOURCE_ID,            "mod_LiveMosaic_Icons")
                .element("mod_LiveMosaic_NavCategories",   RESOURCE_ID,            "mod_LiveMosaic_NavCategories")
                .element("mod_LiveMosaic_Mosaic",          RESOURCE_ID,            "mod_LiveMosaic_Mosaic")
                .element("meshudarAhshavLabel",            TEXT_PRESENT_ANYWHERE, "משודר עכשיו")
                .element("mod_Menu",                       RESOURCE_ID,            "mod_Menu")
                .element("livePipProgressBar",             RESOURCE_ID,            "livePipProgressBar")
                .element("livePipEndTime",                 RESOURCE_ID,            "livePipEndTime")
                .element("livePipStartTime",               RESOURCE_ID,            "livePipStartTime");

        // Genre tabs (exact text match)
        for (String tab : GENRE_TABS) {
            b.element("genreTab::" + tab, TEXT, tab);
        }

        // Crops
        b.crop("title_meshudar_ahshav",    BOUNDS_TITLE,            BASELINE_TITLE)
                .crop("genre_menu",               BOUNDS_GENRE_MENU,       BASELINE_GENRE_MENU)
                .crop("genre_all_tab",            BOUNDS_ALL_TAB,          BASELINE_ALL_TAB)
                .crop("side_menu_closed",         BOUNDS_SIDE_MENU,        BASELINE_SIDE_MENU)
                .crop("live_mosaic_icon_closed",  BOUNDS_LIVE_MOSAIC_ICON, BASELINE_ICON);

        return b.build();
    }
}