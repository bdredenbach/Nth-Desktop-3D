package com.nth.desktop3d;
import org.junit.Test;
import static org.junit.Assert.*;
public class GestureAssignmentsTest {
    @Test public void parallelFingerMovementStaysASwipe(){assertEquals("gesture_two_up",GestureAssignments.twoFinger(1,0,3,-100,55));assertEquals("gesture_two_down",GestureAssignments.twoFinger(1,0,3,100,55));}
    @Test public void pinchingAndRotationAreSeparate(){assertEquals("gesture_pinch_in",GestureAssignments.twoFinger(.6f,0,0,0,55));assertEquals("gesture_pinch_out",GestureAssignments.twoFinger(1.5f,0,0,0,55));assertEquals("gesture_rotate_ccw",GestureAssignments.twoFinger(1,-45,0,0,55));assertEquals("gesture_rotate_cw",GestureAssignments.twoFinger(1,45,0,0,55));}
    @Test public void smallMotionDoesNotTrigger(){assertNull(GestureAssignments.twoFinger(.98f,3,2,4,55));}
    @Test public void folderAndMenuChoicesRemainIndependent(){assertEquals("choice:folder_animation",SettingsCommands.parse("change folder opening animation").page);assertEquals("choice:menu_animation",SettingsCommands.parse("change menu opening animation").page);assertNull(SettingsCommands.parse("How do I change folder opening animation?"));}
    @Test public void shortFlickNeedsSpeedAndHorizontalIntent(){assertTrue(GestureAssignments.horizontalSwipe(-36,2,-900,8,55,600));assertFalse(GestureAssignments.horizontalSwipe(-36,2,-150,8,55,600));assertFalse(GestureAssignments.horizontalSwipe(8,2,1200,8,55,600));assertFalse(GestureAssignments.horizontalSwipe(36,40,1200,8,55,600));assertTrue(GestureAssignments.horizontalSwipe(90,2,100,8,55,600));}
}
