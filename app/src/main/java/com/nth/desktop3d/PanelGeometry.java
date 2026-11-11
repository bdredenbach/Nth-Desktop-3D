package com.nth.desktop3d;

/** Panel bounds in the unobscured window, including asymmetric cutouts and the IME. */
final class PanelGeometry {
    static final class Bounds {
        final int left,top,width,height;
        Bounds(int left,int top,int width,int height){this.left=left;this.top=top;this.width=width;this.height=height;}
    }
    static Bounds place(int windowWidth,int windowHeight,int leftInset,int topInset,int rightInset,int bottomInset,int maxWidth,int maxHeight,int gap,int dockOffset){
        int safeWidth=Math.max(1,windowWidth-leftInset-rightInset);
        int safeHeight=Math.max(1,windowHeight-topInset-bottomInset);
        int width=Math.min(maxWidth,Math.max(1,safeWidth-gap*2));
        int height=Math.min(maxHeight,Math.max(1,safeHeight-gap*2));
        int left=leftInset+Math.max(0,(safeWidth-width)/2);
        int top=topInset+Math.max(0,(safeHeight-height)/2);
        if(dockOffset>0){height=Math.min(maxHeight,Math.max(1,safeHeight-gap-dockOffset));top=Math.max(topInset+gap,windowHeight-bottomInset-dockOffset-height);}
        return new Bounds(left,top,width,height);
    }
}
