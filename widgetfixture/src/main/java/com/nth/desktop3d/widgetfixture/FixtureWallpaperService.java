package com.nth.desktop3d.widgetfixture;

/** Discoverable wallpaper service used to verify the launcher's explicit preview handoff. */
public final class FixtureWallpaperService extends android.service.wallpaper.WallpaperService {
    @Override public Engine onCreateEngine(){return new Engine(){
        @Override public void onVisibilityChanged(boolean visible){if(!visible)return;android.view.SurfaceHolder holder=getSurfaceHolder();android.graphics.Canvas canvas=null;try{canvas=holder.lockCanvas();if(canvas!=null)canvas.drawColor(0xFF23556A);}finally{if(canvas!=null)holder.unlockCanvasAndPost(canvas);}}
    };}
}
