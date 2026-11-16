package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class PresetNamesTest {
    @Test public void namesCanUseNaturalLabelsAndEmoji(){assertEquals("Aurora desk 🌌",PresetNames.checked("  Aurora desk 🌌  "));assertTrue(PresetNames.same("Lava desk","LAVA DESK"));}
    @Test public void labelsNeverBecomePathIdentifiers(){assertEquals("../Work / Play",PresetNames.checked("../Work / Play"));assertFalse(PresetNames.identifier("../Work / Play"));assertTrue(PresetNames.identifier("2cba918e-aaac-430b-8982-f6fd6cb7947d"));}
    @Test public void unicodeLengthCountsCharactersRatherThanSurrogateHalves(){assertEquals(80,PresetNames.checked("🌌".repeat(40)).length());reject("🌌".repeat(41));}
    @Test public void emptyAndMultilineNamesAreRejected(){reject("");reject("  ");reject(null);reject("Home\nWork");reject("Home\u0000Work");}
    private void reject(String input){try{PresetNames.checked(input);fail("Name was accepted: "+input);}catch(IllegalArgumentException expected){assertNotNull(expected.getMessage());}}
}
