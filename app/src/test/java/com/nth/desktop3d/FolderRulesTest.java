package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class FolderRulesTest {
    @Test public void referenceHasTwentyDistinctNonoverlappingFolderCells(){
        assertEquals(20,FolderRules.NAMES.length);java.util.Set<String> names=new java.util.HashSet<>(),cells=new java.util.HashSet<>();
        for(int i=0;i<20;i++){assertTrue(names.add(FolderRules.NAMES[i]));assertTrue(FolderRules.X[i]>=0&&FolderRules.X[i]<5);assertTrue(FolderRules.Y[i]>=2&&FolderRules.Y[i]<10);assertTrue(cells.add(FolderRules.X[i]+":"+FolderRules.Y[i]));}
        assertEquals("Stores",FolderRules.NAMES[0]);assertEquals("Utilities",FolderRules.NAMES[19]);assertEquals(2,FolderRules.X[0]);
    }
    @Test public void specificHintsWinOverBroadAndroidCategories(){
        assertEquals("DnD",group("5e Companion App","com.example.companion",0));assertEquals("Emulation",group("Dolphin Emulator","org.dolphinemu.dolphinemu",0));assertEquals("AI",group("Gemini","com.google.android.apps.bard",7));assertEquals("Browser",group("Chrome","com.android.chrome",-1));assertEquals("Readers",group("Audible","com.audible.application",1));assertEquals("Finances",group("Capital One","com.capitalone.mobile",7));assertEquals("Shopping",group("Chewy","com.chewy.mobile",-1));
    }
    @Test public void declaredCategoriesAndUnknownAppsAlwaysHaveAFolder(){assertEquals("Games",group("Unfamiliar game","example.game",0));assertEquals("Work",group("Unfamiliar tool","example.tool",7));assertEquals("Utilities",group("Unfamiliar app","example.app",-1));assertEquals("Utilities",group("","",999));}
    private String group(String name,String pkg,int category){return FolderRules.NAMES[FolderRules.classify(name,pkg,category)];}
}
