package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocalSearchTest {
    @Test public void exactNamesRankBeforeDescriptions(){assertEquals(0,LocalSearch.rank("icon shape","Icon shape","Shell"));assertTrue(LocalSearch.rank("icon","Icon shape","")<LocalSearch.rank("icon","Look & feel","App icon shapes"));}
    @Test public void tokensCanMatchTitleAndHelpInAnyOrder(){assertTrue(LocalSearch.matches("lighting strength","Scene strength","Lighting intensity"));assertFalse(LocalSearch.matches("lighting missing","Scene strength","Lighting intensity"));}
    @Test public void accentsCaseWhitespaceAndIdsNormalize(){assertTrue(LocalSearch.matches("  CAFE  ","Café",""));assertTrue(LocalSearch.matches("scene light","scene_light_strength",""));assertEquals("nth notes",LocalSearch.fold("  Nth   Notes "));}
    @Test public void excerptsRetainTheMatchedTailWithoutChangingTheNote(){String text=new String(new char[500]).replace((char)0,'x')+"needle";String copy=text;assertTrue(LocalSearch.excerpt(text,"needle").contains("needle"));assertEquals(copy,text);assertTrue(LocalSearch.excerpt(text,"missing").length()<=180);}
    @Test public void emptyAndNullInputsAreSafe(){assertEquals("",LocalSearch.fold(null));assertEquals("",LocalSearch.excerpt(null,"x"));assertTrue(LocalSearch.matches("","Anything",""));}
}
