package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/** Full-border resize hit areas with corner grips; provider axes still apply. */
final class WorkspaceFrame extends FrameLayout {
    interface Resize {void start(int edge);void move(float dx,float dy);void end(boolean cancel);}
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF preview,selection;private boolean valid,editing;private int columns,cellHeight,top,edge,axes=3;private float sx,sy;private Resize resize;
    WorkspaceFrame(Context context){super(context);setClipChildren(false);setClipToPadding(false);}
    void metrics(int columns,int cellHeight,int top){this.columns=columns;this.cellHeight=cellHeight;this.top=top;}
    void preview(RectF bounds,boolean valid){preview=bounds;this.valid=valid;editing=bounds!=null;invalidate();}
    void select(RectF bounds,Resize resize){selection=bounds;this.resize=resize;invalidate();}
    void axes(int axes){this.axes=axes;invalidate();}boolean resizing(){return selection!=null;}
    @Override protected void dispatchDraw(Canvas canvas){super.dispatchDraw(canvas);
        if(editing&&columns>0&&cellHeight>0){paint.setColor(0xAAE4F9FF);paint.setStyle(Paint.Style.FILL);for(int y=top;y<getHeight();y+=cellHeight)for(int x=0;x<=columns;x++)canvas.drawCircle(x*getWidth()/(float)columns,y,Ui.dp(getContext(),2),paint);}
        if(preview!=null){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),2));paint.setColor(valid?0xFFE5FAFF:0xFFFF6B72);canvas.drawRoundRect(preview,Ui.dp(getContext(),9),Ui.dp(getContext(),9),paint);}
        if(selection!=null){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),2));paint.setColor(0xFFE5FAFF);canvas.drawRoundRect(selection,Ui.dp(getContext(),8),Ui.dp(getContext(),8),paint);paint.setStyle(Paint.Style.FILL);
            if((axes&1)!=0){grip(canvas,selection.left,selection.centerY(),false);grip(canvas,selection.right,selection.centerY(),false);}
            if((axes&2)!=0){grip(canvas,selection.centerX(),selection.top,true);grip(canvas,selection.centerX(),selection.bottom,true);}
            if(axes==3)for(float x:new float[]{selection.left,selection.right})for(float y:new float[]{selection.top,selection.bottom})canvas.drawCircle(Math.max(6,Math.min(getWidth()-6,x)),Math.max(6,y),Ui.dp(getContext(),6),paint);
        }
    }
    private void grip(Canvas c,float x,float y,boolean horizontal){float a=Ui.dp(getContext(),horizontal?11:4),b=Ui.dp(getContext(),horizontal?4:11);x=Math.max(a,Math.min(getWidth()-a,x));y=Math.max(b,y);c.drawRoundRect(x-a,y-b,x+a,y+b,Ui.dp(getContext(),4),Ui.dp(getContext(),4),paint);}
    private int edgeAt(float x,float y){
        if(selection==null)return 0;float hit=Ui.dp(getContext(),24);if(x<selection.left-hit||x>selection.right+hit||y<selection.top-hit||y>selection.bottom+hit)return 0;
        int result=0;if((axes&1)!=0){if(Math.abs(x-selection.left)<=hit)result|=1;else if(Math.abs(x-selection.right)<=hit)result|=2;}if((axes&2)!=0){if(Math.abs(y-selection.top)<=hit)result|=4;else if(Math.abs(y-selection.bottom)<=hit)result|=8;}return result;
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN){edge=edgeAt(event.getX(),event.getY());if(edge!=0&&resize!=null){sx=event.getRawX();sy=event.getRawY();getParent().requestDisallowInterceptTouchEvent(true);performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);resize.start(edge);return true;}if(selection!=null&&!selection.contains(event.getX(),event.getY()))select(null,null);}return false;}
    @Override public boolean onTouchEvent(MotionEvent event){if(edge==0||resize==null)return super.onTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_MOVE)resize.move(event.getRawX()-sx,event.getRawY()-sy);if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){resize.end(event.getActionMasked()==MotionEvent.ACTION_CANCEL);edge=0;getParent().requestDisallowInterceptTouchEvent(false);}return true;}
}
