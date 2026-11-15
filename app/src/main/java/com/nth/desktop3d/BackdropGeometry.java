package com.nth.desktop3d;

import android.graphics.*;import android.view.View;import java.util.List;

/** Convert visible native content into the current menu's coordinate space. */
final class BackdropGeometry {
    static void append(List<RectF> out,View content,View target){
        if(content==null||target==null||!content.isShown()||content.getAlpha()<.1f)return;Rect rect=new Rect();if(!content.getGlobalVisibleRect(rect))return;int[] origin=new int[2];target.getLocationOnScreen(origin);float sx=Math.max(.1f,target.getScaleX()),sy=Math.max(.1f,target.getScaleY());out.add(new RectF((rect.left-origin[0])/sx,(rect.top-origin[1])/sy,(rect.right-origin[0])/sx,(rect.bottom-origin[1])/sy));
    }
}
