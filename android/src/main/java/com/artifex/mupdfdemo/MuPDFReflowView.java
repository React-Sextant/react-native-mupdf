package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;

public class MuPDFReflowView extends WebView implements MuPDFView {
	private final MuPDFCore mCore;
	private final Handler mHandler;
	private final Point mParentSize;
	private int mPage;
	private float mScale;
	private int mContentHeight;
	AsyncTask<Void,Void,byte[]> mLoadHTML;

	public MuPDFReflowView(Context c, MuPDFCore core, Point parentSize) {
		super(c);
		mHandler = new Handler();
		mCore = core;
		mParentSize = parentSize;
		mScale = 1.0f;
		mContentHeight = parentSize.y;
		getSettings().setJavaScriptEnabled(true);
		addJavascriptInterface(new Object(){
			public void reportContentHeight(String value) {
				mContentHeight = (int)Float.parseFloat(value);
				mHandler.post(new Runnable() {
					public void run() {
						requestLayout();
					}
				});
			}
		}, "HTMLOUT");
		setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				setScale(mScale);
			}
		});
	}

	private void requestHeight() {
		// Get the webview to report the content height via the interface setup
		// above. Workaround for getContentHeight not working
		loadUrl("javascript:elem=document.getElementById('content');window.HTMLOUT.reportContentHeight("+mParentSize.x+"*elem.offsetHeight/elem.offsetWidth)");
	}

	@Override
	public Annotation.Type getAnnotationType() {
		return null;
	}

	@Override
	public View getCustomerView() {
		return null;
	}

	public void setPage(int page, PointF size) {
		mPage = page;
		if (mLoadHTML != null) {
			mLoadHTML.cancel(true);
		}
		mLoadHTML = new AsyncTask<Void,Void,byte[]>() {
			@Override
			protected byte[] doInBackground(Void... params) {
				return mCore.html(mPage);
			}
			@Override
			protected void onPostExecute(byte[] result) {
				String b64 = Base64.encodeToString(result, Base64.DEFAULT);
				loadData(b64, "text/html; charset=utf-8", "base64");
			}
		};
		mLoadHTML.execute();
	}

	public int getPage() {
		return mPage;
	}

	public void setScale(float scale) {
		mScale = scale;
		loadUrl("javascript:document.getElementById('content').style.zoom=\""+(int)(mScale*100)+"%\"");
		requestHeight();
	}

	public void blank(int page) {
	}

	public Hit passClickEvent(float x, float y) {
		return Hit.Nothing;
	}

	public LinkInfo hitLink(float x, float y) {
		return null;
	}

	public void selectText(float x0, float y0, float x1, float y1) {
	}

	public void deselectText() {
	}

	public boolean copySelection() {
		return false;
	}

	public StringBuilder getSelectedString() {
		return null;
	}

	public boolean markupSelection(Annotation.Type type) {
		return false;
	}
	public boolean markupSelection(int page, PointF[] quadPoints, Annotation.Type type){
		return false;
	}

	public void startDraw(float x, float y) {
	}

	public void continueDraw(float x, float y) {
	}

	@Override
	public void continueDrawLine(float x, float y) {
	}

	@Override
	public void continueDrawArrow(float x, float y) {
	}

	@Override
	public void continueDrawRect(float x, float y) {
	}

	@Override
	public void continueDrawCircle(float x, float y) {
	}

	@Override
	public void stopDraw() {
	}

	@Override
	public void undoDraw() {
	}

	public void cancelDraw() {
	}

	public boolean saveDraw() {
		return false;
	}
	public boolean saveDraw(int page, PointF[][] arcs) {
		return false;
	}

	public void setDraw(PointF[][] path) {
	}

	public void setSearchBoxes(RectF[] searchBoxes) {
	}

	public void setLinkHighlighting(boolean f) {
	}

	public void deleteSelectedAnnotation() {
	}
	public void deleteSelectedAnnotation(int page, int index) {
	}

	public void deselectAnnotation() {
	}

	public void setChangeReporter(Runnable reporter) {
	}

	public void update() {
	}

	public void updateHq(boolean update) {
	}

	public void removeHq() {
	}

	public void releaseResources() {
		if (mLoadHTML != null) {
			mLoadHTML.cancel(true);
			mLoadHTML = null;
		}
	}

	public void releaseBitmaps() {
	}

	@Override
	public void addFreetextAnnotation(float x, float y, float width, float height, String text) {

	}

	@Override
	public void addFreetextAnnotation(HashMap map) {

	}

	@Override
	public int getFreetextIndex() {
		return -1;
	}

	@Override
	public int getSelectedAnnotationIndex() {
		return -1;
	}

	@Override
	public void addRemark(int index) {
	}

	@Override
	public String getScreenShot() {
		return null;
	}

	@Override
	public Bitmap getBitmap() {
		return null;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int x, y;
		switch(MeasureSpec.getMode(widthMeasureSpec)) {
		case MeasureSpec.UNSPECIFIED:
			x = mParentSize.x;
			break;
		default:
			x = MeasureSpec.getSize(widthMeasureSpec);
		}
		switch(MeasureSpec.getMode(heightMeasureSpec)) {
		case MeasureSpec.UNSPECIFIED:
			y = mContentHeight;
			break;
		default:
			y = MeasureSpec.getSize(heightMeasureSpec);
		}

		setMeasuredDimension(x, y);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		return false;
	}
}
