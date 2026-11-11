package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class SceneMathTest {
    @Test public void pageWrapsBothDirections(){assertEquals(2,SceneMath.wrap(-1,3));assertEquals(0,SceneMath.wrap(3,3));}
    @Test public void physicsReturnsOffscreenObjectAndReflectsVelocity(){float[] b=SceneMath.constrain(-5,120,-20,30,100,100,20);assertArrayEquals(new float[]{0,80,10,-15},b,.001f);}
    @Test public void smallViewportCannotCreateNegativeBounds(){float[] b=SceneMath.constrain(10,10,5,5,10,10,50);assertEquals(0,b[0],0);assertEquals(0,b[1],0);}
    @Test public void smoothingIsIndependentOfFrameRate(){float sixty=0,thirty=0;for(int i=0;i<60;i++)sixty=SceneMath.smooth(sixty,1,1f/60,3);for(int i=0;i<30;i++)thirty=SceneMath.smooth(thirty,1,1f/30,3);assertEquals(sixty,thirty,.00001f);}
    @Test public void extremeTiltIsBounded(){assertEquals(1,SceneMath.tilt(100),0);assertEquals(-1,SceneMath.tilt(-100),0);}
}
