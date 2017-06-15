package com.cgfay.cain.camerasample.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.cgfay.cain.camerasample.camera2.Renderer;
import com.cgfay.cain.camerasample.camera2.TextureController;
import com.cgfay.cain.camerasample.detection.FaceEvent;
import com.cgfay.cain.camerasample.detection.StickerFaceDetection;
import com.cgfay.cain.camerasample.util.DisplayUtils;

import java.io.ByteArrayOutputStream;
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

    private Handler mHandler = null;
    private StickerFaceDetection mDetection;

    public KitkatCameraRenderer(Context context, int cameraID, TextureController controller) {
        this.mContext = context;
        this.mController = controller;
        this.mCameraID = cameraID;
        mHandler = new DetectHandler();
        mDetection = new StickerFaceDetection(mContext, mHandler);
    }

    @Override
    public void onDestroy() {

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

        Camera.Size size = getPerfectSize();
        setPictureSize(size.width, size.height);
        // 处理摄像头倒置的问题
        if (camerReverseIfNeeded(size)) {
            mController.setCameraReverse(true);
            mController.setDataSize(size.height, size.width);
        } else {
            mController.setCameraReverse(false);
            mController.setDataSize(size.width, size.height);
        }
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
        mHandler.sendEmptyMessage(FaceEvent.CAMERA_HAS_STARTED_PREVIEW);
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

    /**
     * 设置相机的分辨率(仅相机支持的分辨率)
     * @param width
     * @param height
     */
    private void setPictureSize(int width, int height) {
        if (mCamera != null) {
            Camera.Parameters param = mCamera.getParameters();
            param.setPictureSize(width, height);
            mCamera.setParameters(param);
        }
    }

    /**
     * 判断摄像头是否倒置
     * @param size
     * @return
     */
    private boolean camerReverseIfNeeded(Camera.Size size) {
        boolean reverse = false;
        int width = DisplayUtils.getScreenWidth(mContext);
        int height = DisplayUtils.getScreenHeight(mContext);
        // 判断当前的相机支持的分辨率宽高大小与屏幕宽高相反(宽比高大)，则表示相机倒置了
        if ((size.width > size.height && width < height)
                || (size.width < size.height && width > height)) {
            reverse = true;
        }
        return reverse;
    }

    /**
     * 停止人脸识别监听
     * 注意：takepicture 自身就已经调用stopFaceDetection了，再调用会跑出异常
     */
    private void stopFaceDetector() {
        if (mCamera.getParameters().getMaxNumDetectedFaces() > 0) {
            mCamera.setFaceDetectionListener(null);
            mCamera.stopFaceDetection();
        }
    }

    private class DetectHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FaceEvent.UPDATE_FACE_RECT:
                    Camera.Face[] faces = (Camera.Face[])msg.obj;
                    // 在对应的位置上设置绘制
                    mController.setDetectedFaces(faces);
                    break;

                case FaceEvent.CAMERA_HAS_STARTED_PREVIEW:
                    // 判断是否支持人脸识别
                    if (mCamera.getParameters().getMaxNumDetectedFaces() > 0) {
                        mCamera.setFaceDetectionListener(mDetection);
                        mCamera.startFaceDetection();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    }

}
