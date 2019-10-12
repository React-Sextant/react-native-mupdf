package com.github.react.sextant;

import android.os.Environment;
import android.util.AttributeSet;

import com.artifex.mupdfdemo.FilePicker;
import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFPageAdapter;
import com.artifex.mupdfdemo.MuPDFReaderView;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.ThemedReactContext;


public class MuPdfView extends MuPDFReaderView{
    private ReactContext mContext;

    private final int    OUTLINE_REQUEST=0;
    private final int    PRINT_REQUEST=1;
    private final int    FILEPICK_REQUEST=2;

    private MuPDFCore muPDFCore;

    private FilePicker mFilePicker;

    private String filePath = Environment.getExternalStorageDirectory() + "/Download/pdf_t2.pdf"; // 文件路径

    public MuPdfView(ThemedReactContext ctx, AttributeSet atts) {
        super(ctx, atts);
        mContext = ctx;
    }

    public void drawPdf() {
        muPDFCore = openFile(filePath);

        if (muPDFCore == null) {
            System.out.println("打开失败");
            return;
        }

        this.setAdapter(new MuPDFPageAdapter(mContext, muPDFCore));
        this.resetupChildren();
        this.setDisplayedViewIndex(1);
    }


    /**
     * 打开文件
     * @param path 文件路径
     * @return
     */
    private MuPDFCore openFile(String path) {

        try {
            muPDFCore = new MuPDFCore(mContext, path);
        } catch (Exception e) {
            return null;
        } catch (OutOfMemoryError e) {
            return null;
        }
        return muPDFCore;
    }
}
