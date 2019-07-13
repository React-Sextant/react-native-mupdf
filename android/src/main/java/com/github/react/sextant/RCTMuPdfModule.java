package com.github.react.sextant;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.github.react.sextant.activity.MuPDFActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

public class RCTMuPdfModule extends ReactContextBaseJavaModule {
    private final  int REQUEST_ECODE_SCAN=1498710037;
    private static ReactApplicationContext mContext;

    Intent intent = new Intent();

    public RCTMuPdfModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mContext = reactContext;

        mContext.addActivityEventListener(mActivityEventListener);
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

            if (requestCode == REQUEST_ECODE_SCAN) {

            }
        }
    };

    /**
     * 获得当前top(0)的activity名称
     *
     * @return String["MainActivity","MuPDFActivity"]
     * **/
    private static String getRunningActivityName(){
        ActivityManager activityManager=(ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        String runningActivity=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
        return runningActivity;
    }

    @ReactMethod
    public void startPDFActivity(){

        Activity currentActivity = getCurrentActivity();

        intent.setClass(currentActivity,MuPDFActivity.class);
        currentActivity.startActivityForResult(intent, REQUEST_ECODE_SCAN);
    }

    @ReactMethod
    public void finishPDFActivity(){

        if(!getRunningActivityName().contains("MainActivity")){
            Activity currentActivity = getCurrentActivity();
            currentActivity.finish();
        }

    }

    private static MyListener  myListener;
    public static void setUpListener(MyListener Listener) {
        myListener = Listener;
    }
    @ReactMethod
    public void sendData(){
        myListener.onEvent("asd");

    }

    @Override
    public String getName() {
        return "RNMuPdfModule";
    }
}
