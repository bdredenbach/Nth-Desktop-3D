package com.nth.desktop3d;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class DesktopGridTest {
    private DesktopGrid.Cell icon(String id,int x,int y){return new DesktopGrid.Cell(id,false,x,y,1,1);}
    private DesktopGrid.Cell widget(String id,int x,int y,int w,int h){return new DesktopGrid.Cell(id,true,x,y,w,h);}
    private void valid(List<DesktopGrid.Cell> cells,int columns,int rows){
        assertNotNull(cells);
        for(int i=0;i<cells.size();i++){
            assertTrue(DesktopGrid.fits(cells.get(i),columns,rows));
            for(int j=i+1;j<cells.size();j++)assertFalse(cells.get(i).id+" overlaps "+cells.get(j).id,cells.get(i).overlaps(cells.get(j)));
        }
    }
    @Test public void legacyWidgetAppAndFolderOverlapIsRepaired(){
        List<DesktopGrid.Cell> cells=DesktopGrid.restore(Arrays.asList(icon("app",0,0),icon("folder",0,0),widget("weather",0,0,2,2)),4,5);
        valid(cells,4,5);assertEquals(3,cells.size());assertEquals(0,DesktopGrid.find(cells,"weather").x);
    }
    @Test public void widgetDragRelocatesAppsAndFoldersWithoutChangingOtherWidgets(){
        List<DesktopGrid.Cell> before=Arrays.asList(widget("clock",0,0,2,2),widget("weather",2,0,2,2),icon("app",0,2),icon("folder",1,2));
        List<DesktopGrid.Cell> after=DesktopGrid.move(before,widget("clock",0,2,2,2),4,5);
        valid(after,4,5);assertEquals(2,DesktopGrid.find(after,"weather").x);assertEquals(0,DesktopGrid.find(after,"weather").y);
        assertEquals(0,before.get(0).y);assertEquals(2,before.get(2).y);
    }
    @Test public void widgetOnWidgetAndOffGridDropsAreRejectedWithoutMutation(){
        List<DesktopGrid.Cell> before=Arrays.asList(widget("one",0,0,2,2),widget("two",2,0,2,2));
        assertNull(DesktopGrid.move(before,widget("one",2,0,2,2),4,5));
        assertNull(DesktopGrid.move(before,widget("one",-1,0,2,2),4,5));
        assertNull(DesktopGrid.move(before,widget("one",3,4,2,2),4,5));
        valid(before,4,5);
    }
    @Test public void fullGridRejectsAnAdditionAndKeepsAllExistingItems(){
        List<DesktopGrid.Cell> full=new ArrayList<>();for(int y=0;y<2;y++)for(int x=0;x<3;x++)full.add(icon(x+":"+y,x,y));
        assertNull(DesktopGrid.move(full,widget("new",0,0,2,2),3,2));assertEquals(6,full.size());valid(full,3,2);
    }
    @Test public void resizeReservesTheEntireRectangleAndMovesDisplacedFolder(){
        List<DesktopGrid.Cell> before=Arrays.asList(widget("weather",0,0,2,2),icon("folder",2,0),icon("app",2,1));
        List<DesktopGrid.Cell> after=DesktopGrid.move(before,widget("weather",0,0,3,2),4,4);
        valid(after,4,4);assertEquals(3,DesktopGrid.find(after,"weather").w);assertEquals(3,after.size());
    }
    @Test public void geometryChangesAndRepeatedRestoreAreStable(){
        List<DesktopGrid.Cell> legacy=Arrays.asList(widget("wide",6,5,4,3),icon("a",6,5),icon("folder",6,5));
        List<DesktopGrid.Cell> first=DesktopGrid.restore(legacy,3,8),second=DesktopGrid.restore(first,3,8);
        valid(first,3,8);valid(second,3,8);
        for(DesktopGrid.Cell cell:first){DesktopGrid.Cell copy=DesktopGrid.find(second,cell.id);assertEquals(cell.x,copy.x);assertEquals(cell.y,copy.y);}
    }
    @Test public void allLegalWidgetTargetsPreserveOccupancyAcrossGridSizes(){
        int checked=0;
        for(int columns=3;columns<=8;columns++)for(int rows=3;rows<=9;rows++){
            List<DesktopGrid.Cell> desired=new ArrayList<>();desired.add(widget("moving",0,0,2,2));
            desired.add(widget("fixed",columns-1,rows-1,1,1));
            for(int i=0;i<Math.min(12,columns*rows-5);i++)desired.add(icon("shortcut"+i,i%columns,i/columns));
            List<DesktopGrid.Cell> current=DesktopGrid.restore(desired,columns,rows);valid(current,columns,rows);
            for(int h=1;h<=Math.min(3,rows);h++)for(int w=1;w<=Math.min(3,columns);w++)for(int y=0;y+h<=rows;y++)for(int x=0;x+w<=columns;x++){
                List<DesktopGrid.Cell> planned=DesktopGrid.move(current,widget("moving",x,y,w,h),columns,rows);
                if(planned!=null){valid(planned,columns,rows);assertEquals(current.size(),planned.size());}
                valid(current,columns,rows);checked++;
            }
        }
        assertTrue(checked>5000);
    }
}
