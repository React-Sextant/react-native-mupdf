package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.artifex.utils.DigitalizedEventCallback;
import com.artifex.utils.PdfBitmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import com.github.react.sextant.R;

// Make our ImageViews opaque to optimize redraw
class OpaqueImageView extends ImageView {

    public OpaqueImageView(Context context) {
        super(context);
    }

    @Override
    public boolean isOpaque() {
        return true;
    }
}

interface TextProcessor {
    void onStartLine();

    void onWord(TextWord word);

    void onEndLine();
}

class TextSelector {
    final private TextWord[][] mText;
    final private RectF mSelectBox;

    public TextSelector(TextWord[][] text, RectF selectBox) {
        mText = text;
        mSelectBox = selectBox;
    }

    public void select(TextProcessor tp) {
        if (mText == null || mSelectBox == null)
            return;

        ArrayList<TextWord[]> lines = new ArrayList<TextWord[]>();
        for (TextWord[] line : mText)
            if (line[0].bottom > mSelectBox.top && line[0].top < mSelectBox.bottom)
                lines.add(line);

        Iterator<TextWord[]> it = lines.iterator();
        while (it.hasNext()) {
            TextWord[] line = it.next();
            boolean firstLine = line[0].top < mSelectBox.top;
            boolean lastLine = line[0].bottom > mSelectBox.bottom;
            float start = Float.NEGATIVE_INFINITY;
            float end = Float.POSITIVE_INFINITY;

            if (firstLine && lastLine) {
                start = Math.min(mSelectBox.left, mSelectBox.right);
                end = Math.max(mSelectBox.left, mSelectBox.right);
            } else if (firstLine) {
                start = mSelectBox.left;
            } else if (lastLine) {
                end = mSelectBox.right;
            }

            tp.onStartLine();

            for (TextWord word : line)
                if (word.right > start && word.left < end)
                    tp.onWord(word);

            tp.onEndLine();
        }
    }
}

public abstract class PageView extends ViewGroup {
    private static final int HIGHLIGHT_COLOR = 0x80ff5722;// 选中文字时的颜色
    private static final int LINK_COLOR = 0x80ff5722;// 超链接颜色
    private static final int BOX_COLOR = 0xFF696969;// 选中时边框的颜色
    private static final int INK_COLOR = 0xFFF00000;// 绘制时画笔颜色
    private static final int FREETEXT_COLOR = 0xFFFF0000;// 文本批注字体颜色
    public static float INK_THICKNESS = 4.0f;// 绘制时画笔宽
    private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
    private static final int PROGRESS_DIALOG_DELAY = 200;

    private float CURRENT_SCALE;

    private static final String TAG = "PageView";

    private static final int SIGN_HEIGHT = 50;
    private static final int SIGN_WIDTH = 100;

    protected final Context mContext;
    protected int mPageNumber;
    private Point mParentSize; // Size of the view containing the pdf viewer. It could be the same as the screen if this view is full screen.
    protected Point mSize;   // Size of page at minimum zoom
    protected float mSourceScale;

    private ImageView mEntire; // Image rendered at minimum zoom
    protected Bitmap mEntireBm; // Bitmap used to draw the entire page at minimum zoom.
    private Matrix mEntireMat;
    private AsyncTask<Void, Void, TextWord[][]> mGetText;
    private AsyncTask<Void, Void, LinkInfo[]> mGetLinkInfo;
    private CancellableAsyncTask<Void, Void> mDrawEntire;

    private Point mPatchViewSize; // View size on the basis of which the patch was created. After zoom.
    private Rect mPatchArea; // Area of the screen zoomed.
    private ImageView mPatch; // Image rendered at zoom resolution.
    private Bitmap mPatchBm; // Bitmap used to draw the zoomed image.
    private CancellableAsyncTask<Void, Void> mDrawPatch;
    private RectF mSearchBoxes[];
    protected LinkInfo mLinks[];
    protected RectF mSelectBox;
    private TextWord mText[][];
    protected RectF mItemSelectBox;
    protected Annotation.Type mAnnotationType;
    protected ArrayList<ArrayList<PointF>> mDrawing;
    private View mSearchView;
    public View mCustomerView;
    private boolean mIsBlank;
    private boolean mHighlightLinks;

    private ProgressBar mBusyIndicator;
    private final Handler mHandler = new Handler();

    private static boolean flagPositions = true; // Concurrency flag to avoid entering twice onDoubleTap method.
    private Bitmap signBitmap; // Bitmap for signature at higher resolution. // *BACKWARD COMPATIBILITY*
    private Point signBitmapSize; // Bitmap size, scaled to screen size and pdf.
    private static DigitalizedEventCallback eventCallback; // Callback for the app. The library fires an event when the user touched longPress or doubleTap, and the app can manage the behaviour.

    private Paint mBitmapPaint;
    private MuPDFPageAdapter mAdapter;

    private PointF pdfSize;
    private PdfBitmap picturePdfBitmap; // *BACKWARD COMPATIBILITY*

    private MuPDFCore core;
    public CloudData mCloudData;

    public PageView(Context c, Point parentSize, MuPDFPageAdapter adapter) {
        super(c);
        mContext = c;
        flagPositions = true;
        mParentSize = parentSize;
        setBackgroundColor(BACKGROUND_COLOR);
        mEntireMat = new Matrix();
        mAdapter = adapter;
        mCloudData = CloudData.get();
    }

    protected abstract CancellableTaskDefinition<Void, Void> getDrawPageTask(Bitmap bm, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight);

    protected abstract CancellableTaskDefinition<Void, Void> getUpdatePageTask(Bitmap bm, int sizeX, int sizeY, int patchX, int patchY, int patchWidth, int patchHeight);

    protected abstract LinkInfo[] getLinkInfo();

    protected abstract TextWord[][] getText();

    protected abstract void addMarkup(PointF[] quadPoints, Annotation.Type type);

    private void reinit() {
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancelAndWait();
            mDrawEntire = null;
        }

        if (mDrawPatch != null) {
            mDrawPatch.cancelAndWait();
            mDrawPatch = null;
        }

        if (mGetLinkInfo != null) {
            mGetLinkInfo.cancel(true);
            mGetLinkInfo = null;
        }

        if (mGetText != null) {
            mGetText.cancel(true);
            mGetText = null;
        }

        mIsBlank = true;
        mPageNumber = 0;

