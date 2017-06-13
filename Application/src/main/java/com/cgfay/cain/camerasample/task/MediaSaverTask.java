package com.cgfay.cain.camerasample.task;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.cgfay.cain.camerasample.data.MediaItemData;
import com.cgfay.cain.camerasample.exif.ExifInterface;
import com.cgfay.cain.camerasample.util.Storage;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


public class MediaSaverTask implements MediaSaver {

    private static final String TAG = "MediaSaveTask";

    private static final String VIDEO_BASE_URI = "content://media/external/video/media";

    /**
     * 设置最大保存图片任务使用的内存是30M
     */
    private static final int SAVE_TASK_MEMORY_LIMIT = 30 * 1024 * 1024;

    private final ContentResolver mContentResolver;

    private long mMemoryUse;

    private QueueListener mQueueListener;

    public MediaSaverTask(ContentResolver mContentResolver) {
        this.mContentResolver = mContentResolver;
        mMemoryUse = 0;
    }

    @Override
    public boolean isQueueFull() {
        return (mMemoryUse >= SAVE_TASK_MEMORY_LIMIT);
    }

    @Override
    public void saveImage(Bitmap bitmap, String title, Location location,
                          int width, int height, int orientation,
                          ExifInterface exif, OnMediaSavedListener listener) {
        saveImage(bitmap, title, System.currentTimeMillis(), location, width, height,
                orientation, exif, listener, MediaItemData.MIME_TYPE_JPEG);
    }

    @Override
    public void saveImage(Bitmap bitmap, String title, long date, Location location,
                          int width, int height, int orientation, ExifInterface exif,
                          OnMediaSavedListener listener) {
        saveImage(bitmap, title, date, location, width, height, orientation,
                exif, listener, MediaItemData.MIME_TYPE_JPEG);
    }

    @Override
    public void saveImage(Bitmap bitmap, String title, long date, Location location,
                          int width, int height, int orientation, ExifInterface exif,
                          OnMediaSavedListener listener, String mimeType) {
        if (isQueueFull()) {
            Log.e(TAG, "Cannot add image when the queue is full");
            return;
        }
        ImageSaveTask task = new ImageSaveTask(bitmap, title, date,
                (location == null) ? null : new Location(location),
                width, height, orientation, mimeType, exif, mContentResolver, listener);
        // 计算内存使用
        mMemoryUse += bitmap.getByteCount();
        if (isQueueFull()) {
            onQuqueFull();
        }
        task.execute();
    }


    @Override
    public void saveImage(byte[] data, String title, Location location,
                          int width, int height, int orientation,
                          ExifInterface exif, OnMediaSavedListener listener) {
        saveImage(data, title, System.currentTimeMillis(), location, width, height,
                orientation, exif, listener, MediaItemData.MIME_TYPE_JPEG);
    }

    @Override
    public void saveImage(byte[] data, String title, long date, Location location,
                          int width, int height, int orientation, ExifInterface exif,
                          OnMediaSavedListener listener) {
        saveImage(data, title, date, location, width, height, orientation,
                exif, listener, MediaItemData.MIME_TYPE_JPEG);
    }

    @Override
    public void saveImage(byte[] data, String title, long date, Location location,
                          int width, int height, int orientation, ExifInterface exif,
                          OnMediaSavedListener listener, String mimeType) {
        if (isQueueFull()) {
            Log.e(TAG, "Cannot add image when the queue is full");
            return;
        }
        ImageSaveTask task = new ImageSaveTask(data, title, date,
                (location == null) ? null : new Location(location),
                width, height, orientation, mimeType, exif, mContentResolver, listener);
        // 计算内存使用
        mMemoryUse += data.length;
        if (isQueueFull()) {
            onQuqueFull();
        }
        task.execute();
    }

    @Override
    public void addVideo(String path, ContentValues values, OnMediaSavedListener listener) {
        new VideoSaveTask(path, values, listener, mContentResolver).execute();
    }

