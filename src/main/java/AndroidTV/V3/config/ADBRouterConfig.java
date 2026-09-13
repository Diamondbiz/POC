package AndroidTV.V3.config;

import AndroidTV.V3.Models.Router;
import AndroidTV.V3.Models.RouterType;

import java.util.HashMap;
import java.util.Map;

public class ADBRouterConfig {
    private static final Map<String, Router> ROUTERS = new HashMap<>();

    static {
        // HOT Routers
        ROUTERS.put("HOT-Gigabit-5G", new Router("HOT-Gigabit-5G", "AA:BB:CC:DD:EE:01", RouterType.HOT));
        ROUTERS.put("HOT-Gigabit-2.4G", new Router("HOT-Gigabit-2.4G", "AA:BB:CC:DD:EE:02", RouterType.HOT));
        ROUTERS.put("HOT-Home-WiFi", new Router("HOT-Home-WiFi", "AA:BB:CC:DD:EE:03", RouterType.HOT));
        ROUTERS.put("HOT-5G", new Router("HOT-5G", "AA:BB:CC:DD:EE:04", RouterType.HOT));
        ROUTERS.put("HOT-2.4G", new Router("HOT-2.4G", "AA:BB:CC:DD:EE:05", RouterType.HOT));

        // MOSDI Routers
        ROUTERS.put("Mosdi-HOT-5G", new Router("Mosdi-HOT-5G", "AA:BB:CC:DD:EE:06", RouterType.MOSDI));
        ROUTERS.put("Mosdi-HOT-2.4G", new Router("Mosdi-HOT-2.4G", "AA:BB:CC:DD:EE:07", RouterType.MOSDI));
        ROUTERS.put("Mosdi-5G", new Router("Mosdi-5G", "AA:BB:CC:DD:EE:08", RouterType.MOSDI));
        ROUTERS.put("Mosdi-2.4G", new Router("Mosdi-2.4G", "AA:BB:CC:DD:EE:09", RouterType.MOSDI));

        // Regular User Family Routers
        ROUTERS.put("BLUE SKY-QA1", new Router("BLUE SKY-QA1", "CC:19:A8:D4:D8:8F", RouterType.HOT, "0522522306"));
        ROUTERS.put("Oleg-QA 5MHz-5F", new Router("Oleg-QA 5MHz-5F", "D8:78:7F:B1:04:5F", RouterType.HOT, "0543501323"));

        // QA BLUE-SKY 5Mhz router (regular user)
        ROUTERS.put("QA BLUE-SKY 5Mhz", new Router("QA BLUE-SKY 5Mhz", "AA:BB:CC:DD:EE:10", RouterType.HOT, "0543501323"));

        // MOSDI Family Router
        ROUTERS.put("QA non-HOT", new Router("QA non-HOT", "2C99248AEE0B", RouterType.MOSDI));
    }

    public static Router getRouterBySSID(String ssid) {
        if (ssid == null) return null;
        return ROUTERS.get(ssid);
    }

    public static Router getRouterByMAC(String macAddress) {
        if (macAddress == null) return null;
        for (Router router : ROUTERS.values()) {
            if (router.getMacAddress().equals(macAddress)) {
                return router;
            }
        }
        return null;
    }

    public static boolean isRouterType(String ssid, RouterType type) {
        Router router = getRouterBySSID(ssid);
        return router != null && router.getRouterType() == type;
    }

    public static void addRouter(Router router) {
        if (router != null && router.getSsid() != null) {
            ROUTERS.put(router.getSsid(), router);
        }
    }
}