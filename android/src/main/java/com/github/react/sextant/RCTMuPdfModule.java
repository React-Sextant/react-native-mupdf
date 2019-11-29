package com.github.react.sextant;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;

import com.artifex.mupdfdemo.Annotation;
import com.artifex.mupdfdemo.CloudData;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;

import static com.artifex.utils.SharedPreferencesUtil.CURRENT_PAGE;

public class RCTMuPdfModule extends ReactContextBaseJavaModule {

    private final int REQUEST_ECODE_SCAN=20191017;
    private Promise mPromise;
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
                    map.putString("cloudData",sendCloudDataString());
                    mPromise.resolve(map);
                }
            }
        }
    };

    @ReactMethod
    public void open(ReadableMap map, Promise promise){
        CURRENT_PAGE = -1;
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

        //cloudData
        if(map.hasKey("cloudData")){
            parseCloudDataString(map.getString("cloudData"));
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
     * 关闭页面时将CloudData数据传给promise
     *
     * int page, float size, float x, float y, float width, float height, String text
     * **/
    public String sendCloudDataString(){
        try {
            String cloudData = "";
            if (CloudData.mFreetext.size()>0) {
                for (int i = 0; i < CloudData.mFreetext.size(); i++) {
                    HashMap map = CloudData.mFreetext.get(i);
                    if(cloudData.equals("")){
                        cloudData+= "{\"page\":"+map.get("page")+",\"size\":"+map.get("size")+",\"x\":"+map.get("x")+",\"y\":"+map.get("y")+",\"width\":"+map.get("width")+",\"height\":"+map.get("height")+",\"text\":\""+map.get("text")+"\"}";
                    }else {
                        cloudData+=",{\"page\":"+map.get("page")+",\"size\":"+map.get("size")+",\"x\":"+map.get("x")+",\"y\":"+map.get("y")+",\"width\":"+map.get("width")+",\"height\":"+map.get("height")+",\"text\":\""+map.get("text")+"\"}";
                    }
                }
                CloudData.mFreetext.clear();
                return "["+cloudData+"]";
            }else {
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 解析CloudData数据
     *
     * int page, float size, float x, float y, float width, float height, String text
     * **/
    public void parseCloudDataString(String str){
        try{
            JsonParser jsonParser = new JsonParser();
            JsonArray jsonArray = (JsonArray) jsonParser.parse(str);
            for(int i = 0;i< jsonArray.size(); i++){
                HashMap map = new HashMap();
                map.put("page",jsonArray.get(i).getAsJsonObject().get("page").getAsInt());
                map.put("size",jsonArray.get(i).getAsJsonObject().get("size").getAsFloat());
                map.put("x",jsonArray.get(i).getAsJsonObject().get("x").getAsFloat());
                map.put("y",jsonArray.get(i).getAsJsonObject().get("y").getAsFloat());
                map.put("width",jsonArray.get(i).getAsJsonObject().get("width").getAsFloat());
                map.put("height",jsonArray.get(i).getAsJsonObject().get("height").getAsFloat());
                map.put("text",jsonArray.get(i).getAsJsonObject().get("text").getAsString());
                CloudData.mFreetext.add(map);
            }
        }catch (Exception e){

        }
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
