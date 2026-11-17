package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class LiveQuestionPolicyTest {
    @Test public void billsQuestionRequiresALiveSchedule(){assertTrue(LiveQuestionPolicy.nextGame("Who does the Buffalo Bills play next?"));assertFalse(LiveQuestionPolicy.settings("Who do the Buffalo Bills play next?","Scene lighting strength: 100.\nTilt: On."));}
    @Test public void currentFactsCannotBecomeCreativeFallback(){assertTrue(LiveQuestionPolicy.current("What is the current score?"));assertFalse(LiveQuestionPolicy.creative("Write about the latest news"));assertTrue(LiveQuestionPolicy.needsSpecialSource("What is the weather in Paris?"));}
    @Test public void ordinaryWritingStillWorks(){assertTrue(LiveQuestionPolicy.creative("Write a short poem about trees"));assertFalse(LiveQuestionPolicy.nextGame("What are the Buffalo Bills?"));}
    @Test public void launcherHelpRetainsItsOwnContext(){assertTrue(LiveQuestionPolicy.settings("Explain scene lighting","Scene lighting strength: 100."));assertTrue(LiveQuestionPolicy.settings("How do I change desktop transition?",""));assertFalse(LiveQuestionPolicy.settings("What causes a rainbow?","Scene: 4."));}
    @Test public void queryCleanupAndUnicodeAreSafe(){assertEquals("president of France",LiveQuestionPolicy.searchTerms("Who is the president of France?"));assertEquals("cafe nba",LiveQuestionPolicy.words("Café NBA!"));assertEquals("",LiveQuestionPolicy.words(null));}
    @Test public void onlyHttpsSourceLinksAreClickable(){assertTrue(LiveAnswer.safeLink("https://www.espn.com/nfl/game/_/gameId/123"));assertFalse(LiveAnswer.safeLink("javascript:alert(1)"));assertFalse(LiveAnswer.safeLink("http://example.com"));assertFalse(LiveAnswer.safeLink("https://user:secret@example.com/"));assertFalse(LiveAnswer.safeLink("file:///tmp/file"));}
}
