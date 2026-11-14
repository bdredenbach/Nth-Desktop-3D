package com.nth.desktop3d;

/** A circular folder containing four real shortcut thumbnails, like the reference. */
final class FolderPreviewDrawable extends android.graphics.drawable.Drawable {
    private final java.util.List<android.graphics.drawable.Drawable> icons=new java.util.ArrayList<>();
    private final android.graphics.Paint paint=new android.graphics.Paint(3);
    FolderPreviewDrawable(LauncherStore.Item folder,AppCatalog catalog){for(LauncherStore.Item child:folder.children){if(icons.size()==4)break;android.graphics.drawable.Drawable d=child.folder()?new IconTile.BuiltinIcon("folder"):catalog.icon(child);icons.add(d==null?new IconTile.BuiltinIcon(child.key):d);}}
    @Override public void draw(android.graphics.Canvas canvas){android.graphics.Rect b=getBounds();float w=b.width(),h=b.height();paint.setColor(0x66324258);canvas.drawOval(b.left,b.top,b.right,b.bottom,paint);for(int i=0;i<icons.size();i++){android.graphics.drawable.Drawable d=icons.get(i);android.graphics.Rect old=d.copyBounds();int x=b.left+Math.round(w*(.1f+(i%2)*.46f)),y=b.top+Math.round(h*(.1f+(i/2)*.46f));d.setBounds(x,y,x+Math.round(w*.35f),y+Math.round(h*.35f));d.draw(canvas);d.setBounds(old);}}
    public void setAlpha(int value){paint.setAlpha(value);}public void setColorFilter(android.graphics.ColorFilter f){paint.setColorFilter(f);}public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
}
