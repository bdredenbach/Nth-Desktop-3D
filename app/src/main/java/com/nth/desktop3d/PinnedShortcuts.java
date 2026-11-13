package com.nth.desktop3d;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Store Android shortcut identities, never replace them with a generic app launch intent. */
final class PinnedShortcuts {
    private final MainActivity activity;
    private final LauncherStore store;
    private final LauncherApps launcher;
    private final UserManager users;
    private final Runnable changed;
    private final Map<String,Drawable> icons=new HashMap<>();
    private AlertDialog confirmation;
    PinnedShortcuts(MainActivity activity,LauncherStore store,LauncherApps launcher,Runnable changed){this.activity=activity;this.store=store;this.launcher=launcher;this.changed=changed;users=activity.getSystemService(UserManager.class);}
    static boolean isShortcut(LauncherStore.Item item){return item.shortcutPackage!=null&&item.shortcutId!=null;}
    boolean handle(Intent intent){
        if(intent==null||!LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(intent.getAction()))return false;
        if(confirmation!=null)confirmation.dismiss();
        LauncherApps.PinItemRequest request;
        try{request=launcher.getPinItemRequest(intent);}catch(RuntimeException invalid){request=null;}
        if(request==null||request.getRequestType()!=LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT||!request.isValid()){activity.toast("This shortcut request has expired. Add it again from its app.");clearIntent();return true;}
        ShortcutInfo info=request.getShortcutInfo();if(info==null){clearIntent();return true;}
        final LauncherApps.PinItemRequest accepted=request;final int page=store.page;
        LinearLayout body=Ui.column(activity);Ui.pad(body,20);
        ImageView icon=new ImageView(activity);Drawable drawable=null;
        try{drawable=launcher.getShortcutBadgedIconDrawable(info,activity.getResources().getDisplayMetrics().densityDpi);}catch(RuntimeException ignored){}
        icon.setImageDrawable(drawable==null?activity.getPackageManager().getDefaultActivityIcon():drawable);body.addView(icon,new LinearLayout.LayoutParams(Ui.dp(activity,56),Ui.dp(activity,56)));
        EditText name=new EditText(activity);name.setSingleLine(true);name.setText(info.getShortLabel());name.setContentDescription("Shortcut name");name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(60)});body.addView(name,new LinearLayout.LayoutParams(-1,-2));
        body.addView(Ui.text(activity,"Desktop "+(page+1),14,Ui.MUTED));
        confirmation=new AlertDialog.Builder(activity).setTitle("Add shortcut to desktop?").setView(body).setNegativeButton("Cancel",null).setPositiveButton("Add",null).create();
        confirmation.setOnDismissListener(d->{confirmation=null;clearIntent();});
        confirmation.setOnShowListener(d->confirmation.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String label=name.getText().toString().trim();if(label.isEmpty()){name.setError("Enter a shortcut name");return;}
            try{
                long serial=users.getSerialNumberForUser(info.getUserHandle());String key=identity(info.getPackage(),info.getId(),serial);
                LauncherStore.Item existing=find(key);if(existing==null&&store.pages.get(page).size()>=60){activity.toast("That desktop is full. Choose another desktop and add the shortcut again.");return;}
                if(!accepted.isValid()||!accepted.accept()){activity.toast("This shortcut request expired. Please add it again.");confirmation.dismiss();return;}
                LauncherStore.Item item=existing==null?new LauncherStore.Item(key,label):existing;
                item.shortcutPackage=info.getPackage();item.shortcutId=info.getId();item.shortcutUser=serial;item.label=label;
                saveIcon(info,key);if(existing==null)store.pages.get(page).add(item);
                store.page=existing==null?page:pageOf(existing);store.save();changed.run();
                activity.toast(existing==null?"Shortcut added to Desktop "+(store.page+1):"Shortcut updated on Desktop "+(store.page+1));confirmation.dismiss();
            }catch(RuntimeException error){activity.toast("The shortcut could not be added. Set Nth Desktop 3D as your default Home app and try again.");}
        }));confirmation.show();return true;
    }
    private void clearIntent(){activity.setIntent(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));}
    void open(LauncherStore.Item item){
        UserHandle user=users.getUserForSerialNumber(item.shortcutUser);
        try{if(user==null)throw new IllegalStateException("Profile unavailable");launcher.startShortcut(item.shortcutPackage,item.shortcutId,null,null,user);}
        catch(RuntimeException unavailable){activity.toast("This shortcut is unavailable. Enable its app or profile, or add it again from the app.");}
    }
    Drawable icon(LauncherStore.Item item){
        if(!isShortcut(item))return null;
        if(icons.containsKey(item.key))return icons.get(item.key);
        Drawable icon=Drawable.createFromPath(iconFile(item.key).getAbsolutePath());icons.put(item.key,icon);return icon;
    }
    private void saveIcon(ShortcutInfo info,String key){
        try{
            Drawable icon=launcher.getShortcutBadgedIconDrawable(info,activity.getResources().getDisplayMetrics().densityDpi);if(icon==null)return;
            int size=Ui.dp(activity,64);Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);android.graphics.Rect previous=icon.copyBounds();icon.setBounds(0,0,size,size);icon.draw(new Canvas(bitmap));icon.setBounds(previous);
            try(FileOutputStream output=new FileOutputStream(iconFile(key))){bitmap.compress(Bitmap.CompressFormat.PNG,100,output);}finally{bitmap.recycle();}
            icons.remove(key);
        }catch(java.io.IOException|RuntimeException unavailable){android.util.Log.w("NthShortcuts","Shortcut icon unavailable",unavailable);}
    }
    void refresh(String packageName,List<ShortcutInfo> shortcuts){for(ShortcutInfo info:shortcuts){String key=identity(packageName,info.getId(),users.getSerialNumberForUser(info.getUserHandle()));if(find(key)!=null)saveIcon(info,key);}changed.run();}
    void syncRemoved(){
        // Each launcher owns its own pinned set. Preserve references in folders and other desktops.
        Map<String,LauncherStore.Item> all=new HashMap<>();for(List<LauncherStore.Item> page:store.pages)for(LauncherStore.Item item:page)collect(item,all);
        for(UserHandle user:users.getUserProfiles())try{
            List<ShortcutInfo> pinned=launcher.getShortcuts(new LauncherApps.ShortcutQuery().setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED),user);
            if(pinned==null)continue;Map<String,List<String>> packages=new HashMap<>();
            for(ShortcutInfo info:pinned){List<String> ids=packages.computeIfAbsent(info.getPackage(),p->new ArrayList<>());if(all.containsKey(identity(info.getPackage(),info.getId(),users.getSerialNumberForUser(user))))ids.add(info.getId());}
            for(Map.Entry<String,List<String>> entry:packages.entrySet())launcher.pinShortcuts(entry.getKey(),entry.getValue(),user);
        }catch(RuntimeException unavailable){/* Locked profile or Home changed. */}
    }
    private void collect(LauncherStore.Item item,Map<String,LauncherStore.Item> result){if(isShortcut(item))result.put(item.key,item);for(LauncherStore.Item child:item.children)collect(child,result);}
    private LauncherStore.Item find(String key){Map<String,LauncherStore.Item> all=new HashMap<>();for(List<LauncherStore.Item> page:store.pages)for(LauncherStore.Item item:page)collect(item,all);return all.get(key);}
    private int pageOf(LauncherStore.Item target){for(int p=0;p<store.pages.size();p++)for(LauncherStore.Item item:store.pages.get(p))if(item==target||item.children.contains(target))return p;return store.page;}
    private File iconFile(String key){
        try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));StringBuilder name=new StringBuilder();for(byte b:hash)name.append(String.format(java.util.Locale.ROOT,"%02x",b&255));File dir=new File(activity.getFilesDir(),"shortcut-icons");if(!dir.isDirectory())dir.mkdirs();return new File(dir,name+".png");}catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static String identity(String pkg,String id,long user){return "shortcut:"+pkg+":"+android.net.Uri.encode(id)+"@"+user;}
}
