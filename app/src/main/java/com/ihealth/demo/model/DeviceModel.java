package com.ihealth.demo.model;

public class DeviceModel {
    private String mac;
    private String name;
    private String type;
    private String status; // Connecting, Connected, Disconnected
    private String lastData;

    public DeviceModel(String mac, String name, String type) {
        this.mac = mac;
        this.name = name;
        this.type = type;
        this.status = "Disconnected";
        this.lastData = "";
    }

    public String getMac() {
        return mac;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastData() {
        return lastData;
    }

    public void setLastData(String lastData) {
        this.lastData = lastData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceModel that = (DeviceModel) o;
        return mac.equals(that.mac);
    }

    @Override
    public int hashCode() {
        return mac.hashCode();
    }
}