    @Override
    public void setQueueListener(QueueListener listener) {
        mQueueListener = listener;
        if (listener == null) {
            return;
        }
        listener.onQueueStatus(isQueueFull());
    }


    private void onQuqueFull() {
        if (mQueueListener != null) {
            mQueueListener.onQueueStatus(true);
        }
    }

    private void onQueueAvailable() {
        if (mQueueListener != null) {
            mQueueListener.onQueueStatus(false);
        }
    }

    /**
     * 保存图片任务
     */
    private class ImageSaveTask extends AsyncTask<Void, Void, Uri> {
        private final Bitmap bitmap;
        private final String title;
        private final long date;
        private final Location location;
        private int width, height;
        private final int orientation;
        private String mimeType;
        private final ExifInterface exif;
        private final ContentResolver resolver;
        private final OnMediaSavedListener listener;

        public ImageSaveTask(Bitmap bitmap, String title, long date, Location location,
                             int width, int height, int orientation, String mimeType,
                             ExifInterface exif, ContentResolver resolver,
                             OnMediaSavedListener listener) {
            this.orientation = orientation;
            this.exif = exif;
            this.resolver = resolver;
            this.listener = listener;
            this.width = width;
            this.height = height;
            this.mimeType = mimeType;
            this.bitmap = bitmap;
            this.title = title;
            this.date = date;
            this.location = location;
        }

        public ImageSaveTask(byte[] data, String title, long date, Location location,
                             int width, int height, int orientation, String mimeType,
                             ExifInterface exif, ContentResolver resolver,
                             OnMediaSavedListener listener) {
            this.orientation = orientation;
            this.exif = exif;
            this.resolver = resolver;
            this.listener = listener;
            this.width = width;
            this.height = height;
            this.mimeType = mimeType;
            this.title = title;
            this.date = date;
            this.location = location;
            // 创建Bitmap
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            ByteBuffer b = ByteBuffer.wrap(data);
            bitmap.copyPixelsFromBuffer(b);
            this.bitmap = bitmap;
        }


        @Override
        protected void onPreExecute() {
            // doing nothing
        }

        @Override
        protected Uri doInBackground(Void... params) {
            if (width == 0 || height == 0) {
                width = bitmap.getWidth();
                height = bitmap.getHeight();
            }
            try {
                return Storage.saveImage(resolver, title, date, location, orientation,
                        exif, bitmap, width, height, mimeType);
            } catch (IOException e) {
                Log.e(TAG, "Failed to write data", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (listener != null) {
                listener.onMediaSaved(uri);
            }
            // 调整队列可用内存
            boolean previouslyFull = isQueueFull();
            mMemoryUse -= bitmap.getByteCount();
            if (isQueueFull() != previouslyFull) {
                onQueueAvailable();
            }
        }
    }


    /**
     * 保存视频任务
     */
    private class VideoSaveTask extends AsyncTask<Void, Void, Uri> {
        private String path;
        private final ContentValues values;
        private final OnMediaSavedListener listener;
        private final ContentResolver resolver;

        public VideoSaveTask(String path,
                             ContentValues values,
                             OnMediaSavedListener listener,
                             ContentResolver resolver) {
            this.values = new ContentValues(values);
            this.listener = listener;
            this.resolver = resolver;
            this.path = path;
        }


        @Override
        protected Uri doInBackground(Void... params) {

            Uri uri = null;
            try {
                Uri videoTable = Uri.parse(VIDEO_BASE_URI);
                uri = resolver.insert(videoTable, values);

                String finalName = values.getAsString(MediaStore.Video.Media.DATA);
                File finalFile = new File(finalName);

                if (new File(path).renameTo(finalFile)) {
                    path = finalName;
                }

                resolver.update(uri, values, null, null);

            } catch (Exception e) {
                Log.e(TAG, "failed to add video to media store", e);
                uri = null;
            } finally {
                Log.v(TAG, "Current video URI: " + uri);
            }
            return uri;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (listener != null) {
                listener.onMediaSaved(uri);
            }
        }
    }

}
