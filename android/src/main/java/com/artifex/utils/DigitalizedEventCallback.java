package com.artifex.utils;

import android.graphics.RectF;

/**
 * Created by @elage on 6/2/15.
 */
public interface DigitalizedEventCallback {

    public static final String ERROR_OUTSIDE_VERTICAL = "ERROR_OUTSIDE_VERTICAL";
    public static final String ERROR_OUTSIDE_HORIZONTAL = "ERROR_OUTSIDE_HORIZONTAL";

    public void longPressOnPdfPosition(int page, float viewX, float viewY, float pdfX, float pdfY);
    public void doubleTapOnPdfPosition(int page, float viewX, float viewY, float pdfX, float pdfY);
    public void singleTapOnPdfPosition(int page, float viewX, float viewY, float pdfX, float pdfY);
    public void error(String message);

    public void touchMoveOnPdfPosition(RectF rect, float scale);
    public void touchMoveForAnnotation();
    public void singleTapOnHit(RectF rect, float scale);
}
