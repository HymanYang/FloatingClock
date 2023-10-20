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
import com.avatarmind.floatingclock.util.event.UpdateClockViewEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends Activity {
    private TextView mTVClockSize;
    private TextView mTimeFrom;
    private EditText mEtClockSize;
    final int REQUEST_CODE = 110001;
    Activity mActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivity = this;

        SharedPreferencesUtil.initSharedPreferences(MainActivity.this);
        ClockInfo clockInfo = SharedPreferencesUtil.getClockInfo(MainActivity.this);

        mTVClockSize = (TextView) findViewById(R.id.tv_clocksize);
        mTVClockSize.setText(getString(R.string.currentclocksize) + clockInfo.getTextSize());

        findViewById(R.id.getPermission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkOverlayPermission();
            }
        });

        mTimeFrom = findViewById(R.id.timeFrom);
        mTimeFrom.setText("当前服务器时间：");


        mEtClockSize = (EditText) findViewById(R.id.et_clocksize);
        mEtClockSize.setText(String.valueOf(clockInfo.getTextSize()));
        mEtClockSize.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                LogUtil.i("afterTextChanged()");

                try {
                    String text = mEtClockSize.getText().toString();
                    if (TextUtils.isEmpty(text)) {
                        return;
                    }

                    ClockInfo clockInfo = SharedPreferencesUtil.getClockInfo(MainActivity.this);

                    int size = Integer.parseInt(text);
                    if (size <= 0 || size > 100) {
                        ToastUtil.showToast(MainActivity.this, getString(R.string.clocksizeremind));
                        mEtClockSize.setText("");
                        return;
                    }

                    clockInfo.setTextSize(size);
                    EventBus.getDefault().post(new UpdateClockViewEvent(clockInfo));
                    mTVClockSize.setText(getString(R.string.currentclocksize) + clockInfo.getTextSize());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!SharedPreferencesUtil.isExit(MainActivity.this))
            Util.stopService(MainActivity.this, FloatingService.class);
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
}
