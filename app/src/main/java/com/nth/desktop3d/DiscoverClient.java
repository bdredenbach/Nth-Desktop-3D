package com.nth.desktop3d;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.view.View;
import android.view.WindowManager;
import com.google.android.libraries.launcherclient.ILauncherOverlay;
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback;

/** Optional Google overlay; missing/denied services yield an explicit setup panel. */
final class DiscoverClient {
    private final Activity activity;private final View desktop;private final Runnable unavailable;
    private final Handler main=new Handler(Looper.getMainLooper());
    private ILauncherOverlay overlay;private ServiceConnection connection;private boolean bound,ready,requested,resumed,destroyed,showing,opening,allowScroll;private int generation;
    private String status="Not connected",targetOverride;
    DiscoverClient(Activity activity,View desktop,Runnable unavailable){this.activity=activity;this.desktop=desktop;this.unavailable=unavailable;}
    String status(){return status;}
    boolean showing(){return showing;}
    private boolean installed(String pkg){try{activity.getPackageManager().getApplicationInfo(pkg,0);return true;}catch(android.content.pm.PackageManager.NameNotFoundException missing){return false;}}
    void resume(){resumed=true;if(overlay!=null)call(()->{overlay.setActivityState(3);overlay.onResume();});}
    void pause(){resumed=false;requested=false;close();if(overlay!=null)call(()->{overlay.setActivityState(1);overlay.onPause();});}
    void reconnect(){disconnect();connect();}
    void configurationChanged(){close();if(overlay!=null)attach(generation);}
    void open(){if(destroyed)return;if(bound&&overlay==null)disconnect();requested=true;if(!bound)connect();if(ready)openReady();int token=generation;main.postDelayed(()->{if(!destroyed&&token==generation&&requested&&!showing){requested=false;opening=false;status="Google Discover has not accepted the overlay connection. Use the built-in Nth Feed, or open the Google app.";unavailable.run();}},2500);}
    private void connect(){
        if(destroyed||bound)return;
        final int token=++generation;
        String pkg=targetOverride!=null?targetOverride:"com.google.android.googlequicksearchbox";
        Intent intent=new Intent("com.android.launcher3.WINDOW_OVERLAY").setPackage(pkg).setData(Uri.parse("app://"+activity.getPackageName()+":"+android.os.Process.myUid()+"?v=7&cv=9"));
        connection=new ServiceConnection(){
            @Override public void onServiceConnected(ComponentName name,IBinder service){if(destroyed||token!=generation)return;accept(service,token);}
            @Override public void onServiceDisconnected(ComponentName name){lost(token);}
            @Override public void onNullBinding(ComponentName name){lost(token);}
            @Override public void onBindingDied(ComponentName name){lost(token);}
        };
        try{bound=activity.bindService(intent,connection,Context.BIND_AUTO_CREATE|Context.BIND_ADJUST_WITH_ACTIVITY);status=bound?"Connecting to Google Discover…":"Direct Google overlay unavailable. Nth Feed works without a companion.";}catch(SecurityException denied){status="Google declined the direct connection. Use built-in Nth Feed or the Google app.";bound=false;}
    }
    private void accept(IBinder binder,int token){if(destroyed||token!=generation)return;overlay=ILauncherOverlay.Stub.asInterface(binder);desktop.post(()->attach(token));}
    private ILauncherOverlayCallback callback(int token){return new ILauncherOverlayCallback.Stub(){
        public void overlayScrollChanged(float progress){main.post(()->{if(destroyed||token!=generation||!ready||!resumed)return;float p=Math.max(0,Math.min(1,progress));if(p>0&&!allowScroll)return;showing=p>.01f;if(showing){requested=false;opening=false;}desktop.setTranslationX(desktop.getWidth()*p);});}
        public void overlayStatusChanged(int value){main.post(()->{if(destroyed||token!=generation||overlay==null)return;ready=(value&1)!=0;status=ready?"Google Discover connected":"Google Discover has no overlay content available";if(ready&&requested)openReady();if(!ready){showing=false;desktop.setTranslationX(0);}});}
    };}
    private void attach(int token){
        if(destroyed||token!=generation||overlay==null)return;
        if(!desktop.isAttachedToWindow()){desktop.postDelayed(()->attach(token),100);return;}
        call(()->{WindowManager.LayoutParams params=new WindowManager.LayoutParams();params.copyFrom(activity.getWindow().getAttributes());params.token=desktop.getWindowToken();Bundle bundle=new Bundle();bundle.putParcelable("layout_params",params);bundle.putParcelable("configuration",activity.getResources().getConfiguration());bundle.putInt("client_options",15);bundle.putInt("system_ui_visibility",desktop.getSystemUiVisibility());overlay.windowAttached2(bundle,callback(token));overlay.setActivityState(resumed?3:1);if(resumed)overlay.onResume();});
    }
    private void openReady(){if(opening)return;opening=true;allowScroll=true;boolean animate=new CrystalPreferences(activity).feed3dTransition();call(()->overlay.openOverlay(animate?1:0));}
    boolean close(){boolean was=showing;requested=false;opening=false;allowScroll=false;if(overlay!=null)call(()->overlay.closeOverlay(0));showing=false;desktop.setTranslationX(0);return was;}
    private void lost(int token){if(destroyed||token!=generation)return;ready=false;overlay=null;showing=false;desktop.setTranslationX(0);status="Google Discover disconnected";}
    private interface Remote {void run()throws RemoteException;}
    private void call(Remote work){try{work.run();}catch(RemoteException|RuntimeException failure){ready=false;showing=false;desktop.setTranslationX(0);status="Google Discover connection unavailable";}}
    private void disconnect(){allowScroll=false;++generation;if(overlay!=null)call(()->{overlay.closeOverlay(0);overlay.windowDetached(activity.isChangingConfigurations());});overlay=null;ready=false;showing=false;desktop.setTranslationX(0);if(bound&&connection!=null)try{activity.unbindService(connection);}catch(IllegalArgumentException ignored){}connection=null;bound=false;}
    void disable(){requested=false;opening=false;disconnect();}
    void destroy(){destroyed=true;main.removeCallbacksAndMessages(null);disconnect();}
}
