package com.nth.desktop3d;

import android.content.Context;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Fit short card content, keep long content readable and fully reachable. */
@android.annotation.SuppressLint("ViewConstructor")
final class AdaptiveCardScroll extends ScrollView {
    private static final float[] DENSITY={1f,.92f,.86f};
    private final IdentityHashMap<View,Style> styles=new IdentityHashMap<>();
    private int profile,lastWidth=-1,lastHeight=-1,lastData;
    private static final class Style {
        final int left,top,right,bottom,height;final float sp;
        Style(View view){
            left=view.getPaddingLeft();top=view.getPaddingTop();right=view.getPaddingRight();bottom=view.getPaddingBottom();
            height=view.getLayoutParams()==null?-2:view.getLayoutParams().height;
            sp=view instanceof TextView?((TextView)view).getTextSize()/view.getResources().getDisplayMetrics().scaledDensity:0;
        }
    }
    AdaptiveCardScroll(Context context){
        super(context);setFillViewport(true);setVerticalScrollBarEnabled(true);setScrollbarFadingEnabled(false);
        setClipToPadding(false);setContentDescription("Card content. Scroll to read more when needed.");
    }
    int profile(){return profile;}
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        if(getChildCount()==0){super.onMeasure(widthSpec,heightSpec);return;}
        int width=Math.max(1,MeasureSpec.getSize(widthSpec)-getPaddingLeft()-getPaddingRight());
        int height=Math.max(1,MeasureSpec.getSize(heightSpec)-getPaddingTop()-getPaddingBottom());
        View content=getChildAt(0);Set<View> live=Collections.newSetFromMap(new IdentityHashMap<>());
        capture(content,live);styles.keySet().retainAll(live);
        int data=1;for(View view:live){data+=31*System.identityHashCode(view)+view.getVisibility();if(view instanceof TextView)data+=((TextView)view).getText().toString().hashCode();if(view instanceof ImageView)data+=System.identityHashCode(((ImageView)view).getDrawable());}
        boolean changed=width!=lastWidth||height!=lastHeight||data!=lastData;lastWidth=width;lastHeight=height;lastData=data;
        int start=changed?(width<Ui.dp(getContext(),200)||height<Ui.dp(getContext(),240)?1:0):profile;
        for(int level=start;level<DENSITY.length;level++){
            apply(content,level,height);profile=level;
            content.measure(MeasureSpec.makeMeasureSpec(width,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED));
            if(content.getMeasuredHeight()<=height)break;
        }
        super.onMeasure(widthSpec,heightSpec);
    }
    private void capture(View view,Set<View> live){
        live.add(view);if(!styles.containsKey(view))styles.put(view,new Style(view));
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++)capture(group.getChildAt(n),live);}
    }
    private void apply(View view,int level,int viewportHeight){
        Style style=styles.get(view);float gap=level==0?1:level==1?.7f:.4f;
        view.setPadding(Math.round(style.left*gap),Math.round(style.top*gap),Math.round(style.right*gap),Math.round(style.bottom*gap));
        if(view instanceof TextView){
            TextView text=(TextView)view;float size=Math.max(Math.min(12,style.sp),style.sp*DENSITY[level]);
            if(Math.abs(text.getTextSize()-size*getResources().getDisplayMetrics().scaledDensity)>.1f)text.setTextSize(TypedValue.COMPLEX_UNIT_SP,size);
            if(text.getMaxLines()!=Integer.MAX_VALUE){text.setSingleLine(false);text.setMaxLines(Integer.MAX_VALUE);}if(text.getEllipsize()!=null)text.setEllipsize(null);
            if(view instanceof Button){
                if(text.getMinHeight()!=Ui.dp(getContext(),48))text.setMinHeight(Ui.dp(getContext(),48));if(text.getMinimumHeight()!=Ui.dp(getContext(),48))text.setMinimumHeight(Ui.dp(getContext(),48));
                ViewGroup.LayoutParams lp=view.getLayoutParams();if(lp!=null&&lp.height!=ViewGroup.LayoutParams.WRAP_CONTENT){lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;view.setLayoutParams(lp);}
            }
        }
        if(view instanceof ImageView){
            ((ImageView)view).setScaleType(ImageView.ScaleType.FIT_CENTER);
            ViewGroup.LayoutParams lp=view.getLayoutParams();
            if(lp!=null&&style.height>0){int h=Math.max(Ui.dp(getContext(),48),Math.min(style.height,Math.round(viewportHeight*.42f)));if(lp.height!=h){lp.height=h;view.setLayoutParams(lp);}}
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int n=0;n<group.getChildCount();n++)apply(group.getChildAt(n),level,viewportHeight);}
    }
}
