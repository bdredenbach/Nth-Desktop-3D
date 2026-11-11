package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@android.annotation.SuppressLint("ViewConstructor") // Created with an explicit app identity, never inflated from XML.
final class IconTile extends LinearLayout {
    final TextView label;
    IconTile(Context c,String name,Drawable drawable,String key) {
        super(c);setOrientation(VERTICAL);setGravity(Gravity.CENTER);setClipChildren(false);setClipToPadding(false);
        setPadding(Ui.dp(c,4),Ui.dp(c,7),Ui.dp(c,4),Ui.dp(c,5));
        setBackground(Ui.surface(0x55233755,17,c));setElevation(Ui.dp(c,4));setCameraDistance(Ui.dp(c,1600));
        ImageView image=new ImageView(c);image.setImageDrawable(drawable==null?new BuiltinIcon(key):drawable);image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);addView(image,new LayoutParams(Ui.dp(c,42),Ui.dp(c,42)));
        label=Ui.text(c,name,12,Ui.TEXT);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setShadowLayer(Ui.dp(c,2),0,Ui.dp(c,1),0xFF000000);LayoutParams p=new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);p.topMargin=Ui.dp(c,5);addView(label,p);
        setContentDescription(name);setFocusable(true);setClickable(true);setLongClickable(true);
    }
    @Override public CharSequence getAccessibilityClassName(){return android.widget.Button.class.getName();}
    @Override public boolean performClick(){return super.performClick();}
    static final class BuiltinIcon extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String key;
        BuiltinIcon(String key){this.key=key;}
        @Override public void draw(Canvas c) {
            c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/48f,getBounds().height()/48f);
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF254B75);c.drawRoundRect(2,4,46,46,12,12,paint);
            paint.setColor(0xFF6CB9F6);c.drawRoundRect(2,1,46,42,12,12,paint);
            paint.setColor(0xFFDDF2FF);paint.setStrokeWidth(2.3f);paint.setStrokeCap(Paint.Cap.ROUND);
            if("folder".equals(key)){paint.setColor(0xFFF5C671);c.drawRoundRect(6,13,42,35,4,4,paint);c.drawRoundRect(7,8,24,18,3,3,paint);}
            else if(key.endsWith("notes")){paint.setColor(0xFFEAF6FF);c.drawRoundRect(13,8,35,35,2,2,paint);paint.setColor(0xFF3571A8);for(int i=0;i<3;i++)c.drawLine(18,16+i*6,30,16+i*6,paint);}
            else if(key.endsWith("calculator")){paint.setColor(0xFFEAF6FF);c.drawRoundRect(12,7,36,36,3,3,paint);paint.setColor(0xFF3571A8);c.drawRect(16,11,32,17,paint);for(int i=0;i<6;i++)c.drawCircle(17+(i%3)*7,23+(i/3)*7,2,paint);}
            else if(key.endsWith("calendar")){paint.setColor(0xFFEAF6FF);c.drawRoundRect(10,10,38,35,3,3,paint);paint.setColor(0xFF3571A8);c.drawRect(10,10,38,18,paint);paint.setTextSize(13);paint.setTextAlign(Paint.Align.CENTER);c.drawText("11",24,31,paint);}
            else {
                Path path=new Path();path.moveTo(24,8);path.lineTo(39,17);path.lineTo(39,31);path.lineTo(24,40);path.lineTo(9,31);path.lineTo(9,17);path.close();
                paint.setColor(0xFFEDF8FF);paint.setStyle(Paint.Style.STROKE);c.drawPath(path,paint);c.drawLine(9,17,24,26,paint);c.drawLine(24,26,39,17,paint);c.drawLine(24,26,24,40,paint);paint.setStyle(Paint.Style.FILL);
            }
            c.restore();
        }
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
}
