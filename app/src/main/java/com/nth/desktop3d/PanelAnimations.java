package com.nth.desktop3d;

import android.graphics.Canvas;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;

/** The same opening pose drives real panels and their settings preview. */
final class PanelAnimations {
    static final int STACK=3;
    static final int PHYSICAL=7;
    static final String[] FOLDERS={"Rising Cards","Radial Arc","Floating Quick Actions","3D Stack","Minimal Context","Zoom","None","Physical folder"};
    static float[] pose(int style,float progress){
        float p=Math.max(0,Math.min(1,progress)),q=1-p;
        if(style==PHYSICAL){float e=FolderPose.expand(p);return new float[]{FolderPose.panelAlpha(p),.22f+.78f*e,(1-e)*.15f,-12*(1-e),0};}
        if(style==6)return new float[]{1,1,0,0,0};
        return new float[]{p,1-q*(style==2?.15f:style==5?.25f:0),style==0?q*.08f:0,style==0?-13*q:style==3?-28*q:style==4?-4*q:0,style==1?-32*q:0};
    }
    static void apply(View view,int style,float progress){float[] f=pose(style,progress);view.setAlpha(f[0]);view.setScaleX(f[1]);view.setScaleY(f[1]);view.setTranslationY(f[2]*view.getHeight());view.setRotationX(f[3]);view.setRotationY(f[4]);}
    static void canvas(Canvas canvas,int style,float progress,float w,float h){float[] f=pose(style,progress);canvas.saveLayerAlpha(0,0,w,h,Math.round(f[0]*255));canvas.translate(0,h*f[2]);canvas.scale(f[1],f[1],w/2,h*.65f);Camera camera=new Camera();Matrix matrix=new Matrix();camera.rotateX(f[3]);camera.rotateY(f[4]);camera.getMatrix(matrix);matrix.preTranslate(-w/2,-h*.65f);matrix.postTranslate(w/2,h*.65f);canvas.concat(matrix);}
    static void play(View view,int style,boolean reduced){view.animate().cancel();if(reduced||style==6){apply(view,style,1);return;}view.setPivotX(view.getWidth()/2f);view.setPivotY(view.getHeight()*.65f);android.animation.ValueAnimator animator=android.animation.ValueAnimator.ofFloat(0,1);animator.setDuration(360);animator.setInterpolator(new android.view.animation.DecelerateInterpolator());animator.addUpdateListener(a->{if(view.isAttachedToWindow())apply(view,style,(float)a.getAnimatedValue());});animator.start();}
}
