package com.nth.desktop3d;

import android.content.Context;
import android.view.*;
import android.widget.FrameLayout;

/** Native child taps/scrolling, a stationary hold menu, and one continuous system drag. */
@android.annotation.SuppressLint("ViewConstructor")
class HoldDragFrame extends FrameLayout {
    interface Start {boolean begin(View view,float anchorX,float anchorY);}
    private final Start start;private final Runnable menu;private final int slop;
    private float rawX,rawY,anchorX,anchorY;private boolean eligible,armed;
    private final Runnable hold=()->{if(!eligible||!isAttachedToWindow())return;armed=true;MotionEvent cancel=MotionEvent.obtain(android.os.SystemClock.uptimeMillis(),android.os.SystemClock.uptimeMillis(),MotionEvent.ACTION_CANCEL,0,0,0);super.dispatchTouchEvent(cancel);cancel.recycle();performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);};
    HoldDragFrame(Context context,Start start,Runnable menu){super(context);this.start=start;this.menu=menu;setClipChildren(false);setClipToPadding(false);slop=ViewConfiguration.get(context).getScaledTouchSlop();setOnLongClickListener(v->{menu.run();return true;});}
    @Override public boolean dispatchTouchEvent(MotionEvent event){switch(event.getActionMasked()){
        case MotionEvent.ACTION_DOWN:removeCallbacks(hold);rawX=event.getRawX();rawY=event.getRawY();anchorX=event.getX();anchorY=event.getY();armed=false;eligible=true;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);postDelayed(hold,ViewConfiguration.getLongPressTimeout());break;
        case MotionEvent.ACTION_MOVE:float distance=(float)Math.hypot(event.getRawX()-rawX,event.getRawY()-rawY);if(armed){if(distance>slop){eligible=armed=false;removeCallbacks(hold);start.begin(this,anchorX,anchorY);}return true;}if(distance>slop){eligible=false;removeCallbacks(hold);if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}break;
        case MotionEvent.ACTION_UP:removeCallbacks(hold);eligible=false;if(armed){armed=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);menu.run();return true;}break;
        case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_CANCEL:eligible=armed=false;removeCallbacks(hold);break;
    }return super.dispatchTouchEvent(event);}
    @Override protected void onDetachedFromWindow(){eligible=armed=false;removeCallbacks(hold);super.onDetachedFromWindow();}
}
