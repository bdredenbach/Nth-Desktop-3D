package com.nth.desktop3d.widgetfixture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class FixtureCommandReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        String mode=intent.getStringExtra("discoverMode");if(mode!=null)context.getSharedPreferences("discover_fixture",0).edit().putString("mode",mode).commit();
        int id=intent.getIntExtra("widgetId",-1);String label=intent.getStringExtra("label");
        if(id>=0&&label!=null)FixtureWidgetProvider.update(context,id,label);
    }
}
