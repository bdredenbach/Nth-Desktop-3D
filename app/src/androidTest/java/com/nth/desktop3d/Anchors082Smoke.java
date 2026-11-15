package com.nth.desktop3d;

import android.app.*;import android.content.*;import android.content.pm.ActivityInfo;import android.content.res.Configuration;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicReference;

/** Pixel anchors, actual bottom-folder taps, cold setup recovery and independent light animation. */
public final class Anchors082Smoke {
    private final Instrumentation test;private MainActivity activity;private final String output;
    private SettingsModel model;private LauncherStore store;private LauncherStore.Item last;
    public Anchors082Smoke(Instrumentation t,MainActivity a,String o){test=t;activity=a;output=o;}
    public MainActivity run()throws Exception {
        refreshModel();JSONObject before=DesktopBackup.capture(activity,model);LayeredScene.Document draft=LayeredScene.read(activity,true);
        try{
            icons();lights();setup();
        }finally{
            main(()->{try{call("closePanel",new Class[]{});store.preferences.edit().putBoolean("setup_in_progress",false).putBoolean("welcomed",true).remove("setup_step").apply();model.importDesktopBackup(activity,new DesktopBackup.Prepared(before,model));}catch(Exception e){throw new AssertionError(e);}});LayeredScene.write(activity,draft,true);
        }
        return activity;
    }
    private void icons(){
        main(()->{store.page=0;store.mode=3;store.pages.get(0).clear();for(int i=0;i<FolderRules.NAMES.length;i++){LauncherStore.Item f=new LauncherStore.Item("folder",FolderRules.NAMES[i]);f.cellX=FolderRules.X[i];f.cellY=FolderRules.Y[i];f.children.add(new LauncherStore.Item("builtin:notes","Notes"));store.pages.get(0).add(f);last=f;}store.preferences.edit().putBoolean("folder_pattern_page_0",true).putBoolean("dock_content",true).putBoolean("show_dock",true).putInt("dock_space_height",96).putInt("dock_space_count",5).putInt("desktop_padding",0).apply();model.set("reduced_motion",1);store.save();call("applySettings",new Class[]{boolean.class},true);});idle(500);
        for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}){
            rotate(orientation);
            for(boolean pattern:new boolean[]{true,false}){
                main(()->{store.preferences.edit().putBoolean("folder_pattern_page_0",pattern).apply();call("applySettings",new Class[]{boolean.class},true);});idle(300);
                setIcon(70);Map<String,PointF> small=centers();int width=iconWidth();setIcon(130);Map<String,PointF> large=centers();
                check(iconWidth()>width+Ui.dp(activity,10),"Icon size changed spacing without growing icons");
                for(String id:small.keySet()){PointF a=small.get(id),b=large.get(id);check(Math.abs(a.x-b.x)<=2&&Math.abs(a.y-b.y)<=2,"Icon center shifted on resize in "+orientation+" pattern="+pattern+": "+a+" -> "+b);}
                main(()->{ScrollView sc=scroller();sc.scrollTo(0,0);});idle(150);
                // Scroll by native gestures, starting over a folder where possible.
                for(int i=0;i<12&&!visible(last);i++)scrollUp();
                check(visible(last),"Bottom folder cannot be reached in "+orientation+" pattern="+pattern);tap(last);check(field("overlay")!=null,"Bottom folder no longer opens on tap");invoke("closePanel",new Class[]{});
                capture("082-icon-anchors-"+orientation+"-"+pattern);
            }
        }
        rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    private void setIcon(int size){main(()->model.set("icon_size",size));idle(350);}
    @SuppressWarnings("unchecked") private Map<String,View> tiles(){return (Map<String,View>)member(desktop(),"cellViews");}
    private Map<String,PointF> centers(){AtomicReference<Map<String,PointF>> out=new AtomicReference<>();main(()->{Map<String,PointF> p=new LinkedHashMap<>();for(Map.Entry<String,View> e:tiles().entrySet()){View icon=((IconTile)e.getValue()).getChildAt(0);int[] at=new int[2];icon.getLocationOnScreen(at);p.put(e.getKey(),new PointF(at[0]+icon.getWidth()/2f,at[1]+icon.getHeight()/2f));}out.set(p);});return out.get();}
    private int iconWidth(){AtomicReference<Integer> w=new AtomicReference<>(0);main(()->w.set(((IconTile)tiles().values().iterator().next()).getChildAt(0).getWidth()));return w.get();}
    private ScrollView scroller(){return (ScrollView)member(desktop(),"scroller");}
    private boolean visible(LauncherStore.Item item){AtomicReference<Boolean> r=new AtomicReference<>(false);main(()->{View v=tiles().get(item.id);Rect rect=new Rect(),bounds=new Rect();if(v!=null&&v.getGlobalVisibleRect(rect)&&desktop().getGlobalVisibleRect(bounds)){int[] p=new int[2];v.getLocationOnScreen(p);r.set(rect.contains(p[0]+v.getWidth()/2,p[1]+v.getHeight()/2)&&bounds.contains(p[0]+v.getWidth()/2,p[1]+v.getHeight()/2));}});return r.get();}
    private void scrollUp(){AtomicReference<int[]> pos=new AtomicReference<>();main(()->{int[] p=new int[2];desktop().getLocationOnScreen(p);pos.set(new int[]{p[0]+desktop().getWidth()/2,p[1]+desktop().getHeight()*4/5,p[1]+desktop().getHeight()/5});});int[] p=pos.get();long now=SystemClock.uptimeMillis();touch(now,now,0,p[0],p[1]);for(int i=1;i<=8;i++)touch(now,now+i*25,2,p[0],p[1]+(p[2]-p[1])*i/8f);touch(now,now+225,1,p[0],p[2]);idle(350);}
    private void tap(LauncherStore.Item item){AtomicReference<int[]> p=new AtomicReference<>();main(()->{View v=tiles().get(item.id);int[] at=new int[2];v.getLocationOnScreen(at);p.set(new int[]{at[0]+v.getWidth()/2,at[1]+v.getHeight()/2});});long t=SystemClock.uptimeMillis();touch(t,t,0,p.get()[0],p.get()[1]);touch(t,t+40,1,p.get()[0],p.get()[1]);idle(300);}
    private void touch(long down,long at,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,at,action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);test.getUiAutomation().injectInputEvent(e,true);e.recycle();}
    private void lights()throws Exception {
        LayeredScene.Document d=new LayeredScene.Document();d.shafts=1;d.shaftSpeed=.8f;d.shaftSide=1;
        LayeredScene.Document copy=LayeredScene.parse(LayeredScene.copy(d).json());check(copy.shafts==1&&copy.shaftSide==1&&copy.shaftSpeed==.8f,"Scene JSON lost corner lighting");JSONObject legacy=d.json();legacy.remove("shafts");legacy.remove("shaftSpeed");legacy.remove("shaftSide");check(LayeredScene.parse(legacy).shafts==0,"Old scenes unexpectedly gained lighting");boolean rejected=false;try{LayeredScene.parse(d.json().put("shaftSide",3));}catch(JSONException expected){rejected=true;}check(rejected,"Invalid corner accepted");
        LayeredScene.write(activity,null,true);LayeredScene.write(activity,d,false);invoke("showSceneCreator",new Class[]{});await(()->field("sceneComposer")!=null,"Creator did not open");
        main(()->{SceneComposer editor=(SceneComposer)field("sceneComposer");editor.focus("shafts");View root=activity.getWindow().getDecorView();check(find(root,"Dancing light shafts") instanceof SeekBar&&find(root,"Light shaft speed") instanceof SeekBar,"Light controls missing");check(find(root,"Light source: Top right")!=null,"Corner control missing");find(root,"Light source: Top right").performClick();check(editor.document.shaftSide==2,"Corner source did not cycle");editor.persistNow();});idle(500);
        check(LayeredScene.read(activity,true).shaftSide==2,"Draft lost selected corner");capture("082-creator-corner-lighting");click("Apply scene");await(()->field("sceneComposer")==null&&model.value("wallpaper_source")==3,"Scene did not apply");
        LayeredScene.Document saved=LayeredScene.read(activity,false);check(saved.shaftSide==2&&saved.shafts==1,"Applied scene lost shafts");JSONObject archive=DesktopBackup.capture(activity,model);check(archive.getJSONObject("customScene").getInt("shaftSide")==2,"Backup omitted corner lighting");main(()->{try{model.importDesktopBackup(activity,new DesktopBackup.Prepared(archive,model));}catch(Exception e){throw new AssertionError(e);}});check(LayeredScene.read(activity,false).shaftSide==2,"Restore lost lighting");
        AtomicReference<Bitmap> a=new AtomicReference<>(),b=new AtomicReference<>();
        main(()->{LayerSceneView v=(LayerSceneView)field("layeredWallpaper");v.moving(false);v.lighting(true);a.set(render(v));});idle(1200);main(()->b.set(render((View)field("layeredWallpaper"))));check(difference(a.get(),b.get())>100,"Shafts did not dance with caustics and tilt disabled");recycle(a,b);
        main(()->{LayerSceneView v=(LayerSceneView)field("layeredWallpaper");v.lighting(false);a.set(render(v));});idle(400);main(()->b.set(render((View)field("layeredWallpaper"))));check(difference(a.get(),b.get())==0,"Paused lighting still animates");recycle(a,b);
        main(()->{LayerSceneView v=(LayerSceneView)field("layeredWallpaper");v.document().shaftSide=0;a.set(render(v));v.document().shaftSide=1;b.set(render(v));});check(difference(a.get(),b.get())>100,"Left and right light sources render identically");recycle(a,b);capture("082-corner-shafts-live");
    }
    private void setup(){
        main(()->store.preferences.edit().putBoolean("welcomed",false).apply());invoke("showSetup",new Class[]{int.class},0);invoke("requestHome",new Class[]{});check(!store.preferences.getBoolean("welcomed",false),"Home selection completed setup prematurely");click("Next");check(guide().step()==1,"Setup did not reach media access");
        IntentFilter filter=new IntentFilter(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);filter.addDataScheme("package");Instrumentation.ActivityMonitor denied=test.addMonitor(filter,new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true);click("Open App info");test.removeMonitor(denied);check(denied.getHits()==1&&guide().step()==1,"App info cancellation lost media access step");
        main(()->{((View)field("overlay")).performClick();activity.onBackPressed();});check(guide()!=null&&guide().step()==0,"Setup dismissed instead of going Back");click("Next");
        recreate();check(guide()!=null&&guide().step()==1,"Recreation lost current setup step");rotate(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);check(guide()!=null&&guide().step()==1,"Rotation lost current setup step");
        // CLEAR_TASK starts a new Activity without its old saved-instance Bundle.
        Instrumentation.ActivityMonitor monitor=test.addMonitor(MainActivity.class.getName(),null,false);test.getTargetContext().startActivity(new Intent(test.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));activity=(MainActivity)monitor.waitForActivityWithTimeout(20000);test.removeMonitor(monitor);check(activity!=null,"Fresh launcher restart timed out");idle(800);refreshModel();check(guide()!=null&&guide().step()==1,"Fresh launch lost persisted setup progress");
        invoke("onNewIntent",new Class[]{Intent.class},new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));check(guide()!=null&&guide().step()==1,"Home intent dismissed setup");capture("082-setup-resumed-after-app-info");
        while(guide().step()<5)click("Next");click("Finish");check(guide()==null&&!store.preferences.getBoolean("setup_in_progress",true)&&store.preferences.getBoolean("welcomed",false),"Finish did not complete setup");recreate();check(guide()==null,"Completed setup reopened after recreation");rotate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    private SetupGuide guide(){return (SetupGuide)field("setupGuide");}
    private void recreate(){Instrumentation.ActivityMonitor m=test.addMonitor(MainActivity.class.getName(),null,false);main(activity::recreate);activity=(MainActivity)m.waitForActivityWithTimeout(20000);test.removeMonitor(m);check(activity!=null,"Activity recreation timed out");idle(700);refreshModel();}
    private void refreshModel(){AtomicReference<Object> result=new AtomicReference<>();main(()->result.set(call("settingsModel",new Class[]{})));model=(SettingsModel)result.get();store=model.store;}
    private void rotate(int orientation){main(()->activity.setRequestedOrientation(orientation));int expected=orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;await(()->activity.getResources().getConfiguration().orientation==expected,"Orientation did not change");idle(500);}
    private Bitmap render(View v){Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(b));return b;}
    private int difference(Bitmap a,Bitmap b){int n=0;for(int y=0;y<a.getHeight();y+=3)for(int x=0;x<a.getWidth();x+=3)if(a.getPixel(x,y)!=b.getPixel(x,y))n++;return n;}
    private void recycle(AtomicReference<Bitmap> a,AtomicReference<Bitmap> b){a.get().recycle();b.get().recycle();}
    private void click(String text){main(()->{View v=find(activity.getWindow().getDecorView(),text);check(v!=null,"Missing button "+text);v.performClick();});idle(200);}
    private View find(View v,String text){if(text.contentEquals(v.getContentDescription()==null?"":v.getContentDescription())||v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),text);if(f!=null)return f;}return null;}
    private DesktopView desktop(){return (DesktopView)field("desktop");}
    private Object member(Object o,String name){try{java.lang.reflect.Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new AssertionError(e);}}
    private Object field(String name){return member(activity,name);}
    private Object call(String name,Class<?>[] types,Object...args){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name,Class<?>[] types,Object...args){main(()->call(name,types,args));idle(250);}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private void await(java.util.function.BooleanSupplier ok,String message){long until=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<until){if(ok.getAsBoolean())return;idle(100);}throw new AssertionError(message);}
    private void capture(String name){idle(200);Bitmap b=test.getUiAutomation().takeScreenshot();File dir=output==null?new File(activity.getExternalFilesDir(null),"verification"):new File(output);dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new AssertionError(e);}finally{b.recycle();}}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
