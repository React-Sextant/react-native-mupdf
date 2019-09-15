package com.github.react.sextant;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import com.github.reader.app.ui.activity.ChoosePDFActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;

public class RCTMuPdfModule extends ReactContextBaseJavaModule {
    private final  int REQUEST_ECODE_SCAN=1498710037;
    public static Promise mPromise;
    private static ReactApplicationContext mContext;

    public static String OpenMode = "";

    public RCTMuPdfModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mContext = reactContext;

        mContext.addActivityEventListener(mActivityEventListener);
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

            if (requestCode == REQUEST_ECODE_SCAN) {
                WritableMap map = Arguments.createMap();
                mPromise.resolve(map);
            }
        }
    };

    /**
     * 获得当前top(0)的activity名称
     *
     * @return String["MainActivity","PdfActivity"]
     * **/
    private static String getRunningActivityName(){
        ActivityManager activityManager=(ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        String runningActivity=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
        return runningActivity;
    }

    @ReactMethod
    public void startPDFActivity(ReadableMap options,Promise promise){

        Activity currentActivity = getCurrentActivity();
        Intent intent = new Intent(currentActivity.getApplicationContext(), ChoosePDFActivity.class);
        currentActivity.startActivityForResult(intent, REQUEST_ECODE_SCAN);
        mPromise = promise;
    }


    /**
     * 主动关闭当前页面并返回RN页面
     * **/
    @ReactMethod
    public void finishPDFActivity(){

        if(!getRunningActivityName().contains("MainActivity")){
            Activity currentActivity = getCurrentActivity();
            currentActivity.finish();
        }

    }

    @Override
    public String getName() {
        return "RNMuPdfModule";
    }
}
