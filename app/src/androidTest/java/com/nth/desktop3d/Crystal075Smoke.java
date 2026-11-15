package com.nth.desktop3d;

import android.app.Instrumentation;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.util.concurrent.atomic.AtomicReference;

/** Focused native input and persistence checks for the crystal surfaces and appended scenes. */
public final class Crystal075Smoke {
    private final Instrumentation test;private final MainActivity activity;private final String output;
    public Crystal075Smoke(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;}
    public void run(){
        LauncherStore store=(LauncherStore)field("store");
        String original=store.preferences.getString("layout","");
        invoke("showStart");
        main(()->{EditText search=(EditText)find(v->v instanceof EditText);check(search!=null&&"Search".contentEquals(search.getHint()),"Drawer hint was not simplified");AppCatalog catalog=(AppCatalog)field("catalog");check(catalog.filter("About Nth Desktop 3D",true).isEmpty(),"About tile remains in drawer");check(find(v->v instanceof TextView&&((TextView)v).getText().toString().startsWith("Hold and move an app"))==null,"Drawer footer remains");});
        capture("crystal-drawer");
        invoke("showCards",new Class[]{String.class},"");capture("crystal-cards");
        invoke("showDesktopManager");
        main(()->{View first=find(v->"desktop-preview:0".equals(v.getTag())),add=find(v->"desktop-add-preview".equals(v.getTag()));check(first!=null&&add!=null&&Math.abs(first.getHeight()-add.getHeight())<2,"Desktop and Add cards differ in height");check(first.getBackground() instanceof SolidCrystalDrawable&&add.getBackground() instanceof SolidCrystalDrawable,"Desktop previews lost their solid crystal core");});
        capture("crystal-desktops");
        invoke("showModes");
        main(()->{check(find(v->v instanceof Button&&"Try this scene".contentEquals(((Button)v).getText()))==null,"Obsolete scene action remains");check(find(v->v instanceof Button&&"Menus and transitions".contentEquals(((Button)v).getText()))==null,"Obsolete scene settings shortcut remains");});
        capture("crystal-scene-choices");
        for(int mode=0;mode<LauncherStore.MODES.length;mode++){
            if(mode>0)invoke("showModes");
            final int chosen=mode;AtomicReference<View> target=new AtomicReference<>();
            main(()->{View card=find(v->("scene-card:"+chosen).equals(v.getTag()));check(card!=null,"Scene choice missing");target.set(card);card.requestRectangleOnScreen(new Rect(0,0,card.getWidth(),card.getHeight()),true);});idle(250);
            tap(target.get());
            main(()->{check(field("overlay")==null,"Scene tap did not return to desktop");check(store.mode==chosen&&new LauncherStore(activity).mode==chosen,"Scene was not persisted: "+chosen);check(original.equals(store.preferences.getString("layout","")),"Changing a scene altered shortcut layout");});
            capture("scene-"+chosen);
        }
        // Every appended scene must be valid for the settings preview and restored store.
        for(int mode=4;mode<LauncherStore.MODES.length;mode++){
            final int chosen=mode;main(()->{store.mode=chosen;store.save();});
            invoke("showSettings",new Class[]{String.class},"motion");capture("scene-settings-"+mode);
        }
        invoke("closePanel");
        main(()->{
            for(int material=0;material<4;material++){
                CrystalPreferences prefs=new CrystalPreferences(activity);int previous=prefs.material();prefs.setMaterial(material);
                Bitmap a=Bitmap.createBitmap(200,160,Bitmap.Config.ARGB_8888),b=Bitmap.createBitmap(200,160,Bitmap.Config.ARGB_8888);
                try{SolidCrystalDrawable d=new SolidCrystalDrawable(activity,20,true);d.setBounds(0,0,200,160);Canvas ca=new Canvas(a),cb=new Canvas(b);ca.drawColor(Color.MAGENTA);cb.drawColor(Color.GREEN);d.draw(ca);d.draw(cb);check(a.getPixel(100,80)==b.getPixel(100,80),"Crystal center exposes background");check(a.getPixel(100,80)!=a.getPixel(100,5),"Crystal bevel is flat");}finally{a.recycle();b.recycle();prefs.setMaterial(previous);}
            }
        });
        invoke("showModes");main(()->{View card=find(v->"scene-card:6".equals(v.getTag()));check(card!=null&&card.isSelected(),"Current scene checkmark lost");card.requestRectangleOnScreen(new Rect(0,0,card.getWidth(),card.getHeight()),true);});
        capture("crystal-selected-reef");invoke("closePanel");
    }
    private Object field(String name){try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name){invoke(name,new Class[]{});}
    private void invoke(String name,Class<?>[] types,Object... args){main(()->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}});idle(350);}
    private void tap(View view){AtomicReference<int[]> location=new AtomicReference<>();main(()->{int[] p=new int[2];view.getLocationOnScreen(p);location.set(new int[]{p[0]+view.getWidth()/2,p[1]+view.getHeight()/2});});int[] p=location.get();long now=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,p[0],p[1],0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);test.getUiAutomation().injectInputEvent(e,true);e.recycle();}idle(450);}
    private View find(java.util.function.Predicate<View> match){return find(activity.getWindow().getDecorView(),match);}
    private View find(View v,java.util.function.Predicate<View> match){if(v.getVisibility()!=View.VISIBLE)return null;if(match.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),match);if(found!=null)return found;}return null;}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private void capture(String name){Bitmap image=test.getUiAutomation().takeScreenshot();check(image!=null,"Screenshot unavailable");java.io.File dir=output==null?new java.io.File(test.getTargetContext().getExternalFilesDir(null),"verification"):new java.io.File(output);dir.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new AssertionError(e);}finally{image.recycle();}}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
