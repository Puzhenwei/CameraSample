package com.cgfay.cain.camerasample.task;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;

import com.cgfay.cain.camerasample.exif.ExifInterface;


public interface MediaSaver {

    /**
     * 队列监听器
     */
    public interface QueueListener {
        /**
         * 监听队列状态
         * @param full
         */
        public void onQueueStatus(boolean full);
    }

    /**
     * 多媒体保存监听器
     */
    public interface OnMediaSavedListener {
        /**
         * 保存多媒体(主要是图片)
         * @param uri 路径
         */
        public void onMediaSaved(Uri uri);
    }

    /**
     * 判断队列是否已满
     * @return
     */
    public boolean isQueueFull();


    /**
     * 保存图片
     * @param bitmap  图片数据
     * @param title 标题
     * @param location  位置
     * @param width 宽度
     * @param height    高度
     * @param orientation   旋转方向
     * @param exif  exif信息
     * @param listener  监听器
     */
    public void saveImage(Bitmap bitmap, String title,
                          Location location, int width, int height,
                          int orientation, ExifInterface exif,
                          OnMediaSavedListener listener);

    /**
     * 保存图片
     * @param bitmap  图片数据
     * @param title  标题
     * @param date  时间
     * @param location  位置
     * @param width 宽度
     * @param height    高度
     * @param orientation   旋转方向
     * @param exif  exif信息
     * @param listener  监听器
     */
    public void saveImage(Bitmap bitmap, String title, long date,
                          Location location, int width, int height,
                          int orientation, ExifInterface exif,
                          OnMediaSavedListener listener);

    /**
     * 保存图片
     * @param bitmap 图片数据
     * @param title 标题
     * @param date  时间
     * @param location  位置
     * @param width 宽度
     * @param height    高度
     * @param orientation   旋转方向
     * @param exif  Exif信息
     * @param listener  监听器
     * @param mimeType  mimeType
     */
    public void saveImage(Bitmap bitmap, String title, long date,
                          Location location, int width, int height,
                          int orientation, ExifInterface exif,
                          OnMediaSavedListener listener, String mimeType);


    public void saveImage(byte[] data, String title,
                          Location location, int width, int height,
                          int orientation, ExifInterface exif,
                          OnMediaSavedListener listener);


    public void saveImage(byte[] data, String title, long date,
                          Location location, int width, int height,
                          int orientation, ExifInterface exif,
                          OnMediaSavedListener listener);

    public void saveImage(byte[] data, String title, long date,
                          Location location, int width, int height,
                          int orientation, ExifInterface exif,
                          OnMediaSavedListener listener, String mimeType);

    /**
     * 保存视频
     * @param path  保存的路径
     * @param values    ContentValues
     * @param listener  监听器
     */
    public void addVideo(String path, ContentValues values, OnMediaSavedListener listener);

    /**
     * 设置队列监听器
     * @param listener 监听器实例
     */
    void setQueueListener(QueueListener listener);
}
