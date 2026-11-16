package com.nth.desktop3d;

import android.app.Instrumentation;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Shipped GLES optical glass: visible highlights, live floor light, pause and rotation. */
public final class Atrium085Smoke {
    private final Instrumentation test;private final MainActivity activity;private final String output;
    public Atrium085Smoke(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;}
    public void run()throws Exception {
        LauncherStore s=(LauncherStore)field("store");CrystalPreferences prefs=new CrystalPreferences(activity);
        int mode=s.mode,source=s.preferences.getInt("wallpaper_source",0),orientation=activity.getRequestedOrientation();boolean ambient=s.ambient,reduced=s.reducedMotion,tilt=s.tilt,parallax=prefs.parallax();
        try{
            main(()->{s.mode=4;s.ambient=true;s.reducedMotion=false;s.tilt=false;prefs.setParallax(false);s.preferences.edit().putInt("wallpaper_source",0).apply();apply();});idle(2000);
            Bitmap a=frame();idle(1200);Bitmap b=frame();try{save(a,"085-atrium-optical-glass");save(b,"085-atrium-moving-light");check(difference(a,b,.65f)>.08,"Cool floor light did not move");check(difference(a,b,.20f)>.08,"Glass reflections did not move");}finally{a.recycle();b.recycle();}
            main(()->{s.ambient=false;apply();});idle(1200);a=frame();idle(700);b=frame();try{check(difference(a,b,0)<.01,"Ambient off did not freeze optical glass");}finally{a.recycle();b.recycle();}
            main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));idle(1600);b=frame();try{check(b.getWidth()>b.getHeight(),"Glass scene did not rotate");save(b,"085-atrium-landscape");}finally{b.recycle();}
        }finally{main(()->{activity.setRequestedOrientation(orientation);s.mode=mode;s.ambient=ambient;s.reducedMotion=reduced;s.tilt=tilt;prefs.setParallax(parallax);s.preferences.edit().putInt("wallpaper_source",source).apply();s.save();apply();});idle(800);}
    }
    private Bitmap frame()throws Exception {SceneSurface scene=(SceneSurface)field("scene");AtomicReference<Bitmap> result=new AtomicReference<>();AtomicInteger status=new AtomicInteger(-1);CountDownLatch ready=new CountDownLatch(1);main(()->{Bitmap b=Bitmap.createBitmap(scene.getWidth(),scene.getHeight(),Bitmap.Config.ARGB_8888);result.set(b);PixelCopy.request(scene,b,value->{status.set(value);ready.countDown();},new Handler(Looper.getMainLooper()));});if(!ready.await(5,TimeUnit.SECONDS)||status.get()!=PixelCopy.SUCCESS){result.get().recycle();throw new AssertionError("Could not capture native glass surface: "+status.get());}return result.get();}
    private double difference(Bitmap a,Bitmap b,float from){long sum=0,n=0;for(int y=Math.round(a.getHeight()*from);y<a.getHeight();y+=3)for(int x=0;x<a.getWidth();x+=3){int p=a.getPixel(x,y),q=b.getPixel(x,y);sum+=Math.abs((p>>16&255)-(q>>16&255))+Math.abs((p>>8&255)-(q>>8&255))+Math.abs((p&255)-(q&255));n+=3;}return sum/(double)Math.max(1,n);}
    private void save(Bitmap b,String name)throws Exception{File dir=output==null?new File(activity.getExternalFilesDir(null),"verification"):new File(output);dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
    private Object field(String name){try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private void apply(){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("applySettings",boolean.class);m.setAccessible(true);m.invoke(activity,false);}catch(Exception e){throw new AssertionError(e);}}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
