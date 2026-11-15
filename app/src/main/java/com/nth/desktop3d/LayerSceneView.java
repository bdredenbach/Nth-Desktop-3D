package com.nth.desktop3d;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import java.util.*;

/** Shared creator/live renderer. No sensor motion or continuous rendering when reduced motion is on. */
@android.annotation.SuppressLint("ViewConstructor")
final class LayerSceneView extends View {
    private final Paint paint=new Paint(3);private final Map<String,Bitmap> images=new HashMap<>();
    private final Drawable crystal;private Bitmap background;private LayeredScene.Document document;
    private float tiltX,tiltY;private boolean moving=true,editing,dragging;private String selected;
    private Runnable edited=()->{};private java.util.function.Consumer<String> picked=id->{};
    LayerSceneView(Context c){super(c);crystal=Ui.crystal(c,24,false);setBackgroundColor(0xFF061326);setContentDescription("Layered wallpaper preview");}
    void document(LayeredScene.Document d){document=d;refreshAssets();}
    LayeredScene.Document document(){return document;}
    void refreshAssets(){
        if(background!=null)background.recycle();background=null;for(Bitmap b:images.values())if(b!=null)b.recycle();images.clear();
        if(document!=null){if(document.background!=null)background=BitmapFactory.decodeByteArray(document.background,0,document.background.length);for(LayeredScene.ObjectLayer o:document.objects)if(o.image!=null)images.put(o.id,BitmapFactory.decodeByteArray(o.image,0,o.image.length));}invalidate();
    }
    void editing(boolean value,Runnable change,java.util.function.Consumer<String> pick){editing=value;edited=change;picked=pick;setFocusable(value);}
    void selected(String id){selected=id;invalidate();}
    void moving(boolean enabled){moving=enabled;if(!enabled)tiltX=tiltY=0;invalidate();}
    void tilt(float x,float y){float tx=moving?SceneMath.clamp(x,-1,1):0,ty=moving?SceneMath.clamp(y,-1,1):0;if(Math.abs(tx-tiltX)+Math.abs(ty-tiltY)>.001f){tiltX=tx;tiltY=ty;invalidate();}}
    float offset(float input,float depth,float dimension){return moving&&document!=null&&document.motion?input*depth*document.strength*dimension*.045f:0;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(document==null)return;float w=getWidth(),h=getHeight();
        if(background!=null){float scale=(document.fit==1?Math.min(w/background.getWidth(),h/background.getHeight()):Math.max(w/background.getWidth(),h/background.getHeight()));if(document.fit==0&&moving&&document.motion)scale*=1.1f;float iw=background.getWidth()*scale,ih=background.getHeight()*scale,dx=offset(tiltX,document.backgroundDepth,w),dy=offset(tiltY,document.backgroundDepth,h);paint.setShader(null);paint.setAlpha(255);c.drawBitmap(background,null,new RectF((w-iw)/2+dx,(h-ih)/2+dy,(w+iw)/2+dx,(h+ih)/2+dy),paint);}
        else{paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xFF163F58,0xFF0D182F,0xFF342854},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);}
        for(LayeredScene.ObjectLayer o:document.objects){RectF r=bounds(o);int save=c.save();c.rotate(o.rotation,r.centerX(),r.centerY());Bitmap b=images.get(o.id);
            if(b!=null){float scale=Math.min(r.width()/b.getWidth(),r.height()/b.getHeight());float iw=b.getWidth()*scale,ih=b.getHeight()*scale;paint.setAlpha(255);c.drawBitmap(b,null,new RectF(r.centerX()-iw/2,r.centerY()-ih/2,r.centerX()+iw/2,r.centerY()+ih/2),paint);}
            else if(o.kind.equals("crystal")){crystal.setBounds(Math.round(r.left),Math.round(r.top),Math.round(r.right),Math.round(r.bottom));crystal.draw(c);}
            else if(o.kind.equals("orb")){paint.setShader(new RadialGradient(r.centerX()-r.width()*.2f,r.centerY()-r.height()*.2f,r.width()*.7f,new int[]{0xFFF2FDFF,0xFF74C5DB,0xFF23546D,0xFF06182E},new float[]{0,.2f,.65f,1},Shader.TileMode.CLAMP));c.drawOval(r,paint);paint.setShader(null);}
            else{Path star=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float radius=r.width()*(i%2==0?.5f:.22f),x=r.centerX()+(float)Math.cos(a)*radius,y=r.centerY()+(float)Math.sin(a)*radius;if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}star.close();paint.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,0xFFF3F9FF,0xFF6355A4,Shader.TileMode.CLAMP));c.drawPath(star,paint);paint.setShader(null);}
            if(editing&&o.id.equals(selected)){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),1));paint.setColor(Ui.ACCENT);c.drawRoundRect(r,Ui.dp(getContext(),12),Ui.dp(getContext(),12),paint);paint.setStyle(Paint.Style.FILL);}c.restoreToCount(save);
        }
    }
    private RectF bounds(LayeredScene.ObjectLayer o){float size=o.size*getWidth(),x=o.x*getWidth()+offset(tiltX,o.depth,getWidth()),y=o.y*getHeight()+offset(tiltY,o.depth,getHeight());return new RectF(x-size/2,y-size/2,x+size/2,y+size/2);}
    @Override public boolean onTouchEvent(MotionEvent e){if(!editing||document==null)return false;switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:LayeredScene.ObjectLayer hit=null;for(int i=document.objects.size()-1;i>=0;i--){LayeredScene.ObjectLayer o=document.objects.get(i);if(bounds(o).contains(e.getX(),e.getY())){hit=o;break;}}if(hit==null)return false;selected=hit.id;picked.accept(selected);dragging=true;getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;
        case MotionEvent.ACTION_MOVE:if(dragging){for(LayeredScene.ObjectLayer o:document.objects)if(o.id.equals(selected)){o.x=SceneMath.clamp((e.getX()-offset(tiltX,o.depth,getWidth()))/getWidth(),0,1);o.y=SceneMath.clamp((e.getY()-offset(tiltY,o.depth,getHeight()))/getHeight(),0,1);}invalidate();return true;}return false;
        case MotionEvent.ACTION_UP:performClick();case MotionEvent.ACTION_CANCEL:if(dragging){dragging=false;getParent().requestDisallowInterceptTouchEvent(false);edited.run();return true;}return false;default:return dragging;}}
    @Override public boolean performClick(){return super.performClick();}
    void dispose(){if(background!=null){background.recycle();background=null;}for(Bitmap b:images.values())if(b!=null)b.recycle();images.clear();document=null;}
}
