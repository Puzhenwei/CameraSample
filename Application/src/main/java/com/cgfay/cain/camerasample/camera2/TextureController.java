package com.cgfay.cain.camerasample.camera2;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.cgfay.cain.camerasample.filter.ClearFilter;
import com.cgfay.cain.camerasample.filter.Filter;
import com.cgfay.cain.camerasample.filter.GroupFilter;
import com.cgfay.cain.camerasample.filter.TextureFilter;
import com.cgfay.cain.camerasample.util.GLESUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

/**
 * 借助GLSurfaceView创建的GL环境，做渲染工作。
 * 不将内容渲染到GLSurfaceView的Surface上，
 * 而是将内容绘制到外部提供的Surface、SurfaceHolder或者SurfaceTexture上。
 */
public class TextureController implements GLSurfaceView.Renderer {

    private static final int DEFAULT_WIDTH = 720;
    private static final int DEFAULT_HEIGHT = 1280;

    private Object surface;

    private GLView mGLView;
    private Context mContext;

    private Renderer mRenderer;                                 //用户附加的Renderer或用来监听Renderer
    private TextureFilter mEffectFilter;                        //特效处理的Filter
    private GroupFilter mGroupFilter;                           //中间特效
    private Filter mShowFilter;                                //用来渲染输出的Filter
    private Point mDataSize;                                    //数据的大小
    private Point mWindowSize;                                  //输出视图的大小
    private AtomicBoolean isParamSet = new AtomicBoolean(false);
    private float[] SM = new float[16];                           //用于绘制到屏幕上的变换矩阵
    private int mShowType = GLESUtils.TYPE_CENTERCROP;          //输出到屏幕上的方式
    private int mDirectionFlag = -1;                               //AiyaFilter方向flag

    private float[] callbackOM = new float[16];                   //用于绘制回调缩放的矩阵

    //创建离屏buffer，用于最后导出数据
    private int[] mExportFrame = new int[1];
    private int[] mExportTexture = new int[1];

    private boolean isRecord=false;                             //录像flag
    private boolean isShoot=false;                              //一次拍摄flag
    private ByteBuffer[] outPutBuffer = new ByteBuffer[3];      //用于存储回调数据的buffer
    private FrameCallback mFrameCallback;                       //回调
    private int mFrameCallbackWidth, mFrameCallbackHeight;        //回调数据的宽高
    private int indexOutput = 0;                                //回调数据使用的buffer索引

    public TextureController(Context context) {
        this.mContext=context;
        init();
    }

    public void surfaceCreated(Object nativeWindow){
        this.surface=nativeWindow;
        mGLView.surfaceCreated(null);
    }

    public void surfaceChanged(int width,int height){
        this.mWindowSize.x = width;
        this.mWindowSize.y = height;

        // TODO 这里需要处理相机的排布问题
        mFrameCallbackWidth = width;
        mFrameCallbackHeight = height;
        mGLView.surfaceChanged(null, 0, width, height);
    }

    public void surfaceDestroyed(){
        mGLView.surfaceDestroyed(null);
    }

    public Object getOutput(){
        return surface;
    }

    private void init() {

        mGLView = new GLView(mContext);

        //避免GLView的attachToWindow和detachFromWindow崩溃
        new ViewGroup(mContext) {
            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
            }
        }.addView(mGLView);

        mEffectFilter = new TextureFilter(mContext.getResources());
        mShowFilter = new ClearFilter(mContext.getResources());
        mGroupFilter = new GroupFilter(mContext.getResources());

