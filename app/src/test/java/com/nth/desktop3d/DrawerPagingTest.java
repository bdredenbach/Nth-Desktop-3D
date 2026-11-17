package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class DrawerPagingTest {
    @Test public void viewportCapacityIncludesOnlyWholeRows(){
        assertEquals(9,DrawerPaging.capacity(3,416,132,10));
        assertEquals(6,DrawerPaging.capacity(3,415,132,10));
        assertEquals(5,DrawerPaging.capacity(5,80,132,10));
    }
    @Test public void pagesCoverCatalogExactlyOnce(){
        for(int count=0;count<100;count++)for(int capacity=1;capacity<20;capacity++){
            int total=0;for(int page=0;page<DrawerPaging.pages(count,capacity);page++)total+=DrawerPaging.size(count,page,capacity);
            assertEquals(count,total);
        }
    }
    @Test public void shrinkingSearchAndEmptyResultsClampSafely(){
        assertEquals(0,DrawerPaging.clamp(12,0,9));assertEquals(0,DrawerPaging.clamp(12,2,9));
        assertEquals(2,DrawerPaging.clamp(12,20,9));assertEquals(2,DrawerPaging.size(20,2,9));
        assertEquals(0,DrawerPaging.size(0,0,9));assertEquals(1,DrawerPaging.pages(0,9));
    }
}
