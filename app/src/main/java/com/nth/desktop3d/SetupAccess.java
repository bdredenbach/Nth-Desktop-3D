package com.nth.desktop3d;

import android.app.role.RoleManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

/** Read Android's current grants; a cached service instance is not a permission state. */
final class SetupAccess {
    static boolean home(Context context){
        try{if(Build.VERSION.SDK_INT>=29){RoleManager roles=context.getSystemService(RoleManager.class);if(roles!=null&&roles.isRoleAvailable(RoleManager.ROLE_HOME))return roles.isRoleHeld(RoleManager.ROLE_HOME);}
            ResolveInfo resolved=context.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0);
            return resolved!=null&&resolved.activityInfo!=null&&context.getPackageName().equals(resolved.activityInfo.packageName);
        }catch(RuntimeException unavailable){return false;}
    }
    static boolean media(Context context){try{ComponentName expected=new ComponentName(context,MediaAccess.class);if(Build.VERSION.SDK_INT>=27)return context.getSystemService(android.app.NotificationManager.class).isNotificationListenerAccessGranted(expected);
        String enabled=android.provider.Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");if(enabled!=null)for(String raw:enabled.split(":"))if(expected.equals(ComponentName.unflattenFromString(raw)))return true;
        }catch(RuntimeException unavailable){}return false;}
    static boolean gestures(Context context){
        try{android.view.accessibility.AccessibilityManager manager=context.getSystemService(android.view.accessibility.AccessibilityManager.class);if(manager==null)return false;
            ComponentName expected=new ComponentName(context,GestureAccess.class);
            for(android.accessibilityservice.AccessibilityServiceInfo info:manager.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)){
                android.content.pm.ServiceInfo service=info.getResolveInfo()==null?null:info.getResolveInfo().serviceInfo;
                if(service!=null&&expected.equals(new ComponentName(service.packageName,service.name)))return true;
            }
        }catch(RuntimeException unavailable){}return false;
    }
    static boolean calendar(Context context){return context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED;}
    static boolean location(Context context){return context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    private SetupAccess(){}
}
