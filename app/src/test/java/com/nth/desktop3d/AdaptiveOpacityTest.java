package com.nth.desktop3d;
import org.junit.Test;
import static org.junit.Assert.*;
public class AdaptiveOpacityTest {
    @Test public void emptyWallpaperIsTransparent(){assertEquals(0,AdaptiveOpacity.coverage(20,20,new float[0][],40),0);}
    @Test public void objectAndFeatherHaveDifferentOpacity(){float[][] b={{10,10,30,30}};assertEquals(1,AdaptiveOpacity.coverage(20,20,b,20),0);assertEquals(.5f,AdaptiveOpacity.coverage(40,20,b,20),.0001f);assertEquals(0,AdaptiveOpacity.coverage(60,20,b,20),0);}
    @Test public void overlappingObjectsNeverExceedOne(){assertEquals(1,AdaptiveOpacity.coverage(20,20,new float[][]{{0,0,30,30},{10,10,40,40}},40),0);}
    @Test public void cornersFadeSmoothlyAndSymmetrically(){float[][] b={{0,0,20,20}};assertEquals(AdaptiveOpacity.coverage(-8,-8,b,30),AdaptiveOpacity.coverage(28,28,b,30),.00001f);assertTrue(AdaptiveOpacity.coverage(20.1f,10,b,20)>.999f);assertTrue(AdaptiveOpacity.coverage(39.9f,10,b,20)<.001f);}
}
