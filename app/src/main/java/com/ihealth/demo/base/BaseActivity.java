package com.ihealth.demo.base;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ihealth.demo.R;

/**
 * <li>BaseActivity</li>
 * <li>All Activity Basic</li>
 *
 * Created by wj on 2018/11/20
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    public Context mContext;
    private RelativeLayout mRlMain;
    /** Animation */
    private TranslateAnimation mShowAction;
    private TranslateAnimation mHiddenAction;
    /** Global Log Information */
    private final String mLogInformation = "";
    /** Device Name */
    public String mDeviceName = "";
    /** Device Mac */
    public String mDeviceMac = "";
    /** Global Screen Width Default 1080 px*/
    public int mScreenWidth = 1080;
    /** Global Screen Height Default 1920 px*/
    public int mScreenHeight = 1920;
    /** Handle Message What Code*/
    public static final int HANDLER_MESSAGE = 101;
    /** Importing Layout  Abstraction Method*/
    public abstract int contentViewID();
    /** Init  Abstraction Method*/
    public abstract void initView();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
        setBaseActivityLayout();
        initBaseActivity();

    }

    public void setBaseActivityLayout() {
        setContentView(R.layout.activity_base);
    }


    public void initBaseActivity() {
        mContext = BaseApplication.instance().getApplicationContext();
        mRlMain = findViewById(R.id.rlMain);

        if (contentViewID() != 0) {
            View layout = LayoutInflater.from(mContext).inflate(contentViewID(), null);
            layout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mRlMain.addView(layout);
        }

        WindowManager manager = this.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;

        initAnim();
        initView();

    }

    private void initAnim() {
        mShowAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -2.0f, Animation.RELATIVE_TO_SELF, -1.0f);
        mShowAction.setDuration(500);

        mHiddenAction = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF,
                -2.0f);
        mHiddenAction.setDuration(500);
    }


}
