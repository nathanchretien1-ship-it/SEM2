package com.ihealth.demo.business;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.ec.easylibrary.utils.ToastUtils;
import com.ihealth.communication.manager.iHealthDevicesManager;
import com.ihealth.communication.utils.Log;
import com.ihealth.demo.R;
import com.ihealth.demo.base.BaseApplication;
import com.ihealth.demo.model.DeviceCharacteristic;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.lang.reflect.Field;
import java.util.ArrayList;

import butterknife.BindView;
import io.reactivex.functions.Consumer;

/**
 * MainActivity
 * Containers for all fragment
 */
public class MainActivity extends AppCompatActivity{
    @BindView(R.id.flContent)
    FrameLayout mFlContent;
    @BindView(R.id.tvTitle)
    TextView mTvTitle;

    private Context mContext;
    private RxPermissions permissions;

    //handler 中处理的四种状态
    public static final int HANDLER_SCAN = 101;
    public static final int HANDLER_CONNECTED = 102;
    public static final int HANDLER_DISCONNECT = 103;
    public static final int HANDLER_CONNECT_FAIL = 104;
    public static final int HANDLER_RECONNECT = 105;
    public static final int HANDLER_USER_STATUE = 106;

    public static final int FRAGMENT_CERTIFICATION = 0;
    public static final int FRAGMENT_CERTIFICATION_ERROR = 1;
    public static final int FRAGMENT_DEVICE_MAIN = 2;
    public static final int FRAGMENT_SCAN = 3;

    private int mCurrentFragment;

    private long mTimeKeyBackPressed = 0; // Back键按下时的系统时间

    //退出事件的超时时间
    //Setting this time can change the response time when you exit the application.
    private final static long TIMEOUT_EXIT = 2000;

    //Support device list
    public static ArrayList<DeviceCharacteristic> deviceStructList = new ArrayList<>();

    public int contentViewID() {
        return R.layout.activity_main;
    }

    public void initView() {
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        mContext = this;
        checkPermission();
        initDeviceInfo();
    }

    /**
     * 初始化所有支持设备信息
     * Initialize all support device information
     */
    private void initDeviceInfo() {
        Field[] fields = iHealthDevicesManager.class.getFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            if (fieldName.contains("DISCOVERY_")) {
                DeviceCharacteristic struct = new DeviceCharacteristic();
                struct.setDeviceName(fieldName.substring(10));
                try {
                    struct.setDeviceType(field.getLong(null));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                deviceStructList.add(struct);
            }
        }
    }

    /**
     * 检查权限
     * check Permission
     */
    private void checkPermission() {
        permissions = new RxPermissions(this);
        permissions.requestEach(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) {
                        if (permission.granted) {

                        } else if (permission.shouldShowRequestPermissionRationale) {
                            ToastUtils.showToast(MainActivity.this, "请打开相关权限，否则会影响功能的使用");
                        } else {
                            ToastUtils.showToast(MainActivity.this, "请打开相关权限，否则会影响功能的使用");
                        }
                    }
                });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        BaseApplication.instance().logOut();
    }
}
