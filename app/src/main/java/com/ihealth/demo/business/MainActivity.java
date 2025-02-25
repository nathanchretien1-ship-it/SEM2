package com.ihealth.demo.business;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ihealth.communication.manager.DiscoveryTypeEnum;
import com.ihealth.communication.manager.iHealthDevicesCallback;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.demo.R;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.IOException;
import java.io.InputStream;

import io.reactivex.functions.Consumer;

/**
 * MainActivity
 */
public class MainActivity extends AppCompatActivity {

    private final static String TAG = "SEM2";
    private RxPermissions permissions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();
        findViewById(R.id.test_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initScanAndConnect();
            }
        });
    }

    private void initScanAndConnect() {
        if (init()) {

            iHealthDevicesCallback ihdc = new MyCallback();
            int callbackId = iHealthDevicesManager.getInstance().registerClientCallback(ihdc);


            iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.PO3);

            //iHealthDevicesManager.getInstance().startDiscovery(DiscoveryTypeEnum.NT13B);
        }

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

    private void checkPermission() {
        permissions = new RxPermissions(this);
        permissions.requestEach(Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) {
                        if (permission.granted) {

                        } else if (permission.shouldShowRequestPermissionRationale) {
                            Toast.makeText(MainActivity.this, "Veuillez activer les permissions correspondantes, sinon cela affectera l'utilisation des fonctionnalités.", Toast.LENGTH_SHORT);
                        } else {
                            Toast.makeText(MainActivity.this, "Veuillez activer les permissions correspondantes, sinon cela affectera l'utilisation des fonctionnalités.", Toast.LENGTH_SHORT);
                        }
                    }
                });
    }


    private boolean init() {
        boolean isPass = false;

        /*
         * Initializes the iHealth devices manager. Can discovery available iHealth devices nearby
         * and connect these devices through iHealthDevicesManager.
         */
        iHealthDevicesManager.getInstance().init(getApplication(), Log.VERBOSE, Log.VERBOSE);

        /*
         * Authenticate with iHealth servers to unlock iHealth SDK for sensors
         */
        try {
            InputStream is = getAssets().open("com_demo_sdk_android.pem");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            isPass = iHealthDevicesManager.getInstance().sdkAuthWithLicense(buffer);
            Log.d("SEM2", "isPass: " + isPass);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return isPass;
    }
}
