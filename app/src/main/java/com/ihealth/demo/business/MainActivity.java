package com.ihealth.demo.business;

import android.Manifest;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.ihealth.demo.R;
import com.ihealth.demo.base.BaseApplication;
/**
 * MainActivity
 * Containers for all fragment
 */
public class MainActivity extends AppCompatActivity {
    FrameLayout mFlContent;

    public int contentViewID() {
        return R.layout.activity_main;
    }

    public void initView() {
        init();
    }

    private void init() {
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        BaseApplication.instance().logOut();
    }
}
