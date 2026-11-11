package com.nth.desktop3d;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** A measured shortcut plane: child positions never depend on a previous column count. */
final class ShortcutGrid extends ViewGroup {
    private int columns=1,cellHeight;
    ShortcutGrid(Context context){super(context);setClipToPadding(false);}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec);
        int available=Math.max(1,width-getPaddingLeft()-getPaddingRight());
        float fontScale=getResources().getConfiguration().fontScale;
        int minWidth=Ui.dp(getContext(),84*Math.max(1,fontScale*.8f));
        columns=Math.max(1,Math.min(8,available/Math.max(1,minWidth)));
        cellHeight=Ui.dp(getContext(),112+Math.max(0,fontScale-1)*30);
        int cellWidth=available/columns;
        for(int i=0;i<getChildCount();i++)getChildAt(i).measure(
            MeasureSpec.makeMeasureSpec(cellWidth,MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(cellHeight,MeasureSpec.EXACTLY));
        int rows=(getChildCount()+columns-1)/columns;
        int desired=getPaddingTop()+rows*cellHeight+getPaddingBottom();
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(desired,heightSpec));
    }
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int available=Math.max(1,r-l-getPaddingLeft()-getPaddingRight());
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);int column=i%columns,row=i/columns;
            if(getLayoutDirection()==LAYOUT_DIRECTION_RTL)column=columns-1-column;
            int left=getPaddingLeft()+column*available/columns;
            int right=getPaddingLeft()+(column+1)*available/columns;
            int top=getPaddingTop()+row*cellHeight;
            child.layout(left,top,right,top+cellHeight);
        }
    }
    @Override protected LayoutParams generateDefaultLayoutParams(){return new LayoutParams(-2,-2);}
}
