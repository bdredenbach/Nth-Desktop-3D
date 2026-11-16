package com.nth.desktop3d;

/** A viewport fit, independent of icon size changes and Android view lifecycles. */
final class FittedGrid {
    final int columns,rows,pitch,capacity;final boolean fits;
    private FittedGrid(int c,int r,int p,int capacity,boolean fits){columns=c;rows=r;pitch=p;this.capacity=capacity;this.fits=fits;}
    static FittedGrid choose(int width,int height,int count,int minWidth,int minHeight,int maxPitch,int quantum){
        width=Math.max(1,width);height=Math.max(1,height);count=Math.max(1,count);minHeight=Math.max(1,minHeight);quantum=Math.max(1,quantum);
        int maxColumns=Math.max(3,Math.min(12,width/Math.max(1,minWidth))),maxRows=Math.max(1,height/minHeight),capacity=maxColumns*maxRows;
        if(count>capacity)return new FittedGrid(maxColumns,(count+maxColumns-1)/maxColumns,minHeight,capacity,false);
        FittedGrid best=null;double score=Double.MAX_VALUE;
        for(int c=3;c<=maxColumns;c++){
            int r=(count+c-1)/c;if(r>maxRows)continue;
            int pitch=Math.min(maxPitch,height/r);pitch=Math.max(minHeight,pitch/quantum*quantum);
            if(pitch*r>height)continue;double aspect=(width/(double)c)/pitch;
            double value=Math.abs(Math.log(aspect))+(c*r-count)/(double)count*.35;
            if(value<score){score=value;best=new FittedGrid(c,r,pitch,capacity,true);}
        }
        return best==null?new FittedGrid(maxColumns,(count+maxColumns-1)/maxColumns,minHeight,capacity,false):best;
    }
}
