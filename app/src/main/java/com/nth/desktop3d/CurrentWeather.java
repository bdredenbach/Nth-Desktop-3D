package com.nth.desktop3d;

import android.content.SharedPreferences;
import android.net.Uri;
import org.json.*;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.regex.*;

/** Shared current conditions for Nota and the weather card. Temperatures are stored in Celsius. */
final class CurrentWeather {
    static final String SOURCE = "https://open-meteo.com/";
    private static final long MINUTE = 60_000, FRESH = 15 * MINUTE, MAX_AGE = 3 * 60 * MINUTE;
    private static final String CACHE = "weather_reports_v2";
    private static final Pattern QUESTION = Pattern.compile(
            "(?iu)^(?:(?:what(?: is|'s)|how(?: is|'s)|tell me|show me|check|give me)(?: the| my| today's)? )?"
            + "(?:(?:current |local )?(?:weather|temperature|current conditions)(?: like)?"
            + "|how (?:hot|cold|warm) is it|is it (?:raining|snowing)|what(?: is|'s) it like outside)"
            + "(?: (.*))?$");
    private static final Pattern HERE = Pattern.compile("(?iu)^(?:(?:right now|now|currently|today|outside|out|here|near me|around me|in my area|"
            + "at my (?:current )?location|in my (?:current )?location|where (?:i am|i'm)(?: at)?)(?: |$))+$");
    static String cityInQuestion(String input) {
        String q = input.trim().replace('’', '\'').replaceAll("[?!.]+$", "").replaceAll("\\s+", " ").trim();
        Matcher m = QUESTION.matcher(q);
        if (!m.matches()) return null;
        String tail = m.group(1);
        if (tail == null || HERE.matcher(tail).matches()) return "";
        String city = tail.replaceFirst("(?iu)^(?:(?:right now|today|currently|outside) )?(?:in|for|at|near) ", "");
        if (city.equals(tail)) return null;
        city = city.replaceFirst("(?iu) (?:right now|now|today|currently)$", "").trim();
        if (city.matches("(?iu).*(?:\\b(?:tomorrow|tonight|yesterday|week|weekend|monday|tuesday|wednesday|thursday|friday|saturday|sunday|versus|compared|and)\\b).*")
                || !city.matches("[\\p{L}\\p{M}\\p{N} .,'’–-]{1,100}")) return null;
        return city;
    }
    static final class Place {
        final String city, key;
        final double lat, lon;
        private Place(String city, String key, double lat, double lon) {
            this.city = city; this.key = key; this.lat = lat; this.lon = lon;
        }
        static Place city(String value) {
            String name = value.trim();
            return new Place(name, "city:" + name.toLowerCase(Locale.ROOT), Double.NaN, Double.NaN);
        }
        static Place nearby(double lat, double lon) {
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat)>90 || Math.abs(lon)>180) throw new IllegalArgumentException("Invalid location");
            double y = Math.rint(lat*100)/100, x = Math.rint(lon*100)/100;
            return new Place("", "area:" + y + "," + x, y, x);
        }
        boolean nearby() { return city.isEmpty() && Double.isFinite(lat); }
    }
    private static JSONObject report(SharedPreferences prefs, String key) {
        try { return new JSONObject(prefs.getString(CACHE,"{}")).optJSONObject(key); }
        catch (JSONException e) { return null; }
    }
    private static synchronized void remember(SharedPreferences prefs, String key, JSONObject current) throws JSONException {
        JSONObject entries;
        try { entries = new JSONObject(prefs.getString(CACHE,"{}")); } catch (JSONException e) { entries = new JSONObject(); }
        JSONObject prior = entries.optJSONObject(key);
        if (prior != null && prior.optLong("time") > current.getLong("time")) return;
        entries.put(key,current);
        if (entries.length()>8) {
            String oldest=null; long minimum=Long.MAX_VALUE;
            for (Iterator<String> it=entries.keys();it.hasNext();) {
                String k=it.next(); long saved=entries.getJSONObject(k).optLong("saved");
                if (!k.equals(key) && saved<minimum) { minimum=saved; oldest=k; }
            }
            if (oldest!=null) entries.remove(oldest);
        }
        prefs.edit().putString(CACHE,entries.toString()).apply();
    }
    private static boolean recent(JSONObject r, long now, long window) {
        if (r==null) return false;
        long age=now-r.optLong("saved"), readingAge=now-r.optLong("time");
        return age>=0 && age<=window && readingAge>=-5*MINUTE && readingAge<=Math.min(MAX_AGE,window+30*MINUTE)
                && Double.isFinite(r.optDouble("temp")) && !r.optString("name").isEmpty();
    }
    static LiveAnswer lookup(SharedPreferences prefs, Place place, boolean refresh, Clock clock,
                             LiveQuestions.Transport http, BooleanSupplier allowed) {
        check(allowed);
        long now=clock.millis(); JSONObject cached=report(prefs,place.key);
        if (!refresh && recent(cached,now,FRESH)) return format(prefs,cached,false);
        try {
            double lat=place.lat, lon=place.lon; String name="Your approximate location";
            if (!place.nearby()) {
                String[] parts=place.city.split("\\s*,\\s*");
                String url="https://geocoding-api.open-meteo.com/v1/search?count=20&language=en&format=json&name="+Uri.encode(parts[0]);
                JSONArray choices=new JSONObject(http.get(url)).optJSONArray("results");
                JSONObject match=match(choices,parts);
                if (match==null) return LiveAnswer.plain("I couldn’t find “"+place.city+"”. Try the city with its state, region, or country.","City not found");
                lat=match.getDouble("latitude");lon=match.getDouble("longitude");
                LinkedHashSet<String> names=new LinkedHashSet<>(Arrays.asList(match.getString("name"),match.optString("admin1"),match.optString("country")));
                names.remove("");name=String.join(", ",names);
            }
            check(allowed);
            if (!Double.isFinite(lat)||!Double.isFinite(lon)||Math.abs(lat)>90||Math.abs(lon)>180) throw new IOException("Invalid coordinates");
            JSONObject response=new JSONObject(http.get("https://api.open-meteo.com/v1/forecast?latitude="+lat+"&longitude="+lon
                    +"&current=temperature_2m,weather_code&temperature_unit=celsius&timezone=auto&timeformat=unixtime"));
            check(allowed); JSONObject conditions=response.getJSONObject("current");
            long time=Math.multiplyExact(conditions.getLong("time"),1000L);
            long fetched=clock.millis();
            if (time>fetched+5*MINUTE || time<fetched-60*MINUTE || conditions.isNull("temperature_2m") || conditions.isNull("weather_code"))
                throw new IOException("Current conditions unavailable");
            String zone=ZoneId.of(response.getString("timezone")).getId();
            JSONObject data=new JSONObject().put("name",name).put("time",time).put("saved",fetched).put("zone",zone)
                    .put("temp",conditions.getDouble("temperature_2m")).put("code",conditions.getInt("weather_code"));
            remember(prefs,place.key,data);
            return format(prefs,data,false);
        } catch (CancellationException canceled) { throw canceled; }
        catch (Exception unavailable) {
            check(allowed);
            long failedAt=clock.millis();
            if (recent(cached,failedAt,MAX_AGE)) return format(prefs,cached,true);
            LiveAnswer legacy=previousCityReport(prefs,place,failedAt);
            if (legacy!=null) return legacy;
            return LiveAnswer.plain("I couldn’t get current conditions from Open-Meteo. There’s no recent report for this location. Please try again shortly.","Weather unavailable");
        }
    }
    // Older card builds saved the fetch time only and used one generic key for all device locations.
    private static LiveAnswer previousCityReport(SharedPreferences prefs,Place place,long now) {
        if (place.nearby()) return null;
        try {
            JSONObject old=new JSONObject(prefs.getString("card_weather_cache","{}"));
            long saved=old.getLong("saved");
            if(!place.city.equalsIgnoreCase(old.getString("query")) || now<saved || now-saved>MAX_AGE) return null;
            String text="Open-Meteo is unavailable. Last saved report\n"+old.getString("name")+"\n"
                    +temperature(prefs,old.getDouble("temp"))+" · "+description(old.getInt("code"))
                    +"\nFetched "+java.text.DateFormat.getDateTimeInstance().format(new Date(saved))+". The conditions timestamp wasn’t saved.";
            return new LiveAnswer(text,"Saved weather",saved,Collections.singletonList(source()),false);
        } catch(JSONException invalid) { return null; }
    }
    private static LiveAnswer.Source source() {
        return new LiveAnswer.Source("Open-Meteo weather",SOURCE,"","Current conditions");
    }
    private static void check(BooleanSupplier allowed) { if (!allowed.getAsBoolean()) throw new CancellationException(); }
    private static JSONObject match(JSONArray choices,String[] parts) throws JSONException {
        if (choices==null) return null;
        for (int i=0;i<choices.length();i++) {
            JSONObject p=choices.getJSONObject(i);
            Set<String> qualifiers=new HashSet<>();
            for(String key:new String[]{"admin1","admin2","country","country_code"}) qualifiers.add(p.optString(key).toLowerCase(Locale.ROOT));
            if ("US".equals(p.optString("country_code"))) qualifiers.add(US_STATES.get(p.optString("admin1")));
            if ("US".equals(p.optString("country_code"))) qualifiers.add("usa");
            if ("GB".equals(p.optString("country_code"))) qualifiers.add("uk");
            boolean fits=true;
            for(int n=1;n<parts.length;n++) fits &= qualifiers.contains(parts[n].toLowerCase(Locale.ROOT));
            if(fits) return p;
        }
        return null;
    }
    private static final Map<String,String> US_STATES=new HashMap<>();
    static {
        String[] states=("Alabama|Alaska|Arizona|Arkansas|California|Colorado|Connecticut|Delaware|District of Columbia|Florida|Georgia|Hawaii|Idaho|Illinois|Indiana|Iowa|Kansas|Kentucky|Louisiana|Maine|Maryland|Massachusetts|Michigan|Minnesota|Mississippi|Missouri|Montana|Nebraska|Nevada|New Hampshire|New Jersey|New Mexico|New York|North Carolina|North Dakota|Ohio|Oklahoma|Oregon|Pennsylvania|Rhode Island|South Carolina|South Dakota|Tennessee|Texas|Utah|Vermont|Virginia|Washington|West Virginia|Wisconsin|Wyoming|Puerto Rico").split("\\|");
        String[] codes="al ak az ar ca co ct de dc fl ga hi id il in ia ks ky la me md ma mi mn ms mo mt ne nv nh nj nm ny nc nd oh ok or pa ri sc sd tn tx ut vt va wa wv wi wy pr".split(" ");
        for(int i=0;i<states.length;i++) US_STATES.put(states[i],codes[i]);
    }
    private static LiveAnswer format(SharedPreferences prefs,JSONObject r,boolean offline) {
        String display=temperature(prefs,r.optDouble("temp"));
        ZoneId zone;
        try { zone=ZoneId.of(r.optString("zone")); } catch (DateTimeException invalid) { zone=ZoneId.systemDefault(); }
        DateTimeFormatter stamp=DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(zone);
        long reading=r.optLong("time"), saved=r.optLong("saved");
        String text=r.optString("name")+"\n"+display+" · "+description(r.optInt("code",-1))+"\nAs of "+stamp.format(Instant.ofEpochMilli(reading))+" ("+zone.getId()+")";
        if (offline) text="Open-Meteo is unavailable. Last saved report\n"+text+"\nFetched "+stamp.format(Instant.ofEpochMilli(saved))+".";
        return new LiveAnswer(text,offline?"Saved weather":"Current weather",saved,
                Collections.singletonList(source()),false);
    }
    private static String temperature(SharedPreferences prefs,double temp) {
        boolean fahrenheit=prefs.getBoolean("card_weather_fahrenheit","US".equals(Locale.getDefault().getCountry()));
        return Math.round(fahrenheit?temp*9/5+32:temp)+(fahrenheit?" °F":" °C");
    }
    static String description(int code) {
        switch(code) {
            case 0:return "Clear sky"; case 1:return "Mainly clear"; case 2:return "Partly cloudy"; case 3:return "Overcast";
            case 45:case 48:return "Fog"; case 51:case 53:case 55:return "Drizzle"; case 56:case 57:return "Freezing drizzle";
            case 61:case 63:case 65:return "Rain"; case 66:case 67:return "Freezing rain"; case 71:case 73:case 75:return "Snow"; case 77:return "Snow grains";
            case 80:case 81:case 82:return "Rain showers"; case 85:case 86:return "Snow showers";
            case 95:return "Thunderstorm"; case 96:case 99:return "Thunderstorm with hail"; default:return "Conditions unreported";
        }
    }
}
