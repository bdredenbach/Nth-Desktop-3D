package com.nth.desktop3d;

/** Stable scene order: append new modes so saved desktops keep their environment. */
final class SceneCatalog {
    static final int[] PREVIEWS={R.drawable.scene_spatial,R.drawable.scene_gravity,R.drawable.scene_holographic,R.drawable.scene_pond,R.drawable.scene_atrium,R.drawable.scene_aurora,R.drawable.scene_reef,R.drawable.scene_focus,R.drawable.scene_ice,R.drawable.scene_lava,R.drawable.scene_asteroids};
    static final String[] DESCRIPTIONS={"A photographic dusk lake, near shoreline and optical glass orb.","Tilt, toss, and play with your shortcuts.","A sunlit planet turning against distant stars.","Koi, floating leaves, and touch ripples.","Layered architectural glass. Sky reflections in motion.","Moving northern lights above moonlit alpine peaks.","Luminous jellyfish drifting through deep blue water.","Photographic glasses. A clearer city through moving lenses.","Translucent ice floes drifting on cold blue water.","Molten currents through deeply textured basalt.","Cratered asteroids drifting at different depths among stars."};
    static void drawPreview(android.graphics.drawable.Drawable image,android.graphics.Canvas canvas,float left,float top,float width,float height){
        float iw=Math.max(1,image.getIntrinsicWidth()),ih=Math.max(1,image.getIntrinsicHeight()),scale=Math.max(width/iw,height/ih);
        int x=Math.round(left+(width-iw*scale)/2),y=Math.round(top+(height-ih*scale)/2);
        image.setBounds(x,y,Math.round(x+iw*scale),Math.round(y+ih*scale));image.draw(canvas);
    }
    private SceneCatalog(){}
}
