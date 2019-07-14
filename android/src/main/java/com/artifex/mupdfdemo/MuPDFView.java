package com.artifex.mupdfdemo;

import android.graphics.PointF;
import android.graphics.RectF;

public interface MuPDFView {
	void setPage(int page, PointF size);
	void setScale(float scale);
	int getPage();
	void blank(int page);
	Hit passClickEvent(float x, float y);
	LinkInfo hitLink(float x, float y);
	void selectText(float x0, float y0, float x1, float y1);
	void deselectText();
	boolean copySelection();
	/**
	 * 文字选中后根据类型 高亮、下划线、删除线进行处理标注
	 * @param type
	 * @return
	 */
	boolean markupSelection(Annotation.Type type);
	void deleteSelectedAnnotation();
	void setSearchBoxes(RectF searchBoxes[]);
	void setLinkHighlighting(boolean f);
	void deselectAnnotation();
	void startDraw(float x, float y);
	void continueDraw(float x, float y);
	void cancelDraw();
	boolean saveDraw();
	void setChangeReporter(Runnable reporter);
	void update();
	void updateHq(boolean update);
	void removeHq();
	void releaseResources();
	void releaseBitmaps();
	/**
	 * 设置超链接颜色
	 * @param color 颜色值
	 */
	void setLinkHighlightColor(int color);
	/**
	 * 设置画笔颜色
	 * @param color 颜色值
	 */
	void setInkColor(int color);
	/**
	 * 设置画笔粗细
	 * @param inkThickness 粗细值
	 */
	void setPaintStrockWidth(float inkThickness);

	float getCurrentScale();
}
