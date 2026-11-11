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
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle(1200);
            optionalButton("Explore the desktop");idle(400);
            for(String name:new String[]{"Spatial Desktop","Gravity Playground","Holographic Depth","Living Pond"}){
                clickDescription("Scenes",false);clickButton(name);clickButton("Try this scene");idle(400);
                check(find(v->v instanceof TextView&&((TextView)v).getText().toString().contains(name))!=null,"Missing mode label "+name);
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
            clickDescription("Next desktop",true);idle(650);
            check(getTargetContext().getSharedPreferences("desktop3d",0).getInt("page",0)==(before+1)%3,"Desktop did not advance");clickDescription("Previous desktop",true);idle(650);
            main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));idle(1400);
            main(()->{View content=activity.findViewById(android.R.id.content);check(content.getWidth()>content.getHeight(),"Landscape did not apply");for(String desc:new String[]{"Open 3D Start menu","Device controls","Open calendar and time"}){View v=find(x->android.text.TextUtils.equals(desc,x.getContentDescription()));check(v!=null&&v.getWidth()>0&&v.getHeight()>0,"Missing dock control "+desc);int[] loc=new int[2];v.getLocationOnScreen(loc);check(loc[0]>=0&&loc[1]>=0,"Dock control offscreen "+desc);}});
            capture("landscape");
            clickDescription("Device controls",true);check(find(v->v instanceof Button&&"Internet".contentEquals(((Button)v).getText()))!=null,"Device flyout missing");back();
            main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));idle(900);
            s.putString("stream","All four scenes, app search, Calculator, note persistence, shortcut long press, page changes, landscape dock and device flyout passed.\n");sendStatus(0,s);
            Bundle result=new Bundle();result.putString("stream","OK (1 integration test)\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){try{capture("failure");}catch(Throwable ignored){}s.putString("stack",android.util.Log.getStackTraceString(error));s.putString("stream",error.toString());sendStatus(-2,s);Bundle result=new Bundle();result.putString("shortMsg",error.toString());finish(Activity.RESULT_CANCELED,result);}
    }
    private void openBuiltin(String label){
        clickDescription("Open 3D Start menu",true);capture("Start-menu");clickButton("Nth tools");
        main(()->{EditText search=(EditText)find(v->v instanceof EditText);check(search!=null,"App search missing");search.setText(label);});idle(200);
        main(()->{GridView grid=(GridView)find(v->v instanceof GridView);check(grid!=null&&grid.getAdapter().getCount()==1,"Search must find exactly one Nth tool: "+label);grid.performItemClick(null,0,grid.getAdapter().getItemId(0));});idle(350);
    }
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
    private void idle(long ms){waitForIdleSync();SystemClock.sleep(ms);waitForIdleSync();}
    private void main(Runnable r){AtomicReference<Throwable> failure=new AtomicReference<>();runOnMainSync(()->{try{r.run();}catch(Throwable e){failure.set(e);}});if(failure.get()!=null)throw new AssertionError(failure.get());}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
