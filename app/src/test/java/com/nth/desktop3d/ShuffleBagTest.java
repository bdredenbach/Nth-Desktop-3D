package com.nth.desktop3d;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShuffleBagTest {
    @Test public void aFullCycleVisitsEachSelectedSceneExactlyOnce(){List<Integer> bag=Collections.emptyList();Random random=new Random(29);int current=-1;for(int cycle=0;cycle<100;cycle++){Set<Integer> seen=new HashSet<>();for(int i=0;i<4;i++){ShuffleBag.Draw draw=ShuffleBag.next((1<<1)|(1<<3)|(1<<5)|(1<<9),11,current,bag,random);assertNotEquals(current,draw.scene);assertTrue(seen.add(draw.scene));current=draw.scene;bag=draw.remaining;}assertTrue(bag.isEmpty());}}
    @Test public void aNewCycleAvoidsRepeatingItsPreviousScene(){for(int seed=0;seed<100;seed++)assertNotEquals(2,ShuffleBag.next(7,3,2,Collections.emptyList(),new Random(seed)).scene);}
    @Test public void remainingOrderSurvivesAControllerRestart(){ShuffleBag.Draw first=ShuffleBag.next(15,4,-1,Collections.emptyList(),new Random(8));List<Integer> remaining=new ArrayList<>(first.remaining);ShuffleBag.Draw next=ShuffleBag.next(15,4,first.scene,remaining,new Random(800));assertEquals((int)remaining.get(0),next.scene);assertEquals(3,remaining.size());}
    @Test public void corruptAndUnselectedEntriesCannotEscapeTheSelection(){ShuffleBag.Draw draw=ShuffleBag.next((1<<2)|(1<<7),11,0,Arrays.asList(-1,5,7,7,500),new Random(0));assertEquals(7,draw.scene);assertTrue(draw.remaining.isEmpty());}
    @Test public void manualSelectionCannotCauseAnImmediateRepeat(){assertNotEquals(3,ShuffleBag.next((1<<3)|(1<<5),11,3,Collections.singletonList(3),new Random(2)).scene);}
    @Test public void oneSelectedSceneIsStable(){ShuffleBag.Draw draw=ShuffleBag.next(1<<4,11,4,Collections.emptyList(),new Random(2));assertEquals(4,draw.scene);assertTrue(draw.remaining.isEmpty());}
    @Test(expected=IllegalArgumentException.class) public void anEmptySelectionIsRejected(){ShuffleBag.next(0,11,0,Collections.emptyList(),new Random());}
}
