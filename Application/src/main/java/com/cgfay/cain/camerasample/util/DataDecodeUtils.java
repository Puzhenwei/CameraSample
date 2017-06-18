package com.cgfay.cain.camerasample.util;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class DataDecodeUtils {

    /**
     * 将yuv数据转换为jpeg
     * @param yuvBytes
     * @param width
     * @param height
     * @return
     */
    public static byte[] yuv2Jpeg(byte[] yuvBytes, int width, int height) {
        YuvImage yuvImage = new YuvImage(yuvBytes, ImageFormat.NV21, width, height, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);

        return baos.toByteArray();
    }

    /**
     * 旋转图像
     * @param sourceBitmap
     * @param degree
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap sourceBitmap, int degree) {
        Matrix matrix = new Matrix();
        //旋转90度，并做镜面翻转
        matrix.setRotate(degree);
        matrix.postScale(-1, 1);
        return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
    }

    /**
     * 保存bitmap到path路径
     */
    public static void saveBitmap(Bitmap bitmap, String path) {
        if(bitmap != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(new File(path)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * YUV420、NV21 转成ARGB_8888格式
     * @param data
     * @param width
     * @param height
     * @return
     */
    private static int[] convertNV21toARGB8888(byte [] data, int width, int height) {
        int size = width * height;
        int offset = size;
        int[] pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        for (int i= 0, k= 0; i < size; i += 2, k += 2) {
            y1 = data[i] & 0xff;
            y2 = data[i + 1] & 0xff;
            y3 = data[width + i] & 0xff;
            y4 = data[width + i + 1] & 0xff;

            u = data[offset + k] & 0xff;
            v = data[offset + k + 1] & 0xff;
            u = u - 128;
            v = v - 128;

            pixels[i] = convertYUVtoARGB(y1, u, v);
            pixels[i + 1] = convertYUVtoARGB(y2, u, v);
            pixels[width + i] = convertYUVtoARGB(y3, u, v);
            pixels[width + i + 1] = convertYUVtoARGB(y4, u, v);

            if (i != 0 && (i + 2) % width == 0) {
                i += width;
            }
        }

        return pixels;
    }

    /**
     * YUV转成ARGB
     * @param y
     * @param u
     * @param v
     * @return
     */
    private static int convertYUVtoARGB(int y, int u, int v) {
        int r,g,b;

        r = y + (int)(1.402f * u);
        g = y - (int)(0.344f * v + 0.714f * u);
        b = y + (int)(1.772f * v);
        r = r>255? 255 : r < 0 ? 0 : r;
        g = g>255? 255 : g < 0 ? 0 : g;
        b = b>255? 255 : b < 0 ? 0 : b;

        return 0xff000000 | (r << 16) | (g << 8) | b;
    }
}