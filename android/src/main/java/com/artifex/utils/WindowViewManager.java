package com.artifex.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ViewAnimator;

import com.github.react.sextant.R;

public class WindowViewManager {
    static View windowView;
    static WindowManager windowManager;
    static boolean sShowWindowView = false;

    public static void closeWindowView(){
        System.out.println("luokun: "+windowView+"; "+windowManager);
        if(windowView != null && windowManager != null){
            windowManager.removeView(windowView);
            windowManager = null;
            windowView = null;
        }
    }

    public static void createWindowView(boolean showWindowView) {
        sShowWindowView = showWindowView;
    }
    public static void createWindowView(final Context mContext, final Bitmap bitmap) {
        if(!sShowWindowView){
            return;
        }
        try{
            final ImageView imageView;
            final ImageButton imageButton;
            final ViewAnimator close;
            final WindowManager.LayoutParams params;

            final int height = Math.max(mContext.getResources().getDisplayMetrics().widthPixels,mContext.getResources().getDisplayMetrics().heightPixels);
            final int width = Math.min(mContext.getResources().getDisplayMetrics().widthPixels,mContext.getResources().getDisplayMetrics().heightPixels);

            ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(mContext, R.style.AppBaseTheme);
            windowView = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.mupdf_window_manager, null);
            close = windowView.findViewById(R.id.close);
            imageButton = windowView.findViewById(R.id.close_btn);
            imageView = windowView.findViewById(R.id.image);
            imageView.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap,width/3,height/3));

            windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

            int flag;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                flag = WindowManager.LayoutParams.TYPE_PHONE;
            }
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    flag,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.format = PixelFormat.RGBA_8888;
            // 设置悬浮框的宽高
            params.width = width/3;
            params.height = height/3;
            params.gravity = Gravity.LEFT;
            params.x = 50;
            params.y = 100;
            // 设置悬浮框的Touch监听
            windowView.setOnTouchListener(new View.OnTouchListener() {
                //保存悬浮框最后位置的变量
                int lastX, lastY;
                int paramX, paramY;
                boolean isMoved = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = (int) event.getRawX();
                            lastY = (int) event.getRawY();
                            paramX = params.x;
                            paramY = params.y;
                            isMoved = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            int dx = (int) event.getRawX() - lastX;
                            int dy = (int) event.getRawY() - lastY;
                            params.x = params.gravity == Gravity.LEFT?paramX + dx:paramX - dx;
                            params.y = paramY + dy;
                            // 更新悬浮窗位置
                            windowManager.updateViewLayout(windowView, params);

                            if(Math.abs(dx) > 10 || Math.abs(dy) > 10)
                                isMoved = true;

                            break;
                        case MotionEvent.ACTION_UP:
                            //只有移动时才会出现关闭按钮，否则点击直接进入app
                            if(isMoved){
                                if(close.getVisibility() == View.GONE){
                                    slideDownToVisible(close);
                                }else {
                                    slideUpToHide(close);
                                }
                            }else {
                                //TODO: 点击悬浮窗直接进入APP
                            }

                            if(params.x > mContext.getResources().getDisplayMetrics().widthPixels/3){
                                params.gravity = params.gravity == Gravity.LEFT? Gravity.RIGHT : Gravity.LEFT;
                            }

                            ValueAnimator valueAnimator = ValueAnimator.ofInt(params.x,0);
                            valueAnimator.setDuration(300);
                            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    params.x = (int) animation.getAnimatedValue();
                                    windowManager.updateViewLayout(windowView, params);
                                }
                            });
                            valueAnimator.start();
                            break;

                    }
                    return true;
                }
            });

            // 关闭悬浮窗
            imageButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View view2) {
                    closeWindowView();
                }
            });

            windowManager.addView(windowView, params);
        }catch (Exception e){
        }
    }

    //向下滑动以显示
    static void slideDownToVisible(final ViewAnimator v){
        Animation anim = new TranslateAnimation(0,0, -v.getHeight(),0);
        anim.setDuration(200);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationEnd(Animation animation) {}
        });
        v.startAnimation(anim);
    }

    //向上滑动以隐藏
    static void slideUpToHide(final ViewAnimator v){
        if(v.getVisibility() == View.GONE){
            return;
        }
        Animation anim = new TranslateAnimation(0,0,0, -v.getHeight());
        anim.setDuration(200);
        anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {}
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationEnd(Animation animation) {
                v.setVisibility(View.GONE);
            }
        });
        v.startAnimation(anim);
    }
}
