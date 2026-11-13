package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class SceneMathTest {
    @Test public void pageWrapsBothDirections(){assertEquals(2,SceneMath.wrap(-1,3));assertEquals(0,SceneMath.wrap(3,3));}
    @Test public void physicsReturnsOffscreenObjectAndReflectsVelocity(){float[] b=SceneMath.constrain(-5,120,-20,30,100,100,20);assertArrayEquals(new float[]{0,80,10,-15},b,.001f);}
    @Test public void smallViewportCannotCreateNegativeBounds(){float[] b=SceneMath.constrain(10,10,5,5,10,10,50);assertEquals(0,b[0],0);assertEquals(0,b[1],0);}
    @Test public void smoothingIsIndependentOfFrameRate(){float sixty=0,thirty=0;for(int i=0;i<60;i++)sixty=SceneMath.smooth(sixty,1,1f/60,3);for(int i=0;i<30;i++)thirty=SceneMath.smooth(thirty,1,1f/30,3);assertEquals(sixty,thirty,.00001f);}
    @Test public void extremeTiltIsBounded(){assertEquals(1,SceneMath.tilt(100),0);assertEquals(-1,SceneMath.tilt(-100),0);}
    @Test public void physicsProjectsOutOfAllAdjacentWidgets(){
        float[][] boxes={{0,0,160,160},{160,0,320,160},{100,200,320,300}};
        for(int x=0;x<=280;x+=20)for(int y=0;y<=360;y+=20){
            float[] result=SceneMath.vacantPosition(x,y,40,40,320,400,boxes);assertNotNull(result);
            assertFalse(SceneMath.overlapsObstacle(result[0],result[1],40,40,boxes));
            assertTrue(result[0]>=0&&result[1]>=0&&result[0]+40<=320&&result[1]+40<=400);
        }
    }
    @Test public void fullyCoveredPhysicsSpaceDoesNotPlaceAnIconUnderAWidget(){assertNull(SceneMath.vacantPosition(10,10,40,40,100,100,new float[][]{{0,0,100,100}}));}
}
