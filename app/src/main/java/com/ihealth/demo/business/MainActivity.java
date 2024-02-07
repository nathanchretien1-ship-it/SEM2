package com.ihealth.demo.business;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ihealth.communication.manager.iHealthDevicesManager;
/**
 * MainActivity
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        /*
         * Initializes the iHealth devices manager. Can discovery available iHealth devices nearby
         * and connect these devices through iHealthDevicesManager.
         */
        iHealthDevicesManager.getInstance().init(getApplication(), Log.VERBOSE, Log.VERBOSE);

    }
}
