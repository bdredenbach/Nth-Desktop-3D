package com.nth.desktop3d;

import android.app.Instrumentation;
import android.content.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.FrameLayout;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicReference;

/** Run with -e phase folders: actual native controls, interrupted motion and migration. */
public final class PhysicalFolders090Smoke {
    private final Instrumentation test;private final MainActivity activity;
    public PhysicalFolders090Smoke(Instrumentation test,MainActivity activity){this.test=test;this.activity=activity;}
    public void run(){
        main(this::defaults);
        LauncherStore store=(LauncherStore)field(activity,"store");boolean reduced=store.reducedMotion;boolean existed=store.preferences.contains("folder_animation");int previous=store.preferences.getInt("folder_animation",PanelAnimations.STACK);
        String layout=store.preferences.getString("layout","");FrameLayout root=(FrameLayout)field(activity,"root");LauncherStore.Item folder=new LauncherStore.Item("folder","Physical folder trial");folder.children.add(new LauncherStore.Item("builtin:notes","Trial note"));folder.children.add(new LauncherStore.Item("builtin:calculator","Trial calculator"));IconTile[] source=new IconTile[1];
        try{
            main(()->{invoke("closePanel");store.reducedMotion=false;store.preferences.edit().putInt("folder_animation",PanelAnimations.PHYSICAL).commit();source[0]=new IconTile(activity,folder.label,new IconTile.BuiltinIcon("folder"),"folder");source[0].setTag("shortcut:"+folder.id);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Ui.dp(activity,80),Ui.dp(activity,110));lp.leftMargin=Ui.dp(activity,24);lp.topMargin=Ui.dp(activity,140);root.addView(source[0],lp);});
            idle(100);main(()->invoke("showFolder",folder));idle(850);
            main(()->{check(field(activity,"folderMotion")!=null,"Physical folder controller missing (run with system animation scale enabled)");View panel=(View)field(activity,"panel");check(panel.getAlpha()==1&&panel.getScaleX()==1,"Folder did not settle");check(source[0].getAlpha()==0,"Duplicate source shell remained visible");check(find(panel,"shortcut:"+folder.children.get(0).id).hasOnClickListeners(),"Native tap listener missing");invoke("requestClosePanel");});idle(450);
            main(()->{check(field(activity,"overlay")==null&&source[0].getAlpha()==1,"Reverse close stranded the folder icon");invoke("showFolder",folder);activity.onBackPressed();});idle(450);
            main(()->{check(field(activity,"overlay")==null,"Back before first layout reopened the folder");invoke("showFolder",folder);});idle(80);main(()->activity.onBackPressed());idle(450);
            main(()->{check(field(activity,"overlay")==null&&source[0].getAlpha()==1,"Back during opening did not finish closing");invoke("showFolder",folder);});idle(60);main(()->invoke("showCalculator"));idle(650);
            main(()->{check(field(activity,"overlay")!=null&&field(activity,"folderMotion")==null&&source[0].getAlpha()==1,"Old animation removed its replacement panel");invoke("showFolder",folder);});idle(700);
            main(()->{View tile=find((View)field(activity,"panel"),"shortcut:"+folder.children.get(0).id);check(tile.performLongClick(),"Native hold menu did not respond");check(field(activity,"folderMotion")==null&&source[0].getAlpha()==1,"Hold menu left stale motion");invoke("showFolder",folder);});idle(700);
            main(()->{find((View)field(activity,"panel"),"shortcut:"+folder.children.get(0).id).performClick();check(field(activity,"folderMotion")==null,"App tap retained old animation");store.reducedMotion=true;invoke("showFolder",folder);check(field(activity,"folderMotion")==null,"Reduced motion still animated");check(((View)field(activity,"panel")).getAlpha()==1,"Reduced motion left content hidden");check(layout.equals(store.preferences.getString("layout","")),"Animation changed canonical layout");});
        }finally{main(()->{invoke("closePanel");if(source[0]!=null)root.removeView(source[0]);store.reducedMotion=reduced;SharedPreferences.Editor editor=store.preferences.edit();if(existed)editor.putInt("folder_animation",previous);else editor.remove("folder_animation");editor.commit();invoke("applySettings",false);});}
    }
    private void defaults(){
        String prefix="physical090_"+SystemClock.uptimeMillis();Context isolated=new ContextWrapper(activity){@Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences(prefix+name,mode);}};
        try{LauncherStore fresh=new LauncherStore(isolated);SettingsModel model=new SettingsModel(fresh,new CrystalPreferences(isolated),rebuild->{});check(model.value("icon_shape")==0&&model.value("folder_shape")==1&&model.value("scene_lighting")==1&&model.value("scene_light_strength")==100,"Fresh initialization defaults differ");check("look".equals(model.specs.get("folder_shape").page),"Folder shape is not in Look & feel");model.set("folder_shape",2);model.set("scene_light_strength",0);model.set("scene_lighting",0);new LauncherStore(isolated);check(model.value("folder_shape")==2&&model.value("scene_light_strength")==0&&model.value("scene_lighting")==0,"Reinitialization replaced saved choices");int menu=model.value("menu_animation");model.set("folder_animation",PanelAnimations.PHYSICAL);check(model.value("menu_animation")==menu,"Physical folder changed menu opening");fresh.preferences.edit().clear().putBoolean("seeded",true).commit();LauncherStore legacy=new LauncherStore(isolated);check(legacy.preferences.getInt("folder_shape",-1)==0&&legacy.preferences.getInt("scene_light_strength",-1)==65,"Upgrade changed implicit legacy appearance");}
        finally{isolated.getSharedPreferences("desktop3d",0).edit().clear().commit();isolated.getSharedPreferences("desktop3d_crystal",0).edit().clear().commit();}
    }
    private Object field(Object object,String name){try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name,Object... args){try{Class<?>[] types=new Class<?>[args.length];for(int i=0;i<args.length;i++)types[i]=args[i] instanceof Boolean?boolean.class:args[i].getClass();Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}}
    private View find(View view,String tag){if(tag.equals(view.getTag()))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),tag);if(found!=null)return found;}}return null;}
    private void main(Runnable task){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{task.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.waitForIdleSync();}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
