package com.cgfay.cain.camerasample.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;

import com.cgfay.cain.camerasample.data.MediaItemData;
import com.cgfay.cain.camerasample.data.Size;
import com.cgfay.cain.camerasample.exif.ExifInterface;
import com.google.common.base.Optional;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Storage {
    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();

    public static final String DIRECTORY = DCIM + File.separator + "Camera";
    public static final File DIRECTORY_FILE = new File(DIRECTORY);
    public static final String JPEG_POSTFIX = ".jpg";
    public static final String GIF_POSTFIX = ".gif";
    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long ACCESS_FAILURE = -4L;
    public static final long LOW_STORAGE_THRESHOLOD_BYTES = 50000000; // 50MB
    public static final String CAMERA_SESSION_SCHEME = "camera_session";

    private static final String TAG = "Storage";
    private static final String GOOGLE_COM = "google.com";
    private static final int LRUCACHE_MAX_MEMORY_SIZE = 20 * 1024 * 1024; // 20MB

    private static final HashMap<Uri, Uri> sSessionsToContentUris = new HashMap<>();
    private static final HashMap<Uri, Uri> sContentUrisToSessions = new HashMap<>();

    // 用于存在placeholder 的 LruCache
    private static LruCache<Uri, Bitmap> sSessionsToPlaceHolderBitmap =
            new LruCache<Uri, Bitmap>(LRUCACHE_MAX_MEMORY_SIZE) {
                @Override
                protected int sizeOf(Uri key, Bitmap value) {
                    return value.getByteCount();
                }
            };


    private static HashMap<Uri, Point> sSessionsToSizes = new HashMap<>();
    private static HashMap<Uri, Integer> sSessionsToPlaceholderVersions = new HashMap<>();


    public static Uri saveImage(ContentResolver resolver, String title, long date,
                                Location location, int orientation, ExifInterface exif, byte[] jpeg,
                                int width, int height) throws IOException {
        return saveImage(resolver, title, date, location, orientation, exif, jpeg,
                width, height, MediaItemData.MIME_TYPE_JPEG);
    }

    public static Uri saveImage(ContentResolver resolver, String title, long date,
                                Location location, int orientation, ExifInterface exif, byte[] jpeg,
                                int width, int height, String mimeType) throws IOException {
        String path = generateFilePath(title, mimeType);
        long fileLength = writeFile(path, jpeg, exif);
        if (fileLength > 0) {
            return saveimageToMediaStore(resolver, title, date, location, orientation, fileLength,
                    path, width, height, mimeType);
        }
        return null;
    }

    public static Uri saveImage(ContentResolver resolver, String title, long date,
                                Location location, int orientation, ExifInterface exif, Bitmap bitmap,
                                int width, int height, String mimeType) throws IOException {
        String path = generateFilePath(title, mimeType);
        long fileLength = writeFile(path, bitmap, exif);
        if (fileLength > 0) {
            return saveimageToMediaStore(resolver, title, date, location, orientation, fileLength,
                    path, width, height, mimeType);
        }
        return null;
    }
    /**
     * 将图片信息保存到MediaStore中
     * @param resolver
     * @param title
     * @param date
     * @param location
     * @param orientation
     * @param jpegLength
     * @param path
     * @param width
     * @param height
     * @param mimeType
     * @return
     */
    public static Uri saveimageToMediaStore(ContentResolver resolver, String title, long date,
                                            Location location, int orientation, long jpegLength,
                                            String path, int width, int height, String mimeType) {
        ContentValues values = getContentValuesForData(title, date, location, orientation,
                jpegLength, path, width, height, mimeType);
        Uri uri = null;
        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable throwable) {
            Log.e(TAG, "Failed to write MediaStore" + throwable);
        }
        return uri;
    }

    /**
     * 创建ContentValues对象
     * @param title
     * @param date
     * @param location
     * @param orientation
     * @param jpegLength
     * @param path
     * @param width
     * @param height
     * @param mimeType
     * @return
     */
    public static ContentValues getContentValuesForData(String title, long date,
                                                        Location location,
                                                        int orientation, long jpegLength,
                                                        String path, int width, int height,
                                                        String mimeType) {
        File file = new File(path);
        long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(file.lastModified());
        ContentValues values = new ContentValues(11);
        values.put(MediaStore.Images.ImageColumns.TITLE, title);
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, title + JPEG_POSTFIX);
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date);
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, dateModifiedSeconds);
        values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
        values.put(MediaStore.Images.ImageColumns.DATA, path);
        values.put(MediaStore.Images.ImageColumns.SIZE, jpegLength);

        // 设置图片的大小
        setImageSize(values, width, height);

        // 如果经纬度存在，则添加经纬度值
        if (location != null) {
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location.getLatitude());
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location.getLongitude());
        }
        return values;
    }

    /**
     * 给新增的image图片添加placeholder
     * @param placeholder
     * @return
     */
    public static Uri addPlaceholder(Bitmap placeholder) {
        Uri uri = generateUniquePlaceholderUri();
        replacePlaceholder(uri, placeholder);
        return uri;
    }


    /**
     * 从内存中移除placeholder
     * @param uri
     */
    public static void removePlaceholder(Uri uri) {
        sSessionsToSizes.remove(uri);
        sSessionsToPlaceHolderBitmap.remove(uri);
        sSessionsToPlaceholderVersions.remove(uri);
    }


    /**
     * 替换某个placeholder
     * @param uri
     * @param placeholder
     */
    public static void replacePlaceholder(Uri uri, Bitmap placeholder) {
        Point size = new Point(placeholder.getWidth(), placeholder.getHeight());
        sSessionsToSizes.put(uri, size);
        sSessionsToPlaceHolderBitmap.put(uri, placeholder);
        Integer currentVersion = sSessionsToPlaceholderVersions.get(uri);
        sSessionsToPlaceholderVersions.put(uri, currentVersion == null ? 0 : currentVersion + 1);
    }

    public static Uri addEmptyPlaceholder(Size size) {
        Uri uri = generateUniquePlaceholderUri();
        sSessionsToSizes.put(uri, new Point(size.getWidth(), size.getHeight()));
        sSessionsToPlaceHolderBitmap.remove(uri);
        Integer currentVersion = sSessionsToPlaceholderVersions.get(uri);
        sSessionsToPlaceholderVersions.put(uri, currentVersion == null ? 0 : currentVersion + 1);

        return uri;
    }

    public static Uri updateImage(Uri imageUri, ContentResolver resolver, String title,
                                  long date, Location location, int orientation,
                                  ExifInterface exif, byte[] data, int width,
                                  int height, String mimeType) throws IOException {
        String path = generateFilePath(title, mimeType);
        writeFile(path, data, exif);
        return updateImage(imageUri, resolver, title, date, location,
                orientation, data.length, path, width, height, mimeType);
    }

    private static Uri generateUniquePlaceholderUri() {
        Uri.Builder builder = new Uri.Builder();
        String uuid = UUID.randomUUID().toString();
        builder.scheme(CAMERA_SESSION_SCHEME).authority(GOOGLE_COM).appendPath(uuid);
        return builder.build();
    }

    private static void setImageSize(ContentValues values, int width, int height) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            values.put(MediaStore.MediaColumns.WIDTH, width);
            values.put(MediaStore.MediaColumns.HEIGHT, height);
        }
    }

    public static long writeFile(String path, byte[] jpeg, ExifInterface exif)
            throws IOException {
        if (!createDirectoryIfNeeded(path)) {
            Log.e(TAG, "Failed to create parent directory for title: " + path);
            return -1;
        }
        if (exif != null) {
            exif.writeExif(jpeg, path);
            File file = new File(path);
            return file.length();
        } else {
            return writeFile(path, jpeg);
        }
    }

    public static long writeFile(String path, Bitmap bitmap, ExifInterface exif)
            throws IOException {
        if (!createDirectoryIfNeeded(path)) {
            Log.e(TAG, "Failed to create parent directory for title: " + path);
            return -1;
        }
        if (exif != null) {
            exif.writeExif(bitmap, path);
            File file = new File(path);
            return file.length();
        } else {
            return writeFile(path, bitmap);
        }
    }

    private static long writeFile(String path, byte[] jpeg) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(jpeg);
            return jpeg.length;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write data", e);
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        }
        return -1;
    }

    private static long writeFile(String path, Bitmap bitmap) {
        int length = bitmap.getByteCount();
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        FileOutputStream fout = null;
        BufferedOutputStream bos = null;
        try {
            fout = new FileOutputStream(path);
            bos = new BufferedOutputStream(fout);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            bos.flush();
        } catch (IOException e) {
            Log.e(TAG, "fail to write bitmap to path: " + path, e);
            return -1;
        } finally {
            try {
                fout.close();
            } catch (IOException e) {
                Log.e(TAG, "fail to close FileOutputStream: ", e);
            }
            try {
                bos.close();
            } catch (IOException e) {
                Log.e(TAG, "fail to close BufferedOutputStream: ", e);
            }
        }
        return length;
    }
    /**
     * 重命名文件
     * @param inputPath
     * @param newFilePath
     * @return
     */
    public static boolean renameFile(File inputPath, File newFilePath) {
        if (newFilePath.exists()) {
            Log.e(TAG, "File path already exists: " + newFilePath.getAbsolutePath());
            return false;
        }
        if (inputPath.isDirectory()) {
            Log.e(TAG, "Input path is directory: " + inputPath.getAbsolutePath());
            return false;
        }

        if (!createDirectoryIfNeeded(newFilePath.getAbsolutePath())) {
            Log.e(TAG, "Failed to create parent directory for file: " +
                    newFilePath.getAbsolutePath());
            return false;
        }
        return inputPath.renameTo(newFilePath);
    }

    /**
     * 判断是否需要创建目录
     * @param filePath
     * @return
     */
    private static boolean createDirectoryIfNeeded(String filePath) {
        File parentFile = new File(filePath).getParentFile();
        if (parentFile.exists()) {
            return parentFile.isDirectory();
        }
        return parentFile.mkdirs();
    }

    /**
     * 更新图片
     * @param imageUri
     * @param resolver
     * @param title
     * @param date
     * @param location
     * @param orientation
     * @param jpegLength
     * @param path
     * @param width
     * @param height
     * @param mimeType
     * @return
     */
    private static Uri updateImage(Uri imageUri, ContentResolver resolver, String title, long date,
                                   Location location, int orientation, int jpegLength, String path,
                                   int width, int height, String mimeType) {
        ContentValues values = getContentValuesForData(title, date, location, orientation,
                jpegLength, path, width, height, mimeType);

        Uri resultUri =imageUri;
        if (isSessionUri(imageUri)) {
            resultUri = saveimageToMediaStore(resolver, title, date, location, orientation,
                    jpegLength, path, width, height, mimeType);
            sSessionsToContentUris.put(imageUri, resultUri);
            sContentUrisToSessions.put(resultUri, imageUri);
        } else {
            resolver.update(imageUri, values, null, null);
        }
        return resultUri;
    }

    /**
     * 获取图片的路径
     * @param title
     * @param mimeType
     * @return
     */
    private static String generateFilePath(String title, String mimeType) {
        return generateFilePath(DIRECTORY, title, mimeType);
    }

    /**
     * 获取图片路径
     * @param directory
     * @param title
     * @param mimeType
     * @return
     */
    private static String generateFilePath(String directory, String title, String mimeType) {
        String extension = null;
        if (MediaItemData.MIME_TYPE_JPEG.equals(mimeType)) {
            extension = JPEG_POSTFIX;
        } else if (MediaItemData.MIME_TYPE_GIF.equals(mimeType)) {
            extension = GIF_POSTFIX;
        } else {
            throw new IllegalArgumentException("Invalid mimeType: " + mimeType);
        }
        return (new File(directory, title + extension)).getAbsolutePath();
    }


    public static Optional<Bitmap> getPlaceholderForSession(Uri uri) {
        return Optional.fromNullable(sSessionsToPlaceHolderBitmap.get(uri));
    }

    public static boolean containsPlaceholderSize(Uri uri) {
        return sSessionsToSizes.containsKey(uri);
    }

    public static Point getSizeForSession(Uri uri) {
        return sSessionsToSizes.get(uri);
    }

    public static Uri getContentUriForSessionUri(Uri uri) {
        return sSessionsToContentUris.get(uri);
    }

    public static Uri getSessionUriFromContentUri(Uri contentUri) {
        return sContentUrisToSessions.get(contentUri);
    }

    public static boolean isSessionUri(Uri uri) {
        return uri.getScheme().equals(CAMERA_SESSION_SCHEME);
    }

    /**
     * 获取可用的存储空间
     * @return
     */
    public static long getAvailableSpace() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        File dir = new File(DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(DIRECTORY);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.e(TAG, "Fail to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }

    /**
     * OSX 需要请求 usb 存储器 存在 /DCIM/NNNAAA
     */
    public static void ensureOSXCompatible() {
        File nnnAAA = new File(DCIM, "100ANDRO");
        if (!(nnnAAA.exists() || nnnAAA.mkdirs())) {
            Log.e(TAG, "Fail to create " + nnnAAA.getPath());
        }
    }
}

