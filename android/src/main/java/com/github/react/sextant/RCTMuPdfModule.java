package com.github.react.sextant;

import android.app.Activity;
import android.content.Intent;

import com.artifex.mupdfdemo.MuPDFActivity;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;


public class RCTMuPdfModule extends ReactContextBaseJavaModule {

    private final  int REQUEST_ECODE_SCAN=20191017;
    public static Promise mPromise;
    private static ReactApplicationContext mContext;

    public RCTMuPdfModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mContext = reactContext;

        mContext.addActivityEventListener(mActivityEventListener);
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

            if (requestCode == REQUEST_ECODE_SCAN && mPromise != null) {
                WritableMap map = Arguments.createMap();
                mPromise.resolve(map);
            }
        }
    };

    /**
     * 从JavaScript发送数据给Native
     * **/
    private static MyListener myListener;
    public static void setUpListener(MyListener Listener) { myListener = Listener; }
    @ReactMethod
    public void sendData(String str){
        myListener.onEvent(str);
    }

    /**
     * 发送事件给JavaScript
     * **/
    public static void sendEvent(String data){
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("MUPDF_Event_Manager",data);
    }

    @ReactMethod
    public void open(ReadableMap map, Promise promise){
        mPromise = promise;
        Activity currentActivity = getCurrentActivity();

        Intent intent = new Intent(currentActivity.getApplicationContext(), MuPDFActivity.class);

        //文件地址
        if(map.hasKey("filePath")){
            intent.putExtra("filePath", map.getString("filePath"));
        }

        //文件名称
        if(map.hasKey("fileName")){
            intent.putExtra("fileName", map.getString("fileName"));
        }

        //页码
        if(map.hasKey("page")){
            intent.putExtra("page", map.getInt("page"));
        }

        currentActivity.startActivityForResult(intent, REQUEST_ECODE_SCAN);
    }

    @Override
    public String getName() {
        return "MuPDF";
    }

}
