package com.nth.desktop3d;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Scrollable 3D desktop miniatures; previews never rebind or allocate widgets. */
@android.annotation.SuppressLint("ViewConstructor")
final class DesktopManagerView extends LinearLayout {
    interface Host {void open(int page);void changed();void widgets();void scenes();void settings();void message(String text);}
    private final LauncherStore store;private final AppCatalog catalog;private final Host host;
    private final HorizontalScrollView scroll;private final LinearLayout cards;private final TextView help;private final java.util.List<View> previews=new ArrayList<>();
    private int dragged=-1,drop=-1,edge,cardWidth;private boolean disposed;private float tiltX,tiltY;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable turn=new Runnable(){public void run(){if(dragged<0||edge==0)return;scroll.smoothScrollBy(Ui.dp(getContext(),edge*70),0);main.postDelayed(this,140);}};
    DesktopManagerView(Context c,LauncherStore store,AppCatalog catalog,Host host){
        super(c);this.store=store;this.catalog=catalog;this.host=host;setOrientation(VERTICAL);setClipChildren(false);
        help=Ui.text(c,"Swipe previews. Tap a screen to open it. Hold and drag to reorder.",13,Ui.MUTED);help.setPadding(0,0,0,Ui.dp(c,8));addView(help,new LayoutParams(-1,-2));
        scroll=new HorizontalScrollView(c);scroll.setHorizontalScrollBarEnabled(false);scroll.setFillViewport(false);scroll.setClipToPadding(false);cards=Ui.row(c);cards.setGravity(Gravity.CENTER_VERTICAL);cards.setClipChildren(false);scroll.addView(cards,new HorizontalScrollView.LayoutParams(-2,-1));addView(scroll,new LayoutParams(-1,0,1));
        scroll.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_UP)scroll.postDelayed(this::snap,80);return false;});
        setOnDragListener((v,e)->drag(e));
        TextView hint=Ui.text(c,"The house marks your Home screen. Remove is available for empty screens.",12,Ui.MUTED);hint.setPadding(0,Ui.dp(c,8),0,Ui.dp(c,4));addView(hint);
        LinearLayout actions=Ui.row(c);for(String title:new String[]{"Widgets","Scenes","Settings"}){Button b=Ui.quiet(c,title,()->{if(title.equals("Widgets"))host.widgets();else if(title.equals("Scenes"))host.scenes();else host.settings();});actions.addView(b,new LayoutParams(0,Ui.dp(c,48),1));}addView(actions);
        post(this::refresh);
    }
    void refresh(){
        if(disposed)return;cards.removeAllViews();previews.clear();cardWidth=Math.max(Ui.dp(getContext(),170),Math.round(Math.max(Ui.dp(getContext(),240),getWidth())*.72f));
        for(int p=0;p<store.pageCount();p++)addCard(p);
        LinearLayout add=Ui.column(getContext());add.setGravity(Gravity.CENTER);add.setPadding(Ui.dp(getContext(),12),0,Ui.dp(getContext(),12),0);Button plus=Ui.quiet(getContext(),"＋\nAdd desktop",()->{if(locked())return;int index=store.appendPage();if(index<0){host.message("All 24 desktops are in use.");return;}store.page=index;store.save();host.changed();refresh();});plus.setContentDescription("Add desktop screen");plus.setTextSize(22);plus.setBackground(Ui.satin(getContext(),0xB2355F7A,0xD014263D,24));add.addView(plus,new LayoutParams(-1,-1));cards.addView(add,new LayoutParams(cardWidth,-1));
        scroll.post(()->{scroll.smoothScrollTo(Math.max(0,store.page*cardWidth-(scroll.getWidth()-cardWidth)/2),0);tilt(tiltX,tiltY);});
    }
    private boolean locked(){if(!store.preferences.getBoolean("lock_layout",false))return false;host.message("Unlock desktop layout to manage screens.");return true;}
    private void addCard(int page){
        Context c=getContext();LinearLayout card=Ui.column(c);card.setPadding(Ui.dp(c,10),0,Ui.dp(c,10),0);card.setTag("desktop-card:"+page);
        Button home=Ui.quiet(c,(store.homePage()==page?"⌂  Home · ":"⌂  Set Home · ")+store.pageName(page),()->{if(locked())return;store.setHomePage(page);refresh();host.message(store.pageName(page)+" is now Home.");});home.setSingleLine(false);home.setTextSize(13);home.setContentDescription("Set "+store.pageName(page)+" as Home");home.setSelected(store.homePage()==page);card.addView(home,new LayoutParams(-1,Ui.dp(c,44)));
        Miniature miniature=new Miniature(c,page);miniature.setContentDescription("Open "+store.pageName(page));miniature.setTag("desktop-preview:"+page);miniature.setElevation(Ui.dp(c,store.page==page?14:7));miniature.setCameraDistance(Ui.dp(c,1800));miniature.setOnClickListener(v->host.open(page));miniature.setOnLongClickListener(v->{if(locked())return true;dragged=page;drop=-1;boolean started=startDragAndDrop(android.content.ClipData.newPlainText("Nth desktop",store.pageName(page)),new View.DragShadowBuilder(miniature),this,0);if(!started)dragged=-1;else help.setText("Drag onto another desktop to move this screen.");return true;});card.addView(miniature,new LayoutParams(-1,0,1));previews.add(miniature);
        LinearLayout controls=Ui.row(c);Button left=Ui.icon(c,"left","Move "+store.pageName(page)+" left",()->move(page,page-1));left.setEnabled(page>0);controls.addView(left,new LayoutParams(Ui.dp(c,44),Ui.dp(c,44)));
        Button remove=Ui.quiet(c,"Remove",()->{if(locked())return;if(!store.removeEmptyPage(page)){host.message("Keep at least one screen. Move its shortcuts and widgets before removing it.");return;}host.changed();refresh();});remove.setContentDescription("Remove "+store.pageName(page));remove.setEnabled(store.pageCount()>1&&store.emptyPage(page));controls.addView(remove,new LayoutParams(0,Ui.dp(c,44),1));
        Button right=Ui.icon(c,"right","Move "+store.pageName(page)+" right",()->move(page,page+1));right.setEnabled(page<store.pageCount()-1);controls.addView(right,new LayoutParams(Ui.dp(c,44),Ui.dp(c,44)));card.addView(controls);
        card.setOnDragListener((v,e)->{if(e.getLocalState()!=this||dragged<0)return false;if(e.getAction()==DragEvent.ACTION_DROP){drop=page;return true;}if(e.getAction()==DragEvent.ACTION_DRAG_ENTERED)miniature.setAlpha(.65f);if(e.getAction()==DragEvent.ACTION_DRAG_EXITED||e.getAction()==DragEvent.ACTION_DRAG_ENDED)miniature.setAlpha(1);return true;});cards.addView(card,new LayoutParams(cardWidth,-1));
    }
    private void move(int from,int to){if(!locked()&&store.movePage(from,to)){host.changed();refresh();}}
    private boolean drag(DragEvent e){
        if(e.getLocalState()!=this||dragged<0)return false;
        if(e.getAction()==DragEvent.ACTION_DRAG_LOCATION){int next=e.getX()<Ui.dp(getContext(),35)?-1:e.getX()>getWidth()-Ui.dp(getContext(),35)?1:0;if(next!=edge){main.removeCallbacks(turn);edge=next;if(edge!=0)main.postDelayed(turn,300);}}
        if(e.getAction()==DragEvent.ACTION_DROP){drop=Math.min(store.pageCount()-1,Math.max(0,(int)((e.getX()+scroll.getScrollX())/Math.max(1,cardWidth))));return true;}
        if(e.getAction()==DragEvent.ACTION_DRAG_ENDED){int from=dragged,to=drop;dragged=drop=-1;edge=0;main.removeCallbacks(turn);if(e.getResult()&&to>=0)store.movePage(from,to);host.changed();help.setText("Swipe previews. Tap a screen to open it. Hold and drag to reorder.");post(this::refresh);}return true;
    }
    private void snap(){if(disposed||dragged>=0||cardWidth<=0)return;int page=Math.round((scroll.getScrollX()+(scroll.getWidth()-cardWidth)/2f)/cardWidth);scroll.smoothScrollTo(Math.max(0,Math.min(store.pageCount(),page)*cardWidth-(scroll.getWidth()-cardWidth)/2),0);}
    void tilt(float x,float y){tiltX=x;tiltY=y;for(View v:previews){v.setRotationY(store.reducedMotion?0:x*4);v.setRotationX(store.reducedMotion?0:-y*3);}}
    boolean cancelDrag(){if(dragged<0)return false;cancelDragAndDrop();return true;}
    void dispose(){disposed=true;cancelDragAndDrop();main.removeCallbacksAndMessages(null);}
    private final class Miniature extends View {
        private final int page;private final Paint paint=new Paint(3);private final Drawable background;private final Path clip=new Path();
        Miniature(Context c,int page){super(c);this.page=page;setClickable(true);setLongClickable(true);background=c.getDrawable(new int[]{R.drawable.scene_spatial,R.drawable.scene_gravity,R.drawable.scene_holographic,R.drawable.scene_pond}[store.mode]);setBackground(Ui.satin(c,0xDD345973,0xE411233A,22));}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);float w=getWidth(),h=getHeight(),d=getResources().getDisplayMetrics().density;if(w<=0||h<=0||page>=store.pageCount())return;
            clip.reset();clip.addRoundRect(new RectF(1,1,w-1,h-1),Ui.dp(getContext(),20),Ui.dp(getContext(),20),Path.Direction.CW);int saved=canvas.save();canvas.clipPath(clip);background.setBounds(0,0,(int)w,(int)h);background.draw(canvas);paint.setColor(0x20081425);canvas.drawRect(0,0,w,h,paint);
            boolean pattern=store.preferences.getBoolean("folder_pattern_page_"+page,false);int columns=pattern?5:store.preferences.getInt("grid_columns",4),rows=pattern?10:6;
            java.util.List<LauncherStore.Item> items=store.pages.get(page);for(LauncherStore.Item i:items)rows=Math.max(rows,(i.cellY<0?0:i.cellY)+1);
            java.util.List<int[]> widgets=new ArrayList<>();for(String raw:store.preferences.getString("widget_ids_page_"+page,"").split(","))try{int id=Integer.parseInt(raw),x=store.preferences.getInt("widget_cell_x_"+id,0),y=store.preferences.getInt("widget_cell_y_"+id,0),sw=store.preferences.getInt("widget_span_x_"+id,2),sh=store.preferences.getInt("widget_span_y_"+id,2);widgets.add(new int[]{id,x,y,sw,sh});rows=Math.max(rows,y+sh);}catch(NumberFormatException ignored){}
            float cw=(w-12*d)/Math.max(1,columns),ch=(h-26*d)/Math.max(1,rows),top=14*d;
            for(int[] widget:widgets){RectF box=new RectF(6*d+widget[1]*cw,top+widget[2]*ch,6*d+(widget[1]+widget[3])*cw-3*d,top+(widget[2]+widget[4])*ch-3*d);paint.setColor(0xDD20435A);canvas.drawRoundRect(box,6*d,6*d,paint);paint.setColor(Ui.MUTED);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(10*d,cw*.25f));canvas.drawText("Widget",box.centerX(),box.centerY()+paint.getTextSize()/3,paint);}
            for(int n=0;n<items.size();n++){LauncherStore.Item item=items.get(n);int x=item.cellX<0?n%columns:item.cellX,y=item.cellY<0?n/columns:item.cellY;float size=Math.max(3,Math.min(cw*.68f,ch*.64f)*store.preferences.getInt("icon_size",100)/100f);size=Math.min(size,Math.min(cw-3*d,ch*.70f));float left=6*d+(x+.5f)*cw-size/2,upper=top+y*ch;Drawable surface=Ui.iconSurface(getContext(),item.folder());surface.setBounds(Math.round(left),Math.round(upper),Math.round(left+size),Math.round(upper+size));surface.draw(canvas);Drawable icon=catalog.icon(item);if(icon==null)icon=new IconTile.BuiltinIcon(item.key);Rect old=icon.copyBounds();int inset=Math.round(size*.12f);icon.setBounds(Math.round(left)+inset,Math.round(upper)+inset,Math.round(left+size)-inset,Math.round(upper+size)-inset);icon.draw(canvas);icon.setBounds(old);if(store.preferences.getBoolean("desktop_labels",true)){paint.setColor(Ui.TEXT);paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(Math.min(cw/8,8*d));Ui.drawMenuLabel(canvas,item.label,left+size/2,upper+size+1,cw-2,ch-size-2,paint.getTextSize(),Ui.TEXT);}}
            canvas.restoreToCount(saved);
        }
    }
}
