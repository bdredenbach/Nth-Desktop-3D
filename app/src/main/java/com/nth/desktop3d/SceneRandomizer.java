package com.nth.desktop3d;

import java.util.*;

/** Foreground-only scene cycling; no alarm, service or background wakeup. */
final class SceneRandomizer {
    static final int ALL=(1<<11)-1;
    static final String[] INTERVALS={"15 minutes","30 minutes","1 hour","3 hours","6 hours","24 hours"};
    static final long[] MINUTES={15,30,60,180,360,1440};
    private final LauncherStore store;private final Random random;
    SceneRandomizer(LauncherStore store){this(store,new Random());}SceneRandomizer(LauncherStore store,Random random){this.store=store;this.random=random;}
    long interval(){return MINUTES[Math.max(0,Math.min(MINUTES.length-1,store.preferences.getInt("shuffle_interval",2)))]*60_000L;}
    int mask(){int mask=store.preferences.getInt("shuffle_scenes",ALL)&ALL;return mask==0?1<<store.mode:mask;}
    private String configuration(){return store.preferences.getBoolean("shuffle_enabled",false)+":"+mask()+":"+interval()+":"+store.preferences.getInt("wallpaper_source",0)+":"+store.mode;}
    void sync(long now){String config=configuration();long due=store.preferences.getLong("shuffle_due",0);if(!config.equals(store.preferences.getString("shuffle_configuration",""))||due<=0||due>now+interval()*2)store.preferences.edit().putString("shuffle_configuration",config).putLong("shuffle_due",now+interval()).remove("shuffle_bag").apply();}
    boolean due(long now,boolean eligible){sync(now);return eligible&&store.preferences.getBoolean("shuffle_enabled",false)&&store.preferences.getInt("wallpaper_source",0)==0&&now>=store.preferences.getLong("shuffle_due",Long.MAX_VALUE);}
    int choose(long now){sync(now);List<Integer> bag=new ArrayList<>();for(String raw:store.preferences.getString("shuffle_bag","").split(","))try{bag.add(Integer.parseInt(raw));}catch(NumberFormatException ignored){}ShuffleBag.Draw draw=ShuffleBag.next(mask(),LauncherStore.MODES.length,store.mode,bag,random);StringBuilder remaining=new StringBuilder();for(int scene:draw.remaining){if(remaining.length()>0)remaining.append(',');remaining.append(scene);}store.mode=draw.scene;store.preferences.edit().putInt("wallpaper_source",0).putString("shuffle_bag",remaining.toString()).putLong("shuffle_due",now+interval()).apply();store.preferences.edit().putString("shuffle_configuration",configuration()).apply();store.save();return draw.scene;}
    static void reset(LauncherStore store){store.preferences.edit().remove("shuffle_configuration").remove("shuffle_bag").remove("shuffle_due").apply();}
}
