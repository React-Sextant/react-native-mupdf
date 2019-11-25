package com.github.ReactSextant.mupdf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.artifex.mupdfdemo.MuPDFCore;


public class MuPdfView extends View{

    private final String APP = "MuPDFView";

    private Context     mContext;
    private MuPDFCore   muPDFCore;
    private String filePath = Environment.getExternalStorageDirectory() + "/Download/pdf_t2.pdf"; // 文件路径

    public MuPdfView(final Context ctx, AttributeSet atts) {
        super(ctx, atts);
        mContext = ctx;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        openFile(filePath);

        if (muPDFCore == null) {
            Log.e(APP, "muPDFCore null");
            return;
        }

        @SuppressLint("StaticFieldLeak") AsyncTask<Void,Void, PointF> sizingTask = new AsyncTask<Void,Void,PointF>() {

            @Override
            protected PointF doInBackground(Void... voids) {
                return muPDFCore.getPageSize(0);
            }

            @Override
            protected void onPostExecute(PointF result) {
                super.onPostExecute(result);
                Log.e(APP, "getPageSize: "+result);
            }
        };

        sizingTask.execute((Void)null);
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
