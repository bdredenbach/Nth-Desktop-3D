package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class FolderPoseTest {
    @Test public void closedFolderHasNoContentAndAnUnturnedFront(){
        assertEquals(0,FolderPose.expand(0),0);assertEquals(0,FolderPose.panelAlpha(0),0);assertEquals(0,FolderPose.lidAngle(0),0);assertEquals(1,FolderPose.lidAlpha(0),0);
        for(int i=0;i<24;i++)assertEquals(0,FolderPose.item(0,i,4).alpha,0);
    }
    @Test public void fullOpenLeavesEveryNativeIconUntransformed(){
        assertEquals(1,FolderPose.expand(1),0);assertEquals(1,FolderPose.panelAlpha(1),0);assertEquals(0,FolderPose.lidAlpha(1),0);
        for(int i=0;i<1000;i++){FolderPose.Item item=FolderPose.item(1,i,6);assertEquals(1,item.alpha,0);assertEquals(1,item.scale,0);assertEquals(0,item.rotation,0);assertEquals(0,item.tilt,0);}
    }
    @Test public void staggerIsBoundedEvenForLargeFolders(){
        assertTrue(FolderPose.item(.5f,0,3).progress>FolderPose.item(.5f,8,3).progress);
        assertEquals(FolderPose.item(.6f,17,3).progress,FolderPose.item(.6f,999,3).progress,0);
    }
    @Test public void expansionDoesNotOvershootOrJumpBackwards(){
        float prior=0;for(int i=0;i<=100;i++){float e=FolderPose.expand(i/100f);assertTrue(e>=prior&&e<=1);prior=e;}
        assertEquals(0,FolderPose.expand(-1),0);assertEquals(1,FolderPose.expand(2),0);
    }
    @Test public void iconsFanSymmetricallyWithoutTiltingSingleColumnSideways(){
        assertTrue(FolderPose.item(.3f,0,3).rotation<0);assertTrue(FolderPose.item(.3f,2,3).rotation>0);assertEquals(0,FolderPose.item(.3f,1,3).rotation,0);assertEquals(0,FolderPose.item(.3f,0,1).rotation,0);
    }
    @Test public void invalidProgressIsFiniteAndConservative(){
        for(float p:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){assertEquals(0,FolderPose.clamp(p),0);assertTrue(Float.isFinite(FolderPose.lidAngle(p)));assertTrue(Float.isFinite(FolderPose.item(p,0,0).rotation));}
    }
}
