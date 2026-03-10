package com.ihealth.demo.ui;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.demo.data.IHealthService;
import com.ihealth.demo.model.DeviceModel;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG = "MainViewModel";
    private final IHealthService iHealthService;
    private final MutableLiveData<List<DeviceModel>> discoveredDevices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("Ready");
    private final int callbackId;

    public MainViewModel(@NonNull Application application) {
        super(application);
        iHealthService = IHealthService.getInstance(application);
        callbackId = iHealthService.init(sdkCallback);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        iHealthService.unRegisterClientCallback(callbackId);
    }

    public LiveData<List<DeviceModel>> getDiscoveredDevices() {
        return discoveredDevices;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public void startScan() {
        statusMessage.setValue("Scanning...");
        discoveredDevices.setValue(new ArrayList<>());
        iHealthService.startDiscovery(DiscoveryTypeEnum.PO3);
        // Add other types if needed
    }

    public void connect(DeviceModel device) {
        statusMessage.setValue("Connecting to " + device.getMac());
        iHealthService.connectDevice(device.getMac(), device.getType());
    }

    public void startMeasure(DeviceModel device) {
        iHealthService.startMeasure(device.getMac(), device.getType());
    }

    private final iHealthDevicesCallback sdkCallback = new iHealthDevicesCallback() {
        @Override
        public void onScanDevice(String mac, String deviceType, int rssi) {
            List<DeviceModel> current = discoveredDevices.getValue();
            DeviceModel device = new DeviceModel(mac, deviceType, deviceType);
            if (!current.contains(device)) {
                current.add(device);
                discoveredDevices.postValue(current);
            }
        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            String state;
            switch (status) {
                case iHealthDevicesManager.DEVICE_STATE_CONNECTED:
                    state = "CONNECTED";
                    iHealthService.startMeasure(mac, deviceType);
                    break;
                case iHealthDevicesManager.DEVICE_STATE_CONNECTING:
                    state = "CONNECTING";
                    break;
                case iHealthDevicesManager.DEVICE_STATE_DISCONNECTED:
                    state = "DISCONNECTED";
                    break;
                default:
                    state = "ERROR: " + errorID;
            }
            statusMessage.postValue("Device " + mac + " is " + state);

            // Update device status in list
            List<DeviceModel> current = discoveredDevices.getValue();
            for (DeviceModel d : current) {
                if (d.getMac().equals(mac)) {
                    d.setStatus(state);
                    break;
                }
            }
            discoveredDevices.postValue(current);
        }

        @Override
        public void onDeviceNotify(String mac, String deviceType, String action, String message) {
            Log.d(TAG, "onDeviceNotify: " + action + " -> " + message);
            statusMessage.postValue("Data: " + message);

            List<DeviceModel> current = discoveredDevices.getValue();
            for (DeviceModel d : current) {
                if (d.getMac().equals(mac)) {
                    d.setLastData(message);
                    break;
                }
            }
            discoveredDevices.postValue(current);
        }
    };
}
