package com.nth.desktop3d;
import org.junit.Test;
import static org.junit.Assert.*;
public class SettingsCommandsTest {
    @Test public void spokenNumbersMapToGridCommands(){SettingsCommands.Command c=SettingsCommands.parse("Please set grid to five columns.");assertEquals("grid_columns",c.id);assertEquals(5,c.value.intValue());}
    @Test public void explanationDoesNotModifySettings(){assertNull(SettingsCommands.parse("How do I change the grid to five columns?"));assertNull(SettingsCommands.parse("Explain reduced motion"));}
    @Test public void toggleAndNavigationAreDistinct(){assertEquals(0,SettingsCommands.parse("Turn off tilt").value.intValue());assertEquals(1,SettingsCommands.parse("Show dock").value.intValue());assertEquals("folders",SettingsCommands.parse("Show folder settings").page);}
    @Test public void overflowDoesNotCrash(){assertNull(SettingsCommands.parse("set icon size 999999999999999999999"));}
    @Test public void spacingAndUndoAreUnderstood(){assertEquals("undo",SettingsCommands.parse("Please  undo that!").id);assertEquals(128,SettingsCommands.parse("set grid row height to 128").value.intValue());assertEquals("grid_row_dp",SettingsCommands.parse("set grid row height to 128").id);}
    @Test public void unrelatedConversationDoesNotExecute(){assertNull(SettingsCommands.parse("I like larger icons"));assertNull(SettingsCommands.parse("what is an icon size of 100"));}
    @org.junit.Test public void cardsCanBeControlledWithoutExecutingQuestions(){
        org.junit.Assert.assertEquals("card_weather",SettingsCommands.parse("turn off weather card").id);
        org.junit.Assert.assertEquals(Integer.valueOf(0),SettingsCommands.parse("turn off weather card").value);
        org.junit.Assert.assertEquals("cards",SettingsCommands.parse("open card settings").page);
        org.junit.Assert.assertNull(SettingsCommands.parse("what does the weather card do?"));
    }
}
