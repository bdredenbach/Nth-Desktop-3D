package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class SceneLightTest {
    private SceneLight.Frame frame(int mode,boolean moving,boolean steady,float time){return SceneLight.frame(mode,true,65,moving,steady,time,.4f,-.2f);}
    @Test public void allBuiltInScenesHaveTheirOwnLighting(){
        java.util.Set<Integer> signatures=new java.util.HashSet<>();for(int mode=0;mode<11;mode++){SceneLight.Frame f=frame(mode,true,false,20);assertTrue(f.active());assertEquals(mode,f.mode);assertTrue(signatures.add(f.signature()));}
    }
    @Test public void disabledOrZeroStrengthRestoresUnmodifiedMaterial(){
        for(SceneLight.Frame f:new SceneLight.Frame[]{SceneLight.frame(5,false,65,true,false,20,1,1),SceneLight.frame(5,true,0,true,false,20,1,1),frame(-1,true,false,20),frame(11,true,false,20)}){assertFalse(f.active());assertEquals(0x9A102030,f.tint(0x9A102030,1,.5f));assertEquals(-1,f.signature());}
    }
    @Test public void sceneTintNeverChangesMenuOpacity(){
        for(int mode=0;mode<11;mode++)for(int alpha=0;alpha<256;alpha+=17){int color=alpha<<24|0x081728;assertEquals(alpha,frame(mode,true,false,20).tint(color,.3f,.5f)>>>24);}
    }
    @Test public void auroraMovesButPausesWithAmbientOffOrReducedMotion(){
        assertFalse(frame(5,true,false,2).same(frame(5,true,false,30)));
        assertTrue(frame(5,false,false,2).same(frame(5,false,false,30)));
        assertTrue(frame(5,true,true,2).same(frame(5,true,true,30)));
    }
    @Test public void steadyLightingIgnoresTiltAndTime(){
        assertTrue(SceneLight.frame(5,true,65,true,true,2,1,1).same(SceneLight.frame(5,true,65,true,true,100,-1,-1)));
    }
    @Test public void sunsetAndLavaAreWarmAndAuroraIsGreen(){
        for(int mode:new int[]{0,7,9}){int c=frame(mode,false,true,0).primary;assertTrue((c>>16&255)>(c&255));}
        int aurora=frame(5,false,true,0).primary;assertTrue((aurora>>8&255)>(aurora>>16&255));
    }
    @Test public void invalidTimeAndExtremeInputCannotProduceUnboundedLight(){
        for(float time:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-1,Float.MAX_VALUE}){SceneLight.Frame f=SceneLight.frame(5,true,10000,true,false,time,Float.NaN,10000);assertEquals(1,f.amount,0);assertTrue(Float.isFinite(f.x)&&Float.isFinite(f.y)&&Float.isFinite(f.pulse));assertTrue(Math.abs(f.x)<.5f&&Math.abs(f.y)<.3f);}
    }
}
