package com.nth.desktop3d.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.TextView;
import com.nth.desktop3d.DesktopGrid;
import com.nth.desktop3d.LauncherStore;
import com.nth.desktop3d.MainActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Real cross-process RemoteViews, lifecycle and native hold/drag/resize regression. */
final class WidgetSmoke {
    private static final String FIXTURE="com.nth.desktop3d.widgetfixture";
    private final Instrumentation instrumentation;
    private MainActivity activity;
    private final String outputDirectory;
    WidgetSmoke(Instrumentation instrumentation,MainActivity activity,String outputDirectory){this.instrumentation=instrumentation;this.activity=activity;this.outputDirectory=outputDirectory;}
    void run() throws Exception {
        check(instrumentation.getTargetContext().getPackageManager().getApplicationInfo(FIXTURE,0).uid!=android.os.Process.myUid(),"Widget fixture must have a different UID");
        shell("appwidget grantbind --package com.nth.desktop3d --user 0");
        main(()->{LauncherStore store=store();store.mode=0;store.page=0;store.save();invokeDesktop("rebuild");});idle(700);
        holdEmptyBackground();await(()->findIn(activity.getWindow().getDecorView(),v->v instanceof Button&&((Button)v).getText().toString().endsWith("Widgets"))!=null,"Desktop hold did not expose the widget picker action");main(()->activity.onBackPressed());idle(300);
        int first=previewAndAddWidget();
        await(()->hasText("Hosted widget ready"),"Initial provider RemoteViews were not displayed");assertOccupancy();capture("widget-visible");
        sendUpdate(first,"Live widget update");await(()->hasText("Live widget update"),"Live RemoteViews update did not reach the registered host view");
        moveWidget(first,1,2);assertOccupancy();check(cell("widget:"+first).x==1&&cell("widget:"+first).y==2,"Widget did not snap to requested grid cell; actual "+cell("widget:"+first).x+","+cell("widget:"+first).y);
        capture("widget-drag-and-icon-reflow");
        int second=addWidget(false);await(()->widget(second)!=null,"Second widget missing");assertOccupancy();
        DesktopGrid.Cell before=cell("widget:"+second);moveWidget(second,1,2);
        check(cell("widget:"+second).x==before.x&&cell("widget:"+second).y==before.y,"Widget-on-widget drop did not roll back");assertOccupancy();
        View firstView=widget(first);hold(firstView);clickSystemText("Resize");idle(250);
        int[] location=position(firstView);int width=firstView.getWidth(),height=firstView.getHeight();
        drag(location[0]+width,location[1]+height/2f,metric("cw"),0,false);
        await(()->cell("widget:"+first).w==3,"Resize handle did not snap to a three-column width");assertOccupancy();capture("widget-resize-handles");
        main(()->invokeDesktop("clearSelection"));
        for(boolean folder:new boolean[]{true,false}){
            LauncherStore.Item selected=null;
            for(LauncherStore.Item item:store().pages.get(0))if(item.folder()==folder){View view=view(item.id);Rect visible=new Rect();if(view!=null&&view.getGlobalVisibleRect(visible)){selected=item;break;}}
            check(selected!=null,"No visible app/folder for collision gesture");DesktopGrid.Cell start=cell(selected.id);
            dragView(view(selected.id),(1-start.x)*metric("cw"),(2-start.y)*metric("ch"));assertOccupancy();
        }
        Button remote=(Button)findIn(widget(first),v->v instanceof Button&&"Tap widget".contentEquals(((Button)v).getText()));
        check(remote!=null,"Provider click target missing");tap(remote);await(()->hasText("Widget tapped"),"Provider PendingIntent did not receive the tap");
        shell("am start -W -a android.settings.SETTINGS");idle(300);
        instrumentation.getTargetContext().startActivity(new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle(1000);
        sendUpdate(first,"Updated after returning Home");await(()->hasText("Updated after returning Home"),"Widget updates stopped after pause/resume");
        int cancelled=addWidget(true);clickSystemText("Cancel widget");
        await(()->AppWidgetManager.getInstance(activity).getAppWidgetInfo(cancelled)==null,"Cancelled widget leaked its allocated ID");
        check(!known(cancelled),"Cancelled widget was committed to the desktop");
        int configured=addWidget(true);clickSystemText("Save widget");
        await(()->hasText("Configured widget ready"),"Configured widget was not attached after setup returned");assertOccupancy();capture("configured-widget-visible");
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(MainActivity.class.getName(),null,false);
        main(()->activity.recreate());activity=(MainActivity)monitor.waitForActivityWithTimeout(25000);instrumentation.removeMonitor(monitor);
        check(activity!=null,"Widget Activity recreation timed out");idle(900);
        sendUpdate(configured,"Restored widget content");await(()->hasText("Restored widget content"),"Restored widget was not registered for updates");assertOccupancy();
        main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));idle(1100);assertOccupancy();
        sendUpdate(configured,"Landscape widget update");await(()->hasText("Landscape widget update"),"Rotation lost live widget content");capture("widgets-landscape");
        main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));idle(900);assertOccupancy();
        main(()->{try{java.lang.reflect.Method reveal=desktop().getClass().getDeclaredMethod("revealWidget",int.class);reveal.setAccessible(true);reveal.invoke(desktop(),second);}catch(Exception error){throw new AssertionError(error);}});idle(400);
        hold(widget(second));clickSystemText("Remove");await(()->!known(second),"Widget Remove did not remove its saved record");
        check(AppWidgetManager.getInstance(activity).getAppWidgetInfo(second)==null,"Removed widget ID is still allocated");assertOccupancy();capture("widgets-restored-after-removal");
        main(()->{store().page=1;store().save();invokeDesktop("rebuild");});idle(400);
        String existing=store().pages.get(1).get(0).id;DesktopGrid.Cell original=cell(existing);
        int sparse=addWidget(false);await(()->widget(sparse)!=null,"Sparse-page widget missing");assertOccupancy();
        check(cell(existing).x==original.x&&cell(existing).y==original.y,"Adding a widget moved an icon despite vacant space");capture("widget-vacant-space-placement");
        main(()->{store().page=0;store().save();invokeDesktop("rebuild");});idle(500);
        sendUpdate(first,"Updated after changing desktops");await(()->hasText("Updated after changing desktops"),"Returning to a desktop lost widget update delivery");assertOccupancy();
        main(()->{store().mode=1;store().save();invokeDesktop("rebuild");});idle(1300);
        main(()->{
            int visible=0;
            for(DesktopGrid.Cell target:cells())if(!target.widget){
                View icon=view(target.id);if(icon==null||icon.getVisibility()!=View.VISIBLE)continue;visible++;
                for(DesktopGrid.Cell occupied:cells())if(occupied.widget){View widget=view(occupied.id);android.graphics.RectF a=new android.graphics.RectF(icon.getTranslationX(),icon.getTranslationY(),icon.getTranslationX()+icon.getWidth(),icon.getTranslationY()+icon.getHeight());android.graphics.RectF b=new android.graphics.RectF(widget.getTranslationX(),widget.getTranslationY(),widget.getTranslationX()+widget.getWidth(),widget.getTranslationY()+widget.getHeight());check(!android.graphics.RectF.intersects(a,b),"Gravity icon entered a widget");}
            }
            check(visible>0,"Gravity scene did not render any icons");
        });capture("widgets-gravity-obstacles");
        main(()->{store().mode=0;store().save();invokeDesktop("rebuild");});idle(400);assertOccupancy();
        testPinnedShortcut();
    }
    private void invokeActivity(String name){main(()->{try{java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(activity);}catch(Exception error){throw new AssertionError(error);}});idle(400);}
    private int previewAndAddWidget(){
        AppWidgetHost host=(AppWidgetHost)field(activity,"widgetHost");int before=host.getAppWidgetIds().length;
        invokeActivity("showWidgets");
        check(host.getAppWidgetIds().length==before,"Opening the preview gallery allocated a widget ID");
        clickSystemText("Expand Nth widget test fixture widgets");idle(300);capture("widget-preview-expanded");
        main(()->{android.widget.EditText search=(android.widget.EditText)findIn(activity.getWindow().getDecorView(),v->v instanceof android.widget.EditText);search.setText("does not exist 437");});idle(200);
        check(hasText("No matching widgets."),"Widget search did not filter providers");
        main(()->activity.onBackPressed());idle(200);check(host.getAppWidgetIds().length==before,"Cancelling preview browsing leaked a widget ID");
        invokeActivity("showWidgets");
        main(()->((android.widget.EditText)findIn(activity.getWindow().getDecorView(),v->v instanceof android.widget.EditText)).setText("Live widget"));idle(400);
        await(()->findIn((View)field(activity,"overlay"),v->v instanceof android.widget.ImageView&&((android.widget.ImageView)v).getDrawable() instanceof android.graphics.drawable.BitmapDrawable)!=null,"RemoteViews preview layout was not rendered before placement");capture("widget-preview-before-placement");
        clickSystemText("Add Live widget widget, 2 by 2");
        await(()->host.getAppWidgetIds().length==before+1,"Preview selection did not allocate exactly one widget");
        int[] ids=host.getAppWidgetIds();int result=-1;for(int id:ids)if(widget(id)!=null)result=id;check(result>=0,"Selected preview did not create a hosted widget");return result;
    }
    private void requestShortcutPin(){
        // Android launches CONFIRM_PIN_SHORTCUT with NEW_TASK | CLEAR_TASK, replacing Home.
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(MainActivity.class.getName(),null,false);
        clickSystemText("Pin recipe shortcut");MainActivity replacement=(MainActivity)monitor.waitForActivityWithTimeout(5000);instrumentation.removeMonitor(monitor);
        if(replacement!=null)activity=replacement;idle(400);
    }
    private void testPinnedShortcut()throws Exception{
        main(()->{store().page=2;store().save();invokeDesktop("rebuild");});idle(400);
        int websiteWidget=addWidget(false);await(()->widget(websiteWidget)!=null,"Website shortcut collision fixture missing");assertOccupancy();
        int count=store().pages.get(2).size();
        shell("am start -W -n "+FIXTURE+"/.FixtureShortcutActivity --es shortcutId cancelled-recipe");idle(350);requestShortcutPin();clickSystemText("Cancel");idle(350);
        check(store().pages.get(2).size()==count,"Cancelling a pin request added a shortcut");
        shell("am start -W -n "+FIXTURE+"/.FixtureShortcutActivity --es shortcutId pot-roast");idle(350);requestShortcutPin();
        await(()->field(field(activity,"shortcuts"),"confirmation")!=null,"Browser-style request did not reach the launcher");
        main(()->{android.app.AlertDialog dialog=(android.app.AlertDialog)field(field(activity,"shortcuts"),"confirmation");android.widget.EditText name=(android.widget.EditText)findIn(dialog.getWindow().getDecorView(),v->v instanceof android.widget.EditText);check(name!=null,"Shortcut name field missing");name.setText("Dinner recipe");});capture("shortcut-confirmation");clickSystemText("Add");
        await(()->store().pages.get(2).size()==count+1,"Accepted pin request did not add one desktop item");
        LauncherStore.Item recipe=store().pages.get(2).get(count);check("Dinner recipe".equals(recipe.label)&&"pot-roast".equals(recipe.shortcutId)&&FIXTURE.equals(recipe.shortcutPackage),"Shortcut identity or chosen name was not saved");assertOccupancy();capture("webpage-shortcut-on-desktop");
        LauncherStore restored=new LauncherStore(instrumentation.getTargetContext());LauncherStore.Item saved=restored.pages.get(2).get(count);check(recipe.key.equals(saved.key)&&recipe.shortcutUser==saved.shortcutUser&&"pot-roast".equals(saved.shortcutId),"Shortcut metadata was lost on reload");
        main(()->{try{Object manager=field(activity,"shortcuts");java.lang.reflect.Method icon=manager.getClass().getDeclaredMethod("icon",LauncherStore.Item.class);icon.setAccessible(true);check(icon.invoke(manager,recipe)!=null,"Shortcut icon was not persisted");}catch(Exception error){throw new AssertionError(error);}});
        Instrumentation.ActivityMonitor monitor=instrumentation.addMonitor(MainActivity.class.getName(),null,false);main(()->activity.recreate());activity=(MainActivity)monitor.waitForActivityWithTimeout(25000);instrumentation.removeMonitor(monitor);check(activity!=null,"Shortcut recreation timed out");idle(700);assertOccupancy();
        tap(view(recipe.id));clickSystemText("Opened recipe: https://example.test/recipe/pot-roast");
        instrumentation.getTargetContext().startActivity(new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle(500);
        // Repeating the same request updates the saved shortcut instead of creating a duplicate.
        shell("am start -W -n "+FIXTURE+"/.FixtureShortcutActivity --es shortcutId pot-roast");idle(300);requestShortcutPin();idle(600);if(field(field(activity,"shortcuts"),"confirmation")!=null)clickSystemText("Add");idle(400);check(store().pages.get(2).size()==count+1,"Repeated pin request duplicated the desktop shortcut");
        instrumentation.getTargetContext().startActivity(new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle(500);
        final LauncherStore.Item selected=store().pages.get(2).get(count);
        main(()->activity.menu(selected));clickSystemText("Add to folder…");clickSystemText("Nth tools");idle(300);
        LauncherStore.Item copy=null;for(LauncherStore.Item child:store().folders().get(0).children)if("pot-roast".equals(child.shortcutId))copy=child;check(copy!=null,"Copying a website shortcut into a folder lost its launch identity");
        main(()->activity.menu(selected));clickSystemText("Remove shortcut");idle(300);check(store().pages.get(2).size()==count,"Shortcut removal did not remove the desktop item");
        // Its folder copy must remain launchable after the desktop copy is removed.
        final LauncherStore.Item folderCopy=copy;main(()->activity.open(folderCopy));clickSystemText("Opened recipe: https://example.test/recipe/pot-roast");
        instrumentation.getTargetContext().startActivity(new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle(500);
        // Existing ordinary app shortcuts still use the same add-to-desktop action.
        main(()->{try{Object catalog=field(activity,"catalog");Object entry=((com.nth.desktop3d.AppCatalog)catalog).entries.stream().filter(e->e.component!=null&&FIXTURE.equals(e.component.getPackageName())).findFirst().orElse(null);check(entry!=null,"Fixture app missing from catalog");java.lang.reflect.Method menu=MainActivity.class.getDeclaredMethod("appMenu",entry.getClass(),LauncherStore.Item.class);menu.setAccessible(true);menu.invoke(activity,entry,null);}catch(Exception error){throw new AssertionError(error);}});
        clickSystemText("Add to this desktop");await(()->store().pages.get(2).size()==count+1,"Ordinary app shortcut could not be placed");assertOccupancy();capture("app-shortcut-and-folders");
    }
    private int addWidget(boolean configured){
        AtomicReference<Integer> result=new AtomicReference<>();
        main(()->{
            try{
                AppWidgetHost host=(AppWidgetHost)field(activity,"widgetHost");int id=host.allocateAppWidgetId();result.set(id);
                ComponentName provider=new ComponentName(FIXTURE,FIXTURE+(configured?".ConfiguredWidgetProvider":".FixtureWidgetProvider"));
                check(AppWidgetManager.getInstance(activity).bindAppWidgetIdIfAllowed(id,provider),"Fixture bind permission was not granted");
                set(activity,"pendingWidgetId",id);set(activity,"pendingWidgetPage",store().page);set(activity,"pendingWidgetRequest",3047);
                java.lang.reflect.Method save=MainActivity.class.getDeclaredMethod("savePendingWidget");save.setAccessible(true);save.invoke(activity);
                java.lang.reflect.Method deliver=MainActivity.class.getDeclaredMethod("onActivityResult",int.class,int.class,Intent.class);deliver.setAccessible(true);
                deliver.invoke(activity,3047,Activity.RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));
            }catch(Exception error){throw new AssertionError(error);}
        });idle(700);return result.get();
    }
    private void sendUpdate(int id,String label){instrumentation.getTargetContext().sendBroadcast(new Intent().setComponent(new ComponentName(FIXTURE,FIXTURE+".FixtureCommandReceiver")).putExtra("widgetId",id).putExtra("label",label));}
    private void moveWidget(int id,int x,int y){DesktopGrid.Cell start=cell("widget:"+id);dragView(widget(id),(x-start.x)*metric("cw"),(y-start.y)*metric("ch"));idle(250);}
    private void holdEmptyBackground(){
        ensureGestureWindow();DesktopGrid.Cell empty=DesktopGrid.nearest(new DesktopGrid.Cell("empty",false,0,0,1,1),cells(),metric("columns"),metric("visibleRows"));
        // A populated screen can have no wholly vacant cell; the icon's side gutter is still background.
        int[] p=position((View)field(desktop(),"workspace"));float x=p[0]+(empty==null?1:(empty.x+.5f)*metric("cw")),y=p[1]+24*activity.getResources().getDisplayMetrics().density+(empty==null?.5f:empty.y+.5f)*metric("ch");
        long start=SystemClock.uptimeMillis();pointer(start,MotionEvent.ACTION_DOWN,x,y);idle(android.view.ViewConfiguration.getLongPressTimeout()+150);pointer(start,MotionEvent.ACTION_UP,x,y);idle(300);
    }
    private void dragView(View view,float dx,float dy){ensureGestureWindow();check(view!=null,"Gesture view missing");int[] p=position(view);drag(p[0]+view.getWidth()/2f,p[1]+view.getHeight()/3f,dx,dy,true);}
    private void hold(View view){check(view!=null,"Held widget missing");int[] p=position(view);float x=p[0]+view.getWidth()/2f,y=p[1]+view.getHeight()/3f;long start=SystemClock.uptimeMillis();pointer(start,MotionEvent.ACTION_DOWN,x,y);idle(android.view.ViewConfiguration.getLongPressTimeout()+150);pointer(start,MotionEvent.ACTION_UP,x,y);idle(250);}
    private void tap(View view){int[] p=position(view);long start=SystemClock.uptimeMillis();pointer(start,MotionEvent.ACTION_DOWN,p[0]+view.getWidth()/2f,p[1]+view.getHeight()/2f);pointer(start,MotionEvent.ACTION_UP,p[0]+view.getWidth()/2f,p[1]+view.getHeight()/2f);idle(200);}
    private void drag(float x,float y,float dx,float dy,boolean longHold){
        long start=SystemClock.uptimeMillis();pointer(start,MotionEvent.ACTION_DOWN,x,y);if(longHold)idle(android.view.ViewConfiguration.getLongPressTimeout()+120);
        for(int step=1;step<=8;step++){pointer(start,MotionEvent.ACTION_MOVE,x+dx*step/8,y+dy*step/8);idle(35);}
        pointer(start,MotionEvent.ACTION_UP,x+dx,y+dy);idle(300);
    }
    private void pointer(long start,int action,float x,float y){MotionEvent event=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);try{check(instrumentation.getUiAutomation().injectInputEvent(event,true),"Native touch injection failed");}finally{event.recycle();}}
    private void ensureGestureWindow(){
        AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
        if(root!=null&&!root.findAccessibilityNodeInfosByText("Quickstep isn't responding").isEmpty()){
            clickSystemText("Close app");idle(400);root=instrumentation.getUiAutomation().getRootInActiveWindow();
        }
        check(root!=null&&android.text.TextUtils.equals("com.nth.desktop3d",root.getPackageName()),"Native widget gesture is blocked by another window");
    }
    private boolean clickNode(AccessibilityNodeInfo node,String text){
        if(node==null)return false;
        if((node.getText()!=null&&text.equalsIgnoreCase(node.getText().toString().trim()))||(node.getContentDescription()!=null&&text.equalsIgnoreCase(node.getContentDescription().toString().trim()))){
            Rect bounds=new Rect();node.getBoundsInScreen(bounds);
            if(node.isVisibleToUser()&&!bounds.isEmpty()){long start=SystemClock.uptimeMillis();pointer(start,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY());pointer(start,MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY());return true;}
        }
        for(int i=0;i<node.getChildCount();i++)if(clickNode(node.getChild(i),text))return true;
        return false;
    }
    private void clickSystemText(String text){
        long end=SystemClock.uptimeMillis()+7000;
        while(SystemClock.uptimeMillis()<end){
            if(clickNode(instrumentation.getUiAutomation().getRootInActiveWindow(),text)){idle(600);return;}
            for(android.view.accessibility.AccessibilityWindowInfo window:instrumentation.getUiAutomation().getWindows())if(clickNode(window.getRoot(),text)){idle(600);return;}
            idle(100);
        }
        throw new AssertionError("External widget/config menu missing: "+text);
    }
    private boolean known(int id){return java.util.Arrays.asList(instrumentation.getTargetContext().getSharedPreferences("desktop3d",0).getString("widget_ids_page_0","").split(",")).contains(String.valueOf(id));}
    @SuppressWarnings("unchecked") private List<DesktopGrid.Cell> cells(){return new ArrayList<>((List<DesktopGrid.Cell>)field(desktop(),"cells"));}
    private DesktopGrid.Cell cell(String id){return DesktopGrid.find(cells(),id);}
    private void assertOccupancy(){main(()->{
        List<DesktopGrid.Cell> cells=cells();int columns=metric("columns"),rows=metric("rows");
        check(cells.size()==store().pages.get(store().page).size()+java.util.Arrays.stream(store().preferences.getString("widget_ids_page_"+store().page,"").split(",")).filter(x->!x.isEmpty()).count(),"Layout lost a shortcut/folder/widget");
        for(int i=0;i<cells.size();i++){
            DesktopGrid.Cell current=cells.get(i);check(DesktopGrid.fits(current,columns,rows),"Item is outside the grid");
            View view=view(current.id);check(view!=null&&view.getWidth()>0&&view.getHeight()>0,"Item has no rendered bounds");
            for(int j=i+1;j<cells.size();j++)check(!current.overlaps(cells.get(j)),"Widget/app/folder cell overlap");
        }
    });}
    private int metric(String name){return (Integer)field(desktop(),name);}
    private Object desktop(){return field(activity,"desktop");}
    private LauncherStore store(){return (LauncherStore)field(activity,"store");}
    @SuppressWarnings("unchecked") private View view(String id){return ((Map<String,View>)field(desktop(),"cellViews")).get(id);}
    private View widget(int id){return view("widget:"+id);}
    private int[] position(View view){AtomicReference<int[]> result=new AtomicReference<>();main(()->{int[] location=new int[2];view.getLocationOnScreen(location);result.set(location);});return result.get();}
    private Object field(Object target,String name){try{java.lang.reflect.Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}catch(Exception error){throw new AssertionError(error);}}
    private void set(Object target,String name,Object value){try{java.lang.reflect.Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}catch(Exception error){throw new AssertionError(error);}}
    private void invokeDesktop(String name){try{java.lang.reflect.Method method=desktop().getClass().getDeclaredMethod(name);method.setAccessible(true);method.invoke(desktop());}catch(Exception error){throw new AssertionError(error);}}
    private interface Match {boolean matches(View view);}
    private View findIn(View view,Match match){if(view==null)return null;if(match.matches(view))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View result=findIn(group.getChildAt(i),match);if(result!=null)return result;}}return null;}
    private boolean hasText(String text){return findIn(activity.getWindow().getDecorView(),v->v instanceof TextView&&text.contentEquals(((TextView)v).getText()))!=null;}
    private void await(BooleanSupplier condition,String message){long end=SystemClock.uptimeMillis()+10000;while(SystemClock.uptimeMillis()<end){AtomicReference<Boolean> success=new AtomicReference<>(false);main(()->success.set(condition.getAsBoolean()));if(success.get())return;idle(100);}throw new AssertionError(message);}
    private void idle(long milliseconds){SystemClock.sleep(milliseconds);instrumentation.runOnMainSync(()->{});}
    private void main(Runnable work){AtomicReference<Throwable> failure=new AtomicReference<>();instrumentation.runOnMainSync(()->{try{work.run();}catch(Throwable error){failure.set(error);}});if(failure.get()!=null)throw new AssertionError(failure.get());}
    private void shell(String command)throws Exception{try(ParcelFileDescriptor.AutoCloseInputStream input=new ParcelFileDescriptor.AutoCloseInputStream(instrumentation.getUiAutomation().executeShellCommand(command))){byte[] bytes=new byte[1024];while(input.read(bytes)!=-1){}}}
    private void capture(String name){idle(700);android.graphics.Bitmap image=instrumentation.getUiAutomation().takeScreenshot();check(image!=null,"Cannot capture widget evidence");java.io.File directory=outputDirectory==null?new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null),"verification"):new java.io.File(outputDirectory);directory.mkdirs();try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(directory,name+".png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}catch(java.io.IOException error){throw new AssertionError(error);}finally{image.recycle();}}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
