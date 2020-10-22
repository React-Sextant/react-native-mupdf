package com.artifex.mupdfdemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import java.util.Date;

public class MuPDFReaderView extends ReaderView {
	enum Mode {Viewing, Selecting, Drawing, Freetexting, Move}
	enum DrawingMode {DrawPencil, DrawLine, DrawRect, DrawCircle}
	private final Context mContext;
	private boolean mLinksEnabled = false;
	private Mode mMode = Mode.Viewing;
	private DrawingMode mDrawingMode = DrawingMode.DrawPencil;
	private boolean tapDisabled = false;
	private boolean isLongPressed = false;//Continue onScroll event after onLongPress 长按事件之后继续其他事件
	private int tapPageMargin;

    private final boolean TAP_PAGING_ENABLED = false;

	private float             mLastTouchX;
	private long              touchDuration;

	protected void onTapMainDocArea() {}
	protected void onDocMotion() {}
	protected void onHit(Hit item) {}
	protected void onFreetextAdd(float x, float y) {}

	public void setLinksEnabled(boolean b) {
		mLinksEnabled = b;
		resetupChildren();
	}

	public void setMode(Mode m) {
		mMode = m;
	}

	public Mode getMode() {
		return mMode;
	}

	public void setDrawingMode(DrawingMode m) {
		mDrawingMode = m;
	}

