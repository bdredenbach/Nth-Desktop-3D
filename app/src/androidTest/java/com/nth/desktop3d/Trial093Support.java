package com.nth.desktop3d;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

/** Shared native fixture helpers; every suite restores its complete launcher snapshot. */
abstract class Trial093Support {
    final Instrumentation test;final MainActivity activity;final String output;final LauncherStore store;final DesktopView desktop;final SettingsModel model;
    private DesktopBackup.Prepared original;private LayeredScene.Document draft;
    Trial093Support(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;AtomicReference<SettingsModel> m=new AtomicReference<>();main(()->m.set((SettingsModel)call(activity,"settingsModel")));model=m.get();store=(LauncherStore)field(activity,"store");desktop=(DesktopView)field(activity,"desktop");}
    void snapshot(){main(()->{try{original=new DesktopBackup.Prepared(DesktopBackup.capture(activity,model),model);draft=LayeredScene.read(activity,true);}catch(Exception e){throw new AssertionError(e);}});}
    void restore()throws Exception {main(()->call(activity,"closePanel"));filesIdle();main(()->{try{model.importDesktopBackup(activity,original);LayeredScene.write(activity,draft,true);}catch(Exception e){throw new AssertionError(e);}});}
    void filesIdle()throws Exception {((ExecutorService)field(activity,"sceneFiles")).submit(()->{}).get(20,TimeUnit.SECONDS);main(()->{});}
    Object field(Object object,String name){try{java.lang.reflect.Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new AssertionError(e);}}
    void set(Object object,String name,Object value){try{java.lang.reflect.Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}catch(Exception e){throw new AssertionError(e);}}
    Object call(Object object,String name,Object... arguments){try{for(java.lang.reflect.Method m:object.getClass().getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==arguments.length){m.setAccessible(true);return m.invoke(object,arguments);}throw new NoSuchMethodException(name);}catch(Exception e){throw new AssertionError(e);}}
    View find(Predicate<View> match){return find(activity.getWindow().getDecorView(),match);}View find(View root,Predicate<View> match){if(root==null||root.getVisibility()!=View.VISIBLE)return null;if(match.test(root))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View v=find(group.getChildAt(i),match);if(v!=null)return v;}}return null;}
    Button button(String title){return (Button)find(v->v instanceof Button&&(title.contentEquals(((Button)v).getText())||title.contentEquals(Objects.toString(v.getContentDescription(),""))));}
    void click(String title){main(()->{Button b=button(title);check(b!=null,"Missing control: "+title);b.performClick();});}
    void systemClick(String text){long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();if(root!=null)for(AccessibilityNodeInfo node:root.findAccessibilityNodeInfosByText(text))if(text.equalsIgnoreCase(Objects.toString(node.getText(),""))&&node.isEnabled()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK))return;SystemClock.sleep(100);}throw new AssertionError("Dialog action missing: "+text);}
    void main(Runnable work){AtomicReference<Throwable> failed=new AtomicReference<>();test.runOnMainSync(()->{try{work.run();}catch(Throwable t){failed.set(t);}});if(failed.get()!=null)throw new AssertionError(failed.get());}
    void await(BooleanSupplier ready,String message){long end=SystemClock.uptimeMillis()+15000;while(SystemClock.uptimeMillis()<end){AtomicReference<Boolean> value=new AtomicReference<>(false);main(()->value.set(ready.getAsBoolean()));if(value.get())return;SystemClock.sleep(100);}throw new AssertionError(message);}
    void pointer(long start,int action,float x,float y){MotionEvent e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(test.getUiAutomation().injectInputEvent(e,true),"Touch injection failed");}finally{e.recycle();}}
    void move(long start,float x,float y,float toX,float toY){for(int i=1;i<=10;i++){pointer(start,MotionEvent.ACTION_MOVE,x+(toX-x)*i/10,y+(toY-y)*i/10);SystemClock.sleep(30);}}
    Rect rect(View view){Rect rect=new Rect();main(()->check(view!=null&&view.getGlobalVisibleRect(rect),"Target has no visible bounds"));return rect;}
    void capture(String name)throws Exception {SystemClock.sleep(700);Bitmap bitmap=test.getUiAutomation().takeScreenshot();check(bitmap!=null,"Screenshot unavailable");File folder=output==null?new File(activity.getExternalFilesDir(null),"verification"):new File(output);folder.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}finally{bitmap.recycle();}}
    void delete(File file){File[] children=file.listFiles();if(children!=null)for(File child:children)delete(child);file.delete();}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
