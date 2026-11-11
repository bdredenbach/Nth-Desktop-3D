package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class PanelGeometryTest {
    @Test public void keyboardKeepsSearchInsideVisibleWindow(){
        PanelGeometry.Bounds b=PanelGeometry.place(420,1000,0,40,0,400,720,820,12,0);
        assertEquals(52,b.top);assertTrue(b.top+b.height<=600);assertEquals(396,b.width);
    }
    @Test public void landscapeCentersBetweenAsymmetricCutoutAndEdge(){
        PanelGeometry.Bounds b=PanelGeometry.place(1060,420,80,24,0,24,720,820,12,0);
        assertEquals(210,b.left);assertTrue(b.left>=92);assertTrue(b.left+b.width<=1048);
        assertTrue(b.top>=36);assertTrue(b.top+b.height<=384);
    }
    @Test public void flyoutClearsDockEvenOnShortLandscapeWindow(){
        PanelGeometry.Bounds b=PanelGeometry.place(1060,420,80,24,0,24,520,520,12,84);
        assertTrue(b.top>=36);assertEquals(312,b.top+b.height);assertTrue(b.height>0);
    }
    @Test public void closingKeyboardRestoresFullPanelWithoutOffset(){
        PanelGeometry.Bounds b=PanelGeometry.place(420,1000,0,40,0,24,720,820,12,0);
        assertEquals(98,b.top);assertEquals(820,b.height);assertEquals(12,b.left);
    }
}
