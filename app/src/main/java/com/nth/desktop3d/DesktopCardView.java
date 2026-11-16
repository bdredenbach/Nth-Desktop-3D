package com.nth.desktop3d;

import android.app.Activity;
import android.view.*;
import android.widget.*;

/** A real single-card surface: child taps/scrolling remain native; hold moves or opens its menu. */
@android.annotation.SuppressLint("ViewConstructor")
final class DesktopCardView extends FrameLayout {
    final LauncherStore.Item item;private final CrystalCards content;private final LauncherWidgetView.Gestures gestures;
    private boolean active,disposed,armed,moving,eligible;private float downX,downY;private final int slop;
    private final Runnable hold=()->{if(!eligible||disposed)return;armed=true;MotionEvent cancel=MotionEvent.obtain(android.os.SystemClock.uptimeMillis(),android.os.SystemClock.uptimeMillis(),MotionEvent.ACTION_CANCEL,0,0,0);super.dispatchTouchEvent(cancel);cancel.recycle();getParent().requestDisallowInterceptTouchEvent(true);performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);};
    DesktopCardView(Activity activity,LauncherStore store,LauncherStore.Item item,CrystalCards.Host host,LauncherWidgetView.Gestures gestures){
        super(activity);this.item=item;this.gestures=gestures;slop=ViewConfiguration.get(activity).getScaledTouchSlop();setTag("card:"+item.id);setContentDescription(item.label+" desktop card. Hold to move, resize or remove.");setOnLongClickListener(v->{gestures.menu();return true;});setCameraDistance(Ui.dp(activity,2400));setBackground(Ui.crystal(activity,22,false));setElevation(new CrystalPreferences(activity).shadows()?Ui.dp(activity,9):0);setClipToOutline(true);setPadding(Ui.dp(activity,3),Ui.dp(activity,3),Ui.dp(activity,3),Ui.dp(activity,3));
        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);content=new CrystalCards(activity,store,host,"",CardLayout.type(item.key));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));addView(scroll,new LayoutParams(-1,-1));
    }
    void active(boolean value){if(disposed||active==value)return;active=value;if(value)content.refresh();else content.pause();}
    void refresh(){if(active&&!disposed)content.refresh();}
    void tilt(float x,float y,boolean steady){setRotationX(steady?0:-y*2);setRotationY(steady?0:x*3);}
    void dispose(){if(disposed)return;disposed=true;removeCallbacks(hold);content.dispose();}
    @Override public boolean dispatchTouchEvent(MotionEvent event){switch(event.getActionMasked()){
        case MotionEvent.ACTION_DOWN:removeCallbacks(hold);downX=event.getRawX();downY=event.getRawY();armed=moving=false;eligible=true;getParent().requestDisallowInterceptTouchEvent(true);postDelayed(hold,ViewConfiguration.getLongPressTimeout());break;
        case MotionEvent.ACTION_MOVE:float dx=event.getRawX()-downX,dy=event.getRawY()-downY;if(armed){if(!moving&&Math.hypot(dx,dy)>slop){moving=true;gestures.start();}if(moving)gestures.move(dx,dy);return true;}if(Math.hypot(dx,dy)>slop){eligible=false;removeCallbacks(hold);}break;
        case MotionEvent.ACTION_POINTER_DOWN:case MotionEvent.ACTION_CANCEL:removeCallbacks(hold);eligible=false;if(armed&&moving)gestures.end(true);reset();break;
        case MotionEvent.ACTION_UP:removeCallbacks(hold);eligible=false;if(armed){if(moving)gestures.end(false);else gestures.menu();reset();return true;}reset();break;
    }return super.dispatchTouchEvent(event);}
    private void reset(){armed=moving=false;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
    @Override protected void onDetachedFromWindow(){dispose();reset();super.onDetachedFromWindow();}
}
