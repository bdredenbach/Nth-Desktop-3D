package com.nth.desktop3d;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class IconOpticsTest {
    private static final int N=96;
    private static int[] environment(){int[] p=new int[32*32];for(int y=0;y<32;y++)for(int x=0;x<32;x++)p[y*32+x]=0xFF000000|((50+x*5)<<16)|((80+y*5)<<8)|180;return p;}
    private static int[] render(int shape,boolean folder,int material,int transparency){return IconOptics.render(N,shape,folder,material,transparency,environment(),32,32);}
    @Test public void shapesKeepClearCornersAndDistinctSilhouettes(){
        int[] rounded=render(0,false,0,72),circle=render(1,false,0,72),square=render(2,false,0,72);
        for(int[] p:new int[][]{rounded,circle,square}){assertEquals(0,p[0]>>>24);assertEquals(0,p[N-1]>>>24);assertTrue(p[(N/2)*N+N/2]>>>24>0);}
        assertTrue((square[8*N+8]>>>24)>(rounded[8*N+8]>>>24));
        assertTrue((rounded[14*N+14]>>>24)>(circle[14*N+14]>>>24));
    }
    @Test public void foldersKeepIdenticalShapeAndHitGeometry(){
        for(int shape=0;shape<3;shape++){int[] app=render(shape,false,0,72),folder=render(shape,true,0,72);boolean tintDiffers=false;for(int i=0;i<app.length;i++){assertEquals(app[i]>>>24,folder[i]>>>24);if(app[i]!=folder[i])tintDiffers=true;}assertTrue(tintDiffers);}
    }
    @Test public void transparencyPreservesThePolishedRim(){
        int[] opaque=render(0,false,0,0),clear=render(0,false,0,100);int center=N/2*N+N/2;
        assertEquals(255,opaque[center]>>>24);assertEquals(0,clear[center]>>>24);
        assertTrue((opaque[center]>>>24)>(clear[center]>>>24)+100);
        int max=0;for(int pixel:clear)max=Math.max(max,pixel>>>24);assertTrue(max>(clear[center]>>>24)+100);
    }
    @Test public void photographicEnvironmentChangesTheReflection(){
        int[] dark=new int[16],bright=new int[16];Arrays.fill(dark,0xFF101820);Arrays.fill(bright,0xFFD5E8F5);
        int[] a=IconOptics.render(N,1,false,0,72,dark,4,4),b=IconOptics.render(N,1,false,0,72,bright,4,4);int changed=0;
        for(int i=0;i<a.length;i++){assertEquals(a[i]>>>24,b[i]>>>24);if(a[i]!=b[i])changed++;}assertTrue(changed>N*N/2);
    }
    @Test public void themesChangeTheFaceWithoutChangingItsOutline(){
        int[] first=render(0,false,0,72);for(int material=1;material<4;material++){int[] next=render(0,false,material,72);assertFalse(Arrays.equals(first,next));for(int i=0;i<first.length;i++)assertEquals(first[i]>>>24,next[i]>>>24);}
    }
    @Test public void renderingIsDeterministicAndDoesNotMutateItsEnvironment(){
        int[] env=environment(),original=env.clone();assertArrayEquals(IconOptics.render(N,0,false,0,72,env,32,32),IconOptics.render(N,0,false,0,72,env,32,32));assertArrayEquals(original,env);
    }
}
