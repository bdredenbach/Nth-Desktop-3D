package com.nth.desktop3d;

import java.text.Normalizer;
import java.util.Locale;

/** Local matching only. Search never executes commands or sends a cloud request. */
final class LocalSearch {
    static String fold(String value){
        if(value==null)return "";
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replace('_',' ').trim().replaceAll("\\s+"," ");
    }
    static int rank(String query,String title,String detail){
        String q=fold(query),t=fold(title),d=fold(detail);if(q.isEmpty())return 0;
        if(t.equals(q))return 0;if(t.startsWith(q))return 1;if((" "+t).contains(" "+q))return 2;if(t.contains(q))return 3;
        for(String token:q.split(" "))if(!(t+" "+d).contains(token))return -1;return 4;
    }
    static boolean matches(String query,String title,String detail){return rank(query,title,detail)>=0;}
    static String excerpt(String text,String query){
        if(text==null||text.isEmpty())return "";
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT),lower=text.toLowerCase(Locale.ROOT);
        int at=lower.indexOf(q);if(at<0&&!q.isEmpty())at=lower.indexOf(q.split("\\s+")[0]);
        int start=Math.max(0,Math.min(text.length(),at-40)),end=Math.min(text.length(),start+180);
        if(start>0&&start<text.length()&&Character.isLowSurrogate(text.charAt(start)))start--;
        if(end>start&&end<text.length()&&Character.isHighSurrogate(text.charAt(end-1)))end--;
        return text.substring(start,end);
    }
}
