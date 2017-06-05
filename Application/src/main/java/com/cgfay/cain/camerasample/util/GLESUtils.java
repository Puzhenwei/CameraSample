package com.cgfay.cain.camerasample.util;

import android.opengl.GLES20;
import android.opengl.Matrix;

public class GLESUtils {

    public static final int TYPE_FITXY=0;
    public static final int TYPE_CENTERCROP=1;
    public static final int TYPE_CENTERINSIDE=2;
    public static final int TYPE_FITSTART=3;
    public static final int TYPE_FITEND=4;

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

    public static void getMatrix(float[] matrix,
                                 int type,
                                 int imgWidth,
                                 int imgHeight,
                                 int viewWidth,
                                 int viewHeight){

        if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0) {
            float[] projection = new float[16];
            float[] camera = new float[16];
            if (type == TYPE_FITXY) {
                Matrix.orthoM(projection,0,-1,1,-1,1,1,3);
                Matrix.setLookAtM(camera,0,0,0,1,0,0,0,0,1,0);
                Matrix.multiplyMM(matrix,0,projection,0,camera,0);
            }
            float sWhView = (float) viewWidth/viewHeight;
            float sWhImg = (float) imgWidth/imgHeight;
            if (sWhImg>sWhView) {
                switch (type){
                    case TYPE_CENTERCROP:
                        Matrix.orthoM(projection,0,-sWhView/sWhImg,sWhView/sWhImg,-1,1,1,3);
                        break;
                    case TYPE_CENTERINSIDE:
                        Matrix.orthoM(projection,0,-1,1,-sWhImg/sWhView,sWhImg/sWhView,1,3);
                        break;
                    case TYPE_FITSTART:
                        Matrix.orthoM(projection,0,-1,1,1-2*sWhImg/sWhView,1,1,3);
                        break;
                    case TYPE_FITEND:
                        Matrix.orthoM(projection,0,-1,1,-1,2*sWhImg/sWhView-1,1,3);
                        break;
                }
            } else {
                switch (type) {
                    case TYPE_CENTERCROP:
                        Matrix.orthoM(projection,0,-1,1,-sWhImg/sWhView,sWhImg/sWhView,1,3);
                        break;
                    case TYPE_CENTERINSIDE:
                        Matrix.orthoM(projection,0,-sWhView/sWhImg,sWhView/sWhImg,-1,1,1,3);
                        break;
                    case TYPE_FITSTART:
                        Matrix.orthoM(projection,0,-1,2*sWhView/sWhImg-1,-1,1,1,3);
                        break;
                    case TYPE_FITEND:
                        Matrix.orthoM(projection,0,1-2*sWhView/sWhImg,1,-1,1,1,3);
                        break;
                }
            }
            Matrix.setLookAtM(camera,0,0,0,1,0,0,0,0,1,0);
            Matrix.multiplyMM(matrix,0,projection,0,camera,0);
        }
    }

    public static void getCenterInsideMatrix(float[] matrix,int imgWidth,int imgHeight,int viewWidth,int
            viewHeight){
        if(imgHeight>0&&imgWidth>0&&viewWidth>0&&viewHeight>0){
            float sWhView=(float)viewWidth/viewHeight;
            float sWhImg=(float)imgWidth/imgHeight;
            float[] projection=new float[16];
            float[] camera=new float[16];
            if(sWhImg>sWhView){
                Matrix.orthoM(projection,0,-1,1,-sWhImg/sWhView,sWhImg/sWhView,1,3);
            }else{
                Matrix.orthoM(projection,0,-sWhView/sWhImg,sWhView/sWhImg,-1,1,1,3);
            }
            Matrix.setLookAtM(camera,0,0,0,1,0,0,0,0,1,0);
            Matrix.multiplyMM(matrix,0,projection,0,camera,0);
        }
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

    /**
     * 单位矩阵
     * @return
     */
    public static float[] getOriginalMatrix(){
        return new float[]{
                1,0,0,0,
                0,1,0,0,
                0,0,1,0,
                0,0,0,1
        };
    }

    public static void useTexParameter(){
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
    }

    public static void useTexParameter(int gl_wrap_s,int gl_wrap_t,int gl_min_filter,
                                       int gl_mag_filter){
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,gl_wrap_s);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,gl_wrap_t);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,gl_min_filter);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,gl_mag_filter);
    }

    public static void genTexturesWithParameter(int size,int[] textures,int start,
                                                int gl_format,int width,int height){
        GLES20.glGenTextures(size, textures, start);
        for (int i = 0; i < size; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0,gl_format, width, height,
                    0, gl_format, GLES20.GL_UNSIGNED_BYTE, null);
            useTexParameter();
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
    }

    public static void bindFrameTexture(int frameBufferId,int textureId){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    public static void unBindFrameBuffer(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,0);
    }
}
