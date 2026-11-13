package com.nth.desktop3d.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import com.nth.desktop3d.MainActivity;
import java.util.concurrent.atomic.AtomicReference;

/** Real Android integration test without a third-party runtime test framework. */
public final class SmokeInstrumentation extends Instrumentation {
    private MainActivity activity;
    private String outputDirectory;
    @Override public void onCreate(Bundle args){super.onCreate(args);outputDirectory=args==null?null:args.getString("additionalTestOutputDir");start();}
    private Bundle status(){Bundle b=new Bundle();b.putString("id","InstrumentationTestRunner");b.putInt("numtests",1);b.putInt("current",1);b.putString("class","com.nth.desktop3d.LauncherSmoke");b.putString("test","scenesMenusPersistenceAndLandscape");return b;}
    @Override public void onStart(){
        Bundle s=status();sendStatus(1,s);
        try{
            android.accessibilityservice.AccessibilityServiceInfo accessibility=getUiAutomation().getServiceInfo();
            accessibility.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS|android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            getUiAutomation().setServiceInfo(accessibility);
            // Quickstep is not the app under test; keep it from owning Home/recents input.
            shell("cmd package set-home-activity com.nth.desktop3d/.MainActivity");
            shell("am force-stop com.android.launcher3");
            // Restore a populated desktop before the first Activity measure: this crashed in 0.1.0.
            com.nth.desktop3d.LauncherStore restored=new com.nth.desktop3d.LauncherStore(getTargetContext());
            restored.page=0;restored.mode=0;
            while(restored.pages.get(0).size()<24)restored.pages.get(0).add(new com.nth.desktop3d.LauncherStore.Item("builtin:notes","Restored "+restored.pages.get(0).size()));
            restored.preferences.edit().putBoolean("appsSeeded",true).putBoolean("welcomed",true).commit();restored.save();
            ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
            getTargetContext().startActivity(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            activity=(MainActivity)monitor.waitForActivityWithTimeout(25000);removeMonitor(monitor);check(activity!=null,"Cold launch timed out");idle(1200);
            assertRestoredGrid();capture("restored-desktop");
            monitor=addMonitor(MainActivity.class.getName(),null,false);main(()->activity.recreate());
            activity=(MainActivity)monitor.waitForActivityWithTimeout(25000);removeMonitor(monitor);check(activity!=null,"Activity recreation timed out");idle(700);assertRestoredGrid();
            optionalButton("Explore the desktop");idle(400);
            for(String name:new String[]{"Spatial Desktop","Gravity Playground","Holographic Depth","Living Pond"}){
                invoke("showModes");clickScene(name);clickButton("Try this scene");idle(400);
                check(getTargetContext().getSharedPreferences("desktop3d",0).getInt("mode",-1)==java.util.Arrays.asList("Spatial Desktop","Gravity Playground","Holographic Depth","Living Pond").indexOf(name),"Scene mode not saved: "+name);
                capture(name.replace(' ','-'));
            }
            openBuiltin("Calculator");
            for(String key:new String[]{"2","×","(","3","+","4",")","="})clickButton(key);
            check(find(v->v instanceof TextView&&"14".contentEquals(((TextView)v).getText()))!=null,"Calculator did not return 14");back();
            openBuiltin("Nth Notes");main(()->{EditText e=(EditText)find(v->v instanceof EditText);check(e!=null,"Notes editor missing");e.setText("3D persistence smoke test");});back();
            openBuiltin("Nth Notes");check(find(v->v instanceof EditText&&((EditText)v).getText().toString().contains("persistence smoke test"))!=null,"Note did not persist");back();
            main(()->{View tile=find(v->android.text.TextUtils.equals("Nth Notes",v.getContentDescription()));check(tile!=null,"Desktop Notes shortcut missing");tile.performLongClick();});idle(400);
            check(find(v->v instanceof Button&&"Rename shortcut".contentEquals(((Button)v).getText()))!=null,"Long press did not open shortcut actions");back();
            int before=getTargetContext().getSharedPreferences("desktop3d",0).getInt("page",0);
            page(1);idle(650);
            check(getTargetContext().getSharedPreferences("desktop3d",0).getInt("page",0)==(before+1)%3,"Desktop did not advance");page(-1);idle(650);
            main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));idle(1400);
            main(()->{View content=activity.findViewById(android.R.id.content);check(content.getWidth()>content.getHeight(),"Landscape did not apply");});
            assertRestoredGrid();capture("landscape");
            invoke("showControls");check(find(v->v instanceof Button&&"Internet".contentEquals(((Button)v).getText()))!=null,"Device flyout missing");back();
            main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));idle(900);
            activity=new SettingsSmoke(this,activity,outputDirectory).run();
            new WidgetSmoke(this,activity,outputDirectory).run();
            s.putString("stream","Cold restored grid (24 shortcuts), Activity recreation, all scenes, tools, gestures, landscape and device flyout passed. Separate-UID widget content, live updates, native drag/reflow, resize, app/folder collision, rejected widget overlap, configuration save/cancel, pause/resume, recreation, rotation and removal passed. Preview gallery, browser-style pin confirmation/cancellation, shortcut target/name/icon persistence, folder copies, removal and ordinary app placement passed. Live settings preview, Nota commands/Undo, adaptive opacity and document backups passed.\n");sendStatus(0,s);
            Bundle result=new Bundle();result.putString("stream","OK (1 integration test)\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){try{capture("failure");}catch(Throwable ignored){}s.putString("stack",android.util.Log.getStackTraceString(error));s.putString("stream",error.toString());sendStatus(-2,s);Bundle result=new Bundle();result.putString("shortMsg",error.toString());finish(Activity.RESULT_CANCELED,result);}
    }
    private void assertRestoredGrid(){main(()->{
        ViewGroup stage=(ViewGroup)find(v->v.getClass().getSimpleName().equals("DesktopView"));
        check(stage!=null,"Desktop view missing");
        java.util.ArrayList<View> shortcuts=new java.util.ArrayList<>();
        collectShortcuts(stage,shortcuts);
        check(shortcuts.size()==24,"Restored desktop lost shortcuts: "+shortcuts.size());
        for(View tile:shortcuts){
            check(tile.getWidth()>0&&tile.getHeight()>0,"Shortcut has no bounds");
            check(tile.getTranslationX()>=0&&tile.getTranslationY()>=0,"Shortcut has negative position");
        }
    });}
    private void collectShortcuts(View view,java.util.List<View> result){
        if(view.getClass().getSimpleName().equals("IconTile"))result.add(view);
        if(view instanceof ViewGroup){
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)collectShortcuts(group.getChildAt(i),result);
        }
    }
    private void clickScene(String name){main(()->{View v=find(x->x.getClass().getSimpleName().equals("SceneCard")&&x.getContentDescription()!=null&&x.getContentDescription().toString().startsWith(name));check(v!=null,"Scene missing: "+name);v.performClick();});idle(350);}
    private void openBuiltin(String label){
        invoke("showStart");capture("Start-menu");clickButton("Nth tools");
        main(()->{EditText search=(EditText)find(v->v instanceof EditText);check(search!=null,"App search missing");search.setText(label);});idle(200);
        main(()->{GridView grid=(GridView)find(v->v instanceof GridView);check(grid!=null&&grid.getAdapter().getCount()==1,"Search must find exactly one Nth tool: "+label);grid.performItemClick(null,0,grid.getAdapter().getItemId(0));});idle(350);
    }
    private void invoke(String method){main(()->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(method);m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}});idle(350);}
    private void page(int direction){main(()->{try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("desktop");f.setAccessible(true);Object desktop=f.get(activity);java.lang.reflect.Method m=desktop.getClass().getDeclaredMethod("changePage",int.class);m.setAccessible(true);m.invoke(desktop,direction);}catch(Exception e){throw new AssertionError(e);}});}
    private interface Match{boolean matches(View v);}
    private View find(Match m){return findIn(activity.getWindow().getDecorView(),m);}
    private View findIn(View v,Match m){if(v.getVisibility()!=View.VISIBLE)return null;if(m.matches(v))return v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=group.getChildCount()-1;i>=0;i--){View found=findIn(group.getChildAt(i),m);if(found!=null)return found;}}return null;}
    private void clickButton(String text){main(()->{View v=find(x->x instanceof Button&&(((Button)x).getText().toString().equals(text)||((Button)x).getText().toString().equals("✓  "+text)));check(v!=null,"Button missing: "+text);v.performClick();});idle(350);}
    private void optionalButton(String text){main(()->{View v=find(x->x instanceof Button&&text.contentEquals(((Button)x).getText()));if(v!=null)v.performClick();});}
    private void clickDescription(String description,boolean required){main(()->{View v=find(x->android.text.TextUtils.equals(description,x.getContentDescription())||(x instanceof Button&&description.contentEquals(((Button)x).getText())));if(required)check(v!=null,"Control missing: "+description);if(v!=null)v.performClick();});idle(350);}
    private void capture(String name){
        android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"Cannot capture Android screen");
        java.io.File directory=outputDirectory==null?new java.io.File(getTargetContext().getExternalFilesDir(null),"verification"):new java.io.File(outputDirectory);
        directory.mkdirs();try(java.io.FileOutputStream stream=new java.io.FileOutputStream(new java.io.File(directory,name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}catch(java.io.IOException e){throw new AssertionError(e);}finally{bitmap.recycle();}
    }
    private void back(){main(()->activity.onBackPressed());idle(300);}
    private void shell(String command)throws java.io.IOException{try(android.os.ParcelFileDescriptor.AutoCloseInputStream input=new android.os.ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand(command))){byte[] bytes=new byte[1024];while(input.read(bytes)!=-1){}}}
    // A live wallpaper/physics loop need not leave the message queue completely idle.
    // Use a bounded animation wait and a UI-thread barrier instead of waiting forever.
    private void idle(long ms){SystemClock.sleep(ms);runOnMainSync(()->{});}
    private void main(Runnable r){AtomicReference<Throwable> failure=new AtomicReference<>();runOnMainSync(()->{try{r.run();}catch(Throwable e){failure.set(e);}});if(failure.get()!=null)throw new AssertionError(failure.get());}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
