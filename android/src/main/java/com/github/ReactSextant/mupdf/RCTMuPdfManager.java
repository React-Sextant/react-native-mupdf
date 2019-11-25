package com.github.ReactSextant.mupdf;

import android.content.Context;
import android.view.View;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.ReactRootView;

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
        return new MuPdfView(context, null);
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
