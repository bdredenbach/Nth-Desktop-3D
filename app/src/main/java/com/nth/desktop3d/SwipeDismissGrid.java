package com.nth.desktop3d;

/** A downward pull at the top closes the drawer; scrolling and held drags retain priority. */
@android.annotation.SuppressLint("ViewConstructor")
final class SwipeDismissGrid extends android.widget.GridView {
    private final Runnable close;private float x,y;private boolean dismiss,top;
    SwipeDismissGrid(android.content.Context c,Runnable close){super(c);this.close=close;}
    private void track(android.view.MotionEvent e){if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();top=!canScrollVertically(-1);dismiss=false;}if(e.getActionMasked()==android.view.MotionEvent.ACTION_MOVE&&top&&e.getY()-y>Ui.dp(getContext(),45)&&e.getY()-y>Math.abs(e.getX()-x)*1.4f)dismiss=true;}
    @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){track(e);return dismiss||super.onInterceptTouchEvent(e);}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){boolean before=dismiss;track(e);if(dismiss){if(!before){android.view.MotionEvent cancel=android.view.MotionEvent.obtain(e);cancel.setAction(android.view.MotionEvent.ACTION_CANCEL);super.onTouchEvent(cancel);cancel.recycle();}if(e.getActionMasked()==android.view.MotionEvent.ACTION_UP){dismiss=false;post(close);}else if(e.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)dismiss=false;return true;}return super.onTouchEvent(e);}
}
