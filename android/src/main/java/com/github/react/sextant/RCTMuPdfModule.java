package com.github.react.sextant;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.artifex.mupdfdemo.Annotation;
import com.artifex.mupdfdemo.CloudData;
import com.artifex.mupdfdemo.MuPDFActivity;
import com.artifex.mupdfdemo.PageView;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;

import static com.artifex.utils.SharedPreferencesUtil.CURRENT_PAGE;
import static com.artifex.utils.WindowViewManager.createWindowView;

public class RCTMuPdfModule extends ReactContextBaseJavaModule {

    private final int REQUEST_ECODE_SCAN=30725;
    private final int SYSTEM_OVERLAY_WINDOW=30726;
    private Promise mPromise;
    private Callback mCallback;
    public static boolean error;    //打开文件是否报错
    public static boolean _isInMuPdf;    //是否已经进入
    private static ReactApplicationContext mContext;
    private CloudData mCloudData = CloudData.get();

    public RCTMuPdfModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.mContext = reactContext;

        mContext.addActivityEventListener(mActivityEventListener);
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode == REQUEST_ECODE_SCAN && mPromise != null) {
                _isInMuPdf=false;
                if(error){
                    mPromise.reject("文件打开失败");
                }else {
                    WritableMap map = Arguments.createMap();
                    map.putString("cloudData",stringCloudData(mCloudData));
                    mCloudData.clear();
                    mPromise.resolve(map);
                }
            }else if(requestCode == SYSTEM_OVERLAY_WINDOW){
                createWindowView(true); //resultCode=0
                mCallback.invoke();
            }
        }
    };

    @ReactMethod
    public void open(ReadableMap map, Promise promise){
        error = false;
        CURRENT_PAGE = -1;
        mPromise = promise;
        mCloudData.clear();
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

        //cloudData
        if(map.hasKey("cloudData")){
            mCloudData.setmFreetext(parseCloudData(map.getString("cloudData")));
        }

        //菜单
        if(map.hasKey("menus")){
            intent.putExtra("menus", map.getString("menus"));
        }

        // 主题颜色
        if(map.hasKey("theme")){
            intent.putExtra("theme", map.getString("theme"));
        }

        // 返回按钮
        if(map.hasKey("back")){
            intent.putExtra("back", map.getString("back"));
        }

        // 批注粗细
        if(map.hasKey("INK_THICKNESS")){
            PageView.INK_THICKNESS = (float)map.getInt("INK_THICKNESS");
        }

        // 悬浮窗
        if(map.hasKey("showWindowView")){
            createWindowView(map.getBoolean("showWindowView"));
        }else {
            createWindowView(false);
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
        if(_isInMuPdf && myListener != null){
            myListener.onEvent(str);
        }
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
     * 将批注添加备注事件发送给JavaScript
     * **/
    public static void sendRemarkEvent(int page, int annotationIndex, String base64){
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Event_Manager",
                        "{" +
                                "\"type\":\"add_remark_annotation\", " +
                                "\"page\":"+page + "," +
                                "\"index\":"+annotationIndex + "," +
                                "\"base64\": \""+base64 + "\"" +
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
     * 将当前所选批注索引发送给Javascript
     * **/
    public static void sendIndexSelectedAnnotationEvent(int page, int annot_index){
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Event_Manager",
                        "{" +
                                "\"type\":\"move_annotation\", " +
                                "\"page\":"+page + "," +
                                "\"annot_index\":"+annot_index +
                                "}"
                );
    }

    /**
     * 将动态菜单点击事件发送给Javascript
     * **/
    public static void sendDynamicMenusButtonEvent(String name, String item, String menus){
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Event_Manager",
                        "{" +
                                "\"type\":\"dynamic_menus_button\", " +
                                "\"name\":\""+name + "\", " +
                                "\"item\":  "+item + ", " +
                                "\"menus\":"+menus +
                                "}"
                );
    }

    /**
     * onFinishActivity hook
     * **/
    public static void sendFinishActivityEvent(){
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Event_Manager",
                        "{" +
                                "\"type\":\"on_finish_activity_hook\" " +
                                "}"
                );
    }

    /**
     * onLoadComplete
     * **/
    public static void sendLoadCompleteEvent(){
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Event_Manager",
                        "{" +
                                "\"type\":\"on_load_complete\" " +
                                "}"
                );
    }


    /**
     * 更新CloudData
     * **/
    public static void updateCloudData(int page, CloudData data){
        mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("MUPDF_Event_Manager",
                        "{" +
                                "\"type\":\"update_cloud_data\", " +
                                "\"page\":"+page + "," +
                                "\"data\":"+stringCloudData(data) +
                                "}"
                );
    }

    /**
     * 关闭页面时将CloudData数据传给promise
     *
     * int page, float size, float x, float y, float width, float height, String text
     * **/
    public static String stringCloudData(CloudData data){
        try {
            String cloudData = "";
            if (data.getmFreetext().size()>0) {
                for (int i = 0; i < data.getmFreetext().size(); i++) {
                    HashMap map = data.getmFreetext().get(i);
                    if(cloudData.equals("")){
                        cloudData+= "{\"page\":"+map.get("page")+",\"size\":"+map.get("size")+",\"x\":"+map.get("x")+",\"y\":"+map.get("y")+",\"width\":"+map.get("width")+",\"height\":"+map.get("height")+",\"text\":\""+map.get("text")+"\"}";
                    }else {
                        cloudData+=",{\"page\":"+map.get("page")+",\"size\":"+map.get("size")+",\"x\":"+map.get("x")+",\"y\":"+map.get("y")+",\"width\":"+map.get("width")+",\"height\":"+map.get("height")+",\"text\":\""+map.get("text")+"\"}";
                    }
                }
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
    public static ArrayList<HashMap> parseCloudData(String str){
        ArrayList<HashMap> cloudData = new ArrayList<HashMap>();
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
                cloudData.add(map);
            }
            return cloudData;
        }catch (Exception e){
            return cloudData;
        }
    }

    /**
     * 获取悬浮窗权限
     * **/
    @ReactMethod
    public void getWindowOverlayPermission(Callback callback){
        if (Build.VERSION.SDK_INT >= 23)
        {
            Activity currentActivity = getCurrentActivity();
            if (!Settings.canDrawOverlays(currentActivity))
            {
                callback.invoke(false);
            }
            else
            {
                callback.invoke(true);
                createWindowView(true);
            }
        }
        else callback.invoke(false);
    }
    @ReactMethod
    public void openWindowOverlayPermission(Callback callback){
        if (Build.VERSION.SDK_INT >= 23)
        {
            Activity currentActivity = getCurrentActivity();
            if (!Settings.canDrawOverlays(currentActivity))
            {
                String ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION";
                Intent intent = new Intent(ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + currentActivity.getPackageName()));
                currentActivity.startActivityForResult(intent, SYSTEM_OVERLAY_WINDOW);
                mCallback = callback;
            }
            else callback.invoke();
        }
        else callback.invoke();
    }

    @Override
    public String getName() {
        return "MuPDF";
    }

}
