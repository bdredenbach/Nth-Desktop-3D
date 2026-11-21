package com.nth.desktop3d;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.LinearLayout;

/** Each optional step stays open until Next; only Finish completes setup. */
@android.annotation.SuppressLint("ViewConstructor")
final class SetupGuide extends LinearLayout {
    private final Activity activity;private final LauncherStore store;private final Runnable home,done;private int step;private LinearLayout body;private android.widget.ScrollView scroll;
    SetupGuide(Activity a,LauncherStore s,int start,Runnable home,Runnable done){super(a);activity=a;store=s;this.home=home;this.done=done;setOrientation(VERTICAL);step=Math.max(0,Math.min(5,start));store.preferences.edit().putBoolean("setup_in_progress",true).apply();refresh();}
    int step(){return step;}
    void back(){if(step>0){step--;scroll.scrollTo(0,0);refresh();}}
    private void text(String value){body.addView(Ui.text(activity,value,15,Ui.TEXT),new LayoutParams(-1,-2));}
    private void button(String label,Runnable action){body.addView(Ui.quiet(activity,label,action),new LayoutParams(-1,-2));}
    private void accessButton(boolean granted,String request,String enabled,Runnable action){
        android.widget.Button button=Ui.quiet(activity,granted?enabled:request,action);button.setContentDescription(request);button.setEnabled(!granted);button.setAlpha(granted?.8f:1);body.addView(button,new LayoutParams(-1,-2));
    }
    private void status(boolean granted,String enabled,String optional){android.widget.TextView label=Ui.text(activity,granted?"✓  "+enabled:optional,16,granted?Ui.ACCENT:Ui.TEXT);label.setTag("setup-access-status");label.setPadding(0,Ui.dp(activity,12),0,Ui.dp(activity,12));body.addView(label,new LayoutParams(-1,-2));}
    private void launch(Intent intent){try{activity.startActivityForResult(intent,3083);}catch(RuntimeException e){android.widget.Toast.makeText(activity,"This Android settings page is unavailable. Open App info instead.",android.widget.Toast.LENGTH_SHORT).show();}}
    private void appInfo(){launch(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+activity.getPackageName())));}
    private void gestures(){
        // AOSP's exported service-details action; devices without it keep the public list fallback.
        Intent details=new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").putExtra(Intent.EXTRA_COMPONENT_NAME,new ComponentName(activity,GestureAccess.class).flattenToString());
        try{activity.startActivityForResult(details,3083);}catch(RuntimeException unavailable){launch(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
    }
    void refresh(){
        int previousScroll=scroll==null?0:scroll.getScrollY();removeAllViews();scroll=new android.widget.ScrollView(activity);scroll.setFillViewport(true);body=Ui.column(activity);body.setPadding(0,0,0,Ui.dp(activity,12));scroll.addView(body,new android.widget.ScrollView.LayoutParams(-1,-2));addView(scroll,new LayoutParams(-1,0,1));scroll.post(()->scroll.scrollTo(0,previousScroll));store.preferences.edit().putInt("setup_step",step).apply();
        String[] titles={"Choose your Home app","Media controls access","System gesture access","Calendar card","Weather location","Ready to use"};text("Step "+(step+1)+" of 6 · "+titles[step]);boolean granted=false;
        if(step==0){granted=SetupAccess.home(activity);status(granted,"Nth Desktop 3D is your default Home app.","Choose Nth Desktop 3D as Home, or tap Next to skip for now.");accessButton(granted,"Set as Home app","Home app selected",home);}
        if(step==1){granted=SetupAccess.media(activity);status(granted,"Media access is enabled.","Optional: show the playing track and control its player from the media card.");if(!granted)text("If Android says App was denied access or Restricted setting: tap Open App info below, open its three-dot menu, and choose Allow restricted settings if offered. Return here and tap Enable media access again.");accessButton(granted,"Enable media access","Media access enabled",()->{Intent i=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);if(android.os.Build.VERSION.SDK_INT>=30)i=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,new ComponentName(activity,MediaAccess.class).flattenToString());launch(i);});button("Open App info",this::appInfo);}
        if(step==2){granted=SetupAccess.gestures(activity);status(granted,"System gesture access is enabled.","Optional: enable notifications, quick settings, recent apps and screen lock gestures.");if(!granted){text("Turn on Use Nth gesture actions on the Android page. The accessibility shortcut alone does not enable the service. This service does not read screen contents or typing.");text("If Android blocks this setting, use Open App info and Allow restricted settings when offered, then return to this step.");}accessButton(granted,"Enable system gesture access","System gesture access enabled",this::gestures);button("Open App info",this::appInfo);}
        if(step==3){granted=SetupAccess.calendar(activity);status(granted,"Calendar access is enabled.","Optional: show upcoming events from calendars synced to Android.");accessButton(granted,"Enable calendar card","Calendar access enabled",()->activity.requestPermissions(new String[]{android.Manifest.permission.READ_CALENDAR},3074));button("Open App info",this::appInfo);}
        if(step==4){granted=SetupAccess.location(activity);status(granted,"Approximate location is enabled.","Optional: use approximate location for weather in the card and Nth Nota AI. You can enter a city instead.");accessButton(granted,"Enable weather location","Weather location enabled",()->activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},3075));button("Open App info",this::appInfo);}
        if(step==5){text("Your desktop is ready. Optional access can be changed later in Settings → Launcher → Setup & permissions.");text((SetupAccess.home(activity)?"✓ Home app selected":"Home app: skipped")+"\n"+(SetupAccess.media(activity)?"✓ Media access enabled":"Media access: skipped")+"\n"+(SetupAccess.gestures(activity)?"✓ System gestures enabled":"System gestures: skipped")+"\n"+(SetupAccess.calendar(activity)?"✓ Calendar access enabled":"Calendar access: skipped")+"\n"+(SetupAccess.location(activity)?"✓ Weather location enabled":"Weather location: skipped"));text("Tap Finish to save and open your desktop.");}
        else text(granted?"This step is ready. Tap Next to continue.":"Returning from Android keeps this step open. Tap Next to skip this optional access and continue.");
        LinearLayout navigation=Ui.row(activity);navigation.setTag("setup-navigation");android.widget.Button back=Ui.quiet(activity,"Back",this::back);back.setEnabled(step>0);navigation.addView(back,new LayoutParams(-2,-2));navigation.addView(new android.view.View(activity),new LayoutParams(0,1,1));
        android.widget.Button next=Ui.primary(activity,step==5?"Finish":"Next",()->{if(step==5){store.preferences.edit().putBoolean("welcomed",true).putBoolean("setup_in_progress",false).remove("setup_step").apply();done.run();}else{step++;scroll.scrollTo(0,0);refresh();}});navigation.addView(next,new LayoutParams(-2,-2));addView(navigation,new LayoutParams(-1,-2));
    }
}
