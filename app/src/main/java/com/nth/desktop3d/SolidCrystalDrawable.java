package com.nth.desktop3d;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Translucent optical volume with an edge-to-edge face and rounded open sides. */
final class SolidCrystalDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF face=new RectF(),inner=new RectF();
    private final Path clip=new Path(),reflection=new Path(),lip=new Path();
    private final float density,radius;
    private final boolean selected;
    private final int material,coreAlpha;
    private Shader body,bevel,shine,edge,rightRoll,bottomRoll;
    private int alpha=255;
    private final SceneLighting.Cover sceneLight;
    SolidCrystalDrawable(Context c,int radius,boolean selected){
        density=c.getResources().getDisplayMetrics().density;this.radius=radius*density;sceneLight=new SceneLighting.Cover(c,this);
        this.selected=selected;CrystalPreferences prefs=new CrystalPreferences(c);material=prefs.material();coreAlpha=Math.max(0,Math.min(255,255*(100-prefs.transparency())/100));
    }
    @Override protected void onBoundsChange(Rect b){
        face.set(b);
        float w=face.width(),h=face.height();if(w<=0||h<=0)return;
        float r=Math.min(radius,Math.min(w,h)*.5f);
        clip.reset();clip.addRoundRect(face,r,r,Path.Direction.CW);
        int top=selected?0xFF315C78:material==1?0xFF526F84:material==3?0xFF153A56:0xFF28465E;
        body=new LinearGradient(face.left,face.top,face.right,face.bottom,
            new int[]{0xFF729BAF,top,0xFF12283E,0xFF071626,0xFF1D4159},new float[]{0,.13f,.48f,.85f,1},Shader.TileMode.CLAMP);
        bevel=new LinearGradient(face.left,face.top,face.right,face.bottom,
            new int[]{0xF0F1FCFF,0x904385A7,0xFF061724,0xA51E789F,0xD6CEF6FF},new float[]{0,.18f,.48f,.77f,1},Shader.TileMode.CLAMP);
        shine=new LinearGradient(face.left,face.top,face.left+w*.62f,face.top+h*.8f,
            new int[]{0x00FFFFFF,0x00FFFFFF,0x40E6F6FF,0x05BCDFFF,0x00000000},new float[]{0,.22f,.29f,.46f,1},Shader.TileMode.CLAMP);
        edge=new LinearGradient(face.left,face.top,face.right,face.bottom,
            new int[]{0xCCEFFCFF,0xA0B4E8FF,0x293AA4CE,material==2?0xB3AA99F2:0xA376C2E4,0xD67DE0F7},null,Shader.TileMode.CLAMP);
        float roll=Math.min(density*8,Math.min(w,h)*.19f);
        rightRoll=new LinearGradient(face.right-roll,0,face.right,0,new int[]{0x00081929,0x18051624,0x5003101F,0x407BB9CE},new float[]{0,.45f,.82f,1},Shader.TileMode.CLAMP);
        bottomRoll=new LinearGradient(0,face.bottom-roll,0,face.bottom,new int[]{0x00081929,0x20020B17,0x50051626,0x409AE5ED},new float[]{0,.45f,.84f,1},Shader.TileMode.CLAMP);
        reflection.reset();reflection.moveTo(face.left+w*.06f,face.top);
        reflection.lineTo(face.left+w*.44f,face.top);
        reflection.cubicTo(face.left+w*.27f,face.top+h*.18f,face.left+w*.22f,face.top+h*.39f,face.left+w*.16f,face.top+h*.55f);
        reflection.lineTo(face.left+w*.07f,face.top+h*.60f);
        reflection.cubicTo(face.left+w*.15f,face.top+h*.30f,face.left+w*.11f,face.top+h*.12f,face.left+w*.06f,face.top);reflection.close();
    }
    @Override public void draw(Canvas c){
        if(face.isEmpty()||body==null)return;
        float r=Math.min(radius,Math.min(face.width(),face.height())*.5f),rim=Math.min(density*4,Math.min(face.width(),face.height())*.075f);
        int save=c.save();c.clipPath(clip);paint.setAlpha(alpha);paint.setStyle(Paint.Style.FILL);
        paint.setShader(body);paint.setAlpha(alpha*coreAlpha/255);c.drawRect(face,paint);paint.setAlpha(alpha);
        paint.setShader(shine);c.drawRect(face,paint);
        paint.setShader(null);paint.setColor(0x12EFFAFF);paint.setAlpha(0x12*alpha/255);c.drawPath(reflection,paint);
        // The right and lower sides roll through shaded volume; no rectangular outline there.
        paint.setAlpha(alpha);paint.setShader(rightRoll);c.drawRect(face,paint);paint.setShader(bottomRoll);c.drawRect(face,paint);
        inner.set(face);inner.inset(rim*.65f,rim*.65f);float ir=Math.max(0,r-rim*.65f);
        lip.reset();lip.moveTo(inner.left,inner.bottom-rim);lip.lineTo(inner.left,inner.top+ir);lip.quadTo(inner.left,inner.top,inner.left+ir,inner.top);lip.lineTo(inner.right-rim,inner.top);
        paint.setStyle(Paint.Style.STROKE);paint.setShader(bevel);paint.setStrokeWidth(rim*1.25f);paint.setStrokeCap(Paint.Cap.ROUND);c.drawPath(lip,paint);
        paint.setShader(edge);paint.setStrokeWidth(Math.max(1,density*.65f));c.drawPath(lip,paint);
        if(selected){paint.setShader(null);paint.setColor(0xFFE3FBFF);paint.setAlpha(alpha);c.drawLine(face.left+r,face.top+rim,face.right-r,face.top+rim,paint);}
        sceneLight.draw(c,face,alpha,1);c.restoreToCount(save);paint.setShader(null);paint.setStyle(Paint.Style.FILL);paint.setAlpha(alpha);
    }
    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    @Override public void getOutline(Outline o){o.setRoundRect(getBounds(),Math.min(radius,Math.min(getBounds().width(),getBounds().height())*.5f));o.setAlpha(alpha/255f*.7f);}
}
