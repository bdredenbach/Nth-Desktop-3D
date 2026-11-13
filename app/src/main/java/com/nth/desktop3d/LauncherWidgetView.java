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
    private boolean armed,moving,eligible;
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
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);
        if(w<=0||h<=0)return;
        post(()->{
            float density=getResources().getDisplayMetrics().density;
            android.os.Bundle options=new android.os.Bundle();
            options.putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY,android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN);
            // Provider dimensions are already pixels. Only the options bundle is expressed in dp.
            try{updateAppWidgetSize(options,Math.round(w/density),Math.round(h/density),Math.round(w/density),Math.round(h/density));}
            catch(RuntimeException error){android.util.Log.e("NthWidgets","Could not report widget size",error);}
        });
    }
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
