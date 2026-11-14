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
    private ILauncherOverlayCallback callback;private WindowManager.LayoutParams parent;private View window;private String mode="normal";private boolean invalid;private int lifecycle;
    protected int apiVersion(){return 7;}
    private final ILauncherOverlay.Stub overlay=new ILauncherOverlay.Stub(){
        public void startScroll(){}public void onScroll(float p){}public void endScroll(){}
        public void windowAttached(WindowManager.LayoutParams p,ILauncherOverlayCallback cb,int flags){main.post(()->attach(p,cb,false));}
        public void windowAttached2(Bundle b,ILauncherOverlayCallback cb){WindowManager.LayoutParams p=b.getParcelable("layout_params");main.post(()->attach(p,cb,true));}
        public void windowDetached(boolean changing){main.post(()->hide());}
        public void closeOverlay(int flags){main.post(()->hide());}
        public void onPause(){main.post(()->{if(apiVersion()>=4)invalid=true;hide();});}public void onResume(){main.post(()->{if(apiVersion()>=4)invalid=true;else lifecycle=3;});}
        public void openOverlay(int flags){main.post(()->show());}
        public void requestVoiceDetection(boolean start){}public String getVoiceSearchLanguage(){return "en";}public boolean isVoiceDetectionRunning(){return false;}
        public boolean hasOverlayContent(){return true;}public void unusedMethod(){}public void setActivityState(int flags){main.post(()->{if(apiVersion()<4)invalid=true;else lifecycle=flags;});}public boolean startSearch(byte[] data,Bundle bundle){return false;}
    };
    @Override public IBinder onBind(Intent intent){mode=getSharedPreferences("discover_fixture",0).getString("mode","normal");invalid=false;lifecycle=0;return mode.equals("null")?null:overlay;}
    private void attach(WindowManager.LayoutParams params,ILauncherOverlayCallback cb,boolean bundle){hide();parent=params;callback=cb;invalid=bundle!=(apiVersion()>=3);if(mode.equals("silent"))return;Runnable ready=()->{try{if(callback==cb)cb.overlayStatusChanged(invalid||mode.equals("not_ready")?0:1);}catch(RemoteException ignored){}};if(mode.equals("slow"))main.postDelayed(ready,3500);else ready.run();}
    private void show(){if(mode.equals("refuse_display"))return;if(invalid||lifecycle!=3){try{callback.overlayStatusChanged(0);}catch(RemoteException ignored){}return;}if(parent==null||window!=null)return;LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER);content.setBackgroundColor(0xFF102A42);TextView title=new TextView(this);title.setText("Discover protocol fixture");title.setTextColor(0xFFFFFFFF);title.setTextSize(25);content.addView(title);Button close=new Button(this);close.setText("Close fixture feed");close.setOnClickListener(v->hide());content.addView(close);WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(parent);lp.packageName=getPackageName();lp.type=WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;lp.width=-1;lp.height=-1;lp.x=lp.y=0;lp.flags=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;lp.setTitle("Nth cross-process Discover fixture");try{getSystemService(WindowManager.class).addView(content,lp);window=content;callback.overlayScrollChanged(1);}catch(RuntimeException|RemoteException bad){android.util.Log.e("NthFeedFixture","Overlay attach failed",bad);try{callback.overlayStatusChanged(0);}catch(RemoteException ignored){}}}
    private void hide(){if(window!=null){getSystemService(WindowManager.class).removeViewImmediate(window);window=null;}if(callback!=null)try{callback.overlayScrollChanged(0);}catch(RemoteException ignored){}}
    @Override public void onDestroy(){hide();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
