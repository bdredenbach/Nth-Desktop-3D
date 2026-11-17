package com.nth.desktop3d;

import android.app.Activity;
import android.view.*;
import android.widget.*;

/** A real single-card surface: child taps/scrolling remain native; hold moves or opens its menu. */
@android.annotation.SuppressLint("ViewConstructor")
final class DesktopCardView extends HoldDragFrame {
    final LauncherStore.Item item;private final CrystalCards content;private boolean active,disposed;
    DesktopCardView(Activity activity,LauncherStore store,LauncherStore.Item item,CrystalCards.Host host,HoldDragFrame.Start start,Runnable menu){
        super(activity,start,menu);this.item=item;setTag("card:"+item.id);setContentDescription(item.label+" desktop card. Hold to move, resize or remove.");setCameraDistance(Ui.dp(activity,2400));setBackground(Ui.crystal(activity,22,false));setElevation(new CrystalPreferences(activity).shadows()?Ui.dp(activity,9):0);setClipToOutline(true);setPadding(Ui.dp(activity,3),Ui.dp(activity,3),Ui.dp(activity,3),Ui.dp(activity,3));
        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);content=new CrystalCards(activity,store,host,"",CardLayout.type(item.key));scroll.addView(content,new ScrollView.LayoutParams(-1,-2));addView(scroll,new LayoutParams(-1,-1));
    }
    void active(boolean value){if(disposed||active==value)return;active=value;if(value)content.refresh();else content.pause();}
    void refresh(){if(active&&!disposed)content.refresh();}
    void tilt(float x,float y,boolean steady){setRotationX(steady?0:-y*2);setRotationY(steady?0:x*3);}
    void dispose(){if(disposed)return;disposed=true;content.dispose();}
    @Override protected void onDetachedFromWindow(){dispose();super.onDetachedFromWindow();}
}
