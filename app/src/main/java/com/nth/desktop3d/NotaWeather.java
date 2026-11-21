package com.nth.desktop3d;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.text.InputFilter;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import java.util.function.Consumer;

/** One current-weather turn. Place choices and device coordinates stay outside the general chat gateway. */
final class NotaWeather {
    static final int PERMISSION=3093;
    private final Activity activity;
    private final SharedPreferences preferences;
    private final NotaChatView chat;
    private final NotaSettingsClient cloud;
    private final WeatherLocation location;
    private final Consumer<LiveAnswer> complete;
    private boolean closed, waitingPermission;
    private AlertDialog editor;

    NotaWeather(Activity activity, SharedPreferences preferences, NotaChatView chat,
                NotaSettingsClient cloud, Consumer<LiveAnswer> complete) {
        this.activity=activity; this.preferences=preferences; this.chat=chat; this.cloud=cloud; this.complete=complete;
        location=new WeatherLocation(activity);
    }
    boolean waitingPermission() { return waitingPermission; }
    private boolean active() { return !closed && chat.busy() && preferences.getBoolean("cloud_ai",false); }
    void start(String city) {
        if(!city.isEmpty()) lookup(CurrentWeather.Place.city(city));
        else if(location.permitted()) locate();
        else choices("Where should I check?");
    }
    private void lookup(CurrentWeather.Place place) {
        if(!active()) return;
        chat.suggestions.removeAllViews();
        chat.progress("Checking current conditions with Open-Meteo…");
        cloud.weather(place,location::permitted,answer->{if(active()) complete.accept(answer);});
    }
    private void locate() {
        if(!active()) return;
        chat.suggestions.removeAllViews();
        chat.progress("Checking your approximate location…");
        location.locate(place->{
            if(!active()) return;
            if(place!=null) lookup(place);
            else choices("I couldn’t get a recent location from this phone. Where should I check?");
        });
    }
    private void choices(String question) {
        if(!active()) return;
        chat.suggestions.removeAllViews();
        chat.progress(question+" Open-Meteo uses the place you choose to look up current weather.");
        action("Use approximate location",()->{
            if(location.permitted()) locate();
            else {
                waitingPermission=true;
                activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION);
            }
        });
        action("Enter a city",this::enterCity);
        String saved=preferences.getString("card_weather_city","").trim();
        if(!saved.isEmpty()) action("Use "+saved,()->lookup(CurrentWeather.Place.city(saved)));
    }
    void permissionResult() {
        if(!waitingPermission) return;
        waitingPermission=false;
        if(location.permitted()) locate(); else choices("Location access is off. You can still check a city.");
    }
    private void action(String text,Runnable select) {
        Button b=NotaDesign.action(activity,text,()->{if(active()) select.run();});
        b.setTextColor(NotaDesign.TEXT);
        b.setBackground(Ui.touch(activity,NotaDesign.surface(activity,NotaDesign.SURFACE,14),14));
        android.widget.LinearLayout.LayoutParams lp=new android.widget.LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin=Ui.dp(activity,6); chat.suggestions.addView(b,lp);
    }
    private void enterCity() {
        EditText input=new EditText(activity); input.setSingleLine(true); input.setHint("City, state or country");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        editor=new AlertDialog.Builder(activity).setTitle("Weather for which city?").setView(input)
                .setNegativeButton("Cancel",null).setPositiveButton("Check weather",null).create();
        AlertDialog dialog=editor;
        dialog.setOnDismissListener(d->{if(editor==dialog) editor=null;});
        Ui.themeDialog(dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String city=input.getText().toString().trim();
            if(city.isEmpty()) { input.setError("Enter a city"); return; }
            dialog.dismiss(); lookup(CurrentWeather.Place.city(city));
        });
    }
    void close() {
        closed=true; waitingPermission=false; location.cancel();
        if(editor!=null) editor.dismiss();
    }
}
