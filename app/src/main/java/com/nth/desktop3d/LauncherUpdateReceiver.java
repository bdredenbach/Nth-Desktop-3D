package com.nth.desktop3d;

import android.content.*;

/** An update invalidates only the overlay handshake, never desktop data or user feed choices. */
public final class LauncherUpdateReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){if(Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))context.getSharedPreferences("discover_connection",0).edit().putBoolean("clean_attach",true).apply();}
}
