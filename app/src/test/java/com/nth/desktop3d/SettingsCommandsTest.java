package com.nth.desktop3d;
import org.junit.Test;
import static org.junit.Assert.*;
public class SettingsCommandsTest {
    @Test public void spokenNumbersMapToGridCommands(){SettingsCommands.Command c=SettingsCommands.parse("Please set grid to five columns.");assertEquals("grid_columns",c.id);assertEquals(5,c.value.intValue());}
    @Test public void explanationDoesNotModifySettings(){assertNull(SettingsCommands.parse("How do I change the grid to five columns?"));assertNull(SettingsCommands.parse("Explain reduced motion"));}
    @Test public void toggleAndNavigationAreDistinct(){assertEquals(0,SettingsCommands.parse("Turn off tilt").value.intValue());assertEquals(1,SettingsCommands.parse("Show dock").value.intValue());assertEquals("folders",SettingsCommands.parse("Show folder settings").page);}
    @Test public void overflowDoesNotCrash(){assertNull(SettingsCommands.parse("set icon size 999999999999999999999"));}
    @Test public void spacingAndUndoAreUnderstood(){assertEquals("undo",SettingsCommands.parse("Please  undo that!").id);assertEquals(128,SettingsCommands.parse("set grid row height to 128").value.intValue());assertEquals("grid_row_dp",SettingsCommands.parse("set grid row height to 128").id);}
    @Test public void conversationalMentionsDoNotRunLauncherActions(){assertNull(SettingsCommands.parse("Write a poem about turning off tilt"));assertNull(SettingsCommands.parse("Can you explain why I should turn off tilt?"));assertNull(SettingsCommands.parse("Make a story about how to turn off tilt"));assertNull(SettingsCommands.parse("Compare phones that let me set icon size to 100"));}
    @Test public void unrelatedConversationDoesNotExecute(){assertNull(SettingsCommands.parse("I like larger icons"));assertNull(SettingsCommands.parse("what is an icon size of 100"));}
    @Test public void recordedCommandsOpenTheRelevantChoicesWithoutGuessing(){
        assertEquals("choice:folder_shape",SettingsCommands.parse("change folder shape").page);
        assertEquals("choice:folder_shape",SettingsCommands.parse("change folder icon").page);
        assertEquals("choice:icon_shape",SettingsCommands.parse("change app icon shape").page);
        assertEquals("home",SettingsCommands.parse("change icon size").page);
        assertEquals("choice:transition",SettingsCommands.parse("change transition effect").page);
        assertEquals("choice:transition",SettingsCommands.parse("transition animations").page);
    }
    @Test public void recordedSizeRequestsStayInTheirScope(){
        assertEquals(Integer.valueOf(Integer.MIN_VALUE+1),SettingsCommands.parse("change icon size large").value);
        assertEquals(Integer.valueOf(Integer.MIN_VALUE+2),SettingsCommands.parse("change icon size smaller").value);
        assertEquals("folder_icon_size",SettingsCommands.parse("make folder icons larger").id);
        assertEquals("drawer_icon_size",SettingsCommands.parse("set app drawer icon size to 110").id);
        assertEquals("folder_columns",SettingsCommands.parse("set folder columns to four").id);
    }
    @Test public void shapesAndEffectsUseExplicitChoices(){
        assertEquals(Integer.valueOf(1),SettingsCommands.parse("change folder shape to rounded square").value);
        assertEquals(Integer.valueOf(2),SettingsCommands.parse("change app icon shape to square").value);
        assertEquals(Integer.valueOf(8),SettingsCommands.parse("set transition to flip vertical").value);
        assertEquals("add_desktop",SettingsCommands.parse("add desktop").id);
        assertEquals("remove_desktop",SettingsCommands.parse("remove this desktop").id);
        assertNull(SettingsCommands.parse("How do I remove a desktop?"));
    }
    @org.junit.Test public void cardsCanBeControlledWithoutExecutingQuestions(){
        org.junit.Assert.assertEquals("card_weather",SettingsCommands.parse("turn off weather card").id);
        org.junit.Assert.assertEquals(Integer.valueOf(0),SettingsCommands.parse("turn off weather card").value);
        org.junit.Assert.assertEquals("cards",SettingsCommands.parse("open card settings").page);
        org.junit.Assert.assertNull(SettingsCommands.parse("what does the weather card do?"));
    }
    @org.junit.Test public void dockSpaceCommandsStayIndependent(){
        org.junit.Assert.assertEquals("dock_content",SettingsCommands.parse("enable dock spaces").id);
        org.junit.Assert.assertEquals(Integer.valueOf(0),SettingsCommands.parse("disable dock spaces").value);
        org.junit.Assert.assertEquals("show_dock",SettingsCommands.parse("enable dock").id);
    }
    @org.junit.Test public void sceneLightingCanBeComparedAndAdjusted(){
        SettingsCommands.Command off=SettingsCommands.parse("turn off scene lighting");org.junit.Assert.assertEquals("scene_lighting",off.id);org.junit.Assert.assertEquals(Integer.valueOf(0),off.value);
        SettingsCommands.Command strength=SettingsCommands.parse("set scene lighting strength to 40");org.junit.Assert.assertEquals("scene_light_strength",strength.id);org.junit.Assert.assertEquals(Integer.valueOf(40),strength.value);
    }
    @org.junit.Test public void sceneLightingNavigationFindsItsOwnControls(){org.junit.Assert.assertEquals("look",SettingsCommands.parse("open scene lighting").page);}
}
