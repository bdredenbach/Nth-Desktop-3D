package com.nth.desktop3d;

import android.appwidget.*;
import android.content.*;
import android.os.*;
import android.util.Base64;
import org.json.*;
import java.util.*;
import java.io.*;
import java.security.MessageDigest;

/** Self-contained desktop backups. Parse and validate the entire document before applying anything. */
final class DesktopBackup {
    static final String FORMAT="nth-desktop-3d-backup";
    static final int MAX_BYTES=16*1024*1024;
    private static final String[] GEOMETRY={"cell_x","cell_y","span_x","span_y"};
    static String install(Context c){SharedPreferences p=c.getSharedPreferences("desktop3d",0);String id=p.getString("backup_install_id","");if(id.isEmpty()){id=UUID.randomUUID().toString();p.edit().putString("backup_install_id",id).apply();}return id;}
    static String digest(byte[] bytes)throws IOException {try{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();}catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}}
    static JSONObject capture(Context c,SettingsModel model)throws Exception {
        LauncherStore s=model.store;JSONArray pages=new JSONArray(),widgets=new JSONArray();AppWidgetManager manager=AppWidgetManager.getInstance(c);
        for(int p=0;p<s.pageCount();p++){
            JSONArray items=new JSONArray();for(LauncherStore.Item item:s.pages.get(p))items.put(item.json());
            pages.put(new JSONObject().put("name",s.pageName(p)).put("pattern",s.preferences.getBoolean("folder_pattern_page_"+p,false)).put("items",items));
            for(String raw:s.preferences.getString("widget_ids_page_"+p,"").split(","))try{
                int id=Integer.parseInt(raw);AppWidgetProviderInfo info=manager.getAppWidgetInfo(id);
                String provider=info==null?s.preferences.getString("widget_provider_"+id,""):info.provider.flattenToString();
                long user=info==null?s.preferences.getLong("widget_user_"+id,0):c.getSystemService(android.os.UserManager.class).getSerialNumberForUser(info.getProfile());
                JSONObject widget=new JSONObject().put("token",UUID.randomUUID().toString()).put("id",id).put("page",p).put("provider",provider).put("user",user).put("label",info==null?"Widget":java.util.Objects.toString(info.loadLabel(c.getPackageManager()),"Widget")).put("padding",s.preferences.getBoolean("widget_padding_"+id,false));
                for(String key:GEOMETRY)widget.put(key,s.preferences.getInt("widget_"+key+"_"+id,key.startsWith("span")?2:0));widgets.put(widget);
            }catch(NumberFormatException ignored){}
        }
        JSONArray pending=new JSONArray(s.preferences.getString("restore_widgets","[]"));for(int i=0;i<pending.length();i++)widgets.put(pending.getJSONObject(i));
        JSONObject out=new JSONObject().put("format",FORMAT).put("version",2).put("created",System.currentTimeMillis()).put("install",install(c)).put("settings",model.exportSettings()).put("pages",pages).put("page",s.page).put("home",s.homePage()).put("note",s.preferences.getString("note","")).put("widgets",widgets);
        byte[] photo=WallpaperStore.bytes(c);if(photo!=null)out.put("wallpaper",new JSONObject().put("sha256",digest(photo)).put("data",Base64.encodeToString(photo,Base64.NO_WRAP)));
        if(out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>MAX_BYTES)throw new IOException("Desktop backup is too large");return out;
    }
    static final class Prepared {
        final JSONObject settings;final List<List<LauncherStore.Item>> pages=new ArrayList<>();final JSONArray pageData,widgets;
        final int page,home;final String note,origin;final byte[] photo;int folders,shortcuts;
        Prepared(JSONObject input,SettingsModel model)throws Exception {
            if(!FORMAT.equals(input.optString("format"))||input.optInt("version")!=2)throw new JSONException("Not a supported desktop backup");
            settings=input.getJSONObject("settings");validateSettings(settings,model);
            pageData=input.getJSONArray("pages");if(pageData.length()<1||pageData.length()>LauncherStore.MAX_PAGES)throw new JSONException("Invalid desktop count");
            page=integer(input,"page",0,pageData.length()-1);home=integer(input,"home",0,pageData.length()-1);origin=text(input,"install",200);note=text(input,"note",1024*1024);
            Set<String> ids=new HashSet<>();int[] total={0};
            for(int n=0;n<pageData.length();n++){
                JSONObject desktop=pageData.getJSONObject(n);text(desktop,"name",200);if(!(desktop.get("pattern") instanceof Boolean))throw new JSONException("Invalid desktop layout");
                JSONArray items=desktop.getJSONArray("items");if(items.length()>60)throw new JSONException("Too many desktop shortcuts");List<LauncherStore.Item> entries=new ArrayList<>();
                for(int i=0;i<items.length();i++){JSONObject raw=items.getJSONObject(i);validateItem(raw,0,ids,total);entries.add(LauncherStore.Item.read(raw,0));count(raw);}pages.add(entries);
            }
            widgets=input.getJSONArray("widgets");if(widgets.length()>300)throw new JSONException("Too many widgets");Set<String> tokens=new HashSet<>();
            for(int i=0;i<widgets.length();i++){
                JSONObject w=widgets.getJSONObject(i);integer(w,"id",-1,Integer.MAX_VALUE);integer(w,"page",0,pages.size()-1);String provider=text(w,"provider",1000);if(!provider.isEmpty()&&ComponentName.unflattenFromString(provider)==null)throw new JSONException("Invalid widget provider");text(w,"label",512);String token=text(w,"token",200);if(!tokens.add(token))throw new JSONException("Duplicate widget record");
                integer(w,"cell_x",0,7);integer(w,"cell_y",0,119);integer(w,"span_x",1,8);integer(w,"span_y",1,12);if(!(w.get("padding") instanceof Boolean)||!(w.get("user") instanceof Number))throw new JSONException("Invalid widget placement");
            }
            if(input.has("wallpaper")&&!(input.get("wallpaper") instanceof JSONObject))throw new JSONException("Invalid wallpaper record");JSONObject wallpaper=input.optJSONObject("wallpaper");if(wallpaper!=null){String encoded=text(wallpaper,"data",3*1024*1024);photo=Base64.decode(encoded,Base64.DEFAULT);if(!digest(photo).equals(text(wallpaper,"sha256",64)))throw new JSONException("Wallpaper checksum failed");WallpaperStore.validate(photo);}else photo=null;
        }
        private void count(JSONObject item)throws JSONException {if("folder".equals(item.getString("key")))folders++;else shortcuts++;JSONArray children=item.getJSONArray("children");for(int i=0;i<children.length();i++)count(children.getJSONObject(i));}
        String summary(){return pages.size()+" desktops, "+folders+" folders, "+shortcuts+" shortcuts"+(widgets.length()>0?", "+widgets.length()+" widget placements":"")+".\n\nIncludes launcher settings, notes and the selected photo wallpaper. Android widgets may need permission again after reinstalling. Current desktop contents will be replaced. Undo is available.";}
        void apply(Context c,SettingsModel model)throws Exception {
            LauncherStore s=model.store;Map<String,?> previousPreferences=s.preferences.getAll();JSONObject values=settings.getJSONObject("values");JSONArray oldIds=new JSONArray();
            for(String key:s.preferences.getAll().keySet())if(key.startsWith("widget_ids_page_"))for(String id:s.preferences.getString(key,"").split(","))try{oldIds.put(Integer.parseInt(id));}catch(NumberFormatException ignored){}
            byte[] previousPhoto=WallpaperStore.bytes(c);WallpaperStore.write(c,photo);
            // All values and layout records have already been checked. No Activity or app is launched from a backup.
            Map<SettingsModel.Spec,Integer> oldValues=new LinkedHashMap<>();for(Iterator<String> it=values.keys();it.hasNext();){String key=it.next();if(key.equals("cloud_ai"))continue;SettingsModel.Spec spec=model.specs.get(key);oldValues.put(spec,spec.get.getAsInt());}for(SettingsModel.Spec spec:oldValues.keySet())spec.put.accept(values.getInt(spec.id));if(values.has("wallpaper_source"))model.specs.get("wallpaper_source").put.accept(values.getInt("wallpaper_source"));
            SharedPreferences.Editor edit=s.preferences.edit();for(String key:s.preferences.getAll().keySet())if(key.startsWith("desktop_name_")||key.startsWith("folder_pattern_page_")||key.startsWith("widget_"))edit.remove(key);
            JSONArray layout=new JSONArray();for(int p=0;p<pages.size();p++){JSONArray items=new JSONArray();for(LauncherStore.Item item:pages.get(p))items.put(item.json());layout.put(items);JSONObject d=pageData.getJSONObject(p);edit.putString("desktop_name_"+p,d.getString("name")).putBoolean("folder_pattern_page_"+p,d.getBoolean("pattern"));}
            JSONObject targets=settings.optJSONObject("gestureTargets");for(String[] gesture:GestureAssignments.GESTURES)edit.remove(gesture[0]+"_target");if(targets!=null)for(Iterator<String> it=targets.keys();it.hasNext();){String key=it.next();edit.putString(key+"_target",targets.getString(key));}
            AppWidgetManager manager=AppWidgetManager.getInstance(c);Set<Integer> reused=new HashSet<>();Map<Integer,List<Integer>> perPage=new HashMap<>();JSONArray pending=new JSONArray();
            for(int i=0;i<widgets.length();i++){
                JSONObject w=widgets.getJSONObject(i);int id=w.getInt("id");AppWidgetProviderInfo info=null;try{if(origin.equals(install(c))&&id>=0)info=manager.getAppWidgetInfo(id);}catch(RuntimeException ignored){}
                long user=info==null?-1:c.getSystemService(android.os.UserManager.class).getSerialNumberForUser(info.getProfile());
                if(info!=null&&info.provider.flattenToString().equals(w.getString("provider"))&&user==w.getLong("user")&&reused.add(id)){
                    int p=w.getInt("page");perPage.computeIfAbsent(p,key->new ArrayList<>()).add(id);geometry(edit,id,w);
                }else pending.put(w);
            }
            for(Map.Entry<Integer,List<Integer>> entry:perPage.entrySet()){StringBuilder ids=new StringBuilder();for(int id:entry.getValue()){if(ids.length()>0)ids.append(',');ids.append(id);}edit.putString("widget_ids_page_"+entry.getKey(),ids.toString());}
            edit.putString("layout",layout.toString()).putInt("page_count",pages.size()).putInt("page",page).putInt("home_page",home).putString("note",note).putBoolean("seeded",true).putBoolean("appsSeeded",true).putString("restore_widgets",pending.toString()).putBoolean("padding_expands_073",true).remove("sort_undo_page").remove("pending_widget_restore");
            if(!edit.commit()){restorePreferences(s.preferences,previousPreferences);for(Map.Entry<SettingsModel.Spec,Integer> entry:oldValues.entrySet())entry.getKey().put.accept(entry.getValue());if(oldValues.containsKey(model.specs.get("wallpaper_source")))model.specs.get("wallpaper_source").put.accept(oldValues.get(model.specs.get("wallpaper_source")));WallpaperStore.write(c,previousPhoto);throw new IOException("Could not save restored desktops");}
            s.pages.clear();s.pages.addAll(pages);s.page=page;s.save();
            LauncherWidgetHost host=new LauncherWidgetHost(c);for(int i=0;i<oldIds.length();i++){int id=oldIds.getInt(i);if(!reused.contains(id))try{host.deleteAppWidgetId(id);}catch(RuntimeException ignored){}}
        }
    }
    private static void restorePreferences(SharedPreferences p,Map<String,?> values){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?> entry:values.entrySet()){String k=entry.getKey();Object v=entry.getValue();if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Float)e.putFloat(k,(Float)v);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);}e.commit();}
    static void geometry(SharedPreferences.Editor edit,int id,JSONObject w)throws JSONException {for(String key:GEOMETRY)edit.putInt("widget_"+key+"_"+id,w.getInt(key));edit.putBoolean("widget_padding_"+id,w.getBoolean("padding")).putString("widget_provider_"+id,w.getString("provider")).putLong("widget_user_"+id,w.getLong("user"));}
    static int integer(JSONObject j,String key,int min,int max)throws JSONException {Object value=j.get(key);if(!(value instanceof Number)||!Double.isFinite(((Number)value).doubleValue())||((Number)value).doubleValue()!=((Number)value).longValue()||((Number)value).longValue()<min||((Number)value).longValue()>max)throw new JSONException("Invalid "+key);return ((Number)value).intValue();}
    static String text(JSONObject j,String key,int max)throws JSONException {Object value=j.get(key);if(!(value instanceof String)||((String)value).length()>max)throw new JSONException("Invalid "+key);return (String)value;}
    private static void validateItem(JSONObject item,int depth,Set<String> ids,int[] total)throws JSONException {
        if(++total[0]>20000||depth>2)throw new JSONException("Too many nested shortcuts");String id=text(item,"id",200);if(id.isEmpty()||!ids.add(id))throw new JSONException("Duplicate shortcut identity");String key=text(item,"key",2048);if(key.isEmpty())throw new JSONException("Empty shortcut");text(item,"label",512);integer(item,"cellX",-1,7);integer(item,"cellY",-1,119);
        for(String name:new String[]{"x","y"}){Object value=item.get(name);if(!(value instanceof Number)||!Double.isFinite(((Number)value).doubleValue())||((Number)value).doubleValue()< -1||((Number)value).doubleValue()>1)throw new JSONException("Invalid shortcut position");}
        if(!(item.get("fixed") instanceof Boolean))throw new JSONException("Invalid shortcut state");JSONArray children=item.getJSONArray("children");if(children.length()>1000||children.length()>0&&!key.equals("folder"))throw new JSONException("Invalid folder contents");for(int i=0;i<children.length();i++)validateItem(children.getJSONObject(i),depth+1,ids,total);
        if(item.has("shortcutPackage")){text(item,"shortcutPackage",300);text(item,"shortcutId",1000);if(!(item.get("shortcutUser") instanceof Number))throw new JSONException("Invalid shortcut profile");}
    }
    static void validateSettings(JSONObject settings,SettingsModel model)throws JSONException {
        if(!"nth-desktop-3d-settings".equals(settings.optString("format"))||settings.optInt("version")!=1)throw new JSONException("Unsupported settings record");JSONObject values=settings.getJSONObject("values");
        for(Iterator<String> it=values.keys();it.hasNext();){String key=it.next();if(key.equals("cloud_ai"))continue;SettingsModel.Spec spec=model.specs.get(key);if(spec==null)throw new JSONException("Unknown setting "+key);integer(values,key,spec.min,spec.max);}
        if(settings.has("gestureTargets")&&!(settings.get("gestureTargets") instanceof JSONObject))throw new JSONException("Invalid gesture targets");JSONObject targets=settings.optJSONObject("gestureTargets");if(targets!=null)for(Iterator<String> it=targets.keys();it.hasNext();){String key=it.next();if(!key.startsWith("gesture_")||!model.specs.containsKey(key))throw new JSONException("Invalid gesture");text(targets,key,2048);}
    }
    private DesktopBackup(){}
}
