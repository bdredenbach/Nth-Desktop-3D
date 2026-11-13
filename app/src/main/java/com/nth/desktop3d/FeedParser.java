package com.nth.desktop3d;

import java.io.InputStream;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** Bounded RSS/Atom reader. Publisher markup is text, never executable content. */
public final class FeedParser {
    public static final int MAX_BYTES=2*1024*1024, MAX_ITEMS=60;
    public static final class Entry {
        public final String title,url,summary,source,topic;public final long published;
        public Entry(String title,String url,String summary,String source,String topic,long published){this.title=title;this.url=url;this.summary=summary;this.source=source;this.topic=topic;this.published=published;}
    }
    public static String https(String value,String base){
        try{URI uri=new URI(base).resolve(value.trim()).normalize();if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)return "";return uri.toString();}catch(Exception bad){return "";}
    }
    public static long date(String value){try{return Instant.parse(value.trim()).toEpochMilli();}catch(Exception ignored){}try{return ZonedDateTime.parse(value.trim(),DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();}catch(Exception ignored){}try{return OffsetDateTime.parse(value.trim()).toInstant().toEpochMilli();}catch(Exception ignored){}return 0;}
    public static String plain(String value,int limit){String s=value.replaceAll("(?is)<[^>]*>"," ").replace("&nbsp;"," ").replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&#39;","'").replaceAll("\\s+"," ").trim();return s.length()<=limit?s:s.substring(0,limit)+"…";}
    public static List<Entry> parse(InputStream input,String feed,String source,String topic)throws Exception {
        SAXParserFactory f=SAXParserFactory.newInstance();f.setNamespaceAware(true);
        // Android's Expat lacks Xerces' disallow-doctype feature; LexicalHandler below rejects all DTDs.
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        XMLReader reader=f.newSAXParser().getXMLReader();
        Handler handler=new Handler(feed,source,topic);reader.setContentHandler(handler);reader.setEntityResolver((publicId,systemId)->{throw new SAXException("External entities are not supported");});
        reader.setProperty("http://xml.org/sax/properties/lexical-handler",handler);
        InputStream bounded=new java.io.FilterInputStream(input){int count;@Override public int read()throws java.io.IOException{int n=super.read();if(n!=-1&&++count>MAX_BYTES)throw new java.io.IOException("Feed exceeds 2 MB");return n;}@Override public int read(byte[] b,int off,int len)throws java.io.IOException{int n=in.read(b,off,len);if(n>0&&(count+=n)>MAX_BYTES)throw new java.io.IOException("Feed exceeds 2 MB");return n;}};
        reader.parse(new InputSource(bounded));if(!handler.feedRoot)throw new SAXException("Not an RSS or Atom feed");return handler.entries;
    }
    private static final class Handler extends DefaultHandler implements org.xml.sax.ext.LexicalHandler {
        final String feed,source,topic;final List<Entry> entries=new ArrayList<>();final Set<String> seen=new HashSet<>();final StringBuilder text=new StringBuilder();
        int depth,itemDepth=-1,captureDepth;String field="",title="",url="",summary="",published="";boolean feedRoot;
        Handler(String feed,String source,String topic){this.feed=feed;this.source=source;this.topic=topic;}
        @Override public void startElement(String ns,String local,String q,Attributes a)throws SAXException{depth++;if(depth>40)throw new SAXException("Feed nesting too deep");String name=local.isEmpty()?q:local;if(depth==1)feedRoot=name.equals("rss")||name.equals("feed")||name.equals("RDF");
            if((name.equals("item")||name.equals("entry"))&&itemDepth<0){itemDepth=depth;title=url=summary=published="";}
            else if(itemDepth>=0&&depth==itemDepth+1){field=name;captureDepth=depth;text.setLength(0);if(name.equals("link")){String href=a.getValue("href"),rel=a.getValue("rel");if(href!=null&&(rel==null||rel.equals("alternate"))&&url.isEmpty())url=https(href,feed);}}
        }
        @Override public void characters(char[] c,int start,int length){if(itemDepth>=0&&text.length()<12000)text.append(c,start,Math.min(length,12000-text.length()));}
        @Override public void endElement(String ns,String local,String q){if(itemDepth>=0&&depth==captureDepth){String value=text.toString();switch(field){case "title":title=plain(value,220);break;case "link":if(url.isEmpty())url=https(value,feed);break;case "description":case "summary":if(summary.isEmpty())summary=plain(value,220);break;case "pubDate":case "published":case "date":published=value;break;case "updated":if(published.isEmpty())published=value;break;default:break;}field="";}
            if(depth==itemDepth){if(entries.size()<MAX_ITEMS&&!title.isEmpty()&&!url.isEmpty()&&seen.add(url))entries.add(new Entry(title,url,summary,source,topic,date(published)));itemDepth=-1;field="";}depth--;
        }
        public void startDTD(String name,String publicId,String systemId)throws SAXException{throw new SAXException("DTDs are not supported");}public void endDTD(){}public void startEntity(String n){}public void endEntity(String n){}public void startCDATA(){}public void endCDATA(){}public void comment(char[] c,int s,int l){}
    }
}
