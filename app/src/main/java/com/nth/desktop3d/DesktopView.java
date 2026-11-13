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
    interface Host { void open(LauncherStore.Item item);void menu(LauncherStore.Item item);void backgroundMenu();void pageChanged();void ripple(float x,float y);void startMenu();void feed();void configureWidget(int id); }
    private final LauncherStore store;
    private final AppCatalog catalog;
    private final LauncherWidgetHost widgetHost;
    private final android.appwidget.AppWidgetManager widgetManager;
    private final Host host;
    private final FrameLayout stage;
    private final List<Body> bodies=new ArrayList<>();
    private ScrollView scroller;
    private WorkspaceFrame workspace;
    private final java.util.Map<String,View> cellViews=new java.util.LinkedHashMap<>();
    private final java.util.Map<Integer,android.appwidget.AppWidgetProviderInfo> providers=new java.util.HashMap<>();
    private java.util.List<DesktopGrid.Cell> cells=new ArrayList<>(),originalPlan,previewPlan;
    private DesktopGrid.Cell dragStart;
    private String dragId;
    private int columns,rows,visibleRows,cw,ch;

    private boolean gravity(){return store.mode==1&&!reduced;}
    private String widgetKey(int id){return "widget:"+id;}
    private int top(){return Ui.dp(getContext(),24);}

    private float downX,downY,tiltX,tiltY;
    private final float gestureSlop;
    private boolean swiping,animating,touching,modal,reduced,powerSave,held,verticalGesture,startedOnItem;
    private ValueAnimator animator;
    private int width,height;
    private final Runnable backgroundHold;
    private static final class Body {
        final IconTile tile;final LauncherStore.Item item;
        float x,y,vx,vy,rotation;
        Body(IconTile tile,LauncherStore.Item item){this.tile=tile;this.item=item;}
    }
    DesktopView(Context c,LauncherStore store,AppCatalog catalog,Host host,LauncherWidgetHost widgetHost,android.appwidget.AppWidgetManager widgetManager) {
        super(c);gestureSlop=Ui.dp(c,55);this.store=store;this.catalog=catalog;this.host=host;this.widgetHost=widgetHost;this.widgetManager=widgetManager;setClipChildren(false);setClipToPadding(false);
        backgroundHold=()->{if(!swiping&&!verticalGesture&&touching&&!modal){held=true;this.host.backgroundMenu();}};
        stage=new FrameLayout(c);stage.setClipChildren(false);stage.setClipToPadding(false);stage.setCameraDistance(Ui.dp(c,2200));
        addView(stage,new LayoutParams(-1,-1));setFocusable(false);
    }
    void configure(boolean reduced,boolean powerSave){this.reduced=reduced;this.powerSave=powerSave;}
    void tilt(float x,float y){tiltX=x;tiltY=y;}
    void setModal(boolean v){modal=v;if(v){clearSelection();touching=false;removeCallbacks(backgroundHold);}}
    void rebuild(){
        clearSelection();if(animator!=null)animator.cancel();animating=false;stage.animate().cancel();resetTransform();
        stage.removeAllViews();bodies.clear();cellViews.clear();providers.clear();cells=new ArrayList<>();
        widgetHost.releaseViews();
        workspace=new WorkspaceFrame(getContext());
        if(!gravity()){
            scroller=new ScrollView(getContext());scroller.setFillViewport(true);scroller.setClipToPadding(false);scroller.setVerticalScrollBarEnabled(false);
            scroller.addView(workspace,new ScrollView.LayoutParams(-1,-2));stage.addView(scroller,new LayoutParams(-1,-1));
        }else{scroller=null;stage.addView(workspace,new LayoutParams(-1,-1));}
        int visible=0;
        for(LauncherStore.Item item:store.pages.get(store.page)){
            if(gravity()&&visible++>=18)break;
            AppCatalog.Entry entry=catalog.find(item.key);
            IconTile tile=new IconTile(getContext(),item.label,entry==null?null:entry.icon,item.key);
            tile.setOnClickListener(v->host.open(item));tile.setOnLongClickListener(v->{host.menu(item);return true;});
            if(gravity())tile.physicsStyle();
            Body body=new Body(tile,item);bodies.add(body);cellViews.put(item.id,tile);
            workspace.addView(tile,new LayoutParams(Ui.dp(getContext(),84),Ui.dp(getContext(),112)));installDrag(body);
        }
        for(int id:widgetIds()){
            android.appwidget.AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(id);providers.put(id,info);
            View view;
            try{
                if(info==null)throw new IllegalStateException("Widget provider is unavailable");
                LauncherWidgetView widget=(LauncherWidgetView)widgetHost.createView(getContext(),id,info);
                widget.setGestures(new LauncherWidgetView.Gestures(){
                    public void start(){startDrag(widgetKey(id));}
                    public void move(float dx,float dy){previewDrag(dx,dy);}
                    public void end(boolean cancel){finishDrag(cancel);}
                    public void menu(){showWidgetMenu(widget,id,info);}
                });
                widget.setContentDescription(info.loadLabel(getContext().getPackageManager())+" widget");view=widget;
            }catch(RuntimeException error){
                android.util.Log.e("NthWidgets","Cannot display widget "+id,error);
                android.widget.TextView placeholder=Ui.text(getContext(),"Widget unavailable\nHold to retry or remove",14,0xFFEAF8FF);
                placeholder.setGravity(android.view.Gravity.CENTER);placeholder.setBackground(Ui.satin(getContext(),0xB82B3A50,0xB8111D31,14));
                placeholder.setOnLongClickListener(v->{showWidgetMenu(placeholder,id,info);return true;});view=placeholder;
            }
            view.setTag(widgetKey(id));cellViews.put(widgetKey(id),view);workspace.addView(view,new LayoutParams(1,1));
        }
        updateLayout();post(this::updateLayout);
    }
    private java.util.List<Integer> widgetIds(){
        java.util.List<Integer> result=new ArrayList<>();
        for(String raw:store.preferences.getString("widget_ids_page_"+store.page,"").split(",")){
            try{int id=Integer.parseInt(raw);if(id>=0&&!result.contains(id))result.add(id);}catch(NumberFormatException ignored){}
        }
        return result;
    }
    private int gridColumns(){return Math.max(3,Math.min(8,store.preferences.getInt("grid_columns",4)));}
    private int cellHeight(){return Ui.dp(getContext(),store.preferences.getInt("grid_row_dp",112)+Math.max(0,getResources().getConfiguration().fontScale-1)*30);}
    private android.graphics.RectF rectangle(DesktopGrid.Cell cell){
        return new android.graphics.RectF(cell.x*width/(float)columns,top()+cell.y*ch,(cell.x+cell.w)*width/(float)columns,top()+(cell.y+cell.h)*ch);
    }
    private int span(int pixels,int cell){return Math.max(1,(int)Math.ceil(pixels/(double)Math.max(1,cell)));}
    private void restoreCells(){
        columns=gridColumns();cw=Math.max(1,width/columns);ch=cellHeight();visibleRows=Math.max(1,(height-top()-Ui.dp(getContext(),12))/ch);
        java.util.List<DesktopGrid.Cell> desired=new ArrayList<>();int area=0,maxWidgetHeight=1,maxSavedRow=visibleRows,index=0;
        for(int id:widgetIds()){
            android.appwidget.AppWidgetProviderInfo info=providers.get(id);
            android.graphics.Rect padding=info==null?new android.graphics.Rect():android.appwidget.AppWidgetHostView.getDefaultPaddingForWidget(getContext(),info.provider,null);
            int defaultW=info==null?2:span(info.minWidth+padding.left+padding.right,cw);
            int defaultH=info==null?1:span(info.minHeight+padding.top+padding.bottom,ch);
            if(android.os.Build.VERSION.SDK_INT>=31&&info!=null){if(info.targetCellWidth>0)defaultW=info.targetCellWidth;if(info.targetCellHeight>0)defaultH=info.targetCellHeight;}
            int w=store.preferences.getInt("widget_span_x_"+id,store.preferences.contains("widget_width_"+id)?span(store.preferences.getInt("widget_width_"+id,0),cw):defaultW);
            int h=store.preferences.getInt("widget_span_y_"+id,store.preferences.contains("widget_height_"+id)?span(store.preferences.getInt("widget_height_"+id,0),ch):defaultH);
            w=Math.max(1,Math.min(columns,w));h=Math.max(1,Math.min(12,h));
            int x=store.preferences.getInt("widget_cell_x_"+id,Math.round(store.preferences.getInt("widget_left_"+id,0)/(float)cw));
            int y=store.preferences.getInt("widget_cell_y_"+id,Math.round((store.preferences.getInt("widget_top_"+id,top())-top())/(float)ch));
            x=Math.max(0,Math.min(columns-w,x));y=Math.max(0,Math.min(60,y));
            desired.add(new DesktopGrid.Cell(widgetKey(id),true,x,y,w,h));area+=w*h;maxWidgetHeight=Math.max(maxWidgetHeight,h);maxSavedRow=Math.max(maxSavedRow,y+h);
        }
        for(Body body:bodies){
            LauncherStore.Item item=body.item;
            int x=item.cellX>=0?item.cellX:item.x<0?index%columns:Math.round(item.x*Math.max(0,width-Ui.dp(getContext(),84))/cw);
            int y=item.cellY>=0?item.cellY:item.y<0?index/columns:Math.round((item.y*Math.max(0,height-Ui.dp(getContext(),112))-top())/ch);
            x=Math.max(0,Math.min(columns-1,x));y=Math.max(0,Math.min(60,y));
            desired.add(new DesktopGrid.Cell(item.id,false,x,y,1,1));area++;maxSavedRow=Math.max(maxSavedRow,y+1);index++;
        }
        rows=Math.max(maxSavedRow,Math.max(visibleRows,(area+columns-1)/columns+maxWidgetHeight-1));
        cells=DesktopGrid.restore(desired,columns,rows);
        // Legacy dense layouts may need additional scrollable rows; never stack or discard items.
        while(cells==null&&rows<120)cells=DesktopGrid.restore(desired,columns,++rows);
        if(cells==null)throw new IllegalStateException("Desktop layout exceeds safe capacity");
        workspace.metrics(columns,ch,top());
        if(scroller!=null){android.view.ViewGroup.LayoutParams lp=workspace.getLayoutParams();lp.height=Math.max(height,top()+rows*ch+Ui.dp(getContext(),12));workspace.setLayoutParams(lp);}
        positionCells(cells,true);
    }
    private void positionCells(java.util.List<DesktopGrid.Cell> plan,boolean save){
        android.content.SharedPreferences.Editor editor=save?store.preferences.edit():null;
        for(DesktopGrid.Cell cell:plan){
            View view=cellViews.get(cell.id);if(view==null)continue;
            android.graphics.RectF bounds=rectangle(cell);
            if(cell.widget){
                LayoutParams lp=(LayoutParams)view.getLayoutParams();int w=Math.round(bounds.width()),h=Math.round(bounds.height());
                if(lp.width!=w||lp.height!=h){lp.width=w;lp.height=h;view.setLayoutParams(lp);}
                view.setTranslationX(bounds.left);view.setTranslationY(bounds.top);
                if(save){int id=Integer.parseInt(cell.id.substring(7));editor.putInt("widget_cell_x_"+id,cell.x).putInt("widget_cell_y_"+id,cell.y).putInt("widget_span_x_"+id,cell.w).putInt("widget_span_y_"+id,cell.h).putInt("widget_left_"+id,Math.round(bounds.left)).putInt("widget_top_"+id,Math.round(bounds.top)).putInt("widget_width_"+id,w).putInt("widget_height_"+id,h);}
            }else{
                if(!gravity()){
                    LayoutParams lp=(LayoutParams)view.getLayoutParams();int w=Math.min(Ui.dp(getContext(),84),Math.round(bounds.width())),h=ch;
                    if(lp.width!=w||lp.height!=h){lp.width=w;lp.height=h;view.setLayoutParams(lp);}
                    ((IconTile)view).fitCell(w,h);
                    float x=bounds.left+(bounds.width()-w)/2,y=bounds.top;
                    view.setTranslationX(x);view.setTranslationY(y);
                    for(Body body:bodies)if(body.item.id.equals(cell.id)){body.x=x;body.y=y;break;}
                }
                if(save)for(Body body:bodies)if(body.item.id.equals(cell.id)){body.item.cellX=cell.x;body.item.cellY=cell.y;break;}
            }
        }
        if(save){editor.apply();store.save();}
    }
    private void startDrag(String id){
        if(cells.isEmpty())return;
        workspace.select(null,null);dragId=id;dragStart=DesktopGrid.find(cells,id);originalPlan=new ArrayList<>(cells);previewPlan=new ArrayList<>(cells);
        View view=cellViews.get(id);if(view!=null){view.setElevation(Ui.dp(getContext(),24));view.setAlpha(.86f);}
        workspace.preview(dragStart==null?null:rectangle(dragStart),true);workspace.requestDisallowInterceptTouchEvent(true);
    }
    private void previewDrag(float dx,float dy){
        if(dragStart==null)return;
        int x=dragStart.x+Math.round(dx/cw),y=dragStart.y+Math.round(dy/ch);
        DesktopGrid.Cell target=dragStart.at(x,y);
        previewPlan=DesktopGrid.move(originalPlan,target,columns,rows);
        if(previewPlan==null&&!dragStart.widget){
            java.util.List<DesktopGrid.Cell> others=new ArrayList<>();for(DesktopGrid.Cell cell:originalPlan)if(!cell.id.equals(dragId))others.add(cell);
            DesktopGrid.Cell nearest=DesktopGrid.nearest(target,others,columns,rows);
            if(nearest!=null){target=nearest;previewPlan=DesktopGrid.move(originalPlan,target,columns,rows);}
        }
        positionCells(previewPlan==null?originalPlan:previewPlan,false);workspace.preview(rectangle(target),previewPlan!=null);
        View view=cellViews.get(dragId);android.graphics.RectF original=rectangle(dragStart);
        if(view!=null){view.setTranslationX(original.left+dx+(dragStart.widget?0:(original.width()-view.getLayoutParams().width)/2));view.setTranslationY(original.top+dy);}
    }
    private void finishDrag(boolean cancel){
        if(originalPlan==null)return;
        boolean valid=!cancel&&previewPlan!=null;
        cells=valid?previewPlan:originalPlan;positionCells(cells,valid);
        View view=cellViews.get(dragId);if(view!=null){view.setAlpha(1);view.setElevation(0);}
        workspace.preview(null,true);if(!cancel&&!valid)android.widget.Toast.makeText(getContext(),"That space is occupied",android.widget.Toast.LENGTH_SHORT).show();
        originalPlan=previewPlan=null;dragStart=null;dragId=null;workspace.requestDisallowInterceptTouchEvent(false);
    }
    boolean revealWidget(int id){
        View view=cellViews.get(widgetKey(id));if(!(view instanceof LauncherWidgetView))return false;
        post(()->{updateLayout();View shown=cellViews.get(widgetKey(id));if(shown!=null){if(scroller!=null)scroller.smoothScrollTo(0,Math.max(0,Math.round(shown.getTranslationY())-top()));beginResize(id);}});return true;
    }
    private void showWidgetMenu(View widget,int id,android.appwidget.AppWidgetProviderInfo info){
        android.widget.PopupMenu popup=new android.widget.PopupMenu(getContext(),widget);
        if(info!=null&&info.resizeMode!=android.appwidget.AppWidgetProviderInfo.RESIZE_NONE)popup.getMenu().add("Resize");
        if(info!=null&&info.configure!=null)popup.getMenu().add("Widget settings");
        if(info!=null)popup.getMenu().add("App info");else popup.getMenu().add("Retry");
        popup.getMenu().add("Remove");
        popup.setOnMenuItemClickListener(action->{
            switch(action.getTitle().toString()){
                case "Resize":beginResize(id);break;
                case "Widget settings":host.configureWidget(id);break;
                case "App info":getContext().startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+info.provider.getPackageName())));break;
                case "Retry":rebuild();break;
                case "Remove":
                    java.util.List<String> kept=new ArrayList<>();for(int other:widgetIds())if(other!=id)kept.add(String.valueOf(other));
                    android.content.SharedPreferences.Editor edit=store.preferences.edit().putString("widget_ids_page_"+store.page,android.text.TextUtils.join(",",kept));
                    for(String prefix:new String[]{"widget_cell_x_","widget_cell_y_","widget_span_x_","widget_span_y_","widget_left_","widget_top_","widget_width_","widget_height_"})edit.remove(prefix+id);
                    edit.apply();widgetHost.deleteAppWidgetId(id);rebuild();break;
            }return true;
        });popup.show();
    }
    private void beginResize(int id){
        android.appwidget.AppWidgetProviderInfo info=providers.get(id);DesktopGrid.Cell selected=DesktopGrid.find(cells,widgetKey(id));
        if(info==null||selected==null||info.resizeMode==android.appwidget.AppWidgetProviderInfo.RESIZE_NONE)return;
        workspace.axes(info.resizeMode);workspace.select(rectangle(selected),new WorkspaceFrame.Resize(){
            int edge;DesktopGrid.Cell initial;java.util.List<DesktopGrid.Cell> before,plan;
            public void start(int edge){this.edge=edge;initial=DesktopGrid.find(cells,widgetKey(id));before=new ArrayList<>(cells);plan=before;}
            public void move(float dx,float dy){
                int deltaX=Math.round(dx/cw),deltaY=Math.round(dy/ch),x=initial.x,y=initial.y,w=initial.w,h=initial.h;
                android.graphics.Rect padding=android.appwidget.AppWidgetHostView.getDefaultPaddingForWidget(getContext(),info.provider,null);
                int minW=Math.min(columns,span(Math.max(1,info.minResizeWidth>0?Math.min(info.minWidth,info.minResizeWidth):info.minWidth)+padding.left+padding.right,cw));
                int minH=Math.min(12,span(Math.max(1,info.minResizeHeight>0?Math.min(info.minHeight,info.minResizeHeight):info.minHeight)+padding.top+padding.bottom,ch));
                int maxW=columns,maxH=Math.max(minH,Math.max(initial.h,visibleRows));
                if(android.os.Build.VERSION.SDK_INT>=31){if(info.maxResizeWidth>=info.minWidth&&info.maxResizeWidth>0)maxW=Math.max(minW,Math.min(columns,info.maxResizeWidth/cw));if(info.maxResizeHeight>=info.minHeight&&info.maxResizeHeight>0)maxH=Math.max(minH,Math.min(visibleRows,info.maxResizeHeight/ch));}
                if(edge==1){w=Math.max(minW,Math.min(maxW,initial.w-deltaX));x=initial.x+initial.w-w;}
                if(edge==2)w=Math.max(minW,Math.min(maxW,initial.w+deltaX));
                if(edge==3){h=Math.max(minH,Math.min(maxH,initial.h-deltaY));y=initial.y+initial.h-h;}
                if(edge==4)h=Math.max(minH,Math.min(maxH,initial.h+deltaY));
                DesktopGrid.Cell target=new DesktopGrid.Cell(widgetKey(id),true,x,y,w,h);plan=DesktopGrid.move(before,target,columns,rows);
                positionCells(plan==null?before:plan,false);workspace.preview(rectangle(target),plan!=null);workspace.select(rectangle(plan==null?initial:target),this);
            }
            public void end(boolean cancel){cells=!cancel&&plan!=null?plan:before;positionCells(cells,!cancel&&plan!=null);workspace.preview(null,true);workspace.select(rectangle(DesktopGrid.find(cells,widgetKey(id))),this);}
        });
    }
    private void clearSelection(){
        if(originalPlan!=null)finishDrag(true);
        if(workspace!=null){workspace.preview(null,true);workspace.select(null,null);}
    }
    private int physicsTileHeight(){return Ui.dp(getContext(),98+Math.max(0,getResources().getConfiguration().fontScale-1)*30);}
    private void installDrag(Body body){
        body.tile.setOnTouchListener(new View.OnTouchListener(){
            float sx,sy,bx,by,lastX,lastY;long last;boolean moved,held,cancelledTap;
            final Runnable hold=()->{held=true;body.tile.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);body.tile.getParent().requestDisallowInterceptTouchEvent(true);};
            @Override public boolean onTouch(View v,MotionEvent e){
                switch(e.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        sx=lastX=e.getRawX();sy=lastY=e.getRawY();bx=body.x;by=body.y;last=e.getEventTime();moved=held=cancelledTap=false;v.setPressed(true);v.postDelayed(hold,ViewConfiguration.getLongPressTimeout());return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx=e.getRawX()-sx,dy=e.getRawY()-sy;
                        if(!held&&!gravity()&&Math.hypot(dx,dy)>ViewConfiguration.get(getContext()).getScaledTouchSlop()){cancelledTap=true;v.removeCallbacks(hold);return true;}
                        if((held||gravity())&&Math.hypot(dx,dy)>Ui.dp(getContext(),8)){
                            if(!moved){moved=true;if(!gravity())startDrag(body.item.id);}
                            if(!gravity())previewDrag(dx,dy);
                            else{body.x=SceneMath.clamp(bx+dx,0,Math.max(0,width-v.getWidth()));body.y=SceneMath.clamp(by+dy,0,Math.max(0,height-v.getHeight()));long dt=Math.max(1,e.getEventTime()-last);body.vx=(e.getRawX()-lastX)*1000/dt;body.vy=(e.getRawY()-lastY)*1000/dt;v.setTranslationX(body.x);v.setTranslationY(body.y);last=e.getEventTime();lastX=e.getRawX();lastY=e.getRawY();}
                        }return true;
                    case MotionEvent.ACTION_UP:
                        v.removeCallbacks(hold);v.setPressed(false);
                        if(moved){if(!gravity())finishDrag(false);else{savePositions();store.save();}}
                        else if(held)host.menu(body.item);else if(!cancelledTap)v.performClick();
                        v.getParent().requestDisallowInterceptTouchEvent(false);return true;
                    case MotionEvent.ACTION_CANCEL:
                        v.removeCallbacks(hold);v.setPressed(false);if(moved&&!gravity())finishDrag(true);v.getParent().requestDisallowInterceptTouchEvent(false);return true;
                    default:return true;
                }
            }
        });
    }
    private void updateLayout(){
        width=getWidth();height=getHeight();if(width==0||height==0||workspace==null)return;
        restoreCells();
        if(gravity()){
            int cols=Math.max(1,width/Ui.dp(getContext(),85)),i=0;int size=Ui.dp(getContext(),78),h=physicsTileHeight();
            for(Body body:bodies){
                LayoutParams lp=(LayoutParams)body.tile.getLayoutParams();lp.width=size;lp.height=h;body.tile.setLayoutParams(lp);
                body.x=SceneMath.clamp(body.item.x<0?(i%cols)*(width/(float)cols)+4:body.item.x*Math.max(0,width-size),0,Math.max(0,width-size));
                body.y=SceneMath.clamp(body.item.y<0?Math.max(0,height-h-(i/cols)*(h+Ui.dp(getContext(),7))):body.item.y*Math.max(0,height-h),0,Math.max(0,height-h));
                avoidWidgets(body);body.tile.setTranslationX(body.x);body.tile.setTranslationY(body.y);i++;
            }
        }
    }
    private void avoidWidgets(Body body){
        float w=body.tile.getLayoutParams().width,h=body.tile.getLayoutParams().height;
        for(DesktopGrid.Cell cell:cells)if(cell.widget){
            android.graphics.RectF box=rectangle(cell);
            if(body.x<box.right&&body.x+w>box.left&&body.y<box.bottom&&body.y+h>box.top){
                float[] xs={box.left-w,box.right,body.x,body.x},ys={body.y,body.y,box.top-h,box.bottom};float best=Float.MAX_VALUE;int chosen=-1;
                for(int i=0;i<4;i++)if(xs[i]>=0&&ys[i]>=0&&xs[i]+w<=width&&ys[i]+h<=height){
                    float distance=Math.abs(xs[i]-body.x)+Math.abs(ys[i]-body.y);if(distance<best){best=distance;chosen=i;}
                }
                if(chosen>=0){body.x=xs[chosen];body.y=ys[chosen];body.vx*= -.35f;body.vy*= -.35f;}
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
        for(Body b:bodies){avoidWidgets(b);b.x=SceneMath.clamp(b.x,0,Math.max(0,width-b.tile.getWidth()));b.y=SceneMath.clamp(b.y,0,Math.max(0,height-b.tile.getHeight()));b.rotation=SceneMath.smooth(b.rotation,SceneMath.clamp(b.vx/22,-12,12),time,3);b.tile.setTranslationX(b.x);b.tile.setTranslationY(b.y);b.tile.setRotation(b.item.fixed?0:b.rotation);b.tile.setRotationY(tiltX*store.strength*6);}
    }
    void savePositions(){
        if(!gravity())return;
        for(Body b:bodies){
            b.item.x=b.x/Math.max(1,width-b.tile.getWidth());
            b.item.y=b.y/Math.max(1,height-b.tile.getHeight());
        }
    }
    void arrange(){for(LauncherStore.Item i:store.pages.get(store.page)){i.x=i.y=-1;i.cellX=i.cellY=-1;}rebuild();store.save();}
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
    private boolean itemAt(float x,float y){
        if(workspace==null)return false;
        float scroll=scroller==null?0:scroller.getScrollY();
        for(View child:cellViews.values()){
            float left=child.getLeft()+child.getTranslationX(),top=child.getTop()+child.getTranslationY()-scroll;
            if(x>=left&&x<left+child.getWidth()&&y>=top&&y<top+child.getHeight())return true;
        }return false;
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent e){
        if(animating)return true;
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();swiping=false;verticalGesture=false;startedOnItem=itemAt(downX,downY);}
        if(startedOnItem||dragId!=null||(workspace!=null&&workspace.resizing()))return false;
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE&&(store.mode!=1||reduced)&&!held){float dx=e.getX()-downX,dy=e.getY()-downY;
            int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if(!swiping&&Math.abs(dy)>slop*2&&Math.abs(dy)>Math.abs(dx)*1.4f&&dy<0){return true;}
            if(!swiping&&Math.abs(dy)>slop&&Math.abs(dy)>Math.abs(dx))verticalGesture=true;
            if(!verticalGesture&&Math.abs(dx)>slop*2&&Math.abs(dx)>Math.abs(dy)*1.4f){swiping=true;return true;}}
        return false;
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=e.getX();downY=e.getY();touching=true;swiping=false;verticalGesture=false;startedOnItem=itemAt(downX,downY);postDelayed(backgroundHold,ViewConfiguration.getLongPressTimeout());host.ripple(e.getX(),e.getY());return true;
            case MotionEvent.ACTION_MOVE:float dx=e.getX()-downX,dy=e.getY()-downY;
                if(Math.hypot(dx,dy)>ViewConfiguration.get(getContext()).getScaledTouchSlop()){removeCallbacks(backgroundHold);if(Math.abs(dy)>Math.abs(dx))verticalGesture=true;if(!verticalGesture&&Math.abs(dx)>Math.abs(dy)*1.4f)swiping=true;}return true;
            case MotionEvent.ACTION_UP:removeCallbacks(backgroundHold);if(startedOnItem){startedOnItem=false;return true;}if(!held){
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
