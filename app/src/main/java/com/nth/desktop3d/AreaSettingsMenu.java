package com.nth.desktop3d;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Contextual shortcuts into the shared settings registry; labels always wrap. */
final class AreaSettingsMenu {
    interface Host {void page(String id);void setting(String id);void setup();}
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
        if(area.equals("controls"))return "Device controls";
        if(area.equals("root"))return "All";
        for(String[] page:SettingsStudio.PAGES)if(page[0].equals(area))return page[1];
        return "Area";
    }
    boolean includes(SettingsModel.Spec spec){
        if(area.equals("widgets"))return spec.id.equals("grid_columns")||spec.id.equals("grid_row_dp")||spec.id.equals("lock_layout");
        if(area.equals("controls"))return spec.id.equals("tilt")||spec.id.equals("depth")||spec.id.equals("strength")||spec.id.equals("motion");
        if(area.equals("drawer")&&(spec.id.equals("search_folders")||spec.id.equals("search_keyboard")))return true;
        if(area.equals("home")&&(spec.id.equals("home_rotation")||spec.id.equals("transition")))return true;
        return spec.page.equals(area);
    }
    AlertDialog show(){
        LinearLayout list=Ui.column(context);Ui.pad(list,12);ScrollView scroll=new ScrollView(context);scroll.addView(list);
        AlertDialog dialog=new AlertDialog.Builder(context).setTitle(title(area)+" settings").setView(scroll).setNegativeButton("Close",null).create();
        if(area.equals("root"))for(String[] page:SettingsStudio.PAGES)add(list,dialog,page[1],page[2],()->host.page(page[0]));
        else{
            for(SettingsModel.Spec spec:model.specs.values())if(includes(spec))add(list,dialog,spec.title,spec.value(),()->host.setting(spec.id));
            if(area.equals("home")){add(list,dialog,"Desktop gestures","Choose swipe, tap and button actions",()->host.page("gestures"));add(list,dialog,"Dock settings","Visibility, spacing and material",()->host.page("dock"));}
            if(area.equals("widgets"))list.addView(Ui.text(context,"Hold a widget for its resize, padding and configuration controls.",14,Ui.MUTED));
            if(area.equals("about")||area.equals("controls"))add(list,dialog,"Setup & permissions","Default Home and optional Android access",host::setup);
            add(list,dialog,"All settings","Browse launcher settings",()->host.page("root"));
        }
        dialog.show();if(dialog.getWindow()!=null)dialog.getWindow().setLayout(-1,Math.round(context.getResources().getDisplayMetrics().heightPixels*.72f));return dialog;
    }
    private void add(LinearLayout list,AlertDialog dialog,String label,String detail,Runnable action){Button button=Ui.quiet(context,label+"\n"+detail,()->{dialog.dismiss();action.run();});button.setContentDescription(label);button.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);list.addView(button,new LinearLayout.LayoutParams(-1,-2));}
}
