package com.nth.desktop3d;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

/** Native editor, copied alpha assets, depth, backup/reinstall and wallpaper picker handoff. */
public final class SceneCreator078Smoke {
    private final Instrumentation test;private final MainActivity activity;private final String output;
    public SceneCreator078Smoke(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;}
    public void run()throws Exception {
        SettingsModel model=(SettingsModel)invokeValue("settingsModel");JSONObject before=DesktopBackup.capture(activity,model);LayeredScene.Document previousDraft=LayeredScene.read(activity,true);
        File picture=new File(activity.getExternalFilesDir(null),"scene-background.png"),cutout=new File(activity.getExternalFilesDir(null),"scene-cutout.png");writePicture(picture,false);writePicture(cutout,true);
        try{
            LayeredScene.write(activity,null,true);invoke("showSettings",new Class[]{String.class},"wallpaper");click("Create or edit a scene");await(()->field("sceneComposer")!=null,"Creator did not open from Wallpaper settings");
            invoke("sceneImageResult",new Class[]{int.class,int.class,Intent.class},3080,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(picture)));await(()->editor().document.background!=null,"Gallery background was not copied");capture("creator-1-background");click("Next");click("Crystal");click("Orb");
            invoke("sceneImageResult",new Class[]{int.class,int.class,Intent.class},3081,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(cutout)));await(()->editor().document.objects.size()==3,"Cutout layer was not added");
            main(()->{LayeredScene.Document d=editor().document;LayeredScene.ObjectLayer o=d.objects.get(2);check(o.image!=null,"Cutout bytes missing");Bitmap b=BitmapFactory.decodeByteArray(o.image,0,o.image.length);try{check(Color.alpha(b.getPixel(0,0))==0&&Color.alpha(b.getPixel(b.getWidth()/2,b.getHeight()/2))>240,"Foreground transparency was flattened");}finally{b.recycle();}editor().preview.layout(0,0,150,290);long now=SystemClock.uptimeMillis();float x=o.x*150,y=o.y*290;for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,action==MotionEvent.ACTION_DOWN?x:110,action==MotionEvent.ACTION_DOWN?y:110,0);editor().preview.onTouchEvent(e);e.recycle();}check(o.x>.65f&&o.y<.45f,"Object drag did not update placement");editor().persistNow();});capture("creator-2-layered-objects");
            invoke("closePanel",new Class[]{});invoke("showSceneCreator",new Class[]{});await(()->field("sceneComposer")!=null&&editor().document.objects.size()==3,"Closing creator lost its draft");check(model.value("wallpaper_source")!=3,"Closing a draft applied the wallpaper");main(()->editor().step(2));
            main(()->{LayerSceneView view=editor().preview;view.moving(true);check(view.offset(1,.9f,100)>view.offset(1,.1f,100)*5,"Layers did not have independent depth");view.moving(false);check(view.offset(1,.9f,100)==0,"Reduced motion retained scene movement");view.moving(true);view.tilt(.8f,-.4f);});capture("creator-3-depth-preview");click("Apply scene");await(()->model.value("wallpaper_source")==3&&field("sceneComposer")==null,"Apply did not persist the scene");
            main(()->check(((View)field("layeredWallpaper")).getVisibility()==View.VISIBLE&&((View)field("scene")).getVisibility()==View.GONE,"Live custom scene did not replace GL wallpaper"));capture("custom-scene-live");
            invoke("showModes",new Class[]{});await(()->find("My layered scene")!=null,"Custom scene missing from Scene studio");capture("custom-scene-choice");click("My layered scene");await(()->model.value("wallpaper_source")==3&&field("overlay")==null,"Saved scene selection did not return home");
            LayeredScene.Document active=LayeredScene.read(activity,false);check(active.objects.size()==3,"Applied scene lost layers");JSONObject archive=DesktopBackup.capture(activity,model);check(archive.has("customScene"),"Desktop backup omitted the custom scene");String expected=active.json().toString();LayeredScene.write(activity,null,false);main(()->{try{model.importDesktopBackup(activity,new DesktopBackup.Prepared(archive,model));}catch(Exception e){throw new AssertionError(e);}});check(expected.equals(LayeredScene.read(activity,false).json().toString()),"Scene image/depth/placement did not survive restore");
            JSONObject invalid=new JSONObject(archive.toString());invalid.getJSONObject("customScene").getJSONArray("objects").getJSONObject(2).getJSONObject("image").put("sha256","broken");String unchanged=model.store.preferences.getString("layout","");boolean rejected=false;try{new DesktopBackup.Prepared(invalid,model);}catch(Exception expectedError){rejected=true;}check(rejected&&unchanged.equals(model.store.preferences.getString("layout","")),"Corrupt scene backup changed the desktop");
            invoke("chooseAndroidWallpaper",new Class[]{boolean.class},true);await(()->find("Parallax wallpaper fixture")!=null,"Installed live wallpaper was hidden from launcher picker");capture("live-wallpaper-picker");Instrumentation.ActivityMonitor monitor=test.addMonitor(new IntentFilter(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER),new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true);try{click("Parallax wallpaper fixture");check(monitor.getHits()==1&&model.value("wallpaper_source")==2,"Live wallpaper did not use Android's component preview");}finally{test.removeMonitor(monitor);}
        }finally{
            main(()->{try{invokeDirect("closePanel",new Class[]{});model.importDesktopBackup(activity,new DesktopBackup.Prepared(before,model));}catch(Exception e){throw new AssertionError(e);}});LayeredScene.write(activity,previousDraft,true);picture.delete();cutout.delete();
        }
    }
    private SceneComposer editor(){return (SceneComposer)field("sceneComposer");}
    private void writePicture(File file,boolean alpha)throws Exception {Bitmap b=Bitmap.createBitmap(96,160,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(alpha?Color.TRANSPARENT:0xFF123B58);Paint p=new Paint(3);p.setColor(alpha?0xFF8AF4DE:0xFF3A7493);c.drawCircle(48,80,30,p);try(FileOutputStream out=new FileOutputStream(file)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    private Object field(String name){try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private Object invokeValue(String name){AtomicReference<Object> r=new AtomicReference<>();main(()->r.set(invokeDirect(name,new Class[]{})));return r.get();}
    private Object invokeDirect(String name,Class<?>[] types,Object...args){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name,Class<?>[] types,Object...args){main(()->invokeDirect(name,types,args));idle(250);}
    private View find(String text){return find(activity.getWindow().getDecorView(),text);}
    private View find(View v,String text){if(v.getVisibility()!=View.VISIBLE)return null;if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View f=find(g.getChildAt(i),text);if(f!=null)return f;}}return null;}
    private void click(String value){main(()->{View v=find(value);check(v!=null,"Missing control: "+value);v.performClick();});idle(250);}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private void await(java.util.function.BooleanSupplier ready,String message){long until=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<until){AtomicReference<Boolean> r=new AtomicReference<>(false);main(()->r.set(ready.getAsBoolean()));if(r.get())return;idle(100);}throw new AssertionError(message);}
    private void capture(String name){idle(600);Bitmap b=test.getUiAutomation().takeScreenshot();File dir=output==null?new File(activity.getExternalFilesDir(null),"verification"):new File(output);dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new AssertionError(e);}finally{b.recycle();}}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
