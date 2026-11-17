package com.nth.desktop3d;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.widget.*;
import org.json.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Deterministic provider fixtures, actual consent cancellation, and native source-link controls. */
public final class LiveQuestions095Smoke extends Trial093Support {
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-11-17T02:00:00Z"),ZoneId.of("America/New_York"));
    private static final String FACT="A rainbow is caused by reflection and refraction of sunlight in water droplets.";
    public LiveQuestions095Smoke(Instrumentation test,MainActivity activity,String output){super(test,activity,output);}
    public void run()throws Exception{snapshot();boolean consent=store.preferences.getBoolean("cloud_ai",false);try{engine();consent();ui();}finally{main(()->store.preferences.edit().putBoolean("cloud_ai",consent).apply());restore();}}
    private static JSONObject team(String id,String name,String shortName)throws Exception{return new JSONObject().put("id",id).put("displayName",name).put("name",shortName).put("shortDisplayName",shortName).put("abbreviation",id.equals("2")?"BUF":"NYG");}
    private static String catalog(JSONObject... teams)throws Exception{JSONArray entries=new JSONArray();for(JSONObject team:teams)entries.put(new JSONObject().put("team",team));return new JSONObject().put("sports",new JSONArray().put(new JSONObject().put("leagues",new JSONArray().put(new JSONObject().put("teams",entries))))).toString();}
    private static JSONObject event(String id,String date,String state,String status,boolean completed,String opponent)throws Exception{
        JSONObject type=new JSONObject().put("state",state).put("name",status).put("completed",completed);
        JSONArray competitors=new JSONArray().put(new JSONObject().put("homeAway","home").put("team",team("2","Buffalo Bills","Bills"))).put(new JSONObject().put("homeAway","away").put("team",team("9",opponent,opponent)));
        JSONObject competition=new JSONObject().put("status",new JSONObject().put("type",type)).put("competitors",competitors).put("timeValid",true).put("dateValid",true);
        return new JSONObject().put("id",id).put("date",date).put("name","Bills vs "+opponent).put("competitions",new JSONArray().put(competition)).put("links",new JSONArray().put(new JSONObject().put("href","https://www.espn.com/nfl/game/_/gameId/"+id)));
    }
    private static String schedule(JSONObject... events)throws Exception{return new JSONObject().put("season",new JSONObject().put("year",2026)).put("events",new JSONArray(Arrays.asList(events))).toString();}
    private static final class Fake implements LiveQuestions.Transport {
        final AtomicInteger gets=new AtomicInteger(),posts=new AtomicInteger();final List<String> urls=new ArrayList<>();String prompt="",generated;
        boolean ambiguous,failCloud,block;String games,preseason,postseason;final CountDownLatch entered=new CountDownLatch(1),released=new CountDownLatch(1);
        Fake()throws Exception{
            games=schedule(event("past","2026-11-10T18:00Z","post","STATUS_FINAL",true,"Past opponent"),event("cancel","2026-11-18T18:00Z","pre","STATUS_CANCELED",false,"Canceled opponent"),event("postponed","2026-11-19T18:00Z","pre","STATUS_POSTPONED",false,"Postponed opponent"),event("next","2026-11-22T18:00Z","pre","STATUS_SCHEDULED",false,"Verified Opponent"));
            generated=new JSONObject().put("text",new JSONObject().put("answer","Rainbows form when sunlight is reflected and refracted in water droplets.").put("uncertain",false).put("citations",new JSONArray().put(new JSONObject().put("source",1).put("quote",FACT))).toString()).toString();
        }
        public String get(String url)throws Exception{
            gets.incrementAndGet();urls.add(url);if(block){entered.countDown();released.await(8,TimeUnit.SECONDS);}
            if(url.contains("wikipedia.org"))return new JSONObject().put("query",new JSONObject().put("pages",new JSONArray().put(new JSONObject().put("title","Rainbow").put("index",1).put("fullurl","https://en.wikipedia.org/wiki/Rainbow").put("extract",FACT)))).toString();
            if(url.contains("/teams?")){if(url.contains("/nfl/"))return ambiguous?catalog(team("19","New York Giants","Giants")):catalog(team("2","Buffalo Bills","Bills"));if(ambiguous&&url.contains("/mlb/"))return catalog(team("26","San Francisco Giants","Giants"));return catalog();}
            if(url.contains("seasontype=1")&&preseason!=null)return preseason;
            if(url.contains("seasontype=3"))return postseason==null?schedule():postseason;
            return games;
        }
        public String post(String url,String json)throws Exception{posts.incrementAndGet();prompt=json;if(failCloud)throw new java.io.IOException("Quota");return generated;}
        public void cancel(){released.countDown();}
    }
    private void engine()throws Exception{
        LiveQuestions engine=new LiveQuestions(CLOCK);Fake fake=new Fake();LiveAnswer next=engine.answer("Who do the Buffalo Bills play next?","PRIVATE_SETTINGS_SENTINEL",fake,()->true);
        check(next.text.contains("Verified Opponent")&&!next.text.contains("Canceled opponent")&&!next.text.contains("Past opponent"),"A completed/canceled fixture became the next game");
        check(next.text.contains("1:00 PM EST")&&next.sources.size()==1&&next.checkedAt==CLOCK.millis(),"Game time/source/check date was lost");check(fake.posts.get()==0,"A schedule answer was guessed by the language model");
        Fake playoff=new Fake();playoff.games=schedule();playoff.postseason=schedule(event("playoff","2026-11-25T18:00Z","pre","STATUS_SCHEDULED",false,"Playoff Opponent"));check(engine.answer("NFL Buffalo Bills next game","",playoff,()->true).text.contains("Playoff Opponent"),"Postseason fallback was ignored");
        Fake summer=new Fake();summer.games=schedule(event("regular","2026-09-14T18:00Z","pre","STATUS_SCHEDULED",false,"Regular Opponent"));summer.preseason=schedule(event("pre","2026-08-16T18:00Z","pre","STATUS_SCHEDULED",false,"Preseason Opponent"));
        check(new LiveQuestions(Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"),CLOCK.getZone())).answer("NFL Buffalo Bills next game","",summer,()->true).text.contains("Preseason Opponent"),"Preseason was skipped in favor of a later regular game");
        Fake tbd=new Fake();JSONObject unsure=event("tbd","2026-11-22T18:00Z","pre","STATUS_SCHEDULED",false,"TBD Opponent");unsure.getJSONArray("competitions").getJSONObject(0).put("timeValid",false);tbd.games=schedule(unsure);check(engine.answer("NFL Bills next game","",tbd,()->true).text.contains("time is still to be confirmed"),"A TBD start time was asserted as confirmed");
        Fake ambiguous=new Fake();ambiguous.ambiguous=true;LiveAnswer clarify=engine.answer("Who do the Giants play next?","",ambiguous,()->true);check(clarify.searchFallback&&clarify.text.contains("New York Giants")&&clarify.text.contains("San Francisco Giants"),"An ambiguous team nickname was guessed");
        Fake empty=new Fake();empty.games=schedule();check(engine.answer("NFL Bills next game","",empty,()->true).searchFallback,"An absent future fixture was invented");
        Fake stale=new Fake();stale.games=new JSONObject(stale.games).put("timestamp","2025-01-01T00:00:00Z").toString();boolean refused=false;try{engine.answer("NFL Bills next game","",stale,()->true);}catch(java.io.IOException expected){refused=true;}check(refused,"Stale provider schedule was accepted");
        Fake facts=new Fake();LiveAnswer grounded=engine.answer("What causes a rainbow?","PRIVATE_SETTINGS_SENTINEL",facts,()->true);check(grounded.kind.equals("Source-grounded AI")&&grounded.sources.size()==1,"Source-backed answer failed");
        check(!facts.prompt.contains("PRIVATE_SETTINGS_SENTINEL")&&new JSONObject(facts.prompt).getString("task").equals("summarize"),"General questions leaked settings or used brainstorming mode");
        LiveAnswer invalid=LiveQuestions.grounded("{\"answer\":\"An unsupported claim\",\"uncertain\":false,\"citations\":[{\"source\":1,\"quote\":\"These words are absent from the source\"}]}",grounded.sources,CLOCK.millis());check(invalid.searchFallback&&!invalid.text.contains("unsupported claim"),"Fabricated supporting quote was displayed");
        Fake quota=new Fake();quota.failCloud=true;check(engine.answer("What causes a rainbow?","",quota,()->true).sources.size()==1,"AI outage discarded the retrieved source");
        Fake unsupported=new Fake();check(engine.answer("What is the weather today?","",unsupported,()->true).searchFallback&&unsupported.gets.get()==0&&unsupported.posts.get()==0,"Unsupported live question fell back to model memory");
        check(!LiveAnswer.safeLink("javascript:alert(1)")&&!LiveAnswer.safeLink("https://user:password@example.com"),"Unsafe source URI was accepted");
    }
    private void consent()throws Exception{
        SharedPreferences prefs=activity.getSharedPreferences("live095-fixture",0);main(()->prefs.edit().clear().apply());AtomicInteger factories=new AtomicInteger();AtomicReference<LiveAnswer> result=new AtomicReference<>();
        NotaSettingsClient off=new NotaSettingsClient(prefs,()->{},allowed->{factories.incrementAndGet();try{return new Fake();}catch(Exception e){throw new AssertionError(e);}},CLOCK);
        try{main(()->off.ask("Who do the Bills play next?","",result::set));await(()->result.get()!=null,"Disabled-cloud explanation missing");check(factories.get()==0,"Cloud-off created a network transport");}finally{main(off::close);}
        main(()->prefs.edit().putBoolean("cloud_ai",true).apply());Fake blocked=new Fake();blocked.block=true;AtomicBoolean revoked=new AtomicBoolean(),delivered=new AtomicBoolean();
        NotaSettingsClient active=new NotaSettingsClient(prefs,()->revoked.set(true),allowed->blocked,CLOCK);
        try{main(()->active.ask("Who do the Buffalo Bills play next?","",reply->delivered.set(true)));check(blocked.entered.await(8,TimeUnit.SECONDS),"Provider fixture never started");main(()->prefs.edit().putBoolean("cloud_ai",false).apply());await(revoked::get,"Turning off consent did not cancel the reply");SystemClock.sleep(300);check(!delivered.get()&&blocked.posts.get()==0&&blocked.gets.get()==1,"A canceled request continued or delivered a late reply");}finally{main(active::close);}
        main(()->prefs.edit().clear().apply());
    }
    private void ui()throws Exception{
        main(()->{try{LayeredScene.write(activity,new LayeredScene.Document(),false);}catch(Exception e){throw new AssertionError(e);}store.ambient=false;store.preferences.edit().putInt("wallpaper_source",3).putBoolean("cloud_ai",false).apply();call(activity,"applySettings",true);call(activity,"showSettings","nota");});
        SettingsStudio studio=(SettingsStudio)field(activity,"settingsStudio");Fake fake=new Fake();NotaSettingsClient old=(NotaSettingsClient)field(studio,"cloud");main(old::close);
        NotaSettingsClient client=new NotaSettingsClient(store.preferences,studio::pause,allowed->fake,CLOCK);main(()->set(studio,"cloud",client));
        main(()->{((EditText)field(studio,"question")).setText("Who do the Buffalo Bills play next?");((Button)field(studio,"send")).performClick();});check(fake.gets.get()==0&&fake.posts.get()==0,"Native cloud-off Send performed a lookup");
        main(()->{model.set("cloud_ai",1);((Button)field(studio,"send")).performClick();});await(()->((TextView)field(studio,"answer")).getText().toString().contains("Verified Opponent"),"Opt-in did not enable live questions");
        check(find(v->"nota-source:1".equals(v.getTag()))!=null,"Checked source button missing");capture("095-live-source-answer");
        AtomicReference<Intent> opened=new AtomicReference<>();Instrumentation.ActivityMonitor monitor=new Instrumentation.ActivityMonitor(){@Override public Instrumentation.ActivityResult onStartActivity(Intent intent){if(Intent.ACTION_VIEW.equals(intent.getAction())){opened.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);}return null;}};
        test.addMonitor(monitor);try{main(()->find(v->"nota-source:1".equals(v.getTag())).performClick());await(()->opened.get()!=null,"Source button did not open its URL");check(opened.get().getDataString().equals("https://www.espn.com/nfl/game/_/gameId/next"),"Source button opened a generated or wrong URL");}finally{test.removeMonitor(monitor);}
        main(()->{model.set("cloud_ai",0);((EditText)field(studio,"question")).setText("turn off tilt");((Button)field(studio,"send")).performClick();});check(!store.tilt,"Local commands stopped working with cloud off");
        main(()->call(activity,"closePanel"));
    }
}
