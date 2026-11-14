package com.nth.desktop3d;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.*;
import android.view.View;
import android.view.WindowManager;
import com.google.android.libraries.launcherclient.ILauncherOverlay;
import com.google.android.libraries.launcherclient.ILauncherOverlayCallback;
import java.util.ArrayDeque;

/** Version-negotiated Google overlay, bounded startup, and a local connection report. */
final class DiscoverClient {
    static final String GOOGLE="com.google.android.googlequicksearchbox";
    private static final long STARTUP_MS=15000, DISPLAY_MS=8000;
    private final Activity activity;private final View desktop;private final Runnable unavailable;
    private final Handler main=new Handler(Looper.getMainLooper());private final ArrayDeque<String> events=new ArrayDeque<>();
    private ILauncherOverlay overlay;private ServiceConnection connection;
    private boolean bound,ready,requested,resumed,started,destroyed,showing,opening,allowScroll,attached,failed,reporting;
    private int generation,attempt,apiVersion=1,serviceStatus=-1;private String status="Not connected",result="NOT_TRIED",targetOverride,componentOverride,serviceName="Not resolved",servicePermission="None",googleVersion="Not checked";
    private long began;private Runnable deadline;private String windowContract="Not submitted";
    DiscoverClient(Activity activity,View desktop,Runnable unavailable){this.activity=activity;this.desktop=desktop;this.unavailable=unavailable;}
    private long versionCode(PackageInfo info){return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;}
    String status(){return status;}
    boolean showing(){return showing;}
    boolean isConnecting(){return requested&&!showing;}
    private String target(){return targetOverride==null?GOOGLE:targetOverride;}
    private Intent intent(){Intent value=new Intent("com.android.launcher3.WINDOW_OVERLAY").setPackage(target()).setData(new Uri.Builder().scheme("app").encodedAuthority(activity.getPackageName()+":"+android.os.Process.myUid()).appendQueryParameter("v","7").appendQueryParameter("cv","9").build());if(componentOverride!=null)value.setClassName(target(),componentOverride);return value;}
    private void event(String text){long elapsed=began==0?0:SystemClock.elapsedRealtime()-began;events.addLast(elapsed+" ms · "+text);while(events.size()>40)events.removeFirst();}
    private void persist(){if(events.isEmpty())return;activity.getSharedPreferences("discover_connection",0).edit().putString("last_report",report()).apply();}
    String report(){
        String version="unknown";try{PackageInfo info=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0);version=info.versionName+" ("+versionCode(info)+")";}catch(PackageManager.NameNotFoundException ignored){}
        boolean debug=(activity.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0;
        String text="Nth Desktop 3D · Google Discover connection\nLauncher: "+version+"\nBuild: "+(debug?"Discover/debug test":"Normal release")+"\nAndroid: "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\nDevice: "+Build.MANUFACTURER+" "+Build.MODEL+"\nGoogle app: "+googleVersion+"\nProvider: "+target()+"\nService: "+serviceName+"\nService permission: "+servicePermission+"\nOverlay API: "+apiVersion+"; client v7/cv9\nOutcome: "+result+"\nStatus: "+status+"\nReady bits: "+serviceStatus+"\nStarted: "+started+", resumed: "+resumed+", attach sent: "+attached+", visible: "+showing+"\nWindow: "+windowContract+"\nStartup allowance: 15 seconds; display allowance: 8 seconds\n\nConnection events\n"+String.join("\n",events);
        if(events.isEmpty()){String last=activity.getSharedPreferences("discover_connection",0).getString("last_report","");if(!last.isEmpty())text+="\n\nPrevious attempt\n"+last;}
        return text;
    }
    void start(){started=true;state();}
    void resume(){resumed=true;state();if(ready&&requested)openReady();}
    void pause(){resumed=false;close();state();persist();}
    void stop(){started=false;close();state();}
    private void state(){if(overlay==null||!attached)return;int flags=(started?1:0)|(resumed?2:0);remote("Activity lifecycle",()->{if(apiVersion>=4)overlay.setActivityState(flags);else if(resumed)overlay.onResume();else overlay.onPause();});}
    void reconnect(){disconnect();failed=false;ready=false;result="NOT_TRIED";status="Ready to retry";}
    void configurationChanged(){close();if(overlay!=null){attached=false;attach(generation);}}
    void open(){
        if(destroyed)return;
        if(requested)return;
        if(failed||(bound&&overlay==null))disconnect();
        requested=true;opening=false;allowScroll=false;failed=false;reporting=false;began=SystemClock.elapsedRealtime();events.clear();attempt++;result="CONNECTING";event("User requested Google Discover");
        if(!bound&&!connect())return;
        if(ready&&overlay!=null){openReady();return;}
        status="Connecting to Google Discover… Allowing up to 15 seconds for startup.";schedule(STARTUP_MS,false);
    }
    private boolean connect(){
        final int token=++generation;apiVersion=1;serviceStatus=-1;attached=false;ready=false;serviceName="Not resolved";servicePermission="None";windowContract="Not submitted";
        PackageManager pm=activity.getPackageManager();
        try{PackageInfo info=pm.getPackageInfo(target(),0);googleVersion=info.versionName+" ("+versionCode(info)+")";if(!info.applicationInfo.enabled){fail("APP_DISABLED","The Google app is disabled. Enable it in App info, then retry.");return false;}}
        catch(PackageManager.NameNotFoundException absent){googleVersion="Not installed";fail("APP_MISSING","The Google app is not installed or is unavailable in this Android profile.");return false;}
        try{
            ResolveInfo match=pm.resolveService(intent(),PackageManager.GET_META_DATA);
            if(match==null||match.serviceInfo==null){fail("SERVICE_UNAVAILABLE","This Google app version exposes no compatible overlay service to Nth Desktop.");return false;}
            ServiceInfo service=match.serviceInfo;serviceName=service.packageName+"/"+service.name;servicePermission=service.permission==null?"None":service.permission;apiVersion=Math.max(1,service.metaData==null?1:service.metaData.getInt("service.api.version",1));event("Resolved overlay API "+apiVersion+" · "+serviceName);
            if(!service.enabled||!service.exported){fail("SERVICE_DISABLED","Google's overlay service is disabled or is not exported to other apps.");return false;}
            if(service.permission!=null&&pm.checkPermission(service.permission,activity.getPackageName())!=PackageManager.PERMISSION_GRANTED){event("Required permission denied: "+service.permission);fail("PERMISSION_DENIED","Android requires a Google overlay permission this installation does not have. The connection report identifies it.");return false;}
        }catch(SecurityException denied){event(denied.getClass().getSimpleName()+": "+safe(denied));fail("PERMISSION_DENIED","Android denied access while checking Google's overlay service.");return false;}
        connection=new ServiceConnection(){
            @Override public void onServiceConnected(ComponentName name,IBinder binder){if(destroyed||token!=generation)return;try{String descriptor=binder.getInterfaceDescriptor();event("Service connected · "+descriptor);if(!"com.google.android.libraries.launcherclient.ILauncherOverlay".equals(descriptor)){fail("PROTOCOL_MISMATCH","The service returned an unsupported overlay interface.");return;}overlay=ILauncherOverlay.Stub.asInterface(binder);desktop.post(()->attach(token));}catch(RemoteException|RuntimeException error){remoteFailure("Service connection",error);}}
            @Override public void onServiceDisconnected(ComponentName name){lost(token,"SERVICE_DISCONNECTED","Google's overlay service disconnected.");}
            @Override public void onNullBinding(ComponentName name){lost(token,"NULL_BINDING","Google returned no overlay connection. Its access rules may reject this launcher build.");}
            @Override public void onBindingDied(ComponentName name){lost(token,"BINDING_DIED","The Google connection expired, for example after an app update. Retry to reconnect.");}
        };
        try{bound=activity.bindService(intent(),connection,Context.BIND_AUTO_CREATE|Context.BIND_IMPORTANT|Context.BIND_ADJUST_WITH_ACTIVITY);event("bindService returned "+bound);if(!bound){fail("BIND_REJECTED","Android could not bind Google's overlay service.");return false;}return true;}
        catch(SecurityException denied){bound=false;event(denied.getClass().getSimpleName()+": "+safe(denied));fail("PERMISSION_DENIED","Google or Android rejected this launcher’s access to the overlay service.");return false;}
        catch(RuntimeException error){bound=false;remoteFailure("Bind",error);return false;}
    }
    private ILauncherOverlayCallback callback(int token){return new ILauncherOverlayCallback.Stub(){
        public void overlayScrollChanged(float progress){main.post(()->{if(destroyed||token!=generation||failed||!ready||!resumed||!allowScroll)return;float p=Float.isFinite(progress)?Math.max(0,Math.min(1,progress)):0;boolean before=showing;showing=p>.01f;desktop.setTranslationX(desktop.getWidth()*p);if(showing&&!before){requested=false;opening=false;cancelDeadline();result="VISIBLE";status="Google Discover overlay is visible";event("Google reported visible content");persist();}else if(!showing&&before){allowScroll=false;event("Google overlay closed");persist();}});}
        public void overlayStatusChanged(int value){main.post(()->{if(destroyed||token!=generation||overlay==null||failed)return;serviceStatus=value;ready=(value&1)!=0;event("Google status bits: "+value);if(!requested&&result.equals("CANCELED")){persist();return;}if(ready){status=showing?"Google Discover overlay is visible":"Google Discover connected";result=showing?"VISIBLE":"READY";if(requested)openReady();}else{showing=false;desktop.setTranslationX(0);status="Google connected, but has not marked feed content ready.";}persist();});}
    };}
    private void attach(int token){
        if(destroyed||token!=generation||overlay==null||failed)return;
        if(!desktop.isAttachedToWindow()){main.postDelayed(()->attach(token),100);return;}
        WindowManager.LayoutParams params=new WindowManager.LayoutParams();params.copyFrom(activity.getWindow().getAttributes());
        // Google expects the activity token carried by these attributes. The view's
        // window token identifies a subwindow parent and cannot replace it.
        if(params.token==null){windowContract="Activity token missing";fail("WINDOW_TOKEN_MISSING","Android has not supplied the launcher's activity window token. Return to the desktop, then retry.");return;}
        windowContract="Activity attributes; type "+params.type+"; distinct from view token: "+!params.token.equals(desktop.getWindowToken());
        event("Window contract · "+windowContract);
        if(!remote("Attach window",()->{if(apiVersion<3)overlay.windowAttached(params,callback(token),15);else{Bundle bundle=new Bundle();bundle.putParcelable("layout_params",params);bundle.putParcelable("configuration",activity.getResources().getConfiguration());bundle.putInt("client_options",15);bundle.putInt("system_ui_visibility",desktop.getSystemUiVisibility());overlay.windowAttached2(bundle,callback(token));}}))return;
        attached=true;event("Submitted activity window attributes · "+(apiVersion<3?"legacy":"bundle"));state();persist();
    }
    private void openReady(){if(opening||overlay==null||!attached||!resumed||failed)return;opening=true;allowScroll=true;status="Google connected. Waiting for the overlay to appear…";event("Sent openOverlay");if(remote("Open overlay",()->overlay.openOverlay(new CrystalPreferences(activity).feed3dTransition()?1:0)))schedule(DISPLAY_MS,true);}
    private void schedule(long delay,boolean display){cancelDeadline();int token=generation,request=attempt;deadline=()->{if(destroyed||token!=generation||request!=attempt||!requested||showing||failed)return;if(display)fail("DISPLAY_TIMEOUT","Google accepted the connection but did not show its overlay within 8 seconds.");else if(overlay==null)fail("SERVICE_TIMEOUT","Android accepted the bind request but Google did not return a service within 15 seconds.");else if(serviceStatus<0)fail("NO_STATUS","Google connected but sent no readiness response within 15 seconds. Its access rules may reject this build.");else fail("CONTENT_NOT_READY","Google connected but reported that feed content was not ready after 15 seconds. Check Discover in the Google app, then retry.");};main.postDelayed(deadline,delay);}
    private void cancelDeadline(){if(deadline!=null)main.removeCallbacks(deadline);deadline=null;}
    boolean close(){boolean was=showing||requested||opening;boolean canceled=requested&&!failed;requested=false;opening=false;allowScroll=false;cancelDeadline();if(overlay!=null)quiet(()->overlay.closeOverlay(0));showing=false;desktop.setTranslationX(0);if(canceled){result="CANCELED";status="Google Discover connection canceled";event("User left or canceled the pending connection");persist();}return was;}
    private void lost(int token,String code,String message){if(destroyed||token!=generation)return;boolean waiting=requested;ready=false;overlay=null;attached=false;showing=false;desktop.setTranslationX(0);event(code);if(waiting)fail(code,message);else{if(!result.equals("CANCELED")){result=code;status=message;}persist();}}
    private void fail(String code,String message){boolean notify=requested&&!destroyed;failed=true;requested=false;opening=false;allowScroll=false;ready=false;cancelDeadline();result=code;status=message;event(code+" · "+message);if(overlay!=null)quiet(()->overlay.closeOverlay(0));showing=false;desktop.setTranslationX(0);persist();if(notify&&!reporting){reporting=true;main.post(()->{reporting=false;if(!destroyed&&failed)unavailable.run();});}}
    private interface Remote {void run()throws RemoteException;}
    private boolean remote(String stage,Remote work){try{work.run();return true;}catch(RemoteException|RuntimeException error){remoteFailure(stage,error);return false;}}
    private void quiet(Remote work){try{work.run();}catch(RemoteException|RuntimeException ignored){}}
    private void remoteFailure(String stage,Exception error){event(stage+" · "+error.getClass().getSimpleName()+": "+safe(error));fail(error instanceof SecurityException?"PERMISSION_DENIED":"REMOTE_ERROR",stage+" failed. The connection report contains the error type and details.");}
    private String safe(Exception error){String message=error.getMessage();if(message==null)return "No further message";return message.replace('\n',' ').replace('\r',' ').substring(0,Math.min(300,message.length()));}
    private void disconnect(){cancelDeadline();allowScroll=false;++generation;if(overlay!=null)quiet(()->{overlay.closeOverlay(0);overlay.windowDetached(activity.isChangingConfigurations());});overlay=null;attached=false;ready=false;showing=false;desktop.setTranslationX(0);if(bound&&connection!=null)try{activity.unbindService(connection);}catch(IllegalArgumentException ignored){}connection=null;bound=false;}
    void disable(){requested=false;opening=false;disconnect();}
    void destroy(){destroyed=true;main.removeCallbacksAndMessages(null);disconnect();}
}
