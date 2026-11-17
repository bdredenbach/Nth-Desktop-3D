package com.nth.desktop3d;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** One self-contained scene; no desktop layouts, notes, permissions or executable content. */
final class ScenePack {
    static final int MAX_BYTES=LayeredScene.MAX_BYTES+4096;
    static byte[] encode(LayeredScene.Document scene)throws Exception {
        JSONObject document=scene.json();LayeredScene.parse(document);byte[] bytes=new JSONObject().put("format","nth-desktop-3d-scene-pack").put("version",1).put("scene",document).toString().getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IOException("Scene pack is too large");return bytes;
    }
    static LayeredScene.Document decode(byte[] bytes)throws Exception {
        if(bytes.length>MAX_BYTES)throw new IOException("Scene pack is too large");JSONObject pack=new JSONObject(new String(bytes,StandardCharsets.UTF_8));if(!"nth-desktop-3d-scene-pack".equals(pack.optString("format"))||pack.optInt("version")!=1)throw new JSONException("Not a supported scene pack");return LayeredScene.parse(pack.getJSONObject("scene"));
    }
    private ScenePack(){}
}
