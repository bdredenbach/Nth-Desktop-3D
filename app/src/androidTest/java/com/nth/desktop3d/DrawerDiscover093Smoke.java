package com.nth.desktop3d;

import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

/** Native horizontal paging/search plus a silent binding recovered across a UID boundary. */
public final class DrawerDiscover093Smoke extends Trial093Support {
    public DrawerDiscover093Smoke(Instrumentation test,MainActivity activity,String output){super(test,activity,output);}
    public void run()throws Exception {
        snapshot();DiscoverClient client=(DiscoverClient)field(activity,"discover");
        CrystalPreferences prefs=new CrystalPreferences(activity);boolean feed=prefs.feedEnabled();int provider=prefs.feedProvider();
        try{
            main(()->{
                client.disable();store.preferences.edit().putInt("drawer_scroll",1).putBoolean("search_keyboard",false).apply();
                check(model.specs.get("drawer_scroll").options[1].equals("Horizontal"),"Drawer preference not registered");
                call(activity,"showStart");
            });
            await(()->grid()!=null&&grid().getChildCount()>0&&grid().pageCount()>1,"Horizontal drawer has no pages");
            SwipeDismissGrid grid=grid();int capacity=grid.getAdapter().getCount();
            Rect r=rect(grid);long start=SystemClock.uptimeMillis();float y=r.centerY();
            pointer(start,MotionEvent.ACTION_DOWN,r.right-r.width()*.2f,y);
            move(start,r.right-r.width()*.2f,y,r.left+r.width()*.2f,y);
            pointer(start,MotionEvent.ACTION_UP,r.left+r.width()*.2f,y);
            await(()->grid.page()==1,"Horizontal native swipe did not advance the drawer");
            main(()->check(grid.sourcePosition(0)==capacity,"Drawer page lost its catalog offset"));
            capture("093-horizontal-app-drawer");
            main(()->{
                check(grid.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null),"Accessible drawer paging failed");
                check(grid.page()==0,"Backward paging did not return to page one");
                ((EditText)field(activity,"appSearch")).setText("zzzz-nth-no-results");
            });
            await(()->grid.getAdapter().getCount()==0&&grid.pageCount()==1&&grid.page()==0,"Search left a stale or unreachable drawer page");
            main(()->((EditText)field(activity,"appSearch")).setText(""));
            await(()->grid.getAdapter().getCount()>0,"Clearing search did not restore apps");
            main(()->check(grid.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,null),"Second drawer page unavailable for drag"));
            SystemClock.sleep(200);
            Object expected=grid.getAdapter().getItem(0);String expectedKey=expected instanceof AppCatalog.Entry?((AppCatalog.Entry)expected).key:((LauncherStore.Item)expected).key;
            Rect icon=rect(grid.getChildAt(0));long held=SystemClock.uptimeMillis();float ix=icon.centerX(),iy=icon.centerY();
            pointer(held,MotionEvent.ACTION_DOWN,ix,iy);SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+200);
            pointer(held,MotionEvent.ACTION_MOVE,ix+Ui.dp(activity,20),iy);
            DrawerDragController drag=(DrawerDragController)field(activity,"drawerDrags");
            await(drag::active,"Holding an icon on a horizontal page did not start a drag");
            main(()->{check(expectedKey.equals(((LauncherStore.Item)field(drag,"source")).key),"Horizontal page dragged the wrong catalog item");drag.cancel();});
            pointer(held,MotionEvent.ACTION_UP,ix+Ui.dp(activity,20),iy);await(()->!drag.active(),"Horizontal drawer drag did not cancel");
            main(()->{
                try{check(model.exportSettings().getJSONObject("values").getInt("drawer_scroll")==1,"Drawer mode absent from settings backup");}catch(Exception e){throw new AssertionError(e);}
                call(activity,"closePanel");store.preferences.edit().putInt("drawer_scroll",0).apply();call(activity,"showStart");
            });
            await(()->grid()!=null&&grid().getChildCount()>0,"Vertical drawer missing");
            main(()->check(!grid().performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null),"Vertical drawer retained horizontal paging"));
            main(()->{call(activity,"closePanel");client.disable();});
            mode("stale_binding");
            main(()->{
                set(client,"targetOverride","com.nth.desktop3d.widgetfixture");set(client,"componentOverride",null);
                prefs.setFeedProvider(0);prefs.setFeedEnabled(true);client.start();client.resume();client.open();
            });
            SystemClock.sleep(1500);main(()->check(!client.showing()&&client.isConnecting(),"Stale binding falsely became ready"));
            await(client::showing,"Fresh binding session did not recover the silent provider");
            main(()->check(client.report().contains("fresh binding session")&&client.report().contains("Outcome: VISIBLE"),"Recovery report omitted fresh-binding evidence"));
            capture("093-discover-fresh-session");
            main(()->{client.close();client.disable();});mode("not_ready");main(client::open);
            SystemClock.sleep(8500);
            main(()->check(client.report().contains("Ready bits: 0")&&!client.report().contains("fresh binding session"),"Explicit not-ready status triggered stale-binding recovery"));
            main(client::close);
        }finally{
            main(()->{client.disable();set(client,"targetOverride",null);set(client,"componentOverride",null);prefs.setFeedProvider(provider);prefs.setFeedEnabled(feed);});
            mode("normal");restore();
        }
    }
    private SwipeDismissGrid grid(){return (SwipeDismissGrid)find(v->v instanceof SwipeDismissGrid);}
    private void mode(String value)throws Exception {
        try(ParcelFileDescriptor fd=test.getUiAutomation().executeShellCommand("am broadcast -n com.nth.desktop3d.widgetfixture/.FixtureCommandReceiver --es discoverMode "+value);
            InputStream in=new FileInputStream(fd.getFileDescriptor())){byte[] b=new byte[1024];while(in.read(b)!=-1){}}
    }
}
