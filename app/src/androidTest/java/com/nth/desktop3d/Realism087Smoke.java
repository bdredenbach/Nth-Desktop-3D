package com.nth.desktop3d;

import android.app.Instrumentation;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual GLES pixels plus settings/gallery and full-backup checks for the photographic scenes. */
public final class Realism087Smoke {
    private final Instrumentation test;private final MainActivity activity;private final String output;
    public Realism087Smoke(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;}
    public void run()throws Exception {
        LauncherStore s=(LauncherStore)field("store");CrystalPreferences prefs=new CrystalPreferences(activity);
        AtomicReference<SettingsModel> selected=new AtomicReference<>();main(()->selected.set((SettingsModel)fieldModel()));SettingsModel model=selected.get();int originalMode=s.mode,source=s.preferences.getInt("wallpaper_source",0),orientation=activity.getRequestedOrientation();float strength=s.strength;
        boolean ambient=s.ambient,reduced=s.reducedMotion,tilt=s.tilt,parallax=prefs.parallax();
        String layout=s.preferences.getString("layout","");
        String prefix="realism087_"+SystemClock.uptimeMillis();File isolatedFiles=new File(activity.getCacheDir(),prefix);isolatedFiles.mkdirs();
        Context isolated=new ContextWrapper(activity){@Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences(prefix+name,mode);}@Override public File getFilesDir(){return isolatedFiles;}};
        try{
            check(LauncherStore.MODES.length==11&&SceneCatalog.PREVIEWS.length==11&&SceneCatalog.DESCRIPTIONS.length==11,"Scene catalogs differ in length");
            check("Biolume Reef".equals(LauncherStore.MODES[6])&&"Crystal Atrium".equals(LauncherStore.MODES[4]),"Existing scene IDs changed");
            main(()->{s.tilt=false;s.strength=.5f;s.reducedMotion=false;prefs.setParallax(false);});
            for(int mode:new int[]{2,5,7,10,3,6,4,8,9}){
                final int chosen=mode;main(()->{model.set("scene",chosen);s.ambient=true;apply();});idle(1600);
                check(new LauncherStore(activity).mode==chosen,"Scene selection did not persist: "+chosen);
                Bitmap a=frame();idle(1100);Bitmap b=frame();try{check(difference(a,b,0)>.05,"Scene has no independent ambient motion: "+chosen);save(b,"087-scene-"+chosen+"-moving");}finally{a.recycle();b.recycle();}
                main(()->{s.ambient=false;apply();});idle(1000);a=frame();idle(600);b=frame();try{check(difference(a,b,0)<.01,"Ambient off did not freeze scene: "+chosen);}finally{a.recycle();b.recycle();}
                main(()->{prefs.setParallax(true);SceneSurface scene=(SceneSurface)field("scene");scene.configureCrystal(prefs);scene.tilt(.7f,-.4f);scene.requestRender();});idle(600);a=frame();
                main(()->{prefs.setParallax(false);SceneSurface scene=(SceneSurface)field("scene");scene.configureCrystal(prefs);scene.requestRender();});idle(600);b=frame();try{check(difference(a,b,0)>.10,"Scene ignores parallax: "+chosen);save(a,"087-scene-"+chosen+"-tilted");}finally{a.recycle();b.recycle();}
                main(()->{try{
                    org.json.JSONObject archive=DesktopBackup.capture(activity,model);LauncherStore fresh=new LauncherStore(isolated);SettingsModel restored=new SettingsModel(fresh,new CrystalPreferences(isolated),rebuild->{});restored.importDesktopBackup(isolated,new DesktopBackup.Prepared(archive,restored));check(new LauncherStore(isolated).mode==chosen,"Full backup lost scene: "+chosen);
                }catch(Exception e){throw new AssertionError(e);}});
            }
            main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));idle(1600);
            for(int mode:new int[]{2,5,7,10,3,6,4,8,9}){final int chosen=mode;main(()->model.set("scene",chosen));idle(600);Bitmap b=frame();try{check(b.getWidth()>b.getHeight(),"Scene did not rotate");save(b,"087-scene-"+chosen+"-landscape");}finally{b.recycle();}}
            main(()->invoke("showModes"));idle(400);
            main(()->{for(int mode:new int[]{2,5,7,10,3,6,4,8,9})check(find(activity.getWindow().getDecorView(),"scene-card:"+mode)!=null,"Scene gallery choice missing: "+mode);invoke("closePanel");});
            check(layout.equals(s.preferences.getString("layout","")),"Scene selection changed desktop shortcuts");
        }finally{
            main(()->{activity.setRequestedOrientation(orientation);model.set("scene",originalMode);s.ambient=ambient;s.reducedMotion=reduced;s.tilt=tilt;s.strength=strength;prefs.setParallax(parallax);s.preferences.edit().putInt("wallpaper_source",source).apply();s.save();apply();});
            isolated.getSharedPreferences("desktop3d",0).edit().clear().commit();isolated.getSharedPreferences("desktop3d_crystal",0).edit().clear().commit();
        }
    }
    private Object fieldModel(){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("settingsModel");m.setAccessible(true);return m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}}
    private View find(View v,String tag){if(tag.equals(v.getTag()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),tag);if(found!=null)return found;}return null;}
    private Bitmap frame()throws Exception {SceneSurface scene=(SceneSurface)field("scene");AtomicReference<Bitmap> result=new AtomicReference<>();AtomicInteger status=new AtomicInteger(-1);CountDownLatch ready=new CountDownLatch(1);main(()->{Bitmap b=Bitmap.createBitmap(scene.getWidth(),scene.getHeight(),Bitmap.Config.ARGB_8888);result.set(b);PixelCopy.request(scene,b,value->{status.set(value);ready.countDown();},new Handler(Looper.getMainLooper()));});if(!ready.await(5,TimeUnit.SECONDS)||status.get()!=PixelCopy.SUCCESS){result.get().recycle();throw new AssertionError("Could not capture native scene surface: "+status.get());}return result.get();}
    private double difference(Bitmap a,Bitmap b,float from){long sum=0,n=0;for(int y=Math.round(a.getHeight()*from);y<a.getHeight();y+=3)for(int x=0;x<a.getWidth();x+=3){int p=a.getPixel(x,y),q=b.getPixel(x,y);sum+=Math.abs((p>>16&255)-(q>>16&255))+Math.abs((p>>8&255)-(q>>8&255))+Math.abs((p&255)-(q&255));n+=3;}return sum/(double)Math.max(1,n);}
    private void save(Bitmap b,String name)throws Exception{File dir=output==null?new File(activity.getExternalFilesDir(null),"verification"):new File(output);dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
    private Object field(String name){try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private void apply(){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("applySettings",boolean.class);m.setAccessible(true);m.invoke(activity,false);}catch(Exception e){throw new AssertionError(e);}}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
