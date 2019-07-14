package com.artifex.mupdfdemo;

/**
 * @Description: 大纲目录数据
 * @author: ZhangYW
 * @time: 2019/1/18 10:10
 */
public class OutlineActivityData {
    public OutlineItem items[];
    public int position;
    static private OutlineActivityData singleton;

    static public void set(OutlineActivityData d) {
        singleton = d;
    }

    static public OutlineActivityData get() {
        if (singleton == null)
            singleton = new OutlineActivityData();
        return singleton;
    }
}
