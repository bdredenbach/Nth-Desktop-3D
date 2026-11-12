package com.nth.desktop3d;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.DragEvent;
import android.content.ClipData;
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
            grid.setOnDragListener((v,event)->{
                if(event.getAction()==DragEvent.ACTION_DRAG_STARTED)return event.getLocalState() instanceof LauncherStore.Item;
                if(event.getAction()==DragEvent.ACTION_DROP){
                    LauncherStore.Item dragged=(LauncherStore.Item)event.getLocalState();
                    java.util.List<LauncherStore.Item> items=store.pages.get(store.page);
                    int old=items.indexOf(dragged);
                    if(old<0)return false;
                    int target=grid.indexForPoint(event.getX(),event.getY());
                    target=Math.max(0,Math.min(items.size()-1,target));
                    items.remove(old);items.add(target,dragged);
                    store.save();rebuild();host.pageChanged();return true;
                }
                if(event.getAction()==DragEvent.ACTION_DRAG_ENDED){clearSelection();return true;}
                return true;
            });
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
            else{
                // Normal scenes use free-form icon coordinates, not ordered grid cells.
                stage.addView(tile,new LayoutParams(Ui.dp(getContext(),84),Ui.dp(getContext(),112)));
                installDrag(body);
            }
        }
        addWidgets();
        updateLayout();post(this::updateLayout);
    }
    // Shared desktop grid metrics for widgets and shortcuts.
    private int cellWidth(){return Ui.dp(getContext(),Math.max(84,Math.round(84*getResources().getConfiguration().fontScale*.8f)));}
    private int cellHeight(){return Ui.dp(getContext(),112+Math.max(0,getResources().getConfiguration().fontScale-1)*30);}
    private int snap(int pixels,int cell){return Math.round(pixels/(float)Math.max(1,cell))*Math.max(1,cell);}
    private android.graphics.Point emptyWidgetSpot(int id,int w,int h){
        int screenW=Math.max(Ui.dp(getContext(),320),getWidth());
        int screenH=Math.max(Ui.dp(getContext(),540),getHeight());
        int cols=Math.max(1,Math.min(8,screenW/cellWidth()));
        int cw=screenW/cols,ch=cellHeight();
        int spanX=Math.max(1,(int)Math.ceil(w/(double)cw));
        int spanY=Math.max(1,(int)Math.ceil(h/(double)ch));
        int rows=Math.max(1,(screenH-Ui.dp(getContext(),36))/ch);
        boolean[][] busy=new boolean[rows][cols];
        int shortcutCount=store.pages.get(store.page).size();
        for(int i=0;i<shortcutCount;i++){
            int row=i/cols,col=i%cols;
            if(row<rows)busy[row][col]=true;
        }
        String ids=store.preferences.getString("widget_ids_page_"+store.page,"");
        for(String raw:ids.split(",")){
            int other;try{other=Integer.parseInt(raw);}catch(NumberFormatException e){continue;}
            if(other==id||!store.preferences.contains("widget_top_"+other))continue;
            int ox=store.preferences.getInt("widget_left_"+other,0);
            int oy=store.preferences.getInt("widget_top_"+other,0);
            int ow=store.preferences.getInt("widget_width_"+other,cw*2);
            int oh=store.preferences.getInt("widget_height_"+other,ch);
            int left=Math.max(0,ox/cw),top=Math.max(0,(oy-Ui.dp(getContext(),24))/ch);
            for(int y=top;y<Math.min(rows,top+(int)Math.ceil(oh/(double)ch));y++)
                for(int x=left;x<Math.min(cols,left+(int)Math.ceil(ow/(double)cw));x++)busy[y][x]=true;
        }
        for(int row=0;row+spanY<=rows;row++)for(int col=0;col+spanX<=cols;col++){
            boolean free=true;
            for(int y=row;y<row+spanY;y++)for(int x=col;x<col+spanX;x++)if(busy[y][x])free=false;
            if(free)return new android.graphics.Point(col*cw,Ui.dp(getContext(),24)+row*ch);
        }
        return new android.graphics.Point(0,Math.max(Ui.dp(getContext(),24),screenH-h));
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
                int width=store.preferences.getInt("widget_width_"+id,Ui.dp(getContext(),Math.min(300,Math.max(160,info.minWidth))));
                int height=store.preferences.getInt("widget_height_"+id,Ui.dp(getContext(),Math.min(260,Math.max(100,info.minHeight))));
                FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,height);
                lp.gravity=android.view.Gravity.TOP|android.view.Gravity.LEFT;
                android.graphics.Point spot=emptyWidgetSpot(id,width,height);
                int left=store.preferences.getInt("widget_left_"+id,spot.x);
                int top=store.preferences.getInt("widget_top_"+id,spot.y);
                lp.leftMargin=Math.max(0,Math.min(Math.max(0,getWidth()-width),left));
                lp.topMargin=Math.max(0,top);
                if(!store.preferences.contains("widget_left_"+id))
                    store.preferences.edit().putInt("widget_left_"+id,lp.leftMargin)
                        .putInt("widget_top_"+id,lp.topMargin).apply();
                stage.addView(widget,lp);widget.setElevation(Ui.dp(getContext(),14));
                widget.setOnLongClickListener(v->{selectWidget(widget,id,info);return true;});
                widget.setOnTouchListener(new View.OnTouchListener(){
                    float startX,startY;int originLeft,originTop;boolean dragging;
                    @Override public boolean onTouch(View v,MotionEvent e){
                        switch(e.getActionMasked()){
                            case MotionEvent.ACTION_DOWN:
                                startX=e.getRawX();startY=e.getRawY();
                                originLeft=((FrameLayout.LayoutParams)widget.getLayoutParams()).leftMargin;
                                originTop=((FrameLayout.LayoutParams)widget.getLayoutParams()).topMargin;
                                dragging=false;return selectedWidget==widget;
                            case MotionEvent.ACTION_MOVE:
                                if(selectedWidget!=widget)return false;
                                float dx=e.getRawX()-startX,dy=e.getRawY()-startY;
                                if(Math.hypot(dx,dy)>Ui.dp(getContext(),8))dragging=true;
                                if(dragging){
                                    FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)widget.getLayoutParams();
                                    p.leftMargin=Math.max(0,Math.min(Math.max(0,getWidth()-widget.getWidth()),Math.round(originLeft+dx)));
                                    p.topMargin=Math.max(0,Math.min(Math.max(0,getHeight()-widget.getHeight()),Math.round(originTop+dy)));
                                    widget.setLayoutParams(p);
                                    if(widgetToolbar!=null){
                                        FrameLayout.LayoutParams t=(FrameLayout.LayoutParams)widgetToolbar.getLayoutParams();
                                        t.topMargin=Math.max(0,p.topMargin-Ui.dp(getContext(),46));
                                        t.leftMargin=p.leftMargin;widgetToolbar.setLayoutParams(t);
                                    }
                                }
                                return true;
                            case MotionEvent.ACTION_UP:
                                if(selectedWidget!=widget)return false;
                                if(dragging){
                                    FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)widget.getLayoutParams();
                                    int cols=Math.max(1,Math.min(8,Math.max(1,getWidth())/cellWidth()));
                                    int cw=Math.max(1,getWidth()/cols);
                                    p.leftMargin=Math.max(0,Math.min(Math.max(0,getWidth()-widget.getWidth()),snap(p.leftMargin,cw)));
                                    p.topMargin=Math.max(0,Math.min(Math.max(0,getHeight()-widget.getHeight()),Ui.dp(getContext(),24)+snap(p.topMargin-Ui.dp(getContext(),24),cellHeight())));
                                    widget.setLayoutParams(p);
                                    store.preferences.edit().putInt("widget_left_"+id,p.leftMargin)
                                        .putInt("widget_top_"+id,p.topMargin).apply();
                                }
                                dragging=false;return true;
                            case MotionEvent.ACTION_CANCEL:dragging=false;return selectedWidget==widget;
                            default:return false;
                        }
                    }
                });
                position++;
            }catch(RuntimeException ignored){}
        }
    }
    private IconTile selectedTile;
    private android.appwidget.AppWidgetHostView selectedWidget;
    private android.widget.LinearLayout widgetToolbar;
    private Runnable delayedMenu;
    private void selectWidget(android.appwidget.AppWidgetHostView widget,int id,android.appwidget.AppWidgetProviderInfo info){
        clearSelection();selectedWidget=widget;
        widget.setBackground(Ui.satin(getContext(),0x6645D9F5,0x88203454,18));
        widget.setElevation(Ui.dp(getContext(),22));
        android.widget.LinearLayout tools=new android.widget.LinearLayout(getContext());
        tools.setBackground(Ui.satin(getContext(),0xEE245775,0xEE0B1B30,16));
        tools.setPadding(Ui.dp(getContext(),8),Ui.dp(getContext(),4),Ui.dp(getContext(),8),Ui.dp(getContext(),4));
        addWidgetAction(tools,"←",()->moveWidget(widget,id,-1,0));
        addWidgetAction(tools,"↑",()->moveWidget(widget,id,0,-1));
        addWidgetAction(tools,"↓",()->moveWidget(widget,id,0,1));
        addWidgetAction(tools,"→",()->moveWidget(widget,id,1,0));
        addWidgetAction(tools,"−",()->resizeWidget(widget,id,info,-1));
        addWidgetAction(tools,"+",()->resizeWidget(widget,id,info,1));
        addWidgetAction(tools,"Remove",()->{
            String key="widget_ids_page_"+store.page;
            StringBuilder kept=new StringBuilder();
            for(String raw:store.preferences.getString(key,"").split(",")){
                if(raw.isEmpty()||raw.equals(String.valueOf(id)))continue;
                if(kept.length()>0)kept.append(",");
                kept.append(raw);
            }
            store.preferences.edit().putString(key,kept.toString()).apply();
            widgetHost.deleteAppWidgetId(id);clearSelection();rebuild();
        });
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,Ui.dp(getContext(),44));
        p.gravity=android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL;
        p.topMargin=Math.max(0,((FrameLayout.LayoutParams)widget.getLayoutParams()).topMargin-Ui.dp(getContext(),46));
        stage.addView(tools,p);widgetToolbar=tools;
    }
    private void addWidgetAction(android.widget.LinearLayout parent,String label,Runnable action){
        android.widget.TextView button=new android.widget.TextView(getContext());
        button.setText(label);button.setTextColor(android.graphics.Color.WHITE);
        button.setGravity(android.view.Gravity.CENTER);button.setTextSize(15);
        button.setPadding(Ui.dp(getContext(),14),0,Ui.dp(getContext(),14),0);
        parent.addView(button,new android.widget.LinearLayout.LayoutParams(-2,-1));
        button.setOnClickListener(v->action.run());
    }
    private void moveWidget(android.appwidget.AppWidgetHostView widget,int id,int dx,int dy){
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)widget.getLayoutParams();
        int cols=Math.max(1,Math.min(8,Math.max(1,getWidth())/cellWidth()));
        int cw=Math.max(1,getWidth()/cols);
        p.leftMargin=Math.max(0,Math.min(Math.max(0,getWidth()-widget.getWidth()),p.leftMargin+cw*dx));
        p.topMargin=Math.max(0,Math.min(Math.max(0,getHeight()-widget.getHeight()),p.topMargin+cellHeight()*dy));
        widget.setLayoutParams(p);
        store.preferences.edit().putInt("widget_left_"+id,p.leftMargin).putInt("widget_top_"+id,p.topMargin).apply();
        if(widgetToolbar!=null){
            FrameLayout.LayoutParams toolbar=(FrameLayout.LayoutParams)widgetToolbar.getLayoutParams();
            toolbar.topMargin=Math.max(0,p.topMargin-Ui.dp(getContext(),46));
            toolbar.leftMargin=p.leftMargin;
            widgetToolbar.setLayoutParams(toolbar);
        }
    }
    private void resizeWidget(android.appwidget.AppWidgetHostView widget,int id,android.appwidget.AppWidgetProviderInfo info,int direction){
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)widget.getLayoutParams();
        int minW=Ui.dp(getContext(),Math.max(100,info.minResizeWidth>0?info.minResizeWidth:info.minWidth));
        int minH=Ui.dp(getContext(),Math.max(80,info.minResizeHeight>0?info.minResizeHeight:info.minHeight));
        p.width=Math.max(minW,Math.min(getWidth(),p.width+Ui.dp(getContext(),48)*direction));
        p.height=Math.max(minH,Math.min(getHeight(),p.height+Ui.dp(getContext(),48)*direction));
        widget.setLayoutParams(p);
        widget.updateAppWidgetSize(null,p.width,p.height,p.width,p.height);
        store.preferences.edit().putInt("widget_width_"+id,p.width).putInt("widget_height_"+id,p.height).apply();
    }
    private void clearSelection(){
        if(widgetToolbar!=null){stage.removeView(widgetToolbar);widgetToolbar=null;}
        if(selectedWidget!=null){selectedWidget.setBackground(null);selectedWidget.setElevation(Ui.dp(getContext(),14));selectedWidget=null;}
        if(selectedTile!=null){
            selectedTile.animate().cancel();
            selectedTile.setBackground(Ui.touch(getContext(),null,18));
            selectedTile.setScaleX(1f);selectedTile.setScaleY(1f);selectedTile.setAlpha(1f);
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
                        if(moved){held=false;body.x=SceneMath.clamp(bx+dx,0,Math.max(0,width-v.getWidth()));body.y=SceneMath.clamp(by+dy,0,Math.max(0,height-v.getHeight()));long dt=Math.max(1,e.getEventTime()-last);body.vx=(e.getRawX()-lastX)*1000/dt;body.vy=(e.getRawY()-lastY)*1000/dt;v.setTranslationX(body.x);v.setTranslationY(body.y);last=e.getEventTime();lastX=e.getRawX();lastY=e.getRawY();}return true;
                    case MotionEvent.ACTION_UP:
                        v.removeCallbacks(hold);v.setPressed(false);
                        if(!moved&&!held&&e.getEventTime()-start<500)v.performClick();
                        if(moved){savePositions();store.save();}
                        return true;
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
            int cols=Math.max(1,Math.min(8,width/Ui.dp(getContext(),84)));
            int cw=Math.max(1,width/cols),i=0;
            for(Body body:bodies){
                int tw=body.tile.getLayoutParams().width,th=body.tile.getLayoutParams().height;
                float x=body.item.x<0?(i%cols)*cw:body.item.x*Math.max(0,width-tw);
                float y=body.item.y<0?Ui.dp(getContext(),24)+(i/cols)*Ui.dp(getContext(),112):body.item.y*Math.max(0,height-th);
                body.x=SceneMath.clamp(x,0,Math.max(0,width-tw));
                body.y=SceneMath.clamp(y,0,Math.max(0,height-th));
                body.tile.setTranslationX(body.x);body.tile.setTranslationY(body.y);
                i++;
            }
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
    void savePositions(){
        for(Body b:bodies){
            b.item.x=b.x/Math.max(1,width-b.tile.getWidth());
            b.item.y=b.y/Math.max(1,height-b.tile.getHeight());
        }
    }
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
                    if(endDx>0&&store.page==0){host.feed();}
                    else if(endDx<0&&store.page==LauncherStore.PAGE_COUNT-1){
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
