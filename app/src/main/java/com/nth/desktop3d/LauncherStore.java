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
    public static final String[] TRANSITIONS = {"Cube", "Carousel", "Fold"};
    public static final String[] MENUS = {"Lift", "Orbit", "Unfold"};
    public static final int PAGE_COUNT = 3;
    public final SharedPreferences preferences;
    public final List<List<Item>> pages = new ArrayList<>();
    public int mode, transition, menuStyle, page, fishCount;
    public float strength, fishSpeed;
    public boolean tilt, reducedMotion, shadows, ripples, ambient;
    public static final class Item {
        public String id = UUID.randomUUID().toString(), key, label;
        public float x = -1, y = -1;
        public int cellX=-1,cellY=-1;
        public boolean fixed;
        public final List<Item> children = new ArrayList<>();
        public Item(String key, String label) { this.key = key; this.label = label; }
        public boolean folder() { return "folder".equals(key); }
        JSONObject json() throws JSONException {
            JSONObject j = new JSONObject().put("id", id).put("key", key).put("label", label).put("x", x).put("y", y).put("cellX",cellX).put("cellY",cellY).put("fixed", fixed);
            JSONArray a = new JSONArray(); for (Item c : children) a.put(c.json()); return j.put("children", a);
        }
        static Item read(JSONObject j, int depth) throws JSONException {
            Item i = new Item(j.getString("key"), j.optString("label", "Shortcut"));
            i.id = j.optString("id", i.id); i.x = (float) j.optDouble("x", -1); i.y = (float) j.optDouble("y", -1); i.fixed = j.optBoolean("fixed");
            i.cellX=j.optInt("cellX",-1);i.cellY=j.optInt("cellY",-1);
            JSONArray a = j.optJSONArray("children");
            if (a != null && depth < 2) for (int n = 0; n < Math.min(a.length(), 100); n++) i.children.add(read(a.getJSONObject(n), depth + 1));
            return i;
        }
    }
    public LauncherStore(Context context) {
        preferences = context.getSharedPreferences("desktop3d", Context.MODE_PRIVATE);
        for (int n = 0; n < PAGE_COUNT; n++) pages.add(new ArrayList<>());
        mode = Math.max(0, Math.min(3, preferences.getInt("mode", 0)));
        transition = Math.max(0, Math.min(2, preferences.getInt("transition", 0)));
        menuStyle = Math.max(0, Math.min(2, preferences.getInt("menuStyle", 0)));
        page = SceneMath.wrap(preferences.getInt("page", 0), PAGE_COUNT);
        fishCount = Math.max(3, Math.min(7, preferences.getInt("fishCount", 5)));
        strength = SceneMath.clamp(preferences.getFloat("strength", .5f), 0, 1);
        fishSpeed = SceneMath.clamp(preferences.getFloat("fishSpeed", .6f), .2f, 1.5f);
        tilt = preferences.getBoolean("tilt", true); reducedMotion = preferences.getBoolean("reducedMotion", false);
        shadows = preferences.getBoolean("shadows", true); ripples = preferences.getBoolean("ripples", true); ambient = preferences.getBoolean("ambient", true);
        try {
            JSONArray all = new JSONArray(preferences.getString("layout", "[]"));
            for (int n = 0; n < Math.min(PAGE_COUNT, all.length()); n++) {
                JSONArray items = all.getJSONArray(n);
                for (int j = 0; j < Math.min(items.length(), 60); j++) pages.get(n).add(Item.read(items.getJSONObject(j), 0));
            }
        } catch (JSONException e) { for (List<Item> p : pages) p.clear(); }
        if (!preferences.getBoolean("seeded", false)) {
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
    public boolean add(int page, String key, String label) {
        List<Item> items = pages.get(SceneMath.wrap(page, PAGE_COUNT));
        for (Item i : items) if (i.key.equals(key) && !i.folder()) return false;
        if (items.size() >= 60) return false;
        items.add(new Item(key, label)); save(); return true;
    }
    public void remove(Item item) { for (List<Item> p : pages) { p.remove(item); for (Item i : p) i.children.remove(item); } save(); }
    public List<Item> folders() { List<Item> out = new ArrayList<>(); for (List<Item> p : pages) for (Item i : p) if (i.folder()) out.add(i); return out; }
    public void save() {
        JSONArray all = new JSONArray();
        try { for (List<Item> p : pages) { JSONArray a = new JSONArray(); for (Item i : p) a.put(i.json()); all.put(a); } }
        catch (JSONException e) { throw new IllegalStateException("Cannot serialize desktop", e); }
        preferences.edit().putString("layout", all.toString()).putInt("mode", mode).putInt("transition", transition)
            .putInt("menuStyle", menuStyle).putInt("page", page).putInt("fishCount", fishCount).putFloat("strength", strength)
            .putFloat("fishSpeed", fishSpeed).putBoolean("tilt", tilt).putBoolean("reducedMotion", reducedMotion)
            .putBoolean("shadows", shadows).putBoolean("ripples", ripples).putBoolean("ambient", ambient).apply();
    }
}
