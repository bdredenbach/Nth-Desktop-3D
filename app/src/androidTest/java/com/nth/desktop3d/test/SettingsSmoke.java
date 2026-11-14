package com.nth.desktop3d.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import com.nth.desktop3d.LauncherStore;
import com.nth.desktop3d.MainActivity;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Native settings, real preference/desktop effects, spatial material and document I/O. */
final class SettingsSmoke {
    private final Instrumentation instrumentation;private MainActivity activity;private final String outputDirectory;
    SettingsSmoke(Instrumentation i,MainActivity a,String out){instrumentation=i;activity=a;outputDirectory=out;}
    MainActivity run()throws Exception{
        invoke("showSettings");
        final org.json.JSONObject initial=exportCurrentSettings();await(()->studio()!=null,"Settings did not open");capture("settings-categories");
        click("Home screen");await(()->find(v->v instanceof SeekBar&&android.text.TextUtils.equals("Desktop icon size",v.getContentDescription()))!=null,"Home controls missing");
        setProgress("Desktop icon size",10);check(prefs().getInt("icon_size",0)==120,"Native icon control did not save 120 percent");capture("settings-live-home-preview");
        View preview=find(v->v.getClass().getSimpleName().equals("SettingsPreview"));check(preview!=null,"Live preview missing");
        int[] position=position(preview);long time=SystemClock.uptimeMillis();pointer(time,MotionEvent.ACTION_DOWN,position[0]+preview.getWidth()/2f,position[1]+preview.getHeight()/2f);pointer(time,MotionEvent.ACTION_MOVE,position[0]+preview.getWidth()/2f+55,position[1]+preview.getHeight()/2f);
        AtomicReference<Float> rotation=new AtomicReference<>(0f);main(()->rotation.set(preview.getRotationY()));check(Math.abs(rotation.get())>1,"Preview drag did not rotate its 3D plane");pointer(time,MotionEvent.ACTION_UP,position[0]+preview.getWidth()/2f+55,position[1]+preview.getHeight()/2f);idle(400);
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(MainActivity.class.getName(),null,false);main(()->activity.recreate());activity=(MainActivity)monitor.waitForActivityWithTimeout(25000);instrumentation.removeMonitor(monitor);check(activity!=null,"Settings recreation timed out");await(()->studio()!=null,"Settings page was lost on recreation");check(prefs().getInt("icon_size",0)==120,"Settings were lost on recreation");
        rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);capture("settings-landscape");rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        click("Ask Nota");command("set grid to five columns");check(prefs().getInt("grid_columns",0)==5,"Nth Nota command did not change the grid");click("Undo last setting change");check(prefs().getInt("grid_columns",0)==4,"Undo did not restore the grid");
        command("set grid to 99 columns");check(prefs().getInt("grid_columns",0)==4,"Out-of-range command mutated the grid");
        command("show folder settings");await(()->find(v->v instanceof SeekBar&&android.text.TextUtils.equals("Folder columns",v.getContentDescription()))!=null,"Nota did not navigate to folder settings");capture("settings-folder-preview");
        main(()->activity.onBackPressed());main(()->{EditText search=(EditText)find(v->v instanceof EditText&&android.text.TextUtils.equals("Search settings",v.getContentDescription()));search.setText("opacity");});idle(350);click("Opacity over content");setProgress("Opacity over content",8);check(prefs().getInt("glass_strength",0)==60,"Opacity control was not connected");
        await(()->activityPanel().getBackground().getClass().getSimpleName().equals("AdaptiveMenuDrawable"),"Adaptive material missing");idle(1200);
        main(()->{View panel=activityPanel();Bitmap bitmap=Bitmap.createBitmap(panel.getWidth(),panel.getHeight(),Bitmap.Config.ARGB_8888);panel.getBackground().draw(new Canvas(bitmap));int[] data=new int[bitmap.getWidth()*bitmap.getHeight()];bitmap.getPixels(data,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());int clear=0,dark=0,soft=0;for(int pixel:data){int alpha=pixel>>>24;if(alpha==0)clear++;if(alpha>100)dark++;if(alpha>5&&alpha<100)soft++;}bitmap.recycle();check(clear>0&&dark>0&&soft>0,"Menu background did not contain transparent, opaque and feathered regions");});capture("adaptive-menu-gradient");
        Switch adaptive=(Switch)find(v->v instanceof Switch&&"Fading menu backgrounds".contentEquals(((Switch)v).getText()));check(adaptive!=null,"Fading glass switch missing");scrollTo(adaptive);tap(adaptive);idle(300);check(!activityPanel().getBackground().getClass().getSimpleName().equals("AdaptiveMenuDrawable"),"Disabling adaptive backgrounds had no effect");tap(adaptive);idle(350);
        click("Ask Nota");command("turn off tilt");check(!new LauncherStore(instrumentation.getTargetContext()).tilt,"Nota tilt command did not persist");click("Undo last setting change");check(new LauncherStore(instrumentation.getTargetContext()).tilt,"Nota undo did not restore tilt");capture("nth-nota-settings-assistant");
        // Exercise the Android document result contract and real bytes, without depending on picker UI text.
        main(()->activity.onBackPressed());click("Settings backup & restore");
        java.io.File file=new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null),"settings-smoke.nth3dsettings");
        monitor=instrumentation.addMonitor(documentFilter(Intent.ACTION_CREATE_DOCUMENT),new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(android.net.Uri.fromFile(file))),true);
        click("Export settings");idle(500);instrumentation.removeMonitor(monitor);check(file.isFile()&&file.length()>100,"Settings document was not written");
        click("Ask Nota");command("set grid to six columns");check(prefs().getInt("grid_columns",0)==6,"Restore setup command failed");main(()->activity.onBackPressed());click("Settings backup & restore");
        monitor=instrumentation.addMonitor(documentFilter(Intent.ACTION_OPEN_DOCUMENT),new Instrumentation.ActivityResult(Activity.RESULT_OK,new Intent().setData(android.net.Uri.fromFile(file))),true);
        click("Restore settings");idle(500);instrumentation.removeMonitor(monitor);check(prefs().getInt("grid_columns",0)==4,"Settings document did not restore the grid");
        check(prefs().getString("note","").contains("3D persistence"),"Settings restore replaced saved notes");capture("settings-backup-restored");
        main(()->activity.onBackPressed());click("Gestures");check(find(v->v instanceof android.widget.Button&&((android.widget.Button)v).getText().toString().startsWith("Swipe up"))!=null,"Gesture settings missing");capture("settings-gesture-actions");
        main(()->activity.onBackPressed());main(()->activity.onBackPressed());idle(300);
        main(()->{try{Object model=field(activity,"settingsModel");java.lang.reflect.Method restore=model.getClass().getDeclaredMethod("importSettings",org.json.JSONObject.class);restore.setAccessible(true);restore.invoke(model,initial);}catch(Exception error){throw new AssertionError(error);}});idle(400);return activity;
    }
    private org.json.JSONObject exportCurrentSettings(){AtomicReference<org.json.JSONObject> result=new AtomicReference<>();main(()->{try{Object model=field(activity,"settingsModel");java.lang.reflect.Method method=model.getClass().getDeclaredMethod("exportSettings");method.setAccessible(true);result.set((org.json.JSONObject)method.invoke(model));}catch(Exception error){throw new AssertionError(error);}});return result.get();}
    private IntentFilter documentFilter(String action){IntentFilter filter=new IntentFilter(action);filter.addCategory(Intent.CATEGORY_OPENABLE);try{filter.addDataType("*/*");}catch(IntentFilter.MalformedMimeTypeException impossible){throw new AssertionError(impossible);}return filter;}
    private android.content.SharedPreferences prefs(){return instrumentation.getTargetContext().getSharedPreferences("desktop3d",0);}
    private Object studio(){return field(activity,"settingsStudio");}private View activityPanel(){return (View)field(activity,"panel");}
    private void command(String text){main(()->{EditText question=(EditText)find(v->v instanceof EditText&&android.text.TextUtils.equals("Ask Nth Nota",v.getContentDescription()));check(question!=null,"Nota input missing");question.setText(text);});click("Send to Nth Nota AI");idle(350);}
    private void setProgress(String label,int step){SeekBar view=(SeekBar)find(v->v instanceof SeekBar&&android.text.TextUtils.equals(label,v.getContentDescription()));check(view!=null,"Slider missing: "+label);scrollTo(view);main(()->{Bundle args=new Bundle();args.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,step);check(view.performAccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),args),"Accessible slider action failed");});idle(350);}
    private View control(String text){return find(v->android.text.TextUtils.equals(text,v.getContentDescription())||(v instanceof TextView&&text.contentEquals(((TextView)v).getText())));}
    private void click(String text){await(()->{View view=control(text);return view!=null&&view.isAttachedToWindow()&&view.getWidth()>0&&view.getHeight()>0;},"Settings control missing or not laid out: "+text);AtomicReference<View> target=new AtomicReference<>();main(()->target.set(control(text)));View view=target.get();check(view!=null,"Settings control disappeared: "+text);scrollTo(view);tap(view);idle(350);}
    private void rotate(int orientation){main(()->activity.setRequestedOrientation(orientation));await(()->{View root=activity.getWindow().getDecorView();boolean landscape=orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;return (root.getWidth()>root.getHeight())==landscape&&activity.getResources().getConfiguration().orientation==(landscape?android.content.res.Configuration.ORIENTATION_LANDSCAPE:android.content.res.Configuration.ORIENTATION_PORTRAIT)&&studio()!=null&&control("Ask Nota")!=null;},"Settings were lost after rotation");idle(400);}
    private void scrollTo(View view){main(()->view.requestRectangleOnScreen(new Rect(0,0,Math.max(1,view.getWidth()),Math.max(1,view.getHeight())),true));idle(250);}
    private int[] position(View view){AtomicReference<int[]> result=new AtomicReference<>();main(()->{int[] p=new int[2];view.getLocationOnScreen(p);result.set(p);});return result.get();}
    private void tap(View view){int[] p=position(view);long t=SystemClock.uptimeMillis();pointer(t,MotionEvent.ACTION_DOWN,p[0]+view.getWidth()/2f,p[1]+view.getHeight()/2f);pointer(t,MotionEvent.ACTION_UP,p[0]+view.getWidth()/2f,p[1]+view.getHeight()/2f);}
    private void pointer(long time,int action,float x,float y){MotionEvent event=MotionEvent.obtain(time,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(instrumentation.getUiAutomation().injectInputEvent(event,true),"Settings native touch failed");}finally{event.recycle();}}
    private interface Match{boolean accepts(View view);}private View find(Match match){return findIn(activity.getWindow().getDecorView(),match);}private View findIn(View view,Match match){if(view.getVisibility()!=View.VISIBLE)return null;if(match.accepts(view))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View result=findIn(((ViewGroup)view).getChildAt(i),match);if(result!=null)return result;}return null;}
    private Object field(Object target,String name){try{java.lang.reflect.Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name){main(()->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}});idle(500);}
    private void await(BooleanSupplier condition,String message){long end=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<end){AtomicReference<Boolean> value=new AtomicReference<>(false);main(()->value.set(condition.getAsBoolean()));if(value.get())return;idle(100);}throw new AssertionError(message);}
    private void main(Runnable task){AtomicReference<Throwable> error=new AtomicReference<>();instrumentation.runOnMainSync(()->{try{task.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);instrumentation.runOnMainSync(()->{});}
    private void capture(String name){idle(700);Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();check(bitmap!=null,"Settings screenshot failed");java.io.File folder=outputDirectory==null?new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null),"verification"):new java.io.File(outputDirectory);folder.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(folder,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}catch(java.io.IOException e){throw new AssertionError(e);}finally{bitmap.recycle();}}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
