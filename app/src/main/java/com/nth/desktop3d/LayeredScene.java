package com.nth.desktop3d;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One self-contained scene: bounded copied images, normalized placement, independent depth. */
final class LayeredScene {
    static final int MAX_OBJECTS=6, MAX_BYTES=8*1024*1024;
    static final class ObjectLayer {
        String id=UUID.randomUUID().toString(), kind="crystal", name="Crystal";
        float x=.5f,y=.5f,size=.24f,depth=.7f,rotation;
        byte[] image;
        JSONObject json()throws Exception {
            JSONObject j=new JSONObject().put("id",id).put("kind",kind).put("name",name).put("x",x).put("y",y).put("size",size).put("depth",depth).put("rotation",rotation);
            if(image!=null)j.put("image",asset(image));return j;
        }
    }
    static final class Document {
        byte[] background;int fit;float backgroundDepth=.15f,strength=.6f;boolean motion=true;
        final List<ObjectLayer> objects=new ArrayList<>();
        JSONObject json()throws Exception {
            JSONObject j=new JSONObject().put("format","nth-layered-scene").put("version",1).put("fit",fit).put("backgroundDepth",backgroundDepth).put("strength",strength).put("motion",motion);
            if(background!=null)j.put("background",asset(background));JSONArray a=new JSONArray();for(ObjectLayer o:objects)a.put(o.json());return j.put("objects",a);
        }
    }
    static JSONObject asset(byte[] bytes)throws Exception{return new JSONObject().put("sha256",DesktopBackup.digest(bytes)).put("data",Base64.encodeToString(bytes,Base64.NO_WRAP));}
    static Document copy(Document source){Document d=new Document();d.background=source.background;d.fit=source.fit;d.backgroundDepth=source.backgroundDepth;d.strength=source.strength;d.motion=source.motion;for(ObjectLayer o:source.objects){ObjectLayer n=new ObjectLayer();n.id=o.id;n.kind=o.kind;n.name=o.name;n.x=o.x;n.y=o.y;n.size=o.size;n.depth=o.depth;n.rotation=o.rotation;n.image=o.image;d.objects.add(n);}return d;}
    private static byte[] image(JSONObject j,boolean foreground)throws Exception {
        String encoded=DesktopBackup.text(j,"data",3*1024*1024);byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);
        if(!DesktopBackup.digest(bytes).equals(DesktopBackup.text(j,"sha256",64)))throw new IOException("Scene image checksum failed");WallpaperStore.validate(bytes);
        if(foreground&&bytes.length>1024*1024)throw new IOException("Object image is too large");return bytes;
    }
    static Document parse(JSONObject j)throws Exception {
        if(!"nth-layered-scene".equals(j.optString("format"))||j.optInt("version")!=1)throw new JSONException("Unsupported scene");
        Document d=new Document();d.fit=DesktopBackup.integer(j,"fit",0,1);d.backgroundDepth=number(j,"backgroundDepth",0,1);d.strength=number(j,"strength",0,1);
        if(!(j.get("motion") instanceof Boolean))throw new JSONException("Invalid scene motion");d.motion=j.getBoolean("motion");if(j.has("background"))d.background=image(j.getJSONObject("background"),false);
        JSONArray objects=j.getJSONArray("objects");if(objects.length()>MAX_OBJECTS)throw new JSONException("Too many scene objects");Set<String> ids=new HashSet<>();
        for(int i=0;i<objects.length();i++){
            JSONObject raw=objects.getJSONObject(i);ObjectLayer o=new ObjectLayer();o.id=DesktopBackup.text(raw,"id",100);if(o.id.isEmpty()||!ids.add(o.id))throw new JSONException("Duplicate scene object");
            o.kind=DesktopBackup.text(raw,"kind",20);if(!Arrays.asList("crystal","orb","star","image").contains(o.kind))throw new JSONException("Invalid scene object");o.name=DesktopBackup.text(raw,"name",100);
            o.x=number(raw,"x",0,1);o.y=number(raw,"y",0,1);o.size=number(raw,"size",.08f,.85f);o.depth=number(raw,"depth",0,1);o.rotation=number(raw,"rotation",-180,180);
            if("image".equals(o.kind))o.image=image(raw.getJSONObject("image"),true);else if(raw.has("image"))throw new JSONException("Unexpected object image");d.objects.add(o);
        }
        if(j.toString().getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IOException("Scene is too large");return d;
    }
    private static float number(JSONObject j,String key,float min,float max)throws JSONException {Object v=j.get(key);if(!(v instanceof Number))throw new JSONException("Invalid "+key);float f=((Number)v).floatValue();if(!Float.isFinite(f)||f<min||f>max)throw new JSONException("Invalid "+key);return f;}
    static File file(Context c,boolean draft){return new File(c.getFilesDir(),draft?"layered-scene-draft.json":"layered-scene.json");}
    static Document read(Context c,boolean draft)throws Exception {
        File f=file(c,draft);if(!f.exists())return null;try(InputStream in=new AtomicFile(f).openRead()){return parse(new JSONObject(new String(WallpaperStore.read(in,MAX_BYTES),StandardCharsets.UTF_8)));}
    }
    static void write(Context c,Document d,boolean draft)throws Exception {
        AtomicFile f=new AtomicFile(file(c,draft));if(d==null){f.delete();return;}JSONObject j=d.json();parse(j);byte[] bytes=j.toString().getBytes(StandardCharsets.UTF_8);FileOutputStream out=null;
        try{out=f.startWrite();out.write(bytes);f.finishWrite(out);}catch(IOException error){if(out!=null)f.failWrite(out);throw error;}
    }
    private LayeredScene(){}
}
