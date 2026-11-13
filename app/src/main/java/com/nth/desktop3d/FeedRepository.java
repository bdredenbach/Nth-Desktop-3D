package com.nth.desktop3d;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;

/** Local headline cache and interests. Never uploads reading history or launcher data. */
final class FeedRepository {
    static final String[] TOPICS={"World","Technology","Science","Space","Culture","Sports","Personal"};
    static final class Source {
        final String name,url,topic;boolean enabled;
        Source(String name,String url,String topic,boolean enabled){this.name=name;this.url=url;this.topic=topic;this.enabled=enabled;}
    }
    final SharedPreferences prefs;final List<Source> sources=new ArrayList<>();final List<FeedParser.Entry> cache=new ArrayList<>(),saved=new ArrayList<>();
    final Set<String> hidden=new LinkedHashSet<>(),read=new LinkedHashSet<>(),interests=new LinkedHashSet<>();long updated;
    FeedRepository(Context c){prefs=c.getSharedPreferences("nth_native_feed",0);load();}
    private void load(){
        try{JSONArray a=new JSONArray(prefs.getString("sources","[]"));for(int i=0;i<Math.min(12,a.length());i++){JSONObject s=a.getJSONObject(i);String url=FeedParser.https(s.getString("url"),s.getString("url"));if(!url.isEmpty())sources.add(new Source(s.getString("name"),url,s.getString("topic"),s.optBoolean("enabled",true)));}}catch(JSONException ignored){}
        if(!prefs.contains("sources")){sources.add(new Source("BBC News · World","https://feeds.bbci.co.uk/news/world/rss.xml","World",true));sources.add(new Source("BBC News · Technology","https://feeds.bbci.co.uk/news/technology/rss.xml","Technology",true));sources.add(new Source("NASA","https://www.nasa.gov/feed/","Space",true));}
        cache.addAll(decode(prefs.getString("cache","[]")));saved.addAll(decode(prefs.getString("saved","[]")));hidden.addAll(prefs.getStringSet("hidden",Collections.emptySet()));read.addAll(prefs.getStringSet("read",Collections.emptySet()));interests.addAll(prefs.getStringSet("interests",Collections.emptySet()));updated=prefs.getLong("updated",0);
    }
    static List<FeedParser.Entry> decode(String json){List<FeedParser.Entry> list=new ArrayList<>();try{JSONArray a=new JSONArray(json);for(int i=0;i<Math.min(240,a.length());i++){JSONObject e=a.getJSONObject(i);String u=e.getString("url");if(!FeedParser.https(u,u).isEmpty())list.add(new FeedParser.Entry(e.getString("title"),u,e.optString("summary"),e.optString("source"),e.optString("topic"),e.optLong("published")));}}catch(JSONException ignored){}return list;}
    static String encode(List<FeedParser.Entry> list){JSONArray a=new JSONArray();for(FeedParser.Entry e:list)try{a.put(new JSONObject().put("title",e.title).put("url",e.url).put("summary",e.summary).put("source",e.source).put("topic",e.topic).put("published",e.published));}catch(JSONException ignored){}return a.toString();}
    void save(){JSONArray a=new JSONArray();for(Source s:sources)try{a.put(new JSONObject().put("name",s.name).put("url",s.url).put("topic",s.topic).put("enabled",s.enabled));}catch(JSONException ignored){}trim(hidden,500);trim(read,500);prefs.edit().putString("sources",a.toString()).putString("cache",encode(cache)).putString("saved",encode(saved)).putStringSet("hidden",new LinkedHashSet<>(hidden)).putStringSet("read",new LinkedHashSet<>(read)).putStringSet("interests",new LinkedHashSet<>(interests)).putLong("updated",updated).apply();}
    private void trim(Set<String> set,int max){while(set.size()>max)set.remove(set.iterator().next());}
    boolean isSaved(String url){for(FeedParser.Entry e:saved)if(e.url.equals(url))return true;return false;}
    void toggleSaved(FeedParser.Entry e){if(isSaved(e.url))saved.removeIf(x->x.url.equals(e.url));else{saved.add(0,e);while(saved.size()>200)saved.remove(saved.size()-1);}save();}
    List<FeedParser.Entry> visible(boolean bookmarks,String query){List<FeedParser.Entry> out=new ArrayList<>();String q=query.toLowerCase(Locale.ROOT).trim();for(FeedParser.Entry e:bookmarks?saved:cache){if(!bookmarks&&(hidden.contains(e.url)||sources.stream().noneMatch(s->s.enabled&&s.name.equals(e.source))))continue;if(!(e.title+" "+e.summary+" "+e.source+" "+e.topic).toLowerCase(Locale.ROOT).contains(q))continue;out.add(e);}if(!bookmarks)out.sort(Comparator.<FeedParser.Entry>comparingInt(e->interests.contains(e.topic)?0:1).thenComparingInt(e->read.contains(e.url)?1:0).thenComparing(Comparator.comparingLong((FeedParser.Entry e)->e.published).reversed()));return out;}
    interface Fetcher {List<FeedParser.Entry> fetch(Source source)throws Exception;}
    static List<FeedParser.Entry> fetch(Source source)throws Exception {
        String address=source.url;
        for(int redirect=0;redirect<5;redirect++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();HttpURLConnection connection=(HttpURLConnection)new URL(address).openConnection();connection.setConnectTimeout(6500);connection.setReadTimeout(6500);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("User-Agent","NthDesktop3D/0.6 (personal RSS reader)");connection.setRequestProperty("Accept","application/rss+xml, application/atom+xml, application/xml, text/xml");
            try{int code=connection.getResponseCode();if(code>=300&&code<=399){String next=connection.getHeaderField("Location");address=next==null?"":FeedParser.https(next,address);if(address.isEmpty())throw new IOException("Publisher redirected to an unsupported address");continue;}if(code!=200)throw new IOException("Publisher returned HTTP "+code);if(connection.getContentLengthLong()>FeedParser.MAX_BYTES)throw new IOException("Feed exceeds 2 MB");try(InputStream input=connection.getInputStream()){return FeedParser.parse(input,address,source.name,source.topic);}}finally{connection.disconnect();}
        }throw new IOException("Too many redirects");
    }
    void replaceSource(Source source,List<FeedParser.Entry> entries){cache.removeIf(e->e.source.equals(source.name));cache.addAll(entries);Map<String,FeedParser.Entry> unique=new LinkedHashMap<>();for(FeedParser.Entry e:cache)unique.putIfAbsent(e.url,e);cache.clear();cache.addAll(unique.values());cache.sort(Comparator.comparingLong((FeedParser.Entry e)->e.published).reversed());while(cache.size()>240)cache.remove(cache.size()-1);}
}
