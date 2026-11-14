package com.nth.desktop3d;

import java.util.ArrayList;
import java.util.List;

/** Adds a separate, reversible desktop. It never reorganizes the owner's existing pages. */
final class FolderSorter {
    final List<LauncherStore.Item> folders=new ArrayList<>();int appCount;
    FolderSorter(List<AppCatalog.Entry> apps){
        for(int i=0;i<FolderRules.NAMES.length;i++){LauncherStore.Item folder=new LauncherStore.Item("folder",FolderRules.NAMES[i]);folder.cellX=FolderRules.X[i];folder.cellY=FolderRules.Y[i];folders.add(folder);}
        java.util.Set<String> keys=new java.util.HashSet<>();
        for(AppCatalog.Entry app:apps)if(keys.add(app.key)){
            String pkg=app.component==null?app.key:app.component.getPackageName();int group=FolderRules.classify(app.label,pkg,app.category);
            LauncherStore.Item folder=folders.get(group);
            folder.children.add(new LauncherStore.Item(app.key,app.label));appCount++;
        }
    }
    int apply(LauncherStore store){
        for(LauncherStore.Item folder:folders)if(folder.children.size()>1000)return -2;int page=store.appendPage();if(page<0)return -1;
        store.pages.get(page).addAll(folders);store.page=page;
        java.util.List<String> ids=new ArrayList<>();for(LauncherStore.Item f:folders)ids.add(f.id);
        store.preferences.edit().putBoolean("folder_pattern_page_"+page,true).putString("sort_undo_ids",android.text.TextUtils.join(",",ids)).putInt("sort_undo_page",page).apply();store.save();return page;
    }
    static boolean canUndo(LauncherStore store){return !store.preferences.getString("sort_undo_ids","").isEmpty();}
    static void undo(LauncherStore store){
        java.util.Set<String> ids=new java.util.HashSet<>(java.util.Arrays.asList(store.preferences.getString("sort_undo_ids","").split(",")));
        for(List<LauncherStore.Item> page:store.pages)page.removeIf(item->ids.contains(item.id));
        int page=store.preferences.getInt("sort_undo_page",-1);
        android.content.SharedPreferences.Editor edit=store.preferences.edit().remove("sort_undo_ids").remove("sort_undo_page");
        if(page>=0&&page<store.pageCount()&&store.pages.get(page).isEmpty()){
            edit.remove("folder_pattern_page_"+page);
            if(page==store.pageCount()-1&&page>=1&&store.preferences.getString("widget_ids_page_"+page,"").isEmpty())store.pages.remove(page);
        }edit.apply();store.page=Math.min(store.page,store.pageCount()-1);store.save();
    }
    android.view.View preview(android.content.Context context,AppCatalog catalog){return new android.view.View(context){
        final android.graphics.Paint paint=new android.graphics.Paint(3);
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);float cw=getWidth()/5f,ch=getHeight()/10f;
            for(int n=0;n<20;n++){LauncherStore.Item folder=folders.get(n);float x=(FolderRules.X[n]+.5f)*cw,y=FolderRules.Y[n]*ch;float size=Math.min(cw*.62f,ch*.60f);android.graphics.drawable.Drawable icon=catalog.icon(folder);icon.setBounds(Math.round(x-size/2),Math.round(y),Math.round(x+size/2),Math.round(y+size));icon.draw(c);paint.setColor(Ui.TEXT);paint.setTextAlign(android.graphics.Paint.Align.CENTER);paint.setTextSize(Math.min(Ui.dp(context,11),cw/7));c.drawText(IconLabel.display(folder.label),x,y+size+paint.getTextSize()+2,paint);}
        }
    };}
}
