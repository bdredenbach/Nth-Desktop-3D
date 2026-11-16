package com.nth.desktop3d;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CardLayoutTest {
    @Test public void onlyRealCrystalCardTypesAreRecognized(){assertEquals(8,CardLayout.TYPES.length);for(String type:CardLayout.TYPES){assertEquals(type,CardLayout.type(CardLayout.key(type)));assertTrue(CardLayout.height(type)>=2&&CardLayout.height(type)<=3);}assertNull(CardLayout.type("builtin:notes"));assertNull(CardLayout.type("card:unknown"));assertNull(CardLayout.type(null));}
    @Test public void aCardReservesItsWholeRectangleAgainstWidgets(){List<DesktopGrid.Cell> cells=Arrays.asList(new DesktopGrid.Cell("card:a",true,0,0,2,2),new DesktopGrid.Cell("widget:1",true,2,0,2,2),new DesktopGrid.Cell("app",false,0,2,1,1));assertNull(DesktopGrid.move(cells,new DesktopGrid.Cell("card:a",true,0,0,3,2),4,8));assertEquals(2,cells.get(0).w);}
    @Test public void expandingACardDisplacesAppsWithoutDroppingThem(){List<DesktopGrid.Cell> cells=Arrays.asList(new DesktopGrid.Cell("card:a",true,0,0,2,2),new DesktopGrid.Cell("app",false,2,0,1,1));List<DesktopGrid.Cell> plan=DesktopGrid.move(cells,new DesktopGrid.Cell("card:a",true,0,0,3,2),4,8);assertNotNull(plan);assertEquals(2,plan.size());assertFalse(DesktopGrid.find(plan,"app").overlaps(DesktopGrid.find(plan,"card:a")));assertEquals(2,cells.get(1).x);}
}
