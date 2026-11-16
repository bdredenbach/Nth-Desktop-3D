package com.nth.desktop3d;

/** Deterministic, reversible phases shared by the native folder and its preview. */
final class FolderPose {
    static final class Item {
        final float progress,alpha,scale,rotation,tilt;
        Item(float progress,int index,int columns){this.progress=progress;alpha=progress;scale=.64f+.36f*progress;rotation=((index%Math.max(1,columns))-(Math.max(1,columns)-1)*.5f)*7*(1-progress);tilt=-25*(1-progress);}
    }
    static float clamp(float p){return Float.isFinite(p)?Math.max(0,Math.min(1,p)):0;}
    static float ease(float from,float to,float value){float p=clamp((value-from)/(to-from));return p*p*(3-2*p);}
    static float expand(float p){return ease(0,.72f,p);}
    static float panelAlpha(float p){return ease(.10f,.48f,p);}
    static float lidAngle(float p){return -82*ease(.18f,.80f,p);}
    static float lidAlpha(float p){return 1-ease(.68f,.98f,p);}
    static float snapshotAlpha(float p){return 1-ease(.02f,.25f,p);}
    static Item item(float p,int index,int columns){float delay=Math.min(17,Math.max(0,index))*.012f;return new Item(ease(.18f+delay,.76f+delay,p),index,columns);}
    static float mix(float a,float b,float p){return a+(b-a)*clamp(p);}
    private FolderPose(){}
}
