package com.nth.desktop3d;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Dock contents have independent identities and survive hidden/reduced spaces. */
final class DockStore {
    static final int MAX=5;
    static final class Slot {
        boolean blank;LauncherStore.Item item;int widgetId=-1;String provider="",label="";long user;
        boolean widget(){return !provider.isEmpty();}
        JSONObject json()throws JSONException{JSONObject j=new JSONObject();if(blank)j.put("empty",true);else if(item!=null)j.put("item",item.json());else if(widget())j.put("widgetId",widgetId).put("provider",provider).put("user",user).put("label",label);return j;}
    }
    final List<Slot> slots=new ArrayList<>();private final SharedPreferences prefs;
    DockStore(SharedPreferences p){prefs=p;load();}
    void load(){slots.clear();for(int i=0;i<MAX;i++)slots.add(new Slot());try{JSONArray raw=new JSONArray(prefs.getString("dock_slots","[]"));for(int i=0;i<Math.min(MAX,raw.length());i++)slots.set(i,read(raw.getJSONObject(i)));}catch(JSONException ignored){}}
    static Slot read(JSONObject j)throws JSONException{Slot s=new Slot();if(j.has("empty")){if(!(j.get("empty") instanceof Boolean))throw new JSONException("Invalid empty dock space");s.blank=j.getBoolean("empty");if(s.blank&&(j.has("item")||j.has("provider")))throw new JSONException("Conflicting empty dock space");}if(j.has("item"))s.item=LauncherStore.Item.read(j.getJSONObject("item"),0);else if(j.has("provider")){s.provider=j.getString("provider");s.widgetId=j.getInt("widgetId");s.user=j.getLong("user");s.label=j.getString("label");}return s;}
    JSONArray json()throws JSONException{JSONArray a=new JSONArray();for(Slot slot:slots)a.put(slot.json());return a;}
    void save(){try{prefs.edit().putString("dock_slots",json().toString()).apply();}catch(JSONException e){throw new IllegalStateException(e);}}
    void clear(int index){slots.set(index,new Slot());save();}
    void leaveEmpty(int index){Slot slot=new Slot();slot.blank=true;slots.set(index,slot);save();}
    void item(int index,LauncherStore.Item source){Slot s=new Slot();s.item=copy(source);slots.set(index,s);save();}
    static LauncherStore.Item copy(LauncherStore.Item source){LauncherStore.Item out=source.copy();for(LauncherStore.Item child:source.children)out.children.add(copy(child));return out;}
    void widget(int index,int id,String provider,long user,String label){Slot s=new Slot();s.widgetId=id;s.provider=provider;s.user=user;s.label=label;slots.set(index,s);save();}
    int widgetSlot(int id){if(id<0)return -1;for(int i=0;i<MAX;i++)if(slots.get(i).widget()&&slots.get(i).widgetId==id)return i;return -1;}
    List<LauncherStore.Item> items(){List<LauncherStore.Item> out=new ArrayList<>();for(Slot s:slots)if(s.item!=null)out.add(s.item);return out;}
}
