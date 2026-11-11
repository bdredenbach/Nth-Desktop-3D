package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int TEXT=0xFFF0F6FF, MUTED=0xFFB8C7DE, ACCENT=0xFFA8D5FF, PANEL=0xFF142139;
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable surface(int color,float radius,Context c) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));d.setStroke(dp(c,1),0x446F95C6);return d;
    }
    static TextView text(Context c,String s,int sp,int color) {TextView t=new TextView(c);t.setText(s);t.setTextSize(sp);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    static Button button(Context c,String s,Runnable action) {
        Button b=new Button(c);b.setText(s);b.setTextSize(14);b.setTextColor(TEXT);b.setAllCaps(false);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setMinWidth(0);b.setMinimumWidth(0);
        StateListDrawable bg=new StateListDrawable();bg.addState(new int[]{android.R.attr.state_pressed},surface(0xFF354F76,14,c));bg.addState(new int[]{},surface(0xFF213450,14,c));b.setBackground(bg);
        b.setPadding(dp(c,12),dp(c,6),dp(c,12),dp(c,6));b.setOnClickListener(v->action.run());return b;
    }
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static LinearLayout row(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    static void pad(View v,int p){int d=dp(v.getContext(),p);v.setPadding(d,d,d,d);}
    static LinearLayout.LayoutParams space(int w,int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(0,0,0,8);return p;}
}
