package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * Lightweight Crystal Glass material. Draws a layered translucent gradient,
 * illuminated beveled rim and specular sweep without expensive bitmap blur.
 * No background sampling: this is a stylized glass material, not true refraction.
 */
final class CrystalGlassDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();
    private final float radius, density;
    private final int top, bottom;
    private int alpha = 255;

    CrystalGlassDrawable(Context context, int top, int bottom, int radiusDp) {
        density = context.getResources().getDisplayMetrics().density;
        radius = radiusDp * density;
        this.top = top;
        this.bottom = bottom;
    }

    @Override public void draw(Canvas canvas) {
        android.graphics.Rect b = getBounds();
        if (b.isEmpty()) return;
        rect.set(b.left, b.top, b.right, b.bottom);
        float w = rect.width(), h = rect.height();
        float r = Math.min(radius, Math.min(w,h)*.5f);
        clip.reset(); clip.addRoundRect(rect,r,r,Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(clip);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(rect.left,rect.top,rect.right,rect.bottom,
                new int[]{modulate(0xA8B4F5FF),modulate(top),modulate(bottom),modulate(0xB0081930)},
                new float[]{0f,.16f,.76f,1f},Shader.TileMode.CLAMP));
        canvas.drawRect(rect,paint); paint.setShader(null);

        // Thin, angled specular reflection: concentrated at the upper edge.
        paint.setShader(new LinearGradient(rect.left,rect.top,rect.left,rect.top+h*.54f,
                new int[]{modulate(0x65FFFFFF),modulate(0x14A8F3FF),0x00000000},
                null,Shader.TileMode.CLAMP));
        canvas.drawRect(rect.left,rect.top,rect.right,rect.top+h*.54f,paint);
        paint.setShader(null);

        // A narrow light streak gives a sense of curved glass without obscuring labels.
        paint.setShader(new LinearGradient(rect.left,rect.top,rect.left+w*.55f,rect.top+h*.6f,
                new int[]{0x00000000,modulate(0x26C8F8FF),0x00000000},
                new float[]{0f,.48f,1f},Shader.TileMode.CLAMP));
        canvas.drawRect(rect,paint); paint.setShader(null);
        canvas.restoreToCount(save);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f,density));
        paint.setColor(modulate(0xA3B9F6FF));
        rect.inset(paint.getStrokeWidth()*.5f,paint.getStrokeWidth()*.5f);
        canvas.drawRoundRect(rect,Math.max(0,r-density*.5f),Math.max(0,r-density*.5f),paint);

        // Inner rim catches light on top and left; the darker bottom suggests thickness.
        paint.setStrokeWidth(Math.max(.8f,density*.7f));
        paint.setColor(modulate(0x78FFFFFF));
        canvas.drawLine(rect.left+r,rect.top+density,rect.right-r,rect.top+density,paint);
        paint.setColor(modulate(0x466FC7ED));
        canvas.drawLine(rect.left+r,rect.bottom-density,rect.right-r,rect.bottom-density,paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private int modulate(int color) {
        return (color & 0x00FFFFFF) | ((Color.alpha(color)*alpha/255)<<24);
    }
    @Override public void setAlpha(int value) {alpha=Math.max(0,Math.min(255,value));invalidateSelf();}
    @Override public void setColorFilter(android.graphics.ColorFilter filter) {paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity() {return PixelFormat.TRANSLUCENT;}
}
