package com.nth.desktop3d;

/** Stable action indices preserve the existing three gesture assignments. */
public final class GestureAssignments {
    static final String[] ACTIONS={"App drawer","Settings","Nth Nota AI","Device controls","None","Expand notifications","Expand quick settings","Recent apps","Screen lock","Launcher search","Nth Feed","System assistant","Toggle notification bar","Toggle dock","Desktop previews","Scene studio","Open app","Open shortcut","Previous desktop","Next desktop"};
    static final String[][] GESTURES={{"gesture_home","Home button"},{"gesture_up","Swipe up"},{"gesture_down","Swipe down"},{"gesture_left","Swipe left"},{"gesture_right","Swipe right"},{"gesture_double","Double tap"},{"gesture_double_up","Double tap + swipe up"},{"gesture_double_down","Double tap + swipe down"},{"gesture_two_up","Two finger swipe up"},{"gesture_two_down","Two finger swipe down"},{"gesture_pinch_in","Pinch in"},{"gesture_pinch_out","Pinch out"},{"gesture_rotate_ccw","Two finger rotation (counterclockwise)"},{"gesture_rotate_cw","Two finger rotation (clockwise)"}};
    public static String twoFinger(float ratio,float angle,float dx,float dy,float threshold){if(Math.abs(angle)>28)return angle>0?"gesture_rotate_cw":"gesture_rotate_ccw";if(ratio<.78f)return "gesture_pinch_in";if(ratio>1.22f)return "gesture_pinch_out";if(Math.abs(dy)>threshold&&Math.abs(dy)>Math.abs(dx)*1.4f)return dy<0?"gesture_two_up":"gesture_two_down";return null;}
}