        // 设置默认的DateSize，DataSize
        // 由根据数据源的图像宽高进行设置
        mDataSize = new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        mWindowSize = new Point(DEFAULT_WIDTH, DEFAULT_HEIGHT);

    }

    public SurfaceTexture getTexture(){
        return mEffectFilter.getTexture();
    }

    public void setImageDirection(int flag){
        this.mDirectionFlag = flag;
    }

    public void setRenderer(Renderer renderer){
        mRenderer = renderer;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mEffectFilter.create();
        mGroupFilter.create();
        mShowFilter.create();
        if(!isParamSet.get()){
            if(mRenderer!=null){
                mRenderer.onSurfaceCreated(gl, config);
            }
            sdkParamSet();
        }
        calculateCallbackOM();
        mEffectFilter.setFlag(mDirectionFlag);

        deleteFrameBuffer();
        GLES20.glGenFramebuffers(1,mExportFrame,0);
        GLESUtils.genTexturesWithParameter(1, mExportTexture, 0, GLES20.GL_RGBA, mDataSize.x,
            mDataSize.y);
    }

    private void deleteFrameBuffer() {
        GLES20.glDeleteFramebuffers(1, mExportFrame, 0);
        GLES20.glDeleteTextures(1, mExportTexture, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLESUtils.getMatrix(SM,mShowType,
            mDataSize.x, mDataSize.y, width, height);
        mShowFilter.setSize(width, height);
        mShowFilter.setMatrix(SM);
        mGroupFilter.setSize(mDataSize.x, mDataSize.y);
        mEffectFilter.setSize(mDataSize.x, mDataSize.y);
        mShowFilter.setSize(mDataSize.x, mDataSize.y);
        if (mRenderer != null) {
            mRenderer.onSurfaceChanged(gl, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(isParamSet.get()){
            mEffectFilter.draw();
            mGroupFilter.setTextureId(mEffectFilter.getOutputTexture());
            mGroupFilter.draw();

            // 显示传入的texture上，一般是显示在屏幕上
            GLES20.glViewport(0,0,mWindowSize.x, mWindowSize.y);
            mShowFilter.setMatrix(SM);
            mShowFilter.setTextureId(mGroupFilter.getOutputTexture());
            mShowFilter.draw();

            if (mRenderer != null) {
                mRenderer.onDrawFrame(gl);
            }
            callbackIfNeeded();
        }
    }

    /**
     * 增加滤镜
     * @param filter 滤镜
     */
    public void addFilter(Filter filter){
        mGroupFilter.addFilter(filter);
    }

    /**
     * 移除某个滤镜
     * @param filter
     */
    public void removeFilter(Filter filter) {
        mGroupFilter.removeFilter(filter);
    }

    /**
     * 清除所有滤镜
     */
    public void clearAllFilters() {
        mGroupFilter.clearAll();
    }

    /**
     * 设置输入图像与输出视图大小不同时，图像的展示方式
     * @param type 展示方式，可选项为：
     *  {@link GLESUtils # TYPE_CENTERCROP}、{@link GLESUtils # TYPE_CENTERINSIDE}、
     *  {@link GLESUtils # TYPE_FITEND}、{@link GLESUtils # TYPE_FITSTART}、
     *  {@link GLESUtils # TYPE_FITXY}，与 {@link ImageView.ScaleType}对应
     */
    public void setShowType(int type){
        this.mShowType=type;
        if(mWindowSize.x>0&&mWindowSize.y>0){
            GLESUtils.getMatrix(SM,mShowType,
                mDataSize.x,mDataSize.y,mWindowSize.x,mWindowSize.y);
            mShowFilter.setMatrix(SM);
            mShowFilter.setSize(mWindowSize.x,mWindowSize.y);
        }
    }

    /**
     * 开始录制视频
     */
    public void startRecord() {
        isRecord = true;
    }

    /**
     * 停止录制视频
     */
    public void stopRecord() {
        isRecord = false;
    }

    /**
     * 拍照
     */
    public void takePhoto() {
        isShoot = true;
    }

    /**
     * 这是帧回调
     * @param width     宽度
     * @param height    高度
     * @param frameCallback 帧回调
     */
    public void setFrameCallback(int width, int height, FrameCallback frameCallback) {
        this.mFrameCallbackWidth = width;
        this.mFrameCallbackHeight = height;
        if (mFrameCallbackWidth > 0 && mFrameCallbackHeight > 0) {
            if (outPutBuffer != null) {
                outPutBuffer = new ByteBuffer[3];
            }
            calculateCallbackOM();
            this.mFrameCallback = frameCallback;
        } else {
            this.mFrameCallback = null;
        }
    }

    /**
     * 计算变换
     */
    private void calculateCallbackOM(){
        if (mFrameCallbackHeight > 0 && mFrameCallbackWidth > 0
                && mDataSize.x > 0 && mDataSize.y > 0) {
            // 计算输出的变换矩阵
            GLESUtils.getMatrix(callbackOM, GLESUtils.TYPE_CENTERCROP,
                    mDataSize.x, mDataSize.y, mFrameCallbackWidth, mFrameCallbackHeight);
            // 翻转
            GLESUtils.flip(callbackOM, false, true);
        }
    }

    /**
     * 设置SDK参数
     */
    private void sdkParamSet() {
        if(!isParamSet.get() && mDataSize.x > 0 && mDataSize.y > 0) {
            isParamSet.set(true);
        }
    }

    /**
     * 需要回调，则缩放图片到指定大小，读取数据并回调
     */
    private void callbackIfNeeded() {
        if (mFrameCallback != null && (isRecord || isShoot)) {
            indexOutput = indexOutput++ >= 2 ? 0 : indexOutput;
            if (outPutBuffer[indexOutput] == null) {
                outPutBuffer[indexOutput] = ByteBuffer.allocate(mFrameCallbackWidth *
                        mFrameCallbackHeight * 4);
            }
            GLES20.glViewport(0, 0, mFrameCallbackWidth, mFrameCallbackHeight);
            GLESUtils.bindFrameTexture(mExportFrame[0], mExportTexture[0]);
            mShowFilter.setMatrix(callbackOM);
            mShowFilter.draw();
            frameCallback();
            isShoot = false;
            GLESUtils.unBindFrameBuffer();
            mShowFilter.setMatrix(SM);
        }
    }

    /**
     * 帧画面回调
     */
    private void frameCallback() {
        GLES20.glReadPixels(0, 0, mFrameCallbackWidth, mFrameCallbackHeight,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, outPutBuffer[indexOutput]);
        mFrameCallback.onFrame(outPutBuffer[indexOutput].array(), System.currentTimeMillis());
    }

    /**
     * 创建
     * @param width 宽度
     * @param height  高度
     */
    public void create(int width, int height) {
        mGLView.attachedToWindow();
        surfaceCreated(surface);
        surfaceChanged(width,height);
    }

    /**
     * 销毁
     */
    public void destroy() {
        if (mRenderer != null) {
            mRenderer.onDestroy();
        }
        mGLView.surfaceDestroyed(null);
        mGLView.detachedFromWindow();
        mGLView.clear();
    }

    /**
     * 请求渲染
     */
    public void requestRender(){
        mGLView.requestRender();
    }

    /**
     * 暂停
     */
    public void onPause(){
        mGLView.onPause();
    }

    /**
     * 重启
     */
    public void onResume(){
        mGLView.onResume();
    }

    /** 自定义GLSurfaceView，暴露出onAttachedToWindow
     * 方法及onDetachedFromWindow方法，取消holder的默认监听
     * onAttachedToWindow及onDetachedFromWindow必须保证view
     * 存在Parent */
    private class GLView extends GLSurfaceView{

        public GLView(Context context) {
            super(context);
            init();
        }

        private void init(){
            getHolder().addCallback(null);
            setEGLWindowSurfaceFactory(new EGLWindowSurfaceFactory() {
                @Override
                public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig
                    config, Object window) {
                    return egl.eglCreateWindowSurface(display,config,surface,null);
                }

                @Override
                public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
                    egl.eglDestroySurface(display, surface);
                }
            });
            setEGLContextClientVersion(2);
            setRenderer(TextureController.this);
            setRenderMode(RENDERMODE_WHEN_DIRTY);
            setPreserveEGLContextOnPause(true);
        }

        public void attachedToWindow(){
            super.onAttachedToWindow();
        }

        public void detachedFromWindow(){
            super.onDetachedFromWindow();
        }

        public void clear() {

        }
    }


    //// setter and getter ////

    /**
     * 设置数据的Size
     * @param width
     * @param height
     */
    public void setDataSize(int width, int height) {
        mDataSize.x = width;
        mDataSize.y = height;
    }

    public void setDataSize(Point size) {
        mDataSize = size;
    }

    /**
     *  获取数据的Size
     * @return
     */
    public Point getDataSize() {
        return mDataSize;
    }

    /**
     * 设置windows窗口大小
     * @param width   宽度
     * @param height  高度
     */
    public void setWindowSize(int width, int height) {
        mWindowSize.x = width;
        mWindowSize.y = height;
    }

    public void setWindowSize(Point size) {
        mWindowSize = size;
    }

    /**
     * 获取Window的Size
     * @return
     */
    public Point getWindowSize() {
        return mWindowSize;
    }

}
