package com.cgfay.cain.camerasample.camera;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.util.Arrays;


public class LollipopCamera implements ICamera {

    private static final String TAG = "LollipopCamera";

    // 摄像头的状态
    private static final int STATE_PREVIEW = 0x01;
    private static final int STATE_WAITING_CAPTURE = 0x02;
    private int mState;

    private Context mContext;
    private Config mConfig;

    private Surface mSurface;

    private int mCameraId;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mSession;
    private Handler mHandler;
    private PowerManager.WakeLock mCameraOpenCloseLock;

    private CaptureRequest.Builder mPreviewBuilder;

    // 摄像头是否打开
    private boolean isCameraOpen = false;


    public LollipopCamera(Context context) {
        mContext = context;
        mConfig = new Config();
        mConfig.minPictureWidth = 720;
        mConfig.minPreviewWidth = 720;
        mConfig.rate = 1.778f;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        PowerManager manager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mCameraOpenCloseLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Camera2Lock");
    }

    @Override
    public boolean open(int cameraID) {
        mCameraId = cameraID;
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        try {
            mCameraOpenCloseLock.acquire();
            mCameraManager.openCamera(mCameraId + "", DeviceStateCallback, mHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG,"open camera failed: " + e.getMessage());
            mCameraOpenCloseLock.release();
            return false;
        }
        return true;
    }


    private CameraDevice.StateCallback DeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            mCameraOpenCloseLock.release();
            isCameraOpen = true;
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            // 关闭摄像头
            isCameraOpen = false;
            if (mCameraDevice != null) {
                mCameraDevice.close();
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Toast.makeText(mContext, "摄像头打开失败", Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    public void setConfig(Config config) {
        mConfig = config;
    }

    @Override
    public boolean startPreview() {
        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(mSurface);
            mState = STATE_PREVIEW;
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface),
                    mSessionPreviewStateCallback, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private CameraCaptureSession.StateCallback
            mSessionPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mSession = session;

            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            try {
                session.setRepeatingRequest(mPreviewBuilder.build(),
                        mSessionCaptureCallBack, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    private CameraCaptureSession.CaptureCallback
            mSessionCaptureCallBack = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

    };



    @Override
    public boolean switchTo(int cameraID) {
        return false;
    }

    @Override
    public void takePhoto(Camera.AutoFocusCallback callback) {
        mState = STATE_WAITING_CAPTURE;
        try {
            mSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallBack, mHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopPreview() {

    }

    @Override
    public boolean close() {
        return false;
    }

    @Override
    public void setPreviewTexture(SurfaceTexture texture) {
        mSurface = new Surface(texture);
    }

    @Override
    public Point getPreviewSize() {
        return new Point(720, 1280);
    }

    @Override
    public Point getPictureSize() {
        return new Point(720, 1280);
    }

    @Override
    public void setOnPreviewFrameCallback(PreviewFrameCallback callback) {

    }


}
