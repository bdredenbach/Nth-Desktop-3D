package com.nth.desktop3d;

import android.content.Context;
import android.appwidget.*;
import android.view.*;
import android.widget.*;

/** A fixed set of content spaces; widget touches continue to reach their provider. */
final class DockView extends LinearLayout {
    interface Host {void open(LauncherStore.Item item);void space(int index);void restore();}
    private final LauncherStore store;private final AppCatalog catalog;private final LauncherWidgetHost widgets;private final Host host;
    DockView(Context c,LauncherStore s,AppCatalog a,LauncherWidgetHost widgets,Host h){super(c);store=s;catalog=a;this.widgets=widgets;host=h;setOrientation(HORIZONTAL);setGravity(Gravity.CENTER);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int count=getChildCount(),width=android.view.View.MeasureSpec.getSize(widthSpec);if(count>0&&width>0)for(int i=0;i<count;i++){android.view.ViewGroup frame=(android.view.ViewGroup)getChildAt(i);if(frame.getChildCount()>0&&frame.getChildAt(0) instanceof IconTile)((IconTile)frame.getChildAt(0)).fitCell(Math.max(1,width/count-Ui.dp(getContext(),6)),Ui.dp(getContext(),store.preferences.getInt("dock_space_height",96)));}
        super.onMeasure(widthSpec,heightSpec);
    }
    void rebuild(){removeAllViews();widgets.releaseViews();boolean visible=store.preferences.getBoolean("dock_content",false);setVisibility(visible?VISIBLE:GONE);if(!visible)return;
        int count=store.preferences.getInt("dock_space_count",3),height=Ui.dp(getContext(),store.preferences.getInt("dock_space_height",96));
        for(int i=0;i<Math.min(DockStore.MAX,count);i++){final int index=i;DockStore.Slot slot=store.dock.slots.get(i);FrameLayout frame=new FrameLayout(getContext());frame.setTag("dock-space-"+i);LayoutParams lp=new LayoutParams(0,height,1);lp.setMargins(Ui.dp(getContext(),3),0,Ui.dp(getContext(),3),Ui.dp(getContext(),6));addView(frame,lp);View content;
            if(slot.item!=null){IconTile tile=new IconTile(getContext(),slot.item.label,catalog.icon(slot.item),slot.item.key);tile.fitCell(Ui.dp(getContext(),64),height);tile.setOnClickListener(v->host.open(slot.item));content=tile;}
            else if(slot.widget()){
                AppWidgetProviderInfo info=AppWidgetManager.getInstance(getContext()).getAppWidgetInfo(slot.widgetId);
                if(info!=null){LauncherWidgetView view=(LauncherWidgetView)widgets.createView(getContext(),slot.widgetId,info);view.padding(store.preferences.getBoolean("widget_padding_"+slot.widgetId,false));view.setGestures(new LauncherWidgetView.Gestures(){public void start(){}public void move(float x,float y){}public void end(boolean cancel){if(!cancel)host.space(index);}public void menu(){host.space(index);}});content=view;}
                else content=Ui.button(getContext(),"Restore\n"+slot.label,host::restore);
            }else content=Ui.button(getContext(),"+",()->host.space(index));
            content.setOnLongClickListener(v->{host.space(index);return true;});frame.addView(content,new FrameLayout.LayoutParams(-1,-1));frame.setContentDescription("Dock space "+(i+1));
        }
    }
}
