package com.nth.desktop3d;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import org.json.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.*;

/** Current weather uses its provider even when general web search is enabled. */
final class WeatherSmoke extends Trial093Support {
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-11-18T02:30:00Z"),ZoneOffset.UTC);
    private static final String HERE="What is the weather where I'm at currently?";
    private final Provider provider=new Provider();
    private NotaChatView chat;
    WeatherSmoke(Instrumentation test, MainActivity activity, String output) { super(test,activity,output); }
    private static final class Provider implements LiveQuestions.Transport {
        volatile int cities,weather;
        volatile boolean offline;
        volatile String forecastUrl="";
        public String get(String url) throws Exception {
            if(offline) throw new IOException("Offline fixture");
            if(url.startsWith("https://geocoding-api.open-meteo.com/")) {
                cities++;
                return "{\"results\":[{\"name\":\"Buffalo\",\"admin1\":\"Minnesota\",\"country_code\":\"US\",\"country\":\"United States\",\"latitude\":45,\"longitude\":-94},{\"name\":\"Buffalo\",\"admin1\":\"New York\",\"country_code\":\"US\",\"country\":\"United States\",\"latitude\":42.88,\"longitude\":-78.88}]}";
            }
            check(url.startsWith("https://api.open-meteo.com/"),"Weather reached a general chat/search provider");
            weather++; forecastUrl=url;
            return new JSONObject().put("timezone","America/New_York").put("current",new JSONObject()
                    .put("temperature_2m",10).put("weather_code",2).put("time",CLOCK.instant().minusSeconds(300).getEpochSecond())).toString();
        }
        public String post(String url,String json){throw new AssertionError("Weather was posted to a model");}
        public void cancel(){}
    }
    void run() throws Exception {
        check("".equals(CurrentWeather.cityInQuestion(HERE)),"Exact device question missed the weather route");
        check("Buffalo, NY".equals(CurrentWeather.cityInQuestion("What’s the weather in Buffalo, NY right now?")),"Explicit city lost");
        for(String input:new String[]{"Why does cold weather cause frost?","Forecast for tomorrow","What is the weather in Paris tomorrow?","Open the weather card","Write a poem about the weather"})
            check(CurrentWeather.cityInQuestion(input)==null,"Current conditions hijacked: "+input);
        SharedPreferences scratch=activity.getSharedPreferences("weather_test",0);scratch.edit().clear().commit();
        try { cache(scratch); } finally {scratch.edit().clear().commit();}
        main(()->{
            call(activity,"closePanel");
            store.preferences.edit().putBoolean("cloud_ai",false).putBoolean("card_weather_fahrenheit",true)
                    .putString("card_weather_city","Buffalo, NY").remove("weather_reports_v2").apply();
            call(activity,"showSettings","nota");
            SettingsStudio studio=(SettingsStudio)field(activity,"settingsStudio");
            ((NotaSettingsClient)field(studio,"cloud")).close();
            set(studio,"cloud",new NotaSettingsClient(store.preferences,studio::pause,allowed->provider,CLOCK));
            chat=(NotaChatView)field(studio,"chat"); chat.newChat();
        });
        send(HERE); check(!chat.busy() && provider.weather==0,"Cloud-off weather made a request");
        main(()->{model.set("cloud_ai",1);chat.syncCloud();});
        if(!SetupAccess.location(activity)) {
            send(HERE);
            check(button("Enter a city")!=null && button("Use approximate location")!=null && button("Use Buffalo, NY")!=null,"Missing weather choices");
            check(provider.weather==0,"Saved city was silently used as current location");
            click("Use Buffalo, NY");
        } else send("What's the weather in Buffalo, NY?");
        await(()->!chat.busy(),"City weather timed out");
        check(last().text.contains("Buffalo, New York") && last().text.contains("50 °F") && last().text.contains("Partly cloudy")
                && last().sources.get(0).url.equals(CurrentWeather.SOURCE),"Wrong city, unit, condition or source");
        check(provider.forecastUrl.contains("latitude=42.88") && chat.context().length()==0,"Wrong city or weather leaked into model history");
        capture("0100-nota-city-weather");
        int geocodes=provider.cities;
        LocationManager manager=activity.getSystemService(LocationManager.class);
        shell("appops set com.nth.desktop3d android:mock_location allow");
        try {
            manager.addTestProvider("network",false,false,false,false,true,true,true,Criteria.POWER_LOW,Criteria.ACCURACY_COARSE);
            manager.setTestProviderEnabled("network",true);
            Location fix=new Location("network"); fix.setLatitude(37.76);fix.setLongitude(-122.42);fix.setAccuracy(1500);
            fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());fix.setTime(System.currentTimeMillis());
            manager.setTestProviderLocation("network",fix);
            send(HERE);
            if(!SetupAccess.location(activity)) {click("Use approximate location"); systemClick("While using the app");}
            await(()->!chat.busy(),"Weather did not resume after location access");
            check(last().text.contains("Your approximate location") && last().sources.size()==1 && provider.cities==geocodes
                    && !provider.forecastUrl.contains("latitude=42.88") && chat.context().length()==0,"Current location reused a city or entered model context");
            capture("0100-nota-nearby-weather");
        } finally {
            try { manager.removeTestProvider("network"); } finally { shell("appops set com.nth.desktop3d android:mock_location default"); }
        }
    }
    private void cache(SharedPreferences prefs) {
        Provider api=new Provider(); CurrentWeather.Place city=CurrentWeather.Place.city("Buffalo, NY");
        prefs.edit().putBoolean("card_weather_fahrenheit",false).apply();
        LiveAnswer first=CurrentWeather.lookup(prefs,city,false,CLOCK,api,()->true);
        check(first.text.contains("10 °C")&&first.text.contains("As of")&&first.checkedAt==CLOCK.millis(),"Celsius or conditions time missing");
        api.offline=true;
        Clock later=Clock.offset(CLOCK,Duration.ofMinutes(50));
        LiveAnswer saved=CurrentWeather.lookup(prefs,city,false,later,api,()->true);
        check(saved.kind.equals("Saved weather") && saved.text.contains("Fetched") && saved.checkedAt==first.checkedAt,"Offline report presented as live");
        check(CurrentWeather.lookup(prefs,CurrentWeather.Place.city("Paris"),false,later,api,()->true).sources.isEmpty(),"Wrong place reused a cached report");
        check(CurrentWeather.lookup(prefs,city,false,Clock.offset(CLOCK,Duration.ofHours(5)),api,()->true).sources.isEmpty(),"Expired weather presented as current");
    }
    private LiveAnswer last(){
        NotaConversation.Chat current=((NotaConversation)field(chat,"conversations")).current();
        return current.turns.get(current.turns.size()-1).answer;
    }
    private void send(String text){main(()->{chat.question.setText(text);chat.send.performClick();});}
    private void shell(String cmd)throws Exception{
        try(ParcelFileDescriptor fd=test.getUiAutomation().executeShellCommand(cmd);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){
            byte[] data=new byte[512];while(in.read(data)!=-1){}
        }
    }
}
