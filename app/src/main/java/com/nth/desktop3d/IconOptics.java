package com.nth.desktop3d;

/** Platform-independent optical shell rasterizer, shared by Android and material checks.
 * Shape IDs: rounded square, circle, square. Pixels use straight ARGB.
 */
final class IconOptics {
    private IconOptics(){}
    private static float clamp(float v){return Math.max(0,Math.min(1,v));}
    private static float mix(float a,float b,float t){return a+(b-a)*t;}
    private static float distance(float x,float y,int shape){
        if(shape==1)return (float)Math.hypot(x,y)-.88f;
        float r=shape==2?.022f:.40f,qx=Math.abs(x)-.88f+r,qy=Math.abs(y)-.88f+r;
        return (float)Math.hypot(Math.max(qx,0),Math.max(qy,0))+Math.min(Math.max(qx,qy),0)-r;
    }
    private static int sample(int[] env,int w,int h,float u,float v){
        float px=clamp(u)*(w-1),py=clamp(v)*(h-1);int x=(int)px,y=(int)py;
        int a=env[y*w+x],b=env[y*w+Math.min(w-1,x+1)],c=env[Math.min(h-1,y+1)*w+x],d=env[Math.min(h-1,y+1)*w+Math.min(w-1,x+1)];
        int color=0xFF000000;for(int shift=0;shift<=16;shift+=8){float upper=mix(a>>shift&255,b>>shift&255,px-x),lower=mix(c>>shift&255,d>>shift&255,px-x);color|=Math.round(mix(upper,lower,py-y))<<shift;}return color;
    }
    static int[] render(int size,int shape,boolean folder,int material,int transparency,int[] env,int ew,int eh){
        if(size<8||size>384||shape<0||shape>2||ew<1||eh<1||env==null||env.length!=ew*eh)throw new IllegalArgumentException("Invalid optical shell inputs");
        material=Math.max(0,Math.min(3,material));
        float[] red={.09f,.21f,.17f,.035f},green={.22f,.28f,.16f,.21f},blue={.29f,.31f,.30f,.23f};
        float tr=red[material]+(folder?.045f:0),tg=green[material]+(folder?.012f:0),tb=blue[material]+(folder?.025f:0);
        float core=1-Math.max(0,Math.min(100,transparency))/100f,aa=2.0f/size;
        int[] pixels=new int[size*size];
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            float px=(x+.5f)/size*2-1,py=(y+.5f)/size*2-1,d=distance(px,py,shape);
            float coverage=clamp(.5f-d/aa);
            if(coverage==0){
                // Short dark extrusion follows the same silhouette and stays within the tile.
                float back=distance(px-.025f,py-.042f,shape);
                float a=clamp(.5f-back/aa)*.40f;
                pixels[y*size+x]=(Math.round(a*255)<<24)|0x08151E;continue;
            }
            float depth=Math.max(0,-d),bevel=clamp(depth/.15f),curve=(float)Math.cos(bevel*Math.PI*.5);
            float dx=distance(px+.001f,py,shape)-distance(px-.001f,py,shape),dy=distance(px,py+.001f,shape)-distance(px,py-.001f,shape);
            float length=Math.max(.00001f,(float)Math.hypot(dx,dy));
            float nx=dx/length*curve+px*.045f*(1-curve),ny=dy/length*curve+py*.045f*(1-curve);
            float nz=(float)Math.sqrt(Math.max(.001f,1-nx*nx-ny*ny));
            float rx=2*nx*nz,ry=2*ny*nz;
            int reflected=sample(env,ew,eh,.5f+rx*.46f,.45f+ry*.46f);
            float er=(reflected>>16&255)/255f,eg=(reflected>>8&255)/255f,eb=(reflected&255)/255f;
            float fresnel=.045f+.955f*(float)Math.pow(1-nz,5);
            float softbox=(float)Math.exp(-Math.pow((rx+.40f)/.115f,4)-Math.pow((ry+.22f)/.66f,4));
            float narrow=(float)Math.exp(-Math.pow((rx-.62f)/.035f,2)-Math.pow((ry-.18f)/.55f,4))*.34f;
            float reflect=.12f+curve*.48f+fresnel*.30f;
            float r=mix(tr,er,reflect),g=mix(tg,eg,reflect),b=mix(tb,eb,reflect);
            float spec=softbox*.80f+narrow;
            // Smooth inner chamfer and lower absorption replace the old broken neon outline.
            float inner=(float)Math.exp(-Math.pow((depth-.143f)/.018f,2))*.14f;
            float absorb=1-inner-Math.max(0,py)*curve*.15f;
            r=r*absorb+spec;g=g*absorb+spec*.97f;b=b*absorb+spec*.94f;
            float a=mix(.92f,core,clamp(bevel*1.12f));a=Math.max(a,spec*.87f);
            pixels[y*size+x]=(Math.round(clamp(a*coverage)*255)<<24)|(Math.round(clamp(r)*255)<<16)|(Math.round(clamp(g)*255)<<8)|Math.round(clamp(b)*255);
        }
        return pixels;
    }
}
