package com.nth.desktop3d;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class VisualDefaultsTest {
    @Test public void firstInitializationUsesTheAcceptedLightingAndShapes(){
        Map<String,Object> defaults=VisualDefaults.missing(Collections.emptyMap());
        assertEquals(0,defaults.get("icon_shape"));assertEquals(1,defaults.get("folder_shape"));assertEquals(true,defaults.get("scene_lighting"));assertEquals(100,defaults.get("scene_light_strength"));assertEquals(7,defaults.get("folder_animation"));
    }
    @Test public void explicitChoicesIncludingOffAndZeroSurvive(){
        Map<String,Object> saved=new HashMap<>();saved.put("icon_shape",2);saved.put("folder_shape",0);saved.put("scene_lighting",false);saved.put("scene_light_strength",0);saved.put("folder_animation",6);
        assertTrue(VisualDefaults.missing(saved).isEmpty());assertEquals(5,saved.size());
    }
    @Test public void seededOlderInstallKeepsItsImplicitAppearance(){
        Map<String,Object> defaults=VisualDefaults.missing(Collections.singletonMap("seeded",true));
        assertEquals(0,defaults.get("folder_shape"));assertEquals(65,defaults.get("scene_light_strength"));assertEquals(3,defaults.get("folder_animation"));
    }
    @Test public void restoredLayoutCountsAsExistingEvenBeforeSeeding(){
        Map<String,Object> saved=new HashMap<>();saved.put("layout","[]");saved.put("folder_shape",2);
        Map<String,Object> defaults=VisualDefaults.missing(saved);assertFalse(defaults.containsKey("folder_shape"));assertEquals(65,defaults.get("scene_light_strength"));assertEquals(3,defaults.get("folder_animation"));assertEquals(2,saved.size());
    }
}
