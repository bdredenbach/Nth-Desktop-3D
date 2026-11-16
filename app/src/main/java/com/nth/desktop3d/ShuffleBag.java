package com.nth.desktop3d;

import java.util.*;

/** A selected-scene cycle, with no immediate repeats across cycle boundaries. */
final class ShuffleBag {
    static final class Draw {final int scene;final List<Integer> remaining;Draw(int scene,List<Integer> remaining){this.scene=scene;this.remaining=remaining;}}
    static Draw next(int mask,int count,int current,List<Integer> saved,Random random){
        if(count<1||count>30||(mask&((1<<count)-1))==0)throw new IllegalArgumentException("Select at least one scene");
        List<Integer> bag=new ArrayList<>();for(int scene:saved)if(scene>=0&&scene<count&&(mask&(1<<scene))!=0&&!bag.contains(scene))bag.add(scene);
        if(bag.size()==1&&bag.get(0)==current&&Integer.bitCount(mask&((1<<count)-1))>1)bag.clear();
        if(bag.isEmpty()){for(int i=0;i<count;i++)if((mask&(1<<i))!=0)bag.add(i);Collections.shuffle(bag,random);}
        if(bag.size()>1&&bag.get(0)==current){for(int i=1;i<bag.size();i++)if(bag.get(i)!=current){Collections.swap(bag,0,i);break;}}
        return new Draw(bag.remove(0),bag);
    }
    private ShuffleBag(){}
}
