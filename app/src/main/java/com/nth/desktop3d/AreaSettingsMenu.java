package com.nth.desktop3d;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Contextual shortcuts into the shared settings registry; labels always wrap. */
final class AreaSettingsMenu {
    interface Host {void page(String id);void setting(String id);void setup();default void action(String id){}}
    private final Context context;private final SettingsModel model;private final String area;private final Host host;
    AreaSettingsMenu(Context context,SettingsModel model,String area,Host host){this.context=context;this.model=model;this.area=area;this.host=host;}
    static String area(String title){
        switch(title){
            case "Setup & permissions":case " ":return null;
            case "3D settings":return "root";
            case "Manage desktops":case "Desktop settings":case "Desktop shortcuts":case "Auto pick and sort":return "home";
            case "Your apps":return "drawer";
            case "Widgets":return "widgets";
            case "Scene studio":return "motion";
            case "Scene creator":return "creator";
            case "Live wallpapers":return "wallpaper";
            case "Dock spaces":case "Dock space":case "Dock widgets":return "dock";
            case "Crystal cards":return "cards";
            case "Google Discover":case "Discover connection":case "Nth Feed":return "feed";
            case "Device controls":return "controls";
            case "Nth Desktop 3D":return "about";
            default:return "look";
        }
    }
    static String title(String area){
        if(area==null)return "Area";
        if(area.equals("folders"))return "Folder";
        if(area.equals("widgets"))return "Widget";
        if(area.equals("creator"))return "Scene creator";
        if(area.equals("controls"))return "Device controls";
        if(area.equals("root"))return "All";
        for(String[] page:SettingsStudio.PAGES)if(page[0].equals(area))return page[1];
        return "Area";
    }
    boolean includes(SettingsModel.Spec spec){
        if(area.equals("creator"))return spec.id.equals("reduced_motion")||spec.id.equals("tilt")||spec.id.equals("transparency");
        if(area.equals("widgets"))return spec.id.equals("grid_columns")||spec.id.equals("grid_row_dp")||spec.id.equals("lock_layout");
        if(area.equals("controls"))return spec.id.equals("tilt")||spec.id.equals("depth")||spec.id.equals("strength")||spec.id.equals("motion");
        if(area.equals("folders")&&spec.id.equals("folder_animation"))return true;
        if(area.equals("motion")&&spec.id.equals("transition"))return true;
        if(area.equals("drawer")&&(spec.id.equals("search_folders")||spec.id.equals("search_keyboard")))return true;
        if(area.equals("home")&&(spec.id.equals("home_rotation")||spec.id.equals("transition")))return true;
        return spec.page.equals(area);
    }
    AlertDialog show(){
        LinearLayout list=Ui.column(context);ScrollView scroll=new ScrollView(context);scroll.addView(list);
        LinearLayout surface=Ui.column(context);surface.setPadding(Ui.dp(context,12),Ui.dp(context,12),Ui.dp(context,12),Ui.dp(context,16));
        AlertDialog dialog=new AlertDialog.Builder(context).create();dialog.setView(surface,0,0,0,0);LinearLayout heading=Ui.row(context);android.widget.TextView label=Ui.text(context,title(area)+" settings",20,Ui.TEXT);label.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));heading.addView(Ui.icon(context,"close","Close "+title(area)+" settings",dialog::dismiss),new LinearLayout.LayoutParams(Ui.dp(context,46),Ui.dp(context,46)));surface.addView(heading);surface.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(area.equals("root"))for(String[] page:SettingsStudio.PAGES)add(list,dialog,page[1],page[2],()->host.page(page[0]));
        else{
            for(SettingsModel.Spec spec:model.specs.values())if(includes(spec))add(list,dialog,spec.title,spec.value(),()->host.setting(spec.id));
            if(area.equals("creator")){add(list,dialog,"Object transparency","Choose an object, then adjust its transparency",()->host.action("opacity"));add(list,dialog,"Caustic lighting","Intensity, speed and parallax depth",()->host.action("caustics"));add(list,dialog,"Dancing light shafts","Top-left, top-right or both corners",()->host.action("shafts"));}
            if(area.equals("motion")||area.equals("wallpaper")){add(list,dialog,"Create or edit a scene","Background, objects, transparency and caustics",()->host.action("scene"));if(LayeredScene.file(context,false).exists())add(list,dialog,"Remove my scene","Remove your saved custom scene",()->host.action("remove_scene"));}
            if(area.equals("dock"))add(list,dialog,"Manage dock spaces","Choose apps, folders and widgets for each space",()->host.action("dock"));
            if(area.equals("home")){add(list,dialog,"Desktop gestures","Choose swipe, tap and button actions",()->host.page("gestures"));add(list,dialog,"Dock settings","Visibility, spacing and material",()->host.page("dock"));}
            if(area.equals("widgets"))list.addView(Ui.text(context,"Hold a widget for its resize, padding and configuration controls.",14,Ui.MUTED));
            if(area.equals("about")||area.equals("controls"))add(list,dialog,"Setup & permissions","Default Home and optional Android access",host::setup);
            add(list,dialog,"All settings","Browse launcher settings",()->host.page("root"));
        }
        if(context instanceof MainActivity)((MainActivity)context).presentAreaMenu(dialog,surface,title(area)+" settings");else Ui.themeDialog(dialog);return dialog;
    }
    private void add(LinearLayout list,AlertDialog dialog,String label,String detail,Runnable action){Button button=Ui.quiet(context,label+"\n"+detail,()->{dialog.dismiss();action.run();});button.setContentDescription(label);button.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);list.addView(button,new LinearLayout.LayoutParams(-1,-2));}
}
