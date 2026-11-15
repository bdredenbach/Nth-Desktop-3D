package com.nth.desktop3d;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.*;

/** App-owned bounded photo bytes survive provider-grant changes and travel in desktop backups. */
final class WallpaperStore {
    static final int MAX_BYTES=2*1024*1024;
    static File file(Context c){return new File(c.getFilesDir(),"desktop-wallpaper.webp");}
    static byte[] bytes(Context c)throws IOException {File f=file(c);if(!f.exists())return null;try(InputStream in=new AtomicFile(f).openRead()){return read(in,MAX_BYTES);}}
    static byte[] read(InputStream in,int limit)throws IOException {if(in==null)throw new IOException("File unavailable");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("File is too large");out.write(b,0,n);}return out.toByteArray();}
    static void validate(byte[] data)throws IOException {if(data==null)return;if(data.length>MAX_BYTES)throw new IOException("Wallpaper is too large");BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);if(o.outWidth<1||o.outHeight<1||o.outWidth>2048||o.outHeight>2048)throw new IOException("Invalid wallpaper image");Bitmap decoded=BitmapFactory.decodeByteArray(data,0,data.length);if(decoded==null)throw new IOException("Incomplete wallpaper image");decoded.recycle();}
    static void write(Context c,byte[] data)throws IOException {
        validate(data);AtomicFile file=new AtomicFile(file(c));if(data==null){file.delete();return;}FileOutputStream out=null;
        try{out=file.startWrite();out.write(data);file.finishWrite(out);}catch(IOException e){if(out!=null)file.failWrite(out);throw e;}
    }
    static void importPhoto(Context c,Uri uri)throws IOException {
        byte[] original;try(InputStream in=c.getContentResolver().openInputStream(uri)){original=read(in,32*1024*1024);}
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(original,0,original.length,o);
        if(o.outWidth<1||o.outHeight<1||(long)o.outWidth*o.outHeight>100000000L)throw new IOException("Unsupported image");
        o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1536)o.inSampleSize*=2;o.inJustDecodeBounds=false;o.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap image=BitmapFactory.decodeByteArray(original,0,original.length,o);if(image==null)throw new IOException("Unsupported image");
        try{int orientation=1;try{orientation=new ExifInterface(new ByteArrayInputStream(original)).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);}catch(IOException ignored){}
            Matrix matrix=new Matrix();switch(orientation){case 2:matrix.setScale(-1,1);break;case 3:matrix.setRotate(180);break;case 4:matrix.setScale(1,-1);break;case 5:matrix.setRotate(90);matrix.postScale(-1,1);break;case 6:matrix.setRotate(90);break;case 7:matrix.setRotate(270);matrix.postScale(-1,1);break;case 8:matrix.setRotate(270);break;default:break;}
            if(!matrix.isIdentity()){Bitmap rotated=Bitmap.createBitmap(image,0,0,image.getWidth(),image.getHeight(),matrix,true);if(rotated!=image){image.recycle();image=rotated;}}
            ByteArrayOutputStream out=new ByteArrayOutputStream();if(!image.compress(Bitmap.CompressFormat.WEBP,88,out))throw new IOException("Could not encode image");write(c,out.toByteArray());
        }finally{image.recycle();}
    }
    static Drawable photo(Context c){File f=file(c);if(!f.exists())return null;Bitmap b=BitmapFactory.decodeFile(f.getAbsolutePath());return b==null?null:new BitmapDrawable(c.getResources(),b);}
}
