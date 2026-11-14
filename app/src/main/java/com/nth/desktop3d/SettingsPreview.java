package com.nth.desktop3d;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

@android.annotation.SuppressLint("ViewConstructor")
final class SettingsPreview extends View {
    private final SettingsModel model;private final AppCatalog catalog;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable[] scenes=new Drawable[4];private Drawable material,folderMaterial;private String page="home";private boolean dragging;private float sx,sy;private android.animation.ValueAnimator transitionAnimator;private float transitionProgress;private boolean transitionRunning;private int lastTransition=-1;private boolean openingRunning;private float openingProgress;private int openingStyle;
    SettingsPreview(Context context,SettingsModel model,AppCatalog catalog){super(context);this.model=model;this.catalog=catalog;int[] ids={R.drawable.scene_spatial,R.drawable.scene_gravity,R.drawable.scene_holographic,R.drawable.scene_pond};for(int i=0;i<4;i++)scenes[i]=context.getDrawable(ids[i]);setCameraDistance(Ui.dp(context,1800));setElevation(Ui.dp(context,9));setOutlineProvider(new android.view.ViewOutlineProvider(){@Override public void getOutline(View view,android.graphics.Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),Ui.dp(context,page.equals("folders")?model.value("folder_radius"):18));outline.setAlpha(.5f);}});setFocusable(true);material=Ui.satin(context,0xA348A3C3,0xB4132B49,12);setContentDescription("Live 3D settings preview. Drag to rotate; settings below change this preview.");}
    void page(String value){page=value;openingRunning=false;if(value.equals("menu_animation")||value.equals("folder_animation"))playOpening();else if(value.equals("motion"))playTransition();else{if(transitionAnimator!=null)transitionAnimator.cancel();transitionRunning=false;}invalidate();}
    void refresh(){if(lastTransition!=model.store.transition){lastTransition=model.store.transition;if(page.equals("motion"))playTransition();}material=Ui.iconSurface(getContext(),false);folderMaterial=Ui.iconSurface(getContext(),true);setCameraDistance(Ui.dp(getContext(),2400-model.crystal.depth()*12));invalidateOutline();invalidate();if(model.store.reducedMotion){setRotationX(0);setRotationY(0);}}
    void playOpening(){
        if(transitionAnimator!=null)transitionAnimator.cancel();transitionRunning=false;openingRunning=false;openingStyle=page.equals("folder_animation")?model.value("folder_animation"):model.value("menu_animation");openingProgress=1;setContentDescription("Preview: "+(page.equals("folder_animation")?"folder":"menu")+" opening · "+PanelAnimations.FOLDERS[openingStyle]);if(model.store.reducedMotion||!android.animation.ValueAnimator.areAnimatorsEnabled()||openingStyle==6){invalidate();return;}openingRunning=true;transitionAnimator=android.animation.ValueAnimator.ofFloat(0,1);transitionAnimator.setDuration(1200);transitionAnimator.addUpdateListener(a->{openingProgress=(float)a.getAnimatedValue();invalidate();});transitionAnimator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){openingRunning=false;invalidate();}});transitionAnimator.start();
    }
    void playTransition(){
        if(transitionAnimator!=null)transitionAnimator.cancel();transitionRunning=false;transitionProgress=0;
        setContentDescription("Preview: "+LauncherStore.TRANSITIONS[model.store.transition]+" desktop transition");
        if(model.store.reducedMotion||!android.animation.ValueAnimator.areAnimatorsEnabled()||model.store.transition==15){invalidate();return;}
        transitionRunning=true;transitionAnimator=android.animation.ValueAnimator.ofFloat(0,1);transitionAnimator.setDuration(1200);transitionAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        transitionAnimator.addUpdateListener(a->{transitionProgress=(float)a.getAnimatedValue();invalidate();});transitionAnimator.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){transitionRunning=false;invalidate();}});transitionAnimator.start();
    }
    @Override protected void onDetachedFromWindow(){if(transitionAnimator!=null)transitionAnimator.cancel();super.onDetachedFromWindow();}
    void tilt(float x,float y){if(dragging||model.store.reducedMotion)return;setRotationX(-y*5);setRotationY(x*7);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();RectF area=new RectF(0,0,w,h);Path path=new Path();float radius=(page.equals("folders")?model.value("folder_radius"):18)*d;path.addRoundRect(area,radius,radius,Path.Direction.CW);int saved=c.save();c.clipPath(path);
        Drawable scene=scenes[model.store.mode];scene.setBounds(0,0,(int)w,(int)h);scene.draw(c);paint.setColor(0x6B071522);c.drawRect(area,paint);
        int transformed=c.save();if(openingRunning){PanelAnimations.canvas(c,openingStyle,openingProgress,w,h);paint.setColor(0xC81B354C);c.drawRoundRect(new RectF(4*d,4*d,w-4*d,h-4*d),18*d,18*d,paint);}if(transitionRunning){boolean incoming=transitionProgress>=.5f;float amount=incoming?2-2*transitionProgress:2*transitionProgress;PageTransitions.Frame frame=PageTransitions.frame(model.store.transition,amount,1,incoming);c.saveLayerAlpha(0,0,w,h,Math.round(frame.alpha*255));PageTransitions.canvas(c,frame,w,h);}
        boolean folder=page.equals("folders")||page.equals("folder_animation"),folderIcons=page.equals("folder_shape"),drawer=page.equals("drawer"),demo=page.equals("motion");int columns=folderIcons?4:folder?model.value("folder_columns"):drawer?Math.min(8,model.value("drawer_columns")+(getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE?2:0)):model.value("grid_columns");
        List<LauncherStore.Item> items=new ArrayList<>();if(folder&&!model.store.folders().isEmpty())items.addAll(model.store.folders().get(0).children);else if(drawer){for(AppCatalog.Entry entry:catalog.entries){items.add(new LauncherStore.Item(entry.key,entry.label));if(items.size()>=columns*2)break;}}else items.addAll(model.store.pages.get((model.store.page+(transitionRunning&&transitionProgress>=.5f?1:0))%model.store.pageCount()));
        if(folderIcons){items.clear();items.addAll(model.store.folders());if(items.isEmpty()){LauncherStore.Item example=new LauncherStore.Item("folder","Folder");for(AppCatalog.Entry e:catalog.entries){example.children.add(new LauncherStore.Item(e.key,e.label));if(example.children.size()==4)break;}items.add(example);}}
        boolean sample=items.isEmpty()||(!folder&&!drawer&&!folderIcons&&items.stream().allMatch(item->item.cellY>=2));
        if(items.isEmpty())for(AppCatalog.Entry entry:catalog.entries){items.add(new LauncherStore.Item(entry.key,entry.label));if(items.size()>=columns*2)break;}
        float top=20*d,padding=12*d+model.value("desktop_padding")*d*.25f;
        if(sample)text(c,"Sample preview",w/2,11*d,8*d,Ui.MUTED);
        if(folder&&model.value("folder_title")==1){text(c,model.store.folders().isEmpty()?"Folder":model.store.folders().get(0).label,w/2,16*d,11*d,Ui.TEXT);top=32*d;}
        if(folder&&!items.isEmpty())columns=Math.min(columns,items.size());
        float dockHeight=20*d*(48+model.value("dock_padding")*2)/62f;float bottom=model.value("show_dock")==1&&!folder&&!drawer?dockHeight+8*d:6*d;
        float cw=(w-2*padding)/columns,ch=(h-top-bottom)/2;
        boolean labels=model.value(folder?"folder_labels":drawer?"drawer_labels":"desktop_labels")==1;int percent=model.value(folder?"folder_icon_size":drawer?"drawer_icon_size":"icon_size");
        List<DesktopGrid.Cell> plan=new ArrayList<>();
        if(!folder&&!drawer){
            if(!demo&&!sample&&!folderIcons)for(String raw:model.store.preferences.getString("widget_ids_page_"+model.store.page,"").split(","))try{int id=Integer.parseInt(raw);int sw=Math.max(1,Math.min(columns,model.store.preferences.getInt("widget_span_x_"+id,2))),sh=Math.max(1,Math.min(12,model.store.preferences.getInt("widget_span_y_"+id,2)));int x=Math.max(0,Math.min(columns-sw,model.store.preferences.getInt("widget_cell_x_"+id,0))),y=Math.max(0,Math.min(120-sh,model.store.preferences.getInt("widget_cell_y_"+id,0)));plan.add(new DesktopGrid.Cell("widget:"+id,true,x,y,sw,sh));}catch(NumberFormatException ignored){}
            for(int n=0;n<items.size();n++){LauncherStore.Item item=items.get(n);plan.add(new DesktopGrid.Cell(item.id,false,Math.max(0,Math.min(columns-1,demo||sample||folderIcons||item.cellX<0?n%columns:item.cellX)),Math.max(0,Math.min(118,demo||sample||folderIcons||item.cellY<0?n/columns:item.cellY)),1,1));}
            List<DesktopGrid.Cell> restored=DesktopGrid.restore(plan,columns,120);if(restored!=null)plan=restored;
            for(DesktopGrid.Cell cell:plan)if(cell.widget&&cell.y<2){RectF widget=new RectF(padding+cell.x*cw+3*d,top+cell.y*ch+2*d,padding+(cell.x+cell.w)*cw-3*d,Math.min(h-bottom,top+(cell.y+cell.h)*ch-2*d));paint.setColor(0xD52C5065);c.drawRoundRect(widget,9*d,9*d,paint);text(c,"Widget",widget.centerX(),widget.centerY()+4*d,11*d,Ui.TEXT);}
        }
        for(int n=0;n<items.size();n++){
            LauncherStore.Item item=items.get(n);DesktopGrid.Cell cell=folder||drawer?null:DesktopGrid.find(plan,item.id);int column=cell==null?n%columns:cell.x,row=cell==null?n/columns:cell.y;if(row>=2)continue;float x=padding+(column+.5f)*cw,y=top+row*ch;
            float size=Math.max(12*d,Math.min(cw*.62f,ch*(labels?.55f:.75f))*percent/100f);float font=Math.min(10*d,cw/7)*model.value("label_size")/12f;size=Math.min(size,Math.min(cw-6*d,ch-(labels?font+4*d:4*d)));
            RectF tile=new RectF(x-size/2,y,x+size/2,y+size);if(model.crystal.shadows()){paint.setColor(0x55233951);paint.setShadowLayer(3*d,0,2*d,0x80000000);c.drawRoundRect(tile,8*d,8*d,paint);paint.clearShadowLayer();}if(folderMaterial==null){material=Ui.iconSurface(getContext(),false);folderMaterial=Ui.iconSurface(getContext(),true);}Drawable surface=item.folder()?folderMaterial:material;surface.setBounds(Math.round(tile.left),Math.round(tile.top),Math.round(tile.right),Math.round(tile.bottom));surface.draw(c);
            Drawable icon=catalog.icon(item);if(icon==null)icon=new IconTile.BuiltinIcon(item.key);android.graphics.Rect old=icon.copyBounds();int inset=Math.round(size*.12f);icon.setBounds(Math.round(tile.left)+inset,Math.round(tile.top)+inset,Math.round(tile.right)-inset,Math.round(tile.bottom)-inset);icon.draw(c);icon.setBounds(old);
            if(labels)Ui.drawMenuLabel(c,item.label,x,y+size+2*d,cw-4*d,ch-size-3*d,font,model.labelColor());
        }
        c.restoreToCount(transformed);
        if(model.value("show_dock")==1&&!folder&&!drawer){int opacity=Math.max(50,Math.min(235,255*(100-model.crystal.transparency())/100+80));paint.setColor(model.value("dock_background")==1?(opacity<<24)|0x00223E58:0);float curve=Math.min(dockHeight/2,model.value("dock_radius")*d*.35f);c.drawRoundRect(new RectF(padding,h-4*d-dockHeight,w-padding,h-4*d),curve,curve,paint);text(c,"Start     Search     Controls"+(model.value("dock_clock")==1?"     12:00":""),w/2,h-4*d-dockHeight/2+3*d,9*d,Ui.TEXT);}
        c.restoreToCount(saved);
    }
    private void text(Canvas c,String text,float x,float y,float size,int color){paint.setColor(color);paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);paint.setShadowLayer(2,0,1,0xFF000000);c.drawText(text,x,y,paint);paint.clearShadowLayer();}
    @Override public boolean onTouchEvent(MotionEvent event){if(model.store.reducedMotion){if(event.getActionMasked()==MotionEvent.ACTION_UP)performClick();return true;}switch(event.getActionMasked()){case MotionEvent.ACTION_DOWN:dragging=true;sx=event.getX();sy=event.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;case MotionEvent.ACTION_MOVE:setRotationY(SceneMath.clamp((event.getX()-sx)/getResources().getDisplayMetrics().density/5,-15,15));setRotationX(SceneMath.clamp(-(event.getY()-sy)/getResources().getDisplayMetrics().density/5,-12,12));return true;case MotionEvent.ACTION_UP:performClick();case MotionEvent.ACTION_CANCEL:dragging=false;getParent().requestDisallowInterceptTouchEvent(false);animate().rotationX(0).rotationY(0).setDuration(240).start();return true;default:return true;}}
    @Override public boolean performClick(){super.performClick();if(page.equals("menu_animation")||page.equals("folder_animation"))playOpening();else if(page.equals("motion"))playTransition();return true;}
}
