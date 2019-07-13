package com.github.react.sextant;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.github.react.sextant.activity.MuPDFActivity;

import android.app.Activity;
import android.content.Intent;

public class RCTMuPdfModule extends ReactContextBaseJavaModule {
    private final  int REQUEST_ECODE_SCAN=1498710037;
    private static ReactApplicationContext mContext;

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

    @ReactMethod
    public void startPDFActivity(){
        Intent intent = new Intent();
        Activity currentActivity = getCurrentActivity();

        intent.setClass(currentActivity,MuPDFActivity.class);
        currentActivity.startActivityForResult(intent, REQUEST_ECODE_SCAN);
    }

    @Override
    public String getName() {
        return "RNMuPdfModule";
    }
}
