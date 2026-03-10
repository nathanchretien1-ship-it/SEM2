package com.ihealth.demo.data;

import android.app.Application;
import android.util.Log;

import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

import java.io.IOException;
import java.io.InputStream;

public class IHealthService {
    private static final String TAG = "IHealthService";
    private static IHealthService instance;
    private final Application application;
    private boolean isInitialized = false;

    private IHealthService(Application application) {
        this.application = application;
    }

    public static synchronized IHealthService getInstance(Application application) {
        if (instance == null) {
            instance = new IHealthService(application);
        }
        return instance;
    }

    public int init(iHealthDevicesCallback callback) {
        if (!isInitialized) {
            iHealthDevicesManager.getInstance().init(application, Log.VERBOSE, Log.VERBOSE);
            authenticate();
            isInitialized = true;
        }
        return iHealthDevicesManager.getInstance().registerClientCallback(callback);
    }

    public void unRegisterClientCallback(int callbackId) {
        iHealthDevicesManager.getInstance().unRegisterClientCallback(callbackId);
    }

    private void authenticate() {
        try {
            InputStream is = application.getAssets().open("com_demo_sdk_android.pem");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            boolean isPass = iHealthDevicesManager.getInstance().sdkAuthWithLicense(buffer);
            Log.d(TAG, "SDK Auth: " + isPass);
        } catch (IOException e) {
            Log.e(TAG, "Auth error", e);
        }
    }

    public void startDiscovery(DiscoveryTypeEnum type) {
        iHealthDevicesManager.getInstance().startDiscovery(type);
    }

    public void stopDiscovery() {
        iHealthDevicesManager.getInstance().stopDiscovery();
    }

    public void connectDevice(String mac, String deviceType) {
        iHealthDevicesManager.getInstance().connectDevice("", mac, deviceType);
    }

    public void disconnectDevice(String mac, String deviceType) {
        iHealthDevicesManager.getInstance().disconnectDevice(mac, deviceType);
    }

    public void startMeasure(String mac, String deviceType) {
        if ("PO3".equals(deviceType)) {
            iHealthDevicesManager.getInstance().getPo3Control(mac).startMeasure();
        } else if ("NT13B".equals(deviceType)) {
            iHealthDevicesManager.getInstance().getNT13BControl(mac).getMeasurement();
        }
    }
}
