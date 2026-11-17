package com.nth.desktop3d;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;

/** A picker-safe immutable export snapshot, independent of Activity recreation. */
final class ScenePackFiles {
    private static AtomicFile pending(Context context){return new AtomicFile(new File(context.getCacheDir(),"scene-pack-export.json"));}
    static void prepare(Context context,byte[] bytes)throws Exception {ScenePack.decode(bytes);AtomicFile file=pending(context);FileOutputStream out=null;try{out=file.startWrite();out.write(bytes);file.finishWrite(out);}catch(Exception failure){if(out!=null)file.failWrite(out);throw failure;}}
    static byte[] read(Context context)throws Exception {try(InputStream in=pending(context).openRead()){byte[] bytes=WallpaperStore.read(in,ScenePack.MAX_BYTES);ScenePack.decode(bytes);return bytes;}}
    static void clear(Context context){pending(context).delete();}
    private ScenePackFiles(){}
}
