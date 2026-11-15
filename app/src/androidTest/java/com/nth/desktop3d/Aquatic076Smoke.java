package com.nth.desktop3d;

import android.app.Instrumentation;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.util.concurrent.atomic.AtomicReference;

/** Fresh-install defaults, saved choices, shared effect controls and photographic texture checks. */
public final class Aquatic076Smoke {
    private final Instrumentation test;private final MainActivity activity;private final String output;
    public Aquatic076Smoke(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;}
    public void run(){
        String prefix="aquatic076_"+SystemClock.uptimeMillis()+"_";
        Context isolated=new ContextWrapper(activity){@Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences(prefix+name,mode);}};
        main(()->{try{
            LauncherStore fresh=new LauncherStore(isolated);CrystalPreferences crystal=new CrystalPreferences(isolated);SettingsModel model=new SettingsModel(fresh,crystal,rebuild->{});
            check(fresh.transition==PageTransitions.STACK&&crystal.menuAnimation()==PanelAnimations.STACK&&model.value("folder_animation")==PanelAnimations.STACK,"Fresh install did not use 3D Stack everywhere");
            for(String id:new String[]{"transition","menu_animation","folder_animation"})check("look".equals(model.specs.get(id).page),"Effect is not in Look & feel: "+id);
            model.set("transition",2);model.set("menu_animation",1);model.set("folder_animation",5);
            check(new LauncherStore(isolated).transition==2&&new CrystalPreferences(isolated).menuAnimation()==1&&model.value("folder_animation")==5,"Saved effect choices were reset");
            model.useStackAnimations();check(fresh.transition==4&&crystal.menuAnimation()==3&&model.value("folder_animation")==3,"Shared Stack preset did not apply all three effects");
            model.undo();check(fresh.transition==2&&crystal.menuAnimation()==1&&model.value("folder_animation")==5,"Stack preset Undo did not restore all three choices");
            for(int image:new int[]{R.drawable.pond_bed,R.drawable.reef_water,R.drawable.koi_fish,R.drawable.jellyfish}){
                BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inPremultiplied=false;
                Bitmap bitmap=BitmapFactory.decodeResource(activity.getResources(),image,options);check(bitmap!=null&&bitmap.getWidth()>=768&&bitmap.getHeight()>bitmap.getWidth(),"Aquatic texture failed to decode");
                try{if(image==R.drawable.koi_fish||image==R.drawable.jellyfish)check(bitmap.hasAlpha()&&Color.alpha(bitmap.getPixel(0,0))==0,"Animal cutout lost its transparent background");}finally{bitmap.recycle();}
            }
        }finally{isolated.getSharedPreferences("desktop3d",0).edit().clear().commit();isolated.getSharedPreferences("desktop3d_crystal",0).edit().clear().commit();}});
        invoke("showSettings",new Class[]{String.class},"look");
        main(()->{View parent=null;for(String id:new String[]{"transition","menu_animation","folder_animation"}){View control=find(v->("setting:"+id).equals(v.getTag()));check(control!=null&&control.isShown(),"Effect hidden behind Advanced: "+id);if(parent==null)parent=(View)control.getParent();else check(parent==control.getParent(),"Effects do not share the top-level settings body");}});
        capture("look-feel-all-effects");
        tap(find(v->v instanceof Button&&((Button)v).getText().toString().startsWith("Desktop transition")));
        main(()->{check(find(v->v instanceof Button&&"Advanced  ▾".contentEquals(((Button)v).getText()))==null,"Transition choices are hidden behind Advanced");for(String name:PageTransitions.NAMES)check(find(v->v instanceof Button&&v.getContentDescription()!=null&&(name.contentEquals(v.getContentDescription())||("✓  "+name).contentEquals(v.getContentDescription())))!=null,"Missing transition: "+name);});
        capture("desktop-effect-choices");
        invoke("showSettings",new Class[]{String.class},"look");tap(find(v->"Use 3D Stack everywhere".contentEquals(v.getContentDescription())));
        main(()->{LauncherStore store=(LauncherStore)field("store");SettingsModel model=(SettingsModel)field("settingsModel");check(store.transition==4&&new CrystalPreferences(activity).menuAnimation()==3&&model.value("folder_animation")==3,"Preset tap did not update the live launcher");});
        capture("stack-preset-selected");invoke("closePanel",new Class[]{});
        // Inspect the live GL surface with a clear desktop, without changing saved shortcuts.
        for(int mode:new int[]{3,6}){
            final int chosen=mode;main(()->{LauncherStore store=(LauncherStore)field("store");store.mode=chosen;store.save();((View)field("desktop")).setVisibility(View.INVISIBLE);});invoke("applySettings",new Class[]{boolean.class},false);idle(1200);capture(mode==3?"lifelike-koi-live":"lifelike-jellyfish-live");
        }
        main(()->((View)field("desktop")).setVisibility(View.VISIBLE));
    }
    private Object field(String name){try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name,Class<?>[] types,Object... args){main(()->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}});idle(600);}
    private void tap(View v){check(v!=null,"Tap target missing");AtomicReference<int[]> point=new AtomicReference<>();main(()->{v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);});idle(180);main(()->{int[] p=new int[2];v.getLocationOnScreen(p);point.set(new int[]{p[0]+v.getWidth()/2,p[1]+v.getHeight()/2});});long now=SystemClock.uptimeMillis();for(int action:new int[]{0,1}){MotionEvent e=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,point.get()[0],point.get()[1],0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);test.getUiAutomation().injectInputEvent(e,true);e.recycle();}idle(600);}
    private View find(java.util.function.Predicate<View> match){return find(activity.getWindow().getDecorView(),match);}
    private View find(View v,java.util.function.Predicate<View> match){if(v.getVisibility()!=View.VISIBLE)return null;if(match.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),match);if(found!=null)return found;}return null;}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private void capture(String name){idle(800);Bitmap image=test.getUiAutomation().takeScreenshot();check(image!=null,"Screenshot unavailable");java.io.File dir=output==null?new java.io.File(test.getTargetContext().getExternalFilesDir(null),"verification"):new java.io.File(output);dir.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new AssertionError(e);}finally{image.recycle();}}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
