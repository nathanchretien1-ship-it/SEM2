package com.ihealth.demo.base;

import android.app.Application;
import android.util.Log;

import com.ihealth.communication.manager.iHealthDevicesManager;

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
    }

}
