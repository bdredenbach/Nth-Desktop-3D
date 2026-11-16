package com.nth.desktop3d;

import java.util.LinkedHashMap;
import java.util.Map;

/** Fresh-install visual choices, without replacing explicit or legacy implicit preferences. */
final class VisualDefaults {
    static final int APP_SHAPE=0,FOLDER_SHAPE=1,LIGHT_STRENGTH=100,FOLDER_ANIMATION=7;
    static Map<String,Object> missing(Map<String,?> saved){
        boolean legacy=Boolean.TRUE.equals(saved.get("seeded"))||saved.containsKey("layout");
        Map<String,Object> result=new LinkedHashMap<>();
        put(result,saved,"folder_animation",legacy?3:FOLDER_ANIMATION);
        put(result,saved,"icon_shape",APP_SHAPE);put(result,saved,"folder_shape",legacy?0:FOLDER_SHAPE);
        put(result,saved,"scene_lighting",true);put(result,saved,"scene_light_strength",legacy?65:LIGHT_STRENGTH);
        return result;
    }
    private static void put(Map<String,Object> out,Map<String,?> saved,String key,Object value){if(!saved.containsKey(key))out.put(key,value);}
    private VisualDefaults(){}
}
