package com.nth.desktop3d;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** The owner's existing public Nth Nota gateway; no provider secret is bundled. */
final class NotaSettingsClient {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();private final Handler handler=new Handler(Looper.getMainLooper());
    private volatile HttpURLConnection connection;private volatile int generation;private boolean closed;
    void cancel(){generation++;HttpURLConnection c=connection;if(c!=null)c.disconnect();}
    void ask(String question,String settings,Consumer<String> result){
        cancel();int request=generation;
        executor.execute(()->{HttpURLConnection c=null;String response;
            try{
                c=(HttpURLConnection)new URL("https://nth-nota-free-ai.bdredenbach.workers.dev/v1/write").openConnection();connection=c;c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(35000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Origin","https://appassets.androidplatform.net");
                String prompt="You are Nth Nota, the settings assistant in Nth Desktop 3D. Answer briefly in plain text. Explain only the available settings below. You cannot perform actions through this cloud reply; never claim you changed a setting. Direct commands such as 'turn off tilt', 'set grid to five columns', 'make icons larger', 'show folder settings' and 'undo' are handled on the phone. Do not invent integrations, menus or permissions. Available settings and current values:\n"+settings+"\nUser question:\n"+question;
                byte[] bytes=new JSONObject().put("task","brainstorm").put("prompt",prompt).put("consentAccepted",true).toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);
                if(request!=generation)return;try(OutputStream stream=c.getOutputStream()){stream.write(bytes);}int status=c.getResponseCode();InputStream stream=status>=200&&status<300?c.getInputStream():c.getErrorStream();if(stream==null)throw new java.io.IOException();
                ByteArrayOutputStream data=new ByteArrayOutputStream();try(InputStream in=stream){byte[] chunk=new byte[4096];int count;while((count=in.read(chunk))!=-1){if(data.size()+count>100000)throw new java.io.IOException();data.write(chunk,0,count);}}
                JSONObject json=new JSONObject(data.toString("UTF-8"));response=status>=200&&status<300?json.optString("text"):"Cloud assistance is unavailable right now. Local setting commands still work.";if(response.trim().isEmpty())throw new java.io.IOException();
            }catch(Exception error){response="Cloud assistance could not connect. Try again later, or use a local setting command.";}
            finally{if(c!=null)c.disconnect();if(connection==c)connection=null;}
            final String answer=response;if(request==generation)handler.post(()->{if(!closed&&request==generation)result.accept(answer);});
        });
    }
    void close(){closed=true;cancel();executor.shutdownNow();}
}
