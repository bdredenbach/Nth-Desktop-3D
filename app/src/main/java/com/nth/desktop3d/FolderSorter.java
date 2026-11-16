package com.nth.desktop3d;

import java.util.ArrayList;
import java.util.List;

/** Adds a separate, reversible desktop. It never reorganizes the owner's existing pages. */
final class FolderSorter {
    final List<LauncherStore.Item> folders=new ArrayList<>();int appCount;
    FolderSorter(List<AppCatalog.Entry> apps){
        for(int i=0;i<FolderRules.NAMES.length;i++){LauncherStore.Item folder=new LauncherStore.Item("folder",FolderRules.NAMES[i]);folders.add(folder);}
        java.util.Set<String> keys=new java.util.HashSet<>();
        for(AppCatalog.Entry app:apps)if(keys.add(app.key)){
            String pkg=app.component==null?app.key:app.component.getPackageName();int group=FolderRules.classify(app.label,pkg,app.category);
            LauncherStore.Item folder=folders.get(group);
            folder.children.add(new LauncherStore.Item(app.key,app.label));appCount++;
        }
    }
    int apply(LauncherStore store,android.content.Context context,java.util.function.IntFunction<FittedGrid> choose){
        for(LauncherStore.Item folder:folders)if(folder.children.size()>1000)return -2;
        int capacity=Math.max(1,choose.apply(folders.size()).capacity),count=(folders.size()+capacity-1)/capacity;
        if(store.pageCount()+count>LauncherStore.MAX_PAGES)return -1;
        int first=-1;java.util.List<String> ids=new ArrayList<>(),pages=new ArrayList<>();
        for(int offset=0;offset<folders.size();offset+=capacity){
            int page=store.appendPage();if(first<0)first=page;pages.add(Integer.toString(page));int size=Math.min(capacity,folders.size()-offset);FittedGrid grid=choose.apply(size);
            for(int n=0;n<size;n++){LauncherStore.Item folder=folders.get(offset+n);folder.cellX=n%grid.columns;folder.cellY=n/grid.columns;store.pages.get(page).add(folder);ids.add(folder.id);}
            AutoGrid.enable(store,page,grid,context);
        }
        store.page=first;store.preferences.edit().putString("sort_undo_ids",android.text.TextUtils.join(",",ids)).putInt("sort_undo_page",first).putString("sort_undo_pages",android.text.TextUtils.join(",",pages)).apply();store.save();return first;
    }
    static boolean canUndo(LauncherStore store){return !store.preferences.getString("sort_undo_ids","").isEmpty();}
    static void undo(LauncherStore store){
        java.util.Set<String> ids=new java.util.HashSet<>(java.util.Arrays.asList(store.preferences.getString("sort_undo_ids","").split(",")));
        for(List<LauncherStore.Item> page:store.pages)page.removeIf(item->ids.contains(item.id));
        java.util.List<Integer> generated=new ArrayList<>();for(String raw:store.preferences.getString("sort_undo_pages",Integer.toString(store.preferences.getInt("sort_undo_page",-1))).split(","))try{generated.add(Integer.parseInt(raw));}catch(NumberFormatException ignored){}
        generated.sort(java.util.Collections.reverseOrder());store.preferences.edit().remove("sort_undo_ids").remove("sort_undo_page").remove("sort_undo_pages").apply();
        for(int page:generated)if(page>=0&&page<store.pageCount()&&store.emptyPage(page)&&store.pageCount()>1)store.removeEmptyPage(page);
        store.page=Math.min(store.page,store.pageCount()-1);store.save();
    }
    android.view.View preview(android.content.Context context,AppCatalog catalog,java.util.function.IntFunction<FittedGrid> choose,int width,int height){return new android.view.View(context){
        final android.graphics.Paint paint=new android.graphics.Paint(3);
        @Override protected void onDraw(android.graphics.Canvas c){super.onDraw(c);int count=Math.min(folders.size(),choose.apply(folders.size()).capacity);FittedGrid grid=choose.apply(count);float scale=Math.min(getWidth()/(float)Math.max(1,width),getHeight()/(float)Math.max(1,height));float cw=width*scale/grid.columns,ch=grid.pitch*scale,left=(getWidth()-width*scale)/2,top=Ui.dp(context,24)*scale;
            for(int n=0;n<count;n++){LauncherStore.Item folder=folders.get(n);float x=left+(n%grid.columns+.5f)*cw,y=top+(n/grid.columns)*ch;float size=Math.min(cw*.7f,ch*.65f);android.graphics.drawable.Drawable icon=catalog.icon(folder);icon.setBounds(Math.round(x-size/2),Math.round(y),Math.round(x+size/2),Math.round(y+size));icon.draw(c);paint.setColor(Ui.TEXT);paint.setTextAlign(android.graphics.Paint.Align.CENTER);paint.setTextSize(Math.min(Ui.dp(context,11),cw/7));Ui.drawMenuLabel(c,folder.label,x,y+size+2,cw-4,ch-size-2,paint.getTextSize(),Ui.TEXT);}
        }
    };}
}
