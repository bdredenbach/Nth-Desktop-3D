package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FeedParserTest {
    private List<FeedParser.Entry> parse(String xml)throws Exception{return FeedParser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),"https://publisher.example/news/feed","Publisher","Technology");}
    @Test public void rssExtractsTextAttributionDateAndDeduplicates()throws Exception{
        List<FeedParser.Entry> entries=parse("<rss><channel><item><title>One &amp; Two</title><link>https://publisher.example/one</link><description><![CDATA[<p>Hello <b>reader</b></p>]]></description><pubDate>Fri, 13 Nov 2026 12:00:00 GMT</pubDate></item><item><title>Duplicate</title><link>https://publisher.example/one</link></item></channel></rss>");
        assertEquals(1,entries.size());FeedParser.Entry e=entries.get(0);assertEquals("One & Two",e.title);assertEquals("Hello reader",e.summary);assertEquals("Publisher",e.source);assertEquals("Technology",e.topic);assertEquals(1794571200000L,e.published);
    }
    @Test public void atomSelectsAlternateLinkAndResolvesRelative()throws Exception{
        List<FeedParser.Entry> entries=parse("<feed xmlns='http://www.w3.org/2005/Atom'><entry><title>A story</title><link rel='self' href='/feed/1'/><link rel='alternate' href='/story/1'/><summary>Text</summary><updated>2026-11-13T12:00:00Z</updated></entry></feed>");assertEquals(1,entries.size());assertEquals("https://publisher.example/story/1",entries.get(0).url);assertTrue(entries.get(0).published>0);
    }
    @Test public void invalidAndExecutableLinksAreDiscarded()throws Exception{assertEquals(0,parse("<rss><channel><item><title>Bad</title><link>javascript:alert(1)</link></item><item><title>Bad</title><link>file:///tmp/private</link></item><item><title>Bad</title><link>http://example.com/</link></item></channel></rss>").size());assertEquals("",FeedParser.https("https://user:password@example.com/","https://example.com"));}
    @Test public void rejectsDtdAndExternalEntities()throws Exception{try{parse("<!DOCTYPE rss [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><rss><channel><item><title>&xxe;</title><link>/story</link></item></channel></rss>");fail("Accepted a DTD");}catch(org.xml.sax.SAXException expected){assertTrue(expected.getMessage().contains("DTD"));}}
    @Test public void rejectsNonFeedDocuments()throws Exception{try{parse("<html><body>Not a feed</body></html>");fail("Accepted HTML");}catch(org.xml.sax.SAXException expected){assertTrue(expected.getMessage().contains("RSS"));}}
    @Test public void capsItemsAndText()throws Exception{StringBuilder xml=new StringBuilder("<rss><channel>");for(int i=0;i<100;i++)xml.append("<item><title>").append("x".repeat(400)).append("</title><link>/story/").append(i).append("</link></item>");xml.append("</channel></rss>");List<FeedParser.Entry> entries=parse(xml.toString());assertEquals(60,entries.size());assertTrue(entries.get(0).title.length()<=221);}
    @Test public void limitsResponseBytes()throws Exception{try{parse("<rss><channel><title>"+"x".repeat(FeedParser.MAX_BYTES)+"</title></channel></rss>");fail("Accepted large feed");}catch(IOException expected){assertTrue(expected.getMessage().contains("2 MB"));}}
    @Test public void malformedDatesHaveNoFabricatedTime(){assertEquals(0,FeedParser.date("yesterday"));assertEquals(FeedParser.date("2026-11-13T12:00:00Z"),FeedParser.date("2026-11-13T13:00:00+01:00"));}
}
