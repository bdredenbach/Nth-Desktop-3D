package com.nth.desktop3d;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;

/** Delayed, modern, legacy and rejected real cross-UID Google overlay protocol checks. */
public final class DiscoverSmoke {
    private final Instrumentation test;private final MainActivity activity;private final String output;private DiscoverClient client;
    public DiscoverSmoke(Instrumentation test,MainActivity activity,String output){this.test=test;this.activity=activity;this.output=output;}
    public void run()throws Exception{
        CrystalPreferences prefs=new CrystalPreferences(activity);int provider=prefs.feedProvider();boolean enabled=prefs.feedEnabled(),edge=prefs.feedEdgeSwipe();
        client=(DiscoverClient)field("discover");
        try{
            main(()->{prefs.setFeedEnabled(true);prefs.setFeedEdgeSwipe(true);prefs.setFeedProvider(0);client.start();client.resume();});
            mode("slow",null);open();SystemClock.sleep(2800);check(onMain(()->client.isConnecting()),"Cold Google startup was canceled before the delayed service became ready");capture("discover-startup-wait");visible();check(client.report().contains("Overlay API: 11;"),"Modern API was not negotiated");check(client.report().contains("Window: Activity attributes; type 1; distinct from view token: true"),"Overlay did not preserve the activity token distinct from the view-window token");capture("discover-modern-overlay");close();
            mode("normal","com.nth.desktop3d.widgetfixture.FixtureLegacyDiscoverService");open();visible();check(client.report().contains("Overlay API: 1;")&&client.report().contains("legacy"),"Legacy API did not use its own window/lifecycle calls");capture("discover-legacy-overlay");close();
            mode("reattach",null);open();visible();SystemClock.sleep(2000);check(onMain(client::showing)&&client.report().contains("refreshing window registration"),"Silent window registration did not recover, or an old callback replaced the new one");capture("discover-window-recovery");close();
            mode("legacy_only",null);open();await(()->client.showing(),12000,"Silent modern attachment did not recover through a fresh legacy connection");check(client.report().contains("reconnecting with legacy window attachment")&&client.report().contains("Overlay API: 11;"),"Fallback lost the negotiated modern lifecycle");capture("discover-attachment-recovery");close();
            mode("null",null);open();await(()->client.report().contains("Outcome: NULL_BINDING"),6000,"Null binding was not diagnosed");main(()->invoke("showDiscoverDetails"));await(()->text("Copy connection report")!=null,3000,"Connection report screen missing");main(()->text("Copy connection report").performClick());AtomicReference<String> copied=new AtomicReference<>();main(()->{ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);ClipData clip=clipboard.getPrimaryClip();copied.set(clip==null?"":clip.getItemAt(0).coerceToText(activity).toString());});check(copied.get().contains("Outcome: NULL_BINDING")&&copied.get().contains("Overlay API:"),"Clipboard lost the exact failure report");capture("discover-connection-report");close();
            mode("silent",null);open();await(()->client.report().contains("Outcome: NO_STATUS"),19000,"No readiness callback did not produce the bounded startup diagnostic");check(client.report().contains("Ready bits: -1"),"Missing readiness was confused with content-not-ready");close();
            mode("not_ready",null);open();await(()->client.report().contains("Outcome: CONTENT_NOT_READY"),19000,"Explicit no-content status was not diagnosed");check(client.report().contains("Ready bits: 0"),"Explicit Google status was lost");close();
            mode("refuse_display",null);open();await(()->client.report().contains("Outcome: DISPLAY_TIMEOUT"),12000,"Accepted service without visible overlay was not diagnosed");close();
            mode("slow",null);open();SystemClock.sleep(600);main(client::close);SystemClock.sleep(3600);check(onMain(()->!client.showing()&&!client.isConnecting()&&((View)field("root")).getTranslationX()==0),"Cancel allowed a delayed Google callback to reopen the overlay");check(client.report().contains("Outcome: CANCELED"),"Canceled startup was not identified");
        }finally{mode("normal",null);main(()->{client.disable();set("targetOverride",null);set("componentOverride",null);prefs.setFeedProvider(provider);prefs.setFeedEnabled(enabled);prefs.setFeedEdgeSwipe(edge);invoke("closePanel");});}
    }
    private void mode(String mode,String component)throws Exception{try(ParcelFileDescriptor fd=test.getUiAutomation().executeShellCommand("am broadcast -n com.nth.desktop3d.widgetfixture/.FixtureCommandReceiver --es discoverMode "+mode);FileInputStream in=new FileInputStream(fd.getFileDescriptor())){byte[] buffer=new byte[1024];while(in.read(buffer)!=-1){}}main(()->{invoke("closePanel");client.reconnect();set("targetOverride","com.nth.desktop3d.widgetfixture");set("componentOverride",component);});}
    private void open(){main(()->invoke("openDiscover"));}private void close(){main(()->{client.close();invoke("closePanel");});SystemClock.sleep(200);}
    private void visible(){await(()->client.showing(),8000,"Separate-UID overlay did not become visible");check(((View)field("root")).getTranslationX()>0,"Google scroll callback did not move the launcher");}
    private boolean onMain(BooleanSupplier condition){AtomicBoolean value=new AtomicBoolean();main(()->value.set(condition.getAsBoolean()));return value.get();}
    private void await(BooleanSupplier condition,long timeout,String message){long until=SystemClock.uptimeMillis()+timeout;while(SystemClock.uptimeMillis()<until){if(onMain(condition))return;SystemClock.sleep(100);}throw new AssertionError(message+"\n"+client.report());}
    private View text(String value){return find(activity.getWindow().getDecorView(),v->v instanceof TextView&&value.contentEquals(((TextView)v).getText()));}
    private View find(View root,java.util.function.Predicate<View> match){if(root.getVisibility()!=View.VISIBLE)return null;if(match.test(root))return root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View v=find(((ViewGroup)root).getChildAt(i),match);if(v!=null)return v;}return null;}
    private Object field(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new AssertionError(e);}}
    private void set(String name,Object value){try{Field f=DiscoverClient.class.getDeclaredField(name);f.setAccessible(true);f.set(client,value);}catch(Exception e){throw new AssertionError(e);}}
    private void invoke(String name){try{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new AssertionError(e);}}
    private void main(Runnable run){test.runOnMainSync(run);}private void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private void capture(String name)throws Exception{SystemClock.sleep(500);Bitmap image=test.getUiAutomation().takeScreenshot();check(image!=null,"Discover screenshot unavailable");File directory=output==null?new File(test.getTargetContext().getExternalFilesDir(null),"verification"):new File(output);directory.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}}
}
