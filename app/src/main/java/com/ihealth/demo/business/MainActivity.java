package com.ihealth.demo.business;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;

/**
 * MainActivity
 */
public class MainActivity extends AppCompatActivity {

    private final static String TAG = "SEM2";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        /*
         * Initializes the iHealth devices manager. Can discovery available iHealth devices nearby
         * and connect these devices through iHealthDevicesManager.
         */
        iHealthDevicesManager.getInstance().init(getApplication(), Log.VERBOSE, Log.VERBOSE);

        iHealthDevicesCallback ihdc = new MyCallback();
        int callbackId = iHealthDevicesManager.getInstance().registerClientCallback(ihdc);


        iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.PO3);

        //iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.NT13B);
    }

    @Override
    protected void onPause() {
        super.onPause();

        iHealthDevicesManager.getInstance().disconnectAllDevices(true);
    }

    private static class MyCallback extends iHealthDevicesCallback {

        @Override
        public void onScanDevice(String mac, String deviceType, int rssi) {
            super.onScanDevice(mac, deviceType, rssi);

            Log.d(TAG, "onScan1 -> mac : " + mac + ", deviceType : " + deviceType + ", rssi : " + rssi);

            iHealthDevicesManager.getInstance().connectDevice("", mac, deviceType);
        }

        @Override
        public void onScanFinish() {
            super.onScanFinish();

            Log.d(TAG, "onScanFinish");
        }

        @Override
        public void onScanError(String reason, long latency) {
            super.onScanError(reason, latency);

            Log.d(TAG, "onScanError : " + reason);
        }

        @Override
        public void onDeviceConnectionStateChange(String mac, String deviceType, int status, int errorID) {
            super.onDeviceConnectionStateChange(mac, deviceType, status, errorID);

            switch (status) {
                case iHealthDevicesManager.DEVICE_STATE_CONNECTING:
                    Log.d(TAG, "onDeviceConnectionStateChange1 : CONNECTING");
                    break;
                case iHealthDevicesManager.DEVICE_STATE_CONNECTED:
                    Log.d(TAG, "onDeviceConnectionStateChange1 : CONNECTED");
                    switch (deviceType) {
                        case "PO3":
                            iHealthDevicesManager.getInstance().getPo3Control(mac).startMeasure();
                            break;
                        case "NT13B":
                            iHealthDevicesManager.getInstance().getNT13BControl(mac).getMeasurement();
                            break;

                    }
                    break;
                case iHealthDevicesManager.DEVICE_STATE_DISCONNECTED:
                    Log.d(TAG, "onDeviceConnectionStateChange1 : DISCONNECTED");
                    break;
                case iHealthDevicesManager.DEVICE_STATE_CONNECTIONFAIL:
                    Log.d(TAG, "onDeviceConnectionStateChange1 : CONNECTFAIL");
                    break;
                case iHealthDevicesManager.DEVICE_STATE_RECONNECTING:
                    Log.d(TAG, "onDeviceConnectionStateChange1 : RECONNECTING");
                    break;

            }
        }

        @Override
        public void onUserStatus(String username, int userStatus) {
            super.onUserStatus(username, userStatus);

            Log.d(TAG, "onUserStatus : username=" + username + ", userStatus=" + userStatus);
        }

        @Override
        public void onDeviceNotify(String mac, String deviceType, String action, String message) {
            super.onDeviceNotify(mac, deviceType, action, message);
            Log.d(TAG, "onDeviceNotify : action=" + action + ", message=" + message);
        }

    }
}
