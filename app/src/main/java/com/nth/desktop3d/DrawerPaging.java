package com.nth.desktop3d;

/** Bounded pages for a drawer viewport; positions always refer to the full catalog. */
final class DrawerPaging {
    static int capacity(int columns,int height,int rowHeight,int spacing){
        return Math.max(1,columns)*Math.max(1,(Math.max(0,height)+Math.max(0,spacing))/Math.max(1,rowHeight+Math.max(0,spacing)));
    }
    static int pages(int count,int capacity){return Math.max(1,(Math.max(0,count)+Math.max(1,capacity)-1)/Math.max(1,capacity));}
    static int clamp(int page,int count,int capacity){return Math.max(0,Math.min(page,pages(count,capacity)-1));}
    static int size(int count,int page,int capacity){int cap=Math.max(1,capacity);return Math.max(0,Math.min(cap,count-clamp(page,count,cap)*cap));}
}
