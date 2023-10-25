package com.avatarmind.floatingclock;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.avatarmind.floatingclock.service.FloatingService;
import com.avatarmind.floatingclock.util.ClockInfo;
import com.avatarmind.floatingclock.util.Constant;
import com.avatarmind.floatingclock.util.LogUtil;
import com.avatarmind.floatingclock.util.NetworkTools;
import com.avatarmind.floatingclock.util.SharedPreferencesUtil;
import com.avatarmind.floatingclock.util.ToastUtil;
import com.avatarmind.floatingclock.util.Util;
import com.avatarmind.floatingclock.util.event.CurrentEvent;
import com.avatarmind.floatingclock.util.event.UpdateClockViewEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends Activity {

    private TextView mTimeFrom;
    final int REQUEST_CODE = 110001;
    Activity mActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivity = this;
        EventBus.getDefault().register(this);
        SharedPreferencesUtil.initSharedPreferences(MainActivity.this);

        findViewById(R.id.getPermission).setOnClickListener(v -> checkOverlayPermission());

        mTimeFrom = findViewById(R.id.timeFrom);

        Switch switchCloseClock = (Switch) findViewById(R.id.st_close_clock);
        switchCloseClock.setChecked(SharedPreferencesUtil.isExit(MainActivity.this));
        switchCloseClock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferencesUtil.setIsExit(MainActivity.this, isChecked);
            }
        });

        checkOverlayPermission();


    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            super.onSaveInstanceState(outState);
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(CurrentEvent event) {
        if (event != null && event.getCurrentTime() > 0) {
            mTimeFrom.setText("当前服务器开始时间：" + Util.getDate2String(event.getCurrentTime(), "yyyy-MM-dd HH:mm:ss SSS"));
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // 用户已经授权，进行弹出显示的操作
                ToastUtil.showToast(MainActivity.this, "授权成功");
                Util.startService(MainActivity.this, FloatingService.class);
            } else {
                // 用户未授权，提示用户开启权限
                ToastUtil.showToast(MainActivity.this, "授权失败");
            }
        }
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // 没有权限，进行下一步操作
            ToastUtil.showToast(MainActivity.this, "应用没有显示悬浮窗权限，请授权");
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())), REQUEST_CODE);
        } else {
            // 已经有权限，直接进行弹出显示的操作
            Util.startService(MainActivity.this, FloatingService.class);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        if (!SharedPreferencesUtil.isExit(MainActivity.this))
            Util.stopService(MainActivity.this, FloatingService.class);
    }
}
