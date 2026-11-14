package com.nth.desktop3d;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One canonical layout; visual mode changes never rewrite shortcut identities. */
public final class LauncherStore {
    public static final String[] MODES = {"Spatial Desktop", "Gravity Playground", "Holographic Depth", "Living Pond"};
    public static final String[] TRANSITIONS = PageTransitions.NAMES;
    public static final String[] MENUS = {"Lift", "Orbit", "Unfold"};
    public static final int PAGE_COUNT = 3, MAX_PAGES = 24;
    public final SharedPreferences preferences;
    public final List<List<Item>> pages = new ArrayList<>();
    public int mode, transition, menuStyle, page, fishCount;
    public float strength, fishSpeed;
    public boolean tilt, reducedMotion, shadows, ripples, ambient;
    public static final class Item {
        public String id = UUID.randomUUID().toString(), key, label;
        public String shortcutPackage,shortcutId;
        public long shortcutUser=-1;
        public float x = -1, y = -1;
        public int cellX=-1,cellY=-1;
        public boolean fixed;
        public final List<Item> children = new ArrayList<>();
        public Item(String key, String label) { this.key = key; this.label = label; }
        public Item copy(){Item item=new Item(key,label);item.shortcutPackage=shortcutPackage;item.shortcutId=shortcutId;item.shortcutUser=shortcutUser;return item;}
        public boolean folder() { return "folder".equals(key); }
        JSONObject json() throws JSONException {
            JSONObject j = new JSONObject().put("id", id).put("key", key).put("label", label).put("x", x).put("y", y).put("cellX",cellX).put("cellY",cellY).put("fixed", fixed);
            if(shortcutPackage!=null&&shortcutId!=null)j.put("shortcutPackage",shortcutPackage).put("shortcutId",shortcutId).put("shortcutUser",shortcutUser);
            JSONArray a = new JSONArray(); for (Item c : children) a.put(c.json()); return j.put("children", a);
        }
        static Item read(JSONObject j, int depth) throws JSONException {
            Item i = new Item(j.getString("key"), j.optString("label", "Shortcut"));
            i.id = j.optString("id", i.id); i.x = (float) j.optDouble("x", -1); i.y = (float) j.optDouble("y", -1); i.fixed = j.optBoolean("fixed");
            i.cellX=j.optInt("cellX",-1);i.cellY=j.optInt("cellY",-1);
            if(j.has("shortcutPackage")&&j.has("shortcutId")){i.shortcutPackage=j.getString("shortcutPackage");i.shortcutId=j.getString("shortcutId");i.shortcutUser=j.optLong("shortcutUser",-1);}
            JSONArray a = j.optJSONArray("children");
            if (a != null && depth < 2) for (int n = 0; n < Math.min(a.length(), 1000); n++) i.children.add(read(a.getJSONObject(n), depth + 1));
            return i;
        }
    }
    public LauncherStore(Context context) {
        preferences = context.getSharedPreferences("desktop3d", Context.MODE_PRIVATE);
        for (int n = 0; n < Math.max(1,Math.min(MAX_PAGES,preferences.getInt("page_count",PAGE_COUNT))); n++) pages.add(new ArrayList<>());
        mode = Math.max(0, Math.min(3, preferences.getInt("mode", 0)));
        transition = Math.max(0, Math.min(TRANSITIONS.length-1, preferences.getInt("transition", 0)));
        menuStyle = Math.max(0, Math.min(2, preferences.getInt("menuStyle", 0)));
        page = SceneMath.wrap(preferences.getInt("page", 0), pages.size());
        fishCount = Math.max(3, Math.min(7, preferences.getInt("fishCount", 5)));
        strength = SceneMath.clamp(preferences.getFloat("strength", .5f), 0, 1);
        fishSpeed = SceneMath.clamp(preferences.getFloat("fishSpeed", .6f), .2f, 1.5f);
        tilt = preferences.getBoolean("tilt", true); reducedMotion = preferences.getBoolean("reducedMotion", false);
        shadows = preferences.getBoolean("shadows", true); ripples = preferences.getBoolean("ripples", true); ambient = preferences.getBoolean("ambient", true);
        try {
            JSONArray all = new JSONArray(preferences.getString("layout", "[]"));
            for (int n = 0; n < Math.min(pages.size(), all.length()); n++) {
                JSONArray items = all.getJSONArray(n);
                for (int j = 0; j < Math.min(items.length(), 60); j++) pages.get(n).add(Item.read(items.getJSONObject(j), 0));
            }
        } catch (JSONException e) { for (List<Item> p : pages) p.clear(); }
        if (!preferences.getBoolean("seeded", false)) {
            while(pages.size()<PAGE_COUNT)pages.add(new ArrayList<>());
            pages.get(0).add(new Item("builtin:notes", "Nth Notes"));
            pages.get(0).add(new Item("builtin:calculator", "Calculator"));
            Item folder = new Item("folder", "Nth tools");
            folder.children.add(new Item("builtin:calendar", "Calendar"));
            folder.children.add(new Item("builtin:settings", "3D settings"));
            pages.get(0).add(folder);
            pages.get(1).add(new Item("builtin:modes", "Scene studio"));
            pages.get(2).add(new Item("builtin:about", "Welcome to 3D"));
            preferences.edit().putBoolean("seeded", true).apply(); save();
        }
    }
    public int pageCount(){return pages.size();}
    public int homePage(){return Math.max(0,Math.min(pages.size()-1,preferences.getInt("home_page",0)));}
    public void setHomePage(int index){if(index>=0&&index<pages.size())preferences.edit().putInt("home_page",index).apply();}
    public String pageName(int index){String name=preferences.getString("desktop_name_"+index,"");return name.isEmpty()?"Desktop "+(index+1):name;}
    public boolean emptyPage(int index){return index>=0&&index<pages.size()&&pages.get(index).isEmpty()&&preferences.getString("widget_ids_page_"+index,"").isEmpty();}
    public int appendPage(){if(pages.size()>=MAX_PAGES)return -1;int index=pages.size();preferences.edit().remove("desktop_name_"+index).remove("widget_ids_page_"+index).remove("folder_pattern_page_"+index).apply();pages.add(new ArrayList<>());save();return pages.size()-1;}
    public boolean movePage(int from,int to){
        if(from<0||to<0||from>=pages.size()||to>=pages.size()||from==to)return false;
        List<Integer> order=new ArrayList<>();for(int n=0;n<pages.size();n++)order.add(n);Integer moved=order.remove(from);order.add(to,moved);applyPageOrder(order);return true;
    }
    public boolean removeEmptyPage(int index){
        if(pages.size()<=1||!emptyPage(index))return false;
        List<Integer> order=new ArrayList<>();for(int n=0;n<pages.size();n++)if(n!=index)order.add(n);applyPageOrder(order);return true;
    }
    private void applyPageOrder(List<Integer> order){
        List<List<Item>> previous=new ArrayList<>(pages),next=new ArrayList<>();int home=homePage(),sorted=preferences.getInt("sort_undo_page",-1),oldPage=page;
        SharedPreferences.Editor edit=preferences.edit();for(int n=0;n<previous.size();n++)edit.remove("widget_ids_page_"+n).remove("folder_pattern_page_"+n).remove("desktop_name_"+n);
        for(int n=0;n<order.size();n++){int old=order.get(n);next.add(previous.get(old));edit.putString("widget_ids_page_"+n,preferences.getString("widget_ids_page_"+old,"")).putBoolean("folder_pattern_page_"+n,preferences.getBoolean("folder_pattern_page_"+old,false)).putString("desktop_name_"+n,preferences.getString("desktop_name_"+old,""));}
        page=order.indexOf(oldPage);if(page<0)page=Math.min(oldPage,order.size()-1);int newHome=order.indexOf(home);edit.putInt("home_page",newHome<0?Math.min(home,order.size()-1):newHome);
        int newSorted=order.indexOf(sorted);if(newSorted<0)edit.remove("sort_undo_page");else edit.putInt("sort_undo_page",newSorted);
        edit.apply();pages.clear();pages.addAll(next);save();
    }
    public boolean add(int page, String key, String label) {
        List<Item> items = pages.get(SceneMath.wrap(page, pages.size()));
        for (Item i : items) if (i.key.equals(key) && !i.folder()) return false;
        if (items.size() >= 60) return false;
        items.add(new Item(key, label)); save(); return true;
    }
    public void remove(Item item) { for (List<Item> p : pages) { p.remove(item); for (Item i : p) i.children.remove(item); } save(); }
    public List<Item> folders() { List<Item> out = new ArrayList<>(); for (List<Item> p : pages) for (Item i : p) if (i.folder()) out.add(i); return out; }
    public Item folderContaining(Item source){for(Item folder:folders())if(folder.children.contains(source))return folder;return null;}
    public boolean moveToFolder(Item source,Item destination){
        if(source==null||source.folder()||destination==null||!destination.folder()||!folders().contains(destination))return false;
        List<Item> owner=null;for(List<Item> p:pages){if(p.contains(source))owner=p;for(Item folder:p)if(folder.folder()&&folder.children.contains(source))owner=folder.children;}
        if(owner==null||owner==destination.children)return false;
        boolean duplicate=false;for(Item child:destination.children)if(child.key.equals(source.key)){duplicate=true;break;}
        if(!duplicate&&destination.children.size()>=1000)return false;
        owner.remove(source);if(!duplicate){source.x=source.y=-1;source.cellX=source.cellY=-1;destination.children.add(source);}save();return true;
    }
    public void save() {
        JSONArray all = new JSONArray();
        try { for (List<Item> p : pages) { JSONArray a = new JSONArray(); for (Item i : p) a.put(i.json()); all.put(a); } }
        catch (JSONException e) { throw new IllegalStateException("Cannot serialize desktop", e); }
        preferences.edit().putString("layout", all.toString()).putInt("mode", mode).putInt("transition", transition)
            .putInt("page_count", pages.size()).putInt("menuStyle", menuStyle).putInt("page", page).putInt("fishCount", fishCount).putFloat("strength", strength)
            .putFloat("fishSpeed", fishSpeed).putBoolean("tilt", tilt).putBoolean("reducedMotion", reducedMotion)
            .putBoolean("shadows", shadows).putBoolean("ripples", ripples).putBoolean("ambient", ambient).apply();
    }
}
