package com.artifex.mupdfdemo;

import android.content.Context;
import android.support.annotation.NonNull;

import com.github.react.sextant.RCTMuPdfModule;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 云数据 - 用于与服务端序列化数据
 * **/
public class CloudData {

    private static CloudData sCloudData;
    private ArrayList<HashMap> mFreetext;
    private ArrayList<RemarkItem> mRemarkList;

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
        mRemarkList = new ArrayList<RemarkItem>();
    }

    /************ FreeText ************/
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

    public HashMap get(int i){
        return mFreetext.get(i);
    }

    public void set(int i,HashMap map){
        mFreetext.set(i,map);
        RCTMuPdfModule.updateCloudData((int)map.get("page"), sCloudData);
    }

    /************ MarkList ************/

    public ArrayList<RemarkItem> getmRemarkList() {
        return mRemarkList;
    }
    public void add(RemarkItem item){
        mRemarkList.add(item);
    }


}

class RemarkItem {
    float x;
    float y;
    int page;
    int index;
    int size = 40;
    int width = 40;
    String text = "\uD83C\uDFF7️";

    @NonNull
    @Override
    public String toString() {
        return "{x:"+x+", y:"+y+", page:"+page+", index:"+index+"}";
    }
}