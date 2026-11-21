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
    private LauncherWidgetHost widgetHost,dockWidgetHost;
    private DockView dockContents;
    private int pendingWidgetDock=-1;
    private String sceneFocus;
    private android.appwidget.AppWidgetManager widgetManager;
    private int pendingWidgetId=-1,pendingWidgetPage=-1,pendingWidgetRequest;
    private long pendingWidgetUser=-1;
    private boolean widgetListening;
    private android.content.ComponentName pendingWidgetProvider;
    private AppCatalog catalog;
    private SettingsModel settingsModel;
    private SettingsStudio settingsStudio;
    private CrystalCards cardsView;
    private NativeFeedPage nativeFeed;
    private String cardsQuery="";
    private DiscoverClient discover;
    private WallpaperLayer wallpaper;
    private LayerSceneView layeredWallpaper;private SceneComposer sceneComposer;private long layeredStamp=-1;private byte[] pendingSceneImage;private boolean pendingSceneObject;
    private android.app.AlertDialog restoreConfirmation;
    private SetupGuide setupGuide;
    private final Runnable setupReturn=()->restoreSetup();
    private AdaptiveMenuDrawable menuGlass;
    private long lastGlassUpdate;
    private final java.util.Set<String> newlyInstalled=new java.util.HashSet<>();
    private PinnedShortcuts shortcuts;
    private MotionController motion;
    private SceneSurface scene;
    private DesktopView desktop;private DesktopManagerView desktopManager;private DrawerDragController drawerDrags;private int appliedHomeRotation=-1;
    private FrameLayout root,overlay;
    private LinearLayout shell,header,bar,panel,dock,paging;
    private TextView clock,date,modeLabel,pageLabel,status;
    private AlertDialog areaMenu;private Button clockButton,startButton,areaButton;private String panelArea;
    private Runnable onPanelClose;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService loader=Executors.newSingleThreadExecutor();
    private final ExecutorService sceneFiles=Executors.newSingleThreadExecutor();
    private boolean resumed,destroyed,loaded,powerSave,systemReduced,loading;
    private long previousFrame,lastRender;
    private long rippleUntil;
    private boolean sceneDirty=true;
    private int insetTop,insetBottom,insetLeft,insetRight,imeBottom;
    private int panelKind; // 1 apps, 2 other; allows package callbacks to refresh app results.
    private boolean flyoutPanel;
    private EditText appSearch;
    private AppsAdapter appsAdapter;
    private LinearLayout drawerSearchResults;private ScrollView drawerSearchScroll;private SwipeDismissGrid drawerGrid;
    private String drawerResultQuery;private final List<Runnable> drawerLocalTargets=new ArrayList<>();private int drawerLocalCount;
    private float currentX,currentY;
    private FolderMotion folderMotion;private String physicalFolderId;
    private SceneRandomizer sceneRandomizer;private long shuffleHomeAfter;private AlertDialog randomizerDialog;private AlertDialog scenePackConfirmation;
    private SceneLighting sceneLighting;
    SceneLighting sceneLighting(){return sceneLighting;}

    private final Choreographer.FrameCallback frames=new Choreographer.FrameCallback(){
        @Override public void doFrame(long time){
            if(!resumed||destroyed)return;
            float dt=previousFrame==0?1/60f:Math.min(.05f,(time-previousFrame)/1_000_000_000f);previousFrame=time;
            sceneLighting.tick(scene.elapsedSeconds(),currentX,currentY,store.tilt&&crystal.parallax()&&crystal.dynamicLighting());
            desktop.frame(dt);updateMenuGlass();
            // Cap GPU work at 60 fps (30 in battery saver), independently of screen refresh.
            // Rendering tiers govern GPU submission cadence; Android power saver always wins.
            long interval=(powerSave||crystal.qualityIndex()==0)?33_000_000L:16_000_000L;
            boolean animate=store.ambient&&!store.reducedMotion&&!systemReduced&&!powerSave;
            if(scene.getVisibility()==View.VISIBLE&&time-lastRender>=interval&&(animate||sceneDirty||android.os.SystemClock.uptimeMillis()<rippleUntil)){scene.requestRender();lastRender=time;sceneDirty=false;}
            Choreographer.getInstance().postFrameCallback(this);
        }
    };
    private final Runnable clockTick=new Runnable(){@Override public void run(){if(!resumed)return;updateClock();randomizeIfDue();handler.postDelayed(this,1000);}};
    private final BroadcastReceiver powerReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){applySettings(false);}};
    private final LauncherApps.Callback packageCallback=new LauncherApps.Callback(){
        @Override public void onShortcutsChanged(String p,List<android.content.pm.ShortcutInfo> items,UserHandle user){if(shortcuts!=null)shortcuts.refresh(p,items);}
        @Override public void onPackageRemoved(String p,UserHandle u){reloadApps();}
        @Override public void onPackageAdded(String p,UserHandle u){if(store.preferences.getBoolean("auto_add_apps",false))newlyInstalled.add(p);reloadApps();}
        @Override public void onPackageChanged(String p,UserHandle u){reloadApps();if(DiscoverClient.GOOGLE.equals(p)&&discover!=null)discover.providerChanged();}
        @Override public void onPackagesAvailable(String[] p,UserHandle u,boolean r){reloadApps();for(String name:p)if(DiscoverClient.GOOGLE.equals(name)&&discover!=null)discover.providerChanged();}
        @Override public void onPackagesUnavailable(String[] p,UserHandle u,boolean r){reloadApps();}
    };

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(0xE60A1428);
        store=new LauncherStore(this);sceneRandomizer=new SceneRandomizer(store);crystal=new CrystalPreferences(this);
        sceneLighting=new SceneLighting(this);sceneLighting.configure(store,crystal,false,false);
        if(!store.preferences.getBoolean("padding_expands_073",false)){android.content.SharedPreferences.Editor padding=store.preferences.edit();for(String key:store.preferences.getAll().keySet())if(key.startsWith("widget_padding_"))padding.putBoolean(key,!store.preferences.getBoolean(key,true));padding.putBoolean("padding_expands_073",true).apply();}
        if(state==null&&Intent.ACTION_MAIN.equals(getIntent().getAction())&&getIntent().hasCategory(Intent.CATEGORY_HOME)){store.page=store.homePage();store.save();}
        widgetHost=new LauncherWidgetHost(this);dockWidgetHost=new LauncherWidgetHost(this,3048);
        pendingWidgetDock=store.preferences.getInt("pending_widget_dock",-1);
        pendingWidgetId=store.preferences.getInt("pending_widget_id",-1);
        pendingWidgetPage=store.preferences.getInt("pending_widget_page",store.page);
        pendingWidgetRequest=store.preferences.getInt("pending_widget_request",0);
        pendingWidgetUser=store.preferences.getLong("pending_widget_user",getSystemService(android.os.UserManager.class).getSerialNumberForUser(android.os.Process.myUserHandle()));
        String pendingProvider=store.preferences.getString("pending_widget_provider",null);
        if(pendingProvider!=null)pendingWidgetProvider=android.content.ComponentName.unflattenFromString(pendingProvider);
        widgetManager=android.appwidget.AppWidgetManager.getInstance(this);catalog=new AppCatalog(this);motion=new MotionController(this,this);
        root=new FrameLayout(this);root.setBackgroundColor(0xFF061326);setContentView(root);discover=new DiscoverClient(this,root,this::showDiscoverEntry,this::showDiscoverConnecting);discover.onDesktopReturn(this::restoreDesktopMotion);
        scene=new SceneSurface(this);root.addView(scene,new FrameLayout.LayoutParams(-1,-1));wallpaper=new WallpaperLayer(this);root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));layeredWallpaper=new LayerSceneView(this);root.addView(layeredWallpaper,new FrameLayout.LayoutParams(-1,-1));
        shell=Ui.column(this);shell.setClipChildren(false);shell.setClipToPadding(false);root.addView(shell,new FrameLayout.LayoutParams(-1,-1));
        buildHeader();
        desktop=new DesktopView(this,store,catalog,this,widgetHost,widgetManager);shell.addView(desktop,new LinearLayout.LayoutParams(-1,0,1));
        shortcuts=new PinnedShortcuts(this,store,catalog.launcher,id->{desktop.rebuild();if(id!=null)desktop.revealItem(id);pageChanged();});catalog.shortcuts=shortcuts;
        drawerDrags=new DrawerDragController(root,desktop,store,new DrawerDragController.Host(){public void closeDrawer(){closePanel();}public void changed(){pageChanged();}public void message(String text){toast(text);}});
        buildTaskbar();
        // Immersive home: gestures replace persistent desktop chrome.
        header.setVisibility(View.GONE);bar.setVisibility(View.GONE);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets s=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());insetLeft=s.left;insetTop=s.top;insetRight=s.right;insetBottom=s.bottom;}
            else{insetLeft=insets.getSystemWindowInsetLeft();insetTop=insets.getSystemWindowInsetTop();insetRight=insets.getSystemWindowInsetRight();insetBottom=insets.getSystemWindowInsetBottom();}
            // IME remains inside the safe panel and does not move the taskbar sideways.
            imeBottom=Build.VERSION.SDK_INT>=30&&insets.isVisible(WindowInsets.Type.ime())?insets.getInsets(WindowInsets.Type.ime()).bottom:0;
            applyDesktopChrome();
            layoutPanel();return insets;
        });root.requestApplyInsets();
        catalog.launcher.registerCallback(packageCallback,handler);
        registerReceiver(powerReceiver,new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        applySettings(true);applyCrystalAppearance();reloadApps();
        boolean shortcutHandled=shortcuts.handle(getIntent());
        if(setupPending())handler.post(this::welcome);
        else {
            if(!shortcutHandled&&!store.preferences.getBoolean("welcomed",false))handler.postDelayed(this::welcome,450);
            if(state!=null&&state.getBoolean("scene_creator",false))handler.post(this::showSceneCreator);
            else if(state!=null&&state.containsKey("setup_step"))handler.post(()->showSetup(state.getInt("setup_step")));
            else if(state!=null&&state.getBoolean("desktop_manager",false))handler.post(this::showDesktopManager);
            else if(state!=null&&state.getString("settings_page")!=null&&!LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(getIntent().getAction()))handler.post(()->showSettings(state.getString("settings_page")));
            else if(state!=null&&state.getBundle("native_feed")!=null&&!LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(getIntent().getAction()))handler.post(()->{showNativeFeed();nativeFeed.restore(state.getBundle("native_feed"));});
        }
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
        dockContents=new DockView(this,store,catalog,dockWidgetHost,new DockView.Host(){public void open(LauncherStore.Item item){MainActivity.this.open(item);}public void space(int index){showDockSpace(index);}public void restore(){showRestoreWidgets();}});bar.addView(dockContents);
        dock=Ui.row(this);dock.setBackground(Ui.satin(this,0xB344819C,0xCF0B1E36,24));dock.setPadding(dp(8),dp(7),dp(8),dp(7));dock.setElevation(dp(18));
        startButton=Ui.icon(this,"cube","Open 3D Start menu",this::showStart);dock.addView(startButton,new LinearLayout.LayoutParams(dp(48),dp(48)));
        Button search=Ui.quiet(this,"Search apps",this::showStart);search.setTextSize(13);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(48),1);dock.addView(search,sp);
        Button controls=Ui.icon(this,"sliders","Device controls",this::showControls);dock.addView(controls,new LinearLayout.LayoutParams(dp(48),dp(48)));
        View divider=new View(this);divider.setBackgroundColor(0x24D9E7FF);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(dp(1),dp(26));line.setMargins(dp(5),0,dp(8),0);dock.addView(divider,line);
        clockButton=Ui.quiet(this,"",this::showCalendar);clockButton.setPadding(0,0,0,0);clockButton.setContentDescription("Open calendar and time");dock.addView(clockButton,new LinearLayout.LayoutParams(dp(65),dp(48)));
        dock.setOnLongClickListener(v->{backgroundMenu();return true;});bar.addView(dock);shell.addView(bar,new LinearLayout.LayoutParams(-1,-2));updateClock();
    }
    private void applyDesktopChrome(){
        int padding=PaddingSettings.inset(compact()?16:20,store.preferences.getInt("desktop_padding",0));
        shell.setPadding(insetLeft+dp(padding),insetTop+dp(8),insetRight+dp(padding),insetBottom+dp(8));
        boolean controls=store.preferences.getBoolean("show_dock",false),spaces=store.preferences.getBoolean("dock_content",false),visibleDock=controls||spaces;
        bar.setVisibility(visibleDock?View.VISIBLE:View.GONE);dock.setVisibility(controls?View.VISIBLE:View.GONE);paging.setVisibility(controls?View.VISIBLE:View.GONE);desktop.setClipChildren(visibleDock);desktop.setClipToPadding(visibleDock);
        dockContents.rebuild();
        clockButton.setVisibility(store.preferences.getBoolean("dock_clock",true)?View.VISIBLE:View.GONE);
        int adjustment=store.preferences.getInt("dock_padding",0),horizontal=PaddingSettings.inset(8,adjustment),vertical=PaddingSettings.inset(7,adjustment);
        int chromeWidth=Math.min(dp(560),Math.max(1,(root.getWidth()>0?root.getWidth():getResources().getDisplayMetrics().widthPixels)-insetLeft-insetRight-dp(padding*2)));
        int safeHorizontal=Math.min(dp(horizontal),Math.max(0,(chromeWidth-dp(store.preferences.getBoolean("dock_clock",true)?223:158))/2));dock.setPadding(safeHorizontal,dp(vertical),safeHorizontal,dp(vertical));
        for(View chrome:new View[]{dock,paging,dockContents}){LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(chromeWidth,-2);cp.gravity=Gravity.CENTER_HORIZONTAL;chrome.setLayoutParams(cp);}
    }
    private void applyCrystalAppearance(){
        if(dock!=null){int opacity=Math.max(50,Math.min(235,255*(100-crystal.transparency())/100+80));dock.setBackground(store.preferences.getBoolean("dock_background",true)?Ui.satin(this,(opacity<<24)|0x0044819C,0xCF0B1E36,store.preferences.getInt("dock_radius",24)):new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));}
        if(panel!=null){
            boolean reading = "nota".equals(panelArea);
            boolean adaptive = store.preferences.getBoolean("adaptive_glass", true);
            if (reading) overlay.setBackground(NotaDesign.sceneScrim());
            else overlay.setBackgroundColor(adaptive || flyoutPanel ? Color.TRANSPARENT : 0xAA030A17);
            if(adaptive){if(!(panel.getBackground() instanceof AdaptiveMenuDrawable)){menuGlass=new AdaptiveMenuDrawable(this);panel.setBackground(menuGlass);}menuGlass.radius("folder".equals(panel.getTag())?store.preferences.getInt("folder_radius",26):26);}
            else {
                menuGlass = null;
                int alpha = Math.max(175, Math.min(245, 255 - crystal.transparency() * 2 / 3));
                android.graphics.drawable.Drawable surface = flyoutPanel
                        ? new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
                        : Ui.satin(this, (alpha << 24) | 0x00335C76, 0xF20B1A30, 26);
                if (reading) surface.setAlpha(96 - crystal.transparency() / 2);
                panel.setBackground(surface);
            }
        }
        styleAreaMenu();
    }
    private View areaMenuBody;private AdaptiveMenuDrawable areaMenuGlass;
    private java.util.List<android.graphics.RectF> menuBackdrops(View target){
        if (!shell.isShown()) return java.util.Collections.emptyList();
        java.util.List<android.graphics.RectF> boxes=desktopManager==null||target!=panel?desktop.backdropRects(target):new java.util.ArrayList<>();
        if(dockContents!=null&&dockContents.isShown())for(int i=0;i<dockContents.getChildCount();i++)BackdropGeometry.append(boxes,dockContents.getChildAt(i),target);
        BackdropGeometry.append(boxes,dock,target);BackdropGeometry.append(boxes,paging,target);return boxes;
    }
    private void styleAreaMenu(){
        if(areaMenuBody==null)return;if(store.preferences.getBoolean("adaptive_glass",true)){
            if(!(areaMenuBody.getBackground() instanceof AdaptiveMenuDrawable)){areaMenuGlass=new AdaptiveMenuDrawable(this);areaMenuBody.setBackground(areaMenuGlass);}else areaMenuGlass=(AdaptiveMenuDrawable)areaMenuBody.getBackground();areaMenuGlass.radius(26);
        }else{areaMenuGlass=null;areaMenuBody.setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));}
    }
    void presentAreaMenu(AlertDialog dialog,View body,String title){
        if(areaMenu!=null&&areaMenu.isShowing())areaMenu.dismiss();View covered=panel;if(covered!=null)covered.setVisibility(View.INVISIBLE);areaMenu=dialog;areaMenuBody=body;styleAreaMenu();
        body.setTag("fading-area-menu");body.setCameraDistance(dp(2400));if(Build.VERSION.SDK_INT>=28)body.setAccessibilityPaneTitle(title);
        dialog.setOnDismissListener(d->{if(covered!=null&&panel==covered)covered.setVisibility(View.VISIBLE);if(areaMenu==dialog){areaMenu=null;areaMenuBody=null;areaMenuGlass=null;}});
        dialog.show();android.view.Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);window.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);window.getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility());window.setTitle(title);
            PanelGeometry.Bounds bounds=PanelGeometry.place(root.getWidth(),root.getHeight(),insetLeft,insetTop,insetRight,Math.max(insetBottom,imeBottom),dp(420),dp(540),dp(10),imeBottom==0?dp(84):0);
            window.setGravity(Gravity.TOP|Gravity.LEFT);window.setLayout(bounds.width,bounds.height);android.view.WindowManager.LayoutParams attributes=window.getAttributes();attributes.x=bounds.left;attributes.y=flyoutTop(bounds.height);window.setAttributes(attributes);
        }
        lastGlassUpdate=0;body.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){body.getViewTreeObserver().removeOnPreDrawListener(this);if(areaMenu==dialog){PanelAnimations.play(body,crystal.menuAnimation(),store.reducedMotion||systemReduced);updateMenuGlass();}return true;}});
    }
    private void updateMenuGlass(){
        if((menuGlass==null||panel==null)&&areaMenuGlass==null)return;long now=System.nanoTime();if(now-lastGlassUpdate<33_000_000L)return;float dt=lastGlassUpdate==0?.05f:Math.min(.1f,(now-lastGlassUpdate)/1_000_000_000f);lastGlassUpdate=now;
        if(menuGlass!=null&&panel!=null)menuGlass.update(menuBackdrops(panel),store.preferences.getInt("glass_strength",90),store.preferences.getInt("glass_feather",80),store.preferences.getInt("glass_empty",0),store.reducedMotion||systemReduced,dt);
        if(areaMenuGlass!=null&&areaMenuBody!=null)areaMenuGlass.update(menuBackdrops(areaMenuBody),store.preferences.getInt("glass_strength",90),store.preferences.getInt("glass_feather",80),store.preferences.getInt("glass_empty",0),store.reducedMotion||systemReduced,dt);
    }
    private void updateClock(){
        Date now=new Date();String pattern=android.text.format.DateFormat.is24HourFormat(this)?"HH:mm":"h:mm";
        String time=new SimpleDateFormat(pattern,Locale.getDefault()).format(now);clock.setText(time);
        date.setText(new SimpleDateFormat("EEEE, MMMM d",Locale.getDefault()).format(now));
        clockButton.setText(time+"\n"+new SimpleDateFormat("MMM d",Locale.getDefault()).format(now));clockButton.setTextSize(11);
    }
    @Override protected void onStart(){super.onStart();if(discover!=null)discover.start();try{widgetHost.startListening();dockWidgetHost.startListening();widgetListening=true;}catch(RuntimeException error){android.util.Log.e("NthWidgets","Could not listen for widget updates",error);}}
    @Override protected void onStop(){if(discover!=null)discover.stop();if(widgetListening){widgetHost.stopListening();dockWidgetHost.stopListening();widgetListening=false;}super.onStop();}
    @Override protected void onResume(){super.onResume();resumed=true;shuffleHomeAfter=android.os.SystemClock.uptimeMillis()+5000;desktop.foreground(true);previousFrame=lastRender=0;scene.onResume();applySettings(false);Choreographer.getInstance().removeFrameCallback(frames);Choreographer.getInstance().postFrameCallback(frames);handler.removeCallbacks(clockTick);clockTick.run();if(loaded)reloadApps();if(discover!=null)discover.resume();if(cardsView!=null)cardsView.refresh();if(setupPending())queueSetupReturn();if(nativeFeed!=null)nativeFeed.resume();}
    @Override protected void onPause(){desktop.foreground(false);if(folderMotion!=null){if(folderMotion.closing())closePanel();else stopFolderMotion();}if(drawerDrags!=null)drawerDrags.cancel();if(desktopManager!=null)desktopManager.cancelDrag();resumed=false;if(layeredWallpaper!=null)layeredWallpaper.lighting(false);if(sceneComposer!=null)sceneComposer.reduced(true);Choreographer.getInstance().removeFrameCallback(frames);handler.removeCallbacks(clockTick);motion.pause();scene.onPause();desktop.savePositions();store.save();savePanel();if(settingsStudio!=null)settingsStudio.activityPaused();if(cardsView!=null)cardsView.pause();if(nativeFeed!=null)nativeFeed.pause();if(discover!=null)discover.pause();super.onPause();}
    @Override protected void onDestroy(){desktop.disposeCards();if(randomizerDialog!=null)randomizerDialog.dismiss();if(scenePackConfirmation!=null)scenePackConfirmation.dismiss();stopFolderMotion();if(sceneComposer!=null){sceneComposer.persistNow();sceneComposer.dispose();sceneComposer=null;}if(layeredWallpaper!=null)layeredWallpaper.dispose();if(areaMenu!=null){areaMenu.dismiss();areaMenu=null;}destroyed=true;if(restoreConfirmation!=null){restoreConfirmation.dismiss();restoreConfirmation=null;}handler.removeCallbacksAndMessages(null);catalog.launcher.unregisterCallback(packageCallback);unregisterReceiver(powerReceiver);loader.shutdownNow();sceneFiles.shutdown();if(desktopManager!=null)desktopManager.dispose();widgetHost.releaseViews();dockWidgetHost.releaseViews();if(shortcuts!=null)shortcuts.destroy();if(settingsStudio!=null)settingsStudio.dispose();if(cardsView!=null)cardsView.dispose();if(nativeFeed!=null)nativeFeed.dispose();if(discover!=null)discover.destroy();super.onDestroy();}
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(setupPending()){queueSetupReturn();return;}boolean alreadyHome=resumed&&getWindow().getDecorView().hasWindowFocus()&&overlay==null&&store.page==store.homePage();closePanel();motion.recenter();if(!shortcuts.handle(intent)&&Intent.ACTION_MAIN.equals(intent.getAction())&&intent.hasCategory(Intent.CATEGORY_HOME)){if(alreadyHome)assignedGesture("gesture_home");else{store.page=store.homePage();desktop.rebuild();pageChanged();store.save();}}}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus){restoreDesktopMotion();if(store!=null&&setupPending())queueSetupReturn();}}
    @Override protected void onPostResume(){super.onPostResume();if(setupPending())queueSetupReturn();}
    private void restoreDesktopMotion(){if(!resumed||destroyed||scene==null||motion==null)return;motion.pause();applySettings(false);scene.onResume();sceneDirty=true;scene.requestRender();Choreographer.getInstance().removeFrameCallback(frames);Choreographer.getInstance().postFrameCallback(frames);}

    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(drawerDrags!=null)drawerDrags.cancel();int setup=setupGuide==null?-1:setupGuide.step();String page=settingsStudio==null?null:settingsStudio.page();boolean cards=cardsView!=null,managing=desktopManager!=null,composing=sceneComposer!=null;Bundle feedState=nativeFeed==null?null:nativeFeed.state();closePanel();if(discover!=null)discover.configurationChanged();motion.recenter();clock.setTextSize(compact()?30:54);desktop.rebuild();root.requestApplyInsets();if(composing)handler.post(this::showSceneCreator);else if(setup>=0)handler.post(()->showSetup(setup));else if(managing)handler.post(this::showDesktopManager);else if(page!=null)handler.post(()->showSettings(page));else if(cards)handler.post(()->showCards(cardsQuery));else if(feedState!=null)handler.post(()->{showNativeFeed();nativeFeed.restore(feedState);});}
    @Override protected void onSaveInstanceState(Bundle state){if(sceneComposer!=null){sceneComposer.persistNow();state.putBoolean("scene_creator",true);}if(setupGuide!=null)state.putInt("setup_step",setupGuide.step());if(desktopManager!=null)state.putBoolean("desktop_manager",true);if(settingsStudio!=null)state.putString("settings_page",settingsStudio.page());if(nativeFeed!=null)state.putBundle("native_feed",nativeFeed.state());super.onSaveInstanceState(state);}
    @Override public void onBackPressed(){if(setupGuide!=null){setupGuide.back();return;}if(sceneComposer!=null&&sceneComposer.step()>0){sceneComposer.step(sceneComposer.step()-1);return;}if(desktopManager!=null&&desktopManager.cancelDrag())return;if(drawerDrags!=null&&drawerDrags.active()){drawerDrags.cancel();return;}if(discover!=null&&discover.close())return;if(settingsStudio!=null&&settingsStudio.back())return;if(nativeFeed!=null&&nativeFeed.back())return;if(overlay!=null)requestClosePanel();else if(store.page!=store.homePage()){store.page=store.homePage();desktop.rebuild();pageChanged();store.save();}}
    @Override public int displayRotation(){return getWindowManager().getDefaultDisplay().getRotation();}
    @Override public void onTilt(float x,float y){if(Math.abs(x-currentX)>.002f||Math.abs(y-currentY)>.002f)sceneDirty=true;currentX=x;currentY=y;if(scene!=null)scene.tilt(x,y);if(wallpaper!=null)wallpaper.tilt(x,y);if(layeredWallpaper!=null&&layeredWallpaper.getVisibility()==View.VISIBLE)layeredWallpaper.tilt(x,y);if(sceneComposer!=null)sceneComposer.tilt(x,y);if(desktop!=null)desktop.tilt(x,y);if(settingsStudio!=null)settingsStudio.tilt(x,y);if(desktopManager!=null)desktopManager.tilt(x,y);if(cardsView!=null)cardsView.tilt(x,y);if(nativeFeed!=null)nativeFeed.tilt(x,y);}
    private void applySettings(boolean rebuild){
        int rotation=store.preferences.getInt("home_rotation",0);int requested=new int[]{android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}[Math.max(0,Math.min(3,rotation))];if(appliedHomeRotation!=rotation){appliedHomeRotation=rotation;if(getRequestedOrientation()!=requested)setRequestedOrientation(requested);}
        powerSave=getSystemService(PowerManager.class).isPowerSaveMode();
        systemReduced=Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1)==0;
        boolean reduced=store.reducedMotion||systemReduced;if(reduced||powerSave)stopFolderMotion();
        if(discover!=null&&(!crystal.feedEnabled()||crystal.feedProvider()!=0))discover.disable();
        sceneRandomizer.sync(System.currentTimeMillis());applyWallpaper(reduced);scene.configure(store,powerSave,systemReduced);scene.configureCrystal(crystal);sceneLighting.configure(store,crystal,powerSave,systemReduced);desktop.configure(reduced,powerSave);modeLabel.setText((store.preferences.getInt("wallpaper_source",0)==3?"My layered scene":store.preferences.getInt("wallpaper_source",0)==2?"Android wallpaper":store.preferences.getInt("wallpaper_source",0)==1?"Photo wallpaper":LauncherStore.MODES[store.mode])+(reduced?" · reduced motion":""));
        if(resumed)motion.resume(store.tilt&&crystal.parallax()&&!reduced&&!powerSave);if(rebuild)desktop.rebuild();sceneDirty=true;scene.requestRender();pageChanged();applyDesktopChrome();
    }
    private void applyWallpaper(boolean reduced){
        int source=store.preferences.getInt("wallpaper_source",0);boolean system=source==2;
        if(system){getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);getWindow().setFormat(android.graphics.PixelFormat.TRANSLUCENT);getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));}
        else{getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);getWindow().setFormat(android.graphics.PixelFormat.OPAQUE);}
        root.setBackgroundColor(system?Color.argb(Math.round(store.preferences.getInt("wallpaper_dim",20)*2.55f),0,0,0):0xFF061326);
        wallpaper.apply(store,reduced||powerSave);
        boolean custom=source==3;java.io.File file=LayeredScene.file(this,false);long stamp=file.lastModified()+file.length();
        if(custom&&(layeredWallpaper.document()==null||stamp!=layeredStamp)){try{LayeredScene.Document doc=LayeredScene.read(this,false);layeredWallpaper.document(doc==null?new LayeredScene.Document():doc);layeredStamp=stamp;}catch(Exception invalid){layeredWallpaper.document(new LayeredScene.Document());layeredStamp=stamp;}}
        layeredWallpaper.setVisibility(custom?View.VISIBLE:View.GONE);layeredWallpaper.moving(!reduced&&!powerSave&&store.tilt&&crystal.parallax()&&store.preferences.getBoolean("wallpaper_parallax",true));
        layeredWallpaper.lighting(resumed&&!reduced&&!powerSave&&store.ambient);layeredWallpaper.materialChanged();
        layeredWallpaper.setAlpha(1);layeredWallpaper.setForeground(new android.graphics.drawable.ColorDrawable(Color.argb(Math.round(store.preferences.getInt("wallpaper_dim",20)*2.55f),0,0,0)));
        if(sceneComposer!=null)sceneComposer.motion(reduced||powerSave,store.tilt&&crystal.parallax(),store.ambient);
        scene.setVisibility(source==0||source==1&&!WallpaperStore.file(this).exists()?View.VISIBLE:View.GONE);
    }
    private void chooseWallpaperPhoto(){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),3076);}catch(RuntimeException unavailable){toast("Photo picker unavailable.");}}
    private void wallpaperPhotoResult(int result,Intent data){if(result!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();loader.execute(()->{try{WallpaperStore.importPhoto(this,uri);store.preferences.edit().putInt("wallpaper_source",1).apply();handler.post(()->{if(!destroyed){applySettings(false);showSettings("wallpaper");toast("Wallpaper saved");}});}catch(Exception error){handler.post(()->{if(!destroyed)toast("Could not use that image. Choose another photo.");});}});}
    private void chooseAndroidWallpaper(boolean live){
        if(!live){launchAndroidWallpaper(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER),"Android wallpapers"));return;}
        LinearLayout body=scrollBody(openPanel("Live wallpapers"));
        java.util.List<android.content.pm.ResolveInfo> services=getPackageManager().queryIntentServices(new Intent(android.service.wallpaper.WallpaperService.SERVICE_INTERFACE),android.content.pm.PackageManager.GET_META_DATA);
        int count=0;for(android.content.pm.ResolveInfo result:services){android.content.pm.ServiceInfo info=result.serviceInfo;if(info==null||!info.enabled||!info.exported||!"android.permission.BIND_WALLPAPER".equals(info.permission))continue;
            android.content.ComponentName component=new android.content.ComponentName(info.packageName,info.name);String label=String.valueOf(result.loadLabel(getPackageManager()));action(body,label,()->launchAndroidWallpaper(new Intent(android.app.WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(android.app.WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,component)));count++;
        }
        if(count==0)paragraph(body,"No live wallpaper services are visible. Try Android’s picker or create your own layered scene.");
        action(body,"Android live wallpaper picker",()->launchAndroidWallpaper(new Intent(android.app.WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)));
        action(body,"Use current Android wallpaper",()->{store.preferences.edit().putInt("wallpaper_source",2).apply();applySettings(false);closePanel();});
        action(body,"Create a scene",this::showSceneCreator);
    }
    private void launchAndroidWallpaper(Intent intent){
        try{startActivityForResult(intent,3082);store.preferences.edit().putInt("wallpaper_source",2).apply();applySettings(false);}
        catch(RuntimeException unavailable){toast("This wallpaper picker could not open. Try Choose a photo or Create a scene.");}
    }
    private void updateAndroidWallpaperOffsets(){if(store.preferences.getInt("wallpaper_source",0)!=2||root.getWindowToken()==null)return;try{android.app.WallpaperManager manager=android.app.WallpaperManager.getInstance(this);manager.setWallpaperOffsetSteps(store.pageCount()>1?1f/(store.pageCount()-1):1,1);manager.setWallpaperOffsets(root.getWindowToken(),store.pageCount()>1?store.page/(float)(store.pageCount()-1):.5f,.5f);}catch(RuntimeException ignored){}}
    private void showSceneCreator(){
        if(sceneComposer!=null)return;
        sceneFiles.execute(()->{try{LayeredScene.Document saved;try{saved=LayeredScene.read(this,true);}catch(Exception invalid){saved=null;}if(saved==null)try{saved=LayeredScene.read(this,false);}catch(Exception invalid){saved=null;}if(saved==null){saved=new LayeredScene.Document();saved.background=WallpaperStore.bytes(this);saved.fit=store.preferences.getInt("wallpaper_fit",0);}LayeredScene.Document draft=saved;
            handler.post(()->{if(destroyed||sceneComposer!=null)return;LinearLayout body=openPanel("Scene creator");sceneComposer=new SceneComposer(this,draft,new SceneComposer.Host(){
                public void pick(boolean object){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),object?3081:3080);}catch(RuntimeException unavailable){toast("Gallery picker unavailable.");}}
                public void saveDraft(LayeredScene.Document d){LayeredScene.Document snapshot=LayeredScene.copy(d);sceneFiles.execute(()->{try{LayeredScene.write(MainActivity.this,snapshot,true);}catch(Exception error){handler.post(()->{if(!destroyed)toast("Scene draft could not be saved. Try a smaller image.");});}});}
                public void apply(LayeredScene.Document d){LayeredScene.Document snapshot=LayeredScene.copy(d);SceneComposer editor=sceneComposer;sceneFiles.execute(()->{try{LayeredScene.write(MainActivity.this,snapshot,false);LayeredScene.write(MainActivity.this,null,true);store.preferences.edit().remove("scene_creator_step").apply();handler.post(()->{if(destroyed)return;store.preferences.edit().putInt("wallpaper_source",3).apply();layeredStamp=-1;applySettings(false);if(sceneComposer==editor){onPanelClose=null;closePanel();}toast("Your layered scene is applied");});}catch(Exception error){handler.post(()->{if(!destroyed){if(sceneComposer==editor)editor.failed();toast("Could not save this scene. Try fewer or smaller images.");}});}});}
                public void exportPack(LayeredScene.Document document){exportScenePack(document);}public void importPack(){importScenePack();}
                public void discard(){store.preferences.edit().remove("scene_creator_step").apply();onPanelClose=null;sceneFiles.execute(()->{try{LayeredScene.write(MainActivity.this,null,true);}catch(Exception ignored){}});closePanel();}
            });body.addView(sceneComposer,new LinearLayout.LayoutParams(-1,0,1));onPanelClose=()->{if(sceneComposer!=null){store.preferences.edit().putInt("scene_creator_step",sceneComposer.step()).apply();sceneComposer.persistNow();}};sceneComposer.step(store.preferences.getInt("scene_creator_step",0));sceneComposer.motion(store.reducedMotion||systemReduced||powerSave,store.tilt&&crystal.parallax(),store.ambient);
            if(sceneFocus!=null){sceneComposer.focus(sceneFocus);sceneFocus=null;}
            if(pendingSceneImage!=null){sceneComposer.image(pendingSceneImage,pendingSceneObject);pendingSceneImage=null;}
            });
        }catch(Exception error){handler.post(()->{if(!destroyed)toast("Could not open this scene. Choose a photo to continue.");});}});
    }
    private void importScenePack(){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),3091);}catch(RuntimeException missing){toast("Android file picker unavailable.");}}
    private void exportAppliedScene(){sceneFiles.execute(()->{try{LayeredScene.Document scene=LayeredScene.read(this,false);if(scene==null){handler.post(()->{if(!destroyed)toast("Create or apply a custom scene first.");});return;}handler.post(()->{if(!destroyed)exportScenePack(scene);});}catch(Exception bad){handler.post(()->{if(!destroyed)toast("The saved scene could not be opened.");});}});}
    private void exportScenePack(LayeredScene.Document document){LayeredScene.Document snapshot=LayeredScene.copy(document);sceneFiles.execute(()->{try{ScenePackFiles.prepare(this,ScenePack.encode(snapshot));handler.post(()->{if(destroyed)return;try{startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"My-Scene.nth3dscene"),3090);}catch(RuntimeException missing){toast("Android file picker unavailable.");}});}catch(Exception bad){handler.post(()->{if(!destroyed)toast("Could not prepare the scene pack. Your scene was kept.");});}});}
    private void scenePackResult(int request,int result,Intent data){
        if(result!=RESULT_OK||data==null||data.getData()==null){if(request==3090)sceneFiles.execute(()->ScenePackFiles.clear(this));return;}android.net.Uri uri=data.getData();
        if(request==3090){sceneFiles.execute(()->{try{byte[] bytes=ScenePackFiles.read(this);try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException();out.write(bytes);}byte[] saved;try(java.io.InputStream in=getContentResolver().openInputStream(uri)){saved=WallpaperStore.read(in,ScenePack.MAX_BYTES);}if(!java.util.Arrays.equals(bytes,saved))throw new java.io.IOException("Scene pack verification failed");ScenePackFiles.clear(this);handler.post(()->{if(!destroyed)toast("Scene pack saved and verified");});}catch(Exception bad){handler.post(()->{if(!destroyed)toast("Could not save and verify the scene pack. Please export again.");});}});return;}
        sceneFiles.execute(()->{try{byte[] bytes;try(java.io.InputStream in=getContentResolver().openInputStream(uri)){bytes=WallpaperStore.read(in,ScenePack.MAX_BYTES);}LayeredScene.Document scene=ScenePack.decode(bytes);handler.post(()->{if(destroyed)return;if(scenePackConfirmation!=null)scenePackConfirmation.dismiss();scenePackConfirmation=new AlertDialog.Builder(this).setTitle("Import scene draft?").setMessage(scene.objects.size()+" objects, copied images, motion and lighting. This replaces your current scene draft. Your active wallpaper and desktop layout stay as they are until you choose Apply scene.").setNegativeButton("Cancel",null).setPositiveButton("Import",(dialog,which)->importSceneDraft(scene)).create();scenePackConfirmation.setOnDismissListener(dialog->scenePackConfirmation=null);Ui.themeDialog(scenePackConfirmation);});}catch(Exception bad){handler.post(()->{if(!destroyed)toast("This scene pack is invalid or incomplete. Your scenes were kept.");});}});
    }
    private void importSceneDraft(LayeredScene.Document imported){if(sceneComposer!=null){sceneComposer.applied();sceneComposer.dispose();sceneComposer=null;}onPanelClose=null;closePanel();LayeredScene.Document snapshot=LayeredScene.copy(imported);sceneFiles.execute(()->{try{LayeredScene.write(this,snapshot,true);store.preferences.edit().remove("scene_creator_step").apply();handler.post(()->{if(!destroyed){showSceneCreator();toast("Scene imported as a draft. Preview it before applying.");}});}catch(Exception bad){handler.post(()->{if(!destroyed)toast("Could not save the imported draft. Your active scene was kept.");});}});}
    private void sceneImageResult(int request,int result,Intent data){if(result!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();sceneFiles.execute(()->{try{byte[] image=WallpaperStore.importImage(this,uri,request==3081);handler.post(()->{if(destroyed)return;if(sceneComposer!=null)sceneComposer.image(image,request==3081);else{pendingSceneImage=image;pendingSceneObject=request==3081;showSceneCreator();}});}catch(Exception error){handler.post(()->{if(!destroyed)toast("Could not read this image. Your scene was kept.");});}});}

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
                if(!newlyInstalled.isEmpty()){for(AppCatalog.Entry entry:result)if(entry.component!=null&&newlyInstalled.contains(entry.component.getPackageName()))store.add(store.page,entry.key,entry.label);newlyInstalled.clear();}
                desktop.rebuild();pageChanged();if(settingsStudio!=null)settingsStudio.refreshPreview();if(panelKind==1&&appsAdapter!=null)appsAdapter.filter(appSearch==null?"":appSearch.getText().toString());
            });
        });
    }
    @Override public void pageChanged(){
        if(dockContents!=null)dockContents.rebuild();updateAndroidWallpaperOffsets();int count=store.pages.get(store.page).size();pageLabel.setText("Desktop "+(store.page+1)+" of "+store.pageCount()+"  ·  "+count+" shortcut"+(count==1?"":"s"));
    }
    @Override public void ripple(float x,float y){
        if(store.preferences.getInt("wallpaper_source",0)==2){int[] position=new int[2];desktop.getLocationInWindow(position);try{android.app.WallpaperManager.getInstance(this).sendWallpaperCommand(root.getWindowToken(),android.app.WallpaperManager.COMMAND_TAP,Math.round(x+position[0]),Math.round(y+position[1]),0,null);}catch(RuntimeException ignored){}return;}
        if(scene.getVisibility()!=View.VISIBLE)return;
        if(store.mode==3&&store.ripples&&!store.reducedMotion&&!systemReduced)rippleUntil=android.os.SystemClock.uptimeMillis()+3600;
        int[] loc=new int[2];desktop.getLocationInWindow(loc);scene.ripple((x+loc[0])/Math.max(1,root.getWidth()),(y+loc[1])/Math.max(1,root.getHeight()));
    }
    @Override public void open(LauncherStore.Item item){if(item.card()){showCards(item.key);return;}if(item.folder())showFolder(item);else if(PinnedShortcuts.isShortcut(item)){closePanel();shortcuts.open(item);}else launch(item.key);}
    private void launch(String key){
        if(key.startsWith("builtin:")){
            switch(key.substring(8)){case "cards":showCards("");break;case "notes":showNotes();break;case "nota":showSettings("nota");break;case "calculator":showCalculator();break;case "calendar":showCalendar();break;case "settings":showSettings();break;case "modes":showModes();break;default:showAbout();}return;
        }
        AppCatalog.Entry e=catalog.find(key);if(e==null){toast("This app is unavailable. Hold its shortcut to remove it.");return;}
        try{closePanel();catalog.launcher.startMainActivity(e.component,e.user,null,null);}catch(SecurityException|IllegalStateException|android.content.ActivityNotFoundException ex){toast("Android could not open this app. Check its profile or App info.");}
    }
    private void savePanel(){if(onPanelClose!=null)onPanelClose.run();}
    private boolean folderMotionAllowed(){return !store.reducedMotion&&!systemReduced&&!powerSave&&android.animation.ValueAnimator.areAnimatorsEnabled();}
    private void stopFolderMotion(){if(folderMotion!=null){FolderMotion previous=folderMotion;folderMotion=null;physicalFolderId=null;previous.dispose();}}
    private void requestClosePanel(){
        if(folderMotion!=null&&folderMotionAllowed()){FolderMotion current=folderMotion;current.close(findFolderTile(root,physicalFolderId),()->{if(folderMotion==current)closePanel();});return;}closePanel();
    }
    private IconTile findFolderTile(View view,String id){
        if(id==null||!view.isShown())return null;if(view instanceof IconTile&&("shortcut:"+id).equals(view.getTag()))return (IconTile)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=group.getChildCount()-1;i>=0;i--){IconTile result=findFolderTile(group.getChildAt(i),id);if(result!=null)return result;}}return null;
    }
    private void closePanel(){
        shuffleHomeAfter=android.os.SystemClock.uptimeMillis()+5000;if(randomizerDialog!=null){randomizerDialog.dismiss();randomizerDialog=null;}
        stopFolderMotion();
        savePanel();if(sceneComposer!=null){sceneComposer.dispose();sceneComposer=null;}if(areaMenu!=null){areaMenu.dismiss();areaMenu=null;}setupGuide=null;areaButton=null;setPanelArea(null);if(desktopManager!=null){desktopManager.dispose();desktopManager=null;}if(nativeFeed!=null){nativeFeed.dispose();nativeFeed=null;}if(discover!=null)discover.close();if(cardsView!=null){cardsView.dispose();cardsView=null;}if(settingsStudio!=null){settingsStudio.dispose();settingsStudio=null;}menuGlass=null;lastGlassUpdate=0;onPanelClose=null;panelKind=0;appsAdapter=null;appSearch=null;drawerSearchResults=null;drawerSearchScroll=null;drawerGrid=null;drawerResultQuery=null;drawerLocalTargets.clear();drawerLocalCount=0;
        if(overlay!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(overlay.getWindowToken(),0);root.removeView(overlay);overlay=null;panel=null;}
        desktop.setModal(false);desktop.setAlpha(1);startButton.setSelected(false);restoreDesktopMotion();scene.requestRender();
    }
    private LinearLayout openPanel(String title){return openPanel(title,false);}
    private LinearLayout openPanel(String title,boolean flyout){return openPanel(title,flyout,crystal.menuAnimation());}
    private LinearLayout openPanel(String title,boolean flyout,int animation){
        closePanel();desktop.setModal(true);startButton.setSelected(true);panelKind=2;
        flyoutPanel=flyout;
        overlay=new FrameLayout(this);overlay.setBackgroundColor(flyout?0x00000000:0xAA030A17);overlay.setOnClickListener(v->{if(setupGuide==null)requestClosePanel();});root.addView(overlay,new FrameLayout.LayoutParams(-1,-1));
        panel=Ui.column(this);panel.setBackground(flyout?new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT):Ui.satin(this,0xE3335C76,0xF20B1A30,26));panel.setClipChildren(false);panel.setClipToPadding(false);panel.setElevation(flyout?0:dp(34));panel.setPadding(dp(flyout?12:20),dp(12),dp(flyout?12:20),dp(16));panel.setOnClickListener(v->{});panel.setCameraDistance(dp(2400));
        overlay.addView(panel);layoutPanel();applyCrystalAppearance();
        LinearLayout heading=Ui.row(this);TextView label=Ui.text(this,title,20,Ui.TEXT);label.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));heading.setMinimumHeight(dp(46));
        areaButton=Ui.quiet(this,"⋮",this::showAreaSettings);areaButton.setTextSize(25);heading.addView(areaButton,new LinearLayout.LayoutParams(dp(46),dp(46)));setPanelArea(AreaSettingsMenu.area(title));
        Button close=Ui.icon(this,"close","Close "+title,this::requestClosePanel);heading.addView(close,new LinearLayout.LayoutParams(dp(46),dp(46)));if(title.equals("Setup & permissions"))close.setVisibility(View.GONE);
        if(" ".equals(title)){heading.setVisibility(View.GONE);}else panel.addView(heading);
        LinearLayout opening=panel;opening.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){opening.getViewTreeObserver().removeOnPreDrawListener(this);if(panel==opening&&animation!=PanelAnimations.PHYSICAL)PanelAnimations.play(opening,animation,store.reducedMotion||systemReduced);return true;}});
        return panel;
    }
    private void layoutPanel(){
        if(panel==null)return;
        int bottom=Math.max(insetBottom,imeBottom);
        PanelGeometry.Bounds bounds=PanelGeometry.place(root.getWidth(),root.getHeight(),insetLeft,insetTop,insetRight,bottom,dp(flyoutPanel?420:720),dp(flyoutPanel?540:820),dp(flyoutPanel?10:12),flyoutPanel&&imeBottom==0?dp(84):0);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(bounds.width,bounds.height);
        // Center inside the safe rectangle. FrameLayout's CENTER gravity applies
        // unequal margins as offsets, which can put a keyboard-resized panel offscreen.
        p.gravity=Gravity.TOP|Gravity.LEFT;
        p.leftMargin=bounds.left;p.topMargin=flyoutPanel?flyoutTop(bounds.height):bounds.top;
        panel.setLayoutParams(p);
    }
    private int flyoutTop(int panelHeight){
        int bottom=Math.max(insetBottom,imeBottom);
        int safeTop=insetTop+dp(16);
        int safeBottom=Math.max(safeTop,root.getHeight()-bottom-dp(16));
        // Place short menus below the shortcut grid, not against the navigation bar.
        int available=Math.max(0,safeBottom-safeTop-panelHeight);
        int desired=safeTop+Math.round(available*.52f);
        return Math.max(safeTop,Math.min(desired,safeBottom-panelHeight));
    }
    private LinearLayout scrollBody(LinearLayout parent){
        ScrollView scroll=new ScrollView(this){
            float downX,downY;Button active;boolean cardGesture;
            private Button at(float y){
                if(getChildCount()==0)return null;
                ViewGroup body=(ViewGroup)getChildAt(0);
                float target=y+getScrollY();
                for(int i=0;i<body.getChildCount();i++){
                    View child=body.getChildAt(i);
                    if(child instanceof Button&&target>=child.getTop()&&target<child.getBottom())return (Button)child;
                }
                return null;
            }
            private void focus(Button next){
                if(active==next)return;
                if(active!=null)raise(active,false);
                active=next;
                if(active!=null)raise(active,true);
            }
            @Override public boolean dispatchTouchEvent(MotionEvent e){
                int type=e.getActionMasked();
                if(type==MotionEvent.ACTION_DOWN){
                    downX=e.getX();downY=e.getY();
                    cardGesture=flyoutPanel&&at(e.getY())!=null;
                    focus(at(e.getY()));
                }
                else if(type==MotionEvent.ACTION_MOVE){
                    focus(at(e.getY()));
                    if(active!=null&&!store.reducedMotion&&!systemReduced){
                        float horizontal=Math.max(-1f,Math.min(1f,(e.getX()-getWidth()*.5f)/Math.max(1,getWidth()*.5f)));
                        active.setCameraDistance(dp(1600));
                        active.animate().rotationY(horizontal*9f).translationX(horizontal*dp(5))
                            .setDuration(90).start();
                    }
                }
                else if(type==MotionEvent.ACTION_UP||type==MotionEvent.ACTION_CANCEL){
                    Button target=active;
                    boolean slide=type==MotionEvent.ACTION_UP&&cardGesture&&target!=null
                        &&(Math.abs(e.getX()-downX)>dp(18)||Math.abs(e.getY()-downY)>dp(18));
                    focus(null);
                    if(slide){target.performClick();return true;}
                }
                return super.dispatchTouchEvent(e);
            }
            @Override public boolean onInterceptTouchEvent(MotionEvent event){
                if(flyoutPanel&&cardGesture)return false;
                return super.onInterceptTouchEvent(event);
            }
        };
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);scroll.setClipChildren(false);
        scroll.setVerticalScrollBarEnabled(false);
        parent.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout body=Ui.column(this);
        body.setClipChildren(false);body.setClipToPadding(false);
        body.setPadding(dp(10),dp(12),dp(10),dp(14));
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2));
        if(flyoutPanel){
            body.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener(){
                int previous=-1;
                @Override public void onGlobalLayout(){
                    if(panel==null||body.getParent()!=scroll)return;
                    int content=body.getHeight();
                    if(content<=0||content==previous)return;
                    previous=content;
                    int header=panel.getChildCount()>0&&panel.getChildAt(0)!=scroll
                        ?panel.getChildAt(0).getHeight():0;
                    int wanted=content+header+panel.getPaddingTop()+panel.getPaddingBottom()+dp(6);
                    int maximum=Math.max(dp(180),root.getHeight()-insetTop-Math.max(insetBottom,imeBottom)-dp(36));
                    FrameLayout.LayoutParams params=(FrameLayout.LayoutParams)panel.getLayoutParams();
                    int adjusted=Math.max(dp(120),Math.min(maximum,wanted));
                    if(params.height!=adjusted||params.topMargin!=flyoutTop(adjusted)){
                        params.height=adjusted;
                        params.topMargin=flyoutTop(adjusted);
                        panel.setLayoutParams(params);
                    }
                }
            });
        }
        scroll.setOnScrollChangeListener((View.OnScrollChangeListener)(v,x,y,oldX,oldY)->{
            if(store.reducedMotion||systemReduced)return;
            float focus=y+scroll.getHeight()*.5f;
            for(int i=0;i<body.getChildCount();i++){
                View child=body.getChildAt(i);
                if(!(child instanceof Button)||child.isPressed())continue;
                float center=child.getTop()+child.getHeight()*.5f;
                float proximity=Math.max(0,1-Math.abs(center-focus)/Math.max(1,scroll.getHeight()*.65f));
                if(crystal.menuAnimation()==4)proximity=0;
                child.animate().cancel();
                child.animate().translationZ(dp(9)*proximity).scaleX(1+.025f*proximity)
                    .scaleY(1+.025f*proximity).rotationX(-3f*proximity)
                    .setDuration(150).start();
            }
        });
        return body;
    }
    private void raise(View v,boolean raised){
        v.animate().cancel();
        if(store.reducedMotion||systemReduced){
            v.setTranslationZ(raised?dp(8):0);return;
        }
        v.animate().translationZ(raised?dp(16):0).scaleX(raised?1.055f:1f)
            .scaleY(raised?1.055f:1f).rotationX(raised?-5f:0)
            .rotationY(0).translationX(0)
            .setInterpolator(raised?new android.view.animation.DecelerateInterpolator():
                new android.view.animation.OvershootInterpolator(.65f))
            .setDuration(raised?120:220).start();
    }
    private void paragraph(LinearLayout body,String text){TextView p=Ui.text(this,text,14,Ui.MUTED);p.setPadding(0,dp(8),0,dp(14));p.setLineSpacing(dp(3),1);body.addView(p,new LinearLayout.LayoutParams(-1,-2));}
    private void action(LinearLayout body,String text,Runnable action){
        Button b=Ui.quiet(this,text,action);
        b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setTextSize(13);b.setPadding(dp(12),0,dp(12),0);
        b.setElevation(dp(crystal.lighting()==0?3:7));
        b.setCameraDistance(dp(1600));
        b.setOnTouchListener((v,event)->{
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
                v.animate().cancel();v.animate().translationZ(dp(crystal.lighting()==0?6:14)).scaleX(1.035f).scaleY(1.035f).setDuration(120).start();
            }else if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){
                v.animate().translationZ(0).scaleX(1).scaleY(1).rotationX(0)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(.7f)).setDuration(260).start();
            }
            return false;
        });
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(44));
        p.topMargin=dp(5);p.bottomMargin=dp(9);
        body.addView(b,p);
    }
    private void section(LinearLayout body,String name){TextView h=Ui.text(this,name,16,Ui.TEXT);h.setTypeface(null,Typeface.BOLD);h.setPadding(0,dp(16),0,dp(8));body.addView(h);}
    void toast(String text){Toast.makeText(this,text,Toast.LENGTH_SHORT).show();}

    private void showAutoSort(){
        FolderSorter plan=new FolderSorter(new ArrayList<>(catalog.entries));
        LinearLayout body=scrollBody(openPanel("Auto pick and sort"));
        FittedGrid fit=desktop.sortedLayout(plan.folders.size());int pages=(plan.folders.size()+fit.capacity-1)/fit.capacity;paragraph(body,"Sort "+plan.appCount+" shortcuts into twenty category folders. The grid fits the desktop above your dock, using "+pages+(pages==1?" new desktop.":" new desktops.")+" Some category guesses may need manual changes. Your existing screens stay in place.");
        View preview=plan.preview(this,catalog,desktop::sortedLayout,desktop.getWidth(),desktop.getHeight());preview.setContentDescription("Category folder arrangement preview");body.addView(preview,new LinearLayout.LayoutParams(-1,dp(420)));
        action(body,"Create sorted desktop",()->{if(plan.apply(store,this,desktop::sortedLayout)<0){toast("All 24 desktops are in use.");return;}closePanel();desktop.rebuild();pageChanged();toast("Sorted desktop added. Undo is in Auto pick and sort.");});
        if(FolderSorter.canUndo(store))action(body,"Undo last auto-sort",()->{FolderSorter.undo(store);shortcuts.syncRemoved();closePanel();desktop.rebuild();pageChanged();toast("Generated folders removed.");});
        for(LauncherStore.Item folder:plan.folders)paragraph(body,folder.label+" · "+folder.children.size()+" shortcuts");
    }
    private void showStart(){
        LinearLayout p=openPanel("Your apps");panelKind=1;
        appSearch=new EditText(this);appSearch.setSingleLine(true);appSearch.setHint("Search apps, folders, settings or notes");appSearch.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(512)});appSearch.setTextColor(Ui.TEXT);appSearch.setHintTextColor(Ui.MUTED);appSearch.setTextSize(16);appSearch.setSelectAllOnFocus(true);appSearch.setContentDescription("Search installed apps");
        appSearch.setBackground(Ui.surface(0xFF0B172B,14,this));appSearch.setPadding(dp(14),0,dp(14),0);LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(50));ep.setMargins(0,dp(10),0,dp(10));p.addView(appSearch,ep);
        SwipeDismissGrid grid=new SwipeDismissGrid(this,this::closePanel,store.preferences.getInt("drawer_scroll",0)==1);
        LinearLayout shortcuts=Ui.row(this);
        Button all=Ui.button(this,"All apps",()->{if(appsAdapter!=null){appsAdapter.onlyBuiltins=false;appsAdapter.onlyFolders=false;grid.firstPage();appsAdapter.filter(appSearch.getText().toString());}});
        Button nth=Ui.button(this,"Nth tools",()->{if(appsAdapter!=null){appsAdapter.onlyBuiltins=true;appsAdapter.onlyFolders=false;grid.firstPage();appsAdapter.filter(appSearch.getText().toString());}});
        Button folders=Ui.button(this,"Folders",()->{if(appsAdapter!=null){appsAdapter.onlyBuiltins=false;appsAdapter.onlyFolders=true;grid.firstPage();appsAdapter.filter(appSearch.getText().toString());}});shortcuts.addView(folders,new LinearLayout.LayoutParams(0,dp(44),1));
        shortcuts.addView(all,new LinearLayout.LayoutParams(0,dp(44),1));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,dp(44),1);np.leftMargin=dp(8);shortcuts.addView(nth,np);if(store.preferences.getBoolean("cards_in_search",true)){Button cards=Ui.button(this,"Cards",()->showCards(appSearch==null?"":appSearch.getText().toString()));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(44),1);cp.leftMargin=dp(8);shortcuts.addView(cards,cp);}p.addView(shortcuts);for(int n=0;n<shortcuts.getChildCount();n++)if(shortcuts.getChildAt(n) instanceof Button){Button filter=(Button)shortcuts.getChildAt(n);filter.setBackground(Ui.touch(this,Ui.crystal(this,14,false),14));filter.setTextSize(12);filter.setSingleLine(true);filter.setPadding(dp(6),0,dp(6),0);}
        drawerGrid=grid;
        grid.setOnTouchListener(new View.OnTouchListener(){
            float startY,startX;boolean dismissing;
            @Override public boolean onTouch(View v,MotionEvent e){
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN){
                    startY=e.getY();startX=e.getX();dismissing=grid.getFirstVisiblePosition()==0&&grid.pointToPosition((int)startX,(int)startY)==android.widget.AdapterView.INVALID_POSITION;return false;
                }
                if(e.getActionMasked()==MotionEvent.ACTION_UP&&dismissing
                    &&e.getY()-startY>dp(90)&&Math.abs(e.getX()-startX)<dp(110)){
                    closePanel();return true;
                }
                return false;
            }
        });
        grid.setNumColumns(Math.min(8,store.preferences.getInt("drawer_columns",3)+(compact()?2:0)));grid.setVerticalSpacing(dp(10));grid.setHorizontalSpacing(dp(8));grid.setPadding(0,dp(16),0,dp(10));grid.setClipToPadding(false);grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setSelector(android.R.color.transparent);
        appsAdapter=new AppsAdapter();grid.setAdapter(appsAdapter);p.addView(grid,new LinearLayout.LayoutParams(-1,0,2));
        drawerSearchScroll=new ScrollView(this);drawerSearchScroll.setVisibility(View.GONE);drawerSearchResults=Ui.column(this);drawerSearchResults.setTag("unified-search-results");drawerSearchScroll.addView(drawerSearchResults);p.addView(drawerSearchScroll,new LinearLayout.LayoutParams(-1,0,1));appsAdapter.filter("");
        grid.setOnItemClickListener((a,v,pos,id)->openDrawerItem(grid.sourcePosition(pos),false));
        grid.setLongClickable(false);
        appSearch.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){if(appsAdapter!=null){grid.firstPage();appsAdapter.filter(s.toString());}}public void afterTextChanged(Editable e){}});
        appSearch.setOnEditorActionListener((v,action,event)->{
            if(appsAdapter==null)return false;
            if(appsAdapter.getCount()+drawerLocalCount==1){if(appsAdapter.getCount()==1)openDrawerItem(0,false);else if(!drawerLocalTargets.isEmpty())drawerLocalTargets.get(0).run();}
            return true;
        });
        p.setFocusableInTouchMode(true);p.requestFocus();if(store.preferences.getBoolean("search_keyboard",false)){EditText field=appSearch;handler.postDelayed(()->{if(appSearch==field&&overlay!=null){field.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(field,InputMethodManager.SHOW_IMPLICIT);}},280);}
    }
    private void renderDrawerSearch(String query){
        if(drawerSearchResults==null||drawerSearchScroll==null||drawerGrid==null||appsAdapter==null)return;
        String q=query.trim();boolean show=!q.isEmpty()&&!appsAdapter.onlyBuiltins&&!appsAdapter.onlyFolders;
        drawerSearchScroll.setVisibility(show?View.VISIBLE:View.GONE);
        drawerGrid.setVisibility(!show||appsAdapter.getCount()>0?View.VISIBLE:View.GONE);
        if(!show){drawerResultQuery=null;drawerLocalTargets.clear();drawerLocalCount=0;return;}
        if(q.equals(drawerResultQuery))return;drawerResultQuery=q;drawerLocalTargets.clear();drawerLocalCount=0;drawerSearchResults.removeAllViews();
        List<SettingsModel.Spec> settings=new ArrayList<>();
        for(SettingsModel.Spec spec:settingsModel().specs.values())if(LocalSearch.matches(q,spec.title,spec.help+" "+spec.page))settings.add(spec);
        settings.sort((a,b)->{int score=Integer.compare(LocalSearch.rank(q,a.title,a.help+" "+a.page),LocalSearch.rank(q,b.title,b.help+" "+b.page));return score!=0?score:a.title.compareToIgnoreCase(b.title);});
        if(!settings.isEmpty()){
            drawerSearchResults.addView(Ui.text(this,"Settings",14,Ui.ACCENT));drawerLocalCount+=settings.size();
            for(SettingsModel.Spec spec:settings.subList(0,Math.min(20,settings.size())))searchTarget("search-setting:"+spec.id,spec.title,spec.value(),()->{showSettings(spec.page);settingsStudio.focusSetting(spec.id);});
            if(settings.size()>20)searchRow("search-more-settings","All matching settings",settings.size()+" matches",()->{showSettings("root");settingsStudio.findSettings(q);});
        }
        for(String[] page:SettingsStudio.PAGES)if(LocalSearch.matches(q,page[1],page[2])){
            drawerLocalCount++;searchTarget("search-page:"+page[0],page[1],"Settings category",()->showSettings(page[0]));
        }
        String note=store.preferences.getString("note","");
        if(!note.isEmpty()&&LocalSearch.matches(q,"Nth Notes",note)){
            drawerLocalCount++;searchTarget("search-note","Nth Notes",LocalSearch.excerpt(note,q),()->showNoteMatch(q));
        }
        searchRow("search-nota","Ask Nth Nota AI",q,()->{showSettings("nota");settingsStudio.draftQuestion(q);});
    }
    private void searchTarget(String tag,String title,String detail,Runnable open){drawerLocalTargets.add(open);searchRow(tag,title,detail,open);}
    private void searchRow(String tag,String title,String detail,Runnable open){
        Button button=Ui.quiet(this,title+(detail.isEmpty()?"":"\n"+detail),open);button.setTag(tag);button.setContentDescription(title);button.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);drawerSearchResults.addView(button,new LinearLayout.LayoutParams(-1,-2));
    }
    private void showNoteMatch(String query){
        showNotes();for(int n=0;n<panel.getChildCount();n++)if(panel.getChildAt(n) instanceof EditText){
            EditText editor=(EditText)panel.getChildAt(n);String text=editor.getText().toString(),q=query.trim().toLowerCase(Locale.ROOT);int at=text.toLowerCase(Locale.ROOT).indexOf(q);
            if(at<0&&!q.isEmpty())at=text.toLowerCase(Locale.ROOT).indexOf(q.split("\\s+")[0]);
            int start=Math.max(0,Math.min(text.length(),at)),end=Math.min(text.length(),start+query.trim().length());editor.setSelection(start,end);editor.post(()->editor.bringPointIntoView(start));break;
        }
    }
    private void openDrawerItem(int position,boolean held){if(position<appsAdapter.items.size()){AppCatalog.Entry entry=appsAdapter.items.get(position);if(held)appMenu(entry,null);else launch(entry.key);}else{LauncherStore.Item folder=appsAdapter.folders.get(position-appsAdapter.items.size());if(held)menu(folder);else showFolder(folder);}}
    private final class AppsAdapter extends BaseAdapter {
        final List<AppCatalog.Entry> items=new ArrayList<>();final List<LauncherStore.Item> folders=new ArrayList<>();boolean onlyBuiltins,onlyFolders;
        void filter(String q){items.clear();if(!onlyFolders)items.addAll(catalog.filter(q,onlyBuiltins));folders.clear();if(!onlyBuiltins&&(onlyFolders||(store.preferences.getBoolean("search_folders",true)&&!q.trim().isEmpty())))for(LauncherStore.Item folder:store.folders())if(LocalSearch.matches(q,folder.label,""))folders.add(folder);notifyDataSetChanged();renderDrawerSearch(q);}
        @Override public int getCount(){return items.size()+folders.size();}@Override public Object getItem(int n){return n<items.size()?items.get(n):folders.get(n-items.size());}@Override public long getItemId(int n){return n;}
        @Override public View getView(int n,View old,ViewGroup parent){AppCatalog.Entry e=n<items.size()?items.get(n):null;LauncherStore.Item folder=e==null?folders.get(n-items.size()):null;IconTile tile=new IconTile(MainActivity.this,e==null?folder.label:e.label,e==null?catalog.icon(folder):e.icon,e==null?"folder":e.key);if(folder!=null)tile.setTag("shortcut:"+folder.id);tile.appearance(store.preferences,"drawer");tile.fitCell(Math.max(dp(30),(root.getWidth()-dp(64))/Math.min(8,store.preferences.getInt("drawer_columns",3)+(compact()?2:0))),dp(132));tile.setFocusable(false);drawerDrags.install(tile,e==null?folder:new LauncherStore.Item(e.key,e.label),()->{if(folder!=null)showFolder(folder);else launch(e.key);},()->{if(folder!=null)menu(folder);else appMenu(e,null);});tile.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1,-2));return tile;}
    }

    @Override public void menu(LauncherStore.Item item){
        if(item.card()){desktop.showCardMenu(item);return;}
        if(!item.folder()){appMenu(catalog.find(item.key),item);return;}
        LinearLayout body=scrollBody(openPanel(item.label,true));setPanelArea("folders");action(body,"Open folder",()->showFolder(item));action(body,"Rename folder",()->rename(item));
        action(body,"Move to another desktop",()->move(item));action(body,"Remove folder",()->confirmRemove(item));
    }
    private void appMenu(AppCatalog.Entry entry,LauncherStore.Item shortcut){
        String label=shortcut!=null?shortcut.label:entry.label;String key=shortcut!=null?shortcut.key:entry.key;
        LinearLayout body=scrollBody(openPanel(label,true));setPanelArea("drawer");action(body,"Open",()->{if(shortcut==null)launch(key);else open(shortcut);});
        if(shortcut==null)action(body,"Add to this desktop",()->{boolean added=store.add(store.page,key,label);desktop.rebuild();pageChanged();toast(added?"Shortcut added to Desktop "+(store.page+1):"That shortcut is already here, or this desktop is full.");closePanel();if(added)desktop.revealItem(store.pages.get(store.page).get(store.pages.get(store.page).size()-1).id);});
        else{
            action(body,"Rename shortcut",()->rename(shortcut));
            if(store.pages.get(store.page).contains(shortcut)){action(body,shortcut.fixed?"Unpin in Gravity Playground":"Pin in Gravity Playground",()->{shortcut.fixed=!shortcut.fixed;store.save();desktop.rebuild();closePanel();});action(body,"Move to another desktop",()->move(shortcut));}
            action(body,"Remove shortcut",()->{store.remove(shortcut);shortcuts.syncRemoved();desktop.rebuild();pageChanged();closePanel();});
        }
        if(shortcut==null||store.pages.get(store.page).contains(shortcut))action(body,"Resize",()->{if(store.preferences.getBoolean("lock_layout",false)){toast("Unlock desktop layout to resize apps.");return;}LauncherStore.Item item=shortcut==null?new LauncherStore.Item(key,label):shortcut;if(shortcut==null){item.resizable=true;item.appWidth=item.appHeight=2;}closePanel();if(shortcut==null&&!desktop.placeSized(item,0,0)){toast("No desktop space is available.");return;}desktop.resizeApp(item);});
        if(shortcut!=null&&shortcut.appTile())action(body,"Use normal shortcut",()->{closePanel();shortcut.normalShortcut();store.save();desktop.rebuild();pageChanged();});
        if(!store.folders().isEmpty()){
            action(body,"Add to folder",()->chooseFolder(shortcut==null?new LauncherStore.Item(key,label):shortcut,false));
            if(shortcut!=null&&store.folderContaining(shortcut)!=null)action(body,"Move to folder",()->chooseFolder(shortcut,true));
        }
        if(shortcut!=null&&PinnedShortcuts.isShortcut(shortcut))action(body,"App info",()->{try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package",shortcut.shortcutPackage,null)));}catch(RuntimeException unavailable){toast("App info unavailable.");}});
        if(entry!=null&&entry.component!=null){
            action(body,"App info",()->{try{catalog.launcher.startAppDetailsActivity(entry.component,entry.user,null,null);}catch(RuntimeException e){toast("App info is unavailable for this profile.");}});
            action(body,"Uninstall",()->{try{Intent i=new Intent(Intent.ACTION_DELETE,Uri.fromParts("package",entry.component.getPackageName(),null));i.putExtra(Intent.EXTRA_USER,entry.user);startActivity(i);}catch(RuntimeException e){toast("Open App info to manage this app.");}});
        }
    }
    private void rename(LauncherStore.Item item){
        EditText edit=new EditText(this);edit.setText(item.label);edit.setSingleLine(true);edit.setSelectAllOnFocus(true);edit.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(60)});Ui.pad(edit,16);
        new AlertDialog.Builder(this).setTitle("Rename").setView(edit).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{String s=edit.getText().toString().trim();if(!s.isEmpty()){item.label=s;store.save();desktop.rebuild();pageChanged();closePanel();}}).show();
    }
    private void move(LauncherStore.Item item){
        String[] destinations=new String[store.pageCount()];for(int n=0;n<destinations.length;n++)destinations[n]="Desktop "+(n+1);
        new AlertDialog.Builder(this).setTitle(item.card()?"Move desktop card":item.appTile()?"Move app tile":"Move shortcut").setItems(destinations,(d,w)->{if(item.sized()){int previous=store.page;closePanel();desktop.dragPage(w);if(store.pages.get(w).contains(item)){desktop.revealItem(item.card()?"card:"+item.id:item.id);return;}if(!desktop.placeSized(item,0,dp(24))){desktop.dragPage(previous);toast("No suitable desktop space is available.");}return;}if(store.pages.get(w).size()>=60){toast("That desktop is full.");return;}if(item.card())for(LauncherStore.Item existing:store.pages.get(w))if(existing!=item&&existing.key.equals(item.key)){toast("That desktop already has this card.");return;}store.remove(item);store.pages.get(w).add(item);item.x=item.y=-1;item.cellX=item.cellY=-1;store.save();desktop.rebuild();pageChanged();closePanel();}).show();
    }
    private void confirmRemove(LauncherStore.Item item){new AlertDialog.Builder(this).setTitle("Remove "+item.label+"?").setMessage("This removes the folder and its shortcuts. Installed apps remain on your device.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{store.remove(item);shortcuts.syncRemoved();desktop.rebuild();pageChanged();closePanel();}).show();}
    private void chooseFolder(LauncherStore.Item source,boolean moving){
        LauncherStore.Item origin=moving?store.folderContaining(source):null;List<LauncherStore.Item> destinations=new ArrayList<>(store.folders());destinations.remove(origin);
        if(destinations.isEmpty()){toast("Create another folder first.");return;}
        LinearLayout body=scrollBody(openPanel(moving?"Move to folder":"Add to folder"));
        for(LauncherStore.Item folder:destinations)action(body,folder.label,()->{
            if(moving){if(!store.moveToFolder(source,folder)){toast("The app could not be moved. Check that the destination has space.");return;}shortcuts.syncRemoved();}
            else{for(LauncherStore.Item c:folder.children)if(c.key.equals(source.key)){toast("Already in this folder.");return;}if(folder.children.size()>=1000){toast("This folder is full.");return;}LauncherStore.Item copy=source.copy();copy.normalShortcut();folder.children.add(copy);store.save();}
            desktop.rebuild();pageChanged();toast((moving?"Moved to ":"Added to ")+folder.label);showFolder(folder);
        });
    }
    private void showFolder(LauncherStore.Item folder){
        int animation=store.preferences.getInt("folder_animation",VisualDefaults.FOLDER_ANIMATION);FolderMotion.Origin origin=animation==PanelAnimations.PHYSICAL&&folderMotionAllowed()?FolderMotion.capture(root,findFolderTile(root,folder.id),catalog.icon(folder)):null;
        LinearLayout folderPanel=openPanel(store.preferences.getBoolean("folder_title",true)?folder.label:"Folder",false,animation);folderPanel.setTag("folder");setPanelArea("folders");applyCrystalAppearance();
        LinearLayout body=scrollBody(folderPanel);
        if(folder.children.isEmpty())paragraph(body,"Hold an app in Start and choose Add to folder to put it here.");
        android.widget.GridLayout shelf=new android.widget.GridLayout(this);
        int folderColumns=Math.max(2,Math.min(6,store.preferences.getInt("folder_columns",3)));shelf.setColumnCount(folderColumns);shelf.setClipChildren(false);
        for(LauncherStore.Item item:folder.children){
            AppCatalog.Entry entry=catalog.find(item.key);
            IconTile tile=new IconTile(this,item.label,catalog.icon(item),item.key);tile.setTag("shortcut:"+item.id);tile.appearance(store.preferences,"folder");tile.fitCell(Math.max(dp(30),(folderPanel.getLayoutParams().width-dp(56))/folderColumns),dp(132));
            tile.setOnClickListener(v->open(item));
            tile.setOnLongClickListener(v->{menu(item);return true;});
            android.widget.GridLayout.LayoutParams lp=new android.widget.GridLayout.LayoutParams();
            lp.width=0;lp.columnSpec=android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED,1f);lp.height=-2;
            lp.setMargins(dp(4),dp(6),dp(4),dp(8));shelf.addView(tile,lp);
        }
        body.addView(shelf,new LinearLayout.LayoutParams(-1,-2));
        if(origin!=null){physicalFolderId=folder.id;FolderMotion current=new FolderMotion(root,folderPanel,shelf,folderColumns,origin);folderMotion=current;folderPanel.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){folderPanel.getViewTreeObserver().removeOnPreDrawListener(this);if(folderMotion==current&&panel==folderPanel)current.open();return true;}});}
    }
    private void showDesktopOverview(){
        LinearLayout body=scrollBody(openPanel("Desktop shortcuts"));
        for(int i=0;i<store.pageCount();i++){section(body,"Desktop "+(i+1));if(store.pages.get(i).isEmpty())paragraph(body,"No shortcuts yet.");for(LauncherStore.Item item:store.pages.get(i)){Button b=Ui.button(this,item.label,()->open(item));b.setOnLongClickListener(v->{menu(item);return true;});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.bottomMargin=dp(6);body.addView(b,p);}}
    }
    @Override public void startMenu(){showStart();}
    @Override public boolean feed(){
        if(!crystal.feedEnabled()||!crystal.feedEdgeSwipe()||crystal.feedProvider()==3)return false;
        if(crystal.feedProvider()==2){showNativeFeed();return true;}
        if(crystal.feedProvider()==1){openGoogleFeed();return true;}
        openDiscover();return true;
    }
    private void openDiscover(){
        closePanel();discover.open();
        handler.postDelayed(()->{if(!destroyed&&discover.isConnecting()&&overlay==null)showDiscoverConnecting();},250);
    }
    private void retryDiscover(){crystal.setFeedEnabled(true);crystal.setFeedEdgeSwipe(true);crystal.setFeedProvider(0);closePanel();discover.reconnect();openDiscover();}
    private View discoverWaiting;
    private void showDiscoverConnecting(){
        if(discoverWaiting!=null||(!discover.isConnecting()&&!discover.needsAttention()))return;
        LinearLayout waiting=Ui.row(this);waiting.setPadding(dp(10),dp(6),dp(4),dp(6));waiting.setBackground(Ui.satin(this,0xF02D5268,0xF016273E,18));TextView label=Ui.text(this,"Connecting to Google Discover",13,Ui.TEXT);waiting.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        Button pause=Ui.quiet(this,"Pause",discover::pauseRetries),keep=Ui.quiet(this,"Keep feed open",discover::keepOpen),retry=Ui.quiet(this,"Retry",this::retryDiscover),cancel=Ui.quiet(this,"Close",()->discover.close());
        for(Button button:new Button[]{pause,keep,retry,cancel}){button.setTextSize(12);button.setPadding(dp(6),0,dp(6),0);waiting.addView(button,new LinearLayout.LayoutParams(-2,dp(44)));}
        FrameLayout.LayoutParams place=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);place.setMargins(dp(12),insetTop+dp(12),dp(12),0);root.addView(waiting,place);discoverWaiting=waiting;
        Runnable check=new Runnable(){public void run(){if(destroyed||discoverWaiting!=waiting)return;boolean attention=discover.needsAttention();if(!discover.isConnecting()&&!attention){root.removeView(waiting);discoverWaiting=null;return;}label.setText(attention?"Automatic retries paused":"Connecting to Google Discover");pause.setVisibility(attention?View.GONE:View.VISIBLE);keep.setVisibility(attention?View.VISIBLE:View.GONE);keep.setEnabled(discover.canKeepOpen());retry.setVisibility(attention?View.VISIBLE:View.GONE);handler.postDelayed(this,100);}};check.run();
    }
    private void showDiscoverEntry(){
        LinearLayout body=scrollBody(openPanel("Google Discover",true));
        paragraph(body,discover.status());
        paragraph(body,"Google Discover uses the Google app. Reconnect, check its settings, or choose Nth Feed below.");
        action(body,"Repair Google Discover",this::retryDiscover);
        action(body,"Connection details",this::showDiscoverDetails);
        action(body,"Google app info",this::googleAppInfo);
        action(body,"Open Google app",this::openGoogleFeed);
        action(body,"Open Nth Feed",this::showNativeFeed);
        action(body,"Feed preferences",()->showSettings("feed"));
    }
    private void showDiscoverDetails(){
        String report=discover.report();LinearLayout body=scrollBody(openPanel("Discover connection"));
        paragraph(body,"This report stays on this phone. Copy it after an unsuccessful attempt so we can identify the connection stage. It contains app/build versions and connection events; no Google account or news history.");
        action(body,"Test Google Discover",this::retryDiscover);action(body,"Copy connection report",()->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Nth Discover connection",report));toast("Connection report copied");});action(body,"Google app info",this::googleAppInfo);TextView details=Ui.text(this,report,13,Ui.TEXT);details.setTextIsSelectable(true);body.addView(details,new LinearLayout.LayoutParams(-1,-2));
    }
    private void googleAppInfo(){try{startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:com.google.android.googlequicksearchbox")));}catch(RuntimeException missing){toast("Google app info is unavailable.");}}
    private void showNativeFeed(){
        LinearLayout p=openPanel("Nth Feed");nativeFeed=new NativeFeedPage(this,new NativeFeedPage.Host(){public void cards(){showCards("");}public void settings(){showSettings("feed");}});p.addView(nativeFeed,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void showCards(String query){
        LinearLayout p=openPanel("Crystal cards");cardsQuery=query==null?"":query;
        FadingSettingsScrollView scroll=new FadingSettingsScrollView(this);cardsView=new CrystalCards(this,store,cardHost(),cardsQuery);scroll.addView(cardsView);p.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    }
    @Override public CrystalCards.Host cardHost(){return new CrystalCards.Host(){public void notes(){showNotes();}public void nota(){showSettings("nota");}public void settings(){showSettings("cards");}public void place(){showPlaceCards();}public void place(String type){closePanel();LauncherStore.Item card=desktop.addCard(type);if(card==null)toast("Unlock the layout or make space for a card.");else pageChanged();}public boolean dragCard(View view,String type,float x,float y){LauncherStore.Item item=new LauncherStore.Item(CardLayout.key(type),type);for(String[] card:CrystalCards.TYPES)if(card[0].equals(type))item.label=card[1];item.cardHeight=CardLayout.height(type);return drawerDrags.beginDesktop(view,item,x,y);}public void mediaPermission(){showSetup(1);}public void calendarPermission(){requestPermissions(new String[]{android.Manifest.permission.READ_CALENDAR},3074);}public void photos(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);try{startActivityForResult(intent,3073);}catch(RuntimeException missing){toast("Photo picker unavailable.");}}};}
    @Override public void moveCard(LauncherStore.Item item){move(item);}
    private void showPlaceCards(){LinearLayout body=scrollBody(openPanel("Place Crystal Card"));setPanelArea("cards");paragraph(body,"Choose a card for Desktop "+(store.page+1)+". Tap its controls normally. Hold to move or open its resize menu.");for(String[] type:CrystalCards.TYPES)action(body,type[1],()->{closePanel();LauncherStore.Item card=desktop.addCard(type[0]);if(card==null)toast("Unlock the layout or make space for a card.");else{pageChanged();toast(type[1]+" is on Desktop "+(store.page+1));}});}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==NotaWeather.PERMISSION && settingsStudio!=null) settingsStudio.weatherPermissionResult();
        if(request==CrystalCards.WEATHER_PERMISSION && SetupAccess.location(this)) store.preferences.edit().putBoolean("card_weather_location",true).apply();
        if(request==3074||request==3075||request==CrystalCards.WEATHER_PERMISSION){if(cardsView!=null)cardsView.refresh();desktop.refreshCards();}if(setupGuide!=null)setupGuide.refresh();}
    private void cardsPhotosResult(int result,Intent data){
        if(result!=RESULT_OK||data==null)return;
        org.json.JSONArray saved;try{saved=new org.json.JSONArray(store.preferences.getString("card_photo_uris","[]"));}catch(org.json.JSONException bad){saved=new org.json.JSONArray();}
        java.util.Set<String> uris=new java.util.LinkedHashSet<>();for(int i=0;i<saved.length();i++)uris.add(saved.optString(i));
        java.util.List<Uri> chosen=new ArrayList<>();if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)chosen.add(data.getClipData().getItemAt(i).getUri());}else if(data.getData()!=null)chosen.add(data.getData());
        for(Uri uri:chosen){if(uris.size()>=4)break;try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException|IllegalArgumentException ignored){}uris.add(uri.toString());}
        store.preferences.edit().putString("card_photo_uris",new org.json.JSONArray(uris).toString()).apply();if(cardsView!=null)cardsView.refresh();desktop.refreshCards();
    }
    @Override public void backgroundMenu(){showDesktopManager();}
    private void showDesktopManager(){
        if(pendingWidgetId>=0){toast("Finish placing the widget first.");return;}
        desktop.savePositions();LinearLayout p=openPanel("Manage desktops");
        desktopManager=new DesktopManagerView(this,store,catalog,new DesktopManagerView.Host(){
            public void open(int page){store.page=page;store.save();closePanel();desktop.rebuild();pageChanged();}
            public void changed(){desktop.rebuild();pageChanged();}
            public void widgets(){showWidgets();}public void scenes(){showModes();}public void settings(){showSettings();}public void message(String text){toast(text);}
        });desktop.setAlpha(0);p.addView(desktopManager,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void showDesktopActions(){
        LinearLayout body=scrollBody(openPanel("Desktop settings",true));
        action(body,"Manage desktops",this::showDesktopManager);
        action(body,"3D settings",this::showSettings);
        action(body,"Scene studio",this::showModes);action(body,"Shuffle 3D scene now",()->shuffleScene(true));
        action(body,"New folder",this::newFolder);
        action(body,"Arrange shortcuts",()->{desktop.arrange();closePanel();});
        action(body,"All shortcuts",this::showDesktopOverview);
        action(body,"Set default Home",this::requestHome);
    }
    private void savePendingWidget(){
        android.content.SharedPreferences.Editor edit=store.preferences.edit();
        if(pendingWidgetId<0)edit.remove("pending_widget_id").remove("pending_widget_page").remove("pending_widget_request").remove("pending_widget_provider").remove("pending_widget_user").remove("pending_widget_dock");
        else{edit.putInt("pending_widget_dock",pendingWidgetDock).putLong("pending_widget_user",pendingWidgetUser).putInt("pending_widget_id",pendingWidgetId).putInt("pending_widget_page",pendingWidgetPage).putInt("pending_widget_request",pendingWidgetRequest);if(pendingWidgetProvider!=null)edit.putString("pending_widget_provider",pendingWidgetProvider.flattenToString());else edit.remove("pending_widget_provider").remove("pending_widget_user").remove("pending_widget_dock");}
        edit.apply();
    }
    private void showWidgets(){
        releasePendingWidget();
        LinearLayout widgets=openPanel("Widgets");action(widgets,"Add Crystal Card",this::showPlaceCards);new WidgetPicker(this,widgets,desktop,this::chooseWidget);
    }
    private void showRestoreWidgets(){
        LinearLayout body=scrollBody(openPanel("Restore widgets"));try{org.json.JSONArray pending=new org.json.JSONArray(store.preferences.getString("restore_widgets","[]"));
            if(pending.length()==0){paragraph(body,"All saved widget placements are restored.");return;}
            paragraph(body,"Android may ask you to approve or configure each widget. Its saved desktop position and padding will be used.");
            for(int i=0;i<pending.length();i++){org.json.JSONObject record=pending.getJSONObject(i);String label=record.optString("label","Widget");action(body,"Restore "+label,()->restoreWidget(record));body.getChildAt(body.getChildCount()-1).setOnLongClickListener(v->{new android.app.AlertDialog.Builder(this).setTitle("Remove saved widget placement?").setMessage(label).setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{removeSavedWidget(record.optString("token"));showRestoreWidgets();}).show();return true;});}
        }catch(org.json.JSONException invalid){paragraph(body,"No valid saved widget placements are available.");}
    }
    private void removeSavedWidget(String token){try{org.json.JSONArray list=new org.json.JSONArray(store.preferences.getString("restore_widgets","[]")),keep=new org.json.JSONArray();for(int i=0;i<list.length();i++)if(!token.equals(list.getJSONObject(i).optString("token")))keep.put(list.getJSONObject(i));else if(list.getJSONObject(i).optString("location").equals("dock")){int slot=list.getJSONObject(i).optInt("slot",-1);if(slot>=0&&slot<DockStore.MAX)store.dock.clear(slot);}store.preferences.edit().putString("restore_widgets",keep.toString()).apply();}catch(org.json.JSONException ignored){}}
    private void restoreWidget(org.json.JSONObject record){
        android.content.ComponentName provider=android.content.ComponentName.unflattenFromString(record.optString("provider",""));
        for(UserHandle user:getSystemService(android.os.UserManager.class).getUserProfiles()){
            long serial=getSystemService(android.os.UserManager.class).getSerialNumberForUser(user);if(serial!=record.optLong("user",0))continue;
            try{for(android.appwidget.AppWidgetProviderInfo info:widgetManager.getInstalledProvidersForProfile(user))if(info.provider.equals(provider)){
                chooseWidget(info,record);return;
            }}catch(RuntimeException unavailable){/* A work profile may be locked. */}
        }
        toast("Install or enable this widget’s app/profile, then try again.");
    }
    private void completeWidgetRestore(int id){
        String raw=store.preferences.getString("pending_widget_restore","");if(raw.isEmpty())return;
        try{org.json.JSONObject record=new org.json.JSONObject(raw);android.content.SharedPreferences.Editor edit=store.preferences.edit();DesktopBackup.geometry(edit,id,record);org.json.JSONArray pending=new org.json.JSONArray(store.preferences.getString("restore_widgets","[]")),remaining=new org.json.JSONArray();String token=record.getString("token");for(int i=0;i<pending.length();i++)if(!token.equals(pending.getJSONObject(i).optString("token")))remaining.put(pending.getJSONObject(i));edit.putString("restore_widgets",remaining.toString()).putString("last_restored_widget",token).remove("pending_widget_restore").apply();}catch(org.json.JSONException invalid){store.preferences.edit().remove("pending_widget_restore").apply();}
    }
    private void chooseWidget(android.appwidget.AppWidgetProviderInfo info){chooseWidget(info,null);}
    private void chooseWidget(android.appwidget.AppWidgetProviderInfo info,org.json.JSONObject restore){chooseWidget(info,restore,restore!=null&&restore.optString("location").equals("dock")?restore.optInt("slot",-1):-1);}
    private LauncherWidgetHost pendingHost(){return pendingWidgetDock>=0?dockWidgetHost:widgetHost;}
    private void chooseWidget(android.appwidget.AppWidgetProviderInfo info,org.json.JSONObject restore,int dockIndex){
        try{
            releasePendingWidget();if(restore!=null)store.preferences.edit().putString("pending_widget_restore",restore.toString()).apply();pendingWidgetDock=dockIndex;pendingWidgetId=pendingHost().allocateAppWidgetId();pendingWidgetPage=restore==null?store.page:Math.max(0,Math.min(store.pageCount()-1,restore.optInt("page",store.page)));pendingWidgetRequest=3047;pendingWidgetProvider=info.provider;pendingWidgetUser=getSystemService(android.os.UserManager.class).getSerialNumberForUser(info.getProfile());savePendingWidget();
            closePanel();
            onActivityResult(3047,RESULT_OK,new Intent().putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId));
        }catch(RuntimeException error){releasePendingWidget();toast("This widget could not be added. Please try again.");}
    }
    private boolean knownWidget(int id){
        if(store.dock.widgetSlot(id)>=0)return true;
        for(int page=0;page<store.pageCount();page++)for(String raw:store.preferences.getString("widget_ids_page_"+page,"").split(","))if(raw.equals(String.valueOf(id)))return true;
        return false;
    }
    private void releasePendingWidget(){
        if(pendingWidgetId>=0&&!knownWidget(pendingWidgetId))pendingHost().deleteAppWidgetId(pendingWidgetId);
        pendingWidgetId=-1;pendingWidgetDock=-1;pendingWidgetPage=-1;pendingWidgetRequest=0;pendingWidgetProvider=null;store.preferences.edit().remove("pending_widget_restore").apply();savePendingWidget();
    }
    private android.appwidget.AppWidgetProviderInfo resolveWidget(int id,Intent data){
        android.appwidget.AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(id);if(info!=null)return info;
        android.content.ComponentName provider=pendingWidgetProvider;
        if(data!=null){android.content.ComponentName returned=data.getParcelableExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);if(returned!=null)provider=returned;}
        if(provider!=null)for(UserHandle user:getSystemService(android.os.UserManager.class).getUserProfiles()){if(pendingWidgetUser>=0&&getSystemService(android.os.UserManager.class).getSerialNumberForUser(user)!=pendingWidgetUser)continue;try{for(android.appwidget.AppWidgetProviderInfo candidate:widgetManager.getInstalledProvidersForProfile(user))if(provider.equals(candidate.provider))return candidate;}catch(RuntimeException unavailable){/* Profile locked. */}}
        return null;
    }
    @Override public void configureWidget(int id){
        try{(store.dock.widgetSlot(id)>=0?dockWidgetHost:widgetHost).startAppWidgetConfigureActivityForResult(this,id,0,3050,null);}
        catch(RuntimeException error){toast("Widget settings unavailable.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==101||request==3083){if(setupPending())queueSetupReturn();return;}
        if(request==3073){cardsPhotosResult(result,data);return;}
        if(request==3080||request==3081){sceneImageResult(request,result,data);return;}
        if(request==3082){applySettings(false);return;}
        if(request==3090||request==3091){scenePackResult(request,result,data);return;}
        if(request==3076){wallpaperPhotoResult(result,data);return;}
        if(request==3071||request==3072){settingsDocumentResult(request,result,data);return;}
        if(request==3050){desktop.rebuild();dockContents.rebuild();return;}
        if(request!=3047&&request!=3048&&request!=3049)return;
        if(pendingWidgetId<0||request!=pendingWidgetRequest)return;
        if(result!=RESULT_OK){releasePendingWidget();return;}
        int id=data==null?pendingWidgetId:data.getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,pendingWidgetId);
        if(id!=pendingWidgetId){releasePendingWidget();toast("Widget selection was not completed. Please try again.");return;}
        android.appwidget.AppWidgetProviderInfo info=resolveWidget(id,data);
        if(info==null){releasePendingWidget();toast("Widget provider unavailable.");return;}
        pendingWidgetProvider=info.provider;savePendingWidget();
        if(request==3047&&widgetManager.getAppWidgetInfo(id)==null){
            boolean bound=false;
            try{bound=widgetManager.bindAppWidgetIdIfAllowed(id,info.getProfile(),info.provider,null);}catch(SecurityException error){android.util.Log.w("NthWidgets","Widget binding needs user permission",error);}
            if(!bound){
                try{
                    pendingWidgetRequest=3048;savePendingWidget();
                    Intent bind=new Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND);
                    bind.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,id);bind.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,info.provider);bind.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE,info.getProfile());
                    startActivityForResult(bind,3048);
                }catch(RuntimeException error){releasePendingWidget();toast("Could not request widget permission.");}
                return;
            }
        }
        if(widgetManager.getAppWidgetInfo(id)==null){releasePendingWidget();toast("Widget permission was not applied. Please try again.");return;}
        boolean optional=android.os.Build.VERSION.SDK_INT>=28&&(info.widgetFeatures&android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL)!=0&&(info.widgetFeatures&android.appwidget.AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE)!=0;
        if(request!=3049&&info.configure!=null&&!optional){
            try{pendingWidgetRequest=3049;savePendingWidget();pendingHost().startAppWidgetConfigureActivityForResult(this,id,0,3049,null);}
            catch(RuntimeException error){releasePendingWidget();toast("Widget setup could not open. Please choose the widget again.");}
            return;
        }
        if(pendingWidgetDock>=0){int slot=pendingWidgetDock;clearDockSpace(slot);store.dock.widget(slot,id,info.provider.flattenToString(),pendingWidgetUser,java.util.Objects.toString(info.loadLabel(getPackageManager()),"Widget"));completeWidgetRestore(id);pendingWidgetId=-1;pendingWidgetDock=-1;pendingWidgetPage=-1;pendingWidgetRequest=0;pendingWidgetProvider=null;savePendingWidget();store.preferences.edit().putBoolean("dock_content",true).putInt("dock_space_count",Math.max(slot+1,store.preferences.getInt("dock_space_count",3))).apply();closePanel();applyDesktopChrome();toast("Widget placed in Dock space "+(slot+1));return;}
        int page=Math.max(0,Math.min(store.pageCount()-1,pendingWidgetPage));String key="widget_ids_page_"+page;
        String previous=store.preferences.getString(key,"");
        if(!knownWidget(id))store.preferences.edit().putString(key,previous.isEmpty()?String.valueOf(id):previous+","+id).apply();
        store.preferences.edit().putString("widget_provider_"+id,info.provider.flattenToString()).putLong("widget_user_"+id,getSystemService(android.os.UserManager.class).getSerialNumberForUser(info.getProfile())).apply();completeWidgetRestore(id);
        pendingWidgetId=-1;pendingWidgetDock=-1;pendingWidgetPage=-1;pendingWidgetRequest=0;pendingWidgetProvider=null;savePendingWidget();
        store.page=page;store.save();closePanel();desktop.rebuild();pageChanged();
        toast(desktop.revealWidget(id)?"Widget placed on Desktop "+(page+1):"Widget saved, but its provider is unavailable. Hold the placeholder to retry or remove it.");
    }
    private void newFolder(){EditText edit=new EditText(this);edit.setSingleLine(true);edit.setHint("Folder name");Ui.pad(edit,16);new AlertDialog.Builder(this).setTitle("New folder").setView(edit).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->{String name=edit.getText().toString().trim();if(!name.isEmpty()&&store.pages.get(store.page).size()<60){store.pages.get(store.page).add(new LauncherStore.Item("folder",name));store.save();desktop.rebuild();pageChanged();closePanel();}}).show();}
    private String editDesktop(boolean add){
        if(store.preferences.getBoolean("lock_layout",false))return desktopMessage("Unlock desktop layout first.");
        if(pendingWidgetId>=0)return desktopMessage("Finish placing the widget first.");
        desktop.savePositions();String message;
        if(add){int page=store.appendPage();if(page<0)return desktopMessage("All 24 desktops are in use.");store.page=page;store.save();message="Desktop "+(page+1)+" added.";}
        else{if(!store.removeEmptyPage(store.page))return desktopMessage("Only an empty desktop can be removed. Keep at least one desktop.");message="Empty desktop removed.";}
        desktop.rebuild();pageChanged();return desktopMessage(message);
    }
    private String desktopMessage(String text){toast(text);return text;}

    private void removeScene(){
        Ui.themeDialog(new AlertDialog.Builder(this).setTitle("Remove my scene?").setMessage("This removes the saved custom scene and its draft. Your built-in scenes remain available.").setNegativeButton("Cancel",null).setPositiveButton("Remove",(d,w)->{
            if(sceneComposer!=null){onPanelClose=null;sceneComposer.dispose();sceneComposer=null;}sceneFiles.execute(()->{try{LayeredScene.write(this,null,false);LayeredScene.write(this,null,true);handler.post(()->{if(destroyed)return;if(store.preferences.getInt("wallpaper_source",0)==3)store.preferences.edit().putInt("wallpaper_source",0).apply();store.preferences.edit().remove("scene_creator_step").apply();layeredStamp=-1;applySettings(false);showModes();toast("Custom scene removed");});}catch(Exception error){handler.post(()->toast("Could not remove this scene"));}});
        }).create());
    }
    private void showDockSpaces(){LinearLayout body=scrollBody(openPanel("Dock spaces"));paragraph(body,"Each space can hold an app, a folder or an Android widget. Reducing the space count keeps the hidden contents.");for(int i=0;i<DockStore.MAX;i++){final int n=i;DockStore.Slot slot=store.dock.slots.get(i);String name=slot.item!=null?slot.item.label:slot.widget()?slot.label:slot.blank?"Leave empty":"Empty";action(body,"Space "+(i+1)+" · "+name,()->showDockSpace(n));}action(body,"Dock settings",()->showSettings("dock"));}
    private void showDockSpace(int index){if(index<0||index>=DockStore.MAX)return;DockStore.Slot slot=store.dock.slots.get(index);LinearLayout body=scrollBody(openPanel("Dock space"));paragraph(body,"Space "+(index+1));if(slot.item!=null)action(body,"Open "+slot.item.label,()->open(slot.item));if(slot.item!=null)action(body,slot.item.folder()?"Folder actions":"App actions",()->menu(slot.item));
        action(body,"Choose app",()->chooseDockApp(index));action(body,"Choose folder",()->chooseDockFolder(index));action(body,"Create folder",()->{EditText name=new EditText(this);name.setText("Dock folder");Ui.themeDialog(new AlertDialog.Builder(this).setTitle("New dock folder").setView(name).setNegativeButton("Cancel",null).setPositiveButton("Create",(d,w)->{if(!name.getText().toString().trim().isEmpty())putDockItem(index,new LauncherStore.Item("folder",name.getText().toString().trim()));}).create());});
        action(body,"Choose widget",()->{if(pendingWidgetId>=0){toast("Finish placing the widget first.");return;}new WidgetPicker(this,openPanel("Dock widgets"),desktop,info->chooseWidget(info,null,index));});
        if(slot.widgetId>=0){android.appwidget.AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(slot.widgetId);if(info!=null&&info.configure!=null)action(body,"Widget settings",()->configureWidget(slot.widgetId));action(body,"Padding · "+(store.preferences.getBoolean("widget_padding_"+slot.widgetId,false)?"Expanded":"Default"),()->{store.preferences.edit().putBoolean("widget_padding_"+slot.widgetId,!store.preferences.getBoolean("widget_padding_"+slot.widgetId,false)).apply();dockContents.rebuild();showDockSpace(index);});}
        action(body,"Leave empty",()->{clearDockSpace(index);store.dock.leaveEmpty(index);dockContents.rebuild();showDockSpaces();});action(body,"Dock size and appearance",()->showSettings("dock"));}
    private void chooseDockApp(int index){LinearLayout panel=openPanel("Choose dock app");setPanelArea("dock");EditText search=new EditText(this);search.setHint("Search apps");search.setTextColor(Ui.TEXT);search.setHintTextColor(Ui.MUTED);panel.addView(search);LinearLayout body=scrollBody(panel);Runnable fill=()->{body.removeAllViews();for(AppCatalog.Entry app:catalog.filter(search.getText().toString(),false))action(body,app.label,()->putDockItem(index,new LauncherStore.Item(app.key,app.label)));};search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence t,int st,int count,int after){}public void onTextChanged(CharSequence t,int st,int before,int count){fill.run();}public void afterTextChanged(Editable e){}});fill.run();}
    private void chooseDockFolder(int index){LinearLayout body=scrollBody(openPanel("Choose dock folder"));setPanelArea("dock");paragraph(body,"A separate copy of the folder and its contents will be saved in this dock space.");for(LauncherStore.Item folder:store.folders())action(body,folder.label,()->putDockItem(index,folder));}
    private void putDockItem(int index,LauncherStore.Item item){LauncherStore.Item copy=DockStore.copy(item);clearDockSpace(index);store.dock.slots.get(index).item=copy;store.save();store.preferences.edit().putBoolean("dock_content",true).putInt("dock_space_count",Math.max(index+1,store.preferences.getInt("dock_space_count",3))).apply();applyDesktopChrome();closePanel();}
    private void clearDockSpace(int index){DockStore.Slot old=store.dock.slots.get(index);if(old.widgetId>=0)try{dockWidgetHost.deleteAppWidgetId(old.widgetId);}catch(RuntimeException ignored){}try{org.json.JSONArray queue=new org.json.JSONArray(store.preferences.getString("restore_widgets","[]")),keep=new org.json.JSONArray();for(int i=0;i<queue.length();i++){org.json.JSONObject record=queue.getJSONObject(i);if(!record.optString("location").equals("dock")||record.optInt("slot",-1)!=index)keep.put(record);}store.preferences.edit().putString("restore_widgets",keep.toString()).apply();}catch(org.json.JSONException ignored){}store.dock.clear(index);}
    private void randomizeIfDue(){if(sceneRandomizer==null||desktop==null)return;boolean eligible=resumed&&!destroyed&&overlay==null&&(drawerDrags==null||!drawerDrags.active())&&!powerSave&&getWindow().getDecorView().hasWindowFocus()&&android.os.SystemClock.uptimeMillis()>=shuffleHomeAfter&&pendingWidgetId<0&&!setupPending()&&desktop.idleForShuffle();if(sceneRandomizer.due(System.currentTimeMillis(),eligible))shuffleScene(false);}
    private void shuffleScene(boolean manual){desktop.savePositions();int old=store.mode,source=store.preferences.getInt("wallpaper_source",0);int chosen=sceneRandomizer.choose(System.currentTimeMillis());if(chosen!=old||source!=0)applySettings(true);if(settingsStudio!=null)settingsStudio.refreshPreview();if(manual)toast("Scene: "+LauncherStore.MODES[chosen]);}
    private void randomizerControls(LinearLayout body){paragraph(body,"Shuffle now switches to a built-in 3D scene. Automatic shuffle waits for idle Home and pauses while using menus, apps, other wallpapers or battery saving.");action(body,"Shuffle 3D scene now",()->shuffleScene(true));action(body,"Selected scenes · "+Integer.bitCount(sceneRandomizer.mask()),this::chooseShuffleScenes);body.getChildAt(body.getChildCount()-1).setTag("setting:shuffle_scenes");}
    private void chooseShuffleScenes(){if(randomizerDialog!=null)randomizerDialog.dismiss();boolean[] checked=new boolean[LauncherStore.MODES.length];int mask=sceneRandomizer.mask();for(int i=0;i<checked.length;i++)checked[i]=(mask&(1<<i))!=0;AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Scenes to shuffle").setMultiChoiceItems(LauncherStore.MODES,checked,(d,i,on)->checked[i]=on).setNegativeButton("Cancel",null).setPositiveButton("Apply",null).create();randomizerDialog=dialog;Ui.themeDialog(dialog);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{int selected=0;for(int i=0;i<checked.length;i++)if(checked[i])selected|=1<<i;if(selected==0){toast("Keep at least one scene selected.");return;}settingsModel().set("shuffle_scenes",selected);dialog.dismiss();randomizerDialog=null;if(settingsStudio!=null&&settingsStudio.page().equals("shuffle"))settingsStudio.showPage("shuffle");});}
    private void showModes(){
        LinearLayout p=openPanel("Scene studio");LinearLayout body=scrollBody(p);action(body,"Import scene pack",this::importScenePack);if(LayeredScene.file(this,false).exists())action(body,"Export my scene",this::exportAppliedScene);action(body,"Shuffle 3D scene now",()->shuffleScene(true));action(body,"Scene randomizer settings",()->showSettings("shuffle"));
        if(LayeredScene.file(this,false).exists()){Button own=Ui.button(this,"My layered scene",()->{store.preferences.edit().putInt("wallpaper_source",3).apply();applySettings(false);closePanel();});own.setTag("my-layered-scene");own.setSelected(store.preferences.getInt("wallpaper_source",0)==3);own.setBackground(Ui.touch(this,Ui.crystal(this,20,own.isSelected()),20));body.addView(own,new LinearLayout.LayoutParams(-1,dp(72)));}
        action(body,LayeredScene.file(this,false).exists()?"Edit my scene":"Create a scene",this::showSceneCreator);
        if(LayeredScene.file(this,false).exists())action(body,"Remove my scene",this::removeScene);
        String[] descriptions=SceneCatalog.DESCRIPTIONS;
        for(int n=0;n<LauncherStore.MODES.length;n++){
            final int selected=n;SceneCard card=new SceneCard(this,n,descriptions[n],store.mode==n&&store.preferences.getInt("wallpaper_source",0)==0);
            card.setOnClickListener(v->{desktop.savePositions();store.mode=selected;store.preferences.edit().putInt("wallpaper_source",0).apply();store.save();applySettings(true);closePanel();});
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(108));cp.bottomMargin=dp(12);body.addView(card,cp);
        }
    }
    private SettingsModel settingsModel(){if(settingsModel==null)settingsModel=new SettingsModel(store,crystal,rebuild->{changed(rebuild);applyCrystalAppearance();});return settingsModel;}
    private void showSettings(){showSettings("root");}
    private void showSettings(String page){
        LinearLayout p=openPanel("3D settings");settingsStudio=new SettingsStudio(this,settingsModel(),catalog,new SettingsStudio.Host(){
            public void area(String id){setPanelArea(id);applyCrystalAppearance();if(p.getChildCount()>0)p.getChildAt(0).setVisibility(id.equals("nota")?View.GONE:View.VISIBLE);}public void close(){requestClosePanel();}
            public void setup(){showSetup(0);}public void home(){requestHome();}public void arrange(){desktop.arrange();toast("Desktop arranged");}public void recenter(){motion.recenter();toast("Tilt centered");}public void feed(){showNativeFeed();}public void reconnectFeed(){retryDiscover();}public void feedDetails(){showDiscoverDetails();}public String feedStatus(){return discover.status();}public void googleApp(){openGoogleFeed();}public void cards(){showCards("");}public void widgets(){showWidgets();}public void folder(){newFolder();}public void overview(){showDesktopOverview();}public void manage(){showDesktopManager();}public void autoSort(){showAutoSort();}public String desktop(boolean add){return editDesktop(add);}public void apps(){showStart();}public void about(){showAbout();}public void backup(boolean restore){settingsDocument(restore);}public void restoreWidgets(){showRestoreWidgets();}public void photoWallpaper(){chooseWallpaperPhoto();}public void androidWallpaper(boolean live){chooseAndroidWallpaper(live);}public void createScene(){showSceneCreator();}public void importScene(){importScenePack();}public void exportScene(){exportAppliedScene();}public void removeScene(){MainActivity.this.removeScene();}public void dockSpaces(){showDockSpaces();}public void placeCards(){showPlaceCards();}public void randomizer(LinearLayout body){randomizerControls(body);}public void sceneWallpaper(){store.preferences.edit().putInt("wallpaper_source",0).apply();applySettings(false);showModes();}
            public Runnable presets(LinearLayout body){DesktopPresetControls controls=new DesktopPresetControls(MainActivity.this,settingsModel(),body,loader,handler,new DesktopPresetControls.Host(){public void beforeCapture(){desktop.savePositions();store.save();}public boolean widgetPending(){return pendingWidgetId>=0;}public void applied(){shortcuts.syncRemoved();if(settingsStudio!=null)settingsStudio.refreshPreview();}public void message(String text){toast(text);}});return controls::dispose;}
            public boolean openApp(String query){List<AppCatalog.Entry> matches=catalog.filter(query,false);if(matches.size()==1){launch(matches.get(0).key);return true;}if(matches.size()>1){showStart();appSearch.setText(query);toast("Choose an app from the matching shortcuts.");return true;}return false;}
        },page);p.addView(settingsStudio,new LinearLayout.LayoutParams(-1,0,1));
    }
    private void settingsDocument(boolean restore){
        if(pendingWidgetId>=0){toast("Finish placing the widget before making or restoring a backup.");return;}
        Intent intent=new Intent(restore?Intent.ACTION_OPEN_DOCUMENT:Intent.ACTION_CREATE_DOCUMENT).setType(restore?"*/*":"application/json").addCategory(Intent.CATEGORY_OPENABLE);
        if(!restore)intent.putExtra(Intent.EXTRA_TITLE,"Nth-Desktop-3D-Desktop.nth3dbackup");
        try{startActivityForResult(intent,restore?3072:3071);}catch(RuntimeException unavailable){toast("Android file picker unavailable.");}
    }
    private void settingsDocumentResult(int request,int result,Intent data){
        if(result!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();
        if(request==3071){
            final byte[] snapshot;
            try{desktop.savePositions();store.save();org.json.JSONObject document=DesktopBackup.capture(this,settingsModel());new DesktopBackup.Prepared(document,settingsModel());snapshot=document.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);}catch(Exception error){toast("Could not prepare the desktop backup. Nothing was saved.");return;}
            loader.execute(()->{try{try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new java.io.IOException();out.write(snapshot);}byte[] check;try(java.io.InputStream in=getContentResolver().openInputStream(uri)){check=WallpaperStore.read(in,DesktopBackup.MAX_BYTES);}if(!java.util.Arrays.equals(snapshot,check))throw new java.io.IOException("Backup verification failed");handler.post(()->{if(!destroyed)toast("Desktop backup saved and verified");});}catch(Exception error){handler.post(()->{if(!destroyed)toast("Could not save and verify this backup. Choose another destination.");});}});return;
        }
        loader.execute(()->{try{byte[] bytes;try(java.io.InputStream in=getContentResolver().openInputStream(uri)){bytes=WallpaperStore.read(in,DesktopBackup.MAX_BYTES);}org.json.JSONObject document=new org.json.JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));
            final boolean legacy="nth-desktop-3d-settings".equals(document.optString("format"));final DesktopBackup.Prepared desktopBackup;
            if(legacy){DesktopBackup.validateSettings(document,settingsModel());desktopBackup=null;}else desktopBackup=new DesktopBackup.Prepared(document,settingsModel());
            handler.post(()->{if(destroyed)return;if(restoreConfirmation!=null)restoreConfirmation.dismiss();String message=legacy?"This older file contains settings only. It has no folders, desktop shortcuts, notes or photo wallpaper to recover. Your current desktop contents will be kept.":desktopBackup.summary();
                restoreConfirmation=new android.app.AlertDialog.Builder(this).setTitle(legacy?"Restore settings only?":"Restore desktop backup?").setMessage(message).setNegativeButton("Cancel",null).setPositiveButton(legacy?"Restore settings":"Restore desktop",(dialog,which)->{try{if(legacy)settingsModel().importSettings(document);else settingsModel().importDesktopBackup(this,desktopBackup);showSettings("backup");toast(legacy?"Settings restored. This file had no folders.":"Desktops and folders restored. Undo is available.");}catch(Exception error){toast("Could not restore this backup. Please try again.");}finally{restoreConfirmation=null;}}).create();restoreConfirmation.setOnDismissListener(dialog->{restoreConfirmation=null;});restoreConfirmation.show();});
        }catch(Exception error){handler.post(()->{if(!destroyed)toast("This backup is invalid or incomplete. No desktop contents were replaced.");});}});
    }
    @Override public boolean startItemDrag(View tile,LauncherStore.Item item){return drawerDrags.beginDesktop(tile,item);}
    @Override public boolean startSizedDrag(View tile,LauncherStore.Item item,float x,float y){return drawerDrags.beginDesktop(tile,item,x,y);}
    @Override public void assignedGesture(String id){int fallback=id.equals("gesture_up")?0:id.equals("gesture_down")?3:id.equals("gesture_pinch_in")?14:4;int action=store.preferences.getInt(id,fallback);String target=store.preferences.getString(id+"_target","");if(action==16){if(target.isEmpty())toast("Choose an app in Gesture settings first.");else launch(target);return;}if(action==17){for(List<LauncherStore.Item> page:store.pages)for(LauncherStore.Item item:page){if(item.id.equals(target)){open(item);return;}for(LauncherStore.Item child:item.children)if(child.id.equals(target)){open(child);return;}}toast("This shortcut is no longer available. Choose another in Gesture settings.");return;}gesture(action);}
    @Override public void gesture(int action){switch(action){case 0:showStart();break;case 1:showSettings();break;case 2:showSettings("nota");break;case 3:showControls();break;case 5:case 6:case 7:case 8:if(action==8&&Build.VERSION.SDK_INT<28){toast("Screen lock gestures require Android 9 or later.");break;}int global=action==5?4:action==6?5:action==7?3:8;if(!GestureAccess.perform(global))showSetup(2);break;case 9:showStart();if(appSearch!=null){appSearch.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(appSearch,InputMethodManager.SHOW_IMPLICIT);}break;case 10:showNativeFeed();break;case 11:try{startActivity(new Intent(Intent.ACTION_ASSIST));}catch(RuntimeException unavailable){toast("Choose a system assistant in Android settings first.");}break;case 12:boolean hide=!store.preferences.getBoolean("gesture_hide_status",false);store.preferences.edit().putBoolean("gesture_hide_status",hide).apply();getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|(hide?View.SYSTEM_UI_FLAG_FULLSCREEN:0));break;case 13:settingsModel().set("show_dock",settingsModel().value("show_dock")==1?0:1);break;case 14:showDesktopManager();break;case 15:showModes();break;case 18:desktop.changePage(-1);break;case 19:desktop.changePage(1);break;default:break;}}
    private void openGoogleFeed(){
        Intent intent=getPackageManager().getLaunchIntentForPackage("com.google.android.googlequicksearchbox");
        if(intent==null){toast("Google app unavailable. Install or enable Google to use the fallback.");return;}
        try{
            android.app.ActivityOptions transition=android.app.ActivityOptions.makeCustomAnimation(this,
                android.R.anim.slide_in_left,android.R.anim.fade_out);
            if(crystal.feed3dTransition()&&!store.reducedMotion)startActivity(intent,transition.toBundle());else startActivity(intent);
        }catch(RuntimeException e){toast("Could not open Google app.");}
    }
    private interface BoolAction{void set(boolean v);}private interface IntAction{void set(int v);}
    private void changed(boolean rebuild){store.save();applySettings(rebuild);}
    private void toggle(LinearLayout body,String label,boolean value,BoolAction change){Switch s=new Switch(this);s.setText(label);s.setTextSize(15);s.setTextColor(Ui.TEXT);s.setPadding(0,dp(8),0,dp(8));s.setMinHeight(dp(52));s.setChecked(value);s.setOnCheckedChangeListener((b,v)->change.set(v));body.addView(s,new LinearLayout.LayoutParams(-1,-2));}
    private void slider(LinearLayout body,String label,int value,int min,int max,IntAction change){TextView t=Ui.text(this,label+"  "+value,14,Ui.MUTED);t.setPadding(0,dp(12),0,0);body.addView(t);SeekBar s=new SeekBar(this);s.setMax(max-min);s.setProgress(value-min);s.setContentDescription(label);body.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int progress,boolean user){t.setText(label+"  "+(progress+min));if(user)change.set(progress+min);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});}
    private void choice(LinearLayout body,String[] choices,int selected,IntAction change){for(int i=0;i<choices.length;i++){final int n=i;Button b=Ui.button(this,(i==selected?"✓  ":"")+choices[i],()->change.set(n));b.setSelected(i==selected);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(46));p.bottomMargin=dp(7);body.addView(b,p);}}

    private void showNotes(){
        LinearLayout p=openPanel("Nth Notes");paragraph(p,"Saved on this device as you type.");
        EditText edit=new EditText(this);edit.setGravity(Gravity.TOP);edit.setTextColor(Ui.TEXT);edit.setHintTextColor(Ui.MUTED);edit.setTextSize(17);edit.setHint("Write something");edit.setBackground(Ui.surface(0xFF0E1A2E,14,this));Ui.pad(edit,14);edit.setText(store.preferences.getString("note",""));p.addView(edit,new LinearLayout.LayoutParams(-1,0,1));
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
    private String appVersion(){try{return getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(android.content.pm.PackageManager.NameNotFoundException error){return "";}}
    private void showAbout(){
        LinearLayout body=scrollBody(openPanel("Nth Desktop 3D"));paragraph(body,"Version "+appVersion()+"\n\nSeven scenes. One desktop.\n\nTilt gently to explore depth. Swipe the desktop or use the arrows to turn pages. Hold an app for its menu. Hold the taskbar for desktop options.\n\nScene studio changes the environment. 3D settings mixes the transition and menu style independently.");
        paragraph(body,"This first version includes app launching, persistent shortcuts and folders, Nth Notes, Calculator, Calendar, scene controls, and Android settings shortcuts. Android widgets support direct hold/drag, cell snapping and resizing. The wider Nth utility suite and .nthdesktop imports are planned for later passes.");
        action(body,"Scene studio",this::showModes);action(body,"3D settings",this::showSettings);action(body,"Set default Home",this::requestHome);
    }
    private void queueSetupReturn(){handler.removeCallbacks(setupReturn);handler.post(setupReturn);}
    private void restoreSetup(){
        if(destroyed||!resumed||!setupPending())return;
        if(setupGuide==null||panel==null||overlay==null||overlay.getParent()!=root)showSetup(store.preferences.getInt("setup_step",0));
        else setupGuide.refresh();
        // Home role changes can return through result, intent and focus callbacks in
        // different orders. Recover the existing panel without depending on a later
        // notification-shade round trip or replaying an interrupted opening pose.
        overlay.setVisibility(View.VISIBLE);overlay.setAlpha(1);overlay.setTranslationX(0);overlay.setTranslationY(0);overlay.bringToFront();
        panel.setVisibility(View.VISIBLE);panel.animate().cancel();PanelAnimations.apply(panel,PanelAnimations.STACK,1);panel.setTranslationX(0);
        desktop.setModal(true);layoutPanel();overlay.requestLayout();overlay.invalidate();root.invalidate();
    }
    private boolean setupPending(){return store.preferences.getBoolean("setup_in_progress",false);}
    private void welcome(){if(!destroyed&&resumed&&setupGuide==null&&(setupPending()||!store.preferences.getBoolean("welcomed",false)))showSetup(store.preferences.getInt("setup_step",0));}
    private void showSetup(int step){LinearLayout body=openPanel("Setup & permissions");setPanelArea(null);setupGuide=new SetupGuide(this,store,step,this::requestHome,this::closePanel);body.addView(setupGuide,new LinearLayout.LayoutParams(-1,0,1));}
    private void setPanelArea(String area) {
        panelArea = area;
        boolean reading = "nota".equals(area);
        // The wallpaper is a sibling of the shell, so it keeps its position and animation.
        shell.setVisibility(reading ? View.INVISIBLE : View.VISIBLE);
        if (areaButton != null) {
            areaButton.setVisibility(area == null || reading ? View.GONE : View.VISIBLE);
            areaButton.setContentDescription(AreaSettingsMenu.title(area) + " settings menu");
        }
    }
    private void showAreaSettings(){if(panelArea==null)return;areaMenu=new AreaSettingsMenu(this,settingsModel(),panelArea,new AreaSettingsMenu.Host(){public void page(String id){showSettings(id);}public void setting(String id){SettingsModel.Spec spec=settingsModel().specs.get(id);if(spec!=null){showSettings(spec.page);settingsStudio.focusSetting(id);}}public void setup(){showSetup(0);}public void action(String id){switch(id){case "scene":showSceneCreator();break;case "remove_scene":removeScene();break;case "dock":showDockSpaces();break;default:if(sceneComposer!=null)sceneComposer.focus(id);else{sceneFocus=id;showSceneCreator();}}}}).show();}
    private void requestHome(){
        if(Build.VERSION.SDK_INT>=29){RoleManager roles=getSystemService(RoleManager.class);if(roles.isRoleAvailable(RoleManager.ROLE_HOME)){if(roles.isRoleHeld(RoleManager.ROLE_HOME)){toast("Nth Desktop 3D is already your Home app.");return;}try{startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME),101);return;}catch(RuntimeException ignored){}}}
        openSystem(Settings.ACTION_HOME_SETTINGS);
    }
}
