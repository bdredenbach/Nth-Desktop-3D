package com.nth.desktop3d;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.os.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicReference;

/** Reproduce Google's blank-descriptor forwarding Binder across a real UID boundary. */
public final class Discover085Smoke {
    private final Instrumentation test;private final MainActivity activity;private final String output;
    public Discover085Smoke(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;}
    public void run()throws Exception {
        DiscoverClient client=(DiscoverClient)field(activity,"discover");CrystalPreferences prefs=new CrystalPreferences(activity);int provider=prefs.feedProvider();boolean enabled=prefs.feedEnabled();
        String layout=((LauncherStore)field(activity,"store")).preferences.getString("layout","");
        try{
            mode("empty_descriptor");
            main(()->{client.disable();set(client,"targetOverride","com.nth.desktop3d.widgetfixture");set(client,"componentOverride",null);prefs.setFeedProvider(0);prefs.setFeedEnabled(true);client.start();client.resume();client.open();});
            await(()->client.showing(),7000,"Blank descriptor was rejected before the real overlay handshake");
            check(client.report().contains("Binder interface: Not advertised (empty)")&&client.report().contains("Outcome: VISIBLE")&&client.report().contains("attach sent: true"),"Blank interface connection report lost the handshake evidence");
            capture("085-discover-blank-interface-visible");
            main(()->{client.reconnect();client.open();});await(client::showing,7000,"Repair could not reopen the blank-descriptor feed");
            main(client::disable);mode("wrong_descriptor");main(client::open);
            await(()->client.report().contains("Outcome: PROTOCOL_MISMATCH"),5000,"Explicitly incompatible descriptor was accepted");
            check(client.report().contains("com.nth.fixture.UnsupportedOverlay")&&!client.showing(),"Unsupported interface was not recorded or became visible");
            main(client::disable);mode("empty_descriptor_silent");main(client::open);
            idle(900);check(!client.showing()&&client.isConnecting(),"An empty descriptor or oneway attach falsely marked the feed ready");
            await(()->client.report().contains("Outcome: NO_STATUS"),18000,"Silent blank-descriptor service escaped the bounded startup timeout");
            check(!client.showing()&&!client.isConnecting(),"Timed-out descriptorless service remained active");
            main(()->{client.reconnect();});mode("empty_descriptor");main(client::open);await(client::showing,7000,"Retry did not recover from the silent connection");
            check(layout.equals(((LauncherStore)field(activity,"store")).preferences.getString("layout","")),"Discover compatibility repair changed desktop contents");
        }finally{main(()->{client.disable();set(client,"targetOverride",null);set(client,"componentOverride",null);prefs.setFeedProvider(provider);prefs.setFeedEnabled(enabled);invoke("closePanel");});mode("normal");}
    }
    private void mode(String value)throws Exception {try(ParcelFileDescriptor p=test.getUiAutomation().executeShellCommand("am broadcast -n com.nth.desktop3d.widgetfixture/.FixtureCommandReceiver --es discoverMode "+value);InputStream in=new FileInputStream(p.getFileDescriptor())){byte[] b=new byte[1024];while(in.read(b)!=-1){}}}
    private void capture(String name)throws Exception{idle(700);Bitmap b=test.getUiAutomation().takeScreenshot();check(b!=null,"Discover screenshot unavailable");File dir=output==null?new File(activity.getExternalFilesDir(null),"verification"):new File(output);dir.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}}
    private Object field(Object o,String name){try{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new AssertionError(e);}}
    private void set(Object o,String name,Object value){try{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name){try{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}}
    private void main(Runnable r){AtomicReference<Throwable> error=new AtomicReference<>();test.runOnMainSync(()->{try{r.run();}catch(Throwable e){error.set(e);}});if(error.get()!=null)throw new AssertionError(error.get());}
    private void idle(long ms){SystemClock.sleep(ms);test.runOnMainSync(()->{});}
    private void await(java.util.function.BooleanSupplier ready,long allowance,String message){long end=SystemClock.uptimeMillis()+allowance;while(SystemClock.uptimeMillis()<end){AtomicReference<Boolean> value=new AtomicReference<>(false);main(()->value.set(ready.getAsBoolean()));if(value.get())return;idle(100);}throw new AssertionError(message);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
