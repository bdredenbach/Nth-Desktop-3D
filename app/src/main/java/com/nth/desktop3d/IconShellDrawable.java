package com.nth.desktop3d;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

/** Rounded optical glass with photograph-based reflections and a recessed folder tint. */
final class IconShellDrawable extends Drawable {
    private static final int SIZE=192;
    private static final LruCache<String,Bitmap> CACHE=new LruCache<>(24);
    private final SceneLighting.Cover sceneLight;
    private final Paint paint=new Paint(3),reflectionPaint=new Paint(3);
    private final Path clip=new Path();private final RectF face=new RectF();private final Matrix reflectionMatrix=new Matrix();
    private Bitmap image;private final int shape,material,transparency;private final boolean dynamic,shadows,folder;
    private int environmentKey=Integer.MIN_VALUE;
    private LinearGradient reflection;private int alpha=255;private float lightX,lightY;
    IconShellDrawable(Context context,boolean folder){
        CrystalPreferences prefs=new CrystalPreferences(context);int stored=context.getSharedPreferences("desktop3d",Context.MODE_PRIVATE).getInt(folder?"folder_shape":"icon_shape",folder?VisualDefaults.FOLDER_SHAPE:VisualDefaults.APP_SHAPE);
        shape=stored==2?2:stored==(folder?0:1)?1:0;dynamic=prefs.dynamicLighting();shadows=prefs.shadows();
        this.folder=folder;material=prefs.material();transparency=prefs.transparency();sceneLight=new SceneLighting.Cover(context,this);refreshImage();
    }
    private void refreshImage(){
        int environment=sceneLight.lighting.environmentKey();if(image!=null&&environmentKey==environment)return;
        String key=shape+":"+folder+":"+material+":"+transparency+":"+environment;
        synchronized(CACHE){
            Bitmap cached=CACHE.get(key);
            if(cached==null){
                int[] pixels=IconOptics.render(SIZE,shape,folder,material,transparency,sceneLight.lighting.environment(),128,128);
                cached=Bitmap.createBitmap(pixels,SIZE,SIZE,Bitmap.Config.ARGB_8888);CACHE.put(key,cached);
            }
            image=cached;environmentKey=environment;
        }
    }
    void light(float x,float y){x=dynamic?SceneMath.clamp(x,-1,1):0;y=dynamic?SceneMath.clamp(y,-1,1):0;if(Math.abs(x-lightX)<.008f&&Math.abs(y-lightY)<.008f)return;lightX=x;lightY=y;invalidateSelf();}
    @Override protected void onBoundsChange(Rect bounds){
        face.set(bounds);float inset=Math.min(face.width(),face.height())*.06f;face.inset(inset,inset);float radius=shape==1?Math.min(face.width(),face.height())*.5f:shape==2?Math.min(face.width(),face.height())*.012f:Math.min(face.width(),face.height())*.227f;
        clip.reset();clip.addRoundRect(face,radius,radius,Path.Direction.CW);
        reflection=new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,new int[]{0,0,0x32EAF8FF,0x04DBEEFF,0,0},new float[]{0,.15f,.27f,.40f,.49f,1},Shader.TileMode.CLAMP);
    }
    @Override public void draw(Canvas canvas){
        if(getBounds().isEmpty())return;refreshImage();paint.setAlpha(alpha);canvas.drawBitmap(image,null,getBounds(),paint);
    }
    // Draw above the real app artwork so it appears beneath one continuous glass cover.
    void drawCover(Canvas canvas){
        if(reflection!=null){int save=canvas.save();canvas.clipPath(clip);reflectionMatrix.setTranslate(lightX*face.width()*.15f,lightY*face.height()*.12f);reflection.setLocalMatrix(reflectionMatrix);reflectionPaint.setShader(reflection);reflectionPaint.setAlpha(alpha);canvas.drawRect(face,reflectionPaint);canvas.restoreToCount(save);}
        int save=canvas.save();canvas.clipPath(clip);sceneLight.draw(canvas,face,alpha,.55f);canvas.restoreToCount(save);
    }
    @Override public void getOutline(Outline outline){float radius=shape==1?Math.min(face.width(),face.height())*.5f:shape==2?1:Math.min(face.width(),face.height())*.227f;outline.setRoundRect(Math.round(face.left),Math.round(face.top),Math.round(face.right),Math.round(face.bottom),radius);outline.setAlpha(shadows?alpha/255f*.55f:0);}
    @Override public void setAlpha(int value){alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);reflectionPaint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
