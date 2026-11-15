package com.nth.desktop3d;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.LinearLayout;

/** Returning from Android refreshes grants without silently skipping a recovery step. */
@android.annotation.SuppressLint("ViewConstructor")
final class SetupGuide extends LinearLayout {
    private final Activity activity;private final LauncherStore store;private final Runnable home,done;private int step;private LinearLayout body;private android.widget.ScrollView scroll;
    SetupGuide(Activity a,LauncherStore s,int start,Runnable home,Runnable done){super(a);activity=a;store=s;this.home=home;this.done=done;setOrientation(VERTICAL);step=Math.max(0,Math.min(5,start));store.preferences.edit().putBoolean("setup_in_progress",true).apply();refresh();}
    int step(){return step;}
    void back(){if(step>0){step--;scroll.scrollTo(0,0);refresh();}}
    private void text(String value){body.addView(Ui.text(activity,value,15,Ui.TEXT),new LayoutParams(-1,-2));}
    private void button(String label,Runnable action){body.addView(Ui.quiet(activity,label,action),new LayoutParams(-1,-2));}
    private void launch(Intent intent){try{activity.startActivity(intent);}catch(RuntimeException e){android.widget.Toast.makeText(activity,"This Android settings page is unavailable. Open App info instead.",android.widget.Toast.LENGTH_SHORT).show();}}
    private void appInfo(){launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+activity.getPackageName())));}
    void refresh(){int previousScroll=scroll==null?0:scroll.getScrollY();removeAllViews();scroll=new android.widget.ScrollView(activity);scroll.setFillViewport(true);body=Ui.column(activity);body.setPadding(0,0,0,Ui.dp(activity,12));scroll.addView(body,new android.widget.ScrollView.LayoutParams(-1,-2));addView(scroll,new LayoutParams(-1,0,1));scroll.post(()->scroll.scrollTo(0,previousScroll));store.preferences.edit().putInt("setup_step",step).apply();String[] titles={"Choose your Home app","Media controls access","System gesture access","Calendar card","Weather location","Ready to use"};text("Step "+(step+1)+" of 6 · "+titles[step]);
        if(step==0){boolean held=activity.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0)!=null&&activity.getPackageName().equals(activity.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0).activityInfo.packageName);text(held?"Nth Desktop 3D is your default Home app.":"Choose Nth Desktop 3D as Home. You can also skip this optional step.");button("Set as Home app",home);}
        if(step==1){boolean granted=android.os.Build.VERSION.SDK_INT>=27&&activity.getSystemService(android.app.NotificationManager.class).isNotificationListenerAccessGranted(new ComponentName(activity,MediaAccess.class));text(granted?"Media access is enabled.":"Optional: notification access lets the media card read the playing track and control its player.");text("If Android says Restricted setting: open App info, open its three-dot menu, and choose Allow restricted settings if offered. Then return here and tap Enable media access again. Some devices manage this differently.");button("Enable media access",()->{Intent i=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);if(android.os.Build.VERSION.SDK_INT>=30)i=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,new ComponentName(activity,MediaAccess.class).flattenToString());launch(i);});button("Open App info",this::appInfo);}
        if(step==2){text(GestureAccess.available()?"System gesture access is enabled.":"Optional: enable Nth gesture actions for notifications, quick settings, recent apps and screen lock. This service does not read screen contents or typing.");text("If Android blocks this setting, use App info and Allow restricted settings when offered, then return to this step.");button("Enable system gesture access",()->launch(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));button("Open App info",this::appInfo);}
        if(step==3){text(activity.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED?"Calendar access is enabled.":"Optional: show upcoming events from calendars synced to Android.");button("Enable calendar card",()->activity.requestPermissions(new String[]{android.Manifest.permission.READ_CALENDAR},3074));button("Open App info",this::appInfo);}
        if(step==4){text(activity.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED?"Approximate location is enabled.":"Optional: use approximate location for the weather card. You can enter a city instead.");button("Enable weather location",()->activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},3075));button("Open App info",this::appInfo);}
        if(step==5)text("Hold empty desktop space for screen management. Hold and move an app, folder or widget to drag it; hold still for its menu. You can revisit this guide under Settings → Launcher → Setup & permissions.");
        text("Returning from Android keeps this step open. Tap Next when you are ready; optional access can be skipped.");
        LinearLayout navigation=Ui.row(activity);navigation.setTag("setup-navigation");
        android.widget.Button back=Ui.quiet(activity,"Back",this::back);back.setEnabled(step>0);navigation.addView(back,new LayoutParams(-2,-2));
        navigation.addView(new android.view.View(activity),new LayoutParams(0,1,1));
        android.widget.Button next=Ui.primary(activity,step==5?"Finish":"Next",()->{if(step==5){store.preferences.edit().putBoolean("welcomed",true).putBoolean("setup_in_progress",false).remove("setup_step").apply();done.run();}else{step++;scroll.scrollTo(0,0);refresh();}});navigation.addView(next,new LayoutParams(-2,-2));addView(navigation,new LayoutParams(-1,-2));
    }
}
