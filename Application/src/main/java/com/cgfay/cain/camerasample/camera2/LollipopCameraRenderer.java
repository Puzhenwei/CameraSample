package com.cgfay.cain.camerasample.camera2;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.cgfay.cain.camerasample.util.DisplayUtils;

import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.content.Context.CAMERA_SERVICE;
import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;



@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class LollipopCameraRenderer implements Renderer {

    private final String TAG = "LollipopCameraRenderer";

    private Context mContext;
    private TextureController mController;
    private int mCameraID;
    private CameraDevice mDevice;
    private CameraManager mCameraManager;
    private Surface mSurface;
    private CaptureRequest.Builder builder;

    private HandlerThread mThread;
    private Handler mHandler;
    private Size mPreviewSize;

    private int mDefaultWidth;
    private int mDefaultHeight;

    public LollipopCameraRenderer(Context context, int cameraID, TextureController controller) {
        mContext = context;
        mCameraID = cameraID;
        mController = controller;
        mCameraManager = (CameraManager)context.getSystemService(CAMERA_SERVICE);
        mThread = new HandlerThread("camera2 ");
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
    }

    @Override
    public void onDestroy() {
        closeCamera();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
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

    // 关闭摄像头
    @Override
    public void closeCamera() {
        if (mDevice != null) {
            mDevice.close();
            mDevice = null;
        }
    }

    // 打开摄像头
    @Override
    public void openCamera() {
        try {
            if(mDevice != null){
                mDevice.close();
                mDevice = null;
            }
            CameraCharacteristics characteristics
                    = mCameraManager.getCameraCharacteristics(mCameraID + "");
            StreamConfigurationMap map
                    = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = getPerfectSize(map.getOutputSizes(SurfaceHolder.class));

            if (camerReverseIfNeeded(mPreviewSize)) {
                Log.d(TAG, "reverse = true");
                mController.setCameraReverse(true);
                mController.setDataSize(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            } else {
                Log.d(TAG, "reverse = false");
                mController.setCameraReverse(false);
                mController.setDataSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            }
            mCameraManager.openCamera(mCameraID + "", mDeviceStateCallback, mHandler);
        } catch (SecurityException | CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setDefaultPreviewSize(int width, int height) {
        mDefaultWidth = width;
        mDefaultHeight = height;
    }

    private Size getPerfectSize(Size[] sizes) {
        if (mDefaultWidth == 0 || mDefaultHeight == 0) {
            return sizes[0];
        }

        int width = mDefaultWidth;
        int height = mDefaultHeight;
        Size size = sizes[0];

        // 取出相机支持的恰好大于或等于默认分辨率的分辨率
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i].getWidth() >= mDefaultWidth && sizes[i].getHeight() >= mDefaultHeight) {
                if (sizes[i].getWidth() <= width || sizes[i].getHeight() <= height) {
                    width = sizes[i].getWidth();
                    height = sizes[i].getHeight();
                    size = sizes[i];
                }
            }
        }

        return size;
    }

    /**
     * 处理摄像头倒置
     * @param size
     * @return
     */
    private boolean camerReverseIfNeeded(Size size) {
        boolean reverse = false;
        int width = DisplayUtils.getScreenWidth(mContext);
        int height = DisplayUtils.getScreenHeight(mContext);
        // 判断当前的相机支持的分辨率宽高大小与屏幕宽高相反(宽比高大)，则表示相机倒置了
        if ((size.getWidth() > size.getHeight() && width < height)
                || (size.getWidth() < size.getHeight() && width > height)) {
            reverse = true;
        }
        return reverse;
    }

    /// ------------------ 相机回调 ------------------///

    // 设备状态回调
    private final CameraDevice.StateCallback
            mDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mDevice = camera;
            try {
                mSurface = new Surface(mController.getTexture());
                builder = mDevice.createCaptureRequest(TEMPLATE_PREVIEW);
                builder.addTarget(mSurface);

                mController.getTexture().setDefaultBufferSize(mPreviewSize.getWidth(),
                        mPreviewSize.getHeight());
                mDevice.createCaptureSession(Arrays.asList(mSurface),
                        mCaptureStateCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            mDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {

        }
    };

    // 拍照状态回调
    final CameraCaptureSession.StateCallback
            mCaptureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                session.setRepeatingRequest(builder.build(), mCaptureCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {

        }
    };

    // 拍照回调
    private final CameraCaptureSession.CaptureCallback
            mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                CaptureRequest request,
                CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                CaptureRequest request,
                TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            mController.requestRender();
        }
    };
}
