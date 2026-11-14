package com.nth.desktop3d;

/** The same transform description drives the real workspace and its bounded preview. */
final class PageTransitions {
    static final String[] NAMES={"Cube","Carousel","Fold","Slide","Card stack","Tablet","Revolving door","Flip","Flip vertical","In and out","Accordion","Glass","Wipe","Zoom fade","Throw","None"};
    static final class Frame {
        float rx,ry,rz,tx,ty,sx=1,sy=1,alpha=1,px=.5f,py=.5f,clip=1;boolean fromRight;
    }
    static Frame frame(int effect,float amount,int direction,boolean incoming){
        Frame f=new Frame();float a=Math.max(0,Math.min(1,amount)),sign=incoming?-direction:direction;
        switch(effect){
            case 0:f.px=sign>0?0:1;f.ry=sign*a*78;f.sx=f.sy=1-a*.12f;f.alpha=1-a*.6f;break;
            case 1:f.ry=sign*a*45;f.tx=-sign*a*.35f;f.sx=f.sy=1-a*.12f;f.alpha=1-a*.6f;break;
            case 2:f.rx=sign*a*68;f.sx=f.sy=1-a*.12f;f.alpha=1-a*.6f;break;
            case 3:f.tx=-sign*a;break;
            case 4:f.tx=-sign*a*.18f;f.ty=a*.08f;f.rz=-sign*a*7;f.sx=f.sy=1-a*.24f;f.alpha=1-a*.75f;break;
            case 5:f.tx=-sign*a*.85f;f.ry=sign*a*28;f.px=sign>0?1:0;break;
            case 6:f.px=direction>0?0:1;f.ry=sign*a*90;f.tx=-sign*a*.3f;f.alpha=1-a*.3f;break;
            case 7:f.ry=sign*a*90;f.alpha=1-a*.25f;break;
            case 8:f.rx=-sign*a*90;f.alpha=1-a*.25f;break;
            case 9:f.sx=f.sy=1+(incoming?.55f:-.55f)*a;f.alpha=1-a;break;
            case 10:f.px=sign>0?0:1;f.sx=1-a*.88f;f.ry=sign*a*35;f.alpha=1-a*.4f;break;
            case 11:f.ry=-sign*a*18;f.tx=-sign*a*.1f;f.sx=f.sy=1-a*.18f;f.alpha=1-a*.95f;break;
            case 12:f.clip=1-a;f.fromRight=sign<0;break;
            case 13:f.sx=f.sy=1-a*.4f;f.alpha=1-a;break;
            case 14:f.tx=-sign*a*.9f;f.ty=-a*.45f;f.rz=-sign*a*35;f.alpha=1-a;break;
            default:break;
        }return f;
    }
    static void apply(android.view.View view,int effect,float amount,int direction,boolean incoming){
        Frame f=frame(effect,amount,direction,incoming);float w=view.getWidth(),h=view.getHeight();
        view.setPivotX(w*f.px);view.setPivotY(h*f.py);view.setRotationX(f.rx);view.setRotationY(f.ry);view.setRotation(f.rz);view.setTranslationX(w*f.tx);view.setTranslationY(h*f.ty);view.setScaleX(f.sx);view.setScaleY(f.sy);view.setAlpha(f.alpha);
        view.setClipBounds(f.clip<1?new android.graphics.Rect(f.fromRight?Math.round(w*(1-f.clip)):0,0,f.fromRight?(int)w:Math.round(w*f.clip),(int)h):null);
    }
    static void canvas(android.graphics.Canvas canvas,Frame f,float w,float h){
        float px=w*f.px,py=h*f.py;
        android.graphics.Camera camera=new android.graphics.Camera();android.graphics.Matrix matrix=new android.graphics.Matrix();camera.setLocation(0,0,-Math.max(1,w*6)/72f);camera.rotateX(f.rx);camera.rotateY(f.ry);camera.getMatrix(matrix);matrix.preTranslate(-px,-py);matrix.postTranslate(px,py);canvas.translate(w*f.tx,h*f.ty);canvas.concat(matrix);canvas.rotate(f.rz,px,py);canvas.scale(f.sx,f.sy,px,py);
        if(f.clip<1)canvas.clipRect(f.fromRight?w*(1-f.clip):0,0,f.fromRight?w:w*f.clip,h);
    }
}
