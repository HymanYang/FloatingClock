package com.avatarmind.floatingclock.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextClock;
import android.widget.TextView;

import com.avatarmind.floatingclock.MainActivity;
import com.avatarmind.floatingclock.R;
import com.avatarmind.floatingclock.util.ClockInfo;
import com.avatarmind.floatingclock.util.Constant;
import com.avatarmind.floatingclock.util.LogUtil;
import com.avatarmind.floatingclock.util.NetworkTools;
import com.avatarmind.floatingclock.util.SharedPreferencesUtil;
import com.avatarmind.floatingclock.util.ToastUtil;
import com.avatarmind.floatingclock.util.Util;
import com.avatarmind.floatingclock.util.event.UpdateClockViewEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FloatingService extends Service implements Handler.Callback {
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private TextView mTextClock;
    private Handler handler;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uninit();
    }

    private void init() {
        EventBus.getDefault().register(this);
        handler = new Handler(this);
        ClockInfo clockInfo = SharedPreferencesUtil.getClockInfo(FloatingService.this);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.x = clockInfo.getX();
        layoutParams.y = clockInfo.getY();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            mTextClock = new TextView(getApplicationContext());
//            mTextClock.setFormat24Hour("HH:mm:ss");
            mTextClock.setTextSize(20);
            mTextClock.setGravity(Gravity.CENTER);
            mTextClock.setPadding(10, 15, 10, 15);
            mTextClock.setTextColor(getResources().getColor(R.color.blue_1B82FF));
            mTextClock.setBackgroundColor(getResources().getColor(R.color.colorWhite));
            mTextClock.setOnTouchListener(new FloatingOnTouchListener());

            loadServiceTime();

            windowManager.addView(mTextClock, layoutParams);
            windowManager.updateViewLayout(mTextClock.getRootView(), layoutParams);
        }

    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {

        if (msg.what == 1) {
            long currentTime = (long) msg.obj;
            long changeTime = 100;
            Message obmsg = new Message();
            obmsg.what = 1;
            obmsg.obj = currentTime + changeTime;
            handler.sendMessageDelayed(obmsg, changeTime);
            mTextClock.setText(Util.getDate2String(currentTime, "yyyy-MM-dd HH:mm:ss SSS"));
        }
        return false;
    }

    /**
     * 加载服务器时间
     */
    public void loadServiceTime() {
        NetworkTools.get(Constant.TB_TIME_API, new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                LogUtil.e(e.getMessage());
                ToastUtil.showToast(FloatingService.this, "接口解析数据失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    //{"api":"mtop.common.getTimestamp","v":"*","ret":["SUCCESS::接口调用成功"],"data":{"t":"1697790850658"}}
                    if (response != null) {
                        String resStr = response.body().string();
                        JSONObject obj = new JSONObject(resStr);
                        JSONObject data = obj.getJSONObject("data");
                        Long currentTime = data.getLong("t");
                        Message msg = new Message();
                        msg.what = 1;
                        msg.obj = currentTime;
                        handler.sendMessage(msg);
//                        FloatingService.this.runOnUiThread(() -> {
//                        mTextClock.setText(Util.getDate2String(currentTime, "yyyy-MM-dd HH:mm:ss SSS"));
//                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastUtil.showToast(FloatingService.this, "接口解析数据失败");
                }
            }
        });
    }


    private void uninit() {
        EventBus.getDefault().unregister(this);
        windowManager.removeView(mTextClock);
        if (handler != null) {
            handler.removeMessages(1);
        }
    }

    private class FloatingOnTouchListener implements View.OnTouchListener {
        private int x;
        private int y;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x = (int) event.getRawX();
                    y = (int) event.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    int nowX = (int) event.getRawX();
                    int nowY = (int) event.getRawY();
                    int movedX = nowX - x;
                    int movedY = nowY - y;
                    x = nowX;
                    y = nowY;
                    layoutParams.x = layoutParams.x + movedX;
                    layoutParams.y = layoutParams.y + movedY;
                    windowManager.updateViewLayout(view, layoutParams);

                    ClockInfo clockInfo = SharedPreferencesUtil.getClockInfo(FloatingService.this);
                    clockInfo.setX(layoutParams.x);
                    clockInfo.setY(layoutParams.y);
                    SharedPreferencesUtil.setClockInfo(FloatingService.this, clockInfo);
                    break;
                default:
                    break;
            }
            return false;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(UpdateClockViewEvent event) {
        if (event != null && mTextClock != null) {
            ClockInfo clockInfo = event.getClockInfo();
//            mTextClock.setTextSize(clockInfo.getTextSize());
            SharedPreferencesUtil.setClockInfo(FloatingService.this, clockInfo);
        }
    }
}
