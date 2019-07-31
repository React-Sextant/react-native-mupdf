package com.github.react.sextant;

import com.artifex.mupdfdemo.Annotation;
import com.artifex.mupdfdemo.MuPDFReaderView;
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

import com.github.react.sextant.activity.MuPDFActivity;

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
                map.putString("OpenMode",OpenMode);
                map.putInt("Page",MuPDFReaderView.mPageCore);
                mPromise.resolve(map);
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
    public void startPDFActivity(ReadableMap options,Promise promise){

        Activity currentActivity = getCurrentActivity();
        Intent intent = new Intent(currentActivity.getApplicationContext(), MuPDFActivity.class);
        if (options.hasKey("OpenMode")) {
            intent.putExtra("OpenMode", options.getString("OpenMode"));
            OpenMode = options.getString("OpenMode");
        }
        if(options.hasKey("Uri")){
            intent.putExtra("Uri", options.getString("Uri"));
        }
        if(options.hasKey("Page")){
            intent.putExtra("Page", options.getInt("Page"));
        }
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

    /**
     * 从JavaScript发送数据给Native
     * **/
    private static MyListener myListener;
    public static void setUpListener(MyListener Listener) { myListener = Listener; }
    @ReactMethod
    public void sendData(String str){
        if(!OpenMode.equals("主控方")){
            myListener.onEvent(str);
        }
    }

    /**
     * 将保存当前批注事件传递给JavaScript
     * **/
    public static void sendInkAnnotationEvent(int page, PointF[][] arcs,float color[], float inkThickness){

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
                .emit("MUPDF_Event_Manager",
                    "{" +
                                "\"type\":\"add_annotation\", " +
                                "\"path\":["+path+"],"+
                                "\"page\":"+page+
                         "}"
                );
    }

    /**
     * 将保存下划线/高亮事件传递给JavaScript
     * **/
    public static void sendMarkupAnnotationEvent(int page, PointF[] quadPoints, Annotation.Type type){
        String path = "";
        for(int i=0;i<quadPoints.length;i++){
            if(path.equals("")){
                path+="["+quadPoints[i].x+","+quadPoints[i].y+"]";
            }else {
                path+=",["+quadPoints[i].x+","+quadPoints[i].y+"]";
            }
        }
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Event_Manager",
                    "{" +
                                "\"type\":\"add_markup_annotation\", " +
                                "\"path\":["+path+"],"+
                                "\"page\":"+page + "," +
                                "\"annotation_type\": \"" + type + "\"" +
                          "}"
                );
    }

    /**
     * 将当前页面改变事件发送给JavaScript
     * **/
    public static void sendPageChangeEvent(int page){
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Event_Manager",
                    "{" +
                                "\"type\":\"update_page\", " +
                                "\"page\":"+page+
                         "}"
                );
    }

    /**
     * 将删除批注事件发送给Javascript
     * **/
    public static void sendDeleteSelectedAnnotationEvent(int page, int annot_index){
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Event_Manager",
                    "{" +
                                "\"type\":\"delete_annotation\", " +
                                "\"page\":"+page + "," +
                                "\"annot_index\":"+annot_index +
                         "}"
                );
    }

    @Override
    public String getName() {
        return "RNMuPdfModule";
    }
}
