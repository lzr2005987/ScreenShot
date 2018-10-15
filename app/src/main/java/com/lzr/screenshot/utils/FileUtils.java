package com.lzr.screenshot.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Administrator on 2018/6/3 0003.
 */

public class FileUtils {
    //private static final String filePath = Environment.getExternalStorageDirectory() + "/ScreenRecorder/";

    public static void saveBitmap(Context context, Bitmap bitmap, String fileName) {
        //这个路径不需要另外申请文件读写权限
        String filePath = context.getExternalFilesDir("screenshot").getAbsoluteFile() + "/";

        StringBuffer picName = new StringBuffer();
        picName.append(fileName);
        File dir = new File(filePath);
        if (!dir.exists()) dir.mkdirs();

        File f = new File(filePath, picName.toString() + ".png");
        //文件名：根目录+/ScreenRecorder/phash.png
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
