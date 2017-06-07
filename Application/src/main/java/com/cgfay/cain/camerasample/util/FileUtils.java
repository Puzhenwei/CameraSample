package com.cgfay.cain.camerasample.util;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class FileUtils {

    private static final int BUFFER_SIZE = 1024 * 8;

    private FileUtils() {}

    public static void copyFileOrFolder(String oldPath, String newPath) {
        File oldFile = new File(oldPath);
        if (oldFile.isFile()) {
            copyFile(oldPath, newPath);
        } else if (oldFile.isDirectory()) {
            copyFolder(oldPath, newPath);
        }
    }

    /**
     * 复制文件
     * @param oldPath
     * @param newPath
     * @return
     */
    public static void copyFile(String oldPath, String newPath) {
        InputStream is = null;
        FileOutputStream fs = null;
        int sum = 0;
        int len = 0;
        try {
            File oldFile = new File(oldPath);
            // 判断旧文件是否存在，如果存在，则将旧文件复制到新的地址
            if (oldFile.exists()) {
                is = new FileInputStream(oldPath);
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((len = is.read(buffer)) != -1) {
                    sum += len;
                    fs.write(buffer, 0, len);
                }
                fs.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 删除文件
     * @param context
     * @param fileName
     */
    public static void deleteFile(Context context, String fileName) {
        File file = new File(fileName);
        if (file != null) {
            file.delete();
        }
    }

    /**
     * 复制文件夹
     * @param oldPath
     * @param newPath
     */
    public static void copyFolder(String oldPath, String newPath) {

        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            (new File(newPath)).mkdirs();
            File oldFile = new File(oldPath);
            // 获取文件列表
            String[] file = oldFile.list();
            File temp = null;
            for (int i = 0; i < file.length; i++) {
                // 创建新文件时需要添加文件分隔符
                if (oldPath.endsWith(File.separator)) {
                    temp = new File(oldPath + file[i]);
                } else {
                    temp = new File(oldPath + File.separator + file[i]);
                }

                // 如果是文件，则直接复制，否则递复制子文件夹
                if (temp.isFile()) {
                    input = new FileInputStream(temp);
                    output = new FileOutputStream(newPath + "/" + temp.getName());
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                    output.flush();
                } else if (temp.isDirectory()) {
                    copyFolder(oldPath + "/" + file[i], newPath + "/" + file[i]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 遍历某个目录下的所有文件，包括子目录下的文件
     * @param path 某个目录的绝对路径
     * @return  返回存放文件绝对路径的列表
     */
    public static List<String> listFolder(String path) {
        List<String> result = new ArrayList<String>();
        File file = new File(path);
        File[] subFile = file.listFiles();
        for (int i = 0; i < subFile.length; i++) {
            if (!subFile[i].isDirectory()) {
                String fileName = subFile[i].getAbsolutePath();
                result.add(fileName);
            } else { // 如果是目录，则递归查找，并返回所有的
                List<String> subPath = listFolder(subFile[i].getAbsolutePath());
                result.addAll(subPath);
            }
        }
        return result;
    }

    /**
     * 从绝对路径中提取文件名
     * @param absolutePath 绝对路径
     * @return  不包含后缀的文件名
     */
    public static String getFileNameFromAbsolutePath(String absolutePath) {
        int start = absolutePath.lastIndexOf("/");
        int end = absolutePath.lastIndexOf(".");
        if (start != -1 && end != -1) {
            return absolutePath.substring(start + 1, end);
        }
        // 防止输入当前路径时，不包含"/"符号，导致返回空字符串
        else if (start == -1 && !absolutePath.contains("/") && end != -1) {
            return absolutePath.substring(0, end);
        }
        return null;
    }


    /**
     * 递归删除文件和文件夹
     *
     */
    public static void recursionDeleteFile(File file) {
        // 文件夹则递归删除
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            if (childFiles == null || childFiles.length == 0) {
                file.delete();
                return;
            }

            for (File f : childFiles) {
                recursionDeleteFile(f);
            }
            file.delete();

            return;
        }

        file.delete();
    }


}
