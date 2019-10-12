package com.github.react.sextant;

import android.content.Context;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;

public class RCTMuPdfManager extends SimpleViewManager<MuPdfView> {
    private static final String REACT_CLASS = "RCTMuPdf";
    private Context context;
    private MuPdfView mupdfView;


    public RCTMuPdfManager(ReactApplicationContext reactContext){
        this.context = reactContext;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public MuPdfView createViewInstance(ThemedReactContext context) {
        this.mupdfView = new MuPdfView(context,null);
        return mupdfView;
    }

    @Override
    public void onDropViewInstance(MuPdfView mupdfView) {
        mupdfView = null;
    }

    @Override
    public void onAfterUpdateTransaction(MuPdfView mupdfView) {
        super.onAfterUpdateTransaction(mupdfView);
        mupdfView.drawPdf();
    }
}
