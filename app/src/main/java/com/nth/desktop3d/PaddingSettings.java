package com.nth.desktop3d;

import android.content.SharedPreferences;
import org.json.*;

/** Signed spacing preserves the old neutral appearance and safely reaches the screen edge. */
final class PaddingSettings {
    static int inset(int standard,int adjustment){return Math.max(0,standard+Math.max(-50,Math.min(50,adjustment)));}
    static void migrate(SharedPreferences prefs){if(!prefs.getBoolean("padding_offsets_081",false))prefs.edit().putInt("dock_padding",Math.max(4,Math.min(14,prefs.getInt("dock_padding",7)))-7).putBoolean("padding_offsets_081",true).apply();}
    static JSONObject normalize(JSONObject input)throws JSONException {
        JSONObject copy=new JSONObject(input.toString());if(copy.has("paddingOffsets")&&!(copy.get("paddingOffsets") instanceof Boolean))throw new JSONException("Invalid padding format");
        if(!copy.optBoolean("paddingOffsets",false)){JSONObject values=copy.getJSONObject("values");if(values.has("dock_padding")){Object value=values.get("dock_padding");if(!(value instanceof Number)||((Number)value).doubleValue()!=((Number)value).intValue()||((Number)value).intValue()<4||((Number)value).intValue()>14)throw new JSONException("Invalid legacy dock padding");values.put("dock_padding",((Number)value).intValue()-7);}}
        return copy.put("paddingOffsets",true);
    }
}
