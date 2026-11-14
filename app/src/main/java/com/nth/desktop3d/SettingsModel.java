package com.nth.desktop3d;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import org.json.JSONObject;

/** One registry powers native controls, live previews, offline commands and settings backups. */
final class SettingsModel {
    static final int[] TEXT_COLORS={Ui.TEXT,0xFFFFFFFF,0xFFFFE7B1,0xFFACF3E5};
    interface Changed {void apply(boolean rebuild);}
    static final class Spec {
        final String id,page,title,help;final int min,max,step;final String[] options;final boolean toggle,rebuild;final IntSupplier get;final IntConsumer put;
        Spec(String id,String page,String title,String help,int min,int max,int step,String[] options,boolean toggle,boolean rebuild,IntSupplier get,IntConsumer put){this.id=id;this.page=page;this.title=title;this.help=help;this.min=min;this.max=max;this.step=step;this.options=options;this.toggle=toggle;this.rebuild=rebuild;this.get=get;this.put=put;}
        String value(){int v=get.getAsInt();return toggle?(v==1?"On":"Off"):options!=null?options[Math.max(0,Math.min(options.length-1,v))]:String.valueOf(v);}
    }
    final LauncherStore store;final CrystalPreferences crystal;final Map<String,Spec> specs=new LinkedHashMap<>();
    private final Changed changed;private Runnable undo,gestureUndo;private boolean gesture,pendingRebuild;String lastChange="";
    SettingsModel(LauncherStore s,CrystalPreferences c,Changed changed){store=s;crystal=c;this.changed=changed;
        pref("grid_columns","home","Desktop grid columns","Apps, folders and widgets share this grid.",3,8,1,4,true);
        pref("grid_row_dp","home","Grid row height","Vertical cell spacing in dp. Dense layouts remain scrollable.",88,144,4,112,true);
        pref("icon_size","home","Desktop icon size","Percentage of the standard icon size, constrained to its cell.",70,130,5,100,true);
        bool("desktop_labels","home","Desktop labels","Show shortcut and folder names.",true,true);
        pref("label_size","home","Label text size","Text size in sp, with system font scaling.",10,18,1,12,true);
        pref("desktop_padding","home","Desktop side padding","Space beside the workspace in dp.",0,24,2,0,true);
        bool("lock_layout","home","Lock desktop layout","Prevent dragging and resizing while keeping taps and menus available.",false,false);
        bool("infinite_scroll","home","Infinite desktop scrolling","Wrap between the first and last desktop.",false,false);
        bool("auto_add_apps","home","Add newly installed apps","Place a shortcut on the current desktop when an app is installed.",false,false);
        bool("show_dock","dock","Show dock","Reveal the Start, search, controls and clock dock.",false,true);
        bool("dock_background","dock","Dock background","Use the selected Crystal material behind the dock.",true,false);
        bool("dock_clock","dock","Dock clock","Show the clock in the dock.",true,false);
        pref("dock_padding","dock","Dock padding","Space inside the dock in dp.",4,14,1,7,false);
        pref("dock_radius","dock","Dock corner radius","Round the dock surface in dp.",0,36,2,24,false);
        pref("folder_columns","folders","Folder columns","Number of icon columns inside folders.",2,6,1,3,false);
        pref("folder_icon_size","folders","Folder icon size","Icon size inside a folder, as a percentage.",70,130,5,100,false);
        bool("folder_labels","folders","Folder labels","Show shortcut names inside folders.",true,false);
        bool("folder_title","folders","Show folder name","Show the folder's title above its contents.",true,false);
        pref("folder_radius","folders","Folder corner radius","Round the folder window in dp.",0,36,2,26,false);
        pref("drawer_columns","drawer","App drawer columns","Phone columns; landscape can fit two additional columns.",3,6,1,3,false);
        pref("drawer_icon_size","drawer","App drawer icon size","App icon size as a percentage.",70,130,5,100,false);
        bool("drawer_labels","drawer","App drawer labels","Show installed app names.",true,false);
        bool("search_folders","search","Search folders","Include matching desktop folders in app searches.",true,false);
        bool("search_keyboard","search","Open search keyboard","Focus the app search field when the drawer opens.",false,false);
        bool("cloud_ai","search","Cloud questions for Nth Nota","Send your question and current setting values to Nth Nota's cloud service. Local setting commands always work offline.",false,false);
        bool("cards_in_search","cards","Cards in app search","Show the Cards action beside filtered app results.",true,false);
        for(String[] type:CrystalCards.TYPES)bool("card_"+type[0],"cards",type[1],"Show this crystal card.",true,false);
        enumeration("home_rotation","look","Home screen rotation","Follow Android rotation, allow all device orientations, or keep a fixed orientation.",new String[]{"Follow system","Auto rotate","Portrait","Landscape"},0,false);
        bool("adaptive_glass","look","Fading menu backgrounds","Add opacity over desktop content and feather it to transparent in empty areas.",true,false);
        pref("glass_strength","look","Opacity over content","Maximum opacity percentage over icons, folders and widgets.",20,90,5,90,false);
        pref("glass_feather","look","Fade width","Soft edge width around background objects in dp.",12,80,4,80,false);
        pref("glass_empty","look","Empty-area opacity","Set to zero for an invisible menu background over open wallpaper.",0,25,1,0,false);
        enumeration("label_color","look","Label color","A readable light palette for desktop, folders and app drawer.",new String[]{"Ice","White","Warm","Mint"},0,true);
        add("transparency","look","Crystal transparency","Transparency of the fixed Crystal material when fading backgrounds are off.",0,100,1,null,false,false,c::transparency,c::setTransparency);
        add("material","look","Crystal material","Surface material for icons and the dock.",0,3,1,CrystalPreferences.MATERIALS,false,true,c::material,c::setMaterial);
        add("lighting","look","Lighting","Highlight intensity on Crystal surfaces.",0,2,1,CrystalPreferences.LIGHTING,false,true,c::lighting,c::setLighting);
        add("menu_animation","look","Menu opening animation","How menus rise from the desktop plane.",0,4,1,CrystalPreferences.MENU_ANIMATIONS,false,false,c::menuAnimation,c::setMenuAnimation);
        add("scene","motion","Scene","Choose the desktop environment.",0,3,1,LauncherStore.MODES,false,true,()->s.mode,v->s.mode=v);
        add("transition","motion","Desktop transition","Perspective animation when changing desktops.",0,LauncherStore.TRANSITIONS.length-1,1,LauncherStore.TRANSITIONS,false,false,()->s.transition,v->s.transition=v);
        add("tilt","motion","Tilt parallax","Use device tilt to move the desktop perspective.",0,1,1,null,true,false,()->s.tilt?1:0,v->{s.tilt=v==1;c.setParallax(v==1);});
        add("depth","motion","3D depth","Depth of the scene and layered elements.",0,100,1,null,false,false,c::depth,c::setDepth);
        add("strength","motion","Tilt strength","How strongly device tilt affects the desktop.",0,100,1,null,false,false,()->Math.round(s.strength*100),v->s.strength=v/100f);
        add("motion","motion","Motion intensity","Strength of ambient Crystal movement.",0,100,1,null,false,false,c::motion,c::setMotion);
        add("ambient","motion","Ambient animation","Animate the environment while Home is visible.",0,1,1,null,true,false,()->s.ambient?1:0,v->s.ambient=v==1);
        add("reduced_motion","motion","Reduced motion","Keep settings previews and navigation steady.",0,1,1,null,true,true,()->s.reducedMotion?1:0,v->s.reducedMotion=v==1);
        add("quality","motion","Rendering quality","Balance scene detail and battery use.",0,2,1,new String[]{"Performance","Balanced","Cinematic"},false,false,c::qualityIndex,c::setQuality);
        add("fish_count","motion","Fish count","Number of fish in Living Pond.",3,7,1,null,false,false,()->s.fishCount,v->s.fishCount=v);
        add("fish_speed","motion","Fish speed","Living Pond speed percentage.",20,150,5,null,false,false,()->Math.round(s.fishSpeed*100),v->s.fishSpeed=v/100f);
        add("ripples","motion","Water ripples","Respond to touches in Living Pond.",0,1,1,null,true,false,()->s.ripples?1:0,v->s.ripples=v==1);
        add("shadows","motion","Scene shadows","Draw fish and leaf shadows in Living Pond.",0,1,1,null,true,false,()->s.shadows?1:0,v->s.shadows=v==1);
        add("crystal_shadows","look","Icon shadows","Elevation shadows under Crystal icon surfaces.",0,1,1,null,true,true,()->c.shadows()?1:0,v->c.setShadows(v==1));
        enumeration("gesture_up","gestures","Swipe up","Action on empty desktop space.",new String[]{"App drawer","Settings","Nth Nota","Device controls","None"},0,false);
        enumeration("gesture_down","gestures","Swipe down","Applies to empty space; workspace scrolling keeps priority.",new String[]{"App drawer","Settings","Nth Nota","Device controls","None"},3,false);
        enumeration("gesture_double","gestures","Double tap","Double tap empty desktop space.",new String[]{"App drawer","Settings","Nth Nota","Device controls","None"},4,false);
        add("feed_enabled","feed","Enable feed","Open your selected feed from the first desktop's edge.",0,1,1,null,true,false,()->c.feedEnabled()?1:0,v->c.setFeedEnabled(v==1));
        add("feed_provider","feed","Feed provider","Google Discover opens inside the launcher using the Google app. Nth Feed remains available.",0,3,1,new String[]{"Google Discover","Google App","Nth Feed (built in)","Off"},false,false,c::feedProvider,c::setFeedProvider);
        add("feed_edge","feed","Feed edge swipe","Allow a right swipe on the first desktop to open the feed.",0,1,1,null,true,false,()->c.feedEdgeSwipe()?1:0,v->c.setFeedEdgeSwipe(v==1));
        add("feed_animation","feed","Animate feed opening","Animate Google overlay opening when supported.",0,1,1,null,true,false,()->c.feed3dTransition()?1:0,v->c.setFeed3dTransition(v==1));
    }
    private void add(String id,String page,String title,String help,int min,int max,int step,String[] options,boolean toggle,boolean rebuild,IntSupplier get,IntConsumer put){specs.put(id,new Spec(id,page,title,help,min,max,step,options,toggle,rebuild,get,put));}
    private void pref(String id,String page,String title,String help,int min,int max,int step,int fallback,boolean rebuild){add(id,page,title,help,min,max,step,null,false,rebuild,()->Math.max(min,Math.min(max,store.preferences.getInt(id,fallback))),v->store.preferences.edit().putInt(id,v).apply());}
    private void bool(String id,String page,String title,String help,boolean fallback,boolean rebuild){add(id,page,title,help,0,1,1,null,true,rebuild,()->store.preferences.getBoolean(id,fallback)?1:0,v->store.preferences.edit().putBoolean(id,v==1).apply());}
    private void enumeration(String id,String page,String title,String help,String[] options,int fallback,boolean rebuild){add(id,page,title,help,0,options.length-1,1,options,false,rebuild,()->Math.max(0,Math.min(options.length-1,store.preferences.getInt(id,fallback))),v->store.preferences.edit().putInt(id,v).apply());}
    private Runnable layoutUndo(){
        List<Runnable> restore=new ArrayList<>();for(List<LauncherStore.Item> page:store.pages)for(LauncherStore.Item item:page){int x=item.cellX,y=item.cellY;float px=item.x,py=item.y;restore.add(()->{item.cellX=x;item.cellY=y;item.x=px;item.y=py;});}
        Map<String,Object> widget=new LinkedHashMap<>();for(Map.Entry<String,?> entry:store.preferences.getAll().entrySet())if(widgetPosition(entry.getKey()))widget.put(entry.getKey(),entry.getValue());
        return ()->{for(Runnable task:restore)task.run();SharedPreferences.Editor edit=store.preferences.edit();for(String key:store.preferences.getAll().keySet())if(widgetPosition(key))edit.remove(key);for(Map.Entry<String,Object> e:widget.entrySet())if(e.getValue() instanceof Integer)edit.putInt(e.getKey(),(Integer)e.getValue());edit.apply();};
    }
    private boolean widgetPosition(String k){return k.startsWith("widget_cell_")||k.startsWith("widget_span_")||k.startsWith("widget_left_")||k.startsWith("widget_top_")||k.startsWith("widget_width_")||k.startsWith("widget_height_");}
    String set(String id,int value){Spec s=specs.get(id);if(s==null)return "That setting is not available.";if(value<s.min||value>s.max)return s.title+" must be between "+s.min+" and "+s.max+".";
        int old=s.get.getAsInt();if(old==value)return s.title+" is already "+s.value()+".";
        Runnable layout=layoutUndo();Runnable before=()->{s.put.accept(old);layout.run();store.save();changed.apply(true);};if(gesture&&gestureUndo==null)gestureUndo=before;s.put.accept(value);store.save();if(gesture&&s.rebuild)pendingRebuild=true;changed.apply(s.rebuild&&!gesture);undo=gesture?gestureUndo:before;lastChange=s.title+" set to "+s.value()+".";return lastChange;
    }
    void beginGesture(){gesture=true;gestureUndo=null;pendingRebuild=false;}
    void endGesture(){gesture=false;gestureUndo=null;if(pendingRebuild){pendingRebuild=false;changed.apply(true);}}
    boolean canUndo(){return undo!=null;}
    String undo(){if(undo==null)return "There is no setting change to undo.";Runnable previous=undo;undo=null;previous.run();return lastChange="Last setting change undone.";}
    JSONObject exportSettings()throws org.json.JSONException {JSONObject values=new JSONObject();for(Spec s:specs.values())if(!s.id.equals("cloud_ai"))values.put(s.id,s.get.getAsInt());return new JSONObject().put("format","nth-desktop-3d-settings").put("version",1).put("values",values);}
    void importSettings(JSONObject input)throws org.json.JSONException {
        if(!"nth-desktop-3d-settings".equals(input.optString("format"))||input.optInt("version")!=1)throw new org.json.JSONException("Not a supported 3D settings backup.");
        JSONObject values=input.getJSONObject("values");Map<Spec,Integer> next=new LinkedHashMap<>(),old=new LinkedHashMap<>();
        java.util.Iterator<String> keys=values.keys();while(keys.hasNext()){String key=keys.next();Spec s=specs.get(key);if(key.equals("cloud_ai"))continue;Object raw=values.get(key);if(s==null||!(raw instanceof Number)||((Number)raw).doubleValue()!=((Number)raw).intValue())throw new org.json.JSONException("Unsupported setting: "+key);int value=((Number)raw).intValue();if(value<s.min||value>s.max)throw new org.json.JSONException("Invalid value for "+s.title);next.put(s,value);old.put(s,s.get.getAsInt());}
        Runnable layout=layoutUndo();for(Map.Entry<Spec,Integer> entry:next.entrySet())entry.getKey().put.accept(entry.getValue());store.save();changed.apply(true);undo=()->{for(Map.Entry<Spec,Integer> entry:old.entrySet())entry.getKey().put.accept(entry.getValue());layout.run();store.save();changed.apply(true);};lastChange="Settings restored. Desktop items and notes were kept.";
    }
    int value(String id){return specs.get(id).get.getAsInt();}
    int labelColor(){return TEXT_COLORS[value("label_color")];}
    String context(){StringBuilder b=new StringBuilder();for(Spec s:specs.values())b.append(s.title).append(": ").append(s.value()).append(". ").append(s.help).append('\n');return b.toString();}
}
