package com.nth.desktop3d;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AppCatalog {
    public static final class Entry {
        public final String key, label;
        public final ComponentName component;
        public final UserHandle user;
        public final Drawable icon;
        Entry(String key, String label, ComponentName component, UserHandle user, Drawable icon) {
            this.key = key; this.label = label; this.component = component; this.user = user; this.icon = icon;
        }
    }
    private final Context context;
    public final LauncherApps launcher;
    public final List<Entry> entries = new ArrayList<>();
    private final Map<String, Entry> byKey = new HashMap<>();
    public AppCatalog(Context context) { this.context = context; launcher = context.getSystemService(LauncherApps.class); }
    public synchronized List<Entry> load() {
        List<Entry> next = new ArrayList<>();
        for (String[] b : new String[][]{{"cards", "Crystal cards"}, {"nota", "Nth Nota"}, {"notes", "Nth Notes"}, {"calculator", "Calculator"}, {"calendar", "Calendar"}, {"settings", "3D settings"}, {"modes", "Scene studio"}, {"about", "About Nth Desktop 3D"}})
            next.add(new Entry("builtin:" + b[0], b[1], null, Process.myUserHandle(), null));
        UserManager users = context.getSystemService(UserManager.class);
        for (UserHandle user : launcher.getProfiles()) {
            try {
                for (LauncherActivityInfo info : launcher.getActivityList(null, user)) {
                    if (info.getComponentName().getPackageName().equals(context.getPackageName())) continue;
                    String key = info.getComponentName().flattenToString() + "@" + users.getSerialNumberForUser(user);
                    Drawable icon;
                    try { icon = info.getBadgedIcon(context.getResources().getDisplayMetrics().densityDpi); }
                    catch (RuntimeException ex) { icon = context.getPackageManager().getDefaultActivityIcon(); }
                    next.add(new Entry(key, info.getLabel().toString(), info.getComponentName(), user, icon));
                }
            } catch (SecurityException | IllegalStateException ignored) { /* Locked/unavailable profile. */ }
        }
        Collator collator = Collator.getInstance(); next.sort((a,b) -> collator.compare(a.label, b.label));
        return next;
    }
    public void apply(List<Entry> next) { entries.clear(); entries.addAll(next); byKey.clear(); for (Entry e : next) byKey.put(e.key, e); }
    PinnedShortcuts shortcuts;
    Drawable icon(LauncherStore.Item item){if(PinnedShortcuts.isShortcut(item))return shortcuts==null?null:shortcuts.icon(item);Entry entry=find(item.key);return entry==null?null:entry.icon;}
    public Entry find(String key) { return byKey.get(key); }
    public List<Entry> filter(String text, boolean builtins) {
        String q = text.toLowerCase(java.util.Locale.ROOT).trim(); List<Entry> out = new ArrayList<>();
        for (Entry e : entries) if ((!builtins || e.component == null) && e.label.toLowerCase(java.util.Locale.ROOT).contains(q)) out.add(e);
        return out;
    }
}
