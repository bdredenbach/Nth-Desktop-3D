package com.nth.desktop3d;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Conversation persistence, context boundaries, native composer and deterministic cloud transport. */
public final class Nota096Smoke extends Trial093Support {
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-11-17T03:00:00Z"),ZoneId.of("America/New_York"));
    public Nota096Smoke(Instrumentation test,MainActivity activity,String output){super(test,activity,output);}
    public void run()throws Exception{
        snapshot();SharedPreferences saved=activity.getSharedPreferences("nota_conversations",0);String original=saved.getString("document","");boolean cloud=store.preferences.getBoolean("cloud_ai",false);String draft=store.preferences.getString("nota_draft","");
        try{storage();engine();ui();studioPolish();}finally{main(()->{call(activity,"closePanel");saved.edit().putString("document",original).apply();store.preferences.edit().putBoolean("cloud_ai",cloud).putString("nota_draft",draft).apply();activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);});restore();}
    }
    private void storage()throws Exception{
        NotaConversation saved=new NotaConversation("");String first=saved.current().id;
        saved.append("Explain a rainbow.",LiveAnswer.plain("Sunlight bends in water droplets.","AI answer"),true);
        saved.append("turn off tilt",LiveAnswer.plain("Tilt off.","On this device"),false);
        saved.append("Latest forecast?",LiveAnswer.unavailable("Unavailable",CLOCK.millis(),Collections.emptyList()),true);
        saved.append("Incomplete",LiveAnswer.plain("Stopped","Stopped"),false);
        JSONArray history=saved.current().context();check(history.length()==2&&!history.toString().contains("Tilt"),"Local/failed/stopped actions leaked into model context");
        saved.newChat();check(saved.current().context().length()==0,"New conversation inherited old context");saved.select(first);check(saved.current().context().length()==2,"Selecting a saved chat lost context");
        NotaConversation restored=new NotaConversation(saved.save());check(restored.current().id.equals(first)&&restored.current().turns.size()==4,"Conversation round trip lost messages or selection");
        for(int i=0;i<40;i++)restored.append("Question "+i,LiveAnswer.plain(new String(new char[1800]).replace('\0','x'),"AI answer"),true);check(restored.current().turns.size()==24&&restored.current().context().length()<=12,"Conversation/context size is unbounded");
        for(int i=0;i<18;i++){restored.newChat();restored.append("Chat "+i,LiveAnswer.plain("Answer","AI answer"),true);}check(restored.chats.size()==12,"Saved chat capacity is unbounded");
        check(new NotaConversation("invalid").current()!=null,"Corrupt local data crashed the chat");
    }
    private static final class Fake implements LiveQuestions.Transport {
        int gets,posts;boolean legacy,block;String payload="",endpoint="";final CountDownLatch entered=new CountDownLatch(1),released=new CountDownLatch(1);
        public String get(String url)throws Exception{gets++;if(legacy)throw new java.io.IOException("Old gateway");return "{\"chatVersion\":1,\"webSearch\":true}";}
        public String post(String url,String json)throws Exception{posts++;payload=json;endpoint=url;if(block){entered.countDown();released.await(8,TimeUnit.SECONDS);}return legacy?"{\"text\":\"A simpler explanation.\"}":"{\"text\":\"Light bends and reflects in water droplets.\",\"searched\":true,\"sources\":[{\"title\":\"Rainbow science\",\"url\":\"https://science.example/rainbow\",\"detail\":\"Retrieved source\"},{\"title\":\"Unsafe\",\"url\":\"javascript:alert(1)\"}]}";}
        public void cancel(){released.countDown();}
    }
    private void engine()throws Exception{
        Fake fake=new Fake();JSONArray history=new JSONArray().put(new JSONObject().put("role","user").put("content","Explain rainbows.")).put(new JSONObject().put("role","assistant").put("content","Light bends."));
        LiveAnswer answer=new LiveQuestions(CLOCK).converse("Explain that simply.","PRIVATE_SETTINGS_SENTINEL",history,false,fake,()->true,stage->{});JSONObject payload=new JSONObject(fake.payload);
        check(payload.getJSONArray("messages").length()==2&&fake.endpoint.equals(LiveQuestions.CHAT),"Conversation context did not reach the chat endpoint");check(!payload.has("launcherContext"),"An ordinary question leaked launcher settings");check(answer.sources.size()==1&&answer.kind.equals("Web sources"),"Source URL filtering or reply label failed");
        Fake legacy=new Fake();legacy.legacy=true;LiveAnswer fallback=new LiveQuestions(CLOCK).converse("Help me plan today.","",history,false,legacy,()->true,stage->{});check(fallback.text.equals("A simpler explanation.")&&legacy.endpoint.equals(LiveQuestions.GATEWAY)&&new JSONObject(legacy.payload).getString("prompt").contains("Explain rainbows."),"Existing gateway lost conversational compatibility");
        Fake off=new Fake();boolean canceled=false;try{new LiveQuestions(CLOCK).converse("Explain rainbows.","",history,false,off,()->false,stage->{});}catch(CancellationException expected){canceled=true;}check(canceled&&off.gets==0&&off.posts==0,"Consent-off conversation performed network work");
    }
    private NotaSettingsClient inject(SettingsStudio studio,Fake fake){NotaSettingsClient old=(NotaSettingsClient)field(studio,"cloud");main(old::close);NotaSettingsClient client=new NotaSettingsClient(store.preferences,studio::pause,allowed->fake,CLOCK);main(()->set(studio,"cloud",client));return client;}
    private NotaChatView chat(){Object studio=field(activity,"settingsStudio");return studio==null?null:(NotaChatView)field(studio,"chat");}
    private void send(String text){main(()->{chat().question.setText(text);chat().send.performClick();});}
    private void ui()throws Exception{
        main(()->{activity.getSharedPreferences("nota_conversations",0).edit().clear().apply();store.preferences.edit().putBoolean("cloud_ai",false).putString("nota_draft","").putBoolean("nota_web_always",false).apply();try{LayeredScene.write(activity,new LayeredScene.Document(),false);}catch(Exception error){throw new AssertionError(error);}store.ambient=false;model.set("reduced_motion",1);store.preferences.edit().putInt("wallpaper_source",3).apply();call(activity,"applySettings",true);call(activity,"showSettings","nota");});
        SettingsStudio studio=(SettingsStudio)field(activity,"settingsStudio");Fake fake=new Fake();inject(studio,fake);
        await(()->((android.view.View)field(activity,"panel")).isLaidOut()&&((android.view.View)field(activity,"panel")).getAlpha()>.99f,"Chat panel did not become visible");check(find(v->v instanceof SettingsPreview)==null,"Settings preview consumes the chat screen");check(button("Conversation menu")!=null&&button("Close Nth Nota AI")!=null,"Chat navigation is missing");main(()->{android.graphics.drawable.Drawable background=((View)field(activity,"panel")).getBackground();check(background instanceof android.graphics.drawable.GradientDrawable,"Chat reading surface inherited transparent desktop glass");android.graphics.Bitmap pixel=android.graphics.Bitmap.createBitmap(40,40,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.drawable.Drawable.ConstantState saved=background.getConstantState();check(saved!=null,"Chat surface has no drawable state");android.graphics.drawable.Drawable sample=saved.newDrawable();sample.setBounds(0,0,40,40);sample.draw(new android.graphics.Canvas(pixel));check(android.graphics.Color.alpha(pixel.getPixel(20,20))==255,"Desktop icons can show through conversation text");pixel.recycle();});capture("097-nota-welcome");
        click("Explore an idea  ↗");check(fake.gets==0&&fake.posts==0&&!chat().question.getText().toString().isEmpty(),"A suggested prompt sent itself");
        send("Explain rainbows.");check(fake.gets==0&&fake.posts==0,"Cloud-off send used the network");send("turn off tilt");check(!store.tilt,"Local launcher command stopped working");
        main(()->{model.set("cloud_ai",1);chat().syncCloud();});send("Explain rainbows.");await(()->!chat().busy(),"Cloud reply did not finish");check(new JSONObject(fake.payload).getJSONArray("messages").length()==0,"Local exchanges became cloud history");
        send("Explain that simply.");await(()->!chat().busy(),"Follow-up reply did not finish");check(new JSONObject(fake.payload).getJSONArray("messages").length()==2,"Follow-up did not include the previous cloud exchange");capture("097-nota-conversation");
        AtomicReference<Intent> opened=new AtomicReference<>();Instrumentation.ActivityMonitor monitor=new Instrumentation.ActivityMonitor(){@Override public Instrumentation.ActivityResult onStartActivity(Intent intent){if(Intent.ACTION_VIEW.equals(intent.getAction())){opened.set(intent);return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);}return null;}};test.addMonitor(monitor);try{main(()->find(v->"nota-source:1".equals(v.getTag())).performClick());await(()->opened.get()!=null,"Source link did not open");check(opened.get().getDataString().equals("https://science.example/rainbow"),"Source link opened a fabricated URL");}finally{test.removeMonitor(monitor);}
        main(()->{call(activity,"closePanel");call(activity,"showSettings","nota");});check(chat().context().length()==4,"Reopening lost the conversation");
        main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));await(()->activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE,"Landscape did not apply");await(()->chat()!=null&&chat().getWidth()>0,"Landscape chat missing");check(chat().context().length()==4,"Rotation lost conversation history");check(rect(chat().answer).height()>0,"Landscape opens below the latest reply text");capture("097-nota-landscape");
        main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));await(()->activity.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT,"Portrait did not return");await(()->chat()!=null&&chat().getWidth()>0,"Portrait chat missing");
        studio=(SettingsStudio)field(activity,"settingsStudio");Fake blocked=new Fake();blocked.block=true;inject(studio,blocked);send("Tell me more.");check(blocked.entered.await(8,TimeUnit.SECONDS),"Blocked request never started");main(()->model.set("cloud_ai",0));await(()->!chat().busy(),"Opt-out did not stop the reply");SystemClock.sleep(300);check(chat().answer.getText().toString().contains("stopped"),"Late reply replaced the stopped state");check(chat().context().length()==4,"Stopped request entered model context");
        main(()->chat().newChat());check(chat().context().length()==0,"New chat inherited context");capture("097-nota-new-chat");
        main(()->{chat().draft("A draft stays ready while I type.");((android.view.inputmethod.InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(chat().question,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});SystemClock.sleep(700);check(rect(chat().send).height()>0&&rect(chat().question).height()>0,"Keyboard hid the composer");capture("097-nota-keyboard");
    }
    private void studioPolish()throws Exception{
        main(()->{((android.view.inputmethod.InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(chat().question.getWindowToken(),0);chat().newChat();});
        String code="  int count = 3;\n  System.out.println(count);";
        String rich="## A small plan\n\nStart with **one clear idea**, then take a quiet moment.\n\n- Write it down\n- Make a little space\n\n> Small steps count.\n\n```java\n"+code+"\n```\n\n| Option | Time |\n| --- | --- |\n| Start small | 5 minutes |\n| Take a break | 10 minutes |";
        main(()->{chat().begin("Help me shape a calmer day.");chat().finish(LiveAnswer.plain(rich,"AI answer"),false);});
        check(button("New conversation")!=null&&button("Conversation menu")!=null,"Dedicated AI header controls missing");
        check(find(v->v instanceof TextView&&"A small plan".contentEquals(((TextView)v).getText()))!=null,"Markdown heading was not rendered");
        check(find(v->v instanceof TableLayout)!=null,"Comparison table missing");
        main(()->button("Copy code").performClick());
        main(()->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);check(clipboard.hasPrimaryClip()&&code.contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"Code copy lost its whitespace or contents");});
        main(()->button("Edit question without sending").performClick());check(chat().question.getText().toString().equals("Help me shape a calmer day."),"Edit question did not restore its draft");
        main(()->{chat().question.setText("");ScrollView transcript=(ScrollView)find(v->"nota-transcript".equals(v.getTag()));transcript.scrollTo(0,0);});SystemClock.sleep(1500);capture("097-nota-formatted");
        main(()->((ScrollView)find(v->"nota-transcript".equals(v.getTag()))).fullScroll(View.FOCUS_DOWN));capture("097-nota-code-table");
        main(()->chat().recent());systemClick("Close");
        main(()->chat().draft("A draft for the previous conversation."));click("New conversation");check(chat().context().length()==0&&chat().question.length()==0,"Header new chat kept the prior draft or context");
        click("Conversation menu");systemClick("Launcher settings");check(((SettingsStudio)field(activity,"settingsStudio")).page().equals("root"),"Chat menu cannot return to launcher settings");
    }

}
