package com.artifex.mupdfdemo;

import android.graphics.RectF;

/**
 * @Description: 搜索结果
 * @author: ZhangYW
 * @time: 2019/1/18 10:26
 */
public class SearchTaskResult {
    public final String txt;
    public final int pageNumber;
    public final RectF searchBoxes[];
    static private SearchTaskResult singleton;

    SearchTaskResult(String _txt, int _pageNumber, RectF _searchBoxes[]) {
        txt = _txt;
        pageNumber = _pageNumber;
        searchBoxes = _searchBoxes;
    }

    static public SearchTaskResult get() {
        return singleton;
    }

    static public void set(SearchTaskResult r) {
        singleton = r;
    }
}
