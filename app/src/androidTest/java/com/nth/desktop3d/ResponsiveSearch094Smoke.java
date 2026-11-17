package com.nth.desktop3d;

import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Native small/large card content and local search navigation without executing queries. */
public final class ResponsiveSearch094Smoke extends Trial093Support {
    public ResponsiveSearch094Smoke(Instrumentation test,MainActivity activity,String output){super(test,activity,output);}
    public void run()throws Exception {
        snapshot();try{cards();search();}finally{restore();}
    }
    private String note(){StringBuilder out=new StringBuilder("Opening paragraph. ");for(int n=0;n<25;n++)out.append("Complete card text stays available "+n+". ");return out+"UniqueNeedle094 at the end.";}
    private void cards()throws Exception {
        main(()->{
            call(activity,"closePanel");store.preferences.edit().putString("note",note()).putInt("wallpaper_source",3).putBoolean("card_weather_location",false).putString("card_weather_city","").apply();
            try{LayeredScene.write(activity,new LayeredScene.Document(),false);}catch(Exception e){throw new AssertionError(e);}
            store.ambient=false;call(activity,"applySettings",true);
        });
        AtomicReference<DesktopCardView> shown=new AtomicReference<>();
        for(String[] type:CrystalCards.TYPES)for(int[] size:new int[][]{{170,210},{260,210},{170,360},{300,460}}){
            main(()->{
                LinearLayout p=(LinearLayout)call(activity,"openPanel","Card layout checks");
                FrameLayout slot=new FrameLayout(activity);p.addView(slot,new LinearLayout.LayoutParams(-1,0,1));
                LauncherStore.Item item=new LauncherStore.Item("card:"+type[0],type[1]);
                DesktopCardView view=new DesktopCardView(activity,store,item,activity.cardHost(),(v,x,y)->false,()->{});
                slot.addView(view,new FrameLayout.LayoutParams(Ui.dp(activity,size[0]),Ui.dp(activity,size[1])));view.active(true);shown.set(view);
            });
            SystemClock.sleep(160);DesktopCardView card=shown.get();await(()->card.getWidth()>0&&card.getHeight()>0,"Card did not measure");
            main(()->{
                AdaptiveCardScroll scroll=(AdaptiveCardScroll)card.getChildAt(0);
                check(scroll.getVerticalScrollbarWidth()>0,"Overflow has no visible scroll affordance");
                inspect(scroll);
                if(type[0].equals("notes"))check(find(scroll,v->v instanceof TextView&&note().contentEquals(((TextView)v).getText()))!=null,"Notes were truncated to a preview");
                ImageView image=(ImageView)find(scroll,v->v instanceof ImageView);if(image!=null)check(image.getScaleType()==ImageView.ScaleType.FIT_CENTER,"Photo was cropped at a small size");
            });
        }
        main(()->{
            call(activity,"closePanel");LinearLayout p=(LinearLayout)call(activity,"openPanel","Card resize checks");
            AdaptiveCardScroll scroll=new AdaptiveCardScroll(activity);scroll.setTag("adaptive-test");
            LinearLayout content=Ui.column(activity);TextView longTitle=Ui.text(activity,"A complete media title and artist that need more than one line to fit",18,Ui.TEXT);content.addView(longTitle);
            Button action=Ui.quiet(activity,"A complete button label that wraps over several lines",()->{});content.addView(action,new LinearLayout.LayoutParams(-1,48));scroll.addView(content);
            p.addView(scroll,new LinearLayout.LayoutParams(Ui.dp(activity,170),Ui.dp(activity,210)));
        });
        SystemClock.sleep(300);AtomicReference<Float> small=new AtomicReference<>();
        main(()->{AdaptiveCardScroll scroll=(AdaptiveCardScroll)find(v->"adaptive-test".equals(v.getTag()));TextView title=(TextView)((LinearLayout)scroll.getChildAt(0)).getChildAt(0);small.set(title.getTextSize());ViewGroup.LayoutParams lp=scroll.getLayoutParams();lp.width=Ui.dp(activity,300);lp.height=Ui.dp(activity,460);scroll.setLayoutParams(lp);});
        SystemClock.sleep(300);main(()->{AdaptiveCardScroll scroll=(AdaptiveCardScroll)find(v->"adaptive-test".equals(v.getTag()));TextView title=(TextView)((LinearLayout)scroll.getChildAt(0)).getChildAt(0);check(title.getTextSize()>=small.get(),"Enlarging did not restore readable text sizing");inspect(scroll);});
        capture("094-responsive-card");
    }
    private void inspect(View view){
        if(view instanceof TextView){TextView text=(TextView)view;check(text.getEllipsize()==null&&text.getMaxLines()==Integer.MAX_VALUE,"Card text is capped or ellipsized");check(text.getTextSize()+.5f>=12*activity.getResources().getDisplayMetrics().scaledDensity,"Card text became unreadably small");}
        if(view instanceof Button)check(view.getLayoutParams().height==ViewGroup.LayoutParams.WRAP_CONTENT&&view.getMinimumHeight()>=Ui.dp(activity,48),"Card action was clipped or lost its touch target");
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++)inspect(group.getChildAt(n));}
    }
    private void query(String text){main(()->{call(activity,"showStart");((EditText)field(activity,"appSearch")).setText(text);});}
    private View result(String tag){return find(v->tag.equals(v.getTag()));}
    private void search()throws Exception {
        main(()->{store.preferences.edit().putString("note",note()).putBoolean("search_folders",true).apply();});
        query("UniqueNeedle094");await(()->result("search-note")!=null,"Notes content was missing from unified search");
        main(()->result("search-note").performClick());await(()->find(v->v instanceof EditText&&note().contentEquals(((EditText)v).getText()))!=null,"Note result did not open the complete note");
        int strength=model.value("scene_light_strength");query("Scene lighting strength");await(()->result("search-setting:scene_light_strength")!=null,"Setting result missing");
        main(()->{check(model.value("scene_light_strength")==strength,"Typing a query changed a setting");result("search-setting:scene_light_strength").performClick();});
        await(()->find(v->"setting:scene_light_strength".equals(v.getTag()))!=null,"Setting result did not focus its control");
        main(()->check(model.value("scene_light_strength")==strength,"Opening a search result changed its value"));
        boolean tilt=store.tilt;query("turn off tilt");await(()->result("search-nota")!=null,"Nota draft action missing");
        main(()->{check(store.tilt==tilt,"Search executed a Nota command");result("search-nota").performClick();});
        await(()->find(v->v instanceof EditText&&"turn off tilt".contentEquals(((EditText)v).getText()))!=null,"Nota query was not opened as a draft");
        main(()->check(store.tilt==tilt,"Opening the Nota draft executed its command"));
        LauncherStore.Item folder=new LauncherStore.Item("folder","FolderNeedle094"),child=new LauncherStore.Item("builtin:calculator","Folder child094");folder.children.add(child);main(()->{store.pages.get(1).add(folder);store.save();});
        query("FolderNeedle094");await(()->find(v->("shortcut:"+folder.id).equals(v.getTag()))!=null,"A folder on another desktop was not searchable");
        main(()->find(v->("shortcut:"+folder.id).equals(v.getTag())).performClick());await(()->find(v->("shortcut:"+child.id).equals(v.getTag()))!=null,"Folder result opened the wrong saved folder");
        query("grid");capture("094-unified-search");main(()->((EditText)field(activity,"appSearch")).setText(""));main(()->check(((ScrollView)field(activity,"drawerSearchScroll")).getVisibility()==View.GONE,"Clearing search retained local results"));
    }
}
