package com.nth.desktop3d;

import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.view.accessibility.AccessibilityNodeInfo;import android.widget.*;import java.io.*;import java.util.concurrent.atomic.AtomicReference;

/** Replays the recorded Home handoff and verifies visible controls, grants and cancellation. */
public final class Setup083Smoke {
    private final Instrumentation test;private MainActivity activity;private final String output;private LauncherStore store;
    public Setup083Smoke(Instrumentation t,MainActivity a,String o){test=t;activity=a;output=o;}
    public MainActivity run()throws Exception {
        store=(LauncherStore)field("store");boolean reduced=store.reducedMotion;int menu=new CrystalPreferences(activity).menuAnimation();
        try{
            main(()->{store.reducedMotion=false;new CrystalPreferences(activity).setMenuAnimation(PanelAnimations.STACK);});
            // Unlike the old already-default fixture, this opens Android's actual chooser.
            shell("cmd package set-home-activity com.nth.desktop3d.widgetfixture/.FixtureHomeActivity");check(!SetupAccess.home(activity),"Fixture did not replace the default Home");
            invoke("showSetup",new Class[]{int.class},0);Instrumentation.ActivityMonitor homeReturn=test.addMonitor(MainActivity.class.getName(),null,false);click("Set as Home app");
            long until=SystemClock.uptimeMillis()+12000;
            while(SystemClock.uptimeMillis()<until&&!SetupAccess.home(activity)){
                systemClick("Nth Desktop 3D");for(String text:new String[]{"Set as default","Set default","Always","Use app"})systemClick(text);idle(150);
            }
            check(SetupAccess.home(activity),"Actual Home chooser did not select Nth Desktop 3D");await(()->{Activity returned=homeReturn.getLastActivity();if(returned instanceof MainActivity&&returned!=activity){activity=(MainActivity)returned;store=(LauncherStore)field("store");}return nextVisible();},"Setup did not reappear after choosing Home");test.removeMonitor(homeReturn);check(guide().step()==0,"Home chooser skipped the current setup step");
            main(()->{View button=find("Set as Home app");check(button!=null&&!button.isEnabled(),"Selected Home still offers an active enable button");});capture("083-real-home-chooser-return");
            // Recover an interrupted/invisible opening pose through the result callback alone.
            main(()->{View panel=(View)field("panel"),overlay=(View)field("overlay");panel.setAlpha(0);panel.setRotationX(-28);overlay.setVisibility(View.INVISIBLE);call("onActivityResult",new Class[]{int.class,int.class,Intent.class},101,Activity.RESULT_CANCELED,null);});await(this::nextVisible,"Canceled Home result left setup invisible");
            main(()->{View panel=(View)field("panel");check(panel.getAlpha()>.99f&&Math.abs(panel.getRotationX())<.01,"Interrupted opening pose was not settled");});
            // Next is a real tap, proving the recovered panel owns input without a shade round trip.
            tap("Next");check(guide().step()==1,"Next is unresponsive after the Home chooser");
            shell("cmd notification disallow_listener com.nth.desktop3d/.MediaAccess");focus();check(!SetupAccess.media(activity)&&findEnabled("Enable media access"),"Missing access not offered");
            IntentFilter filter=new IntentFilter(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);filter.addDataScheme("package");Instrumentation.ActivityMonitor denied=test.addMonitor(filter,new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true);click("Open App info");test.removeMonitor(denied);check(denied.getHits()==1&&guide().step()==1,"Denied App info return changed the current step");check(findEnabled("Enable media access")&&findEnabled("Open App info"),"Restricted-setting recovery controls disappeared");
            shell("cmd notification allow_listener com.nth.desktop3d/.MediaAccess");await(()->SetupAccess.media(activity),"Media access did not enable");focus();check(!findEnabled("Enable media access"),"Granted media access still offers Enable");capture("083-media-granted-status");
            click("Next");check(guide().step()==2,"Media step auto-skipped or Next failed");
            // Android's list, rather than a stale bound-service pointer, owns this label.
            main(()->{try{java.lang.reflect.Field f=GestureAccess.class.getDeclaredField("active");f.setAccessible(true);f.set(null,new java.lang.ref.WeakReference<>(new GestureAccess()));}catch(Exception e){throw new AssertionError(e);}});focus();check(findEnabled("Enable system gesture access"),"A stale service instance was mistaken for enabled access");
            check(find("Turn on Use Nth gesture actions on the Android page. The accessibility shortcut alone does not enable the service. This service does not read screen contents or typing.")!=null,"Shortcut-only clarification missing");
            // Give the shortcut its own enabled preference without enabling the service.
            shell("settings put secure accessibility_button_targets com.nth.desktop3d/com.nth.desktop3d.GestureAccess");focus();check(!SetupAccess.gestures(activity)&&findEnabled("Enable system gesture access"),"Accessibility shortcut was treated as service access");
            IntentFilter details=new IntentFilter("android.settings.ACCESSIBILITY_DETAILS_SETTINGS");Instrumentation.ActivityMonitor detail=test.addMonitor(details,new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true);click("Enable system gesture access");test.removeMonitor(detail);check(detail.getHits()==1&&guide().step()==2,"Service-specific settings handoff or cancellation failed");
            click("Next");check(guide().step()==3,"Gesture access could not be skipped");shell("pm grant com.nth.desktop3d android.permission.READ_CALENDAR");focus();check(!findEnabled("Enable calendar card"),"Calendar grant still offers Enable");click("Next");shell("pm grant com.nth.desktop3d android.permission.ACCESS_COARSE_LOCATION");focus();check(!findEnabled("Enable weather location"),"Location grant still offers Enable");capture("083-weather-granted-status");
            click("Next");check(guide().step()==5&&find("Finish")!=null,"Setup summary missing");capture("083-finish-summary");click("Finish");check(!store.preferences.getBoolean("setup_in_progress",true)&&guide()==null,"Finish did not close setup");focus();check(guide()==null,"Completed setup reopened on window focus");
        }finally{
            shell("cmd package set-home-activity com.nth.desktop3d/.MainActivity");shell("cmd notification disallow_listener com.nth.desktop3d/.MediaAccess");shell("settings delete secure accessibility_button_targets");
            main(()->{try{java.lang.reflect.Field f=GestureAccess.class.getDeclaredField("active");f.setAccessible(true);f.set(null,new java.lang.ref.WeakReference<GestureAccess>(null));}catch(Exception e){throw new AssertionError(e);}store.preferences.edit().putBoolean("setup_in_progress",false).putBoolean("welcomed",true).remove("setup_step").apply();call("closePanel",new Class[]{});store.reducedMotion=reduced;new CrystalPreferences(activity).setMenuAnimation(menu);});
        }
        return activity;
    }
    private void focus(){main(()->activity.onWindowFocusChanged(true));idle(250);}
    private SetupGuide guide(){return (SetupGuide)field("setupGuide");}
    private boolean nextVisible(){AtomicReference<Boolean> visible=new AtomicReference<>(false);main(()->{View next=find("Next"),panel=(View)field("panel"),overlay=(View)field("overlay");Rect r=new Rect();visible.set(next!=null&&next.isShown()&&next.getGlobalVisibleRect(r)&&r.height()>0&&panel!=null&&panel.getAlpha()>.99f&&overlay!=null&&overlay.getVisibility()==View.VISIBLE);});return visible.get();}
    private boolean findEnabled(String text){AtomicReference<Boolean> result=new AtomicReference<>(false);main(()->{View v=find(text);result.set(v!=null&&v.isEnabled());});return result.get();}
    private void tap(String text){AtomicReference<int[]> point=new AtomicReference<>();main(()->{View v=find(text);check(v!=null,"Missing tap "+text);int[] p=new int[2];v.getLocationOnScreen(p);point.set(new int[]{p[0]+v.getWidth()/2,p[1]+v.getHeight()/2});});long now=SystemClock.uptimeMillis();for(int action:new int[]{0,1}){MotionEvent event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,point.get()[0],point.get()[1],0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);test.getUiAutomation().injectInputEvent(event,true);event.recycle();}idle(250);}
    private void systemClick(String text){AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();if(root==null)return;for(AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text))if(text.equalsIgnoreCase(String.valueOf(n.getText()))){AccessibilityNodeInfo target=n;for(int i=0;i<4&&target!=null;i++,target=target.getParent())if(target.isClickable()&&target.performAction(AccessibilityNodeInfo.ACTION_CLICK)){idle(100);return;}}}
    private void click(String text){main(()->{View v=find(text);check(v!=null,"Missing button "+text);v.performClick();});idle(200);}
    private View find(String text){return find(activity.getWindow().getDecorView(),text);}
    private View find(View v,String text){if(text.contentEquals(v.getContentDescription()==null?"":v.getContentDescription())||v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),text);if(f!=null)return f;}return null;}
    private Object field(String name){try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private Object call(String name,Class<?>[] types,Object...args){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name,Class<?>[] types,Object...args){main(()->call(name,types,args));idle(250);}
    private void main(Runnable run){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{run.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private void await(java.util.function.BooleanSupplier ready,String message){long until=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<until){if(ready.getAsBoolean())return;idle(100);}throw new AssertionError(message);}
    private void shell(String command)throws Exception{try(ParcelFileDescriptor p=test.getUiAutomation().executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(p)){byte[] bytes=new byte[1024];while(in.read(bytes)!=-1){}}}
    private void capture(String name){idle(200);Bitmap image=test.getUiAutomation().takeScreenshot();File dir=output==null?new File(activity.getExternalFilesDir(null),"verification"):new File(output);dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new AssertionError(e);}finally{image.recycle();}}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
