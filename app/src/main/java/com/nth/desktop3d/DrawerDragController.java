package com.nth.desktop3d;

import android.view.View;
import android.view.MotionEvent;
import android.view.DragEvent;
import android.widget.FrameLayout;

/** One continuous Android drag from drawer to workspace, with edge paging and cancel rollback. */
final class DrawerDragController {
    interface Host {void closeDrawer();void changed();void message(String text);}
    private final FrameLayout root;private final DesktopView desktop;private final LauncherStore store;private final Host host;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private LauncherStore.Item source;private android.widget.TextView hint;private int originalPage,originalCount,edge,created=-1;private float x,y,anchorX,anchorY;private boolean dropped,tracking;
    private final Runnable turn=new Runnable(){public void run(){if(source==null||edge==0)return;int next=store.page+edge;if(next<0)return;if(next>=store.pageCount()){if(created>=0)return;created=store.appendPage();if(created<0)return;next=created;}desktop.dragPage(next);showHint();preview();main.postDelayed(this,900);}};
    DrawerDragController(FrameLayout root,DesktopView desktop,LauncherStore store,Host host){this.root=root;this.desktop=desktop;this.store=store;this.host=host;root.setOnDragListener((view,event)->drag(event));}
    boolean active(){return source!=null;}
    boolean tracking(){return tracking&&source!=null;}
    void cancel(){if(source!=null){root.cancelDragAndDrop();if(source!=null)finish();}}
    void install(View tile,LauncherStore.Item item,Runnable open,Runnable menu){
        tile.setClickable(true);tile.setLongClickable(true);tile.setOnClickListener(v->open.run());tile.setOnLongClickListener(v->{menu.run();return true;});
        tile.setOnTouchListener(new View.OnTouchListener(){float sx,sy;long downAt;boolean held,moved,started,canceled;void arm(){if(held||canceled)return;held=true;tile.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);if(tile.getParent()!=null)tile.getParent().requestDisallowInterceptTouchEvent(true);}
            final Runnable hold=this::arm;
            public boolean onTouch(View v,MotionEvent e){switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:downAt=e.getEventTime();sx=e.getRawX();sy=e.getRawY();held=moved=started=canceled=false;tile.postDelayed(hold,android.view.ViewConfiguration.getLongPressTimeout());tile.setPressed(true);if(tile.getParent()!=null)tile.getParent().requestDisallowInterceptTouchEvent(true);return true;
                case MotionEvent.ACTION_MOVE:if(!held&&!canceled&&e.getEventTime()-downAt>=android.view.ViewConfiguration.getLongPressTimeout())arm();float distance=(float)Math.hypot(e.getRawX()-sx,e.getRawY()-sy);if(!held&&distance>android.view.ViewConfiguration.get(tile.getContext()).getScaledTouchSlop()){tile.removeCallbacks(hold);canceled=true;if(tile.getParent()!=null)tile.getParent().requestDisallowInterceptTouchEvent(false);}if(held&&!started&&distance>Ui.dp(tile.getContext(),8)){moved=true;started=begin(tile,item);}return true;
                case MotionEvent.ACTION_UP:if(!held&&!canceled&&e.getEventTime()-downAt>=android.view.ViewConfiguration.getLongPressTimeout())arm();tile.removeCallbacks(hold);tile.setPressed(false);if(tile.getParent()!=null)tile.getParent().requestDisallowInterceptTouchEvent(false);if(!moved&&!canceled){if(held)menu.run();else tile.performClick();}return true;
                case MotionEvent.ACTION_CANCEL:tile.removeCallbacks(hold);tile.setPressed(false);if(tile.getParent()!=null)tile.getParent().requestDisallowInterceptTouchEvent(false);return true;
                default:return true;
            }}
        });
    }
    boolean beginDesktop(View tile,LauncherStore.Item item){return begin(tile,item);}
    boolean beginDesktop(View tile,LauncherStore.Item item,float ax,float ay){return begin(tile,item,ax,ay);}
    private boolean begin(View tile,LauncherStore.Item item){return begin(tile,item,tile.getWidth()/2f,tile.getHeight()/2f);}
    private boolean begin(View tile,LauncherStore.Item item,float ax,float ay){
        if(store.preferences.getBoolean("lock_layout",false)){host.message("Unlock desktop layout to add or move shortcuts.");return false;}
        int[] pixels=desktop.sizedPixels(item);if(item.sized()&&pixels==null){host.message("This screen is too short for a card. Rotate the phone or reduce the grid row size.");return false;}anchorX=item.sized()?Math.max(0,Math.min(1,ax/Math.max(1,tile.getWidth())))*pixels[0]:0;anchorY=item.sized()?Math.max(0,Math.min(1,ay/Math.max(1,tile.getHeight())))*pixels[1]:0;
        originalPage=store.page;originalCount=store.pageCount();created=-1;dropped=tracking=false;source=item;
        // The drawer is removed as dragging begins. Keep the source view attached
        // for the complete system drag, including on newer Android releases.
        tile.setPressed(false);
        boolean accepted=root.startDragAndDrop(android.content.ClipData.newPlainText("Nth shortcut",item.label),item.sized()?new View.DragShadowBuilder(tile){@Override public void onProvideShadowMetrics(android.graphics.Point size,android.graphics.Point touch){size.set(pixels[0],pixels[1]);touch.set(Math.round(anchorX),Math.round(anchorY));}@Override public void onDrawShadow(android.graphics.Canvas canvas){canvas.save();canvas.scale(pixels[0]/(float)Math.max(1,tile.getWidth()),pixels[1]/(float)Math.max(1,tile.getHeight()));tile.draw(canvas);canvas.restore();}}:new View.DragShadowBuilder(tile),this,0);
        if(!accepted){source=null;return false;}host.closeDrawer();showHint();return true;
    }
    private boolean drag(DragEvent e){
        if(e.getLocalState()!=this||source==null)return false;
        switch(e.getAction()){
            case DragEvent.ACTION_DRAG_STARTED:tracking=true;location(e);return true;
            case DragEvent.ACTION_DRAG_LOCATION:location(e);return true;
            case DragEvent.ACTION_DROP:x=e.getX();y=e.getY();float[] p=point();dropped=(!source.sized()||desktop.dragPoint(p[0],p[1]))&&desktop.dropExternal(source,p[0]-anchorX,p[1]-anchorY);if(!dropped)host.message("Choose a free desktop space.");return dropped;
            case DragEvent.ACTION_DRAG_ENDED:finish();return true;
            default:return true;
        }
    }
    private void location(DragEvent e){x=e.getX();y=e.getY();int next=x<Ui.dp(root.getContext(),32)?-1:x>root.getWidth()-Ui.dp(root.getContext(),32)?1:0;if(next!=edge){main.removeCallbacks(turn);edge=next;if(edge!=0)main.postDelayed(turn,650);}preview();}
    private float[] point(){int[] a=new int[2],b=new int[2];root.getLocationOnScreen(a);desktop.getLocationOnScreen(b);return new float[]{x+a[0]-b[0],y+a[1]-b[1]};}
    private void preview(){float[] p=point();desktop.previewExternal(source,p[0]-anchorX,p[1]-anchorY);}
    private void showHint(){if(hint==null){hint=Ui.text(root.getContext(),"",13,Ui.TEXT);hint.setGravity(android.view.Gravity.CENTER);hint.setPadding(8,8,8,8);hint.setBackground(Ui.satin(root.getContext(),0xEF325266,0xEF112B43,14));FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-2,-2,android.view.Gravity.TOP|android.view.Gravity.CENTER_HORIZONTAL);lp.topMargin=Ui.dp(root.getContext(),38);root.addView(hint,lp);}hint.setText("Desktop "+(store.page+1)+" · release to place · hold at an edge to change screens");host.changed();}
    private void finish(){main.removeCallbacks(turn);edge=0;desktop.clearExternal();if(hint!=null){root.removeView(hint);hint=null;}if(!dropped)store.page=Math.min(originalPage,store.pageCount()-1);boolean cleaned=false;for(int p=store.pageCount()-1;p>=originalCount;p--)if(p!=store.page&&store.emptyPage(p)){store.removeEmptyPage(p);cleaned=true;}if(!dropped||cleaned)desktop.rebuild();source=null;tracking=false;store.save();host.changed();}
}
