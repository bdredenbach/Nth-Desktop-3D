package com.nth.desktop3d;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.Button;

/** Nth Nota AI's own restrained graphite, lilac and ice visual language. */
final class NotaDesign {
    static final int CANVAS=0xFF171A22,SURFACE=0xFF222630,TEXT=0xFFF3F2F7,MUTED=0xFFB6B9C9,ACCENT=0xFFD3C5FF,LINE=0xFF353A49;
    static GradientDrawable surface(Context c,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(Ui.dp(c,radius));d.setStroke(Ui.dp(c,1),LINE);return d;}
    static Button action(Context c,String label,Runnable run){Button b=Ui.quiet(c,label,run);b.setTextColor(MUTED);b.setTextSize(12);b.setMinHeight(Ui.dp(c,44));b.setMinimumHeight(Ui.dp(c,44));b.setPadding(Ui.dp(c,10),Ui.dp(c,6),Ui.dp(c,10),Ui.dp(c,6));return b;}
    static final class Mark extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        Mark(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);int save=canvas.save();float scale=Math.min(getWidth(),getHeight())/64f;canvas.translate((getWidth()-64*scale)/2,(getHeight()-64*scale)/2);canvas.scale(scale,scale);
            paint.setStyle(Paint.Style.FILL);paint.setShader(new LinearGradient(4,4,60,60,new int[]{0xFF7462AB,0xFF333B56,0xFF426A78},null,Shader.TileMode.CLAMP));canvas.drawRoundRect(2,2,62,62,19,19,paint);paint.setShader(null);
            paint.setColor(0x66DDD2FF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);canvas.drawRoundRect(2.5f,2.5f,61.5f,61.5f,19,19,paint);
            paint.setColor(TEXT);paint.setStrokeWidth(3);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);Path n=new Path();n.moveTo(20,43);n.lineTo(20,22);n.lineTo(39,43);n.lineTo(39,22);canvas.drawPath(n,paint);paint.setColor(0xFFB6F1ED);paint.setStrokeWidth(2);canvas.drawLine(45,16,45,24,paint);canvas.drawLine(41,20,49,20,paint);canvas.restoreToCount(save);
        }
    }
}
