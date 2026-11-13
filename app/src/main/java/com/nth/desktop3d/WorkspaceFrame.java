package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/** Temporary grid/drop outline and four resize grips, drawn directly over the workspace. */
final class WorkspaceFrame extends FrameLayout {
    interface Resize {void start(int edge);void move(float dx,float dy);void end(boolean cancel);}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF preview,selection;
    private boolean valid,editing;
    private int columns,cellHeight,top,edge,axes=3;
    private float sx,sy;
    private Resize resize;
    WorkspaceFrame(Context context){super(context);setClipChildren(false);setClipToPadding(false);}
    void metrics(int columns,int cellHeight,int top){this.columns=columns;this.cellHeight=cellHeight;this.top=top;}
    void preview(RectF bounds,boolean valid){preview=bounds;this.valid=valid;editing=bounds!=null;invalidate();}
    void select(RectF bounds,Resize resize){selection=bounds;this.resize=resize;invalidate();}
    void axes(int axes){this.axes=axes;invalidate();}
    boolean resizing(){return selection!=null;}
    @Override protected void dispatchDraw(Canvas canvas){
        super.dispatchDraw(canvas);
        if(editing&&columns>0&&cellHeight>0){
            paint.setColor(0xAAE4F9FF);paint.setStyle(Paint.Style.FILL);
            for(int y=top;y<getHeight();y+=cellHeight)for(int x=0;x<=columns;x++)canvas.drawCircle(x*getWidth()/(float)columns,y,Ui.dp(getContext(),2),paint);
        }
        if(preview!=null){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),2));paint.setColor(valid?0xFFE5FAFF:0xFFFF6B72);canvas.drawRoundRect(preview,Ui.dp(getContext(),9),Ui.dp(getContext(),9),paint);}
        if(selection!=null){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),2));paint.setColor(0xFFE5FAFF);canvas.drawRoundRect(selection,Ui.dp(getContext(),8),Ui.dp(getContext(),8),paint);
            paint.setStyle(Paint.Style.FILL);float radius=Ui.dp(getContext(),5);
            if((axes&1)!=0){canvas.drawCircle(selection.left,selection.centerY(),radius,paint);canvas.drawCircle(selection.right,selection.centerY(),radius,paint);}
            if((axes&2)!=0){canvas.drawCircle(selection.centerX(),selection.top,radius,paint);canvas.drawCircle(selection.centerX(),selection.bottom,radius,paint);}
        }
    }
    private int edgeAt(float x,float y){
        if(selection==null)return 0;float hit=Ui.dp(getContext(),23);
        if((axes&1)!=0&&Math.abs(y-selection.centerY())<=hit){if(Math.abs(x-selection.left)<=hit)return 1;if(Math.abs(x-selection.right)<=hit)return 2;}
        if((axes&2)!=0&&Math.abs(x-selection.centerX())<=hit){if(Math.abs(y-selection.top)<=hit)return 3;if(Math.abs(y-selection.bottom)<=hit)return 4;}
        return 0;
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
            edge=edgeAt(event.getX(),event.getY());
            if(edge!=0&&resize!=null){sx=event.getRawX();sy=event.getRawY();getParent().requestDisallowInterceptTouchEvent(true);resize.start(edge);return true;}
            if(selection!=null&&!selection.contains(event.getX(),event.getY()))select(null,null);
        }
        return false;
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(edge==0||resize==null)return super.onTouchEvent(event);
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE)resize.move(event.getRawX()-sx,event.getRawY()-sy);
        if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){
            resize.end(event.getActionMasked()==MotionEvent.ACTION_CANCEL);edge=0;getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }
}
