package com.nth.desktop3d;

/** Stable scene order: append new modes so saved desktops keep their environment. */
final class SceneCatalog {
    static final int[] PREVIEWS={R.drawable.scene_spatial,R.drawable.scene_gravity,R.drawable.scene_holographic,R.drawable.scene_pond,R.drawable.scene_atrium,R.drawable.scene_aurora,R.drawable.scene_reef};
    static final String[] DESCRIPTIONS={"Layered mountains. Sculpted light.","Tilt, toss, and play with your shortcuts.","A quiet orbit through a field of stars.","Koi, floating leaves, and touch ripples.","Polished monoliths above a perspective stone floor.","Flowing northern lights above a still alpine lake.","Luminous jellyfish drifting through deep blue water."};
    static void drawPreview(android.graphics.drawable.Drawable image,android.graphics.Canvas canvas,float left,float top,float width,float height){
        float iw=Math.max(1,image.getIntrinsicWidth()),ih=Math.max(1,image.getIntrinsicHeight()),scale=Math.max(width/iw,height/ih);
        int x=Math.round(left+(width-iw*scale)/2),y=Math.round(top+(height-ih*scale)/2);
        image.setBounds(x,y,Math.round(x+iw*scale),Math.round(y+ih*scale));image.draw(canvas);
    }
    private SceneCatalog(){}
}
