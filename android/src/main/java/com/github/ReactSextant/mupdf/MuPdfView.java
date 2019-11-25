package com.github.ReactSextant.mupdf;

import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import com.artifex.mupdfdemo.FilePicker;
import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFPageAdapter;
import com.artifex.mupdfdemo.MuPDFReaderView;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.ThemedReactContext;


public class MuPdfView extends RelativeLayout implements FilePicker.FilePickerSupport{
    private Context mContext;

    private final int    OUTLINE_REQUEST=0;
    private final int    PRINT_REQUEST=1;
    private final int    FILEPICK_REQUEST=2;

    private MuPDFCore muPDFCore;
    private MuPDFReaderView muPDFReaderView;

    private FilePicker mFilePicker;

    private String filePath = Environment.getExternalStorageDirectory() + "/Download/pdf_t2.pdf"; // 文件路径

    public MuPdfView(final Context ctx, AttributeSet atts) {
        super(ctx, atts);
        mContext = ctx;
//        drawPdf();
    }

    public void drawPdf() {
        muPDFCore = openFile(filePath);

        if (muPDFCore == null) {
            System.out.println("打开失败");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.youkan_view, this);
        muPDFReaderView = view.findViewById(R.id.mu_pdf_mupdfreaderview);
        muPDFReaderView.setAdapter(new MuPDFPageAdapter(mContext, this,muPDFCore));
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

    @Override
    public void performPickFor(FilePicker picker) {}
}
