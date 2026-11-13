package com.nth.desktop3d;

/** Pure scene calculations shared by rendering, input and unit tests. */
public final class SceneMath {
    private SceneMath() {}
    public static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    public static float smooth(float from, float to, float seconds, float rate) {
        return from + (to - from) * (1f - (float) Math.exp(-Math.max(0, seconds) * rate));
    }
    public static int wrap(int page, int count) { return ((page % count) + count) % count; }
    public static float[] constrain(float x, float y, float vx, float vy, float width, float height, float size) {
        float maxX = Math.max(0, width - size), maxY = Math.max(0, height - size);
        if (x < 0) { x = 0; vx = Math.abs(vx) * .5f; }
        if (x > maxX) { x = maxX; vx = -Math.abs(vx) * .5f; }
        if (y < 0) { y = 0; vy = Math.abs(vy) * .5f; }
        if (y > maxY) { y = maxY; vy = -Math.abs(vy) * .5f; }
        return new float[]{x, y, vx, vy};
    }
    public static float tilt(float radians) { return clamp(radians / .45f, -1f, 1f); }
    public static boolean overlapsObstacle(float x,float y,float w,float h,float[][] boxes){
        for(float[] box:boxes)if(x<box[2]&&x+w>box[0]&&y<box[3]&&y+h>box[1])return true;
        return false;
    }
    /** Project against all widget obstacles together; a push from one must not enter another. */
    public static float[] vacantPosition(float x,float y,float w,float h,float width,float height,float[][] boxes){
        if(w>width||h>height)return null;
        x=clamp(x,0,width-w);y=clamp(y,0,height-h);
        if(!overlapsObstacle(x,y,w,h,boxes))return new float[]{x,y};
        float[] xs=new float[boxes.length*2+3],ys=new float[boxes.length*2+3];
        xs[0]=x;xs[1]=0;xs[2]=width-w;ys[0]=y;ys[1]=0;ys[2]=height-h;
        for(int i=0;i<boxes.length;i++){xs[3+i*2]=boxes[i][0]-w;xs[4+i*2]=boxes[i][2];ys[3+i*2]=boxes[i][1]-h;ys[4+i*2]=boxes[i][3];}
        float best=Float.MAX_VALUE;float[] result=null;
        for(float cx:xs)for(float cy:ys)if(cx>=0&&cy>=0&&cx+w<=width&&cy+h<=height&&!overlapsObstacle(cx,cy,w,h,boxes)){
            float distance=(cx-x)*(cx-x)+(cy-y)*(cy-y);if(distance<best){best=distance;result=new float[]{cx,cy};}
        }
        return result;
    }
}
