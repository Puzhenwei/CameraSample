package com.cgfay.cain.camerasample.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;

import com.cgfay.cain.camerasample.camera2.Renderer;
import com.cgfay.cain.camerasample.camera2.TextureController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class KitkatCameraRenderer implements Renderer {

    private final String TAG = "KitkatCameraRenderer";

    private Context mContext;
    private Camera mCamera;
    private int mCameraID;
    private TextureController mController;
    private List<Camera.Size> mSupportSizes = new ArrayList<>();
    private int mDefaultWidth;
    private int mDefaultHeight;

    public KitkatCameraRenderer(Context context, int cameraID, TextureController controller) {
        this.mContext = context;
        this.mController = controller;
        this.mCameraID = cameraID;
    }

    @Override
    public void onDestroy() {
        closeCamera();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        closeCamera();
        openCamera();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }

    @Override
    public void switchCamera(int cameraID) {
        if (mCameraID == cameraID) {
            return;
        }
        mCameraID = cameraID;
        closeCamera();
        openCamera();
    }

    @Override
    public void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void openCamera() {
        mCamera = Camera.open(mCameraID);
        mController.setImageDirection(mCameraID);

        setCameraDisplayOrientation((Activity) mContext, mCameraID, mCamera);
        Camera.Size size = getPerfectSize();
        Log.d(TAG, "width: " + size.width + ", height: " + size.height);
        setPictureSize(size.width, size.height);
        mController.setDataSize(size.height, size.width);
        try {
            mCamera.setPreviewTexture(mController.getTexture());
            mController.getTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    mController.requestRender();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    public void setDefaultPreviewSize(int width, int height) {
        mDefaultWidth = width;
        mDefaultHeight = height;
    }

    /**
     * 获取当前分辨率最佳Size
     * @return
     */
    private Camera.Size getPerfectSize() {

        // 如果没有设置默认分辨率，则使用相机默认返回的分辨率
        if (mDefaultWidth == 0 || mDefaultHeight == 0) {
            return mCamera.getParameters().getPreviewSize();
        }

        // 获取相机支持的所有分辨率
        mSupportSizes.clear();
        mSupportSizes.addAll(mCamera.getParameters().getSupportedPreviewSizes());
        int width = mDefaultWidth;
        int height = mDefaultHeight;
        Camera.Size result = mCamera.getParameters().getPreviewSize();
        // 取出相机支持的恰好大于或等于默认分辨率的分辨率
        for (Camera.Size size : mSupportSizes) {
            if (size.width >= mDefaultWidth && size.height >= mDefaultHeight) {
                if (size.width <= width || size.height <= height) {
                    width = size.width;
                    height = size.height;
                    result = size;
                }
            }
        }
        return result;
    }

    // 设置相机的分辨率(仅相机支持的分辨率)
    private void setPictureSize(int width, int height) {
        if (mCamera != null) {
            Camera.Parameters param = mCamera.getParameters();
            param.setPictureSize(width, height);
            mCamera.setParameters(param);
        }
    }

    /**
     * 这里主要是解决摄像头倒置的情况
     * @param activity
     * @param cameraId
     * @param camera
     */
    public void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}
