package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class SceneObjectMotionTest {
    @Test public void disabledAndStillPathsNeverMove(){for(int mode=0;mode<=3;mode++)for(float time:new float[]{0,1,200,10000}){SceneObjectMotion.Pose off=SceneObjectMotion.at(mode,1,1,time,false);assertEquals(0,off.x,0);assertEquals(0,off.y,0);assertEquals(0,off.angle,0);}assertEquals(0,SceneObjectMotion.at(0,1,1,100,true).x,0);}
    @Test public void allPathsRemainBoundedOverLongSessions(){for(int mode=1;mode<=3;mode++)for(int t=0;t<72000;t+=17){SceneObjectMotion.Pose p=SceneObjectMotion.at(mode,1,1,t,true);assertTrue(Float.isFinite(p.x)&&Float.isFinite(p.y)&&Float.isFinite(p.angle));assertTrue(Math.abs(p.x)<=.12001f&&Math.abs(p.y)<=.07801f&&Math.abs(p.angle)<=5.4001f);}}
    @Test public void orbitFollowsAnEllipseInsteadOfDriftingAway(){for(int t=0;t<100;t++){SceneObjectMotion.Pose p=SceneObjectMotion.at(2,.5f,.7f,t,true);assertEquals(1,Math.pow(p.x/.06f,2)+Math.pow(p.y/.039f,2),.00001);}}
    @Test public void speedAndPathChoicesProduceDifferentPositions(){SceneObjectMotion.Pose a=SceneObjectMotion.at(1,.5f,.2f,8,true),b=SceneObjectMotion.at(1,.5f,.9f,8,true),c=SceneObjectMotion.at(3,.5f,.2f,8,true);assertTrue(Math.abs(a.y-b.y)>.001f);assertTrue(Math.abs(a.x-c.x)+Math.abs(a.y-c.y)>.001f);}
    @Test public void zeroAmountAndMalformedInputsStayStill(){for(int mode=1;mode<=3;mode++){SceneObjectMotion.Pose p=SceneObjectMotion.at(mode,0,.5f,12,true);assertEquals(0,p.x,0);assertEquals(0,p.y,0);}assertEquals(0,SceneObjectMotion.at(2,Float.NaN,.5f,10,true).x,0);assertEquals(0,SceneObjectMotion.at(2,1,Float.POSITIVE_INFINITY,10,true).x,0);assertEquals(0,SceneObjectMotion.at(2,1,1,Float.NaN,true).x,0);}
}
