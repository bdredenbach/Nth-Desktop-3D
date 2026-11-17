package com.nth.desktop3d.widgetfixture;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.android.libraries.launcherclient.*;

/** Separate-UID overlay protocol fixture. Never shipped in the launcher or companion. */
public class FixtureDiscoverService extends Service {
    private final Handler main=new Handler(Looper.getMainLooper());
    private ILauncherOverlayCallback callback;private WindowManager.LayoutParams parent;private View window;private String mode="normal";private boolean invalid,scrollStarted,registrationCleared,staleBinding;private float scrollPosition;private int lifecycle,attachCount;
    protected int apiVersion(){return 11;}
    private final ILauncherOverlay.Stub overlay=new ILauncherOverlay.Stub(){
        public void startScroll(){main.post(()->scrollStarted=true);}public void onScroll(float p){main.post(()->scrollPosition=p);}public void endScroll(){main.post(()->{if(mode.equals("gesture_only")&&scrollStarted&&scrollPosition>.99f)show();});}
        public void windowAttached(WindowManager.LayoutParams p,ILauncherOverlayCallback cb,int flags){main.post(()->attach(p,cb,false));}
        public void windowAttached2(Bundle b,ILauncherOverlayCallback cb){WindowManager.LayoutParams p=b.getParcelable("layout_params");main.post(()->attach(p,cb,true));}
        public void windowDetached(boolean changing){main.post(()->{if(!changing)registrationCleared=true;hide();});}
        public void closeOverlay(int flags){main.post(()->hide());}
        public void onPause(){main.post(()->{if(apiVersion()>=4)invalid=true;hide();});}public void onResume(){main.post(()->{if(apiVersion()>=4)invalid=true;else lifecycle=3;});}
        public void openOverlay(int flags){main.post(()->{if(!mode.equals("gesture_only"))show();});}
        public void requestVoiceDetection(boolean start){}public String getVoiceSearchLanguage(){return "en";}public boolean isVoiceDetectionRunning(){return false;}
        public boolean hasOverlayContent(){return true;}public void unusedMethod(){}public void setActivityState(int flags){main.post(()->{if(apiVersion()<4)invalid=true;else lifecycle=flags;if(mode.equals("early_scroll")&&flags==3)show();});}public boolean startSearch(byte[] data,Bundle bundle){return false;}
    };
    @Override public IBinder onBind(Intent intent){mode=getSharedPreferences("discover_fixture",0).getString("mode","normal");invalid=scrollStarted=registrationCleared=staleBinding=false;
        if(mode.equals("stale_binding")){android.content.SharedPreferences prefs=getSharedPreferences("discover_fixture",0);String first=prefs.getString("stale_session",null);if(first==null){first=intent.getDataString();prefs.edit().putString("stale_session",first).commit();}staleBinding=first.equals(intent.getDataString());}scrollPosition=0;lifecycle=attachCount=0;if(mode.equals("null"))return null;
        if(mode.startsWith("empty_descriptor")||mode.equals("wrong_descriptor"))return new Binder(){
            @Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
                if(code==IBinder.INTERFACE_TRANSACTION){if(reply!=null)reply.writeString(mode.equals("wrong_descriptor")?"com.nth.fixture.UnsupportedOverlay":"");return true;}
                return overlay.asBinder().transact(code,data,reply,flags);
            }
        };
        return overlay;
    }
    private void attach(WindowManager.LayoutParams params,ILauncherOverlayCallback cb,boolean bundle){hide();parent=params;callback=cb;attachCount++;invalid=bundle!=(apiVersion()>=3)&&!mode.equals("legacy_only")&&!mode.equals("stale_binding");if(staleBinding||mode.equals("silent")||mode.equals("empty_descriptor_silent")||mode.equals("early_scroll")||(mode.equals("requires_reset")&&!registrationCleared)||(mode.equals("legacy_only")&&bundle))return;
        if(mode.equals("reattach")&&attachCount==1){main.postDelayed(()->{try{cb.overlayStatusChanged(0);}catch(RemoteException ignored){}},5500);return;}
        Runnable ready=()->{try{if(callback==cb)cb.overlayStatusChanged(invalid||mode.equals("not_ready")?0:1);}catch(RemoteException ignored){}};if(mode.equals("slow"))main.postDelayed(ready,3500);else ready.run();}
    // TYPE_APPLICATION requires the activity token. A panel would wrongly accept
    // View.getWindowToken(), hiding the production client regression.
    private void show(){if(mode.equals("refuse_display"))return;if(invalid||lifecycle!=3){try{callback.overlayStatusChanged(0);}catch(RemoteException ignored){}return;}if(parent==null||window!=null)return;LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER);content.setBackgroundColor(0xFF102A42);TextView title=new TextView(this);title.setText("Discover protocol fixture");title.setTextColor(0xFFFFFFFF);title.setTextSize(25);content.addView(title);Button close=new Button(this);close.setText("Close fixture feed");close.setOnClickListener(v->hide());content.addView(close);WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(parent);lp.packageName=getPackageName();lp.type=WindowManager.LayoutParams.TYPE_APPLICATION;lp.width=-1;lp.height=-1;lp.x=lp.y=0;lp.flags=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;lp.setTitle("Nth cross-process Discover fixture");try{getSystemService(WindowManager.class).addView(content,lp);window=content;if(mode.equals("transient_ready"))callback.overlayStatusChanged(0);callback.overlayScrollChanged(1);if(mode.equals("transient_ready"))main.postDelayed(()->{try{if(window==content)callback.overlayStatusChanged(0);}catch(RemoteException ignored){}},1500);}catch(RuntimeException|RemoteException bad){android.util.Log.e("NthFeedFixture","Overlay attach failed",bad);try{callback.overlayStatusChanged(0);}catch(RemoteException ignored){}}}
    private void hide(){if(window!=null){getSystemService(WindowManager.class).removeViewImmediate(window);window=null;}if(callback!=null)try{callback.overlayScrollChanged(0);}catch(RemoteException ignored){}}
    @Override public void onDestroy(){hide();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
