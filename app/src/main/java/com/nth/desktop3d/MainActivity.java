package com.nth.desktop3d;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements MotionController.Listener, DesktopView.Host {
    private LauncherStore store;
    private CrystalPreferences crystal;
    private AppCatalog catalog;
    private MotionController motion;
    private SceneSurface scene;
    private DesktopView desktop;
    private FrameLayout root,overlay;
    private LinearLayout shell,header,bar,panel,dock,paging;
    private TextView clock,date,modeLabel,pageLabel,status;
    private Button clockButton,startButton;
    private Runnable onPanelClose;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService loader=Executors.newSingleThreadExecutor();
    private boolean resumed,destroyed,loaded,powerSave,systemReduced,loading;
    private long previousFrame,lastRender;
    private long rippleUntil;
    private boolean sceneDirty=true;
    private int insetTop,insetBottom,insetLeft,insetRight,imeBottom;
    private int panelKind; // 1 apps, 2 other; allows package callbacks to refresh app results.
    private boolean flyoutPanel;
    private EditText appSearch;
    private AppsAdapter appsAdapter;
    private float currentX,currentY;

    private final Choreographer.FrameCallback frames=new Choreographer.FrameCallback(){
        @Override public void doFrame(long time){
            if(!resumed||destroyed)return;
            float dt=previousFrame==0?1/60f:Math.min(.05f,(time-previousFrame)/1_000_000_000f);previousFrame=time;
            desktop.frame(dt);
            // Cap GPU work at 60 fps (30 in battery saver), independently of screen refresh.
            long interval=powerSave?33_000_000L:16_000_000L;
            boolean animate=store.ambient&&!store.reducedMotion&&!systemReduced&&!powerSave;
            if(time-lastRender>=interval&&(animate||sceneDirty||android.os.SystemClock.uptimeMillis()<rippleUntil)){scene.requestRender();lastRender=time;sceneDirty=false;}
            Choreographer.getInstance().postFrameCallback(this);
        }
    };
    private final Runnable clockTick=new Runnable(){@Override public void run(){if(!resumed)return;updateClock();handler.postDelayed(this,1000);}};
    private final BroadcastReceiver powerReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){applySettings(false);}};
    private final LauncherApps.Callback packageCallback=new LauncherApps.Callback(){
        @Override public void onPackageRemoved(String p,UserHandle u){reloadApps();}
        @Override public void onPackageAdded(String p,UserHandle u){reloadApps();}
        @Override public void onPackageChanged(String p,UserHandle u){reloadApps();}
        @Override public void onPackagesAvailable(String[] p,UserHandle u,boolean r){reloadApps();}
        @Override public void onPackagesUnavailable(String[] p,UserHandle u,boolean r){reloadApps();}
    };

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(0xE60A1428);
        store=new LauncherStore(this);crystal=new CrystalPreferences(this);catalog=new AppCatalog(this);motion=new MotionController(this,this);
        root=new FrameLayout(this);root.setBackgroundColor(0xFF061326);setContentView(root);
        scene=new SceneSurface(this);root.addView(scene,new FrameLayout.LayoutParams(-1,-1));
        shell=Ui.column(this);shell.setClipChildren(false);shell.setClipToPadding(false);root.addView(shell,new FrameLayout.LayoutParams(-1,-1));
        buildHeader();
        desktop=new DesktopView(this,store,catalog,this);shell.addView(desktop,new LinearLayout.LayoutParams(-1,0,1));
        buildTaskbar();
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets s=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());insetLeft=s.left;insetTop=s.top;insetRight=s.right;insetBottom=s.bottom;}
            else{insetLeft=insets.getSystemWindowInsetLeft();insetTop=insets.getSystemWindowInsetTop();insetRight=insets.getSystemWindowInsetRight();insetBottom=insets.getSystemWindowInsetBottom();}
            // IME remains inside the safe panel and does not move the taskbar sideways.
            imeBottom=Build.VERSION.SDK_INT>=30&&insets.isVisible(WindowInsets.Type.ime())?insets.getInsets(WindowInsets.Type.ime()).bottom:0;
            shell.setPadding(insetLeft+dp(compact()?16:20),insetTop+dp(8),insetRight+dp(compact()?16:20),insetBottom+dp(8));
            int chromeWidth=Math.min(dp(560),Math.max(1,(root.getWidth()>0?root.getWidth():getResources().getDisplayMetrics().widthPixels)-insetLeft-insetRight-dp(compact()?32:40)));
            for(View chrome:new View[]{dock,paging}){LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(chromeWidth,-2);cp.gravity=Gravity.CENTER_HORIZONTAL;chrome.setLayoutParams(cp);}
            layoutPanel();return insets;
        });root.requestApplyInsets();
        catalog.launcher.registerCallback(packageCallback,handler);
        registerReceiver(powerReceiver,new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        applySettings(true);reloadApps();
        if(!store.preferences.getBoolean("welcomed",false))handler.postDelayed(this::welcome,450);
    }
    private int dp(float n){return Ui.dp(this,n);}
    private boolean compact(){return getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;}
    private void buildHeader(){
        header=Ui.column(this);header.setClipChildren(false);
        LinearLayout heading=Ui.row(this);
        android.widget.ImageView mark=new android.widget.ImageView(this);mark.setImageDrawable(new GlyphDrawable("cube",Ui.ACCENT));heading.addView(mark,new LinearLayout.LayoutParams(dp(22),dp(25)));
        TextView brand=Ui.text(this,"NTH DESKTOP",11,Ui.ACCENT);brand.setLetterSpacing(.16f);brand.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(44),1);bp.leftMargin=dp(10);heading.addView(brand,bp);
        Button mode=Ui.quiet(this,"Scenes",this::showModes);mode.setContentDescription("Scenes");heading.addView(mode,new LinearLayout.LayoutParams(dp(80),dp(48)));header.addView(heading);
        LinearLayout info=Ui.row(this);info.setPadding(0,dp(4),0,dp(4));LinearLayout time=Ui.column(this);
        clock=Ui.text(this,"",compact()?30:54,Ui.TEXT);clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));clock.setLetterSpacing(-.04f);clock.setIncludeFontPadding(false);time.addView(clock);
        date=Ui.text(this,"",13,Ui.MUTED);date.setPadding(dp(2),dp(5),0,0);time.addView(date);info.addView(time,new LinearLayout.LayoutParams(0,-2,1));
        Button all=Ui.icon(this,"grid","All shortcuts",this::showDesktopOverview);info.addView(all,new LinearLayout.LayoutParams(dp(48),dp(48)));header.addView(info);
        modeLabel=Ui.text(this,LauncherStore.MODES[store.mode],11,Ui.ACCENT);modeLabel.setLetterSpacing(.04f);modeLabel.setPadding(dp(2),dp(12),0,0);header.addView(modeLabel);
        shell.addView(header,new LinearLayout.LayoutParams(-1,-2));
    }
    private void buildTaskbar(){
        bar=Ui.column(this);bar.setClipChildren(false);bar.setClipToPadding(false);
        paging=Ui.row(this);
        Button previous=Ui.icon(this,"left","Previous desktop",()->desktop.changePage(-1));paging.addView(previous,new LinearLayout.LayoutParams(dp(48),dp(48)));
        pageLabel=Ui.text(this,"",11,Ui.MUTED);pageLabel.setGravity(Gravity.CENTER);paging.addView(pageLabel,new LinearLayout.LayoutParams(0,dp(48),1));
        Button next=Ui.icon(this,"right","Next desktop",()->desktop.changePage(1));paging.addView(next,new LinearLayout.LayoutParams(dp(48),dp(48)));bar.addView(paging);
        dock=Ui.row(this);dock.setBackground(Ui.satin(this,0xB344819C,0xCF0B1E36,24));dock.setPadding(dp(8),dp(7),dp(8),dp(7));dock.setElevation(dp(18));
        startButton=Ui.icon(this,"cube","Open 3D Start menu",this::showStart);dock.addView(startButton,new LinearLayout.LayoutParams(dp(48),dp(48)));
        Button search=Ui.quiet(this,"Search apps",this::showStart);search.setTextSize(13);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(48),1);dock.addView(search,sp);
        Button controls=Ui.icon(this,"sliders","Device controls",this::showControls);dock.addView(controls,new LinearLayout.LayoutParams(dp(48),dp(48)));
        View divider=new View(this);divider.setBackgroundColor(0x24D9E7FF);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(dp(1),dp(26));line.setMargins(dp(5),0,dp(8),0);dock.addView(divider,line);
        clockButton=Ui.quiet(this,"",this::showCalendar);clockButton.setPadding(0,0,0,0);clockButton.setContentDescription("Open calendar and time");dock.addView(clockButton,new LinearLayout.LayoutParams(dp(65),dp(48)));
        dock.setOnLongClickListener(v->{backgroundMenu();return true;});bar.addView(dock);shell.addView(bar,new LinearLayout.LayoutParams(-1,-2));updateClock();
    }
    private void updateClock(){
        Date now=new Date();String pattern=android.text.format.DateFormat.is24HourFormat(this)?"HH:mm":"h:mm";
        String time=new SimpleDateFormat(pattern,Locale.getDefault()).format(now);clock.setText(time);
        date.setText(new SimpleDateFormat("EEEE, MMMM d",Locale.getDefault()).format(now));
        clockButton.setText(time+"\n"+new SimpleDateFormat("MMM d",Locale.getDefault()).format(now));clockButton.setTextSize(11);
    }
    @Override protected void onResume(){super.onResume();resumed=true;previousFrame=lastRender=0;scene.onResume();applySettings(false);Choreographer.getInstance().removeFrameCallback(frames);Choreographer.getInstance().postFrameCallback(frames);handler.removeCallbacks(clockTick);clockTick.run();if(loaded)reloadApps();}
    @Override protected void onPause(){resumed=false;Choreographer.getInstance().removeFrameCallback(frames);handler.removeCallbacks(clockTick);motion.pause();scene.onPause();desktop.savePositions();store.save();savePanel();super.onPause();}
    @Override protected void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);catalog.launcher.unregisterCallback(packageCallback);unregisterReceiver(powerReceiver);loader.shutdownNow();super.onDestroy();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);closePanel();motion.recenter();}
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);closePanel();motion.recenter();clock.setTextSize(compact()?30:54);desktop.rebuild();root.requestApplyInsets();}
    @Override public void onBackPressed(){if(overlay!=null)closePanel();else if(store.page!=0){store.page=0;desktop.rebuild();pageChanged();store.save();}}
    @Override public int displayRotation(){return getWindowManager().getDefaultDisplay().getRotation();}
    @Override public void onTilt(float x,float y){if(Math.abs(x-currentX)>.002f||Math.abs(y-currentY)>.002f)sceneDirty=true;currentX=x;currentY=y;if(scene!=null)scene.tilt(x,y);if(desktop!=null)desktop.tilt(x,y);}
    private void applySettings(boolean rebuild){
        powerSave=getSystemService(PowerManager.class).isPowerSaveMode();
        systemReduced=Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1)==0;
        boolean reduced=store.reducedMotion||systemReduced;
        scene.configure(store,powerSave,systemReduced);desktop.configure(reduced,powerSave);modeLabel.setText(LauncherStore.MODES[store.mode]+(reduced?" · reduced motion":""));
        if(resumed)motion.resume(store.tilt&&!reduced&&!powerSave);if(rebuild)desktop.rebuild();sceneDirty=true;scene.requestRender();pageChanged();
    }
    private void reloadApps(){
        if(loading||destroyed)return;loading=true;
        loader.execute(()->{
            List<AppCatalog.Entry> next;
            try{next=catalog.load();}catch(RuntimeException e){next=new ArrayList<>();}
            final List<AppCatalog.Entry> result=next;
            handler.post(()->{loading=false;if(destroyed)return;catalog.apply(result);loaded=true;
                if(!store.preferences.getBoolean("appsSeeded",false)){
                    int added=0;for(AppCatalog.Entry e:result)if(e.component!=null&&added<5){store.add(0,e.key,e.label);added++;}
                    if(added>0)store.preferences.edit().putBoolean("appsSeeded",true).apply();
                }
                desktop.rebuild();pageChanged();if(panelKind==1&&appsAdapter!=null)appsAdapter.filter(appSearch==null?"":appSearch.getText().toString());
            });
        });
    }
    @Override public void pageChanged(){
        int count=store.pages.get(store.page).size();pageLabel.setText("Desktop "+(store.page+1)+" of "+LauncherStore.PAGE_COUNT+"  ·  "+count+" shortcut"+(count==1?"":"s"));
    }
    @Override public void ripple(float x,float y){
        if(store.mode==3&&store.ripples&&!store.reducedMotion&&!systemReduced)rippleUntil=android.os.SystemClock.uptimeMillis()+3600;
        int[] loc=new int[2];desktop.getLocationInWindow(loc);scene.ripple((x+loc[0])/Math.max(1,root.getWidth()),(y+loc[1])/Math.max(1,root.getHeight()));
    }
    @Override public void open(LauncherStore.Item item){if(item.folder())showFolder(item);else launch(item.key);}
    private void launch(String key){
        if(key.startsWith("builtin:")){
            switch(key.substring(8)){case "notes":showNotes();break;case "calculator":showCalculator();break;case "calendar":showCalendar();break;case "settings":showSettings();break;case "modes":showModes();break;default:showAbout();}return;
        }
        AppCatalog.Entry e=catalog.find(key);if(e==null){toast("This app is unavailable. Hold its shortcut to remove it.");return;}
        try{closePanel();catalog.launcher.startMainActivity(e.component,e.user,null,null);}catch(SecurityException|IllegalStateException|android.content.ActivityNotFoundException ex){toast("Android could not open this app. Check its profile or App info.");}
    }
    private void savePanel(){if(onPanelClose!=null)onPanelClose.run();}
    private void closePanel(){
        savePanel();onPanelClose=null;panelKind=0;appsAdapter=null;appSearch=null;
        if(overlay!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(overlay.getWindowToken(),0);root.removeView(overlay);overlay=null;panel=null;}
        desktop.setModal(false);startButton.setSelected(false);scene.requestRender();
    }
    private LinearLayout openPanel(String title){return openPanel(title,false);}
    private LinearLayout openPanel(String title,boolean flyout){
        closePanel();desktop.setModal(true);startButton.setSelected(true);panelKind=2;
        flyoutPanel=flyout;
        overlay=new FrameLayout(this);overlay.setBackgroundColor(0xAA030A17);overlay.setOnClickListener(v->closePanel());root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        panel=Ui.column(this);panel.setBackground(Ui.satin(this,0xE3335C76,0xF20B1A30,26));panel.setElevation(dp(34));panel.setPadding(dp(20),dp(12),dp(20),dp(16));panel.setOnClickListener(v->{});panel.setCameraDistance(dp(2400));
        overlay.addView(panel);layoutPanel();
        LinearLayout heading=Ui.row(this);TextView label=Ui.text(this,title,20,Ui.TEXT);label.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));heading.addView(label,new LinearLayout.LayoutParams(0,dp(46),1));
        Button close=Ui.icon(this,"close","Close "+title,this::closePanel);heading.addView(close,new LinearLayout.LayoutParams(dp(46),dp(46)));panel.addView(heading);
        if(!store.reducedMotion&&!systemReduced){
            LinearLayout opening=panel;
            opening.setAlpha(0);opening.setTranslationY(dp(20));
            opening.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){
                @Override public boolean onPreDraw(){opening.getViewTreeObserver().removeOnPreDrawListener(this);if(panel!=opening)return true;
                    opening.setPivotX(opening.getWidth()*.5f);opening.setPivotY(opening.getHeight()*(store.menuStyle==2?1:.65f));
                    if(store.menuStyle==1)opening.setRotationY(-18);else opening.setRotationX(store.menuStyle==2?-28:-9);
                    opening.animate().alpha(1).translationY(0).rotationX(0).rotationY(0).setInterpolator(new android.view.animation.DecelerateInterpolator()).setDuration(240).start();return true;}
            });
        }
        return panel;
    }
    private void layoutPanel(){
        if(panel==null)return;
        int bottom=Math.max(insetBottom,imeBottom);
        PanelGeometry.Bounds bounds=PanelGeometry.place(root.getWidth(),root.getHeight(),insetLeft,insetTop,insetRight,bottom,dp(flyoutPanel?520:720),dp(flyoutPanel?520:820),dp(12),flyoutPanel&&imeBottom==0?dp(84):0);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(bounds.width,bounds.height);
        // Center inside the safe rectangle. FrameLayout's CENTER gravity applies
        // unequal margins as offsets, which can put a keyboard-resized panel offscreen.
        p.gravity=Gravity.TOP|Gravity.LEFT;
        p.leftMargin=bounds.left;p.topMargin=bounds.top;
        panel.setLayoutParams(p);
    }
    private LinearLayout scrollBody(LinearLayout parent){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);parent.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout body=Ui.column(this);body.setPadding(0,dp(10),0,dp(8));scroll.addView(body,new ScrollView.LayoutParams(-1,-2));return body;
    }
    private void paragraph(LinearLayout body,String text){TextView p=Ui.text(this,text,14,Ui.MUTED);p.setPadding(0,dp(8),0,dp(14));p.setLineSpacing(dp(3),1);body.addView(p,new LinearLayout.LayoutParams(-1,-2));}
    private void action(LinearLayout body,String text,Runnable action){Button b=Ui.quiet(this,text,action);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(2);body.addView(b,p);}
    private void section(LinearLayout body,String name){TextView h=Ui.text(this,name,16,Ui.TEXT);h.setTypeface(null,Typeface.BOLD);h.setPadding(0,dp(16),0,dp(8));body.addView(h);}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}

    private void showStart(){
        LinearLayout p=openPanel("Your apps");panelKind=1;
        appSearch=new EditText(this);appSearch.setSingleLine(true);appSearch.setHint("Search apps and Nth tools");appSearch.setTextColor(Ui.TEXT);appSearch.setHintTextColor(Ui.MUTED);appSearch.setTextSize(16);appSearch.setSelectAllOnFocus(true);appSearch.setContentDescription("Search installed apps");
        appSearch.setBackground(Ui.surface(0xFF0B172B,14,this));appSearch.setPadding(dp(14),0,dp(14),0);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(50));ep.setMargins(0,dp(10),0,dp(10));p.addView(appSearch,ep);
        LinearLayout shortcuts=Ui.row(this);
        Button all=Ui.button(this,"All apps",()->{if(appsAdapter!=null){appsAdapter.onlyBuiltins=false;appsAdapter.filter(appSearch.getText().toString());}});
        Button nth=Ui.button(this,"Nth tools",()->{if(appsAdapter!=null){appsAdapter.onlyBuiltins=true;appsAdapter.filter(appSearch.getText().toString());}});
        shortcuts.addView(all,new LinearLayout.LayoutParams(0,dp(44),1));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,dp(44),1);np.leftMargin=dp(8);shortcuts.addView(nth,np);p.addView(shortcuts);
        GridView grid=new GridView(this);grid.setNumColumns(compact()?6:3);grid.setVerticalSpacing(dp(10));grid.setHorizontalSpacing(dp(8));grid.setPadding(0,dp(16),0,dp(10));grid.setClipToPadding(false);grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setSelector(android.R.color.transparent);
        appsAdapter=new AppsAdapter();grid.setAdapter(appsAdapter);p.addView(grid,new LinearLayout.LayoutParams(-1,0,1));appsAdapter.filter("");
        grid.setOnItemClickListener((a,v,pos,id)->launch(appsAdapter.items.get(pos).key));
        grid.setOnItemLongClickListener((a,v,pos,id)->{appMenu(appsAdapter.items.get(pos),null);return true;});
        appSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){if(appsAdapter!=null)appsAdapter.filter(s.toString());}public void afterTextChanged(Editable e){}});
        appSearch.setOnEditorActionListener((v,action,event)->{if(appsAdapter!=null&&appsAdapter.items.size()==1){launch(appsAdapter.items.get(0).key);return true;}return false;});
        TextView tip=Ui.text(this,"Hold an app for shortcuts, App info, or uninstall.",12,Ui.MUTED);tip.setPadding(0,dp(6),0,0);p.addView(tip);p.setFocusableInTouchMode(true);p.requestFocus();
    }
    private final class AppsAdapter extends BaseAdapter {
        final List<AppCatalog.Entry> items=new ArrayList<>();boolean onlyBuiltins;
        void filter(String q){items.clear();items.addAll(catalog.filter(q,onlyBuiltins));notifyDataSetChanged();}
        @Override public int getCount(){return items.size();}@Override public Object getItem(int n){return items.get(n);}@Override public long getItemId(int n){return items.get(n).key.hashCode();}
        @Override public View getView(int n,View old,ViewGroup parent){AppCatalog.Entry e=items.get(n);IconTile tile=new IconTile(MainActivity.this,e.label,e.icon,e.key);tile.setFocusable(false);tile.setClickable(false);tile.setLongClickable(false);tile.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1,dp(114)));return tile;}
    }

    @Override public void menu(LauncherStore.Item item){
        if(!item.folder()){appMenu(catalog.find(item.key),item);return;}
        LinearLayout body=scrollBody(openPanel(item.label));action(body,"Open folder",()->showFolder(item));action(body,"Rename folder",()->rename(item));
        action(body,"Move to another desktop",()->move(item));action(body,"Remove folder…",()->confirmRemove(item));
    }
    private void appMenu(AppCatalog.Entry entry,LauncherStore.Item shortcut){
        String label=shortcut!=null?shortcut.label:entry.label;String key=shortcut!=null?shortcut.key:entry.key;
        LinearLayout body=scrollBody(openPanel(label,true));action(body,"Open",()->launch(key));
        if(shortcut==null)action(body,"Add to this desktop",()->{boolean added=store.add(store.page,key,label);desktop.rebuild();pageChanged();toast(added?"Shortcut added to Desktop "+(store.page+1):"That shortcut is already here, or this desktop is full.");closePanel();});
        else{
            action(body,"Rename shortcut",()->rename(shortcut));
            if(store.pages.get(store.page).contains(shortcut)){action(body,shortcut.fixed?"Unpin in Gravity Playground":"Pin in Gravity Playground",()->{shortcut.fixed=!shortcut.fixed;store.save();desktop.rebuild();closePanel();});action(body,"Move to another desktop",()->move(shortcut));}
            action(body,"Remove shortcut",()->{store.remove(shortcut);desktop.rebuild();pageChanged();closePanel();});
        }
        if(!store.folders().isEmpty())action(body,"Add to folder…",()->chooseFolder(key,label));
        if(entry!=null&&entry.component!=null){
            action(body,"App info",()->{try{catalog.launcher.startAppDetailsActivity(entry.component,entry.user,null,null);}catch(RuntimeException e){toast("App info is unavailable for this profile.");}});
            action(body,"Uninstall…",()->{try{Intent i=new Intent(Intent.ACTION_DELETE,Uri.fromParts("package",entry.component.getPackageName(),null));i.putExtra(Intent.EXTRA_USER,entry.user);startActivity(i);}catch(RuntimeException e){toast("Open App info to manage this app.");}});
        }
    }
    private void rename(LauncherStore.Item item){
        EditText edit=new EditText(this);edit.setText(item.label);edit.setSingleLine(true);edit.setSelectAllOnFocus(true);edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(60)});Ui.pad(edit,16);
        new AlertDialog.Builder(this).setTitle("Rename").setView(edit).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String s=edit.getText().toString().trim();if(!s.isEmpty()){item.label=s;store.save();desktop.rebuild();closePanel();}}).show();
    }
    private void move(LauncherStore.Item item){
        new AlertDialog.Builder(this).setTitle("Move shortcut").setItems(new String[]{"Desktop 1","Desktop 2","Desktop 3"},(d,w)->{if(store.pages.get(w).size()>=60){toast("That desktop is full.");return;}store.remove(item);store.pages.get(w).add(item);item.x=item.y=-1;store.save();desktop.rebuild();pageChanged();closePanel();}).show();
    }
    private void confirmRemove(LauncherStore.Item item){new AlertDialog.Builder(this).setTitle("Remove "+item.label+"?").setMessage("This removes the folder and its shortcuts. Installed apps remain on your device.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{store.remove(item);desktop.rebuild();pageChanged();closePanel();}).show();}
    private void chooseFolder(String key,String label){
        List<LauncherStore.Item> folders=store.folders();String[] names=new String[folders.size()];for(int i=0;i<names.length;i++)names[i]=folders.get(i).label;
        new AlertDialog.Builder(this).setTitle("Add to folder").setItems(names,(d,w)->{LauncherStore.Item folder=folders.get(w);for(LauncherStore.Item c:folder.children)if(c.key.equals(key)){toast("Already in this folder.");return;}if(folder.children.size()>=100){toast("This folder is full.");return;}folder.children.add(new LauncherStore.Item(key,label));store.save();toast("Added to "+folder.label);closePanel();}).show();
    }
    private void showFolder(LauncherStore.Item folder){
        LinearLayout body=scrollBody(openPanel(folder.label));
        if(folder.children.isEmpty())paragraph(body,"Hold an app in Start and choose Add to folder to put it here.");
        for(LauncherStore.Item item:folder.children){Button b=Ui.button(this,item.label,()->open(item));b.setOnLongClickListener(v->{menu(item);return true;});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.bottomMargin=dp(8);body.addView(b,p);}
        action(body,"Rename folder",()->rename(folder));
    }
    private void showDesktopOverview(){
        LinearLayout body=scrollBody(openPanel("Desktop shortcuts"));
        for(int i=0;i<LauncherStore.PAGE_COUNT;i++){section(body,"Desktop "+(i+1));if(store.pages.get(i).isEmpty())paragraph(body,"No shortcuts yet.");for(LauncherStore.Item item:store.pages.get(i)){Button b=Ui.button(this,item.label,()->open(item));b.setOnLongClickListener(v->{menu(item);return true;});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.bottomMargin=dp(6);body.addView(b,p);}}
    }
    @Override public void backgroundMenu(){
        LinearLayout body=scrollBody(openPanel("Desktop",true));action(body,"Scene studio",this::showModes);action(body,"3D settings",this::showSettings);action(body,"New folder",this::newFolder);action(body,"Arrange shortcuts",()->{desktop.arrange();closePanel();});action(body,"All desktop shortcuts",this::showDesktopOverview);action(body,"Recenter tilt",()->{motion.recenter();closePanel();toast("Tilt centered");});action(body,"Set default Home",this::requestHome);
    }
    private void newFolder(){EditText edit=new EditText(this);edit.setSingleLine(true);edit.setHint("Folder name");Ui.pad(edit,16);new AlertDialog.Builder(this).setTitle("New folder").setView(edit).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->{String name=edit.getText().toString().trim();if(!name.isEmpty()&&store.pages.get(store.page).size()<60){store.pages.get(store.page).add(new LauncherStore.Item("folder",name));store.save();desktop.rebuild();pageChanged();closePanel();}}).show();}

    private void showModes(){
        LinearLayout p=openPanel("Scene studio");LinearLayout body=scrollBody(p);
        paragraph(body,"A different dimension for your desktop.");
        String[] descriptions={"Layered mountains. Sculpted light.","Tilt, toss, and play with your shortcuts.","A quiet orbit through a field of stars.","Koi, floating leaves, and touch ripples."};
        for(int n=0;n<4;n++){
            final int selected=n;SceneCard card=new SceneCard(this,n,descriptions[n],store.mode==n);
            card.setOnClickListener(v->{desktop.savePositions();store.mode=selected;store.save();applySettings(true);showModes();});
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(108));cp.bottomMargin=dp(12);body.addView(card,cp);
        }
        Button tryScene=Ui.primary(this,"Try this scene",this::closePanel);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(52));tp.topMargin=dp(10);p.addView(tryScene,tp);
        Button mix=Ui.quiet(this,"Menus and transitions",this::showSettings);p.addView(mix,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    private void showSettings(){
        LinearLayout body=scrollBody(openPanel("3D settings"));
        section(body,"Scene and movement");action(body,"Scene: "+LauncherStore.MODES[store.mode],this::showModes);
        toggle(body,"Tilt parallax",store.tilt,v->{store.tilt=v;changed(false);});
        slider(body,"Depth strength",Math.round(store.strength*100),0,100,v->{store.strength=v/100f;changed(false);});
        toggle(body,"Ambient scene animation",store.ambient,v->{store.ambient=v;changed(false);});
        toggle(body,"Reduced motion",store.reducedMotion,v->{store.reducedMotion=v;changed(true);});
        paragraph(body,motion.sensorName()+(powerSave?" · battery saver pauses tilt and physics":"")+(systemReduced?" · Android animations are disabled":""));
        action(body,"Recenter tilt",()->{motion.recenter();toast("Hold the phone comfortably; tilt is centered.");});
        section(body,"Desktop transition");choice(body,LauncherStore.TRANSITIONS,store.transition,v->{store.transition=v;changed(false);showSettings();});
        section(body,"Start menu and flyouts");choice(body,LauncherStore.MENUS,store.menuStyle,v->{store.menuStyle=v;changed(false);showSettings();});
        section(body,"Living Pond");slider(body,"Fish count",store.fishCount,3,7,v->{store.fishCount=v;changed(false);});slider(body,"Fish speed",Math.round(store.fishSpeed*100),20,150,v->{store.fishSpeed=v/100f;changed(false);});
        toggle(body,"Water ripples",store.ripples,v->{store.ripples=v;changed(false);});toggle(body,"Fish and leaf shadows",store.shadows,v->{store.shadows=v;changed(false);});
        section(body,"Crystal Glass · visual defaults");
        slider(body,"Glass transparency",crystal.transparency(),0,100,v->crystal.setTransparency(v));
        slider(body,"Crystal depth",crystal.depth(),0,100,v->crystal.setDepth(v));
        slider(body,"Crystal motion",crystal.motion(),0,100,v->crystal.setMotion(v));
        toggle(body,"Crystal parallax",crystal.parallax(),v->crystal.setParallax(v));
        toggle(body,"Crystal dynamic shadows",crystal.shadows(),v->crystal.setShadows(v));
        action(body,"Rendering: "+crystal.quality(),()->new AlertDialog.Builder(this).setTitle("Rendering quality").setSingleChoiceItems(new String[]{"Performance","Balanced","Cinematic"},crystal.qualityIndex(),(d,which)->{crystal.setQuality(which);d.dismiss();showSettings();}).setNegativeButton("Cancel",null).show());
        paragraph(body,"Crystal controls are saved now; renderer material integration is a subsequent pass.");
        section(body,"Feed Page");
        toggle(body,"Enable feed",crystal.feedEnabled(),v->crystal.setFeedEnabled(v));
        action(body,"Feed provider: "+crystal.feedProviderLabel(),()->new AlertDialog.Builder(this).setTitle("Feed provider").setSingleChoiceItems(new String[]{"Google Discover (preferred)","Google App","Nth Feed (coming later)","Off"},crystal.feedProvider(),(d,which)->{crystal.setFeedProvider(which);d.dismiss();showSettings();}).setNegativeButton("Cancel",null).show());
        toggle(body,"Edge swipe",crystal.feedEdgeSwipe(),v->crystal.setFeedEdgeSwipe(v));
        toggle(body,"Feed 3D transition",crystal.feed3dTransition(),v->crystal.setFeed3dTransition(v));
        action(body,"Open feed (Google App fallback)",this::openGoogleFeed);
        paragraph(body,"Discover companion compatibility is experimental and not yet integrated. Google App fallback opens the installed Google app.");
        section(body,"Launcher");action(body,"Set default Home",this::requestHome);action(body,"Arrange current desktop",()->{desktop.arrange();toast("Shortcuts arranged");});action(body,"About Nth Desktop 3D",this::showAbout);
    }
    private void openGoogleFeed(){
        Intent intent=getPackageManager().getLaunchIntentForPackage("com.google.android.googlequicksearchbox");
        if(intent==null){toast("Google app unavailable. Install or enable Google to use the fallback.");return;}
        try{startActivity(intent);}catch(RuntimeException e){toast("Could not open Google app.");}
    }
    private interface BoolAction{void set(boolean v);}private interface IntAction{void set(int v);}
    private void changed(boolean rebuild){store.save();applySettings(rebuild);}
    private void toggle(LinearLayout body,String label,boolean value,BoolAction change){Switch s=new Switch(this);s.setText(label);s.setTextSize(15);s.setTextColor(Ui.TEXT);s.setPadding(0,dp(8),0,dp(8));s.setMinHeight(dp(52));s.setChecked(value);s.setOnCheckedChangeListener((b,v)->change.set(v));body.addView(s,new LinearLayout.LayoutParams(-1,-2));}
    private void slider(LinearLayout body,String label,int value,int min,int max,IntAction change){TextView t=Ui.text(this,label+"  "+value,14,Ui.MUTED);t.setPadding(0,dp(12),0,0);body.addView(t);SeekBar s=new SeekBar(this);s.setMax(max-min);s.setProgress(value-min);s.setContentDescription(label);body.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int progress,boolean user){t.setText(label+"  "+(progress+min));if(user)change.set(progress+min);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});}
    private void choice(LinearLayout body,String[] choices,int selected,IntAction change){for(int i=0;i<choices.length;i++){final int n=i;Button b=Ui.button(this,(i==selected?"✓  ":"")+choices[i],()->change.set(n));b.setSelected(i==selected);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.bottomMargin=dp(7);body.addView(b,p);}}

    private void showNotes(){
        LinearLayout p=openPanel("Nth Notes");paragraph(p,"Saved on this device as you type.");
        EditText edit=new EditText(this);edit.setGravity(Gravity.TOP);edit.setTextColor(Ui.TEXT);edit.setHintTextColor(Ui.MUTED);edit.setTextSize(17);edit.setHint("Write something…");edit.setBackground(Ui.surface(0xFF0E1A2E,14,this));Ui.pad(edit,14);edit.setText(store.preferences.getString("note",""));p.addView(edit,new LinearLayout.LayoutParams(-1,0,1));
        Runnable save=()->store.preferences.edit().putString("note",edit.getText().toString()).apply();onPanelClose=save;
        edit.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){save.run();}public void afterTextChanged(Editable e){}});
    }
    private void showCalculator(){
        LinearLayout body=scrollBody(openPanel("Calculator"));EditText expression=new EditText(this);expression.setSingleLine(true);expression.setTextColor(Ui.TEXT);expression.setHintTextColor(Ui.MUTED);expression.setHint("2 × (3 + 4)");expression.setTextSize(24);expression.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);expression.setShowSoftInputOnFocus(false);body.addView(expression,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView result=Ui.text(this,"0",32,Ui.ACCENT);result.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);body.addView(result,new LinearLayout.LayoutParams(-1,dp(64)));
        String[][] keys={{"C","(",")","⌫"},{"7","8","9","÷"},{"4","5","6","×"},{"1","2","3","−"},{"0",".","=","+"}};
        for(String[] row:keys){LinearLayout l=Ui.row(this);for(String key:row){Button b=Ui.button(this,key,()->{String old=expression.getText().toString();if(key.equals("C")){expression.setText("");result.setText("0");}else if(key.equals("⌫")){if(!old.isEmpty())expression.setText(old.substring(0,old.length()-1));}else if(key.equals("=")){try{double v=Expression.evaluate(old);result.setText(java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString());}catch(IllegalArgumentException e){result.setText(e.getMessage());result.setTextSize(17);}}else if(old.length()<180){expression.append(key);result.setTextSize(32);}expression.setSelection(expression.length());});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1);lp.setMargins(dp(3),dp(3),dp(3),dp(3));l.addView(b,lp);}body.addView(l);}
    }
    private void showCalendar(){
        LinearLayout body=scrollBody(openPanel("Calendar and time",true));TextView time=Ui.text(this,DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.SHORT).format(new Date()),17,Ui.TEXT);time.setPadding(0,dp(8),0,dp(12));body.addView(time);CalendarView calendar=new CalendarView(this);body.addView(calendar,new LinearLayout.LayoutParams(-1,dp(320)));action(body,"Open Android calendar",()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("content://com.android.calendar/time/"+System.currentTimeMillis())));}catch(RuntimeException e){toast("No calendar app is installed.");}});
    }
    private void showControls(){
        LinearLayout body=scrollBody(openPanel("Device controls",true));paragraph(body,"Open Android’s controls for your device. Tilt and scene settings are available below.");
        action(body,"Internet",()->systemPanel(Settings.Panel.ACTION_INTERNET_CONNECTIVITY,Settings.ACTION_WIFI_SETTINGS));action(body,"Volume",()->systemPanel(Settings.Panel.ACTION_VOLUME,Settings.ACTION_SOUND_SETTINGS));action(body,"Bluetooth",()->openSystem(Settings.ACTION_BLUETOOTH_SETTINGS));action(body,"Display and brightness",()->openSystem(Settings.ACTION_DISPLAY_SETTINGS));action(body,"Battery",()->openSystem(Settings.ACTION_BATTERY_SAVER_SETTINGS));action(body,"3D settings",this::showSettings);
    }
    private void systemPanel(String action,String fallback){if(Build.VERSION.SDK_INT>=29){try{startActivity(new Intent(action));return;}catch(RuntimeException ignored){}}openSystem(fallback);}
    private void openSystem(String action){try{startActivity(new Intent(action));}catch(RuntimeException e){toast("That settings screen is unavailable on this device.");}}
    private void showAbout(){
        LinearLayout body=scrollBody(openPanel("Nth Desktop 3D"));paragraph(body,"Version 0.2.0\n\nFour scenes. One desktop.\n\nTilt gently to explore depth. Swipe the desktop or use the arrows to turn pages. Hold an app for its menu. Hold the taskbar for desktop options.\n\nScene studio changes the environment. 3D settings mixes the transition and menu style independently.");
        paragraph(body,"This first version includes app launching, persistent shortcuts and folders, Nth Notes, Calculator, Calendar, scene controls, and Android settings shortcuts. Android widgets, the wider Nth utility suite, and .nthdesktop imports are planned for later passes.");
        action(body,"Scene studio",this::showModes);action(body,"3D settings",this::showSettings);action(body,"Set default Home",this::requestHome);
    }
    private void welcome(){
        if(destroyed||!resumed)return;LinearLayout body=scrollBody(openPanel("Welcome to Nth Desktop 3D"));paragraph(body,"Your desktop has depth.\n\nTilt your phone gently for parallax. Swipe or tap the arrows to turn desktops. Open the cube in the dock for apps and hold an app for shortcut options. Try all four scenes from Scene studio.\n\nReduced motion, tilt strength, menu styles, and transitions are in 3D settings.");
        action(body,"Set as my Home app",this::requestHome);action(body,"Explore the desktop",()->{store.preferences.edit().putBoolean("welcomed",true).apply();closePanel();});action(body,"Choose a scene",()->{store.preferences.edit().putBoolean("welcomed",true).apply();showModes();});
    }
    private void requestHome(){
        store.preferences.edit().putBoolean("welcomed",true).apply();
        if(Build.VERSION.SDK_INT>=29){RoleManager roles=getSystemService(RoleManager.class);if(roles.isRoleAvailable(RoleManager.ROLE_HOME)){if(roles.isRoleHeld(RoleManager.ROLE_HOME)){toast("Nth Desktop 3D is already your Home app.");return;}try{startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME),101);return;}catch(RuntimeException ignored){}}}
        openSystem(Settings.ACTION_HOME_SETTINGS);
    }
}
