package com.nth.desktop3d;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual SurfaceView pixels: intensity reaches both rock floors, animation moves, pause is stable. */
public final class AquaticLight078Smoke {
    private final Instrumentation test;private final MainActivity activity;private final String output;
    public AquaticLight078Smoke(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;}
    public void run()throws Exception {
        LauncherStore s=(LauncherStore)field("store");CrystalPreferences prefs=new CrystalPreferences(activity);int mode=s.mode,count=s.fishCount,source=s.preferences.getInt("wallpaper_source",0),light=s.preferences.getInt("aquatic_light",75);boolean ambient=s.ambient,ripples=s.ripples,tilt=s.tilt,parallax=prefs.parallax(),reduced=s.reducedMotion;
        try{
            main(()->{s.preferences.edit().putInt("wallpaper_source",0).apply();s.fishCount=0;s.ambient=false;s.ripples=false;s.tilt=false;s.reducedMotion=false;prefs.setParallax(false);});
            for(int chosen:new int[]{3,6}){
                main(()->{s.mode=chosen;s.preferences.edit().putInt("aquatic_light",0).apply();apply();});idle(450);Bitmap unlit=frame();
                main(()->{s.preferences.edit().putInt("aquatic_light",100).apply();apply();});idle(350);Bitmap lit=frame();
                try{check(difference(unlit,lit,.80f)>.2,"Aquatic intensity did not illuminate the rock floor: "+chosen);save(lit,"water-light-native-"+chosen);}finally{unlit.recycle();lit.recycle();}
                main(()->{s.ambient=true;apply();});idle(300);Bitmap a=frame();idle(850);Bitmap b=frame();try{check(difference(a,b,.80f)>.1,"Aquatic rock floor is not alive: "+chosen);save(b,"water-light-moving-"+chosen);}finally{a.recycle();b.recycle();}
                main(()->{s.ambient=false;apply();});idle(350);a=frame();idle(600);b=frame();try{check(difference(a,b,0)<.01,"Ambient off did not freeze aquatic light: "+chosen);}finally{a.recycle();b.recycle();}
            }
        }finally{main(()->{s.mode=mode;s.fishCount=count;s.ambient=ambient;s.ripples=ripples;s.tilt=tilt;s.reducedMotion=reduced;prefs.setParallax(parallax);s.preferences.edit().putInt("wallpaper_source",source).putInt("aquatic_light",light).apply();s.save();apply();});}
    }
    private Bitmap frame()throws Exception {SceneSurface scene=(SceneSurface)field("scene");AtomicReference<Bitmap> result=new AtomicReference<>();AtomicInteger status=new AtomicInteger(-1);CountDownLatch ready=new CountDownLatch(1);main(()->{Bitmap b=Bitmap.createBitmap(scene.getWidth(),scene.getHeight(),Bitmap.Config.ARGB_8888);result.set(b);PixelCopy.request(scene,b,value->{status.set(value);ready.countDown();},new Handler(Looper.getMainLooper()));});if(!ready.await(5,TimeUnit.SECONDS)||status.get()!=PixelCopy.SUCCESS){result.get().recycle();throw new AssertionError("Could not capture native aquatic surface: "+status.get());}return result.get();}
    private double difference(Bitmap a,Bitmap b,float from){long sum=0,n=0;for(int y=Math.round(a.getHeight()*from);y<a.getHeight();y+=3)for(int x=0;x<a.getWidth();x+=3){int p=a.getPixel(x,y),q=b.getPixel(x,y);sum+=Math.abs((p>>16&255)-(q>>16&255))+Math.abs((p>>8&255)-(q>>8&255))+Math.abs((p&255)-(q&255));n+=3;}return sum/(double)Math.max(1,n);}
    private void save(Bitmap b,String name)throws Exception{File dir=output==null?new File(activity.getExternalFilesDir(null),"verification"):new File(output);dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
    private Object field(String name){try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private void apply(){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("applySettings",boolean.class);m.setAccessible(true);m.invoke(activity,false);}catch(Exception e){throw new AssertionError(e);}}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
