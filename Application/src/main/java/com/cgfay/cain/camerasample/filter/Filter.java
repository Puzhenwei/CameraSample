
package com.cgfay.cain.camerasample.filter;

import android.content.res.Resources;
import android.opengl.GLES20;
import android.util.Log;
import android.util.SparseArray;

import com.cgfay.cain.camerasample.util.GLESUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;


public abstract class Filter {

    private static final String TAG = "Filter";

    public static final int KEY_OUT = 0x101;
    public static final int KEY_IN = 0x102;
    public static final int KEY_INDEX = 0x201;

    public static boolean DEBUG = true;
    /**
     * 单位矩阵
     */
    public static final float[] OM = GLESUtils.getOriginalMatrix();
    /**
     * 程序句柄
     */
    protected int mProgram;
    /**
     * 顶点坐标句柄
     */
    protected int mHPosition;
    /**
     * 纹理坐标句柄
     */
    protected int mHCoord;
    /**
     * 总变换矩阵句柄
     */
    protected int mHMatrix;
    /**
     * 默认纹理贴图句柄
     */
    protected int mHTexture;

    /**
     * Resources
     */
    protected Resources mResources;


    /**
     * 顶点坐标Buffer
     */
    protected FloatBuffer mVerBuffer;

    /**
     * 纹理坐标Buffer
     */
    protected FloatBuffer mTexBuffer;

    /**
     * 索引坐标Buffer
     */
    protected ShortBuffer mIndexBuffer;

    /**
     * 用于标志摄像头是前置还是后置
     */
    protected int mFlag = 0;


    private float[] matrix = Arrays.copyOf(OM, 16);

    /**
     * texture 的类型, 默认使用Texture2D0
     */
    private int mTextureType = 0;
    /**
     * texture 的 id 值
     */
    private int textureId = 0;
    /**
     * 顶点坐标
     */
    private float mVertex[] = {
        -1.0f,  1.0f,
        -1.0f, -1.0f,
        1.0f, 1.0f,
        1.0f,  -1.0f,
    };

    /**
     * 纹理坐标
     */
    private float[] mTexCoord = {
        0.0f, 0.0f,
        0.0f,  1.0f,
        1.0f,  0.0f,
        1.0f, 1.0f,
    };

    private SparseArray<boolean[]> mBools;
    private SparseArray<int[]> mInts;
    private SparseArray<float[]> mFloats;

    public Filter(Resources resources) {
        mResources = resources;
        initBuffer();
    }

    /**
     * 创建程序
     */
    public final void create() {
        onCreate();
    }

    /**
     * 设置宽高
     * @param width 宽度
     * @param height    高度
     */
    public final void setSize(int width, int height) {
        onSizeChanged(width, height);
    }

    /**
     * 绘制
     */
    public void draw() {
        onClear();
        onUseProgram();
        setExpandData();
        onBindTexture();
        onDraw();
    }

    public int getOutputTexture() {
        return -1;
    }

    /**
     * 实现此方法，完成程序的创建，可直接调用createProgram来实现
     */
    protected abstract void onCreate();

    /**
     * 宽高发生变化时调用该函数
     * @param width 宽度
     * @param height    高度
     */
    protected abstract void onSizeChanged(int width, int height);

    /**
     * 创建program程序
     * @param vertex    顶点程序字符串
     * @param fragment  片元程序字符串
     */
    protected final void createProgram(String vertex, String fragment) {
        mProgram = loadProgram(vertex,fragment);
        mHPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mHCoord = GLES20.glGetAttribLocation(mProgram, "vCoord");
        mHMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        mHTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
    }

    /**
     * 通过加载assets目录下的顶点和片元着色器的glsl文件创建program
     * @param vertex
     * @param fragment
     */
    protected final void createProgramByAssetsFile(String vertex, String fragment) {
        createProgram(getResources(mResources, vertex), getResources(mResources, fragment));
    }

    /**
     * Buffer初始化
     */
    protected void initBuffer() {
        ByteBuffer a = ByteBuffer.allocateDirect(32);
        a.order(ByteOrder.nativeOrder());
        mVerBuffer = a.asFloatBuffer();
        mVerBuffer.put(mVertex);
        mVerBuffer.position(0);
        ByteBuffer b = ByteBuffer.allocateDirect(32);
        b.order(ByteOrder.nativeOrder());
        mTexBuffer = b.asFloatBuffer();
        mTexBuffer.put(mTexCoord);
        mTexBuffer.position(0);
    }

    protected void onUseProgram() {
        GLES20.glUseProgram(mProgram);
    }

    /**
     * 启用顶点坐标和纹理坐标进行绘制
     */
    protected void onDraw() {
        GLES20.glEnableVertexAttribArray(mHPosition);
        GLES20.glVertexAttribPointer(mHPosition, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);
        GLES20.glEnableVertexAttribArray(mHCoord);
        GLES20.glVertexAttribPointer(mHCoord, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mHPosition);
        GLES20.glDisableVertexAttribArray(mHCoord);
    }

    /**
     * 清除画布
     */
    protected void onClear() {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * 设置其他扩展数据
     */
    protected void setExpandData() {
        GLES20.glUniformMatrix4fv(mHMatrix, 1, false, matrix, 0);
    }

    /**
     * 绑定默认纹理
     */
    protected void onBindTexture() {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + mTextureType);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getTextureId());
        GLES20.glUniform1i(mHTexture, mTextureType);
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
     * 通过路径加载Assets中的resources 的资源
     * @param resources
     * @param path
     * @return
     */
    public static String getResources(Resources resources, String path) {
        StringBuilder result = new StringBuilder();
        try{
            InputStream is = resources.getAssets().open(path);
            int ch;
            byte[] buffer = new byte[1024];
            while (-1 != (ch = is.read(buffer))) {
                result.append(new String(buffer, 0, ch));
            }
        }catch (Exception e) {
            return null;
        }
        return result.toString().replaceAll("\\r\\n", "\n");
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

    //// setter and getter ///

    public void setMatrix(float[] matrix) {
        this.matrix=matrix;
    }

    public float[] getMatrix() {
        return matrix;
    }

    public final void setTextureType(int type) {
        this.mTextureType =type;
    }

    public final int getTextureType() {
        return mTextureType;
    }

    public final int getTextureId() {
        return textureId;
    }

    public final void setTextureId(int textureId) {
        this.textureId = textureId;
    }

    public void setFlag(int flag) {
        this.mFlag = flag;
    }

    public int getFlag() {
        return mFlag;
    }

    public void setFloat(int type, float ... params) {
        if (mFloats == null) {
            mFloats = new SparseArray<>();
        }
        mFloats.put(type, params);
    }
    public void setInt(int type, int ... params) {
        if (mInts == null) {
            mInts = new SparseArray<>();
        }
        mInts.put(type, params);
    }
    public void setBool(int type, boolean ... params) {
        if (mBools == null) {
            mBools = new SparseArray<>();
        }
        mBools.put(type, params);
    }

    public boolean getBool(int type, int index) {
        if (mBools == null) return false;
        boolean[] b = mBools.get(type);
        return !(b == null || b.length <= index) && b[index];
    }

    public int getInt(int type, int index) {
        if (mInts == null) return 0;
        int[] b = mInts.get(type);
        if (b == null || b.length <= index) {
            return 0;
        }
        return b[index];
    }

    public float getFloat(int type, int index) {
        if (mFloats == null) return 0;
        float[] b = mFloats.get(type);
        if (b == null || b.length <= index) {
            return 0;
        }
        return b[index];
    }

}
