package com.nth.desktop3d;

import android.content.Context;
import android.content.SharedPreferences;

/** Independent persisted preferences; existing launcher layouts and scene modes remain untouched. */
public final class CrystalPreferences {
    private final SharedPreferences prefs;
    public CrystalPreferences(Context context) {
        prefs = context.getSharedPreferences("desktop3d_crystal", Context.MODE_PRIVATE);
        if(!prefs.getBoolean("google_default_070",false)){android.content.SharedPreferences.Editor edit=prefs.edit().putBoolean("google_default_070",true).putBoolean("native_feed_migration",true);if(!prefs.contains("feed_provider")||prefs.getInt("feed_provider",0)==2)edit.putInt("feed_provider",0);edit.apply();}
    }
    private int percent(String key,int fallback){return Math.max(0,Math.min(100,prefs.getInt(key,fallback)));}
    private void put(String key,int value){prefs.edit().putInt(key,value).apply();}
    private void put(String key,boolean value){prefs.edit().putBoolean(key,value).apply();}
    public int depth(){return percent("depth",60);}
    public void setDepth(int v){put("depth",Math.max(0,Math.min(100,v)));}
    public int transparency(){return percent("transparency",72);}
    public void setTransparency(int v){put("transparency",Math.max(0,Math.min(100,v)));}
    public int motion(){return percent("motion",35);}
    public void setMotion(int v){put("motion",Math.max(0,Math.min(100,v)));}
    public boolean parallax(){return prefs.getBoolean("parallax",true);}
    public void setParallax(boolean v){put("parallax",v);}
    public boolean shadows(){return prefs.getBoolean("shadows",true);}
    public void setShadows(boolean v){put("shadows",v);}
    public int qualityIndex(){return Math.max(0,Math.min(2,prefs.getInt("quality",1)));}
    public String quality(){return new String[]{"Performance","Balanced","Cinematic"}[qualityIndex()];}
    public void setQuality(int v){put("quality",Math.max(0,Math.min(2,v)));}
    public static final String[] MATERIALS={"Crystal Glass","Frosted Crystal","Prism Crystal","Deep Crystal"};
    public static final String[] MENU_ANIMATIONS={"Rising Cards","Radial Arc","Floating Quick Actions","3D Stack","Minimal Context"};
    public static final String[] LIGHTING={"Subtle","Dynamic","Prismatic"};
    public int material(){return Math.max(0,Math.min(3,prefs.getInt("material",2)));}
    public void setMaterial(int value){put("material",Math.max(0,Math.min(3,value)));}
    public int menuAnimation(){return Math.max(0,Math.min(4,prefs.getInt("menu_animation",0)));}
    public void setMenuAnimation(int value){put("menu_animation",Math.max(0,Math.min(4,value)));}
    public int lighting(){return Math.max(0,Math.min(2,prefs.getInt("lighting",1)));}
    public void setLighting(int value){put("lighting",Math.max(0,Math.min(2,value)));}
    public boolean dynamicLighting(){return lighting()!=0;}
    public boolean feedEnabled(){return prefs.getBoolean("feed_enabled",true);}
    public void setFeedEnabled(boolean v){put("feed_enabled",v);}
    public int feedProvider(){return Math.max(0,Math.min(3,prefs.getInt("feed_provider",0)));}
    public void setFeedProvider(int v){put("feed_provider",Math.max(0,Math.min(3,v)));}
    public String feedProviderLabel(){return new String[]{"Google Discover","Google App","Nth Feed (built in)","Off"}[feedProvider()];}
    public boolean feedEdgeSwipe(){return prefs.getBoolean("feed_edge_swipe",true);}
    public void setFeedEdgeSwipe(boolean v){put("feed_edge_swipe",v);}
    public boolean feed3dTransition(){return prefs.getBoolean("feed_3d_transition",false);}
    public void setFeed3dTransition(boolean v){put("feed_3d_transition",v);}
}
