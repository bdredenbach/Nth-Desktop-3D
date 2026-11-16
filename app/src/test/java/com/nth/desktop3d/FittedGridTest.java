package com.nth.desktop3d;
import org.junit.Test;
import static org.junit.Assert.*;

public class FittedGridTest {
    @Test public void twentyFoldersFitAboveAPortraitDock(){FittedGrid grid=FittedGrid.choose(372,702,20,80,104,144,4);assertTrue(grid.fits);assertEquals(4,grid.columns);assertEquals(5,grid.rows);assertTrue(grid.pitch*grid.rows<=702);}
    @Test public void wideShortDesktopUsesItsWidth(){FittedGrid grid=FittedGrid.choose(826,247,20,80,104,144,4);assertTrue(grid.fits);assertEquals(10,grid.columns);assertEquals(2,grid.rows);}
    @Test public void constrainedViewportReportsCapacityForAllocation(){FittedGrid grid=FittedGrid.choose(826,148,20,80,104,144,4);assertFalse(grid.fits);assertEquals(10,grid.capacity);FittedGrid page=FittedGrid.choose(826,148,grid.capacity,80,104,144,4);assertTrue(page.fits);assertEquals(1,page.rows);}
    @Test public void fittedLayoutsKeepEveryCellWithinTheUsableViewport(){for(int w=280;w<=1600;w+=37)for(int h=104;h<=1000;h+=31)for(int count=1;count<=60;count++){FittedGrid grid=FittedGrid.choose(w,h,count,80,104,144,4);if(grid.fits){assertTrue(grid.columns>=3&&grid.columns<=12);assertTrue(grid.columns*grid.rows>=count);assertTrue(grid.rows*grid.pitch<=h);assertTrue(grid.pitch>=104);}else assertTrue(grid.capacity>=1);}}
}
