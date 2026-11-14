package com.nth.desktop3d;

/** A downward pull at the top closes the drawer; scrolling and held drags retain priority. */
@android.annotation.SuppressLint("ViewConstructor")
final class SwipeDismissGrid extends android.widget.GridView {
    private final Runnable close;private float x,y;private boolean dismiss,top;
    SwipeDismissGrid(android.content.Context c,Runnable close){super(c);this.close=close;}
    @Override public boolean onInterceptTouchEvent(android.view.MotionEvent e){if(e.getActionMasked()==0){x=e.getX();y=e.getY();top=!canScrollVertically(-1);dismiss=false;}if(e.getActionMasked()==2&&top&&e.getY()-y>Ui.dp(getContext(),45)&&e.getY()-y>Math.abs(e.getX()-x)*1.4f){dismiss=true;return true;}return super.onInterceptTouchEvent(e);}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){if(dismiss){if(e.getActionMasked()==1){dismiss=false;post(close);}else if(e.getActionMasked()==3)dismiss=false;return true;}return super.onTouchEvent(e);}
}
