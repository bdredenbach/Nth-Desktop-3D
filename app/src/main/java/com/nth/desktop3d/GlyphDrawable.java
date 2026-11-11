package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Original, resolution-independent line icons shared by the shell. */
final class GlyphDrawable extends Drawable {
    private final String name;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    GlyphDrawable(String name,int color){this.name=name;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
    @Override public void draw(Canvas c){
        c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
        switch(name){
            case "close":c.drawLine(7,7,17,17,p);c.drawLine(17,7,7,17,p);break;
            case "left":case "right":float x=name.equals("left")?14:10;float mid=name.equals("left")?9:15;Path a=new Path();a.moveTo(x,6);a.lineTo(mid,12);a.lineTo(x,18);c.drawPath(a,p);break;
            case "search":c.drawCircle(10.5f,10.5f,6,p);c.drawLine(15,15,20,20,p);break;
            case "grid":for(int i=0;i<4;i++)c.drawRoundRect(4+(i%2)*9,4+(i/2)*9,10+(i%2)*9,10+(i/2)*9,1.5f,1.5f,p);break;
            case "sliders":for(int i=0;i<3;i++){float y=6+i*6,knob=i==1?15:8;c.drawLine(4,y,20,y,p);p.setStyle(Paint.Style.FILL);c.drawCircle(knob,y,2.4f,p);p.setStyle(Paint.Style.STROKE);}break;
            case "check":Path check=new Path();check.moveTo(5,12);check.lineTo(10,17);check.lineTo(19,7);c.drawPath(check,p);break;
            default:Path cube=new Path();cube.moveTo(12,2);cube.lineTo(21,7);cube.lineTo(21,17);cube.lineTo(12,22);cube.lineTo(3,17);cube.lineTo(3,7);cube.close();c.drawPath(cube,p);c.drawLine(3,7,12,12,p);c.drawLine(12,12,21,7,p);c.drawLine(12,12,12,22,p);break;
        }c.restore();
    }
    @Override public void setAlpha(int a){p.setAlpha(a);}
    @Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    static final class Centered extends Drawable {
        private final Drawable icon;private final int size;
        Centered(Drawable icon,Context c){this.icon=icon;size=Ui.dp(c,23);}
        @Override public void draw(Canvas c){int x=getBounds().centerX()-size/2,y=getBounds().centerY()-size/2;icon.setBounds(x,y,x+size,y+size);icon.draw(c);}
        @Override public void setAlpha(int a){icon.setAlpha(a);}
        @Override public void setColorFilter(ColorFilter f){icon.setColorFilter(f);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
