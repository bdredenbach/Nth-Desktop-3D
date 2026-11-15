package com.nth.desktop3d;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

/** A stale remote registration requires a reset before this cross-UID fixture will report ready. */
public final class Discover077Smoke {
    private final Instrumentation test;private final MainActivity activity;
    public Discover077Smoke(Instrumentation test,MainActivity activity){this.test=test;this.activity=activity;}
    public void run()throws Exception {
        DiscoverClient client=(DiscoverClient)field(activity,"discover");CrystalPreferences prefs=new CrystalPreferences(activity);int provider=prefs.feedProvider();boolean enabled=prefs.feedEnabled();String layout=((LauncherStore)field(activity,"store")).preferences.getString("layout","");
        try{
            try(ParcelFileDescriptor p=test.getUiAutomation().executeShellCommand("am broadcast -n com.nth.desktop3d.widgetfixture/.FixtureCommandReceiver --es discoverMode requires_reset");InputStream in=new FileInputStream(p.getFileDescriptor())){byte[] b=new byte[1024];while(in.read(b)!=-1){}}
            main(()->{try{prefs.setFeedProvider(0);prefs.setFeedEnabled(true);client.disable();set(client,"targetOverride","com.nth.desktop3d.widgetfixture");set(client,"cleanNextBind",false);activity.getSharedPreferences("discover_connection",0).edit().putBoolean("clean_attach",false).putLong("launcher_update",activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).lastUpdateTime).apply();client.start();client.resume();client.open();}catch(Exception e){throw new AssertionError(e);}});
            idle(900);check(!client.showing(),"Stale fixture unexpectedly accepted the previous window registration");
            main(()->{client.reconnect();client.open();});await(client::showing,"Repair did not recover stale registration");check(client.report().contains("clearing the previous Google window registration"),"Repair did not submit a clean handshake");
            main(()->{client.disable();set(client,"cleanNextBind",false);activity.getSharedPreferences("discover_connection",0).edit().putBoolean("clean_attach",false).apply();new LauncherUpdateReceiver().onReceive(activity,new Intent(Intent.ACTION_MY_PACKAGE_REPLACED));client.open();});await(client::showing,"Update marker did not trigger a fresh connection");
            check(!activity.getSharedPreferences("discover_connection",0).getBoolean("clean_attach",true),"Successful connection did not acknowledge the update");check(layout.equals(((LauncherStore)field(activity,"store")).preferences.getString("layout","")),"Discover repair changed the desktop layout");
        }finally{main(()->{client.disable();set(client,"targetOverride",null);prefs.setFeedProvider(provider);prefs.setFeedEnabled(enabled);});}
    }
    private Object field(Object object,String name){try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new AssertionError(e);}}
    private void set(Object object,String name,Object value){try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}catch(Exception e){throw new AssertionError(e);}}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private void await(java.util.function.BooleanSupplier ready,String message){long until=SystemClock.uptimeMillis()+7000;while(SystemClock.uptimeMillis()<until){AtomicReference<Boolean> r=new AtomicReference<>(false);main(()->r.set(ready.getAsBoolean()));if(r.get())return;idle(100);}throw new AssertionError(message);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
