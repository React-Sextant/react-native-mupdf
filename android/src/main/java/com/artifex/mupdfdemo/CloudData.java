package com.artifex.mupdfdemo;

import android.content.Context;

import com.github.react.sextant.RCTMuPdfModule;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 云数据 - 用于与服务端序列化数据
 * **/
public class CloudData {

    private static CloudData sCloudData;
    public ArrayList<HashMap> mFreetext;

    /**
     * 创建sCloudData单例
     * **/
    public static CloudData get(Context context){
        if(sCloudData == null){
            sCloudData = new CloudData();
            System.out.println("LUOKUN: new");
        }

        return sCloudData;
    }

    private CloudData(){
        mFreetext = new ArrayList<HashMap>();
    }

    public CloudData(ArrayList<HashMap> list){
        mFreetext = list;
    }

    public ArrayList<HashMap> getmFreetext() {
        return mFreetext;
    }

    public void add(HashMap map){
        mFreetext.add(map);
        RCTMuPdfModule.updateCloudData((int)map.get("page"), sCloudData);
    }

//    //文本批注{x,y,width,size,page,type:"textBox"}
//    public static ArrayList<HashMap> mFreetext = new ArrayList<HashMap>();
//
//    public static void addFreetext(HashMap map){
//        mFreetext.add(map);
//        RCTMuPdfModule.updateCloudData((int)map.get("page"));
//    }
}
