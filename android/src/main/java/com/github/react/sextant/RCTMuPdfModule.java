package com.github.react.sextant;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.github.react.sextant.activity.MuPDFActivity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 将保存当前批注事件传递给JavaScript
     * **/
    public static void sendInkAnnotationEvent(PointF[][] arcs,float color[], float inkThickness){

        String path = "";
        for(int i=0;i<arcs.length;i++){
            String xy = "";
            for(int j=0;j<arcs[i].length;j++){
                if(xy.equals("")){
                    xy+="["+arcs[i][j].x+","+arcs[i][j].y+"]";
                }else {
                    xy+=",["+arcs[i][j].x+","+arcs[i][j].y+"]";
                }
            }
            if(path.equals("")){
                path+="["+xy+"]";
            }else {
                path+=",["+xy+"]";
            }
        }


        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Ink_Annotation", "["+path+"]");
    }

    @Override
    public String getName() {
        return "RNMuPdfModule";
    }
}
