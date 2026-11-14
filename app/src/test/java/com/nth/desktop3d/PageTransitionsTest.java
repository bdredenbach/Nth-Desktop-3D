package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class PageTransitionsTest {
    @Test public void allEffectsRestoreAnUntransformedPageAtTheirEndpoint(){for(int effect=0;effect<PageTransitions.NAMES.length;effect++)for(int direction:new int[]{-1,1})for(boolean incoming:new boolean[]{false,true}){PageTransitions.Frame f=PageTransitions.frame(effect,0,direction,incoming);assertEquals(0,f.rx,0);assertEquals(0,f.ry,0);assertEquals(0,f.rz,0);assertEquals(0,f.tx,0);assertEquals(0,f.ty,0);assertEquals(1,f.sx,0);assertEquals(1,f.sy,0);assertEquals(1,f.alpha,0);assertEquals(1,f.clip,0);}}
    @Test public void everyIntermediateFrameIsFiniteAndDrawable(){for(int effect=0;effect<PageTransitions.NAMES.length;effect++)for(int step=0;step<=100;step++)for(boolean incoming:new boolean[]{false,true}){PageTransitions.Frame f=PageTransitions.frame(effect,step/100f,1,incoming);for(float value:new float[]{f.rx,f.ry,f.rz,f.tx,f.ty,f.sx,f.sy,f.alpha,f.clip})assertTrue(Float.isFinite(value));assertTrue(f.sx>0&&f.sy>0&&f.alpha>=0&&f.alpha<=1&&f.clip>=0&&f.clip<=1);}}
    @Test public void wipeAndFlipUseDifferentGeometryAndNoneStaysStill(){assertEquals(.5f,PageTransitions.frame(12,.5f,1,false).clip,0);assertEquals(45,PageTransitions.frame(7,.5f,1,false).ry,0);assertEquals(-45,PageTransitions.frame(8,.5f,1,false).rx,0);assertEquals(0,PageTransitions.frame(15,1,1,false).tx,0);assertEquals(1,PageTransitions.frame(15,1,1,false).alpha,0);}
}
