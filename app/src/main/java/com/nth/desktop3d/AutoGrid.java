package com.nth.desktop3d;

import android.content.Context;
import android.content.SharedPreferences;

/** Auto-sort owns a responsive page until the owner explicitly changes its grid or drags. */
final class AutoGrid {
    static String key(String field,int page){return "auto_grid_"+field+"_"+page;}
    static boolean active(LauncherStore store,int page){return store.preferences.getBoolean(key("enabled",page),false);}
    static int value(LauncherStore store,String setting,int fallback){
        boolean managed=active(store,store.page),pattern=store.preferences.getBoolean("folder_pattern_page_"+store.page,false);
        if(managed||pattern)return store.preferences.getInt(key(setting,store.page),setting.equals("grid_columns")?5:100);
        return store.preferences.getInt(setting,fallback);
    }
    static FittedGrid fit(Context c,SharedPreferences prefs,int width,int height,int count){
        android.text.TextPaint paint=new android.text.TextPaint();paint.setTextSize(Math.max(10,Math.min(18,prefs.getInt("label_size",12)))*c.getResources().getDisplayMetrics().scaledDensity);
        int labels=prefs.getBoolean("desktop_labels",true)?(int)Math.ceil(paint.getFontSpacing()):0;
        int quantum=Ui.dp(c,4),minimum=((Ui.dp(c,89)+labels+quantum-1)/quantum)*quantum;
        return FittedGrid.choose(width,Math.max(1,height-Ui.dp(c,36)),count,Ui.dp(c,80),minimum,Math.max(minimum,Ui.dp(c,144)),quantum);
    }
    static void enable(LauncherStore store,int page,FittedGrid grid,Context context){store.preferences.edit().putBoolean(key("enabled",page),true).putInt(key("grid_columns",page),grid.columns).putInt(key("grid_row_dp",page),Math.round(grid.pitch/context.getResources().getDisplayMetrics().density)).putBoolean("folder_pattern_page_"+page,false).apply();}
    static void metrics(LauncherStore store,int columns,int rowDp){if(store.preferences.getInt(key("grid_columns",store.page),-1)!=columns||store.preferences.getInt(key("grid_row_dp",store.page),-1)!=rowDp)store.preferences.edit().putInt(key("grid_columns",store.page),columns).putInt(key("grid_row_dp",store.page),rowDp).apply();}
    static boolean legacy(LauncherStore store){
        if(!store.preferences.getBoolean("folder_pattern_page_"+store.page,false)||store.pages.get(store.page).isEmpty())return false;
        for(LauncherStore.Item item:store.pages.get(store.page)){
            int n=java.util.Arrays.asList(FolderRules.NAMES).indexOf(item.label);
            if(!item.folder()||n<0||item.cellX!=FolderRules.X[n]||item.cellY!=FolderRules.Y[n])return false;
        }return true;
    }
    static void manual(LauncherStore store,String setting,int value){
        if(active(store,store.page)||store.preferences.getBoolean("folder_pattern_page_"+store.page,false)){
            int columns=AutoGrid.value(store,"grid_columns",4),pitch=AutoGrid.value(store,"grid_row_dp",112);
            if(active(store,store.page)){int n=0;for(LauncherStore.Item item:store.pages.get(store.page)){item.cellX=n%columns;item.cellY=n/columns;n++;}}
            store.preferences.edit().putBoolean(key("enabled",store.page),false).putBoolean("folder_pattern_page_"+store.page,false).putInt("grid_columns",columns).putInt("grid_row_dp",pitch).apply();
        }
        store.preferences.edit().putInt(setting,value).apply();store.save();
    }
    static void clear(SharedPreferences.Editor edit,int page){for(String field:new String[]{"enabled","grid_columns","grid_row_dp"})edit.remove(key(field,page));}
    static void copy(SharedPreferences.Editor edit,SharedPreferences prefs,int from,int to){edit.putBoolean(key("enabled",to),prefs.getBoolean(key("enabled",from),false)).putInt(key("grid_columns",to),prefs.getInt(key("grid_columns",from),4)).putInt(key("grid_row_dp",to),prefs.getInt(key("grid_row_dp",from),112));}
    private AutoGrid(){}
}
