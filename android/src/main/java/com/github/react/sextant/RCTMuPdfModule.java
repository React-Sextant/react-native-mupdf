package com.github.react.sextant;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;

import com.artifex.mupdfdemo.Annotation;
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

    private final int REQUEST_ECODE_SCAN=20191017;
    public static Promise mPromise;
    public static boolean error;    //打开文件是否报错
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
                if(error){
                    mPromise.reject("文件打开失败");
                }else {
                    WritableMap map = Arguments.createMap();
                    mPromise.resolve(map);
                }
            }
        }
    };

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

        //模式："主控方"
        if(map.hasKey("mode")){
            intent.putExtra("mode", map.getString("mode"));
        }

        currentActivity.startActivityForResult(intent, REQUEST_ECODE_SCAN);
    }

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

    /**
     * 主动关闭当前页面并返回RN页面
     * **/
    @ReactMethod
    public void finishPDFActivity(){
        Activity currentActivity = getCurrentActivity();
        currentActivity.finish();
    }

    @Override
    public String getName() {
        return "MuPDF";
    }

}