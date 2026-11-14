package com.nth.desktop3d;

import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/** Direct widget hold/drag without a separate handle; ordinary taps and scrolling reach RemoteViews. */
final class LauncherWidgetView extends AppWidgetHostView {
    interface Gestures {void start();void move(float dx,float dy);void end(boolean cancel);void menu();}
    private Gestures gestures;
    private float downX,downY;
    private boolean armed,moving,eligible,paddingEnabled=true;
    private final int slop;
    private final Runnable hold=()->{
        if(!eligible||gestures==null)return;
        armed=true;
        MotionEvent cancel=MotionEvent.obtain(android.os.SystemClock.uptimeMillis(),android.os.SystemClock.uptimeMillis(),MotionEvent.ACTION_CANCEL,0,0,0);
        super.dispatchTouchEvent(cancel);cancel.recycle();
        getParent().requestDisallowInterceptTouchEvent(true);
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    };
    LauncherWidgetView(Context context){super(context);setClickable(true);slop=ViewConfiguration.get(context).getScaledTouchSlop();}
    void setGestures(Gestures gestures){this.gestures=gestures;}
    void padding(boolean enabled){paddingEnabled=enabled;android.appwidget.AppWidgetProviderInfo info=getAppWidgetInfo();android.graphics.Rect p=enabled&&info!=null?getDefaultPaddingForWidget(getContext(),info.provider,null):new android.graphics.Rect();setPadding(p.left,p.top,p.right,p.bottom);post(this::reportSize);}
    private void reportSize(){
        if(!isAttachedToWindow()||getWidth()<=0||getHeight()<=0)return;float density=getResources().getDisplayMetrics().density;android.os.Bundle options=new android.os.Bundle();options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
        int w=Math.max(1,Math.round((getWidth()-getPaddingLeft()-getPaddingRight())/density)),h=Math.max(1,Math.round((getHeight()-getPaddingTop()-getPaddingBottom())/density));
        options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,w);options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,w);options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,h);options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h);
        if(android.os.Build.VERSION.SDK_INT>=31){java.util.ArrayList<android.util.SizeF> sizes=new java.util.ArrayList<>();sizes.add(new android.util.SizeF(w,h));options.putParcelableArrayList(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_SIZES,sizes);}
        try{android.appwidget.AppWidgetManager.getInstance(getContext()).updateAppWidgetOptions(getAppWidgetId(),options);}catch(RuntimeException error){android.util.Log.e("NthWidgets","Could not report widget size",error);}
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);if(w>0&&h>0)post(this::reportSize);}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(hold);downX=event.getRawX();downY=event.getRawY();armed=moving=false;eligible=true;
                postDelayed(hold,ViewConfiguration.getLongPressTimeout());break;
            case MotionEvent.ACTION_MOVE:
                float dx=event.getRawX()-downX,dy=event.getRawY()-downY;
                if(armed&&gestures!=null){
                    if(!moving&&Math.hypot(dx,dy)>slop){moving=true;gestures.start();}
                    if(moving)gestures.move(dx,dy);
                    return true;
                }
                if(Math.hypot(dx,dy)>slop){eligible=false;removeCallbacks(hold);}break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(hold);eligible=false;
                if(armed){if(moving&&gestures!=null)gestures.end(true);reset();return true;}
                reset();break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(hold);eligible=false;
                if(armed&&gestures!=null){if(moving)gestures.end(false);else gestures.menu();reset();return true;}
                reset();break;
        }
        return super.dispatchTouchEvent(event);
    }
    private void reset(){armed=moving=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
    @Override protected void onDetachedFromWindow(){removeCallbacks(hold);eligible=false;reset();super.onDetachedFromWindow();}
}
