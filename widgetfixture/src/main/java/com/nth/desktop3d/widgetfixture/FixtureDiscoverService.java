package com.nth.desktop3d.widgetfixture;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.android.libraries.launcherclient.*;

/** Separate-UID overlay protocol fixture. Never shipped in the launcher or companion. */
public final class FixtureDiscoverService extends Service {
    private final Handler main=new Handler(Looper.getMainLooper());
    private ILauncherOverlayCallback callback;private WindowManager.LayoutParams parent;private View window;
    private final ILauncherOverlay.Stub overlay=new ILauncherOverlay.Stub(){
        public void startScroll(){}public void onScroll(float p){}public void endScroll(){}
        public void windowAttached(WindowManager.LayoutParams p,ILauncherOverlayCallback cb,int flags){main.post(()->attach(p,cb));}
        public void windowAttached2(Bundle b,ILauncherOverlayCallback cb){WindowManager.LayoutParams p=b.getParcelable("layout_params");main.post(()->attach(p,cb));}
        public void windowDetached(boolean changing){main.post(()->hide());}
        public void closeOverlay(int flags){main.post(()->hide());}
        public void onPause(){main.post(()->hide());}public void onResume(){}
        public void openOverlay(int flags){main.post(()->show());}
        public void requestVoiceDetection(boolean start){}public String getVoiceSearchLanguage(){return "en";}public boolean isVoiceDetectionRunning(){return false;}
        public boolean hasOverlayContent(){return true;}public void unusedMethod(){}public void setActivityState(int flags){}public boolean startSearch(byte[] data,Bundle bundle){return false;}
    };
    @Override public IBinder onBind(Intent intent){return overlay;}
    private void attach(WindowManager.LayoutParams params,ILauncherOverlayCallback cb){hide();parent=params;callback=cb;try{cb.overlayStatusChanged(1);}catch(RemoteException ignored){}}
    private void show(){if(parent==null||window!=null)return;LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setGravity(Gravity.CENTER);content.setBackgroundColor(0xFF102A42);TextView title=new TextView(this);title.setText("Discover protocol fixture");title.setTextColor(0xFFFFFFFF);title.setTextSize(25);content.addView(title);Button close=new Button(this);close.setText("Close fixture feed");close.setOnClickListener(v->hide());content.addView(close);WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(parent);lp.packageName=getPackageName();lp.type=WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;lp.width=-1;lp.height=-1;lp.x=lp.y=0;lp.flags=WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;lp.setTitle("Nth cross-process Discover fixture");try{getSystemService(WindowManager.class).addView(content,lp);window=content;callback.overlayScrollChanged(1);}catch(RuntimeException|RemoteException bad){android.util.Log.e("NthFeedFixture","Overlay attach failed",bad);try{callback.overlayStatusChanged(0);}catch(RemoteException ignored){}}}
    private void hide(){if(window!=null){getSystemService(WindowManager.class).removeViewImmediate(window);window=null;}if(callback!=null)try{callback.overlayScrollChanged(0);}catch(RemoteException ignored){}}
    @Override public void onDestroy(){hide();main.removeCallbacksAndMessages(null);super.onDestroy();}
}
