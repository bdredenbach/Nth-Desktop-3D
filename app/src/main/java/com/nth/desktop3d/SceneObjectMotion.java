package com.nth.desktop3d;

/** Bounded, deterministic object paths shared by editor and wallpaper. Off is exactly still. */
final class SceneObjectMotion {
    static final String[] NAMES={"Still","Float","Orbit","Drift"};
    static final class Pose {final float x,y,angle;Pose(float x,float y,float angle){this.x=x;this.y=y;this.angle=angle;}}
    static Pose at(int mode,float amount,float speed,float seconds,boolean enabled){
        if(!enabled||mode<1||mode>3||!Float.isFinite(seconds)||!Float.isFinite(amount)||!Float.isFinite(speed))return new Pose(0,0,0);
        float a=Math.max(0,Math.min(1,amount))*.12f,t=Math.max(0,seconds)*(.12f+Math.max(0,Math.min(1,speed))*.8f);
        if(mode==1)return new Pose((float)Math.sin(t*.43f)*a*.18f,(float)Math.sin(t)*a*.55f,(float)Math.sin(t*.7f)*a*20);
        if(mode==2)return new Pose((float)Math.cos(t)*a,(float)Math.sin(t)*a*.65f,(float)Math.sin(t)*a*45);
        return new Pose((float)Math.sin(t*.37f)*a,(float)Math.sin(t*.23f+1.3f)*a*.65f,(float)Math.sin(t*.29f)*a*25);
    }
    private SceneObjectMotion(){}
}
