package com.nth.desktop3d;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Bounded HTTPS requests to fixed providers, canceled when consent or the screen changes. */
final class LiveQuestionHttp implements LiveQuestions.Transport {
    private final BooleanSupplier allowed;
    private final long lookupDeadline = System.nanoTime() + 25_000_000_000L;
    private volatile HttpURLConnection connection;
    private volatile boolean canceled;
    LiveQuestionHttp(BooleanSupplier allowed) { this.allowed = allowed; }
    private void check() { if (canceled || !allowed.getAsBoolean()) throw new CancellationException(); }
    @Override public void cancel() { canceled=true;HttpURLConnection current=connection;if(current!=null)current.disconnect(); }
    @Override public String get(String url)throws Exception{return request(url,null);}
    @Override public String post(String url,String json)throws Exception{return request(url,json);}
    private String request(String url,String payload)throws Exception{
        check();URI uri=new URI(url);
        if(!LiveAnswer.safeLink(url)||!Arrays.asList("en.wikipedia.org","site.api.espn.com","nth-nota-free-ai.bdredenbach.workers.dev").contains(uri.getHost()))throw new IOException("Unsupported provider");
        boolean post=payload!=null;long deadline=post?System.nanoTime()+60_000_000_000L:lookupDeadline;
        int remaining=(int)Math.min(55000,Math.max(0,(deadline-System.nanoTime())/1_000_000L));if(remaining<=0)throw new SocketTimeoutException();
        HttpURLConnection current=(HttpURLConnection)new URL(url).openConnection();connection=current;
        try{
            check();current.setInstanceFollowRedirects(false);current.setUseCaches(false);
            current.setConnectTimeout(Math.min(8000,remaining));current.setReadTimeout(Math.min(post?55000:5000,remaining));
            current.setRequestProperty("User-Agent","NthDesktop3D/0.9.6 (https://github.com/bdredenbach/Nth-Desktop-3D)");current.setRequestProperty("Accept","application/json");
            if(post){
                if(!url.equals(LiveQuestions.GATEWAY)&&!url.equals(LiveQuestions.CHAT))throw new IOException("Unsupported cloud endpoint");
                byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);current.setRequestMethod("POST");current.setDoOutput(true);
                current.setRequestProperty("Content-Type","application/json");current.setRequestProperty("Origin","https://appassets.androidplatform.net");current.setFixedLengthStreamingMode(bytes.length);
                check();try(OutputStream out=current.getOutputStream()){check();out.write(bytes);}
            }
            int code=current.getResponseCode();check();if(code<200||code>=300)throw new IOException("Provider HTTP "+code);
            int max=post?100000:2_000_000;ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=current.getInputStream()){
                byte[] block=new byte[8192];int count;
                while((count=in.read(block))!=-1){check();if(System.nanoTime()>deadline)throw new SocketTimeoutException();if(bytes.size()+count>max)throw new IOException("Provider response too large");bytes.write(block,0,count);}
            }
            check();return bytes.toString("UTF-8");
        }finally{current.disconnect();if(connection==current)connection=null;}
    }
}
