package com.nth.desktop3d;

import org.json.*;
import java.util.*;

/** Bounded, device-local conversations. Only completed cloud exchanges become model context. */
final class NotaConversation {
    static final int MAX_CHATS=12, MAX_TURNS=24, CONTEXT_CHARS=9000;
    static final class Turn {
        final String question; final LiveAnswer answer; final boolean cloud;
        Turn(String question,LiveAnswer answer,boolean cloud){this.question=question;this.answer=answer;this.cloud=cloud;}
    }
    static final class Chat {
        final String id; final List<Turn> turns=new ArrayList<>(); long updated;
        Chat(String id){this.id=id;updated=System.currentTimeMillis();}
        String title(){return turns.isEmpty()?"New conversation":LiveQuestionPolicy.limit(turns.get(0).question.replaceAll("\\s+"," "),64);}
        JSONArray context(){
            List<Turn> included=new ArrayList<>();int size=0;
            for(int i=turns.size()-1;i>=0&&included.size()<6;i--){Turn t=turns.get(i);if(!t.cloud||t.answer.searchFallback)continue;int chars=t.question.length()+t.answer.text.length();if(size+chars>CONTEXT_CHARS)break;included.add(0,t);size+=chars;}
            JSONArray messages=new JSONArray();try{for(Turn t:included){messages.put(new JSONObject().put("role","user").put("content",t.question));messages.put(new JSONObject().put("role","assistant").put("content",t.answer.text));}}catch(JSONException impossible){throw new IllegalStateException(impossible);}return messages;
        }
    }
    final List<Chat> chats=new ArrayList<>(); private Chat current;
    NotaConversation(String saved){
        try{JSONObject document=new JSONObject(saved);JSONArray list=document.getJSONArray("chats");for(int i=0;i<list.length()&&i<MAX_CHATS;i++){JSONObject item=list.getJSONObject(i);Chat chat=new Chat(item.getString("id"));chat.updated=item.optLong("updated",0);JSONArray turns=item.getJSONArray("turns");for(int n=Math.max(0,turns.length()-MAX_TURNS);n<turns.length();n++){JSONObject t=turns.getJSONObject(n),a=t.getJSONObject("answer");List<LiveAnswer.Source> sources=new ArrayList<>();JSONArray refs=a.optJSONArray("sources");if(refs!=null)for(int s=0;s<refs.length()&&s<5;s++){JSONObject ref=refs.getJSONObject(s);String url=ref.optString("url");if(LiveAnswer.safeLink(url))sources.add(new LiveAnswer.Source(ref.optString("title"),url,"",ref.optString("detail")));}chat.turns.add(new Turn(LiveQuestionPolicy.limit(t.getString("question"),2000),new LiveAnswer(LiveQuestionPolicy.limit(a.getString("text"),6000),a.optString("kind","AI answer"),a.optLong("checkedAt",0),sources,a.optBoolean("fallback",false)),t.optBoolean("cloud",false)));}chats.add(chat);}String selected=document.optString("current");for(Chat c:chats)if(c.id.equals(selected))current=c;}catch(JSONException|RuntimeException invalid){chats.clear();}
        if(current==null){if(chats.isEmpty())newChat();else current=chats.get(0);}
    }
    Chat current(){return current;}
    void newChat(){if(current!=null&&current.turns.isEmpty())return;current=new Chat(UUID.randomUUID().toString());chats.add(0,current);while(chats.size()>MAX_CHATS)chats.remove(chats.size()-1);}
    void select(String id){for(Chat c:chats)if(c.id.equals(id)){current=c;return;}}
    void deleteCurrent(){chats.remove(current);current=null;if(chats.isEmpty())newChat();else current=chats.get(0);}
    void append(String question,LiveAnswer answer,boolean cloud){current.turns.add(new Turn(question,answer,cloud));while(current.turns.size()>MAX_TURNS)current.turns.remove(0);current.updated=System.currentTimeMillis();chats.remove(current);chats.add(0,current);}
    String save(){try{JSONArray list=new JSONArray();for(Chat c:chats){JSONArray turns=new JSONArray();for(Turn t:c.turns){JSONArray sources=new JSONArray();for(LiveAnswer.Source s:t.answer.sources)sources.put(new JSONObject().put("title",s.title).put("url",s.url).put("detail",s.detail));turns.put(new JSONObject().put("question",t.question).put("cloud",t.cloud).put("answer",new JSONObject().put("text",t.answer.text).put("kind",t.answer.kind).put("checkedAt",t.answer.checkedAt).put("fallback",t.answer.searchFallback).put("sources",sources)));}list.put(new JSONObject().put("id",c.id).put("updated",c.updated).put("turns",turns));}return new JSONObject().put("current",current.id).put("chats",list).toString();}catch(JSONException impossible){throw new IllegalStateException(impossible);}}
}
