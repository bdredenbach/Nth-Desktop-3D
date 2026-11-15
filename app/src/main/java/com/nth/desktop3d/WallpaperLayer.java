package com.nth.desktop3d;

import android.content.Context;
import android.graphics.*;
import android.widget.ImageView;

@android.annotation.SuppressLint("ViewConstructor")
final class WallpaperLayer extends ImageView {
    private long stamp=-1;private boolean parallax;
    WallpaperLayer(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(false);}
    void apply(LauncherStore store,boolean reduced){
        boolean photo=store.preferences.getInt("wallpaper_source",0)==1&&WallpaperStore.file(getContext()).exists();setVisibility(photo?VISIBLE:GONE);
        if(!photo){setImageDrawable(null);stamp=-1;return;}
        long current=WallpaperStore.file(getContext()).lastModified();if(current!=stamp){setImageDrawable(WallpaperStore.photo(getContext()));stamp=current;}
        setScaleType(store.preferences.getInt("wallpaper_fit",0)==0?ScaleType.CENTER_CROP:ScaleType.FIT_CENTER);
        int dim=store.preferences.getInt("wallpaper_dim",20);setColorFilter(Color.argb(Math.round(dim*2.55f),0,0,0),PorterDuff.Mode.SRC_ATOP);
        parallax=store.preferences.getBoolean("wallpaper_parallax",true)&&!reduced&&store.tilt;setScaleX(parallax?1.06f:1);setScaleY(parallax?1.06f:1);if(!parallax){setTranslationX(0);setTranslationY(0);}
    }
    void tilt(float x,float y){if(getVisibility()==VISIBLE&&parallax){setTranslationX(x*getWidth()*.018f);setTranslationY(y*getHeight()*.018f);}}
}
