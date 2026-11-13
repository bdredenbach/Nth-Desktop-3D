package com.nth.desktop3d;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;

/** Every displayed widget must be registered through createView, including restored widgets. */
final class LauncherWidgetHost extends AppWidgetHost {
    LauncherWidgetHost(Context context){super(context,3047);}
    @Override protected AppWidgetHostView onCreateView(Context context,int id,AppWidgetProviderInfo info){return new LauncherWidgetView(context);}
    void releaseViews(){clearViews();}
}
