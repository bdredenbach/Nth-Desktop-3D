package com.nth.desktop3d;

import android.animation.*;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Reversible presentation only: canonical items, grid order and native listeners stay intact. */
final class FolderMotion {
    private static final int MAX_TILES=24;
    static final class Origin {
        final RectF bounds;final Bitmap snapshot;final IconTile tile;
        Origin(RectF bounds,Bitmap snapshot,IconTile tile){this.bounds=bounds;this.snapshot=snapshot;this.tile=tile;}
    }
    private static final class Tile {
        final IconTile view;final float x,y;
        Tile(IconTile view,float x,float y){this.view=view;this.x=x;this.y=y;}
    }
    private final FrameLayout root;private final LinearLayout panel;private final GridLayout shelf;private final int columns;
    private final Origin origin;private final List<Tile> tiles=new ArrayList<>();private final Map<IconTile,Float> hidden=new IdentityHashMap<>();
    private final LidView lid;private final RectF anchor=new RectF(),bounds=new RectF();private ValueAnimator animator;
    private float progress;private int epoch;private boolean disposed,closing;
    static Origin capture(FrameLayout root,IconTile source,Drawable folderIcon){
        Context context=root.getContext();RectF bounds=sourceBounds(root,source);int size=192;Bitmap snapshot;
        if(bounds!=null&&source.getChildCount()>0){
            View plinth=source.getChildAt(0);float scale=Math.min(1,256f/Math.max(plinth.getWidth(),plinth.getHeight()));int w=Math.max(1,Math.round(plinth.getWidth()*scale)),h=Math.max(1,Math.round(plinth.getHeight()*scale));snapshot=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(snapshot);canvas.scale(scale,scale);plinth.draw(canvas);
        }else{
            float s=Ui.dp(context,64),cx=root.getWidth()*.5f,cy=root.getHeight()*.74f;bounds=new RectF(cx-s/2,cy-s/2,cx+s/2,cy+s/2);snapshot=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(snapshot);IconShellDrawable shell=new IconShellDrawable(context,true);shell.setBounds(0,0,size,size);shell.draw(canvas);
            if(folderIcon!=null){Rect old=folderIcon.copyBounds();folderIcon.setBounds(24,24,size-24,size-24);folderIcon.draw(canvas);folderIcon.setBounds(old);}shell.drawCover(canvas);
        }
        return new Origin(bounds,snapshot,source);
    }
    private static RectF sourceBounds(FrameLayout root,IconTile tile){
        if(tile==null||!tile.isAttachedToWindow()||tile.getChildCount()==0)return null;View plinth=tile.getChildAt(0);Rect visible=new Rect();if(plinth.getWidth()==0||plinth.getHeight()==0||!plinth.getGlobalVisibleRect(visible))return null;int[] location=new int[2];root.getLocationOnScreen(location);return new RectF(visible.left-location[0],visible.top-location[1],visible.right-location[0],visible.bottom-location[1]);
    }
    FolderMotion(FrameLayout root,LinearLayout panel,GridLayout shelf,int columns,Origin origin){
        this.root=root;this.panel=panel;this.shelf=shelf;this.columns=columns;this.origin=origin;anchor.set(origin.bounds);lid=new LidView(root.getContext());lid.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);lid.setClickable(false);lid.setFocusable(false);root.addView(lid,new FrameLayout.LayoutParams(-1,-1));panel.setAlpha(0);
    }
    void open(){if(disposed||closing)return;collect();hide(origin.tile);apply(0);animate(1,560,null);}
    boolean closing(){return closing;}
    void close(IconTile target,Runnable complete){
        if(disposed)return;if(closing)return;closing=true;RectF current=sourceBounds(root,target);if(current!=null){anchor.set(current);hide(target);}collect();animate(0,280,complete);
    }
    private void hide(IconTile tile){if(tile!=null&&!hidden.containsKey(tile)){hidden.put(tile,tile.getAlpha());tile.setAlpha(0);}}
    private RectF panelBounds(){bounds.set(panel.getLeft(),panel.getTop(),panel.getRight(),panel.getBottom());return bounds;}
    private void collect(){
        for(Tile item:tiles)reset(item.view);tiles.clear();
        for(int i=0;i<shelf.getChildCount()&&tiles.size()<MAX_TILES;i++){View child=shelf.getChildAt(i);if(!(child instanceof IconTile))continue;float x=child.getWidth()*.5f,y=child.getHeight()*.5f;View current=child;
            while(current!=panel&&current.getParent() instanceof View){View parent=(View)current.getParent();x+=current.getLeft()-parent.getScrollX();y+=current.getTop()-parent.getScrollY();current=parent;}
            if(y+child.getHeight()/2f>=0&&y-child.getHeight()/2f<=panel.getHeight())tiles.add(new Tile((IconTile)child,x,y));
        }
    }
    private void stop(){epoch++;if(animator!=null){ValueAnimator previous=animator;animator=null;previous.cancel();}}
    private void animate(float target,int duration,Runnable complete){
        stop();int token=epoch;animator=ValueAnimator.ofFloat(progress,target);animator.setDuration(Math.max(120,Math.round(duration*Math.abs(target-progress))));animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener(a->{if(!disposed&&epoch==token)apply((float)a.getAnimatedValue());});
        animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){if(disposed||epoch!=token)return;animator=null;apply(target);if(target==1)lid.setVisibility(View.INVISIBLE);if(complete!=null)complete.run();}});lid.setVisibility(View.VISIBLE);animator.start();
    }
    private void apply(float value){
        progress=FolderPose.clamp(value);RectF bounds=panelBounds();if(bounds.isEmpty())return;float expand=FolderPose.expand(progress);
        panel.setPivotX(panel.getWidth()/2f);panel.setPivotY(panel.getHeight()/2f);panel.setScaleX(FolderPose.mix(Math.max(.05f,anchor.width()/bounds.width()),1,expand));panel.setScaleY(FolderPose.mix(Math.max(.05f,anchor.height()/bounds.height()),1,expand));panel.setTranslationX((anchor.centerX()-bounds.centerX())*(1-expand));panel.setTranslationY((anchor.centerY()-bounds.centerY())*(1-expand));panel.setRotationX(-12*(1-expand));panel.setAlpha(FolderPose.panelAlpha(progress));
        for(int i=0;i<tiles.size();i++){Tile item=tiles.get(i);FolderPose.Item pose=FolderPose.item(progress,i,columns);item.view.setAlpha(pose.alpha);item.view.setScaleX(pose.scale);item.view.setScaleY(pose.scale);item.view.setRotation(pose.rotation);item.view.setRotationX(pose.tilt);item.view.setTranslationX((panel.getWidth()*.5f-item.x)*(1-pose.progress)*.65f);item.view.setTranslationY((panel.getHeight()*.82f-item.y)*(1-pose.progress)*.75f);}
        lid.invalidate();
    }
    private static void reset(View view){view.setAlpha(1);view.setScaleX(1);view.setScaleY(1);view.setTranslationX(0);view.setTranslationY(0);view.setRotation(0);view.setRotationX(0);}
    void dispose(){
        if(disposed)return;disposed=true;stop();reset(panel);for(Tile item:tiles)reset(item.view);tiles.clear();for(Map.Entry<IconTile,Float> item:hidden.entrySet())item.getKey().setAlpha(item.getValue());hidden.clear();if(lid.getParent()==root)root.removeView(lid); // Removed display lists release their small snapshot naturally.
    }
    @android.annotation.SuppressLint("ViewConstructor")
    private final class LidView extends View {
        private final FolderLid material;
        LidView(Context context){super(context);material=new FolderLid(context);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);if(!disposed)material.draw(canvas,panelBounds(),anchor,progress,origin.snapshot);}
    }
}
