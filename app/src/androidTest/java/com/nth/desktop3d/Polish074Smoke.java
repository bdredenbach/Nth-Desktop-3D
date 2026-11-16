package com.nth.desktop3d;

import android.app.Instrumentation;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import java.util.concurrent.atomic.AtomicReference;

/** Phone regressions: pinned setup controls, contextual settings and native downward swipes. */
public final class Polish074Smoke {
    private final Instrumentation test;private final MainActivity activity;private final String output;
    public Polish074Smoke(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;}
    public void run()throws Exception{
        setupNavigation();contextMenus();managerGeometry();downwardAssignments();
        main(()->activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        await(()->activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE,"Landscape did not settle");idle(500);
        managerGeometry();setupNavigation();capture("setup-landscape-footer");
        main(()->activity.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        await(()->activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT,"Portrait did not settle");idle(400);invoke("closePanel",new Class[]{});
    }
    private void setupNavigation(){
        invoke("showSetup",new Class[]{int.class},0);
        await(()->{View panel=(View)field(activity,"panel");return panel!=null&&Math.abs(panel.getRotationX())<.01f&&Math.abs(panel.getRotationY())<.01f&&panel.getAlpha()>.99f;},"Setup opening animation did not settle");
        for(int step=0;step<6;step++){
            SetupGuide guide=(SetupGuide)field(activity,"setupGuide");check(guide.step()==step,"Setup skipped a step");
            View back=find(v->v instanceof Button&&"Back".contentEquals(((Button)v).getText()));
            final String nextText=step==5?"Finish":"Next";View next=find(v->v instanceof Button&&nextText.contentEquals(((Button)v).getText()));
            check(back!=null&&next!=null,"Setup navigation missing");await(()->!guide.isLayoutRequested()&&back.getWidth()>0&&next.getWidth()>0,"Setup navigation did not finish layout");check(back.isEnabled()==(step>0),"Back enabled on first step or disabled later");
            int[] b=position(back),n=position(next),g=position(guide);
            check(b[0]+back.getWidth()<=n[0]&&Math.abs(b[1]-n[1])<=2,"Back and Next are not aligned at opposite sides");
            check(b[1]+back.getHeight()>=g[1]+guide.getHeight()-Ui.dp(activity,3),"Navigation is not pinned to the bottom");
            check(find(v->v instanceof Button&&((Button)v).getText().toString().toLowerCase().contains("explore desktop"))==null,"Explore desktop remained in setup");
            android.widget.ScrollView sc=(android.widget.ScrollView)find(v->v instanceof android.widget.ScrollView);main(()->sc.fullScroll(View.FOCUS_DOWN));idle(100);check(position(back)[1]==b[1],"Scrolling moved setup navigation");
            if(step==1){click("Back");check(((SetupGuide)field(activity,"setupGuide")).step()==0,"Back did not return to the previous step");click("Next");}
            if(step==1)capture("setup-media-pinned-footer");
            click(nextText);
        }
        check(field(activity,"setupGuide")==null&&field(activity,"overlay")==null,"Finish did not return to the desktop");
    }
    private void contextMenus(){
        LauncherStore.Item folder=new LauncherStore.Item("folder","Context fixture");
        invoke("showFolder",new Class[]{LauncherStore.Item.class},folder);click("Folder settings menu");
        android.app.AlertDialog dialog=(android.app.AlertDialog)field(activity,"areaMenu");check(dialog!=null&&dialog.isShowing(),"Folder menu did not open");
        capture("folder-context-settings");View content=dialog.getWindow().getDecorView();SettingsModel model=(SettingsModel)field(activity,"settingsModel");
        for(SettingsModel.Spec spec:model.specs.values())if(spec.page.equals("folders"))check(find(content,v->android.text.TextUtils.equals(spec.title,v.getContentDescription()))!=null,"Folder menu omitted "+spec.title);
        check(find(content,v->android.text.TextUtils.equals("Desktop grid columns",v.getContentDescription()))==null,"Folder menu included unrelated desktop controls");
        check(find(content,v->android.text.TextUtils.equals("Folder icon shape",v.getContentDescription()))==null,"Folder shape remained under Folder settings");
        invoke("showSettings",new Class[]{String.class},"look");click("Look & feel settings menu");android.app.AlertDialog look=(android.app.AlertDialog)field(activity,"areaMenu");View lookControls=look.getWindow().getDecorView();
        main(()->find(lookControls,v->android.text.TextUtils.equals("Folder icon shape",v.getContentDescription())).performClick());idle(250);
        check("choice:folder_shape".equals(((SettingsStudio)field(activity,"settingsStudio")).page()),"Context option opened the wrong settings page");
        click("Rounded square");check(store().preferences.getInt("folder_shape",-1)==1,"Folder contextual choice was not applied");
        invoke("showFolder",new Class[]{LauncherStore.Item.class},folder);click("Folder settings menu");dialog=(android.app.AlertDialog)field(activity,"areaMenu");View folderControls=dialog.getWindow().getDecorView();main(()->find(folderControls,v->android.text.TextUtils.equals("Folder columns",v.getContentDescription())).performClick());idle(300);
        check(find(v->v instanceof android.widget.SeekBar&&android.text.TextUtils.equals("Folder columns",v.getContentDescription()))!=null,"Folder slider was not focused");View focused=find(v->v instanceof android.widget.SeekBar&&android.text.TextUtils.equals("Folder columns",v.getContentDescription()));android.graphics.Rect visible=new android.graphics.Rect();check(focused.getGlobalVisibleRect(visible)&&visible.height()>0,"Context slider remained outside the visible settings area");
        invoke("showSettings",new Class[]{String.class},"look");click("Look & feel settings menu");dialog=(android.app.AlertDialog)field(activity,"areaMenu");View appearance=dialog.getWindow().getDecorView();main(()->find(appearance,v->android.text.TextUtils.equals("Menu opening animation",v.getContentDescription())).performClick());idle(250);
        check("choice:menu_animation".equals(((SettingsStudio)field(activity,"settingsStudio")).page()),"Advanced menu setting did not open directly");
        invoke("showStart",new Class[]{});click("App drawer settings menu");dialog=(android.app.AlertDialog)field(activity,"areaMenu");View drawer=dialog.getWindow().getDecorView();check(find(drawer,v->android.text.TextUtils.equals("Search folders",v.getContentDescription()))!=null,"Drawer omitted its search settings");
        invoke("showModes",new Class[]{});check(!((android.app.AlertDialog)dialog).isShowing(),"Context menu survived closing its area");check(find(v->android.text.TextUtils.equals("Scenes & motion settings menu",v.getContentDescription()))!=null,"Scene menu missing");
        invoke("showSettings",new Class[]{String.class},"nota");check(find(v->android.text.TextUtils.equals("Command examples",v.getContentDescription()))!=null,"Nota examples menu missing");check(find(v->android.text.TextUtils.equals("Nth Nota AI settings menu",v.getContentDescription()))==null,"Nota has a duplicate settings menu");
        invoke("closePanel",new Class[]{});
    }
    private void managerGeometry(){
        invoke("showDesktopManager",new Class[]{});idle(350);DesktopManagerView manager=(DesktopManagerView)field(activity,"desktopManager");main(()->manager.tilt(0,0));
        View preview=manager.findViewWithTag("desktop-preview:0"),add=manager.findViewWithTag("desktop-add-preview");check(preview!=null&&add!=null,"Desktop previews missing");
        check(preview.getWidth()>0&&preview.getHeight()>0,"Preview has no size");check(preview.getWidth()==add.getWidth()&&preview.getHeight()==add.getHeight()&&preview.getTop()==add.getTop(),"Add desktop preview does not match the existing screen");
        main(()->manager.tilt(.5f,.3f));check(preview.getRotationX()==add.getRotationX()&&preview.getRotationY()==add.getRotationY(),"Add desktop does not share preview perspective");capture("desktop-matching-add-preview");invoke("closePanel",new Class[]{});
    }
    private void downwardAssignments(){
        LauncherStore.Item folder=new LauncherStore.Item("folder","Downward gesture fixture");
        main(()->{store().mode=0;store().page=store().pageCount()-1;store().pages.get(store().page).clear();folder.cellX=1;folder.cellY=2;store().pages.get(store().page).add(folder);store().preferences.edit().putInt("grid_columns",4).putInt("grid_row_dp",88).putBoolean("folder_pattern_page_"+store().page,false).apply();store().save();desktop().rebuild();});idle(450);
        for(int action:new int[]{0,1,2,3,5,14,15,16,17}){
            invoke("closePanel",new Class[]{});final int chosen=action;main(()->{store().preferences.edit().putInt("gesture_down",chosen).putString("gesture_down_target",chosen==17?folder.id:"builtin:calculator").apply();});idle(380);
            View target=action==0?cell(folder.id):desktop();int[] p=position(target);float x=p[0]+(action==0?target.getWidth()/2f:Ui.dp(activity,12)),y=p[1]+(action==0?target.getHeight()/2f:Ui.dp(activity,100));
            swipe(x,y,0,Ui.dp(activity,120),false,0);
            await(()->downwardOpened(chosen),"Swipe down did not execute action "+action);
        }
        capture("swipe-down-opens-saved-folder");invoke("closePanel",new Class[]{});
        // Scrolling an overflowing desktop must take precedence when it can move toward the top.
        main(()->{store().preferences.edit().putInt("grid_row_dp",144).apply();store().pages.get(store().page).clear();for(int i=0;i<60;i++)store().pages.get(store().page).add(new LauncherStore.Item("builtin:notes","Scroll "+i));store().save();desktop().rebuild();});idle(400);
        android.widget.ScrollView sc=(android.widget.ScrollView)field(desktop(),"scroller");await(()->!sc.isLayoutRequested()&&sc.canScrollVertically(1),"Overflow fixture did not finish layout");main(()->sc.scrollTo(0,Ui.dp(activity,170)));await(()->sc.canScrollVertically(-1),"Overflow fixture did not scroll");int before=sc.getScrollY();int[] p=position(desktop());swipe(p[0]+Ui.dp(activity,8),p[1]+Ui.dp(activity,140),0,Ui.dp(activity,100),false,0);check(field(activity,"overlay")==null&&sc.getScrollY()<before,"Swipe down intercepted real desktop scrolling");
        main(()->{store().preferences.edit().putInt("gesture_down",1).remove("gesture_down_target").apply();sc.scrollTo(0,0);});idle(150);
        long start=SystemClock.uptimeMillis();float x=p[0]+Ui.dp(activity,8),y=p[1]+Ui.dp(activity,140);pointer(start,0,x,y);for(int i=1;i<=24;i++){pointer(start,2,x,y+Ui.dp(activity,120)*i/24f);idle(16);}pointer(start,1,x,y+Ui.dp(activity,120));await(()->downwardOpened(1),"A gradual downward swipe at the top of a populated desktop was lost to scrolling");invoke("closePanel",new Class[]{});
    }
    private boolean downwardOpened(int action){switch(action){case 0:return field(activity,"appsAdapter")!=null;case 1:return field(activity,"settingsStudio")!=null&&"root".equals(((SettingsStudio)field(activity,"settingsStudio")).page());case 2:return field(activity,"settingsStudio")!=null&&"nota".equals(((SettingsStudio)field(activity,"settingsStudio")).page());case 3:return find(v->v instanceof Button&&"Internet".contentEquals(((Button)v).getText()))!=null;case 5:return GestureAccess.available()||field(activity,"setupGuide")!=null&&((SetupGuide)field(activity,"setupGuide")).step()==2;case 14:return field(activity,"desktopManager")!=null;case 15:return find(v->v instanceof SceneCard)!=null;case 16:return find(v->v instanceof Button&&"=".contentEquals(((Button)v).getText()))!=null;case 17:return find(v->"folder".equals(v.getTag()))!=null;default:return false;}}
    private LauncherStore store(){return (LauncherStore)field(activity,"store");}private DesktopView desktop(){return (DesktopView)field(activity,"desktop");}
    @SuppressWarnings("unchecked") private View cell(String id){return ((java.util.Map<String,View>)field(desktop(),"cellViews")).get(id);}
    private void clickPrefix(String text){main(()->{View v=find(x->x instanceof Button&&((Button)x).getText().toString().startsWith(text));check(v!=null,"Missing control: "+text);v.performClick();});idle(100);}
    private void click(String text){main(()->{View v=find(x->android.text.TextUtils.equals(text,x.getContentDescription())||android.text.TextUtils.equals("✓  "+text,x.getContentDescription())||(x instanceof Button&&(((Button)x).getText().toString().equals(text)||((Button)x).getText().toString().equals("✓  "+text))));check(v!=null,"Missing control: "+text);v.performClick();});idle(100);}
    private void systemClick(String text){long until=SystemClock.uptimeMillis()+5000;while(SystemClock.uptimeMillis()<until){android.view.accessibility.AccessibilityNodeInfo root=test.getUiAutomation().getRootInActiveWindow();if(root!=null)for(android.view.accessibility.AccessibilityNodeInfo n:root.findAccessibilityNodeInfosByText(text))if(android.text.TextUtils.equals(text,n.getText())){android.graphics.Rect r=new android.graphics.Rect();n.getBoundsInScreen(r);pointer(SystemClock.uptimeMillis(),0,r.centerX(),r.centerY());pointer(SystemClock.uptimeMillis(),1,r.centerX(),r.centerY());idle(200);return;}idle(100);}throw new AssertionError("Example row missing");}
    private int[] position(View view){check(view!=null,"Gesture target missing");AtomicReference<int[]> p=new AtomicReference<>();main(()->{int[] v=new int[2];view.getLocationOnScreen(v);p.set(v);});return p.get();}
    private void swipe(float x,float y,float dx,float dy,boolean hold,long edgeWait){long start=SystemClock.uptimeMillis();pointer(start,0,x,y);if(hold)idle(android.view.ViewConfiguration.getLongPressTimeout()+150);for(int i=1;i<=10;i++){pointer(start,2,x+dx*i/10,y+dy*i/10);idle(40);}if(edgeWait>0){idle(edgeWait);pointer(start,2,x+dx-70,y+dy);idle(80);}pointer(start,1,x+dx-(edgeWait>0?70:0),y+dy);idle(350);}
    private void pointer(long start,int action,float x,float y){MotionEvent event=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);test.getUiAutomation().injectInputEvent(event,true);event.recycle();}
    private void twoFinger(float x,float y,float x2,float y2,float dy){long start=SystemClock.uptimeMillis();pointer(start,0,x,y);multi(start,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),x,y,x2,y2);for(int i=1;i<=8;i++){multi(start,2,x,y+dy*i/8,x2,y2+dy*i/8);idle(40);}multi(start,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),x,y+dy,x2,y2+dy);pointer(start,1,x,y+dy);idle(400);}
    private void multi(long start,int action,float x,float y,float x2,float y2){MotionEvent.PointerProperties[] pp={new MotionEvent.PointerProperties(),new MotionEvent.PointerProperties()};MotionEvent.PointerCoords[] cc={new MotionEvent.PointerCoords(),new MotionEvent.PointerCoords()};for(int i=0;i<2;i++){pp[i].id=i;pp[i].toolType=MotionEvent.TOOL_TYPE_FINGER;cc[i].x=i==0?x:x2;cc[i].y=i==0?y:y2;cc[i].pressure=cc[i].size=1;}MotionEvent event=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,2,pp,cc,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);test.getUiAutomation().injectInputEvent(event,true);event.recycle();}
    private interface Match{boolean matches(View v);}private View find(Match m){return find(activity.getWindow().getDecorView(),m);}private View find(View v,Match m){if(v.getVisibility()!=View.VISIBLE)return null;if(m.matches(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),m);if(found!=null)return found;}return null;}
    private Object field(Object target,String name){try{java.lang.reflect.Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name,Class<?>[] types,Object... args){main(()->{try{java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(activity,args);}catch(Exception e){throw new AssertionError(e);}});idle(200);}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}private void await(java.util.function.BooleanSupplier ready,String message){long until=SystemClock.uptimeMillis()+7000;while(SystemClock.uptimeMillis()<until){AtomicReference<Boolean> r=new AtomicReference<>(false);main(()->r.set(ready.getAsBoolean()));if(r.get())return;idle(100);}throw new AssertionError(message);}
    private void capture(String name){android.graphics.Bitmap image=test.getUiAutomation().takeScreenshot();java.io.File dir=output==null?new java.io.File(test.getTargetContext().getExternalFilesDir(null),"verification"):new java.io.File(output);dir.mkdirs();try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(dir,name+".png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new AssertionError(e);}finally{image.recycle();}}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
