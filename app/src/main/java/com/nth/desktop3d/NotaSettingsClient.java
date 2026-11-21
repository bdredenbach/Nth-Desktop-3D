package com.nth.desktop3d;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.time.Clock;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.function.*;

/** Opt-in online questions, with local commands kept outside the cloud path. */
final class NotaSettingsClient {
    interface Factory { LiveQuestions.Transport create(BooleanSupplier allowed); }
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final SharedPreferences preferences;private final Factory factory;private final Clock clock;
    private final SharedPreferences.OnSharedPreferenceChangeListener consent;
    private volatile LiveQuestions.Transport transport;private volatile int generation;private volatile boolean closed;
    NotaSettingsClient(SharedPreferences preferences,Runnable revoked){this(preferences,revoked,LiveQuestionHttp::new,Clock.systemDefaultZone());}
    NotaSettingsClient(SharedPreferences preferences,Runnable revoked,Factory factory,Clock clock){
        this.preferences=preferences;this.factory=factory;this.clock=clock;
        consent=(values,key)->{if("cloud_ai".equals(key)&&!enabled()){cancel();handler.post(()->{if(!closed)revoked.run();});}};
        preferences.registerOnSharedPreferenceChangeListener(consent);
    }
    private boolean enabled(){return preferences.getBoolean("cloud_ai",false);}
    void cancel(){generation++;LiveQuestions.Transport current=transport;if(current!=null)current.cancel();}
    private interface Lookup {
        LiveAnswer run(LiveQuestions.Transport transport, BooleanSupplier allowed) throws Exception;
    }
    void ask(String question,String settings,Consumer<LiveAnswer> result) {
        submit((transport,allowed)->new LiveQuestions(clock).answer(question,settings,transport,allowed),result);
    }
    void ask(String question,String settings,org.json.JSONArray history,boolean alwaysSearch,Consumer<String> progress,Consumer<LiveAnswer> result) {
        submit((transport,allowed)->new LiveQuestions(clock).converse(question,settings,history,alwaysSearch,transport,allowed,
                stage->handler.post(()->{if(allowed.getAsBoolean()) progress.accept(stage);})),result);
    }
    void weather(CurrentWeather.Place place, BooleanSupplier locationAllowed, Consumer<LiveAnswer> result) {
        submit((transport,allowed)->{
            BooleanSupplier permitted=()->!place.nearby() || locationAllowed.getAsBoolean();
            try {
                return CurrentWeather.lookup(preferences,place,false,clock,transport,()->allowed.getAsBoolean() && permitted.getAsBoolean());
            } catch(CancellationException canceled) {
                if(allowed.getAsBoolean() && !permitted.getAsBoolean())
                    return LiveAnswer.plain("Location access is off. Ask for weather in a city instead.","Location unavailable");
                throw canceled;
            }
        },result);
    }
    private void submit(Lookup lookup, Consumer<LiveAnswer> result) {
        cancel();if(closed)return;final int request=generation;
        if(!enabled()){handler.post(()->{if(!closed&&request==generation)result.accept(LiveAnswer.plain("Turn on Cloud questions for Nth Nota AI to ask online questions. Local launcher commands still work.","Cloud questions off"));});return;}
        BooleanSupplier allowed=()->!closed&&request==generation&&enabled();
        executor.execute(()->{
            LiveQuestions.Transport current=null;LiveAnswer reply;
            try{
                if(!allowed.getAsBoolean())return;current=factory.create(allowed);transport=current;if(!allowed.getAsBoolean()){current.cancel();return;}
                reply=lookup.run(current,allowed);
            }catch(CancellationException canceled){return;}
            catch(Exception error){
                if(!allowed.getAsBoolean())return;
                reply=LiveAnswer.unavailable("I couldn't verify an online answer right now. Check the connection and try again, or use Search the web. Local launcher commands still work.",clock.millis(),Collections.emptyList());
            }finally{if(current!=null)current.cancel();if(transport==current)transport=null;}
            LiveAnswer answer=reply;if(allowed.getAsBoolean())handler.post(()->{if(allowed.getAsBoolean())result.accept(answer);});
        });
    }
    void close(){closed=true;preferences.unregisterOnSharedPreferenceChangeListener(consent);cancel();executor.shutdownNow();}
}