        if (mSize == null)
            mSize = mParentSize;

        if (mEntire != null) {
            mEntire.setImageBitmap(null);
            mEntire.invalidate();
        }

        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }

        mPatchViewSize = null;
        mPatchArea = null;

        mSearchBoxes = null;
        mLinks = null;
        mSelectBox = null;
        mText = null;
        mItemSelectBox = null;
    }

    public void releaseResources() {
        releaseBitmaps();

        reinit();

        if (mBusyIndicator != null) {
            removeView(mBusyIndicator);
            mBusyIndicator = null;
        }
    }

    public void releaseBitmaps() {
        if (mEntire != null) {
            mEntire.setImageBitmap(null);
            mEntire.invalidate();
        }

        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }

        Log.i(TAG, "Recycle mEntire on releaseBitmaps: " + mEntireBm);
        recycleBitmap(mEntireBm);
        mEntireBm = null;
        Log.i(TAG, "Recycle mPathBm on releaseBitmaps: " + mPatchBm);
        recycleBitmap(mPatchBm);
        mPatchBm = null;
    }

    public void blank(int page) {
        reinit();
        mPageNumber = page;

        if (mBusyIndicator == null) {
            mBusyIndicator = new ProgressBar(mContext);
            mBusyIndicator.setIndeterminate(true);
            mBusyIndicator.setBackgroundResource(R.drawable.busy);
            addView(mBusyIndicator);
        }

        setBackgroundColor(BACKGROUND_COLOR);
    }

    public void setPage(int page, PointF size) {
        pdfSize = correctBugMuPdf(size);

        if (mEntireBm == null) {
            try {
                mEntireBm = Bitmap.createBitmap(mParentSize.x, mParentSize.y, Config.ARGB_8888);
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
        }

        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancelAndWait();
            mDrawEntire = null;
        }

        mIsBlank = false;
        // Highlights may be missing because mIsBlank was true on last draw
        if (mSearchView != null)
            mSearchView.invalidate();

        mPageNumber = page;
        if (mEntire == null) {
            mEntire = new OpaqueImageView(mContext);
            mEntire.setScaleType(ImageView.ScaleType.MATRIX);
            addView(mEntire);
        }

        // Calculate scaled size that fits within the screen limits
        // This is the size at minimum zoom
        mSourceScale = Math.min(mParentSize.x / size.x, mParentSize.y / size.y);
        Point newSize = new Point((int) (size.x * mSourceScale), (int) (size.y * mSourceScale));
        mSize = newSize;

        mEntire.setImageBitmap(null);
        mEntire.invalidate();

        // Get the link info in the background
        mGetLinkInfo = new AsyncTask<Void, Void, LinkInfo[]>() {
            protected LinkInfo[] doInBackground(Void... v) {
                return getLinkInfo();
            }

            protected void onPostExecute(LinkInfo[] v) {
                mLinks = v;
                if (mSearchView != null)
                    mSearchView.invalidate();
            }
        };

        mGetLinkInfo.execute();

        updateEntireCanvas(false);

        if (mSearchView == null) {
            mSearchView = new View(mContext) {
                @Override
                protected void onDraw(final Canvas canvas) {
                    super.onDraw(canvas);
                    // Work out current total scale factor
                    // from source to view
                    final float scale = mSourceScale * (float) getWidth() / (float) mSize.x;

                    /**
                     * 存储当前scale值
                     * **/
                    CURRENT_SCALE = scale;

                    final Paint paint = new Paint();
                    final RectF mSelectFirstRect = new RectF();  //选择文本时的第一个字节节点

                    if (!mIsBlank && mSearchBoxes != null) {
                        paint.setColor(HIGHLIGHT_COLOR);
                        for (RectF rect : mSearchBoxes)
                            canvas.drawRect(rect.left * scale, rect.top * scale,
                                    rect.right * scale, rect.bottom * scale,
                                    paint);
                    }

                    if (!mIsBlank && mLinks != null && mHighlightLinks) {
                        paint.setColor(LINK_COLOR);
                        for (LinkInfo link : mLinks)
                            canvas.drawRect(link.rect.left * scale, link.rect.top * scale,
                                    link.rect.right * scale, link.rect.bottom * scale,
                                    paint);
                    }

                    if (mSelectBox != null && mText != null) {
                        paint.setColor(HIGHLIGHT_COLOR);
                        processSelectedText(new TextProcessor() {
                            RectF rect;

                            public void onStartLine() {
                                rect = new RectF();
                            }

                            public void onWord(TextWord word) {
                                if(mSelectFirstRect.left == 0 && mSelectFirstRect.right == 0 && mSelectFirstRect.top == 0 && mSelectFirstRect.bottom == 0){
                                    mSelectFirstRect.union(word);
                                }

                                rect.union(word);
                            }

                            public void onEndLine() {
                                if (!rect.isEmpty())
                                    canvas.drawRect(rect.left * scale, rect.top * scale, rect.right * scale, rect.bottom * scale, paint);
                                    if (eventCallback != null)
                                        eventCallback.touchMoveOnPdfPosition(new RectF(mSelectFirstRect.left,mSelectFirstRect.top,rect.right,rect.bottom),scale);
                            }
                        });
                    }

                    if (mItemSelectBox != null) {
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setColor(BOX_COLOR);
                        canvas.drawRect(mItemSelectBox.left * scale, mItemSelectBox.top * scale, mItemSelectBox.right * scale, mItemSelectBox.bottom * scale, paint);
                    }

                    if (mDrawing != null) {
                        Path path = new Path();
                        PointF p;

                        paint.setAntiAlias(true);
                        paint.setDither(true);
                        paint.setStrokeJoin(Paint.Join.ROUND);
                        paint.setStrokeCap(Paint.Cap.ROUND);

                        paint.setStyle(Paint.Style.FILL);
                        paint.setStrokeWidth(INK_THICKNESS * scale);
                        paint.setColor(INK_COLOR);

                        Iterator<ArrayList<PointF>> it = mDrawing.iterator();
                        while (it.hasNext()) {
                            ArrayList<PointF> arc = it.next();
                            if (arc.size() >= 2) {
                                Iterator<PointF> iit = arc.iterator();
                                p = iit.next();
                                float mX = p.x * scale;
                                float mY = p.y * scale;
                                path.moveTo(mX, mY);
                                while (iit.hasNext()) {
                                    p = iit.next();
                                    float x = p.x * scale;
                                    float y = p.y * scale;
                                    path.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                                    mX = x;
                                    mY = y;
                                }
                                path.lineTo(mX, mY);
                            } else {
                                p = arc.get(0);
                                canvas.drawCircle(p.x * scale, p.y * scale, INK_THICKNESS * scale / 2, paint);
                            }
                        }

                        paint.setStyle(Paint.Style.STROKE);
                        canvas.drawPath(path, paint);
                    }
                }
            };

            addView(mSearchView);
        }
        if(mCustomerView == null){
            mCustomerView = new View(mContext) {
                /**
                 * 监听custom_view层【文本、其他】填充动作
                 * **/
                @Override
                protected void onDraw(final Canvas canvas) {
                    super.onDraw(canvas);
                    // Work out current total scale factor
                    // from source to view
                    final float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
                    float scaledSizeInPixels = this.getResources().getDisplayMetrics().scaledDensity;
                    TextPaint textPaint = new TextPaint();
                    textPaint.setAntiAlias(true);

                    try{
                        if (mCloudData.getmFreetext().size()>0) {
                            textPaint.setColor(FREETEXT_COLOR);
                            for (int i = 0; i < mCloudData.getmFreetext().size(); i++) {
                                HashMap map = mCloudData.getmFreetext().get(i);
                                if((int)map.get("page") == getPage()){

                                    textPaint.setTextSize((float)map.get("size") * scale);
                                    // 自动换行
                                    StaticLayout layout = new StaticLayout((String)map.get("text"), textPaint, (int)((float)map.get("width") * scale * scaledSizeInPixels),
                                            Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                    canvas.save();
                                    canvas.translate((float)map.get("x") * scale, (float)map.get("y") * scale);
                                    layout.draw(canvas);
                                    canvas.restore();
                                }
                            }
                        }
                        if (mCloudData.getmRemarkList().size()>0) {
                            for(int i = 0; i < mCloudData.getmRemarkList().size(); i++) {
                                RemarkItem item = mCloudData.getmRemarkList().get(i);
                                if(item.page == getPage()){

                                    textPaint.setTextSize((float)item.size * scale);
                                    // 自动换行
                                    StaticLayout layout = new StaticLayout(item.text, textPaint, (int)((float)item.width * scale * scaledSizeInPixels),
                                            Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
                                    canvas.save();
                                    canvas.translate((float)item.x * scale, (float)item.y * scale);
                                    layout.draw(canvas);
                                    canvas.restore();
                                }
                            }
                        }
                    }catch (RuntimeException e){

                    }

                }
            };
            addView(mCustomerView);
        }
        requestLayout();
    }

    public void updateEntireCanvas(final boolean updateZoomed) {
        // Render the page in the background
        mDrawEntire = new CancellableAsyncTask<Void, Void>(getDrawPageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

            @Override
            public void cancelAndWait() {
                super.cancelAndWait();
                flagHQ = false;
            }

            @Override
            public void onPreExecute() {
                setBackgroundColor(BACKGROUND_COLOR);
                mEntire.setImageBitmap(null);
                mEntire.invalidate();

                if (mBusyIndicator == null) {
                    mBusyIndicator = new ProgressBar(mContext);
                    mBusyIndicator.setIndeterminate(true);
                    mBusyIndicator.setBackgroundResource(R.drawable.busy);
                    addView(mBusyIndicator);
                    mBusyIndicator.setVisibility(INVISIBLE);
                    mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (mBusyIndicator != null)
                                mBusyIndicator.setVisibility(VISIBLE);
                        }
                    }, PROGRESS_DIALOG_DELAY);
                }
            }

            @Override
            public void onPostExecute(Void result) {
                removeView(mBusyIndicator);
                mBusyIndicator = null;
                mEntire.setImageBitmap(mEntireBm);

                // Draws the signatures on EntireCanvas after changing pages (post loading).
                if (mEntireBm != null && !mEntireBm.isRecycled()) {
                    Canvas entireCanvas = new Canvas(mEntireBm);
                    drawBitmaps(entireCanvas, null, null);
                }

                if (updateZoomed && (mPatchBm != null) && !mPatchBm.isRecycled()) {
                    Canvas zoomedCanvas = new Canvas(mPatchBm);
                    drawBitmaps(zoomedCanvas, mPatchViewSize, mPatchArea);
                }
                flagHQ = false;
                mEntire.invalidate();
                setBackgroundColor(Color.TRANSPARENT);

            }
        };

        mDrawEntire.execute();

    }

    public void setSearchBoxes(RectF searchBoxes[]) {
        mSearchBoxes = searchBoxes;
        if (mSearchView != null)
            mSearchView.invalidate();
    }

    public void setLinkHighlighting(boolean f) {
        mHighlightLinks = f;
        if (mSearchView != null)
            mSearchView.invalidate();
    }

    public void deselectText() {
        mSelectBox = null;
        if (mSearchView != null)
            mSearchView.invalidate();
    }

    public void selectText(float x0, float y0, float x1, float y1) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX0 = (x0 - getLeft()) / scale;
        float docRelY0 = (y0 - getTop()) / scale;
        float docRelX1 = (x1 - getLeft()) / scale;
        float docRelY1 = (y1 - getTop()) / scale;
        // Order on Y but maintain the point grouping
        if (docRelY0 <= docRelY1)
            mSelectBox = new RectF(docRelX0, docRelY0, docRelX1, docRelY1);
        else
            mSelectBox = new RectF(docRelX1, docRelY1, docRelX0, docRelY0);

        if (mSearchView != null)
            mSearchView.invalidate();

        if (mGetText == null) {
            mGetText = new AsyncTask<Void, Void, TextWord[][]>() {
                @Override
                protected TextWord[][] doInBackground(Void... params) {
                    return getText();
                }

                @Override
                protected void onPostExecute(TextWord[][] result) {
                    mText = result;
                    if (mSearchView != null)
                        mSearchView.invalidate();
                }
            };

            mGetText.execute();
        }
    }

    public void startDraw(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;
        if (mDrawing == null)
            mDrawing = new ArrayList<ArrayList<PointF>>();

        ArrayList<PointF> arc = new ArrayList<PointF>();
        arc.add(new PointF(docRelX, docRelY));
        mDrawing.add(arc);
        if (mSearchView != null)
            mSearchView.invalidate();
    }

    public void continueDraw(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;

        if (mDrawing != null && mDrawing.size() > 0) {
            ArrayList<PointF> arc = mDrawing.get(mDrawing.size() - 1);
            arc.add(new PointF(docRelX, docRelY));
            if (mSearchView != null)
                mSearchView.invalidate();
        }
    }

    /**
     * 直线批注
     * **/
    public void continueDrawLine(float x, float y){
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;

        if (mDrawing != null && mDrawing.size() > 0) {
            ArrayList<PointF> arc = mDrawing.get(mDrawing.size() - 1);
            if(arc.size() == 2){
                arc.set(1,new PointF(docRelX, docRelY));
            }else {
                arc.add(new PointF(docRelX, docRelY));
            }

            if (mSearchView != null)
                mSearchView.invalidate();
        }
    }

    /**
     * 箭头批注
     * **/
    public void continueDrawArrow(float x, float y){
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;

        if (mDrawing != null && mDrawing.size() > 0) {
            ArrayList<PointF> arc = mDrawing.get(mDrawing.size() - 1);

            float fromX = arc.get(0).x;
            float fromY = arc.get(0).y;
            float toX = docRelX;
            float toY = docRelY;
            int headlen = 60;//自定义箭头线的长度
            int theta = 35;//自定义箭头线与直线的夹角
            // 计算各角度和对应的箭头终点坐标
            double angle = Math.atan2(fromY-toY , fromX-toX) * 180 / Math.PI;
            double angle1 = (angle + theta) * Math.PI / 180;
            double angle2 = (angle - theta) * Math.PI / 180;
            double topX = headlen * Math.cos(angle1);
            double topY = headlen * Math.sin(angle1);
            double botX = headlen * Math.cos(angle2);
            double botY = headlen * Math.sin(angle2);

            if(arc.size() == 8){
                arc.set(1,new PointF(docRelX, docRelY));
                arc.set(2,new PointF(docRelX, docRelY));
                arc.set(3,new PointF((float)(toX+topX), (float)(toY+topY)));
                arc.set(4,new PointF(docRelX, docRelY));
                arc.set(5,new PointF(docRelX, docRelY));
                arc.set(6,new PointF((float)(toX+botX), (float)(toY+botY)));
                arc.set(7,new PointF(docRelX, docRelY));
            }else {
                arc.add(new PointF(docRelX, docRelY));
                arc.add(new PointF(docRelX, docRelY));
                arc.add(new PointF((float)(toX+topX), (float)(toY+topY)));
                arc.add(new PointF(docRelX, docRelY));
                arc.add(new PointF(docRelX, docRelY));
                arc.add(new PointF((float)(toX+botX), (float)(toY+botY)));
                arc.add(new PointF(docRelX, docRelY));
            }

            if (mSearchView != null)
                mSearchView.invalidate();
        }
    }

    /**
     * 矩形批注
     * **/
    public void continueDrawRect(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;

        if (mDrawing != null && mDrawing.size() > 0) {
            ArrayList<PointF> arc = mDrawing.get(mDrawing.size() - 1);
            if(arc.size() == 8){
                arc.set(1,new PointF(arc.get(0).x, docRelY));
                arc.set(2,new PointF(arc.get(0).x, docRelY));
                arc.set(3,new PointF(docRelX, docRelY));
                arc.set(4,new PointF(docRelX, docRelY));
                arc.set(5,new PointF(docRelX, arc.get(0).y));
                arc.set(6,new PointF(docRelX, arc.get(0).y));
                arc.set(7,new PointF(arc.get(0).x, arc.get(0).y));
            }else {
                arc.add(new PointF(arc.get(0).x, docRelY));
                arc.add(new PointF(arc.get(0).x, docRelY));
                arc.add(new PointF(docRelX, docRelY));
                arc.add(new PointF(docRelX, docRelY));
                arc.add(new PointF(docRelX, arc.get(0).y));
                arc.add(new PointF(docRelX, arc.get(0).y));
                arc.add(new PointF(arc.get(0).x, arc.get(0).y));
            }

            if (mSearchView != null)
                mSearchView.invalidate();
        }
    }

    /**
     * 圆形批注
     * TODO: 未实现椭圆
     * **/
