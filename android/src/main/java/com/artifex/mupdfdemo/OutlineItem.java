package com.artifex.mupdfdemo;

/**
 * @Description: 大纲目录bean
 * @author: ZhangYW
 * @time: 2019/1/18 10:10
 */
public class OutlineItem {
    public final int level;
    public final String title;
    public final int page;

    OutlineItem(int _level, String _title, int _page) {
        level = _level;
        title = _title;
        page = _page;
    }

}
