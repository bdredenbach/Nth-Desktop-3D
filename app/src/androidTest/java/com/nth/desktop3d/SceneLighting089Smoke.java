package com.nth.desktop3d;

import android.app.Instrumentation;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Native material/controller checks; isolated settings never replace the live desktop. */
public final class SceneLighting089Smoke {
    private final Instrumentation test;private final MainActivity activity;
    public SceneLighting089Smoke(Instrumentation test,MainActivity activity){this.test=test;this.activity=activity;}
    public void run(){
        AtomicReference<Throwable> error=new AtomicReference<>();
        test.runOnMainSync(()->{String prefix="lighting089_"+android.os.SystemClock.uptimeMillis();Context isolated=new ContextWrapper(activity){@Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences(prefix+name,mode);}};
            LauncherStore live=new LauncherStore(activity);CrystalPreferences crystal=new CrystalPreferences(activity);String layout=live.preferences.getString("layout","");SceneLighting lighting=activity.sceneLighting();
            try{
                LauncherStore probe=new LauncherStore(isolated);probe.reducedMotion=true;
                SettingsModel model=new SettingsModel(probe,new CrystalPreferences(isolated),rebuild->lighting.configure(probe,crystal,false,false));
                model.set("scene_lighting",0);IconShellDrawable shell=new IconShellDrawable(activity,false);shell.setBounds(0,0,192,192);
                Bitmap neutral=render(shell);try{
                    for(int mode:new int[]{0,5,9,2}){model.set("scene",mode);model.set("scene_lighting",1);check(lighting.frame().mode==mode,"Light did not follow scene selection");Bitmap matched=render(shell);try{check(!neutral.sameAs(matched),"Scene did not alter optical material: "+mode);}finally{matched.recycle();}}
                    model.set("scene_light_strength",0);Bitmap zero=render(shell);try{check(neutral.sameAs(zero),"Zero strength did not restore exact neutral glass");}finally{zero.recycle();}
                    model.set("scene_light_strength",65);model.set("scene_lighting",0);Bitmap off=render(shell);try{check(neutral.sameAs(off),"Off did not restore exact neutral glass");}finally{off.recycle();}
                }finally{neutral.recycle();}
                AdaptiveMenuDrawable menu=new AdaptiveMenuDrawable(activity);menu.setBounds(0,0,192,192);java.util.List<RectF> boxes=Collections.singletonList(new RectF(20,20,60,80));
                menu.update(boxes,90,12,0,true,.1f);Bitmap a=render(menu);model.set("scene_lighting",1);model.set("scene",5);menu.update(boxes,90,12,0,true,.1f);Bitmap b=render(menu);
                try{boolean changed=false;for(int y=0;y<192;y++)for(int x=0;x<192;x++){int p=a.getPixel(x,y),q=b.getPixel(x,y);check(Color.alpha(p)==Color.alpha(q),"Scene tint changed fading menu opacity");if(p!=q)changed=true;}check(changed,"Menu did not receive scene light");}finally{a.recycle();b.recycle();}
                org.json.JSONObject exported=model.exportSettings();model.set("scene_lighting",0);model.set("scene_light_strength",20);model.importSettings(exported);check(model.value("scene_lighting")==1&&model.value("scene_light_strength")==65,"Lighting settings did not survive export/import");
                check(layout.equals(live.preferences.getString("layout","")),"Lighting trial changed live desktop items");
            }catch(Throwable failure){error.set(failure);}finally{
                try{java.lang.reflect.Method apply=MainActivity.class.getDeclaredMethod("applySettings",boolean.class);apply.setAccessible(true);apply.invoke(activity,false);}catch(Exception failure){error.compareAndSet(null,failure);}
                isolated.getSharedPreferences("desktop3d",0).edit().clear().commit();isolated.getSharedPreferences("desktop3d_crystal",0).edit().clear().commit();
            }
        });if(error.get()!=null)throw new AssertionError(error.get());
    }
    private Bitmap render(Drawable d){Bitmap b=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);d.draw(c);if(d instanceof IconShellDrawable)((IconShellDrawable)d).drawCover(c);return b;}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
