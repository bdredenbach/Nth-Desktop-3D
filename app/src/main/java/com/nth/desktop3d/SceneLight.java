package com.nth.desktop3d;

/** Shared, deterministic light from the built-in environment, without Android dependencies. */
final class SceneLight {
    private static final int[] PRIMARY={0xFFFFC49A,0xFFB9A4FA,0xFFD8EBFF,0xFF39E3D0,0xFFB5DFFF,0xFF4AE777,0xFF81CFEF,0xFFF2D3A5,0xFFB5EDFA,0xFFFF8C1A,0xFF182027};
    private static final int[] SECONDARY={0xFFA2D4DF,0xFF91C8ED,0xFF8CAFD8,0xFF268F9E,0xFF73BFFA,0xFF1E9656,0xFFB5A8EE,0xFF91C5E8,0xFF8DBAD5,0xFFFFBA46,0xFF444D53};
    static final class Frame {
        final int mode,primary,secondary;final float amount,x,y,pulse;
        Frame(int mode,int primary,int secondary,float amount,float x,float y,float pulse){this.mode=mode;this.primary=primary;this.secondary=secondary;this.amount=amount;this.x=x;this.y=y;this.pulse=pulse;}
        boolean active(){return mode>=0&&amount>0;}
        int signature(){return active()?mode*101+Math.round(amount*100):-1;}
        int tint(int argb,float scale,float position){
            if(!active())return argb;
            float band=clamp(.5f+position*.5f+x*.65f),weight=clamp(amount*scale*pulse);
            return blend(argb,blend(primary,secondary,band),weight);
        }
        boolean dominantPalette(){return mode==3||mode==4||mode==5||mode==9||mode==10;}
        int surface(int argb,float position){return tint(argb,dominantPalette()?.68f:.30f,position);}
        boolean same(Frame other){return other!=null&&signature()==other.signature()&&Math.round(x*256)==Math.round(other.x*256)&&Math.round(y*256)==Math.round(other.y*256)&&Math.round(pulse*256)==Math.round(other.pulse*256);}
    }
    static Frame frame(int mode,boolean enabled,int strength,boolean moving,boolean steady,float seconds,float tiltX,float tiltY){
        if(!enabled||strength<=0||mode<0||mode>=PRIMARY.length)return new Frame(-1,0,0,0,0,0,1);
        float t=moving&&!steady&&Float.isFinite(seconds)?Math.max(0,seconds):0;
        float x=0,y=0,pulse=1;
        if(t!=0){
            if(mode==5){x=(float)(Math.sin(t*.16)*.17+Math.sin(t*.07)*.10);y=(float)Math.sin(t*.12)*.09f;pulse=(float)(.88+.10*Math.sin(t*.21)+.02*Math.sin(t*.28));}
            else if(mode==9){x=(float)Math.sin(t*.12)*.14f;y=(float)Math.sin(t*.08)*.05f;pulse=(float)(.94+.06*Math.sin(t*.61));}
            else{x=(float)Math.sin(t*.10+mode)*.09f;y=(float)Math.sin(t*.075+mode*.3)*.05f;pulse=(float)(.96+.04*Math.sin(t*.15));}
        }
        if(!steady){x+=Math.max(-1,Math.min(1,Float.isFinite(tiltX)?tiltX:0))*.14f;y+=Math.max(-1,Math.min(1,Float.isFinite(tiltY)?tiltY:0))*.10f;}
        return new Frame(mode,PRIMARY[mode],SECONDARY[mode],clamp(strength/100f),x,y,pulse);
    }
    static int blend(int base,int light,float weight){weight=clamp(weight);int out=base&0xFF000000;for(int shift=0;shift<=16;shift+=8){int a=base>>shift&255,b=light>>shift&255;out|=Math.round(a+(b-a)*weight)<<shift;}return out;}
    private static float clamp(float value){return Math.max(0,Math.min(1,value));}
    private SceneLight(){}
}
