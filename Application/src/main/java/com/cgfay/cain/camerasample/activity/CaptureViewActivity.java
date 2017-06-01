package com.cgfay.cain.camerasample.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.cgfay.cain.camerasample.R;
import com.cgfay.cain.camerasample.util.DisplayUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CaptureViewActivity extends AppCompatActivity {

    private static final String TAG = "SurfaceCapture";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 270);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 90);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private Button mTakePhoto;
    private ImageView mImageShow;
    private SurfaceView mCameraView;

    private SurfaceHolder mSurfaceHolder;

    private CameraManager mCameraManager;
    private Handler childHandler, mainHandler;
    // 摄像头id， 0为后摄像头，1为前置摄像头
    private String mCameraID;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);

        mCameraView = (SurfaceView) findViewById(R.id.view_camera);
        mImageShow = (ImageView) findViewById(R.id.iv_show);

        mTakePhoto = (Button) findViewById(R.id.btn_take);
        mTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        mSurfaceHolder = mCameraView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera2();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    CaptureViewActivity.this.mCameraDevice = null;
                }
            }
        });
    }


    /**
     * 初始化摄像头
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCamera2() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());
        // Nexus 5X 由于排线接反了，导致相机反过来了，只能软件层面解决
        // 默认情况下，LENS_FACING_BACK 表示后置摄像头, LENS_FACING_FRONT表示前置摄像头
        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;
        mImageReader = ImageReader.newInstance(DisplayUtils.getScreenWidth(CaptureViewActivity.this),
                DisplayUtils.getScreenHeight(CaptureViewActivity.this), ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
//                mCameraDevice.close();
                mCameraView.setVisibility(View.GONE);
                mImageShow.setVisibility(View.VISIBLE);
                mTakePhoto.setVisibility(View.GONE);
                // 获取拍照照片数据
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                // 由缓冲区写入字节数组
                buffer.get(bytes);
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    mImageShow.setImageBitmap(bitmap);
                }
            }
        }, mainHandler);

        // 获取摄像头管理器
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                // 打开摄像头
                mCameraManager.openCamera(mCameraID, mStateCallback, mainHandler);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[] {android.Manifest.permission.CAMERA}, 1);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 摄像头监听
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        // 打开摄像头
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            // 开启预览
            takePreview();
        }

        // 关闭摄像头
        @Override
        public void onDisconnected(CameraDevice camera) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                CaptureViewActivity.this.mCameraDevice = null;
            }
        }

        // 发生错误
        @Override
        public void onError(CameraDevice camera, int error) {
            Toast.makeText(CaptureViewActivity.this, "摄像头打开失败", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 预览
     */
    private void takePreview() {
        try {
            final CaptureRequest.Builder builder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mSurfaceHolder.getSurface());
            mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(), mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    // 当摄像头准备好时开始显示预览
                    mCameraCaptureSession = session;
                    try {
                        // 自动对焦
                        builder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // 打开闪光灯
                        builder.set(CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        // 显示预览
                        CaptureRequest request = builder.build();
//                        mCameraCaptureSession.setRepeatingRequest(request, mCaptureCallback, childHandler);
                        mCameraCaptureSession.setRepeatingRequest(request, null, childHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 拍照
     */
    private void takePhoto() {
        if (mCameraDevice == null) {
            return;
        }
        // 创建拍照需要的CaptureRequest.Builder;
        final CaptureRequest.Builder builder;
        try {
            builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(mImageReader.getSurface());
            // 自动对焦
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 打开闪光灯
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            builder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            // 拍照
            CaptureRequest request = builder.build();
            mCameraCaptureSession.capture(request, mCaptureCallback, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * 拍照过程回调
     */
    CameraCaptureSession.CaptureCallback
            mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session,
                                     CaptureRequest request,
                                     long timestamp,
                                     long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
            Log.d(TAG, "onCaptureStarted:" + " timestamp: " + timestamp + ", frameNumber:" + frameNumber);
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            Log.d(TAG, "onCaptureProgressed:" + " partialResult: " + partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.d(TAG, "onCaptureCompleted:" + " TotalCaptureResult: " + result);
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session,
                                    CaptureRequest request,
                                    CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Log.d(TAG, "onCaptureFailed:" + " CaptureFailure: " + failure);
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // 打开摄像头
            try {
                mCameraManager.openCamera(mCameraID, mStateCallback, mainHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onBackPressed() {
        // 判断是否显示拍摄到的照片预览页面
        if (mImageShow != null
                && mImageShow.getVisibility() == View.VISIBLE) {
            mImageShow.setVisibility(View.GONE);
            mCameraView.setVisibility(View.VISIBLE);
            mTakePhoto.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }
    }
}
