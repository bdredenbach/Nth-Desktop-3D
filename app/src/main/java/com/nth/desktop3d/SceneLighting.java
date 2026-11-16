package com.nth.desktop3d;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.*;

/** One foreground light clock, bounded reflection maps and weak drawable observers per launcher. */
final class SceneLighting {
    private final Context context;
    private final WeakHashMap<Drawable,Boolean> drawables=new WeakHashMap<>();
    private final WeakHashMap<View,Boolean> views=new WeakHashMap<>();
    private final Map<Integer,int[]> environments=new HashMap<>();
    private int[] mixed;private int mixedKey=Integer.MIN_VALUE;
    private int mode=-1,strength=VisualDefaults.LIGHT_STRENGTH;private boolean enabled,moving,steady;private long lastFrame;
    private float seconds,tiltX,tiltY;
    private SceneLight.Frame frame=SceneLight.frame(-1,false,0,false,true,0,0,0);
    SceneLighting(Context context){this.context=context;}
    static SceneLighting from(Context context){
        Context current=context;while(current instanceof ContextWrapper){if(current instanceof MainActivity){SceneLighting shared=((MainActivity)current).sceneLighting();if(shared!=null)return shared;}Context base=((ContextWrapper)current).getBaseContext();if(base==current)break;current=base;}
        return new SceneLighting(context); // Standalone snapshots use the neutral legacy material.
    }
    void configure(LauncherStore store,CrystalPreferences crystal,boolean powerSave,boolean systemReduced){
        int source=store.preferences.getInt("wallpaper_source",0);mode=source==0||source==1&&!WallpaperStore.file(context).exists()?store.mode:-1;
        enabled=store.preferences.getBoolean("scene_lighting",true);strength=store.preferences.getInt("scene_light_strength",VisualDefaults.LIGHT_STRENGTH);
        steady=store.reducedMotion||systemReduced||powerSave;
        moving=store.ambient&&crystal.dynamicLighting()&&!steady;
        if(steady||!store.tilt||!crystal.parallax()||!crystal.dynamicLighting()){tiltX=0;tiltY=0;}
        update(true);
    }
    void tick(float seconds,float x,float y,boolean allowTilt){
        long now=android.os.SystemClock.uptimeMillis();if(now-lastFrame<33)return;lastFrame=now;
        this.seconds=seconds;tiltX=allowTilt&&!steady?x:0;tiltY=allowTilt&&!steady?y:0;update(false);
    }
    private void update(boolean force){
        SceneLight.Frame next=SceneLight.frame(mode,enabled,strength,moving,steady,seconds,tiltX,tiltY);
        if(!force&&next.same(frame))return;frame=next;
        for(Drawable d:new ArrayList<>(drawables.keySet()))if(d!=null){Drawable.Callback target=d.getCallback();for(int depth=0;target instanceof Drawable&&depth<8;depth++)target=((Drawable)target).getCallback();if(target!=null&&(!(target instanceof View)||((View)target).isShown()))d.invalidateSelf();}
        for(View v:new ArrayList<>(views.keySet()))if(v!=null&&v.isShown())v.invalidate();
    }
    SceneLight.Frame frame(){return frame;}
    void watch(View view){views.put(view,true);}
    void watch(Drawable drawable){drawables.put(drawable,true);}
    int environmentKey(){return frame.signature();}
    int[] environment(){
        int key=environmentKey();if(key==mixedKey&&mixed!=null)return mixed;
        int[] legacy=map(-1);if(!frame.active()){mixed=legacy;mixedKey=key;return mixed;}
        int[] scene=map(frame.mode);mixed=new int[128*128];for(int i=0;i<mixed.length;i++)mixed[i]=SceneLight.blend(legacy[i],scene[i],frame.amount);
        mixedKey=key;return mixed;
    }
    private int[] map(int mode){
        int[] cached=environments.get(mode);if(cached!=null)return cached;
        int resource=mode==0?R.drawable.spatial_environment:mode==1?R.drawable.scene_gravity:mode==2?R.drawable.scene_holographic:mode==3?R.drawable.pond_bed:mode==5||mode==10?R.drawable.celestial_sky:mode==6?R.drawable.reef_water:mode==7?R.drawable.focus_city:mode==8?R.drawable.ice_material:mode==9?R.drawable.lava_material:R.drawable.glass_sky;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=8;
        Bitmap image=BitmapFactory.decodeResource(context.getResources(),resource,options);if(image==null)throw new IllegalStateException("Missing scene-light environment");
        try{
            int w=image.getWidth(),h=image.getHeight();int[] source=new int[w*h];image.getPixels(source,0,w,0,0,w,h);cached=new int[128*128];
            for(int y=0;y<128;y++)for(int x=0;x<128;x++){
                float u=x/127f,v=y/127f;if(mode==5)v=.502f+v*.496f;else if(mode==10)v=.002f+v*.496f;else if(mode==7)u*=.5f;
                cached[y*128+x]=source[Math.min(h-1,Math.round(v*(h-1)))*w+Math.min(w-1,Math.round(u*(w-1)))];
            }
        }finally{image.recycle();}
        environments.put(mode,cached);return cached;
    }
    /** A cached gradient, transformed by the shared scene clock rather than rebuilt each frame. */
    static final class Cover {
        final SceneLighting lighting;private final Paint paint=new Paint(3);private final Matrix matrix=new Matrix();
        private Shader shader;private float left,top,width,height;private int primary,secondary;
        Cover(Context context,Drawable target){lighting=SceneLighting.from(context);lighting.watch(target);}
        void draw(Canvas canvas,RectF bounds,int alpha,float scale){
            SceneLight.Frame f=lighting.frame();if(!f.active()||bounds.isEmpty())return;
            if(shader==null||left!=bounds.left||top!=bounds.top||width!=bounds.width()||height!=bounds.height()||primary!=f.primary||secondary!=f.secondary){
                left=bounds.left;top=bounds.top;width=bounds.width();height=bounds.height();primary=f.primary;secondary=f.secondary;
                shader=new LinearGradient(left-width*.35f,top-height*.15f,left+width*1.1f,top+height*.9f,new int[]{primary&0xFFFFFF,(0x78<<24)|(primary&0xFFFFFF),(0x36<<24)|(secondary&0xFFFFFF),secondary&0xFFFFFF},new float[]{0,.35f,.65f,1},Shader.TileMode.CLAMP);
            }
            matrix.setTranslate(f.x*width,f.y*height);shader.setLocalMatrix(matrix);paint.setShader(shader);paint.setAlpha(Math.round(Math.max(0,Math.min(255,alpha))*Math.min(1,f.amount*f.pulse*scale)));canvas.drawRect(bounds,paint);
        }
    }
}
