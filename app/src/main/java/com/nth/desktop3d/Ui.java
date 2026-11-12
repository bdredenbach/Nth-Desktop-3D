package com.nth.desktop3d;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int TEXT=0xFFF1FAFF, MUTED=0xFFB0C5D9, ACCENT=0xFF8FEAFF, PANEL=0xFF101F35;
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable surface(int color,float radius,Context c) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));d.setStroke(dp(c,1),0x1FFFFFFF);return d;
    }
    static GradientDrawable satin(Context c,int top,int bottom,int radius){
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xAA80E7FF,top,bottom});
        d.setCornerRadius(dp(c,radius));d.setStroke(dp(c,1),0x7A9FEFFF);return d;
    }
    static Drawable touch(Context c,Drawable base,int radius){return new RippleDrawable(ColorStateList.valueOf(0x25B6D5FF),base,surface(0xFFFFFFFF,radius,c));}
    static TextView text(Context c,String s,int sp,int color) {
        TextView t=new TextView(c);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);
        t.setFontFeatureSettings("kern");return t;
    }
    static Button button(Context c,String s,Runnable action) {
        Button b=new Button(c);b.setText(s);b.setTextSize(14);b.setTextColor(TEXT);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setMinWidth(0);b.setMinimumWidth(0);b.setStateListAnimator(null);
        b.setBackground(touch(c,satin(c,0xB52D5268,0xC016273E,14),14));
        b.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));b.setOnClickListener(v->action.run());return b;
    }
    static Button quiet(Context c,String text,Runnable action){Button b=button(c,text,action);b.setBackground(touch(c,null,14));return b;}
    static Button primary(Context c,String text,Runnable action){Button b=button(c,text,action);b.setTextColor(0xFF101B30);b.setBackground(touch(c,surface(ACCENT,16,c),16));return b;}
    static Button icon(Context c,String glyph,String description,Runnable action){
        Button b=quiet(c,"",action);GlyphDrawable d=new GlyphDrawable(glyph,ACCENT);int s=dp(c,23);d.setBounds(0,0,s,s);
        // Background drawing keeps the icon centered without reserving text space.
        StateListDrawable state=new StateListDrawable();state.addState(new int[]{android.R.attr.state_selected},surface(0x354D6EAA,14,c));state.addState(new int[]{},new ColorDrawable(0));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x28B6D5FF),new LayerDrawable(new Drawable[]{state,new GlyphDrawable.Centered(d,c)}),surface(0xFFFFFFFF,14,c)));
        b.setContentDescription(description);return b;
    }
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    static void pad(View v,int p){int d=dp(v.getContext(),p);v.setPadding(d,d,d,d);}
}
