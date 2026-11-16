package com.nth.desktop3d;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

/** The same hinged glass front is drawn by the live transition and settings preview. */
final class FolderLid {
    private final Drawable glass;private final Paint paint=new Paint(3);private final Camera camera=new Camera();
    private final Matrix matrix=new Matrix();private final RectF sheet=new RectF();
    FolderLid(Context context){glass=Ui.crystal(context,24,false);}
    void draw(Canvas canvas,RectF panel,RectF origin,float progress,Bitmap snapshot){
        float alpha=FolderPose.lidAlpha(progress);if(alpha<=0||panel.isEmpty())return;
        float p=FolderPose.expand(progress);
        sheet.set(FolderPose.mix(origin.left,panel.left+panel.width()*.03f,p),FolderPose.mix(origin.top,panel.bottom-panel.height()*.58f,p),FolderPose.mix(origin.right,panel.right-panel.width()*.03f,p),FolderPose.mix(origin.bottom,panel.bottom-panel.height()*.025f,p));
        int save=canvas.save();camera.save();camera.setLocation(0,0,-Math.max(8,sheet.height()/72f*2.8f));camera.rotateX(FolderPose.lidAngle(progress));camera.getMatrix(matrix);camera.restore();matrix.preTranslate(-sheet.centerX(),-sheet.bottom);matrix.postTranslate(sheet.centerX(),sheet.bottom);canvas.concat(matrix);
        float material=FolderPose.ease(.02f,.27f,progress);glass.setBounds(Math.round(sheet.left),Math.round(sheet.top),Math.round(sheet.right),Math.round(sheet.bottom));glass.setAlpha(Math.round(255*alpha*material));glass.draw(canvas);
        if(snapshot!=null){paint.setAlpha(Math.round(255*alpha*FolderPose.snapshotAlpha(progress)));canvas.drawBitmap(snapshot,null,sheet,paint);}
        paint.setColor(0xFFDAEDF1);paint.setAlpha(Math.round(110*alpha*material));paint.setStrokeWidth(Math.max(1,sheet.width()*.003f));canvas.drawLine(sheet.left+sheet.width()*.08f,sheet.bottom-sheet.height()*.025f,sheet.right-sheet.width()*.08f,sheet.bottom-sheet.height()*.025f,paint);canvas.restoreToCount(save);
    }
}
