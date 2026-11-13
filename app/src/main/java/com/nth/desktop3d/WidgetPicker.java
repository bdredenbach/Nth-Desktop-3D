package com.nth.desktop3d;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.ListView;
import android.widget.BaseAdapter;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Browse provider previews without allocating, binding or starting a real widget. */
final class WidgetPicker {
    interface Selection { void choose(AppWidgetProviderInfo info); }
    private final Context context;
    private final DesktopView desktop;
    private final Selection selection;
    private final ListView groups;
    private final List<Row> rows=new ArrayList<>();
    private final android.util.LruCache<String,Drawable> previews=new android.util.LruCache<>(8);
    private interface Row { View create(); }
    private final BaseAdapter adapter=new BaseAdapter(){
        public int getCount(){return rows.size();}public Object getItem(int position){return rows.get(position);}public long getItemId(int position){return position;}
        public View getView(int position,View recycled,ViewGroup parent){return rows.get(position).create();}
        public boolean isEnabled(int position){return true;}
    };
    private final List<Group> catalog=new ArrayList<>();
    private final java.util.Set<String> expanded=new java.util.HashSet<>();
    private static final class Group {
        String key,label;Drawable icon;
        final List<AppWidgetProviderInfo> providers=new ArrayList<>();
    }
    WidgetPicker(Context context,LinearLayout panel,DesktopView desktop,Selection selection){
        this.context=context;this.desktop=desktop;this.selection=selection;
        EditText search=new EditText(context);search.setSingleLine(true);search.setTextSize(16);search.setTextColor(Ui.TEXT);search.setHintTextColor(Ui.MUTED);search.setHint("Search widgets");search.setContentDescription("Search widgets");
        panel.addView(search,new LinearLayout.LayoutParams(-1,Ui.dp(context,52)));
        TextView tip=Ui.text(context,"Expand an app to preview its widgets. Tap a preview to add it.",13,Ui.MUTED);tip.setPadding(0,Ui.dp(context,8),0,Ui.dp(context,12));panel.addView(tip);
        groups=new ListView(context);groups.setDivider(null);groups.setSelector(android.R.color.transparent);groups.setAdapter(adapter);panel.addView(groups,new LinearLayout.LayoutParams(-1,0,1));
        load();render("");
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int b,int c){render(s.toString());}});
        panel.setFocusableInTouchMode(true);panel.requestFocus();
    }
    private void load(){
        AppWidgetManager manager=AppWidgetManager.getInstance(context);Map<String,Group> map=new LinkedHashMap<>();
        for(UserHandle user:context.getSystemService(UserManager.class).getUserProfiles()){
            List<AppWidgetProviderInfo> installed;
            try{installed=manager.getInstalledProvidersForProfile(user);}catch(RuntimeException unavailable){continue;}
            for(AppWidgetProviderInfo info:installed){
                if((info.widgetCategory&AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)==0)continue;
                String key=info.provider.getPackageName()+"@"+user;
                Group group=map.get(key);
                if(group==null){
                    group=new Group();group.key=key;group.label=info.provider.getPackageName();
                    try{android.content.pm.ApplicationInfo app=context.getPackageManager().getApplicationInfo(info.provider.getPackageName(),0);group.label=app.loadLabel(context.getPackageManager()).toString();group.icon=context.getPackageManager().getUserBadgedIcon(app.loadIcon(context.getPackageManager()),user);}catch(android.content.pm.PackageManager.NameNotFoundException|RuntimeException ignored){}
                    map.put(key,group);
                }
                group.providers.add(info);
            }
        }
        Collator order=Collator.getInstance();catalog.addAll(map.values());catalog.sort((a,b)->order.compare(a.label,b.label));
        for(Group group:catalog)group.providers.sort((a,b)->order.compare(a.loadLabel(context.getPackageManager()),b.loadLabel(context.getPackageManager())));
    }
    private void render(String query){
        rows.clear();String q=query.trim().toLowerCase(Locale.ROOT);int count=0;
        for(Group group:catalog){
            List<AppWidgetProviderInfo> matches=new ArrayList<>();
            for(AppWidgetProviderInfo info:group.providers)if(q.isEmpty()||group.label.toLowerCase(Locale.ROOT).contains(q)||info.loadLabel(context.getPackageManager()).toLowerCase(Locale.ROOT).contains(q))matches.add(info);
            if(matches.isEmpty())continue;count++;
            boolean open=!q.isEmpty()||expanded.contains(group.key);
            rows.add(()->{
                Button heading=Ui.button(context,group.label+"  ·  "+matches.size()+" widget"+(matches.size()==1?"":"s")+(open?"  ▴":"  ▾"),()->{
                    boolean wasOpen=expanded.contains(group.key);expanded.clear();if(!wasOpen)expanded.add(group.key);render(query);
                });
                heading.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);heading.setContentDescription((open?"Collapse ":"Expand ")+group.label+" widgets");
                if(group.icon!=null){Drawable icon=group.icon.mutate();icon.setBounds(0,0,Ui.dp(context,32),Ui.dp(context,32));heading.setCompoundDrawablesRelative(icon,null,null,null);heading.setCompoundDrawablePadding(Ui.dp(context,12));}
                return heading;
            });
            if(open)for(AppWidgetProviderInfo info:matches)rows.add(()->createPreview(info));
        }
        if(count==0){TextView empty=Ui.text(context,catalog.isEmpty()?"No Home screen widgets are available from your installed apps.":"No matching widgets.",15,Ui.MUTED);rows.add(()->empty);}
        adapter.notifyDataSetChanged();
    }
    private View createPreview(AppWidgetProviderInfo info){
        String label=info.loadLabel(context.getPackageManager());int[] spans=desktop.widgetSpan(info);
        LinearLayout card=Ui.column(context);card.setGravity(Gravity.CENTER);card.setPadding(Ui.dp(context,12),Ui.dp(context,12),Ui.dp(context,12),Ui.dp(context,12));card.setBackground(Ui.touch(context,Ui.surface(0x70445E71,18,context),18));
        card.setFocusable(true);card.setClickable(true);card.setContentDescription("Add "+label+" widget, "+spans[0]+" by "+spans[1]);
        card.setOnClickListener(v->selection.choose(info));card.setOnLongClickListener(v->{selection.choose(info);return true;});
        String key=info.provider+"@"+info.getProfile();Drawable preview=previews.get(key);if(preview==null){preview=preview(info,spans);previews.put(key,preview);}ImageView image=new ImageView(context);image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);image.setImageDrawable(preview);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int height=Ui.dp(context,Math.max(80,Math.min(250,spans[1]*70)));card.addView(image,new LinearLayout.LayoutParams(-1,height));
        TextView title=Ui.text(context,label,14,Ui.TEXT);title.setGravity(Gravity.CENTER);card.addView(title);
        TextView size=Ui.text(context,spans[0]+" × "+spans[1],12,Ui.MUTED);size.setGravity(Gravity.CENTER);card.addView(size);
        if(info.previewImage==0&&(Build.VERSION.SDK_INT<31||info.previewLayout==0)){TextView unavailable=Ui.text(context,"App icon · no widget preview supplied",12,Ui.MUTED);unavailable.setGravity(Gravity.CENTER);card.addView(unavailable);}
        return card;
    }
    private Drawable preview(AppWidgetProviderInfo info,int[] span){
        if(Build.VERSION.SDK_INT>=31&&info.previewLayout!=0){
            // Snapshot a static RemoteViews preview. Its buttons never receive taps or PendingIntents.
            try{
                int w=Ui.dp(context,Math.min(400,span[0]*85)),h=Ui.dp(context,Math.min(360,span[1]*100));
                FrameLayout frame=new FrameLayout(context);View view=new RemoteViews(info.provider.getPackageName(),info.previewLayout).apply(context,frame);frame.addView(view,new FrameLayout.LayoutParams(-1,-1));
                frame.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));frame.layout(0,0,w,h);
                Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);frame.draw(new Canvas(bitmap));return new BitmapDrawable(context.getResources(),bitmap);
            }catch(RuntimeException invalidPreview){android.util.Log.w("NthWidgets","Preview layout unavailable for "+info.provider,invalidPreview);}
        }
        try{Drawable preview=info.loadPreviewImage(context,context.getResources().getDisplayMetrics().densityDpi);if(preview!=null)return preview;}catch(RuntimeException ignored){}
        try{Drawable icon=info.loadIcon(context,context.getResources().getDisplayMetrics().densityDpi);if(icon!=null)return icon;}catch(RuntimeException ignored){}
        return context.getPackageManager().getDefaultActivityIcon();
    }
}
