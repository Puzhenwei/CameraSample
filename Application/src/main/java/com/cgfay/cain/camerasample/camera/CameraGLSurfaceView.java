
package com.cgfay.cain.camerasample.camera;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import com.cgfay.cain.camerasample.R;
import com.cgfay.cain.camerasample.filter.ClearFilter;
import com.cgfay.cain.camerasample.filter.Filter;
import com.cgfay.cain.camerasample.filter.GroupFilter;
import com.cgfay.cain.camerasample.filter.StickerFilter;
import com.cgfay.cain.camerasample.filter.TextureFilter;
import com.cgfay.cain.camerasample.util.GLESUtils;
import com.cgfay.cain.camerasample.util.StickerUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CameraGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private static final String TAG = "CameraView";

    private ICamera mCamera;
    private int cameraId = 1;
    private Runnable mRunnable;

    private float[] matrix = new float[16];
    private SurfaceTexture mSurfaceTexture;
    private int width, height;
    private int dataWidth, dataHeight;
    private Filter mFilter;
    private GroupFilter mGroupFilter;
    private TextureFilter mEffectFilter;

    // 是否允许人脸识别
    private boolean mEnableDetection = true;
    // 是否绘制贴纸
    private volatile boolean mDrawStriker = false;
    // 用于标志其他特效存在时是否允许绘制贴纸
    private volatile boolean mEnableSticker = true;


    public CameraGLSurfaceView(Context context) {
        this(context,null);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        mCamera = new KitkatCamera(context, this);
        mEffectFilter = new TextureFilter(context.getResources());
        mFilter = new ClearFilter(context.getResources());
        mGroupFilter = new GroupFilter(context.getResources());
        StickerFilter filter = new StickerFilter(context.getResources());
        filter.setWaterMask(BitmapFactory.decodeResource(context.getResources(), R.mipmap.huaji));
        filter.setSubFilterType(StickerUtils.STICKER_HEAD);
        mGroupFilter.addFilter(filter);
        this.getHolder().addCallback(this);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mEffectFilter.create();
        mGroupFilter.create();
        mFilter.create();
        mSurfaceTexture = mEffectFilter.getTexture();
        mSurfaceTexture.setOnFrameAvailableListener(onFrameAvailableListener);
        if (mRunnable != null) {
            mRunnable.run();
            mRunnable = null;
        }
        mCamera.open(cameraId);
        Point point = mCamera.getPreviewSize();
        dataWidth = point.x;
        dataHeight = point.y;
        mCamera.setPreviewTexture(mSurfaceTexture);
        mCamera.startPreview();
    }

    public void switchCamera(){
        mRunnable = new Runnable() {
            @Override
            public void run() {
                cameraId = (cameraId == 1) ? 0 : 1;
                mCamera.switchTo(cameraId);
            }
        };
        onPause();
        onResume();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
        onFilterSizeChange(width, height);
    }


    private void onFilterSizeChange(int width, int height) {
        GLESUtils.getMatrix(matrix, GLESUtils.TYPE_CENTERCROP,
                dataWidth, dataHeight, width, height);
        mFilter.setSize(width, height);
        mFilter.setMatrix(matrix);
        mGroupFilter.setSize(width, height);
        mEffectFilter.setSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mSurfaceTexture == null) {
            return;
        }
        mSurfaceTexture.updateTexImage();
        mEffectFilter.draw();

        if (mEnableSticker && mDrawStriker) {
            mGroupFilter.setTextureId(mEffectFilter.getOutputTexture());
            mGroupFilter.draw();
            mFilter.setTextureId(mGroupFilter.getOutputTexture());
            mFilter.draw();
        } else {
            mFilter.setTextureId(mEffectFilter.getOutputTexture());
            mFilter.draw();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mCamera.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCamera != null) {
            mCamera.open(cameraId);
        }
    }

    // 拍照
    public void takePhoto(Camera.AutoFocusCallback callback) {
        if (mCamera != null) {
            mCamera.takePhoto(callback);
        }
    }

    private SurfaceTexture.OnFrameAvailableListener
            onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            requestRender();
        }
    };


    /**
     * 设置人脸的位置信息
     * @param faces
     */
    public void setDetectedFaces(Camera.Face[] faces) {
        if (faces == null || faces.length < 1) {
            mDrawStriker = false;
            return;
        }
        mDrawStriker = true;
        updateStickerFilterPosition(faces);
        requestRender();
    }

    /**
     * 更新Sticker过滤器的位置
     */
    private void updateStickerFilterPosition(Camera.Face[] faces) {
        // TODO 目前仅实现处理存在一个人脸的情况, 多个人脸的情况需要对GroupFilter进行改造
        Camera.Face face = faces[0];
        if (mGroupFilter.getFilterQueue().isEmpty() && mGroupFilter.getFilters().isEmpty()) {
            return;
        }

        // 计算filter的位置
        for (Filter filter : mGroupFilter.getFilters()) {
            if (filter instanceof StickerFilter) {
                calculateStickerPosition(face, (StickerFilter) filter);
            }
        }
    }

    private void calculateStickerPosition(Camera.Face face, StickerFilter filter) {

        Rect rect = face.rect;
        Log.d(TAG, "left " + rect.left + ", top = " + rect.top
                + ", right = " + rect.right + ", bottom = " + rect.bottom);
        switch (filter.getSubFilterType()) {
            case StickerUtils.STICKER_HEAD:
                filter.setPosition(rect.centerX() - filter.getWidth() / 2,
                        rect.top - filter.getHeight(),
                        filter.getWidth(), filter.getHeight());
                break;

            case StickerUtils.STICKER_NOSE:
                filter.setPosition(rect.centerX() - filter.getWidth() / 2,
                        rect.centerY() - filter.getHeight() / 2,
                        filter.getWidth(), filter.getHeight());
                break;

            case StickerUtils.STICKER_FRAME:
                filter.setPosition(0, 0, filter.getWidth(), filter.getHeight());
                break;

            case StickerUtils.STICKER_FOREGROUND:
                filter.setPosition(0, dataHeight - filter.getHeight(),
                        filter.getWidth(), filter.getHeight());
                break;
        }
    }

}
