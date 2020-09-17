package com.artifex.utils;

import android.text.InputFilter;
import android.text.Spanned;

public class PageLimitFilter implements InputFilter {

    int maxPage;

    public PageLimitFilter(int maxPage){
        this.maxPage = maxPage;
    }

    /**
     * @param source 新输入的字符串
     * @param start  新输入的字符串起始下标，一般为0
     * @param end    新输入的字符串终点下标，一般为source长度-1
     * @param dest   输入之前文本框内容
     * @param dstart 新输入的字符在原字符串中的位置
     * @param dend   原内容终点坐标，
     * @return 输入内容
     */
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

        String sourceText = source.toString();
        String destText = dest.toString();



        if(dstart==0 && "0".equals(source)){
            //如果输入是0 且位置在第一位，取消输入
            return "";
        }

        StringBuilder totalText=new StringBuilder();
        totalText.append(destText.substring(0,dstart))
                .append(sourceText)
                .append(destText.substring(dstart,destText.length()));


        try {
            if (Integer.parseInt(totalText.toString()) > maxPage) {
                return "";
            }else if (Integer.parseInt(totalText.toString())==0){
                //如果输入是0，取消输入
                return "";
            }
        }
        catch (Exception e){
            return "";
        }

        if ("".equals(source.toString())){
            return "";
        }
        return ""+Integer.parseInt(source.toString());
    }
}
