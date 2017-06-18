package com.cgfay.cain.camerasample.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class GLESUtils {

    private static final String TAG = "GLESUtils";
    private static final boolean DEBUG = true;

    public static final int NO_TEXTURE = -1;
    private static final int SIZEOF_FLOAT = 4;


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

    /**
     * 根据输入矩阵和类型，计算输出矩阵
     * @param matrix    输入矩阵
     * @param type      类型
     * @param imgWidth  图片宽度
     * @param imgHeight 图片高度
     * @param viewWidth 视图宽度
     * @param viewHeight    视图高度
     */
    public static void getMatrix(float[] matrix,
                                 int type,
                                 int imgWidth,
                                 int imgHeight,
                                 int viewWidth,
                                 int viewHeight) {

        if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0) {
            float[] projection = new float[16];
            float[] camera = new float[16];
            if (type == TYPE_FITXY) {
                Matrix.orthoM(projection, 0, -1, 1, -1, 1, 1, 3);
                Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
                Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
            }
            float sWhView = (float) viewWidth / viewHeight;
            float sWhImg = (float) imgWidth / imgHeight;
            if (sWhImg>sWhView) {
                switch (type) {
                    case TYPE_CENTERCROP:
                        Matrix.orthoM(projection, 0, -sWhView/sWhImg, sWhView/sWhImg, -1, 1, 1, 3);
                        break;
                    case TYPE_CENTERINSIDE:
                        Matrix.orthoM(projection, 0, -1, 1, -sWhImg/sWhView, sWhImg/sWhView, 1, 3);
                        break;
                    case TYPE_FITSTART:
                        Matrix.orthoM(projection, 0, -1, 1, 1 - 2 * sWhImg/sWhView, 1, 1, 3);
                        break;
                    case TYPE_FITEND:
                        Matrix.orthoM(projection, 0, -1, 1, -1, 2 * sWhImg/sWhView - 1, 1, 3);
                        break;
                }
            } else {
                switch (type) {
                    case TYPE_CENTERCROP:
                        Matrix.orthoM(projection, 0, -1, 1, -sWhImg/sWhView, sWhImg/sWhView, 1, 3);
                        break;
                    case TYPE_CENTERINSIDE:
                        Matrix.orthoM(projection, 0, -sWhView/sWhImg, sWhView/sWhImg, -1, 1, 1, 3);
                        break;
                    case TYPE_FITSTART:
                        Matrix.orthoM(projection, 0, -1, 2 * sWhView/sWhImg - 1, -1, 1, 1, 3);
                        break;
                    case TYPE_FITEND:
                        Matrix.orthoM(projection, 0, 1 - 2 * sWhView/sWhImg, 1, -1, 1, 1, 3);
                        break;
                }
            }
            Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        }
    }

    /**
     * 获取类似ImageView 的 Center Inside 模式的变换矩阵
     * @param matrix    输入矩阵
     * @param imgWidth  图片宽度
     * @param imgHeight 图片高度
     * @param viewWidth 视图宽度
     * @param viewHeight    视图高度
     */
    public static void getCenterInsideMatrix(float[] matrix,
                                             int imgWidth,
                                             int imgHeight,
                                             int viewWidth,
                                             int viewHeight) {

        if(imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0) {
            float sWhView = (float)viewWidth/viewHeight;
            float sWhImg = (float)imgWidth/imgHeight;
            float[] projection = new float[16];
            float[] camera = new float[16];
            if (sWhImg > sWhView) {
                Matrix.orthoM(projection, 0, -1, 1, -sWhImg/sWhView, sWhImg/sWhView, 1, 3);
            }else{
                Matrix.orthoM(projection, 0, -sWhView/sWhImg, sWhView/sWhImg, -1, 1, 1, 3);
            }
            Matrix.setLookAtM(camera, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        }
    }

    /**
     * 平移
     * @param matrix
     * @param x
     * @param y
     * @return
     */
    public static float[] translate(float[] matrix, float x, float y) {
        return translate(matrix, x, y, 0);
    }

    /**
     * 平移
     * @param matrix 输入矩阵
     * @param x x轴平移量
     * @param y y轴平移量
     * @param z z轴平移量
     * @return 平移后的矩阵
     */
    public static float[] translate(float[] matrix, float x, float y, float z) {
        return translate(matrix, 0, x, y, z);
    }

    /**
     * 平移
     * @param matrix 输入矩阵
     * @param offset 偏移
     * @param x x轴平移量
     * @param y y轴平移量
     * @param z z轴平移量
     * @return 平移后的矩阵
     */
    public static float[] translate(float[] matrix, int offset, float x, float y, float z) {
        Matrix.translateM(matrix, offset, x, y, z);
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

    /**
     * 单位矩阵
     * @return
     */
    public static float[] getOriginalMatrix(){
        return new float[] {
                1,0,0,0,
                0,1,0,0,
                0,0,1,0,
                0,0,0,1
        };
    }

    /**
     * 设置Texture参数
     */
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

    /**
     * 设置texture参数
     * @param gl_wrap_s
     * @param gl_wrap_t
     * @param gl_min_filter
     * @param gl_mag_filter
     */
    public static void useTexParameter(int gl_wrap_s,
                                       int gl_wrap_t,
                                       int gl_min_filter,
                                       int gl_mag_filter){
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,gl_wrap_s);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,gl_wrap_t);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,gl_min_filter);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,gl_mag_filter);
    }

    /**
     * 创建Texture
     * @param size
     * @param textures
     * @param start
     * @param gl_format
     * @param width
     * @param height
     */
    public static void genTexturesWithParameter(int size,int[] textures,int start,
                                                int gl_format,int width,int height){
        // 创建Texture
        GLES20.glGenTextures(size, textures, start);
        // 绑定Texture
        for (int i = 0; i < size; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, gl_format,
                    width, height, 0, gl_format, GLES20.GL_UNSIGNED_BYTE, null);
            // 设置Texture 参数
            useTexParameter();
        }
        // 取消绑定
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    /**
     * 绑定FrameBuffer和Texture
     * @param frameBufferId
     * @param textureId
     */
    public static void bindFrameTexture(int frameBufferId, int textureId){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    /**
     * 解绑FrameBuffer
     */
    public static void unBindFrameBuffer(){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }


    /**
     * 创建program程序
     * @param vertexSource  顶点数据源
     * @param fragmentSource    片元数据源
     * @return  program 的 id值
     */
    public static int loadProgram(String vertexSource, String fragmentSource){
        int vertex= loadShader(GLES20.GL_VERTEX_SHADER,vertexSource);
        if(vertex==0)return 0;
        int fragment= loadShader(GLES20.GL_FRAGMENT_SHADER,fragmentSource);
        if(fragment==0)return 0;
        int program= GLES20.glCreateProgram();
        if(program!=0){
            GLES20.glAttachShader(program,vertex);
            GLES20.glAttachShader(program,fragment);
            GLES20.glLinkProgram(program);
            int[] linkStatus=new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS,linkStatus,0);
            if(linkStatus[0]!= GLES20.GL_TRUE){
                showError(1, "Could not link program:" + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program=0;
            }
        }
        return program;
    }

    /**
     * 加载shader
     * @param shaderType shader类型
     * @param source    shader文件
     * @return  shader 的 id
     */
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (0 != shader) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                showError(1, "Could not compile shader:" + shaderType);
                showError(1, "GLES20 Error:" + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    /**
     * 显示出错信息
     * @param code  出错代码
     * @param index
     */
    public static void showError(int code, Object index) {
        if (DEBUG && code != 0) {
            Log.e(TAG, "glError:" + code + "---" + index);
        }
    }


    /**
     * @param textureTarget Texture类型。
     * 1. 相机用 GLES11Ext.GL_TEXTURE_EXTERNAL_OES
     * 2. 图片用GLES20.GL_TEXTURE_2D
     * @param minFilter 缩小过滤类型 (1.GL_NEAREST ; 2.GL_LINEAR)
     * @param magFilter 放大过滤类型
     * @param wrapS X方向边缘环绕
     * @param wrapT Y方向边缘环绕
     * @return 返回创建的 Texture ID
     */
    public static int createTexture(int textureTarget, @Nullable Bitmap bitmap, int minFilter,
                                    int magFilter, int wrapS, int wrapT) {
        int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);
        checkGlError("glGenTextures");
        GLES20.glBindTexture(textureTarget, textureHandle[0]);
        checkGlError("glBindTexture " + textureHandle[0]);
        GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, minFilter);
        GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, magFilter); //线性插值
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, wrapS);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, wrapT);

        if (bitmap != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        }

        checkGlError("glTexParameter");
        return textureHandle[0];
    }

    public static int createTexture(int textureTarget) {
        return createTexture(textureTarget, null, GLES20.GL_LINEAR, GLES20.GL_LINEAR,
                GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE);
    }

    public static int createTexture(int textureTarget, Bitmap bitmap) {
        return createTexture(textureTarget, bitmap, GLES20.GL_LINEAR, GLES20.GL_LINEAR,
                GLES20.GL_CLAMP_TO_EDGE, GLES20.GL_CLAMP_TO_EDGE);
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     * <p>
     * Throws a RuntimeException if the location is invalid.
     */
    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    public static String readTextFromRawResource(final Context applicationContext,
                                                 @RawRes final int resourceId) {
        final InputStream inputStream =
                applicationContext.getResources().openRawResource(resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String nextLine;
        final StringBuilder body = new StringBuilder();
        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            return null;
        }

        return body.toString();
    }

    public static int createTextureWithTextContent(String text) {
        // Create an empty, mutable bitmap
        Bitmap bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        // get a canvas to paint over the bitmap
        Canvas canvas = new Canvas(bitmap);
        canvas.drawARGB(0, 0, 255, 0);
        // get a background image from resources
        // note the image format must match the bitmap format
        //        Drawable background = context.getResources().getDrawable(R.drawable.background);
        //        background.setBounds(0, 0, 256, 256);
        //        background.draw(canvas); // draw the background to our bitmap
        // Draw the text
        Paint textPaint = new Paint();
        textPaint.setTextSize(32);
        textPaint.setAntiAlias(true);
        textPaint.setARGB(0xff, 0xff, 0xff, 0xff);
        // draw the text centered
        canvas.drawText(text, 16, 112, textPaint);

        int[] textures = new int[1];

        //Generate one texture pointer...
        GLES20.glGenTextures(1, textures, 0);

        //...and bind it to our array
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);

        //Create Nearest Filtered Texture
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        //Different possible texture parameters, e.g. GLES20.GL_CLAMP_TO_EDGE
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        //Alpha blending
        //GLES20.glEnable(GLES20.GL_BLEND);
        //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        //Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        //Clean up
        bitmap.recycle();

        return textures[0];
    }

    public static int getExternalOESTextureID(){
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }
}
