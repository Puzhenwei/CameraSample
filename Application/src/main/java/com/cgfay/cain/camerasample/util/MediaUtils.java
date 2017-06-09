package com.cgfay.cain.camerasample.util;

import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;



public class MediaUtils {

    private static final String TAG = "MediaUtils";

    private MediaUtils() {}

    /**
     * 将bitmap数据和exif数据保存到path路径中
     * @param path 写入路径
     * @param bitmap  bitmap数据
     * @param exif  exif信息
     */
    public static long writeExifAndBitmapData(String path, Bitmap bitmap, ExifInterface exif) {
        // TODO 保存Exif信息和Bitmap数据


        File file = new File(path);
        return file.length();
    }


    /**
     * 保存bitmap数据到path路径中
     * @param path
     * @param bitmap
     */
    public static long writeBitmapWithoutExif(String path, Bitmap bitmap) {

        File file = new File(path);
        FileOutputStream fos = null;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
        } catch (IOException e) {
            Log.e(TAG, "fail to create new file: " + e);
            return -1;
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                Log.e(TAG, "fail to close FileOutputStream: " + e);
            }
        }

        return file.length();
    }

}