//    PointF center;
    public void continueDrawCircle(float x, float y) {
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;

        if (mDrawing != null && mDrawing.size() > 0) {
            ArrayList<PointF> arc = mDrawing.get(mDrawing.size() - 1);
            // 方案1： 第一个点为最左边的点
            PointF center = new PointF((docRelX+arc.get(0).x)/2,(docRelY+arc.get(0).y)/2);

            if(arc.size() == 25){
                for (int i = 1; i < 25; i++) {
                    arc.set(i, new PointF((float) (arc.get(0).x + (1 - Math.cos(Math.PI / 360 * i * 30)) * (center.x - arc.get(0).x)), (float) (arc.get(0).y + Math.sin(Math.PI / 360 * i * 30) * (center.x - arc.get(0).x))));
                }
            }else {
                for (int i = 1; i < 25; i++) {
                    arc.add(new PointF((float) (arc.get(0).x + (1 - Math.cos(Math.PI / 360 * i * 30)) * (center.x - arc.get(0).x)), (float) (arc.get(0).y + Math.sin(Math.PI / 360 * i * 30) * (center.x - arc.get(0).x))));
                }
            }

            // 方案2：第一个点为中心点
//            if(arc.size() == 25){
//                PointF radius = new PointF(Math.abs(docRelX-center.x),Math.abs(docRelY-center.y));
//                for (int i = 0; i < 25; i++) {
//                    arc.set(i, new PointF((float) (center.x-radius.x * Math.cos(Math.PI / 360 * i * 30)),(float) (center.y+radius.x * Math.sin(Math.PI / 360 * i * 30))));
//                }
//            }else {
//                center = arc.get(0);
//                PointF radius = new PointF(Math.abs(docRelX-center.x),Math.abs(docRelY-center.y));
//                for (int i = 1; i < 25; i++) {
//                    arc.add(new PointF((float) (center.x-radius.x * Math.cos(Math.PI / 360 * i * 30)),(float) (center.y+radius.x * Math.sin(Math.PI / 360 * i * 30))));
//                }
//            }

            //方案3：两点确定一个矩形，根据矩形来画圆。一共12个点
//            if(arc.size() == 13){
//                arc.set(0,new PointF((center.x+docRelX)/2,center.y));
//
//                arc.set(1,new PointF((3*center.x+docRelX)/4,(7*center.y+docRelY)/8));
//                arc.set(2,new PointF((7*center.x+docRelX)/8,(3*center.y+docRelY)/4));
//
//                arc.set(3,new PointF(center.x,(center.y+docRelY)/2));
//
//                arc.set(4,new PointF((7*center.x+docRelX)/8,(center.y+3*docRelY)/4));
//                arc.set(5,new PointF((3*center.x+docRelX)/4,(center.y+7*docRelY)/8));
//
//                arc.set(6,new PointF((center.x+docRelX)/2,docRelY));
//
//                arc.set(7,new PointF((center.x+3*docRelX)/4,(center.y+7*docRelY)/8));
//                arc.set(8,new PointF((center.x+7*docRelX)/8,(center.y+3*docRelY)/4));
//
//                arc.set(9,new PointF(docRelX,(center.y+docRelY)/2));
//
//                arc.set(10,new PointF((center.x+7*docRelX)/8,(3*center.y+docRelY)/4));
//                arc.set(11,new PointF((center.x+3*docRelX)/4,(7*center.y+docRelY)/8));
//
//                arc.set(12,new PointF((center.x+docRelX)/2,center.y));
//            }else {
//                center = arc.get(0);
//                arc.add(new PointF((center.x+docRelX)/2,center.y));
//
//                arc.add(new PointF((3*center.x+docRelX)/4,(7*center.y+docRelY)/8));
//                arc.add(new PointF((7*center.x+docRelX)/8,(3*center.y+docRelY)/4));
//
//                arc.add(new PointF(center.x,(center.y+docRelY)/2));
//
//                arc.add(new PointF((7*center.x+docRelX)/8,(center.y+3*docRelY)/4));
//                arc.add(new PointF((3*center.x+docRelX)/4,(center.y+7*docRelY)/8));
//
//                arc.add(new PointF((center.x+docRelX)/2,docRelY));
//
//                arc.add(new PointF((center.x+3*docRelX)/4,(center.y+7*docRelY)/8));
//                arc.add(new PointF((center.x+7*docRelX)/8,(center.y+3*docRelY)/4));
//
//                arc.add(new PointF(docRelX,(center.y+docRelY)/2));
//
//                arc.add(new PointF((center.x+7*docRelX)/8,(3*center.y+docRelY)/4));
//                arc.add(new PointF((center.x+3*docRelX)/4,(7*center.y+docRelY)/8));
//            }

            if (mSearchView != null)
                mSearchView.invalidate();
        }
    }

    /**
     * 单点批注不保存
     * **/
    public void stopDraw(){
        ArrayList<PointF> arc = mDrawing.get(mDrawing.size() - 1);
        if(arc.size() == 1){
            mDrawing.remove(mDrawing.size() - 1);
        }
        if (mSearchView != null)
            mSearchView.invalidate();
    }

    /**
     * 回退上一步
     * **/
    public void undoDraw() {
        if(mDrawing.size() > 0){
            mDrawing.remove(mDrawing.size() - 1);

            if (mSearchView != null)
                mSearchView.invalidate();
        }

    }

    public void cancelDraw() {
        mDrawing = null;
        if (mSearchView != null)
            mSearchView.invalidate();
        if (mCustomerView != null)
            mCustomerView.bringToFront();
    }

    /**
     * 获得批注数据path
     * **/
    protected PointF[][] getDraw() {
        if (mDrawing == null)
            return null;

        PointF[][] path = new PointF[mDrawing.size()][];

        for (int i = 0; i < mDrawing.size(); i++) {
            ArrayList<PointF> arc = mDrawing.get(i);
            path[i] = arc.toArray(new PointF[arc.size()]);
        }

        return path;
    }

    protected void setDraw(PointF[][] arcs){
        mDrawing = new ArrayList<ArrayList<PointF>>();
        for (int i = 0; i < arcs.length; i++) {
            ArrayList<PointF> arc = new ArrayList<PointF>(Arrays.asList(arcs[i]));
            mDrawing.add(arc);
        }
    }

    protected void processSelectedText(TextProcessor tp) {
        (new TextSelector(mText, mSelectBox)).select(tp);
    }

    public void setItemSelectBox(RectF rect) {
        /**
         * 显示菜单框
         * **/
        if (eventCallback != null) {
            float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
            eventCallback.singleTapOnHit(rect,scale);
            deselectText();
        }

        mItemSelectBox = rect;
        if (mSearchView != null)
            mSearchView.invalidate();

        if (mCustomerView != null)
            mCustomerView.invalidate();
    }

    public void setItemSelectBox(Annotation annotation){
        if(annotation != null){
            mAnnotationType = annotation.type;
        }
        setItemSelectBox((RectF) annotation);
    }

    public View getCustomerView(){
        return mCustomerView;
    }

    public void addFreetextAnnotation(float x, float y, float width, float height, String text){
        float scale = mSourceScale * (float) getWidth() / (float) mSize.x;
        float docRelX = (x - getLeft()) / scale;
        float docRelY = (y - getTop()) / scale;
        HashMap map = new HashMap();
        map.put("type","textbox");
        map.put("x",docRelX);
        map.put("y",docRelY);
        map.put("width",width);
        map.put("height",height);
        map.put("text",text);
        map.put("size",50 / scale);
        map.put("page",getPage());
        map.put("scale",scale);
        mCloudData.add(map);

        if (mCustomerView != null)
            mCustomerView.invalidate();
    }

    public void addFreetextAnnotation(HashMap map){
        mCloudData.add(map);

        if (mCustomerView != null)
            mCustomerView.invalidate();
    }


    /**
     * 获得画笔粗细
     */
    protected float getInkThickness() {
        return INK_THICKNESS / 2;
    }

    /**
     * 获得画笔颜色
     */
    protected float[] getColor() {

        return changeColor(INK_COLOR);
    }

    /**
     * 将十进制颜色值转换成RGB格式
     * @param color
     * @return
     */
    private float[] changeColor(int color) {

        int red = (color & 0xff0000) >> 16;
        int green = (color & 0x00ff00) >> 8;
        int blue = (color & 0x0000ff);

        float colors[] = new float[3];
        colors[0] = red/255f;
        colors[1] = green/255f;
        colors[2] = blue/255f;

        return colors;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int x, y;
        switch (MeasureSpec.getMode(widthMeasureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                x = mSize.x;
                break;
            default:
                x = MeasureSpec.getSize(widthMeasureSpec);
        }
        switch (MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.UNSPECIFIED:
                y = mSize.y;
                break;
            default:
                y = MeasureSpec.getSize(heightMeasureSpec);
        }

        setMeasuredDimension(x, y);

        if (mBusyIndicator != null) {
            int limit = Math.min(mParentSize.x, mParentSize.y) / 2;
            mBusyIndicator.measure(MeasureSpec.AT_MOST | limit, MeasureSpec.AT_MOST | limit);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int w = right - left;
        int h = bottom - top;

        if (mEntire != null) {
            if (mEntire.getWidth() != w || mEntire.getHeight() != h) {
                mEntireMat.setScale(w / (float) mSize.x, h / (float) mSize.y);
                mEntire.setImageMatrix(mEntireMat);
                mEntire.invalidate();
            }
            mEntire.layout(0, 0, w, h);
        }

        /**
         * 初始化时 mSearchView占满全屏
         * 1。用于批注被选中时显示box
         * 2。用于画批注保存前的预览图
         * 3。feature 添加自定义文本的话得又加一层画布了或者 svg也不错
         * **/
        if (mSearchView != null) {
            mSearchView.layout(0, 0, w, h);
        }
        if(mCustomerView != null) {
            mCustomerView.layout(0, 0, w, h);
        }

        if (mPatchViewSize != null) {
            if (mPatchViewSize.x != w || mPatchViewSize.y != h) {
                // Zoomed since patch was created
                mPatchViewSize = null;
                mPatchArea = null;
                if (mPatch != null) {
                    mPatch.setImageBitmap(null);
                    mPatch.invalidate();
                }
            } else {
                mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
            }
        }

        if (mBusyIndicator != null) {
            int bw = mBusyIndicator.getMeasuredWidth();
            int bh = mBusyIndicator.getMeasuredHeight();

            mBusyIndicator.layout((w - bw) / 2, (h - bh) / 2, (w + bw) / 2, (h + bh) / 2);
        }
    }

    private boolean flagHQ = false;
    public void updateHq(boolean update) {
        Rect viewArea = new Rect(getLeft(), getTop(), getRight(), getBottom());
        if (viewArea.width() == mSize.x || viewArea.height() == mSize.y) {
            // If the viewArea's size matches the unzoomed size, there is no need for an hq patch
            if (mPatch != null) {
                mPatch.setImageBitmap(null);
                mPatch.invalidate();
            }
        } else {
            final Point patchViewSize = new Point(viewArea.width(), viewArea.height());
            final Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);

            // Intersect and test that there is an intersection
            if (!patchArea.intersect(viewArea))
                return;

            // Offset patch area to be relative to the view top left
            patchArea.offset(-viewArea.left, -viewArea.top);

            boolean area_unchanged = patchArea.equals(mPatchArea) && patchViewSize.equals(mPatchViewSize);

            // If being asked for the same area as last time and not because of an update then nothing to do
            if (area_unchanged && !update)
                return;

            boolean completeRedraw = !(area_unchanged);

            // Stop the drawing of previous patch if still going
            if (mDrawPatch != null) {
                mDrawPatch.cancelAndWait();
                mDrawPatch = null;
            }

            // Create and add the image view if not already done
            if (mPatch == null) {
                mPatch = new OpaqueImageView(mContext);
                mPatch.setScaleType(ImageView.ScaleType.MATRIX);
                addView(mPatch);
                mSearchView.bringToFront();
            }

            CancellableTaskDefinition<Void, Void> task;

            try {
                if(mDrawing == null){
                    int mPatchAreaHeight = patchArea.bottom - patchArea.top;
                    int mPatchAreaWidth = patchArea.right - patchArea.left;
                    mPatchBm = Bitmap.createBitmap(mPatchAreaWidth, mPatchAreaHeight, Bitmap.Config.ARGB_8888);
                    cancelDraw();
                }
            } catch (OutOfMemoryError e) {
                Log.e(TAG, e.getMessage(), e);
                flagHQ = false;
            }

            if (completeRedraw)
                task = getDrawPageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
                        patchArea.left, patchArea.top,
                        patchArea.width(), patchArea.height());
            else
                task = getUpdatePageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
                        patchArea.left, patchArea.top,
                        patchArea.width(), patchArea.height());

            mDrawPatch = new CancellableAsyncTask<Void, Void>(task) {

                public void onPostExecute(Void result) {
                    mPatchViewSize = patchViewSize;
                    mPatchArea = patchArea;
                    mPatch.setImageBitmap(mPatchBm);
                    mPatch.invalidate();
                    //requestLayout();
                    // Calling requestLayout here doesn't lead to a later call to layout. No idea
                    // why, but apparently others have run into the problem.
                    mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
                }
            };

            mDrawPatch.execute();
        }
    }

    public void update() {
        // Cancel pending render task
        if (mDrawEntire != null) {
            mDrawEntire.cancelAndWait();
            mDrawEntire = null;
        }

        if (mDrawPatch != null) {
            mDrawPatch.cancelAndWait();
            mDrawPatch = null;
        }

        mDrawEntire = new CancellableAsyncTask<Void, Void>(getUpdatePageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

            @Override
            public void cancelAndWait() {
                super.cancelAndWait();
                flagHQ = false;
            }

            public void onPostExecute(Void result) {
                if (mEntireBm != null && !mEntireBm.isRecycled()) {
                    Canvas entireCanvas = new Canvas(mEntireBm);
                    drawBitmaps(entireCanvas, null, null);
                    mEntire.setImageBitmap(mEntireBm);
                    mEntire.invalidate();
                    flagHQ=false;
                }
            }
        };

        mDrawEntire.execute();

        updateHq(true);
    }

    public void removeHq() {
        // Stop the drawing of the patch if still going
        if (mDrawPatch != null) {
            mDrawPatch.cancelAndWait();
            mDrawPatch = null;
        }

        // And get rid of it
        mPatchViewSize = null;
        mPatchArea = null;
        if (mPatch != null) {
            mPatch.setImageBitmap(null);
            mPatch.invalidate();
        }
        flagHQ = false;
    }

    public int getPage() {
        return mPageNumber;
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    protected void redrawEntireBitmaps() {
        if (mEntireBm != null && !mEntireBm.isRecycled()) {
            Canvas entireCanvas = new Canvas(mEntireBm);
            drawBitmaps(entireCanvas, null, null);
            mEntire.setImageBitmap(mEntireBm);
            mEntire.invalidate();
        }
    }

    private void redrawZoomedBitmaps() {
        if (mPatchBm != null && !mPatchBm.isRecycled()) {
            Canvas zoomedCanvas = new Canvas(mPatchBm);
            drawBitmaps(zoomedCanvas, mPatchViewSize, mPatchArea);
            mPatch.setImageBitmap(mPatchBm);
            mPatch.invalidate();
        }
    }


    public boolean removeBitmapOnPosition(Point point) {
        boolean removed = false;
        switch (removeIfExistSign(point)) {
            case -1:
                removed = false;
                break;
            case 0:
                removed = true;
                break;
            case 1:
                removed = true;
                break;
        }
        //Forzamos pintado de pantalla
        invalidate();

        return removed;
    }

    public void onLongPress(MotionEvent e, float mScale) {
        if (eventCallback != null) {
            float x = e.getX();
            float y = e.getY();

            //Comprobamos si ha picado dentro o fuera del espacio del pdf
            if (x < getLeft() || x > getRight()) {
                eventCallback.error(DigitalizedEventCallback.ERROR_OUTSIDE_HORIZONTAL);
            }

            if (y < getTop() || y > getBottom()) {
                eventCallback.error(DigitalizedEventCallback.ERROR_OUTSIDE_VERTICAL);
            }

            float[] coords = translateCoords(mScale, x, y);
            if (coords != null) {
                eventCallback.longPressOnPdfPosition(mPageNumber, coords[0], coords[1], coords[2], coords[3]);
            }
        }
    }

    public void onSingleTap(MotionEvent e, float mScale) {
        if (eventCallback != null) {
            float x = e.getX();
            float y = e.getY();

            //Comprobamos si ha picado dentro o fuera del espacio del pdf
            if (x < getLeft() || x > getRight()) {
                eventCallback.error(DigitalizedEventCallback.ERROR_OUTSIDE_HORIZONTAL);
            }

            if (y < getTop() || y > getBottom()) {
                eventCallback.error(DigitalizedEventCallback.ERROR_OUTSIDE_VERTICAL);
            }

            float[] coords = translateCoords(mScale, x, y);
            if (coords != null) {
                eventCallback.singleTapOnPdfPosition(mPageNumber, coords[0], coords[1], coords[2], coords[3]);
            }
        }
    }

    private float[] translateCoords(float mScale, float x, float y) {
        float screenX, screenY, percentX, percentY;

        if (pdfSize != null && mSize != null) {
            //Factor de corrección por si se gira
            float factorRotationX = ((float) mSize.x / (float) getWidth()) * mScale;
            float factorRotationY = ((float) mSize.y / (float) getHeight()) * mScale;

            //Posicion en la pantalla respecto a las coordenadas del pdf (el 0.0 es la esquina arriba izquierda del pdf). Usado para poder dibujar las firmas encima del PDF. En esta representación, el PDF tendría de alto valores similares al alto de la pantalla en la que se muestra.
            screenX = ((x - getLeft()) / mScale) * factorRotationX;
            screenY = ((y - getTop()) / mScale) * factorRotationY;

            // Calculamos posicion en el pdf. No se usa en la visualización, pero es necesario para conocer la posición. En esta representación, el alto del pdf será de unos 900 píxeles, y no variará se muestre donde se muestre.
            percentX = (x - getLeft()) / getWidth();
            percentY = (y - getTop()) / getHeight(); //Se coge la posicion en porcentaje

            float pdfX = percentX * pdfSize.x; //Se calcula X el punto en el pdf
            float pdfY = (1 - percentY) * pdfSize.y;//Se calcula Y

            // Proportions: screenX / mSize.x == pdfX / pdfSize.x !!!
            return new float[]{screenX, screenY, pdfX, pdfY};
        } else {
            return null;
        }
    }

    private float[] pdfCoordsToScreen(float pdfX, float pdfY) {
        float screenX = (pdfX * mSize.x) / pdfSize.x;
        float screenY = ((pdfSize.y - pdfY) * mSize.y) / pdfSize.y;
        return new float[]{screenX, screenY};
    }

    public boolean onDoubleTap(MotionEvent e, float mScale) {

        if (flagPositions) {
            flagPositions = false;

            float x = e.getX();
            float y = e.getY();

            //Comprobamos si ha picado dentro o fuera del espacio del pdf
            if (x < getLeft() || x > getRight()) {
                flagPositions = true;
                if (eventCallback != null) {
                    eventCallback.error(DigitalizedEventCallback.ERROR_OUTSIDE_HORIZONTAL);
                }
                return true;
            }
            if (y < getTop() || y > getBottom()) {
                flagPositions = true;
                if (eventCallback != null) {
                    eventCallback.error(DigitalizedEventCallback.ERROR_OUTSIDE_VERTICAL);
                }
                return true;
            }

            float[] coords = translateCoords(mScale, x, y);
            if (coords != null) {
                float screenX = coords[0];
                float screenY = coords[1];
                float pdfX = coords[2];
                float pdfY = coords[3];

                if (eventCallback != null) {
                    flagPositions = true;
                    eventCallback.doubleTapOnPdfPosition(mPageNumber, screenX, screenY, pdfX, pdfY);
                    return true;
                }

                //Salvamos la posicion donde se ha elegido estampar la firma
                Point point = new Point((int) screenX, (int) screenY);
                boolean removed = removeBitmapOnPosition(point);

                if (signBitmap != null && signBitmapSize != null && !removed) {
                    PdfBitmap newPdfBitmap = new PdfBitmap(signBitmap, SIGN_WIDTH, SIGN_HEIGHT, (int) screenX, (int) screenY, mPageNumber, PdfBitmap.Type.SIGNATURE);
                    mAdapter.getPdfBitmapList().add(newPdfBitmap);
                    mAdapter.setNumSignature(mAdapter.getNumSignature() + 1);
                }
            }
            flagPositions = true;
        }
        return true;
    }

    /**
     * Check if a Bitmap exists in the point coordinates, and remove it.
     *
     * @param screenPoint Point for the pdf to check
     * @return
     */
    private int removeIfExistSign(Point screenPoint) {
        PdfBitmap toRemove = null;
        for (PdfBitmap pdfBitmap : mAdapter.getPdfBitmapList()) {
            if (pdfBitmap.getPageNumber() == mPageNumber) {
                float[] scaledSize = scaledSize(pdfBitmap.getWidth(), pdfBitmap.getHeight());
                int originalW = (int) scaledSize[0];
                int originalH = (int) scaledSize[1];

                float[] screenCoords = pdfCoordsToScreen(pdfBitmap.getPdfX(), pdfBitmap.getPdfY());
                int screenX = (int) screenCoords[0];
                int screenY = (int) screenCoords[1];

                Rect r = new Rect(screenX - (originalW / 2), screenY + (originalH / 2), screenX + (originalW / 2), screenY - (originalH / 2));
                if (screenPoint.x > r.left && screenPoint.x < r.right && screenPoint.y < r.top && screenPoint.y > r.bottom) {
                    toRemove = pdfBitmap;

                    boolean indexOf = mAdapter.getPdfBitmapList().contains(toRemove);
                    if (indexOf && toRemove.isRemovable()) {
                        mAdapter.getPdfBitmapList().remove(toRemove);
                        mAdapter.setNumSignature(mAdapter.getNumSignature() - 1);

                        // We need to remove the previous entireBm (with the bitmaps added), and create a new one empty (the bitmaps will be added on update)
                        Log.i(TAG, "Recycle mEntire on removeIfExistSign: " + mEntireBm);
                        final Bitmap oldEntireBm = mEntireBm;
                        try {
                            mEntireBm = Bitmap.createBitmap(mParentSize.x, mParentSize.y, Bitmap.Config.ARGB_8888);
                            updateEntireCanvas(true);
                            updateHq(true);
                        } catch (OutOfMemoryError e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                        if(oldEntireBm!=null && !oldEntireBm.equals(mEntireBm)) {
                            recycleBitmap(oldEntireBm);
                        }
                        // Bitmap removed
                        return 0;
                    }
                }
            }
        }
        // No bitmap removed
        return -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    /**
     * Por defecto la medida de pagina que devuelve MuPdf parece ser dos veces superior al correcto
     *
     * @param size
     * @return
     */
    private PointF correctBugMuPdf(PointF size) {
        return new PointF(size.x / 2, size.y / 2);
    }

    public DigitalizedEventCallback getEventCallback() {
        return eventCallback;
    }

    public void setEventCallback(DigitalizedEventCallback eventCallback) {
        this.eventCallback = eventCallback;
    }

    private void drawBitmaps(Canvas canvas, Point patchViewSize, Rect patchArea) {
        // Sólo ejecutamos este código en caso de que tengamos un Bitmap de firma:
        for (PdfBitmap pdfBitmap : mAdapter.getPdfBitmapList()) {

            float[] scaledSize = scaledSize(pdfBitmap.getWidth(), pdfBitmap.getHeight());

            float originalW = scaledSize[0];
            float originalH = scaledSize[1];
            float zoomRatio = patchViewSize != null ? (float) patchViewSize.y / (float) mSize.y : 1.0f;
            float newWidth = originalW * zoomRatio;
            float newHeight = originalH * zoomRatio;

            if (pdfBitmap.getPageNumber() == getPage()) {

                float[] screenCoords = pdfCoordsToScreen(pdfBitmap.getPdfX(), pdfBitmap.getPdfY());

                float newGlobalPosX = (screenCoords[0] * zoomRatio);
                float newGlobalPosY = (screenCoords[1] * zoomRatio);
                float newZoomPosX = patchArea != null ? newGlobalPosX - patchArea.left : newGlobalPosX;
                float newZoomPosY = patchArea != null ? newGlobalPosY - patchArea.top : newGlobalPosY;

                float leftGlobalMargin = newGlobalPosX - newWidth / 2;
                float rightGlobalMargin = newGlobalPosX + newWidth / 2;
                float topGlobalMargin = newGlobalPosY - newHeight / 2;
                float bottomGlobalMargin = newGlobalPosY + newHeight / 2;

                Rect signZoomedRect = new Rect(
                        (int) newZoomPosX - (int) newWidth / 2,
                        (int) newZoomPosY - (int) newHeight / 2,
                        (int) newZoomPosX + (int) newWidth / 2,
                        (int) newZoomPosY + (int) newHeight / 2);

                boolean outside;
                if (patchArea == null) {
                    outside = false;
                } else {
                    outside = (rightGlobalMargin <= patchArea.left ||
                            leftGlobalMargin >= patchArea.right ||
                            topGlobalMargin >= patchArea.bottom ||
                            bottomGlobalMargin <= patchArea.top);
                }

                if (!outside) {
                    Bitmap bitmap = pdfBitmap.getBitmapImage();
                    try {
                        if (!isBitmapRecycled(bitmap)) {
                            canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), signZoomedRect, mBitmapPaint);
                            canvas.save();
                        } else {
                            Log.i(TAG, "Avoided using recycled bitmap");
                        }
                    } catch (RuntimeException e) {
                        Log.e(TAG, e.getLocalizedMessage(), e);
                    }
                }
            }
        }
    }

    private float[] scaledSize(int width, int height) {
        float x = 0, y = 0;
        if (pdfSize != null && mSize != null) {
            x = (width * mSize.x) / pdfSize.x;
            y = (height * mSize.y) / pdfSize.y;
        }
        return new float[]{x, y};
    }

    public void setParentSize(Point parentSize) {
        this.mParentSize = parentSize;
    }

    public boolean isBitmapRecycled(Bitmap bitmap) {
        if (android.os.Build.VERSION.SDK_INT < 17) {
            return bitmap.isRecycled();
        } else {
            return bitmap.isRecycled() || (!bitmap.isPremultiplied() && bitmap.getConfig() == Bitmap.Config.ARGB_8888 && bitmap.hasAlpha());
        }
    }

    public void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            Log.d(TAG, "Recycling bitmap " + bitmap.toString());
            bitmap.recycle();
            if(!bitmap.isRecycled()){
                Log.e(TAG, "NOT Recycled bitmap " + bitmap.toString());
            }
        }
    }

}
