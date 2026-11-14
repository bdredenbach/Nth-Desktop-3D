package com.nth.desktop3d;

/** Opt-in system actions only. No window contents or user events are read. */
public final class GestureAccess extends android.accessibilityservice.AccessibilityService {
    private static java.lang.ref.WeakReference<GestureAccess> active=new java.lang.ref.WeakReference<>(null);
    @Override protected void onServiceConnected(){active=new java.lang.ref.WeakReference<>(this);}
    @Override public void onDestroy(){if(active.get()==this)active.clear();super.onDestroy();}
    @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event){}
    @Override public void onInterrupt(){}
    static boolean perform(int action){GestureAccess service=active.get();return service!=null&&service.performGlobalAction(action);}
    static boolean available(){return active.get()!=null;}
}
