package com.nth.desktop3d;

import android.annotation.SuppressLint;
import android.app.*;
import android.graphics.Typeface;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

/** One installed launcher: native feed, local choices, saved headlines and publisher reader. */
@SuppressLint("ViewConstructor")
final class NativeFeedPage extends LinearLayout {
    interface Host {void cards();void settings();}
    final FeedRepository repository;private final Activity activity;private final Host host;private final FeedRepository.Fetcher fetcher;
    private final LinearLayout content;private final TextView status;private final Button refresh;private final EditText search;
    private final ExecutorService network=Executors.newFixedThreadPool(3);private final List<Future<?>> requests=new ArrayList<>();
    private FadingSettingsScrollView scroll;private ArticleReader reader;private String tab="news",query="",message="";private int generation,remaining,successes;private boolean disposed,busy;private int articleScroll;
    NativeFeedPage(Activity activity,Host host){
        this(activity,host,FeedRepository::fetch);
    }
    NativeFeedPage(Activity activity,Host host,FeedRepository.Fetcher fetcher){
        super(activity);this.activity=activity;this.host=host;this.fetcher=fetcher;repository=new FeedRepository(activity);setOrientation(VERTICAL);setTag("native-feed");
        LinearLayout top=Ui.row(activity);TextView welcome=Ui.text(activity,"Your world, in crystal",18,Ui.TEXT);welcome.setTypeface(null,Typeface.BOLD);top.addView(welcome,new LayoutParams(0,-2,1));refresh=Ui.quiet(activity,"Refresh",()->refresh());refresh.setContentDescription("Refresh Nth Feed");top.addView(refresh);addView(top);
        status=Ui.text(activity,"",12,Ui.MUTED);status.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);addView(status,new LayoutParams(-1,-2));
        search=new EditText(activity);search.setSingleLine(true);search.setTextColor(Ui.TEXT);search.setHintTextColor(Ui.MUTED);search.setHint("Search headlines");search.setContentDescription("Search feed headlines");addView(search,new LayoutParams(-1,Ui.dp(activity,46)));search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();if(!tab.equals("sources"))render();}});
        LinearLayout tabs=Ui.row(activity);for(String label:new String[]{"News","Saved","Sources"}){String id=label.toLowerCase(Locale.ROOT);Button b=Ui.quiet(activity,label,()->{closeReader();tab=id;render();});b.setContentDescription("Feed "+label);tabs.addView(b,new LayoutParams(0,Ui.dp(activity,46),1));}addView(tabs);
        content=Ui.column(activity);addView(content,new LayoutParams(-1,0,1));render();
        if(System.currentTimeMillis()-repository.updated>15*60*1000L)post(this::refresh);
    }
    private void text(LinearLayout into,String value,int size,int color){TextView t=Ui.text(activity,value,size,color);t.setPadding(0,Ui.dp(activity,7),0,Ui.dp(activity,7));into.addView(t,new LayoutParams(-1,-2));}
    private void button(LinearLayout into,String label,Runnable run){Button b=Ui.button(activity,label,run);LayoutParams lp=new LayoutParams(-1,-2);lp.bottomMargin=Ui.dp(activity,6);into.addView(b,lp);}
    private void updateStatus(){String when=repository.updated==0?"No refresh yet":"Updated "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(repository.updated));status.setText(busy?"Refreshing publishers… "+remaining+" remaining":message.isEmpty()?when+" · Saved headlines work offline":message+"\n"+when);refresh.setEnabled(!busy);}
    private void render(){
        if(disposed||reader!=null)return;content.removeAllViews();scroll=new FadingSettingsScrollView(activity);LinearLayout body=Ui.column(activity);Ui.pad(body,3);scroll.addView(body);content.addView(scroll,new LayoutParams(-1,-1));updateStatus();
        if(tab.equals("sources")){sources(body);return;}
        text(body,tab.equals("saved")?"Saved for later":"For you",19,Ui.TEXT);
        if(tab.equals("news")){text(body,"Interests and reading choices stay on this phone. Publishers receive normal feed and page requests.",12,Ui.MUTED);button(body,"Crystal cards  ›",host::cards);}
        List<FeedParser.Entry> entries=repository.visible(tab.equals("saved"),query);
        if(entries.isEmpty())text(body,!query.isEmpty()?"No headlines match your search.":tab.equals("saved")?"Tap Save on a story to keep its headline here. Full articles need a connection.":repository.sources.stream().noneMatch(s->s.enabled)?"Choose a source in Sources to start your feed.":busy?"Fetching your first headlines…":"No headlines yet. Tap Refresh, or check your sources and connection.",15,Ui.MUTED);
        for(FeedParser.Entry e:entries.subList(0,Math.min(60,entries.size())))story(body,e);
        if(entries.size()>60)text(body,"Showing 60 headlines. Search to find more from the cache.",12,Ui.MUTED);
        if(!entries.isEmpty())text(body,"Headlines and excerpts from the publishers above. Full stories open on the publisher’s website.",12,Ui.MUTED);
    }
    private void story(LinearLayout body,FeedParser.Entry entry){
        LinearLayout card=Ui.column(activity);Ui.pad(card,14);card.setBackground(Ui.satin(activity,0xC83A627B,0xE511203B,20));card.setElevation(Ui.dp(activity,5));card.setCameraDistance(Ui.dp(activity,2200));LayoutParams lp=new LayoutParams(-1,-2);lp.setMargins(0,Ui.dp(activity,7),0,Ui.dp(activity,9));body.addView(card,lp);card.setTag("feed-story");
        String date=entry.published==0?"":DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(entry.published));text(card,entry.source+" · "+entry.topic+(date.isEmpty()?"":" · "+date),12,Ui.ACCENT);
        TextView title=Ui.text(activity,entry.title,18,Ui.TEXT);title.setTypeface(null,Typeface.BOLD);title.setPadding(0,Ui.dp(activity,5),0,Ui.dp(activity,8));title.setContentDescription("Read story: "+entry.title);title.setOnClickListener(v->open(entry));title.setFocusable(true);card.addView(title,new LayoutParams(-1,-2));if(!entry.summary.isEmpty())text(card,entry.summary,14,Ui.MUTED);
        LinearLayout actions=Ui.row(activity);actions.addView(Ui.quiet(activity,repository.read.contains(entry.url)?"Read again":"Read",()->open(entry)),new LayoutParams(0,Ui.dp(activity,48),1));Button save=Ui.quiet(activity,repository.isSaved(entry.url)?"Saved ✓":"Save",()->{repository.toggleSaved(entry);render();});save.setContentDescription((repository.isSaved(entry.url)?"Unsave story: ":"Save story: ")+entry.title);actions.addView(save,new LayoutParams(0,Ui.dp(activity,48),1));Button more=Ui.quiet(activity,"•••",()->new AlertDialog.Builder(activity).setTitle(entry.source).setItems(new String[]{"More "+entry.topic,"Hide story","Feed preferences"},(d,index)->{if(index==0){repository.interests.add(entry.topic);message="Prioritizing "+entry.topic;}else if(index==1){repository.hidden.add(entry.url);message="Story hidden. Restore hidden stories in Sources.";}else{host.settings();return;}repository.save();render();}).show());more.setContentDescription("Story options: "+entry.title);actions.addView(more,new LayoutParams(Ui.dp(activity,52),Ui.dp(activity,48)));card.addView(actions);
    }
    private void sources(LinearLayout body){
        text(body,"Make it yours",20,Ui.TEXT);text(body,"Select interests to bring those topics first. News then favors unread stories and recent publication dates.",13,Ui.MUTED);
        for(String topic:FeedRepository.TOPICS){Switch interest=new Switch(activity);interest.setText(topic);interest.setTextColor(Ui.TEXT);interest.setMinHeight(Ui.dp(activity,48));interest.setChecked(repository.interests.contains(topic));interest.setOnCheckedChangeListener((b,checked)->{if(checked)repository.interests.add(topic);else repository.interests.remove(topic);repository.save();});body.addView(interest,new LayoutParams(-1,-2));}
        text(body,"Your publishers",18,Ui.TEXT);
        for(FeedRepository.Source source:new ArrayList<>(repository.sources)){Switch toggle=new Switch(activity);toggle.setText(source.name+" · "+source.topic);toggle.setTextColor(Ui.TEXT);toggle.setMinHeight(Ui.dp(activity,48));toggle.setChecked(source.enabled);toggle.setOnCheckedChangeListener((b,enabled)->{cancel();source.enabled=enabled;repository.save();updateStatus();});body.addView(toggle,new LayoutParams(-1,-2));text(body,source.url,11,Ui.MUTED);button(body,"Remove "+source.name,()->{cancel();repository.sources.remove(source);repository.cache.removeIf(e->e.source.equals(source.name));repository.save();render();});}
        button(body,"Add RSS or Atom source",this::addSource);button(body,"Restore hidden stories",()->{repository.hidden.clear();repository.save();message="Hidden stories restored";render();});button(body,"Reset reading preferences",()->{repository.read.clear();repository.interests.clear();repository.save();message="Reading preferences reset";render();});text(body,"Nth Feed is built into this launcher. Google Discover is a separate optional provider in Feed preferences.",13,Ui.MUTED);button(body,"Feed preferences",host::settings);
    }
    private void addSource(){
        if(repository.sources.size()>=12){Toast.makeText(activity,"Up to 12 sources are supported.",Toast.LENGTH_SHORT).show();return;}
        LinearLayout form=Ui.column(activity);Ui.pad(form,18);EditText name=new EditText(activity);name.setHint("Publisher name");name.setSingleLine(true);name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});form.addView(name);EditText url=new EditText(activity);url.setHint("https://… RSS or Atom URL");url.setSingleLine(true);url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);url.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2048)});form.addView(url);Spinner topic=new Spinner(activity);topic.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,FeedRepository.TOPICS));form.addView(topic);TextView error=Ui.text(activity,"",13,0xFFFFC9A4);form.addView(error);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Add source").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Add",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String label=name.getText().toString().trim(),address=FeedParser.https(url.getText().toString(),url.getText().toString());if(label.isEmpty()||address.isEmpty()){error.setText("Enter a name and a complete HTTPS feed URL.");return;}if(repository.sources.stream().anyMatch(s->s.name.equalsIgnoreCase(label)||s.url.equals(address))){error.setText("That name or feed URL is already present.");return;}cancel();repository.sources.add(new FeedRepository.Source(label,address,FeedRepository.TOPICS[topic.getSelectedItemPosition()],true));repository.save();dialog.dismiss();render();refresh();}));dialog.show();
    }
    void refresh(){
        if(disposed||busy)return;List<FeedRepository.Source> enabled=new ArrayList<>();for(FeedRepository.Source s:repository.sources)if(s.enabled)enabled.add(s);if(enabled.isEmpty()){message="Enable a publisher in Sources";render();return;}int token=++generation;busy=true;remaining=enabled.size();successes=0;message="";render();List<String> failed=new ArrayList<>();
        for(FeedRepository.Source source:enabled)requests.add(network.submit(()->{List<FeedParser.Entry> loaded=null;try{loaded=fetcher.fetch(source);}catch(Exception ignored){}final List<FeedParser.Entry> result=loaded;activity.runOnUiThread(()->{if(disposed||token!=generation)return;if(result!=null){repository.replaceSource(source,result);successes++;}else failed.add(source.name);remaining--;if(remaining==0){busy=false;requests.clear();if(successes>0)repository.updated=System.currentTimeMillis();message=failed.isEmpty()?"Headlines refreshed":successes==0?"Could not refresh. Showing cached headlines. Check your connection or feed URLs.":"Refreshed "+successes+" sources. Cached headlines retained for: "+String.join(", ",failed);repository.save();}updateStatus();int y=scroll==null?0:scroll.getScrollY();render();if(scroll!=null)scroll.post(()->{if(scroll!=null)scroll.scrollTo(0,y);});});}));
    }
    private void open(FeedParser.Entry entry){repository.read.add(entry.url);repository.save();articleScroll=scroll==null?0:scroll.getScrollY();reader=new ArticleReader(activity,entry.url,this::closeReader);content.removeAllViews();content.addView(reader,new LayoutParams(-1,-1));search.setVisibility(GONE);}
    private void closeReader(){if(reader==null)return;reader.dispose();reader=null;search.setVisibility(VISIBLE);render();if(scroll!=null)scroll.post(()->{if(scroll!=null)scroll.scrollTo(0,articleScroll);});}
    android.os.Bundle state(){android.os.Bundle state=new android.os.Bundle();state.putString("tab",tab);state.putString("query",query);state.putInt("scroll",reader==null?(scroll==null?0:scroll.getScrollY()):articleScroll);if(reader!=null)state.putString("article",reader.web.getUrl());return state;}
    void restore(android.os.Bundle state){if(state==null)return;String target=state.getString("tab","news");tab=target.equals("saved")||target.equals("sources")?target:"news";search.setText(state.getString("query",""));render();String address=state.getString("article","");if(!FeedParser.https(address,address).isEmpty()){open(new FeedParser.Entry("",address,"","","",0));articleScroll=state.getInt("scroll",0);}else if(scroll!=null)scroll.post(()->{if(scroll!=null)scroll.scrollTo(0,state.getInt("scroll",0));});}
    boolean back(){if(reader!=null){if(!reader.back())closeReader();return true;}if(!tab.equals("news")){tab="news";render();return true;}return false;}
    private void cancel(){generation++;for(Future<?> request:requests)request.cancel(true);requests.clear();if(busy)message="Refresh paused. Tap Refresh to try again.";busy=false;remaining=0;}
    void pause(){cancel();if(reader!=null)reader.pause();updateStatus();}void resume(){if(reader!=null)reader.resume();updateStatus();}
    void tilt(float x,float y){if(reader!=null||scroll==null||activity.getSharedPreferences("desktop3d",0).getBoolean("reducedMotion",false))return;View child=scroll.getChildAt(0);if(child instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)child;for(int i=0;i<group.getChildCount();i++){View card=group.getChildAt(i);if("feed-story".equals(card.getTag())){card.setRotationX(Math.max(-2,Math.min(2,y*2)));card.setRotationY(Math.max(-2,Math.min(2,-x*2)));}}}}
    void dispose(){disposed=true;cancel();if(reader!=null){reader.dispose();reader=null;}network.shutdownNow();}
}
