package com.nth.desktop3d;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;

/** Vertical list or bounded horizontal pages. Held icon drags retain priority. */
@android.annotation.SuppressLint("ViewConstructor")
final class SwipeDismissGrid extends GridView {
    private final Runnable close;private final boolean horizontal;
    private float x,y;private boolean dismiss,top,paging;
    private int page,capacity=1,columns=3,rowHeight;
    private ListAdapter source;private BaseAdapter pages;
    private final Paint indicator=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DataSetObserver observer=new DataSetObserver(){
        @Override public void onChanged(){page=0;refresh();}
        @Override public void onInvalidated(){page=0;if(pages!=null)pages.notifyDataSetInvalidated();}
    };
    SwipeDismissGrid(Context c,Runnable close){this(c,close,false);}
    SwipeDismissGrid(Context c,Runnable close,boolean horizontal){
        super(c);this.close=close;this.horizontal=horizontal;rowHeight=Ui.dp(c,132);
        if(horizontal){setVerticalScrollBarEnabled(false);setOverScrollMode(OVER_SCROLL_NEVER);}
    }
    int page(){return page;}
    int pageCount(){return DrawerPaging.pages(source==null?0:source.getCount(),capacity);}
    int sourcePosition(int position){return horizontal?page*capacity+position:position;}
    @Override public void setNumColumns(int value){columns=Math.max(1,value);super.setNumColumns(value);resizePages();}
    @Override public void setAdapter(ListAdapter adapter){
        if(source!=null&&pages!=null)source.unregisterDataSetObserver(observer);source=adapter;pages=null;page=0;
        if(!horizontal||adapter==null){super.setAdapter(adapter);return;}
        pages=new BaseAdapter(){
            public int getCount(){return DrawerPaging.size(source.getCount(),page,capacity);}
            public Object getItem(int n){return source.getItem(sourcePosition(n));}
            public long getItemId(int n){return source.getItemId(sourcePosition(n));}
            @Override public int getViewTypeCount(){return source.getViewTypeCount();}
            @Override public int getItemViewType(int n){return source.getItemViewType(sourcePosition(n));}
            @Override public boolean areAllItemsEnabled(){return source.areAllItemsEnabled();}
            @Override public boolean isEnabled(int n){return source.isEnabled(sourcePosition(n));}
            public View getView(int n,View old,ViewGroup parent){
                View view=source.getView(sourcePosition(n),old,parent);
                if(view instanceof IconTile)((IconTile)view).fitCell(Math.max(Ui.dp(getContext(),30),(getWidth()-getPaddingLeft()-getPaddingRight()-getHorizontalSpacing()*(columns-1))/columns),rowHeight);
                view.setLayoutParams(new AbsListView.LayoutParams(-1,rowHeight));return view;
            }
        };
        source.registerDataSetObserver(observer);super.setAdapter(pages);resizePages();
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);resizePages();}
    private void resizePages(){
        if(!horizontal)return;
        int usable=Math.max(1,getHeight()-getPaddingTop()-getPaddingBottom()-Ui.dp(getContext(),24));
        rowHeight=Math.min(Ui.dp(getContext(),132),usable);
        int next=DrawerPaging.capacity(columns,usable,rowHeight,getVerticalSpacing());
        if(next!=capacity){capacity=next;refresh();}
    }
    private void refresh(){
        if(!horizontal||pages==null)return;page=DrawerPaging.clamp(page,source.getCount(),capacity);
        pages.notifyDataSetChanged();setSelection(0);
        setContentDescription("App drawer, page "+(page+1)+" of "+pageCount());invalidate();
    }
    private boolean turn(int delta){
        if(!horizontal||source==null)return false;
        int next=DrawerPaging.clamp(page+delta,source.getCount(),capacity);
        if(next==page)return false;page=next;refresh();announceForAccessibility("Page "+(page+1)+" of "+pageCount());return true;
    }
    private void track(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();top=!canScrollVertically(-1);dismiss=paging=false;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE){
            float dx=e.getX()-x,dy=e.getY()-y;
            if(horizontal&&Math.abs(dx)>Ui.dp(getContext(),24)&&Math.abs(dx)>Math.abs(dy)*1.4f)paging=true;
            if(!paging&&top&&dy>Ui.dp(getContext(),45)&&dy>Math.abs(dx)*1.4f)dismiss=true;
        }
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent e){track(e);return paging||dismiss||super.onInterceptTouchEvent(e);}
    @Override public boolean onTouchEvent(MotionEvent e){
        boolean before=paging||dismiss;track(e);
        if(paging||dismiss){
            if(!before){MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.onTouchEvent(cancel);cancel.recycle();}
            if(e.getActionMasked()==MotionEvent.ACTION_UP){
                if(paging){float delta=e.getX()-x;if(Math.abs(delta)>Ui.dp(getContext(),45))turn((delta<0?1:-1)*(getLayoutDirection()==LAYOUT_DIRECTION_RTL?-1:1));}
                else post(close);paging=dismiss=false;
            }else if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)paging=dismiss=false;
            return true;
        }
        return super.onTouchEvent(e);
    }
    @Override protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);if(!horizontal||pageCount()<2)return;
        indicator.setColor(Ui.MUTED);indicator.setTextSize(Ui.dp(getContext(),11));indicator.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Page "+(page+1)+" of "+pageCount(),getWidth()/2f,getHeight()-Ui.dp(getContext(),6),indicator);
    }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);if(!horizontal)return;
        if(page+1<pageCount())info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        if(page>0)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args){
        if(horizontal&&action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)return turn(1);
        if(horizontal&&action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)return turn(-1);
        return super.performAccessibilityAction(action,args);
    }
}
