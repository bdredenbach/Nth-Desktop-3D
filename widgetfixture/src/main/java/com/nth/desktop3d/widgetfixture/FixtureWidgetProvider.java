package com.nth.desktop3d.widgetfixture;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Separate-UID provider used only by Android integration tests, never bundled in the launcher. */
public class FixtureWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){for(int id:ids)update(context,id,"Hosted widget ready");}
    public static void update(Context context,int id,String label){
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.widget);views.setTextViewText(R.id.label,label);
        Intent tap=new Intent(context,FixtureCommandReceiver.class).putExtra("widgetId",id).putExtra("label","Widget tapped");
        views.setOnClickPendingIntent(R.id.tap,PendingIntent.getBroadcast(context,id,tap,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
        AppWidgetManager.getInstance(context).updateAppWidget(id,views);
    }
}
