package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Scene previews come from the same shader that draws the live desktop. */
@android.annotation.SuppressLint("ViewConstructor")
final class SceneCard extends LinearLayout {
    SceneCard(Context c,int mode,String description,boolean selected){
        super(c);setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);
        setPadding(Ui.dp(c,10),Ui.dp(c,10),Ui.dp(c,12),Ui.dp(c,10));
        setBackground(Ui.touch(c,Ui.surface(selected?0xFF304563:0xFF202D42,18,c),18));
        setSelected(selected);setClickable(true);setFocusable(true);
        setContentDescription(LauncherStore.MODES[mode]+(selected?", selected":"")+". "+description);
        int[] previews={R.drawable.scene_spatial,R.drawable.scene_gravity,R.drawable.scene_holographic,R.drawable.scene_pond};
        ImageView preview=new ImageView(c);preview.setImageResource(previews[mode]);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setOutlineProvider(new ViewOutlineProvider(){@Override public void getOutline(View v,Outline o){o.setRoundRect(0,0,v.getWidth(),v.getHeight(),Ui.dp(c,12));}});preview.setClipToOutline(true);
        preview.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);addView(preview,new LayoutParams(Ui.dp(c,72),-1));
        LinearLayout copy=Ui.column(c);LayoutParams cp=new LayoutParams(0,-2,1);cp.leftMargin=Ui.dp(c,14);addView(copy,cp);
        TextView title=Ui.text(c,LauncherStore.MODES[mode],15,Ui.TEXT);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));copy.addView(title);
        TextView subtitle=Ui.text(c,description,12,Ui.MUTED);subtitle.setPadding(0,Ui.dp(c,5),0,0);subtitle.setMaxLines(3);copy.addView(subtitle);
        copy.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        if(selected){ImageView check=new ImageView(c);check.setImageDrawable(new GlyphDrawable("check",Ui.ACCENT));check.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);LayoutParams p=new LayoutParams(Ui.dp(c,20),Ui.dp(c,20));p.leftMargin=Ui.dp(c,8);addView(check,p);}
    }
    @Override public CharSequence getAccessibilityClassName(){return android.widget.Button.class.getName();}
    @Override public boolean performClick(){return super.performClick();}
}
