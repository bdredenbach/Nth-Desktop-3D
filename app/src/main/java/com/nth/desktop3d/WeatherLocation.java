package com.nth.desktop3d;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** One foreground weather fix. Coordinates leave this class rounded to two decimal places. */
final class WeatherLocation {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private LocationListener listener;
    private Consumer<CurrentWeather.Place> callback;
    private final Runnable timeout = () -> finish(null);
    WeatherLocation(Context context) { this.context=context; }

    boolean permitted() {
        return context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private boolean recent(Location fix) {
        if(fix==null) return false;
        long age=SystemClock.elapsedRealtimeNanos()-fix.getElapsedRealtimeNanos();
        return age>=0 && age<=TimeUnit.MINUTES.toNanos(15);
    }
    private void accept(Location fix) {
        if(!permitted() || !recent(fix)) return;
        finish(CurrentWeather.Place.nearby(fix.getLatitude(),fix.getLongitude()));
    }
    @SuppressLint("MissingPermission")
    void locate(Consumer<CurrentWeather.Place> result) {
        cancel(); callback=result;
        if(!permitted()) { finish(null); return; }
        LocationManager manager=context.getSystemService(LocationManager.class);
        try {
            if (manager==null || Build.VERSION.SDK_INT>=28 && !manager.isLocationEnabled()) { finish(null); return; }
            Location newest=null;
            for (String provider:manager.getProviders(true)) {
                try {
                    Location fix=manager.getLastKnownLocation(provider);
                    if(recent(fix) && (newest==null || fix.getElapsedRealtimeNanos()>newest.getElapsedRealtimeNanos())) newest=fix;
                } catch(SecurityException ignored) { /* Some providers require precise access. */ }
            }
            if(newest!=null) { accept(newest); return; }
            listener=new LocationListener() {
                @Override public void onLocationChanged(Location fix) { if(listener==this) accept(fix); }
                @Override public void onProviderEnabled(String p) {}
                @Override public void onProviderDisabled(String p) {}
                @Override public void onStatusChanged(String p,int s,Bundle extras) {}
            };
            boolean registered=false;
            for(String provider:new String[]{"network","fused","gps"}) {
                if(!manager.getProviders(true).contains(provider)) continue;
                try {
                    manager.requestLocationUpdates(provider,0L,0f,listener,main.getLooper());
                    registered=true;
                    break;
                } catch(SecurityException ignored) { /* Try the next enabled provider. */ }
            }
            if(registered) main.postDelayed(timeout,10000); else finish(null);
        } catch(RuntimeException unavailable) { finish(null); }
    }
    private void finish(CurrentWeather.Place place) {
        Consumer<CurrentWeather.Place> done=callback;
        cancel();
        if(done!=null) done.accept(place);
    }
    void cancel() {
        main.removeCallbacks(timeout); callback=null;
        if(listener!=null) {
            LocationManager manager=context.getSystemService(LocationManager.class);
            try { if(manager!=null) manager.removeUpdates(listener); } catch(RuntimeException ignored) {}
            listener=null;
        }
    }
}
