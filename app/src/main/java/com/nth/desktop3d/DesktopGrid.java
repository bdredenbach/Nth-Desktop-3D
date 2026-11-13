package com.nth.desktop3d;

import java.util.ArrayList;
import java.util.List;

/** Pure cell occupancy policy shared by widgets, apps and folders. Plans never mutate input. */
public final class DesktopGrid {
    private DesktopGrid() {}
    public static final class Cell {
        public final String id;
        public final boolean widget;
        public final int x,y,w,h;
        public Cell(String id,boolean widget,int x,int y,int w,int h){this.id=id;this.widget=widget;this.x=x;this.y=y;this.w=w;this.h=h;}
        public Cell at(int x,int y){return new Cell(id,widget,x,y,w,h);}
        public boolean overlaps(Cell other){return x<other.x+other.w&&x+w>other.x&&y<other.y+other.h&&y+h>other.y;}
    }
    public static Cell find(List<Cell> cells,String id){for(Cell c:cells)if(c.id.equals(id))return c;return null;}
    public static boolean fits(Cell cell,int columns,int rows){return cell.w>0&&cell.h>0&&cell.x>=0&&cell.y>=0&&cell.x+cell.w<=columns&&cell.y+cell.h<=rows;}
    private static boolean free(Cell candidate,List<Cell> placed,int columns,int rows){
        if(!fits(candidate,columns,rows))return false;
        for(Cell other:placed)if(candidate.overlaps(other))return false;
        return true;
    }
    public static Cell nearest(Cell desired,List<Cell> placed,int columns,int rows){
        Cell best=null;long distance=Long.MAX_VALUE;
        for(int y=0;y+desired.h<=rows;y++)for(int x=0;x+desired.w<=columns;x++){
            Cell candidate=desired.at(x,y);
            if(!free(candidate,placed,columns,rows))continue;
            long dx=(long)x-desired.x,dy=(long)y-desired.y,d=dx*dx+dy*dy;
            if(d<distance){best=candidate;distance=d;}
        }
        return best;
    }
    /** Repairs legacy overlap and geometry changes, reserving complete widget rectangles first. */
    public static List<Cell> restore(List<Cell> desired,int columns,int rows){
        List<Cell> result=new ArrayList<>();
        for(int pass=0;pass<2;pass++)for(Cell cell:desired){
            if(cell.widget!=(pass==0))continue;
            Cell bounded=new Cell(cell.id,cell.widget,cell.x,cell.y,Math.min(columns,Math.max(1,cell.w)),Math.min(rows,Math.max(1,cell.h)));
            Cell position=nearest(bounded,result,columns,rows);
            if(position==null)return null;
            result.add(position);
        }
        return result;
    }
    /** Keeps other widgets fixed; displaced apps/folders move to the nearest vacant cells. */
    public static List<Cell> move(List<Cell> current,Cell target,int columns,int rows){
        if(!fits(target,columns,rows))return null;
        List<Cell> result=new ArrayList<>(),displaced=new ArrayList<>();
        for(Cell cell:current)if(!cell.id.equals(target.id)&&cell.widget){
            if(target.overlaps(cell))return null;
            result.add(cell);
        }
        result.add(target);
        for(Cell cell:current)if(!cell.widget&&!cell.id.equals(target.id)){
            if(free(cell,result,columns,rows))result.add(cell);else displaced.add(cell);
        }
        for(Cell cell:displaced){
            Cell position=nearest(cell,result,columns,rows);
            if(position==null)return null;
            result.add(position);
        }
        return result;
    }
}
