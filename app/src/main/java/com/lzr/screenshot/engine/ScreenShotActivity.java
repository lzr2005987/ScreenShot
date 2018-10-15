package com.lzr.screenshot.engine;


import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

/**
 * 用于弹出权限申请的窗
 */
public class ScreenShotActivity extends Activity {

    public static final int REQUEST_MEDIA_PROJECTION = 0x2893;

    static {
        if (!OpenCVLoader.initDebug()) { // Handle initialization error }
        }
    }

    private Shotter shotter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setTheme(android.R.style.Theme_Dialog);//这个在这里设置 之后导致 的问题是 背景很黑
        super.onCreate(savedInstanceState);

        //如下代码 只是想 启动一个透明的Activity 而上一个activity又不被pause
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        getWindow().setDimAmount(0f);
        requestScreenShot();
    }


    public void requestScreenShot() {
        if (Build.VERSION.SDK_INT >= 21) {
            startActivityForResult(createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        } else {
            toast("版本过低,无法截屏");
        }
    }

    private Intent createScreenCaptureIntent() {
        //这里用media_projection代替Context.MEDIA_PROJECTION_SERVICE 是防止低于21 api编译不过
        return ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE)).createScreenCaptureIntent();
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_MEDIA_PROJECTION: {
                if (resultCode == RESULT_OK && data != null) {
                    startEngine(data);
                } else if (resultCode == RESULT_CANCELED) {
                    toast("权限被拒绝");
                }
            }
        }
    }

    private void startEngine(Intent data) {
        ExecuteRunnable mRunnable = new ExecuteRunnable(data);
        handler.postDelayed(mRunnable, 100);
    }

    private class ExecuteRunnable implements Runnable {
        Intent data;

        public ExecuteRunnable(Intent data) {
            this.data = data;
        }

        @Override
        public void run() {
            //防止shotter反复创建引起MediaProjection重复创建错误
            if (shotter == null) shotter = new Shotter(ScreenShotActivity.this, -1, data);
            shotter.startScreenShot(new Shotter.OnShotListener() {
                @Override
                public void onFinish() {
                    /**
                     * 如果这里没有时间间隔，系统可能会来不及判断截屏的权限有没有获取到，造成异常
                     */
                    handler.postDelayed(ExecuteRunnable.this, 10);
                    //finish(); // don't forget finish activity
                }
            });

        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private void toast(String str) {
        Toast.makeText(ScreenShotActivity.this, str, Toast.LENGTH_LONG).show();
    }

    /**
     * openCV初始化
     */
    @Override
    protected void onResume() {
        super.onResume();
        //load OpenCV engine and init OpenCV library
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, getApplicationContext(), mLoaderCallback);
    }

    //OpenCV库加载并初始化成功后的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            // TODO Auto-generated method stub
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
}