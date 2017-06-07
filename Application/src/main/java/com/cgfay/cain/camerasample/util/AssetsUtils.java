package com.cgfay.cain.camerasample.util;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class AssetsUtils {

    private static final String TAG = "AssetsUtils";

    private static final int BUFFER_SIZE = 1024 * 8;

    private AssetsUtils() {}

    /**
     * 获取Assets文件夹下某个目录的文件列表
     * @param context  上下文
     * @param path  assets下面的某个目录
     * @return
     */
    public static String[] getFileFromAssets(Context context, String path) {
        AssetManager manager = context.getAssets();
        String[] files = null;
        try {
            files = manager.list(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }

    /**
     * 将Assets下面某个文件夹的所有文件（包括文件夹）复制到指定的目录
     * @param context   上下文
     * @param path  Assets下面的文件夹
     * @param outPath 输出路径
     * @return  是否复制成功
     */
    public static boolean copyFromAssets(Context context, String path, String outPath) {

        String[] files = getFileFromAssets(context, path);
        // 如果是目录
        if (files.length > 0) {
            File file = new File(outPath);
            file.mkdirs(); // 如果文件夹不存在，则递归创建

            String oldPath = path;
            if (!path.endsWith("/")) {
                oldPath = path + File.separator;
            }
            String newPath = outPath;
            if (!outPath.endsWith("/")) {
                newPath = outPath + File.separator;
            }
            for (String fileName : files) {
                boolean success = copyFromAssets(context, oldPath + fileName, newPath + fileName);
                // 如果复制不成功，则返回false
                if (!success) {
                    return false;
                }
            }
        }
        // 如果是文件
        else {
            try {
                InputStream is = context.getAssets().open(path);
                FileOutputStream fos = new FileOutputStream(outPath);
                byte[] buffer = new byte[BUFFER_SIZE];
                int count = 0;
                while ((count = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                }
                fos.flush();
                is.close();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }
}
