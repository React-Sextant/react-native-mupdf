package com.artifex.mupdfdemo;

import android.graphics.PointF;
import android.graphics.RectF;

enum Hit {Nothing, Widget, Annotation};

public interface MuPDFView {
	public void setPage(int page, PointF size);
	public void setScale(float scale);
	public int getPage();
	public void blank(int page);
	public Hit passClickEvent(float x, float y);
	public LinkInfo hitLink(float x, float y);
	public void selectText(float x0, float y0, float x1, float y1);
	public void deselectText();
	public boolean copySelection();
	public StringBuilder getSelectedString();
	public boolean markupSelection(Annotation.Type type);
	public boolean markupSelection(int page, PointF[] quadPoints, Annotation.Type type);
	public void deleteSelectedAnnotation();
	public void deleteSelectedAnnotation(int page, int index);
	public void setSearchBoxes(RectF searchBoxes[]);
	public void setLinkHighlighting(boolean f);
	public void deselectAnnotation();
	public void startDraw(float x, float y);
	public void continueDraw(float x, float y);
	public void cancelDraw();
	public boolean saveDraw();
	public boolean saveDraw(int page, PointF[][] arcs);
	public void setChangeReporter(Runnable reporter);
	public void update();
	public void updateHq(boolean update);
	public void removeHq();
	public void releaseResources();
	public void releaseBitmaps();

	public int getTop();
	public int getLeft();
}
