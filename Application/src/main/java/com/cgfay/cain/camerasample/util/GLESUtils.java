package com.cgfay.cain.camerasample.util;

import android.opengl.Matrix;

public class GLESUtils {

    private GLESUtils() {}

    /**
     * 裁剪图片，类似ImageView的centerCrop
     */
    public static float[] getShowMatrix(int imgWidth, int imgHieght,
                                        int viewWidth, int viewHeight) {
        float[] projection = new float[16];
        float[] camera = new float[16];
        float[] matrix = new float[16];

        float scaleView = (float) viewWidth / viewHeight;
        float scaleImg = (float) imgWidth / imgHieght;

        if (scaleImg > scaleView) {
            Matrix.orthoM(projection, 0, -scaleView / scaleImg,
                    scaleView / scaleImg, -1, 1, 1, 3);
        } else {
            Matrix.orthoM(projection, 0, -1, 1,
                    -scaleImg / scaleView, scaleImg / scaleView, 1, 3);
        }
        Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
        Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        return matrix;
    }

    /**
     * 旋转
     * @param matrix
     * @param angle
     * @return
     */
    public static float[] rotate(float[] matrix, float angle) {
        Matrix.rotateM(matrix, 0, angle, 0, 0, 1);
        return matrix;
    }

    /**
     * 镜像
     * @param matrix
     * @param x
     * @param y
     * @return
     */
    public static float[] flip(float[] matrix, boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(matrix, 0, x ? -1 : 1, y ? -1 : 1, 1);
        }
        return matrix;
    }

    /**
     * 立方翻转
     * @param matrix
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static float[] flip(float[] matrix, boolean x, boolean y, boolean z) {
        if (x || y || z) {
            Matrix.scaleM(matrix, 0, x ? -1 : 1, y ? -1 : 1, z ? -1 : 1);
        }
        return matrix;
    }

    /**
     * 缩放
     * @param matrix
     * @param x
     * @param y
     * @return
     */
    public static float[] scale(float[] matrix, float x, float y) {
        Matrix.scaleM(matrix, 0, x, y, 1);
        return matrix;
    }

    /**
     * 立方缩放
     * @param matrix
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static float[] scale(float[] matrix, float x, float y, float z) {
        Matrix.scaleM(matrix, 0, x, y, z);
        return matrix;
    }

    public static float[] getOriginalMatrix(){
        return new float[]{
                1,0,0,0,
                0,1,0,0,
                0,0,1,0,
                0,0,0,1
        };
    }
}
