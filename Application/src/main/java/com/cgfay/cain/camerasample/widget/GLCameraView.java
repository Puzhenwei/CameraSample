package com.cgfay.cain.camerasample.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class GLCameraView extends GLSurfaceView
        implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    private static final String TAG = "CameraView";

    private Context mContext;

    private Callback mCallback;

    SurfaceTexture mSurface;
    int mTextureID = -1;
    GLCameraDrawer mGLCameraDrawer;

    private float[] mtx = new float[16];

    public GLCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        this.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mTextureID = createTextureID();
        mSurface = new SurfaceTexture(mTextureID);
        mSurface.setOnFrameAvailableListener(this);
        mGLCameraDrawer = new GLCameraDrawer(mTextureID);
        // 打开相机回调
        if (mCallback != null) {
            mCallback.surfaceCreated(mSurface);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        // 相机预览接口回调
        if (mCallback != null) {
            mCallback.surfaceChanged(mSurface, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BITS);
        mSurface.updateTexImage();
        mSurface.getTransformMatrix(mtx);
        mGLCameraDrawer.onDraw(mtx);
    }

    @Override
    public void onPause() {
        super.onPause();
        // 停止相机回调
    }

    private int createTextureID() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurface;
    }


    public interface Callback {
        void surfaceCreated(SurfaceTexture surfaceTexture);
        void surfaceChanged(SurfaceTexture surfaceTexture, int width, int height);
    }

    public void addCallback(Callback callback) {
        mCallback = callback;
    }

}
