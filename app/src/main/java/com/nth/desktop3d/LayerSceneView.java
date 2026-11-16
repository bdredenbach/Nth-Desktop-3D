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
    private Drawable crystal;private Bitmap background;private LayeredScene.Document document;
    private final Paint lightPaint=new Paint(3);
    private float tiltX,tiltY,seconds;private long lastClock;private boolean moving=true,lighting=true,editing,dragging;private String selected;
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
    void moving(boolean enabled){moving=enabled;if(!enabled)tiltX=tiltY=0;effectsChanged();invalidate();}
    void effectsChanged(){invalidate();}
    void lighting(boolean enabled){lighting=enabled;lastClock=0;invalidate();}
    void materialChanged(){crystal=Ui.crystal(getContext(),24,false);invalidate();}
    void tilt(float x,float y){float tx=moving?SceneMath.clamp(x,-1,1):0,ty=moving?SceneMath.clamp(y,-1,1):0;if(Math.abs(tx-tiltX)+Math.abs(ty-tiltY)>.001f){tiltX=tx;tiltY=ty;invalidate();}}
    float offset(float input,float depth,float dimension){return moving&&document!=null&&document.motion?input*depth*document.strength*dimension*.045f:0;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);if(document==null)return;long now=android.os.SystemClock.uptimeMillis();if(lastClock>0&&lighting&&isAttachedToWindow()&&isShown())seconds+=Math.min(.1f,(now-lastClock)/1000f);lastClock=now;float w=getWidth(),h=getHeight();
        if(background!=null){float scale=(document.fit==1?Math.min(w/background.getWidth(),h/background.getHeight()):Math.max(w/background.getWidth(),h/background.getHeight()));if(document.fit==0&&moving&&document.motion)scale*=1.1f;float iw=background.getWidth()*scale,ih=background.getHeight()*scale,dx=offset(tiltX,document.backgroundDepth,w),dy=offset(tiltY,document.backgroundDepth,h);paint.setShader(null);paint.setAlpha(255);c.drawBitmap(background,null,new RectF((w-iw)/2+dx,(h-ih)/2+dy,(w+iw)/2+dx,(h+ih)/2+dy),paint);}
        else{paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xFF163F58,0xFF0D182F,0xFF342854},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,paint);paint.setShader(null);}
        drawCaustics(c,w,h);drawShafts(c,w,h);
        boolean animated=document.caustics>0||document.shafts>0;for(LayeredScene.ObjectLayer object:document.objects)if(document.animate&&!editing&&object.movement!=0&&object.motionAmount>0)animated=true;if(animated&&lighting&&isAttachedToWindow()&&isShown())postInvalidateDelayed(33);
        for(LayeredScene.ObjectLayer o:document.objects){RectF r=bounds(o);int save=c.save();c.rotate(o.rotation+pose(o).angle,r.centerX(),r.centerY());int layer=c.saveLayerAlpha(r,Math.round(o.opacity*255));Bitmap b=images.get(o.id);
            if(b!=null){float scale=Math.min(r.width()/b.getWidth(),r.height()/b.getHeight());float iw=b.getWidth()*scale,ih=b.getHeight()*scale;paint.setAlpha(255);c.drawBitmap(b,null,new RectF(r.centerX()-iw/2,r.centerY()-ih/2,r.centerX()+iw/2,r.centerY()+ih/2),paint);}
            else if(o.kind.equals("crystal")){crystal.setBounds(Math.round(r.left),Math.round(r.top),Math.round(r.right),Math.round(r.bottom));crystal.draw(c);}
            else if(o.kind.equals("orb")){paint.setShader(new RadialGradient(r.centerX()-r.width()*.2f,r.centerY()-r.height()*.2f,r.width()*.7f,new int[]{0xFFF2FDFF,0xFF74C5DB,0xFF23546D,0xFF06182E},new float[]{0,.2f,.65f,1},Shader.TileMode.CLAMP));c.drawOval(r,paint);paint.setShader(null);}
            else{Path star=new Path();for(int i=0;i<10;i++){double a=-Math.PI/2+i*Math.PI/5;float radius=r.width()*(i%2==0?.5f:.22f),x=r.centerX()+(float)Math.cos(a)*radius,y=r.centerY()+(float)Math.sin(a)*radius;if(i==0)star.moveTo(x,y);else star.lineTo(x,y);}star.close();paint.setShader(new LinearGradient(r.left,r.top,r.right,r.bottom,0xFFF3F9FF,0xFF6355A4,Shader.TileMode.CLAMP));c.drawPath(star,paint);paint.setShader(null);}
            drawObjectLight(c,r);c.restoreToCount(layer);if(editing&&o.id.equals(selected)){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),1));paint.setColor(Ui.ACCENT);c.drawRoundRect(r,Ui.dp(getContext(),12),Ui.dp(getContext(),12),paint);paint.setStyle(Paint.Style.FILL);}c.restoreToCount(save);
        }
    }
    private void drawCaustics(Canvas c,float w,float h){
        if(document.caustics<=0)return;float t=lighting?seconds*(.15f+document.lightSpeed*.85f):0;
        lightPaint.setStyle(Paint.Style.STROKE);lightPaint.setStrokeWidth(Math.max(1,w*.0025f));lightPaint.setColor(light(0xFFB0FFF1));lightPaint.setAlpha(Math.round(document.caustics*95));
        // A moving, irregular Voronoi field gives closed light cells rather than a square grid.
        float cell=w/6f;int columns=10,rows=(int)Math.ceil(h/cell)+4;float[][] sites=new float[columns*rows][2];
        for(int y=0;y<rows;y++)for(int x=0;x<columns;x++){int i=y*columns+x;sites[i][0]=(x-2+.5f+.32f*(float)Math.sin(x*13.7+y*7.3+t*.37f))*cell;sites[i][1]=(y-2+.5f+.32f*(float)Math.cos(x*8.3+y*17.1-t*.29f))*cell;}
        float[] polygon=new float[128],clipped=new float[128];Path path=new Path();
        for(int y=1;y<rows-1;y++)for(int x=1;x<columns-1;x++){
            float[] site=sites[y*columns+x];int count=4;polygon[0]=site[0]-cell*2;polygon[1]=site[1]-cell*2;polygon[2]=site[0]+cell*2;polygon[3]=site[1]-cell*2;polygon[4]=site[0]+cell*2;polygon[5]=site[1]+cell*2;polygon[6]=site[0]-cell*2;polygon[7]=site[1]+cell*2;
            for(int ny=Math.max(0,y-2);ny<=Math.min(rows-1,y+2);ny++)for(int nx=Math.max(0,x-2);nx<=Math.min(columns-1,x+2);nx++){if(nx==x&&ny==y)continue;float[] neighbor=sites[ny*columns+nx];float dx=neighbor[0]-site[0],dy=neighbor[1]-site[1],limit=(neighbor[0]*neighbor[0]+neighbor[1]*neighbor[1]-site[0]*site[0]-site[1]*site[1])*.5f;int next=0;
                for(int i=0;i<count;i++){int a=i*2,b=(i+1)%count*2;float da=polygon[a]*dx+polygon[a+1]*dy-limit,db=polygon[b]*dx+polygon[b+1]*dy-limit;if(da<=0){clipped[next*2]=polygon[a];clipped[next*2+1]=polygon[a+1];next++;}if((da<=0)!=(db<=0)){float ratio=da/(da-db);clipped[next*2]=polygon[a]+(polygon[b]-polygon[a])*ratio;clipped[next*2+1]=polygon[a+1]+(polygon[b+1]-polygon[a+1])*ratio;next++;}}float[] swap=polygon;polygon=clipped;clipped=swap;count=next;
            }
            if(count<3)continue;path.reset();path.moveTo(warpX(polygon[0],polygon[1],cell,t),warpY(polygon[0],polygon[1],cell,t));for(int i=0;i<count;i++){int a=i*2,b=(i+1)%count*2;float mx=(polygon[a]+polygon[b])/2,my=(polygon[a+1]+polygon[b+1])/2;path.quadTo(warpX(mx,my,cell,t),warpY(mx,my,cell,t),warpX(polygon[b],polygon[b+1],cell,t),warpY(polygon[b],polygon[b+1],cell,t));}path.close();
            int glow=Math.round(document.caustics*(55+25*(float)Math.sin(t*.63f+x*1.7+y*2.3)));lightPaint.setStrokeWidth(Math.max(3,w*.012f));lightPaint.setAlpha(glow/8);c.drawPath(path,lightPaint);lightPaint.setStrokeWidth(Math.max(1,w*.0022f));lightPaint.setAlpha(glow);c.drawPath(path,lightPaint);
        }

    }
    private void drawShafts(Canvas canvas,float w,float h){
        if(document.shafts<=0||w<=0||h<=0)return;
        float time=lighting?seconds*(.2f+document.shaftSpeed*1.4f):0;
        lightPaint.setStyle(Paint.Style.FILL);lightPaint.setAlpha(255);
        // Tapered, feathered ribbons sway independently from either corner. They are
        // composed over the background, below objects, in both preview and wallpaper.
        for(int side=0;side<2;side++){
            if(document.shaftSide!=2&&document.shaftSide!=side)continue;
            int save=canvas.save();if(side==1){canvas.translate(w,0);canvas.scale(-1,1);}
            lightPaint.setAlpha(255);float sourceX=-w*.05f,sourceY=-h*.06f;
            for(int beam=0;beam<4;beam++){
                float phase=beam*1.83f+side*2.1f;
                float endX=w*(.1f+beam*.26f+.12f*(float)Math.sin(time*.69f+phase));
                float endY=h*(.78f+.13f*(float)Math.sin(time*.43f+phase));
                float spread=w*(.08f+.025f*beam),pulse=.72f+.28f*(float)Math.sin(time*1.1f+phase);
                for(int feather=5;feather>=1;feather--){
                    float radius=spread*feather/5f;int alpha=Math.round(document.shafts*pulse*12);
                    lightPaint.setShader(new LinearGradient(sourceX,sourceY,endX,endY,new int[]{(alpha<<24)|(light(0xFFC9F8FF)&0xFFFFFF),((alpha/2)<<24)|(light(0xFF8CE4F3)&0xFFFFFF),light(0x006CCADA)},new float[]{0,.42f,1},Shader.TileMode.CLAMP));
                    Path ribbon=new Path();ribbon.moveTo(sourceX-radius*.04f,sourceY);ribbon.cubicTo(endX*.22f-radius*.5f,h*.3f,endX-radius,endY*.8f,endX-radius,endY);ribbon.quadTo(endX,endY+h*.05f,endX+radius,endY);ribbon.cubicTo(endX+radius,endY*.8f,endX*.22f+radius*.5f,h*.3f,sourceX+radius*.04f,sourceY);ribbon.close();canvas.drawPath(ribbon,lightPaint);
                }
            }
            lightPaint.setShader(null);lightPaint.setAlpha(Math.round(document.shafts*38));
            lightPaint.setShader(new RadialGradient(0,0,w*.65f,new int[]{light(0xFFB3EEFA),light(0x0071CFE3)},null,Shader.TileMode.CLAMP));canvas.drawRect(0,0,w,h,lightPaint);canvas.restoreToCount(save);
        }
        lightPaint.setShader(null);lightPaint.setAlpha(255);
    }
    private float warpX(float x,float y,float cell,float t){return x+cell*.13f*(float)Math.sin(y/cell*3.1f+t*.52f);}
    private float warpY(float x,float y,float cell,float t){return y+cell*.13f*(float)Math.cos(x/cell*2.7f-t*.41f);}
    private SceneObjectMotion.Pose pose(LayeredScene.ObjectLayer o){return SceneObjectMotion.at(o.movement,o.motionAmount,o.motionSpeed,seconds,lighting&&document.animate&&!editing);}
    private int light(int base){return SceneLight.blend(base,0xFF000000|document.lightColor,document.lightIntensity);}
    private void drawObjectLight(Canvas canvas,RectF r){if(document.lightIntensity<=0)return;double a=Math.toRadians(document.lightAngle);float dx=(float)Math.cos(a)*r.width()*.6f,dy=(float)Math.sin(a)*r.height()*.6f;lightPaint.setStyle(Paint.Style.FILL);lightPaint.setAlpha(255);lightPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));int color=document.lightColor;int alpha=Math.round(document.lightIntensity*110);lightPaint.setShader(new LinearGradient(r.centerX()-dx,r.centerY()-dy,r.centerX()+dx,r.centerY()+dy,new int[]{(alpha<<24)|color,0x00FFFFFF,(Math.round(document.lightIntensity*53)<<24)|0x010A14},new float[]{0,.65f,1},Shader.TileMode.CLAMP));canvas.drawRect(r,lightPaint);lightPaint.setXfermode(null);lightPaint.setShader(null);lightPaint.setAlpha(255);}
    private RectF bounds(LayeredScene.ObjectLayer o){SceneObjectMotion.Pose motion=pose(o);float size=o.size*getWidth(),x=(o.x+motion.x)*getWidth()+offset(tiltX,o.depth,getWidth()),y=o.y*getHeight()+motion.y*getWidth()+offset(tiltY,o.depth,getHeight());return new RectF(x-size/2,y-size/2,x+size/2,y+size/2);}
    @Override public boolean onTouchEvent(MotionEvent e){if(!editing||document==null)return false;switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN:LayeredScene.ObjectLayer hit=null;for(int i=document.objects.size()-1;i>=0;i--){LayeredScene.ObjectLayer o=document.objects.get(i);if(bounds(o).contains(e.getX(),e.getY())){hit=o;break;}}if(hit==null)return false;selected=hit.id;picked.accept(selected);dragging=true;getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;
        case MotionEvent.ACTION_MOVE:if(dragging){for(LayeredScene.ObjectLayer o:document.objects)if(o.id.equals(selected)){o.x=SceneMath.clamp((e.getX()-offset(tiltX,o.depth,getWidth()))/getWidth(),0,1);o.y=SceneMath.clamp((e.getY()-offset(tiltY,o.depth,getHeight()))/getHeight(),0,1);}invalidate();return true;}return false;
        case MotionEvent.ACTION_UP:performClick();case MotionEvent.ACTION_CANCEL:if(dragging){dragging=false;getParent().requestDisallowInterceptTouchEvent(false);edited.run();return true;}return false;default:return dragging;}}
    @Override public boolean performClick(){return super.performClick();}
    void dispose(){if(background!=null){background.recycle();background=null;}for(Bitmap b:images.values())if(b!=null)b.recycle();images.clear();document=null;}
}
