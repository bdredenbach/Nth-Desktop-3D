package com.nth.desktop3d;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import java.util.List;

/** Only the menu material changes opacity; controls keep native input and accessibility. */
final class AdaptiveMenuDrawable extends Drawable {
    private static final int COLS=48,ROWS=80;
    private final Bitmap bitmap=Bitmap.createBitmap(COLS,ROWS,Bitmap.Config.ARGB_8888);
    private final float[] values=new float[COLS*ROWS],targets=new float[COLS*ROWS];
    private final int[] pixels=new int[COLS*ROWS];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path clip=new Path();
    private final float density;
    private long previousHash=Long.MIN_VALUE;
    private int radius=26,alpha=255;
    AdaptiveMenuDrawable(float density){this.density=density;}
    void radius(int dp){radius=dp;invalidateSelf();}
    void update(List<RectF> occupied,int strength,int feather,int empty,boolean reduced,float dt){
        int w=getBounds().width(),h=getBounds().height();if(w<=0||h<=0)return;
        long hash=w*31L+h;hash=hash*31+strength;hash=hash*31+feather;hash=hash*31+empty;
        float[][] boxes=new float[occupied.size()][4];int n=0;
        for(RectF b:occupied){boxes[n++]=new float[]{b.left,b.top,b.right,b.bottom};hash=hash*31+Math.round(b.left);hash=hash*31+Math.round(b.top);hash=hash*31+Math.round(b.right);hash=hash*31+Math.round(b.bottom);}
        if(hash!=previousHash){
            previousHash=hash;
            for(int y=0;y<ROWS;y++)for(int x=0;x<COLS;x++)targets[y*COLS+x]=Math.max(empty/100f,strength/100f*AdaptiveOpacity.coverage((x+.5f)*w/COLS,(y+.5f)*h/ROWS,boxes,feather*density));
        }
        boolean changed=false;float blend=reduced?1:1-(float)Math.exp(-Math.max(0,dt)*13);
        for(int i=0;i<values.length;i++){float next=Math.abs(targets[i]-values[i])<.002f?targets[i]:values[i]+(targets[i]-values[i])*blend;if(next!=values[i])changed=true;values[i]=next;pixels[i]=(Math.round(next*255)<<24)|0x00081728;}
        if(changed){bitmap.setPixels(pixels,0,COLS,0,0,COLS,ROWS);invalidateSelf();}
    }
    @Override public void draw(Canvas canvas){RectF b=new RectF(getBounds());clip.reset();clip.addRoundRect(b,radius*density,radius*density,Path.Direction.CW);int save=canvas.save();canvas.clipPath(clip);paint.setAlpha(alpha);canvas.drawBitmap(bitmap,null,b,paint);canvas.restoreToCount(save);}
    @Override public void setAlpha(int alpha){this.alpha=alpha;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
