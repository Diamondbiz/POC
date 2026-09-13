package AndroidTV.V3.Models;

import AndroidTV.V3.Models.RouterType;

public class Router {
    private String ssid;
    private String macAddress;
    private RouterType adbRouterType;
    private String validPhoneNumber;

    public Router(String ssid, String macAddress, RouterType adbRouterType) {
        this.ssid = ssid;
        this.macAddress = macAddress;
        this.adbRouterType = adbRouterType;
        this.validPhoneNumber = "";
    }

    public Router(String ssid, String macAddress, RouterType adbRouterType, String validPhoneNumber) {
        this.ssid = ssid;
        this.macAddress = macAddress;
        this.adbRouterType = adbRouterType;
        this.validPhoneNumber = validPhoneNumber;
    }

    public String getSsid() {
        return ssid;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public RouterType getRouterType() {
        return adbRouterType;
    }

    public String getValidPhoneNumber() {
        return validPhoneNumber;
    }
}