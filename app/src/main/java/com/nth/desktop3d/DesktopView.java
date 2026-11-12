package com.nth.desktop3d;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import java.util.ArrayList;
import java.util.List;

/** Accessible shortcut planes, with bounded physics and native inverse-transform input. */
@android.annotation.SuppressLint("ViewConstructor") // Requires the canonical layout and launcher action host.
final class DesktopView extends FrameLayout {
    interface Host { void open(LauncherStore.Item item);void menu(LauncherStore.Item item);void backgroundMenu();void pageChanged();void ripple(float x,float y);void startMenu();void feed(); }
    private final LauncherStore store;
    private final AppCatalog catalog;
    private final android.appwidget.AppWidgetHost widgetHost;
    private final android.appwidget.AppWidgetManager widgetManager;
    private final Host host;
    private final FrameLayout stage;
    private final List<Body> bodies=new ArrayList<>();
    private ScrollView scroller;
    private ShortcutGrid grid;
    private float downX,downY,tiltX,tiltY;
    private final float gestureSlop;
    private boolean swiping,animating,touching,modal,reduced,powerSave,held,verticalGesture;
    private ValueAnimator animator;
    private int width,height;
    private final Runnable backgroundHold;
    private static final class Body {
        final IconTile tile;final LauncherStore.Item item;
        float x,y,vx,vy,rotation;
        Body(IconTile tile,LauncherStore.Item item){this.tile=tile;this.item=item;}
    }
    DesktopView(Context c,LauncherStore store,AppCatalog catalog,Host host,android.appwidget.AppWidgetHost widgetHost,android.appwidget.AppWidgetManager widgetManager) {
        super(c);gestureSlop=Ui.dp(c,55);this.store=store;this.catalog=catalog;this.host=host;this.widgetHost=widgetHost;this.widgetManager=widgetManager;setClipChildren(false);setClipToPadding(false);
        backgroundHold=()->{if(!swiping&&!verticalGesture&&touching&&!modal){held=true;this.host.backgroundMenu();}};
        stage=new FrameLayout(c);stage.setClipChildren(false);stage.setClipToPadding(false);stage.setCameraDistance(Ui.dp(c,2200));
        addView(stage,new LayoutParams(-1,-1));setFocusable(false);
    }
    void configure(boolean reduced,boolean powerSave){this.reduced=reduced;this.powerSave=powerSave;}
    void tilt(float x,float y){tiltX=x;tiltY=y;}
    void setModal(boolean v){modal=v;if(v){clearSelection();touching=false;removeCallbacks(backgroundHold);}}
    void rebuild(){
        clearSelection();if(animator!=null)animator.cancel();animating=false;stage.animate().cancel();resetTransform();stage.removeAllViews();bodies.clear();
        boolean gravity=store.mode==1&&!reduced;
        if(!gravity){
            scroller=new ScrollView(getContext());scroller.setFillViewport(true);scroller.setClipToPadding(false);scroller.setVerticalScrollBarEnabled(false);
            grid=new ShortcutGrid(getContext());grid.setPadding(0,Ui.dp(getContext(),24),0,Ui.dp(getContext(),12));
            grid.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_DOWN)host.ripple(e.getX(),e.getY()-scroller.getScrollY());return false;});
            grid.setOnLongClickListener(v->{held=true;host.backgroundMenu();return true;});
            scroller.addView(grid,new ScrollView.LayoutParams(-1,-2));stage.addView(scroller,new LayoutParams(-1,-1));
        }else {scroller=null;grid=null;}
        List<LauncherStore.Item> items=store.pages.get(store.page);
        int visible=0;
        for(LauncherStore.Item item:items){
            if(gravity&&visible++>=18)break; // Every shortcut is also accessible in All shortcuts.
            AppCatalog.Entry entry=catalog.find(item.key);
            IconTile tile=new IconTile(getContext(),item.label,entry==null?null:entry.icon,item.key);
            tile.setOnClickListener(v->host.open(item));tile.setOnLongClickListener(v->{selectTile(tile,item);return true;});
            Body body=new Body(tile,item);bodies.add(body);
            if(gravity){tile.physicsStyle();stage.addView(tile,new LayoutParams(Ui.dp(getContext(),78),physicsTileHeight()));installDrag(body);}
            else grid.addView(tile);
        }
        addWidgets();
        updateLayout();post(this::updateLayout);
    }
    private void addWidgets(){
        String ids=store.preferences.getString("widget_ids_page_"+store.page,"");
        if(ids.isEmpty())return;
        int position=0;
        for(String raw:ids.split(",")){
            try{
                int id=Integer.parseInt(raw);
                android.appwidget.AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(id);
                if(info==null)continue;
                android.appwidget.AppWidgetHostView widget=widgetHost.createView(getContext(),id,info);
                widget.setAppWidget(id,info);
                int width=Ui.dp(getContext(),Math.min(300,Math.max(160,info.minWidth)));
                int height=Ui.dp(getContext(),Math.min(260,Math.max(100,info.minHeight)));
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,height);
                lp.gravity=android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL;
                lp.topMargin=Ui.dp(getContext(),16+position*24);
                stage.addView(widget,lp);widget.setElevation(Ui.dp(getContext(),14));
                position++;
            }catch(RuntimeException ignored){}
        }
    }
    private IconTile selectedTile;
    private Runnable delayedMenu;
    private void clearSelection(){
        if(selectedTile!=null){
            selectedTile.animate().cancel();
            selectedTile.setBackground(Ui.touch(getContext(),null,18));
            selectedTile.setScaleX(1f);selectedTile.setScaleY(1f);
            selectedTile=null;
        }
        if(delayedMenu!=null){removeCallbacks(delayedMenu);delayedMenu=null;}
    }
    private void selectTile(IconTile tile,LauncherStore.Item item){
        clearSelection();selectedTile=tile;
        tile.setBackground(Ui.touch(getContext(),Ui.satin(getContext(),0x7445D9F5,0x99203454,18),18));
        if(!reduced)tile.animate().scaleX(1.07f).scaleY(1.07f).setDuration(130).start();
        delayedMenu=()->{if(selectedTile==tile&&!modal){clearSelection();host.menu(item);}};
        postDelayed(delayedMenu,220);
    }
    private int physicsTileHeight(){return Ui.dp(getContext(),98+Math.max(0,getResources().getConfiguration().fontScale-1)*30);}
    private void installDrag(Body body){
        body.tile.setOnTouchListener(new View.OnTouchListener(){float sx,sy,bx,by,lastX,lastY;long start,last;boolean moved,held;final Runnable hold=()->{held=true;body.tile.performLongClick();};
            @Override public boolean onTouch(View v,MotionEvent e){
                switch(e.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:sx=lastX=e.getRawX();sy=lastY=e.getRawY();bx=body.x;by=body.y;last=start=e.getEventTime();moved=held=false;v.setPressed(true);v.postDelayed(hold,ViewConfiguration.getLongPressTimeout());return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx,dy=e.getRawY()-sy;
                        if(Math.hypot(dx,dy)>Ui.dp(getContext(),9)){moved=true;v.removeCallbacks(hold);}
                        if(moved&&!held){body.x=SceneMath.clamp(bx+dx,0,Math.max(0,width-v.getWidth()));body.y=SceneMath.clamp(by+dy,0,Math.max(0,height-v.getHeight()));long dt=Math.max(1,e.getEventTime()-last);body.vx=(e.getRawX()-lastX)*1000/dt;body.vy=(e.getRawY()-lastY)*1000/dt;v.setTranslationX(body.x);v.setTranslationY(body.y);last=e.getEventTime();lastX=e.getRawX();lastY=e.getRawY();}return true;
                    case MotionEvent.ACTION_UP:v.removeCallbacks(hold);v.setPressed(false);if(!moved&&!held&&e.getEventTime()-start<500)v.performClick();savePositions();return true;
                    case MotionEvent.ACTION_CANCEL:v.removeCallbacks(hold);v.setPressed(false);return true;
                    default:return false;
                }
            }
        });
    }
    private void updateLayout(){
        width=getWidth();height=getHeight();if(width==0||height==0)return;
        if(grid!=null){
            grid.requestLayout();
        }else{
            int cols=Math.max(1,width/Ui.dp(getContext(),85));int size=Ui.dp(getContext(),78),h=physicsTileHeight();int i=0;
            for(Body b:bodies){
                // A bounded shelf for extra items keeps even large desktops reachable.
                float x=b.item.x<0?(i%cols)*(width/(float)cols)+4:b.item.x*Math.max(0,width-size);
                float y=b.item.y<0?Math.max(0,height-h-(i/cols)*(h+Ui.dp(getContext(),7))):b.item.y*Math.max(0,height-h);
                b.x=SceneMath.clamp(x,0,Math.max(0,width-size));b.y=SceneMath.clamp(y,0,Math.max(0,height-h));
                b.tile.setTranslationX(b.x);b.tile.setTranslationY(b.y);i++;
            }
        }
    }
    void frame(float dt){
        if(modal||touching||animating)return;
        if(store.mode!=1||reduced){stage.setRotationY(reduced?0:tiltX*store.strength*3);stage.setRotationX(reduced?0:-tiltY*store.strength*2);return;}
        if(powerSave)return;
        float scale=getResources().getDisplayMetrics().density;float time=Math.min(dt,.025f);
        for(Body b:bodies){
            if(b.item.fixed)continue;
            b.vx+=tiltX*store.strength*950*scale*time;b.vy+=(250+tiltY*store.strength*700)*scale*time;
            b.vx*=Math.pow(.88,time*60);b.vy*=Math.pow(.985,time*60);b.x+=b.vx*time;b.y+=b.vy*time;
            float[] hit=SceneMath.constrain(b.x,b.y,b.vx,b.vy,width,height,b.tile.getWidth());b.x=hit[0];b.vx=hit[2];
            float maxY=Math.max(0,height-b.tile.getHeight());if(b.y<0){b.y=0;b.vy=Math.abs(b.vy)*.45f;}if(b.y>maxY){b.y=maxY;b.vy=-Math.abs(b.vy)*.36f;}
        }
        for(int pass=0;pass<2;pass++)for(int i=0;i<bodies.size();i++)for(int j=i+1;j<bodies.size();j++){
            Body a=bodies.get(i),b=bodies.get(j);float dx=b.x-a.x,dy=b.y-a.y;float w=(a.tile.getWidth()+b.tile.getWidth())*.47f,h=(a.tile.getHeight()+b.tile.getHeight())*.46f;
            if(Math.abs(dx)<w&&Math.abs(dy)<h){
                float overlapX=w-Math.abs(dx),overlapY=h-Math.abs(dy),sign;
                if(overlapX<overlapY){sign=dx>=0?1:-1;float amount=overlapX*.51f;if(!a.item.fixed)a.x-=sign*amount;if(!b.item.fixed)b.x+=sign*amount;float v=a.vx;if(!a.item.fixed)a.vx=b.vx*.55f;if(!b.item.fixed)b.vx=v*.55f;}
                else{sign=dy>=0?1:-1;float amount=overlapY*.51f;if(!a.item.fixed)a.y-=sign*amount;if(!b.item.fixed)b.y+=sign*amount;float v=a.vy;if(!a.item.fixed)a.vy=b.vy*.3f;if(!b.item.fixed)b.vy=v*.3f;}
            }
        }
        for(Body b:bodies){b.x=SceneMath.clamp(b.x,0,Math.max(0,width-b.tile.getWidth()));b.y=SceneMath.clamp(b.y,0,Math.max(0,height-b.tile.getHeight()));b.rotation=SceneMath.smooth(b.rotation,SceneMath.clamp(b.vx/22,-12,12),time,3);b.tile.setTranslationX(b.x);b.tile.setTranslationY(b.y);b.tile.setRotation(b.item.fixed?0:b.rotation);b.tile.setRotationY(tiltX*store.strength*6);}
    }
    void savePositions(){if(store.mode==1&&!reduced)for(Body b:bodies){b.item.x=b.x/Math.max(1,width-b.tile.getWidth());b.item.y=b.y/Math.max(1,height-b.tile.getHeight());}}
    void arrange(){for(LauncherStore.Item i:store.pages.get(store.page)){i.x=i.y=-1;}rebuild();store.save();}
    void changePage(int direction){
        if(animating||modal)return;savePositions();
        if(reduced){store.page=SceneMath.wrap(store.page+direction,LauncherStore.PAGE_COUNT);rebuild();store.save();host.pageChanged();return;}
        animating=true;int next=SceneMath.wrap(store.page+direction,LauncherStore.PAGE_COUNT);final boolean[] switched={false};
        animator=ValueAnimator.ofFloat(0,1);animator.setDuration(470);animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a->{float progress=(float)a.getAnimatedValue();if(progress>=.5f&&!switched[0]){switched[0]=true;store.page=next;ValueAnimator keep=animator;animator=null;rebuild();animator=keep;animating=true;host.pageChanged();}
            float local=progress<.5f?progress*2:(progress-.5f)*2;boolean incoming=progress>=.5f;float amount=incoming?(1-local):local;float sign=(incoming?-direction:direction);
            stage.setPivotX(store.transition==0?(sign>0?0:width):width*.5f);stage.setPivotY(height*.5f);
            stage.setRotationY(store.transition==2?0:sign*amount*(store.transition==0?78:45));stage.setRotationX(store.transition==2?sign*amount*68:0);
            stage.setTranslationX(store.transition==1?-sign*amount*width*.35f:0);stage.setScaleX(1-amount*.12f);stage.setScaleY(1-amount*.12f);stage.setAlpha(1-amount*.6f);
        });
        animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){animating=false;resetTransform();store.save();}});animator.start();
    }
    private void resetTransform(){stage.setRotationX(0);stage.setRotationY(0);stage.setTranslationX(0);stage.setScaleX(1);stage.setScaleY(1);stage.setAlpha(1);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);post(this::updateLayout);}
    @Override public boolean dispatchTouchEvent(MotionEvent e){
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){touching=true;held=false;verticalGesture=false;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&Math.hypot(e.getX()-downX,e.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())removeCallbacks(backgroundHold);
        boolean result=super.dispatchTouchEvent(e);
        if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL){touching=false;removeCallbacks(backgroundHold);}
        return result;
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent e){
        if(animating)return true;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();swiping=false;}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&(store.mode!=1||reduced)&&!held){float dx=e.getX()-downX,dy=e.getY()-downY;
            int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if(!swiping&&Math.abs(dy)>slop*2&&Math.abs(dy)>Math.abs(dx)*1.4f&&dy<0){return true;}
            if(!swiping&&Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx))verticalGesture=true;
            if(!verticalGesture&&Math.abs(dx)>slop*2&&Math.abs(dx)>Math.abs(dy)*1.4f){swiping=true;return true;}}
        return false;
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();touching=true;swiping=false;postDelayed(backgroundHold,ViewConfiguration.getLongPressTimeout());host.ripple(e.getX(),e.getY());return true;
            case MotionEvent.ACTION_MOVE:float dx=e.getX()-downX,dy=e.getY()-downY;
                if(Math.hypot(dx,dy)>ViewConfiguration.get(getContext()).getScaledTouchSlop()){removeCallbacks(backgroundHold);if(Math.abs(dy)>Math.abs(dx))verticalGesture=true;if(!verticalGesture&&Math.abs(dx)>Math.abs(dy)*1.4f)swiping=true;}return true;
            case MotionEvent.ACTION_UP:removeCallbacks(backgroundHold);if(!held){
                float endDx=e.getX()-downX,endDy=e.getY()-downY;
                if(Math.abs(endDy)>gestureSlop&&Math.abs(endDy)>Math.abs(endDx)*1.4f&&endDy<0){host.startMenu();}
                else if(swiping&&Math.abs(endDx)>gestureSlop){
                    if(endDx<0&&store.page==0&&downX<Ui.dp(getContext(),45)){host.feed();}
                    else if((endDx>0&&store.page==0)||(endDx<0&&store.page==LauncherStore.PAGE_COUNT-1)){
                        float tilt=endDx>0?12f:-12f;
                        stage.animate().rotationY(tilt).setDuration(110).withEndAction(()->stage.animate().rotationY(0).setDuration(240).start()).start();
                    }else changePage(endDx<0?1:-1);
                }else if(!swiping&&!verticalGesture)performClick();
            }return true;
            case MotionEvent.ACTION_CANCEL:removeCallbacks(backgroundHold);return true;
            default:return true;
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
