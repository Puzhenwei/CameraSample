
package com.cgfay.cain.camerasample.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private ICamera mCamera;
    private CameraRanderer mCameraRanderer;
    private int cameraId = 1;

    private Runnable mRunnable;

    public CameraView(Context context) {
        this(context,null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        mCamera = new KitkatCamera(context);
        mCameraRanderer = new CameraRanderer(getResources());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraRanderer.onSurfaceCreated(gl,config);
        if (mRunnable != null) {
            mRunnable.run();
            mRunnable = null;
        }
        mCamera.open(cameraId);
        mCameraRanderer.setCameraId(cameraId);
        Point point = mCamera.getPreviewSize();
        mCameraRanderer.setDataSize(point.x, point.y);
        mCamera.setPreviewTexture(mCameraRanderer.getSurfaceTexture());
        mCameraRanderer.getSurfaceTexture()
                .setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
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
        mCameraRanderer.setViewSize(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mCameraRanderer.onDrawFrame(gl);
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
}