	private void setup()
	{
		// Get the screen size etc to customise tap margins.
		// We calculate the size of 1 inch of the screen for tapping.
		// On some devices the dpi values returned are wrong, so we
		// sanity check it: we first restrict it so that we are never
		// less than 100 pixels (the smallest Android device screen
		// dimension I've seen is 480 pixels or so). Then we check
		// to ensure we are never more than 1/5 of the screen width.
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);
		tapPageMargin = (int)dm.xdpi;
		if (tapPageMargin < 100)
			tapPageMargin = 100;
		if (tapPageMargin > dm.widthPixels/5)
			tapPageMargin = dm.widthPixels/5;
	}

	public MuPDFReaderView(Context context) {
		super(context);
		mContext = context;
		setup();
	}

	public MuPDFReaderView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
		setup();
	}

	public boolean onSingleTapUp(MotionEvent e) {
		LinkInfo link = null;
		MuPDFView pageView = (MuPDFView) getDisplayedView();
		if (mMode == Mode.Viewing && !tapDisabled) {
			Hit item = pageView.passClickEvent(e.getX(), e.getY());
			onHit(item);
			if (item == Hit.Nothing) {
				if (mLinksEnabled && pageView != null
				&& (link = pageView.hitLink(e.getX(), e.getY())) != null) {
					link.acceptVisitor(new LinkInfoVisitor() {
						@Override
						public void visitInternal(LinkInfoInternal li) {
							// Clicked on an internal (GoTo) link
							setDisplayedViewIndex(li.pageNumber);
						}

						@Override
						public void visitExternal(LinkInfoExternal li) {
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri
									.parse(li.url));
							mContext.startActivity(intent);
						}

						@Override
						public void visitRemote(LinkInfoRemote li) {
							// Clicked on a remote (GoToR) link
						}
					});
				} else if (TAP_PAGING_ENABLED && e.getX() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (TAP_PAGING_ENABLED && e.getX() > super.getWidth() - tapPageMargin) {
					super.smartMoveForwards();
				} else if (TAP_PAGING_ENABLED && e.getY() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (TAP_PAGING_ENABLED && e.getY() > super.getHeight() - tapPageMargin) {
					super.smartMoveForwards();
				} else {
					onTapMainDocArea();
				}
			}
		}else if (mMode == Mode.Freetexting) {
			onFreetextAdd(e.getX(), e.getY());
		}else if (mMode == Mode.Selecting) {
			mMode = Mode.Viewing;
			Hit item = pageView.passClickEvent(e.getX(), e.getY());
			onHit(item);
		}
		return super.onSingleTapUp(e);
	}

	@Override
	public boolean onDown(MotionEvent e) {

		return super.onDown(e);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		MuPDFView pageView = (MuPDFView)getDisplayedView();
		switch (mMode) {
		case Viewing:
			if (!tapDisabled)
				onDocMotion();

			return super.onScroll(e1, e2, distanceX, distanceY);
		case Selecting:
			if (pageView != null)
				pageView.selectText(e1.getX(), e1.getY(), e2.getX(), e2.getY());
			return true;
		default:
			return true;
		}
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		switch (mMode) {
		case Viewing:
			return super.onFling(e1, e2, velocityX, velocityY);
		default:
			return true;
		}
	}

	public boolean onScaleBegin(ScaleGestureDetector d) {
		// Disabled showing the buttons until next touch.
		// Not sure why this is needed, but without it
		// pinch zoom can make the buttons appear
		tapDisabled = true;
		switch (mMode) {
			case Viewing:
				return super.onScaleBegin(d);
			default:
				return false;
		}
	}

	public void onLongPress(MotionEvent event) {
		isLongPressed = true;
		super.onLongPress(event);
	}

	public boolean onTouchEvent(MotionEvent event) {

		if ( mMode == Mode.Drawing )
		{
			float x = event.getX();
			float y = event.getY();
			int toolType=event.getToolType(0);
			switch (event.getAction())
			{
				case MotionEvent.ACTION_DOWN:
					/**
					 * 笔触与手触事件
					 * 场景1: only finger 设为默认
					 * 场景2: stylus first , finger after（系统自动优化）
					 * 场景3: finger first , stylus after
					 *  - 利用时间差判断，短时间内finger的操作需要被撤销
					 *  - finger还在就开始stylus（系统自动优化）
					 * **/
					if(toolType == MotionEvent.TOOL_TYPE_STYLUS && touchDuration > 0 && new Date().getTime() - touchDuration < 150){
						touch_undo();
					}

					touch_start(x, y);
					break;
				case MotionEvent.ACTION_MOVE:
					touch_move(x, y);
					break;
				case MotionEvent.ACTION_UP:
					if(toolType == MotionEvent.TOOL_TYPE_FINGER){
						touchDuration = new Date().getTime();
					}else {
						touchDuration = 0;
					}
					touch_up();
					break;
			}
		}

		if ( mMode == Mode.Selecting && isLongPressed )
		{
			switch (event.getActionMasked()) {
				case MotionEvent.ACTION_MOVE:
					MotionEvent cancel = MotionEvent.obtain(event);
					cancel.setAction(MotionEvent.ACTION_CANCEL);
					mGestureDetector.onTouchEvent(cancel);
					isLongPressed = false;
					break;
			}
		}else if(mMode != Mode.Drawing & mMode != Mode.Move){

			// We need this check to avoid refreshing the screen after a "tap" or "double tap". We only want to refresh the PDF after a pan, pinch or drag.
			int ident = MotionEventCompat.getActionIndex(event);
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
				mLastTouchX = MotionEventCompat.getX(event, ident);
				touchDuration = new Date().getTime();
			}

			if (event.getActionMasked() == MotionEvent.ACTION_UP) {
				float upX = MotionEventCompat.getX(event, ident);
				int displacementX = (int) Math.abs(mLastTouchX - upX);
				/**
				 * 短时间内（< 150）滑动一定距离(> 100)时，允许翻页操作
				 *
				 * mLastTouchX > upX 下一页
				 * mLastTouchX < upX 上一页
				 * **/
				if(new Date().getTime() - touchDuration < 150 && displacementX > 100){
					if(mLastTouchX > upX && mChildViews.get(mCurrent+1) != null){
						smartMoveForwards();
						return true;
					} else if(mLastTouchX < upX && mChildViews.get(mCurrent-1) != null){
						smartMoveBackwards();
						return true;
					}
				}
			}
		}

		if ((event.getAction() & event.getActionMasked()) == MotionEvent.ACTION_DOWN)
		{
			tapDisabled = false;
		}

		return super.onTouchEvent(event);
	}

	private float mX, mY;

	private static final float TOUCH_TOLERANCE = 2;

	private void touch_start(float x, float y) {
		if ( mMode == Mode.Drawing ) {
			MuPDFView pageView = (MuPDFView) getDisplayedView();
			if (pageView != null) {
				pageView.startDraw(x, y);
			}
			mX = x;
			mY = y;
		}
	}

	private void touch_move(float x, float y) {
		if ( mMode == Mode.Drawing ) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE)
			{
				MuPDFView pageView = (MuPDFView)getDisplayedView();
				if (pageView != null)
				{
					if(mDrawingMode == DrawingMode.DrawPencil){
						pageView.continueDraw(x, y);
					}else if(mDrawingMode == DrawingMode.DrawLine){
						pageView.continueDrawLine(x, y);
					}else if(mDrawingMode == DrawingMode.DrawRect){
						pageView.continueDrawRect(x, y);
					}else if(mDrawingMode == DrawingMode.DrawCircle){
						pageView.continueDrawCircle(x, y);
					}

				}
				mX = x;
				mY = y;
			}
		}
	}

	private void touch_up() {
		if ( mMode == Mode.Drawing ) {
			MuPDFView pageView = (MuPDFView)getDisplayedView();
			if (pageView != null)
			{
				pageView.stopDraw();
			}
		}

	}

	private void touch_undo() {
		if ( mMode == Mode.Drawing ) {
			MuPDFView pageView = (MuPDFView)getDisplayedView();
			if (pageView != null)
			{
				pageView.undoDraw();
			}
		}
	}

	protected void onChildSetup(int i, View v) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber == i)
			((MuPDFView) v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
		else
			((MuPDFView) v).setSearchBoxes(null);

		((MuPDFView) v).setLinkHighlighting(mLinksEnabled);

		((MuPDFView) v).setChangeReporter(new Runnable() {
			public void run() {
				applyToChildren(new ReaderView.ViewMapper() {
					@Override
					void applyToView(View view) {
						((MuPDFView) view).update();
					}
				});
			}
		});
	}

	protected void onMoveToChild(int i) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber != i) {
			SearchTaskResult.set(null);
			resetupChildren();
		}
	}

	@Override
	protected void onMoveOffChild(int i) {
		View v = getView(i);
		if (v != null)
			((MuPDFView)v).deselectAnnotation();
	}

	protected void onSettle(View v) {
		// When the layout has settled ask the page to render
		// in HQ
		((MuPDFView) v).updateHq(false);
	}

	protected void onUnsettle(View v) {
		// When something changes making the previous settled view
		// no longer appropriate, tell the page to remove HQ
		((MuPDFView) v).removeHq();
	}

	@Override
	protected void onNotInUse(View v) {
		((MuPDFView) v).releaseResources();
	}

	@Override
	protected void onScaleChild(View v, Float scale) {
		((MuPDFView) v).setScale(scale);
	}
}
