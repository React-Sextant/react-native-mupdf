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
    private ArrayList<HashMap> mFreetext;

    /**
     * 创建sCloudData单例
     * **/
    public static CloudData get(){
        if(sCloudData == null){
            sCloudData = new CloudData();
        }

        return sCloudData;
    }

    private CloudData(){
        mFreetext = new ArrayList<HashMap>();
    }

    public void setmFreetext(ArrayList<HashMap> list){
        mFreetext = list;
    }

    public ArrayList<HashMap> getmFreetext() {
        return mFreetext;
    }

    public void clear(){
        mFreetext.clear();
    }

    public void add(HashMap map){
        mFreetext.add(map);
        RCTMuPdfModule.updateCloudData((int)map.get("page"), sCloudData);
    }

    public void remove(int i){
        RCTMuPdfModule.updateCloudData((int)mFreetext.remove(i).get("page"), sCloudData);
    }
}
