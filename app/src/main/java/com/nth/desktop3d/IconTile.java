package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

@android.annotation.SuppressLint("ViewConstructor") // Created with an explicit app identity, never inflated from XML.
final class IconTile extends LinearLayout {
    final TextView label;
    private int desiredSize;
    IconTile(Context c,String name,Drawable drawable,String key) {
        super(c);desiredSize=Ui.dp(c,60);setOrientation(VERTICAL);setGravity(Gravity.CENTER);setClipChildren(false);setClipToPadding(false);
        setPadding(Ui.dp(c,3),Ui.dp(c,8),Ui.dp(c,3),Ui.dp(c,6));
        setBackground(Ui.touch(c,null,18));setCameraDistance(Ui.dp(c,1800));
        FrameLayout plinth=new FrameLayout(c);plinth.setBackground(Ui.satin(c,0xA348A3C3,0xB4132B49,18));plinth.setElevation(Ui.dp(c,9));
        ImageView image=new ImageView(c);image.setImageDrawable(drawable==null?new BuiltinIcon(key):drawable);image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(Ui.dp(c,45),Ui.dp(c,45),Gravity.CENTER);plinth.addView(image,ip);addView(plinth,new LayoutParams(Ui.dp(c,60),Ui.dp(c,60)));
        label=Ui.text(c,name,12,Ui.TEXT);label.setGravity(Gravity.CENTER);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setShadowLayer(Ui.dp(c,2),0,Ui.dp(c,1),0xFF000000);LayoutParams p=new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);p.topMargin=Ui.dp(c,7);addView(label,p);
        label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setContentDescription(name);setFocusable(true);setClickable(true);setLongClickable(true);
    }
    void appearance(android.content.SharedPreferences prefs,String scope){
        int size=Math.max(70,Math.min(130,prefs.getInt(scope.equals("desktop")?"icon_size":scope+"_icon_size",100)));desiredSize=Ui.dp(getContext(),60*size/100f);
        label.setVisibility(prefs.getBoolean(scope+"_labels",true)?VISIBLE:GONE);label.setTextSize(Math.max(10,Math.min(18,prefs.getInt("label_size",12))));int color=Math.max(0,Math.min(3,prefs.getInt("label_color",0)));label.setTextColor(SettingsModel.TEXT_COLORS[color]);
        FrameLayout plinth=(FrameLayout)getChildAt(0);plinth.setElevation(new CrystalPreferences(getContext()).shadows()?Ui.dp(getContext(),9):0);plinth.setLayoutParams(new LayoutParams(desiredSize,desiredSize));int icon=desiredSize*3/4;plinth.getChildAt(0).setLayoutParams(new FrameLayout.LayoutParams(icon,icon,Gravity.CENTER));
    }
    void physicsStyle(){
        setPadding(Ui.dp(getContext(),3),Ui.dp(getContext(),4),Ui.dp(getContext(),3),Ui.dp(getContext(),4));
        int size=Math.round(desiredSize*.8f);FrameLayout plinth=(FrameLayout)getChildAt(0);plinth.setLayoutParams(new LayoutParams(size,size));
        plinth.getChildAt(0).setLayoutParams(new FrameLayout.LayoutParams(size*3/4,size*3/4,Gravity.CENTER));
        ((LayoutParams)label.getLayoutParams()).topMargin=Ui.dp(getContext(),4);
        setBackground(Ui.touch(getContext(),Ui.satin(getContext(),0xEF2B3752,0xEE172137,18),18));setElevation(new CrystalPreferences(getContext()).shadows()?Ui.dp(getContext(),6):0);}
    void fitCell(int width,int height){
        setClipChildren(true);setClipToPadding(false);
        int lines=height<Ui.dp(getContext(),105)?1:2;label.setMaxLines(lines);
        int labelHeight=(int)Math.ceil(label.getVisibility()==GONE?0:label.getPaint().getFontSpacing()*lines);
        int size=Math.max(Ui.dp(getContext(),22),Math.min(desiredSize,Math.min(width-Ui.dp(getContext(),6),height-labelHeight-Ui.dp(getContext(),21))));
        FrameLayout plinth=(FrameLayout)getChildAt(0);LayoutParams p=(LayoutParams)plinth.getLayoutParams();
        if(p.width!=size||p.height!=size){p.width=size;p.height=size;plinth.setLayoutParams(p);}
        int icon=Math.max(1,size*3/4);android.view.View image=plinth.getChildAt(0);FrameLayout.LayoutParams imageParams=(FrameLayout.LayoutParams)image.getLayoutParams();
        if(imageParams.width!=icon){imageParams.width=icon;imageParams.height=icon;image.setLayoutParams(imageParams);}
    }
    @Override public CharSequence getAccessibilityClassName(){return android.widget.Button.class.getName();}
    @Override public boolean performClick(){return super.performClick();}
    static final class BuiltinIcon extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final String key;
        BuiltinIcon(String key){this.key=key;}
        @Override public void draw(Canvas c) {
            c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/48f,getBounds().height()/48f);
            paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF20354B);c.drawRoundRect(2,4,46,46,12,12,paint);
            paint.setColor(key.endsWith("notes")?0xFFEBB66E:key.endsWith("calculator")?0xFF83BDAE:key.endsWith("calendar")?0xFFD694A8:key.endsWith("settings")?0xFF94A8C5:0xFF8FA8DE);c.drawRoundRect(2,1,46,42,12,12,paint);
            paint.setColor(0xFFDDF2FF);paint.setStrokeWidth(2.3f);paint.setStrokeCap(Paint.Cap.ROUND);
            if("folder".equals(key)){paint.setColor(0xFFF5C671);c.drawRoundRect(6,13,42,35,4,4,paint);c.drawRoundRect(7,8,24,18,3,3,paint);}
            else if(key.endsWith("notes")){paint.setColor(0xFFEAF6FF);c.drawRoundRect(13,8,35,35,2,2,paint);paint.setColor(0xFF3571A8);for(int i=0;i<3;i++)c.drawLine(18,16+i*6,30,16+i*6,paint);}
            else if(key.endsWith("calculator")){paint.setColor(0xFFEAF6FF);c.drawRoundRect(12,7,36,36,3,3,paint);paint.setColor(0xFF3571A8);c.drawRect(16,11,32,17,paint);for(int i=0;i<6;i++)c.drawCircle(17+(i%3)*7,23+(i/3)*7,2,paint);}
            else if(key.endsWith("calendar")){paint.setColor(0xFFEAF6FF);c.drawRoundRect(10,10,38,35,3,3,paint);paint.setColor(0xFF3571A8);c.drawRect(10,10,38,18,paint);paint.setTextSize(13);paint.setTextAlign(Paint.Align.CENTER);c.drawText("11",24,31,paint);}
            else if(key.endsWith("settings")){for(int i=0;i<3;i++){float y=13+i*9;paint.setColor(0xFFEEF3FB);c.drawLine(12,y,36,y,paint);c.drawCircle(i==1?30:18,y,3,paint);}}
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
