package com.nth.desktop3d.widgetfixture;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class FixtureCommandReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        int id=intent.getIntExtra("widgetId",-1);String label=intent.getStringExtra("label");
        if(id>=0&&label!=null)FixtureWidgetProvider.update(context,id,label);
    }
}
