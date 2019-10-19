package com.artifex.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {
    private static SharedPreferences sharedPreferences;

    /**
     * 初始化 在application中
     *
     * @param application
     */
    public static void init(Application application) {

        sharedPreferences = application.getSharedPreferences("LocalStorage", Context.MODE_PRIVATE);
    }

    /**
     * 插入
     *
     * @param key
     * @param value
     */
    public static void put(String key, Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (value.getClass() == Boolean.class) {
            editor.putBoolean(key, (Boolean) value);
        }
        if (value.getClass() == String.class) {
            editor.putString(key, (String) value);
        }
        if (value.getClass() == Integer.class) {
            editor.putInt(key, ((Integer) value).intValue());
        }
        editor.commit();
    }

    /**
     * 获取 已阅状态
     *
     * 0 未读
     * 1 已读
     * **/
    public static int getYIYUEState(){
        int state;
        state = sharedPreferences.getInt("YIYUE_STATE",0);
        return state;
    }

    /**
     * 存储页码
     * **/
    public static int CURRENT_PAGE = -1;
}
