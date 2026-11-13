package com.nth.desktop3d.discover;

import android.app.Service;
import android.content.*;
import android.net.Uri;
import android.os.*;
import java.util.ArrayList;
import java.util.List;

/** Signature-scoped IPC companion. It does not read, store or redraw Google content. */
public final class DiscoverBridgeService extends Service {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<ServiceConnection> connections=new ArrayList<>();
    private boolean destroyed;
    private void enforceOwner(){if(getPackageManager().checkSignatures(Binder.getCallingUid(),android.os.Process.myUid())!=android.content.pm.PackageManager.SIGNATURE_MATCH)throw new SecurityException("Only the paired Nth launcher can connect");}
    private final IFeedBridge.Stub bridge=new IFeedBridge.Stub(){@Override public void connect(IFeedBridgeCallback callback){enforceOwner();main.post(()->connectGoogle(callback));}};
    @Override public IBinder onBind(Intent intent){return bridge;}
    private void connectGoogle(IFeedBridgeCallback callback){
        if(destroyed)return;
        Intent intent=new Intent("com.android.launcher3.WINDOW_OVERLAY").setPackage("com.google.android.googlequicksearchbox").setData(Uri.parse("app://"+getPackageName()+":"+android.os.Process.myUid()+"?v=7&cv=9"));
        ServiceConnection connection=new ServiceConnection(){
            @Override public void onServiceConnected(ComponentName name,IBinder target){
                if(destroyed)return;
                try{callback.connected(new Binder(){@Override protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{enforceOwner();long identity=Binder.clearCallingIdentity();try{return target.transact(code,data,reply,flags);}finally{Binder.restoreCallingIdentity(identity);}}});}catch(RemoteException ignored){}
            }
            @Override public void onServiceDisconnected(ComponentName name){try{callback.unavailable();}catch(RemoteException ignored){}}
            @Override public void onNullBinding(ComponentName name){onServiceDisconnected(name);}
        };
        try{if(bindService(intent,connection,BIND_AUTO_CREATE|BIND_ADJUST_WITH_ACTIVITY))connections.add(connection);else callback.unavailable();}catch(SecurityException|RemoteException ignored){try{callback.unavailable();}catch(RemoteException gone){}}
    }
    @Override public boolean onUnbind(Intent intent){main.removeCallbacksAndMessages(null);disconnect();return false;}
    private void disconnect(){for(ServiceConnection connection:connections)try{unbindService(connection);}catch(IllegalArgumentException ignored){}connections.clear();}
    @Override public void onDestroy(){destroyed=true;main.removeCallbacksAndMessages(null);disconnect();super.onDestroy();}
}
