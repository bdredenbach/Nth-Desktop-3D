package com.nth.desktop3d;

/** Spatial opacity envelope: solid over content, smooth falloff to empty space. */
public final class AdaptiveOpacity {
    private AdaptiveOpacity(){}
    public static float coverage(float x,float y,float[][] boxes,float feather){
        float result=0;float edge=Math.max(1,feather);
        for(float[] b:boxes){float dx=Math.max(Math.max(b[0]-x,0),x-b[2]);float dy=Math.max(Math.max(b[1]-y,0),y-b[3]);float distance=(float)Math.hypot(dx,dy);float t=Math.max(0,1-distance/edge);result=Math.max(result,t*t*(3-2*t));if(result>=1)return 1;}
        return result;
    }
}
