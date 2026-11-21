package com.nth.desktop3d;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.*;
import android.media.MediaMetadata;
import android.media.session.*;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.*;
import android.provider.CalendarContract;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Real local/service data in owner-controlled crystal surfaces, with explicit empty states. */
@android.annotation.SuppressLint("ViewConstructor")
final class CrystalCards extends LinearLayout {
    interface Host {void notes();void nota();void mediaPermission();void calendarPermission();void photos();void settings();default void place(){}default void place(String type){place();}default boolean dragCard(View view,String type,float x,float y){return false;}}
    static final String[][] TYPES={{"media","Media controls"},{"ai","Nth Nota AI"},{"weather","Weather"},{"calendar","Calendar"},{"photos","Photos"},{"reading","Reading"},{"notes","Notes"},{"apps","App cards"}};
    private final Activity activity;private final LauncherStore store;private final Host host;private final String query,embedded;
    private final Handler main=new Handler(Looper.getMainLooper());private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final WeatherLocation weatherLocation;private volatile boolean disposed,paused;private volatile int generation;private MediaController controller;private MediaSessionManager sessions;private LinearLayout mediaBody;
    private final MediaController.Callback playback=new MediaController.Callback(){@Override public void onMetadataChanged(MediaMetadata m){renderMedia();}@Override public void onPlaybackStateChanged(PlaybackState s){renderMedia();}@Override public void onSessionDestroyed(){observeMedia();}};
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionListener=list->observeMedia();
    CrystalCards(Activity activity,LauncherStore store,Host host,String query){this(activity,store,host,query,null);}
    CrystalCards(Activity activity,LauncherStore store,Host host,String query,String embedded){super(activity);weatherLocation=new WeatherLocation(activity);this.activity=activity;this.store=store;this.host=host;this.query=query==null?"":query.toLowerCase(Locale.ROOT).trim();this.embedded=embedded;setOrientation(VERTICAL);setClipChildren(false);setClipToPadding(false);if(embedded==null)refresh();}
    private int dp(float n){return Ui.dp(activity,n);}
    void refresh(){if(disposed)return;paused=false;generation++;weatherLocation.cancel();stopMedia();removeAllViews();
        if(embedded==null){Button manage=Ui.quiet(activity,"Manage crystal cards",this::manage);addView(manage,new LayoutParams(-1,dp(48)));action(this,"Place card on desktop",host::place);}int shown=0;String forced=embedded!=null?embedded:CardLayout.type(query);
        for(String[] type:TYPES){if(forced!=null){if(!type[0].equals(forced))continue;}else{if(!store.preferences.getBoolean("card_"+type[0],true))continue;if(!query.isEmpty()&&!matches(type))continue;}shown++;LinearLayout body=card(type[0],type[1]);switch(type[0]){case "media":mediaBody=body;observeMedia();break;case "ai":ai(body);break;case "weather":weather(body);break;case "calendar":calendar(body);break;case "photos":photos(body);break;case "reading":reading(body);break;case "notes":String note=store.preferences.getString("note","");text(body,note.isEmpty()?"Capture a thought in Nth Notes.":note);action(body,"Open Nth Notes",host::notes);break;default:apps(body);}}
        if(shown==0)text(this,query.isEmpty()?"Choose cards using Manage crystal cards.":"No matching cards. Try music, photos, reading, weather or calendar.");
    }
    private boolean matches(String[] type){String extra=type[0].equals("media")?" music spotify player ":type[0].equals("ai")?" assistant chatgpt gemini ":type[0].equals("calendar")?" outlook events appointments ":type[0].equals("apps")?" outlook drive dropbox discord twitch ":type[0].equals("reading")?store.preferences.getString("card_bookmarks",""):"";return (type[0]+" "+type[1]+extra).toLowerCase(Locale.ROOT).contains(query);}
    private LinearLayout card(String type,String title){LinearLayout body=Ui.column(activity);Ui.pad(body,embedded==null?16:10);body.setBackground(embedded==null?Ui.crystal(activity,24,false):null);body.setElevation(embedded==null&&new CrystalPreferences(activity).shadows()?dp(10):0);body.setCameraDistance(dp(2200));body.setTag("crystal-card");LayoutParams lp=new LayoutParams(-1,-2);lp.setMargins(dp(2),dp(embedded==null?8:2),dp(2),dp(embedded==null?14:2));if(embedded==null){HoldDragFrame holder=new HoldDragFrame(activity,(view,x,y)->host.dragCard(view,type,x,y),()->Ui.themeDialog(new AlertDialog.Builder(activity).setTitle(title).setItems(new String[]{"Place on desktop"},(dialog,which)->host.place(type)).create()));holder.setContentDescription(title+" card. Hold and drag to a desktop.");holder.setTag("crystal-card");body.setTag(null);holder.addView(body,new android.widget.FrameLayout.LayoutParams(-1,-2));addView(holder,lp);}else addView(body,lp);TextView heading=Ui.text(activity,title,embedded==null?18:15,Ui.TEXT);heading.setTypeface(null,Typeface.BOLD);heading.setPadding(0,0,0,dp(10));body.addView(heading);return body;}
    private TextView text(LinearLayout parent,String value){TextView text=Ui.text(activity,value,embedded==null?14:13,Ui.TEXT);text.setPadding(0,dp(5),0,dp(8));parent.addView(text,new LayoutParams(-1,-2));return text;}
    private Button action(LinearLayout parent,String title,Runnable click){Button b=Ui.quiet(activity,title,click);parent.addView(b,new LayoutParams(-1,embedded==null?dp(48):LayoutParams.WRAP_CONTENT));return b;}
    private void launch(Intent intent){try{activity.startActivity(intent);}catch(ActivityNotFoundException|SecurityException missing){Toast.makeText(activity,"No available app can open this item.",Toast.LENGTH_SHORT).show();}}
    private void web(String url){Uri uri=Uri.parse(url);if("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))launch(new Intent(Intent.ACTION_VIEW,uri));}
    private void app(LinearLayout parent,String title,String pkg){Intent intent=activity.getPackageManager().getLaunchIntentForPackage(pkg);if(intent!=null)action(parent,title,()->launch(intent));}
    private void ai(LinearLayout body){text(body,"Ask Nth Nota AI to find, explain or change launcher settings.");action(body,"Ask Nth Nota AI",host::nota);app(body,"Open ChatGPT","com.openai.chatgpt");app(body,"Open Gemini","com.google.android.apps.bard");}
    private void apps(LinearLayout body){int before=body.getChildCount();app(body,"Open Outlook","com.microsoft.office.outlook");app(body,"Open Google Drive","com.google.android.apps.docs");app(body,"Open Dropbox","com.dropbox.android");app(body,"Open Discord","com.discord");app(body,"Open Twitch","tv.twitch.android.app");if(body.getChildCount()==before)text(body,"Outlook, Drive, Dropbox, Discord and Twitch appear here when installed.");text(body,"These buttons open the installed apps. Synced calendar events appear in the Calendar card.");}
    private ComponentName mediaPermission(){return new ComponentName(activity,MediaAccess.class);}
    private void observeMedia(){if(disposed||paused)return;stopMedia();try{sessions=activity.getSystemService(MediaSessionManager.class);java.util.List<MediaController> active=sessions.getActiveSessions(mediaPermission());if(!active.isEmpty()){controller=active.get(0);controller.registerCallback(playback,main);}sessions.addOnActiveSessionsChangedListener(sessionListener,mediaPermission(),main);}catch(SecurityException denied){sessions=null;}renderMedia();}
    private void renderMedia(){if(disposed||paused||mediaBody==null)return;while(mediaBody.getChildCount()>1)mediaBody.removeViewAt(1);if(sessions==null){text(mediaBody,"Enable media access to show the playing track and player controls.");action(mediaBody,"Enable media access",host::mediaPermission);return;}if(controller==null){text(mediaBody,"Nothing is playing. Start music in your player.");app(mediaBody,"Open Spotify","com.spotify.music");return;}
        MediaMetadata metadata=controller.getMetadata();CharSequence title=metadata==null?null:metadata.getText(MediaMetadata.METADATA_KEY_TITLE),artist=metadata==null?null:metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);text(mediaBody,title==null?"Playing media":title.toString());if(artist!=null)text(mediaBody,artist.toString());
        LinearLayout buttons=Ui.row(activity);mediaBody.addView(buttons);PlaybackState state=controller.getPlaybackState();long allowed=state==null?0:state.getActions();boolean playing=state!=null&&state.getState()==PlaybackState.STATE_PLAYING;
        mediaButton(buttons,"Previous",(allowed&PlaybackState.ACTION_SKIP_TO_PREVIOUS)!=0,()->controller.getTransportControls().skipToPrevious());mediaButton(buttons,playing?"Pause":"Play",(allowed&(PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE))!=0,()->{if(playing)controller.getTransportControls().pause();else controller.getTransportControls().play();});mediaButton(buttons,"Next",(allowed&PlaybackState.ACTION_SKIP_TO_NEXT)!=0,()->controller.getTransportControls().skipToNext());
        PendingIntent open=controller.getSessionActivity();if(open!=null)action(mediaBody,"Open player",()->{try{open.send();}catch(PendingIntent.CanceledException ignored){}});
    }
    private void mediaButton(LinearLayout row,String title,boolean enabled,Runnable run){Button b=Ui.quiet(activity,title,()->{try{run.run();}catch(RuntimeException lost){observeMedia();}});b.setEnabled(enabled);row.addView(b,new LayoutParams(0,dp(48),1));}
    private void stopMedia(){if(controller!=null){controller.unregisterCallback(playback);controller=null;}if(sessions!=null){try{sessions.removeOnActiveSessionsChangedListener(sessionListener);}catch(RuntimeException ignored){}sessions=null;}}
    private void calendar(LinearLayout body){if(activity.checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED){text(body,"Show upcoming events from calendars synced on this phone, including Outlook calendars when Android calendar sync is enabled.");action(body,"Enable calendar card",host::calendarPermission);return;}
        TextView events=text(body,"Loading upcoming events");int token=generation;worker.execute(()->{if(disposed||paused||token!=generation)return;StringBuilder result=new StringBuilder();try{long now=System.currentTimeMillis();Uri.Builder uri=CalendarContract.Instances.CONTENT_URI.buildUpon();ContentUris.appendId(uri,now);ContentUris.appendId(uri,now+7L*24*60*60*1000);try(Cursor cursor=activity.getContentResolver().query(uri.build(),new String[]{CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN},null,null,CalendarContract.Instances.BEGIN+" ASC")){int count=0;while(cursor!=null&&cursor.moveToNext()&&count++<5)result.append(cursor.getString(0)).append('\n').append(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(cursor.getLong(1)))).append("\n\n");}if(result.length()==0)result.append("No upcoming events in the next seven days.");}catch(RuntimeException denied){result.append("Calendar access is unavailable. You can enable it in Android App info.");}post(token,()->events.setText(result.toString().trim()));});
        action(body,"Open calendar",()->launch(new Intent(Intent.ACTION_VIEW,Uri.parse("content://com.android.calendar/time/"+System.currentTimeMillis()))));action(body,"Add event",()->launch(new Intent(Intent.ACTION_INSERT,CalendarContract.Events.CONTENT_URI)));
    }
    private void photos(LinearLayout body){JSONArray photos=array("card_photo_uris");if(photos.length()==0)text(body,"Choose pictures to display in your crystal photo card.");for(int i=0;i<Math.min(embedded==null?4:1,photos.length());i++){Uri uri=Uri.parse(photos.optString(i));ImageView image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setContentDescription("Saved photo "+(i+1)+". Hold to remove.");body.addView(image,new LayoutParams(-1,dp(embedded==null?150:115)));image.setOnClickListener(v->launch(new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)));image.setOnLongClickListener(v->{new AlertDialog.Builder(activity).setTitle("Remove photo card item?").setPositiveButton("Remove",(d,w)->{JSONArray kept=new JSONArray();for(int n=0;n<photos.length();n++)if(!photos.optString(n).equals(uri.toString()))kept.put(photos.optString(n));store.preferences.edit().putString("card_photo_uris",kept.toString()).apply();try{activity.getContentResolver().releasePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException|IllegalArgumentException ignored){}refresh();}).setNegativeButton("Cancel",null).show();return true;});int token=generation;worker.execute(()->{if(disposed||paused||token!=generation)return;Bitmap bitmap=null;try{BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;try(InputStream in=activity.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,options);}options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>768)options.inSampleSize*=2;options.inJustDecodeBounds=false;try(InputStream in=activity.getContentResolver().openInputStream(uri)){bitmap=BitmapFactory.decodeStream(in,null,options);}}catch(IOException|SecurityException ignored){}Bitmap decoded=bitmap;main.post(()->{if(disposed||paused||token!=generation){if(decoded!=null)decoded.recycle();return;}if(decoded!=null)image.setImageBitmap(decoded);else image.setContentDescription("Photo unavailable. Hold to remove or choose another picture.");});});}
        action(body,"Choose photos",host::photos);
    }
    private JSONArray array(String key){try{Object stored=store.preferences.getAll().get(key);return new JSONArray(stored instanceof String?(String)stored:"[]");}catch(JSONException bad){return new JSONArray();}}
    private void reading(LinearLayout body){JSONArray entries=array("card_bookmarks");if(entries.length()==0)text(body,"Save articles and links to read later.");for(int i=0;i<entries.length();i++){JSONObject entry=entries.optJSONObject(i);if(entry==null)continue;String url=entry.optString("url"),title=entry.optString("title",url);Button b=action(body,title,()->web(url));int index=i;b.setOnLongClickListener(v->{new AlertDialog.Builder(activity).setTitle("Remove saved link?").setPositiveButton("Remove",(d,w)->{JSONArray kept=new JSONArray();for(int n=0;n<entries.length();n++)if(n!=index)kept.put(entries.opt(n));store.preferences.edit().putString("card_bookmarks",kept.toString()).apply();refresh();}).setNegativeButton("Cancel",null).show();return true;});}action(body,"Add reading link",()->{LinearLayout fields=Ui.column(activity);Ui.pad(fields,16);EditText title=new EditText(activity),url=new EditText(activity);title.setHint("Title");title.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});url.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2048)});url.setHint("https://example.com");url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);fields.addView(title);fields.addView(url);AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Save reading link").setView(fields).setPositiveButton("Save",null).setNegativeButton("Cancel",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String value=url.getText().toString().trim();Uri uri=Uri.parse(value);if(!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme()))||uri.getHost()==null){url.setError("Enter an http or https address");return;}if(entries.length()>=20){url.setError("Remove a saved link before adding another");return;}try{JSONObject entry=new JSONObject();entry.put("url",value);entry.put("title",title.getText().toString().trim().isEmpty()?uri.getHost():title.getText().toString().trim());entries.put(entry);store.preferences.edit().putString("card_bookmarks",entries.toString()).apply();dialog.dismiss();refresh();}catch(JSONException ignored){}}));dialog.show();});}
    static final int WEATHER_PERMISSION=3092;
    private void weather(LinearLayout body) {
        boolean nearby=store.preferences.getBoolean("card_weather_location",false);
        String saved=store.preferences.getString("card_weather_city","").trim();
        TextView conditions=text(body,"");
        action(body,"Choose weather city",()->{
            EditText input=new EditText(activity);
            input.setSingleLine(true); input.setHint("City, state or country"); input.setText(saved);
            input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(100)});
            new AlertDialog.Builder(activity).setTitle("Weather city").setMessage("The city name is sent to Open-Meteo to fetch weather.")
                    .setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
                        store.preferences.edit().putBoolean("card_weather_location",false)
                                .putString("card_weather_city",input.getText().toString().trim())
                                .remove("card_weather_lat").remove("card_weather_lon").apply();
                        refresh();
                    }).show();
        });
        action(body,"Use my location",()->new AlertDialog.Builder(activity).setTitle("Use approximate location?")
                .setMessage("Your approximate location will be sent to Open-Meteo for weather.")
                .setNegativeButton("Cancel",null).setPositiveButton("Use location",(d,w)->{
                    if(!weatherLocation.permitted()) activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},WEATHER_PERMISSION);
                    else { store.preferences.edit().putBoolean("card_weather_location",true).apply(); refresh(); }
                }).show());
        action(body,"Weather data: Open-Meteo",()->web(CurrentWeather.SOURCE));
        if(!nearby && saved.isEmpty()) { conditions.setText("Choose a city for the weather card."); return; }
        Runnable load=()->loadWeather(conditions,nearby,saved,false);
        load.run();
        action(body,"Switch °C / °F",()->{
            boolean f=store.preferences.getBoolean("card_weather_fahrenheit","US".equals(Locale.getDefault().getCountry()));
            store.preferences.edit().putBoolean("card_weather_fahrenheit",!f).apply(); refresh();
        });
        action(body,"Refresh weather",()->loadWeather(conditions,nearby,saved,true));
    }
    private volatile int weatherRequest;
    private void loadWeather(TextView view,boolean nearby,String city,boolean refresh) {
        int token=generation, request=++weatherRequest;
        view.setText(nearby?"Checking your approximate location":"Loading weather for "+city);
        java.util.function.Consumer<CurrentWeather.Place> load=place->{
            if(disposed || paused || token!=generation || request!=weatherRequest) return;
            if(place==null) { view.setText("Location unavailable. Choose a city or enable weather location."); return; }
            java.util.function.BooleanSupplier active=()->!disposed && !paused && token==generation && request==weatherRequest
                    && (!nearby || weatherLocation.permitted());
            worker.execute(()->{
                LiveQuestionHttp http=new LiveQuestionHttp(active);
                try {
                    LiveAnswer answer=CurrentWeather.lookup(store.preferences,place,refresh,java.time.Clock.systemDefaultZone(),http,active);
                    post(token,()->{if(request==weatherRequest) view.setText(answer.text);});
                } catch(java.util.concurrent.CancellationException canceled) { /* A newer request owns the card. */ }
                finally { http.cancel(); }
            });
        };
        if(nearby) weatherLocation.locate(load); else load.accept(CurrentWeather.Place.city(city));
    }
    private void post(int token,Runnable work){main.post(()->{if(!disposed&&!paused&&token==generation)work.run();});}
    private void manage(){String[] names=new String[TYPES.length];boolean[] checked=new boolean[TYPES.length];for(int i=0;i<TYPES.length;i++){names[i]=TYPES[i][1];checked[i]=store.preferences.getBoolean("card_"+TYPES[i][0],true);}new AlertDialog.Builder(activity).setTitle("Crystal cards").setMultiChoiceItems(names,checked,(dialog,index,on)->checked[index]=on).setPositiveButton("Apply",(d,w)->{SharedPreferences.Editor edit=store.preferences.edit();for(int i=0;i<TYPES.length;i++)edit.putBoolean("card_"+TYPES[i][0],checked[i]);edit.apply();refresh();}).setNegativeButton("Cancel",null).setNeutralButton("Settings",(d,w)->host.settings()).show();}
    void tilt(float x,float y){if(store.reducedMotion)return;for(int i=0;i<getChildCount();i++){View card=getChildAt(i);if("crystal-card".equals(card.getTag())){card.setRotationX(-y*2);card.setRotationY(x*2);}}}
    void pause(){weatherLocation.cancel();paused=true;generation++;stopMedia();}
    void dispose(){disposed=true;pause();main.removeCallbacksAndMessages(null);worker.shutdownNow();}
}
