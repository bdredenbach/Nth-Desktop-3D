package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.widget.ScrollView;

/** A transparent alpha fade contained entirely inside the list viewport. */
@android.annotation.SuppressLint("ViewConstructor")
final class FadingSettingsScrollView extends ScrollView {
    private final Paint fade=new Paint();
    FadingSettingsScrollView(Context context){
        super(context);setFillViewport(true);setClipChildren(true);setClipToPadding(true);
        fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    }
    @Override protected void dispatchDraw(Canvas canvas){
        int x=getScrollX(),y=getScrollY(),w=getWidth(),h=getHeight();
        int save=canvas.saveLayer(x,y,x+w,y+h,null);
        canvas.clipRect(x,y,x+w,y+h);
        super.dispatchDraw(canvas);
        if(y>0){float height=Math.min(Ui.dp(getContext(),28),h/3f);
            fade.setShader(new LinearGradient(0,y,0,y+height,0x00FFFFFF,0xFFFFFFFF,Shader.TileMode.CLAMP));
            canvas.drawRect(x,y,x+w,y+height,fade);
        }
        canvas.restoreToCount(save);
    }
}
