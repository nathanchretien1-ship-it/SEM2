package com.ihealth.demo.base;

import android.app.Application;
import android.util.Log;

import com.ihealth.communication.manager.iHealthDevicesManager;

import java.io.IOException;
import java.io.InputStream;

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        /*
         * Initializes the iHealth devices manager. Can discovery available iHealth devices nearby
         * and connect these devices through iHealthDevicesManager.
         */
        iHealthDevicesManager.getInstance().init(this, Log.VERBOSE, Log.VERBOSE);


        /*
         * Authenticate with iHealth servers to unlock iHealth SDK for sensors
         */
        try {
            InputStream is = getAssets().open("com_demo_sdk_android.pem");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            boolean isPass = iHealthDevicesManager.getInstance().sdkAuthWithLicense(buffer);
            Log.d("SEM2", "isPass: " + isPass);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
