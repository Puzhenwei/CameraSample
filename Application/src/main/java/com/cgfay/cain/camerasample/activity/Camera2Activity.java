
package com.cgfay.cain.camerasample.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
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
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cgfay.cain.camerasample.R;
import com.cgfay.cain.camerasample.camera2.FrameCallback;
import com.cgfay.cain.camerasample.camera2.Renderer;
import com.cgfay.cain.camerasample.camera2.TextureController;
import com.cgfay.cain.camerasample.filter.WaterMaskFilter;
import com.cgfay.cain.camerasample.data.Facer;
import com.cgfay.cain.camerasample.data.Frame;
import com.cgfay.cain.camerasample.data.Organ;
import com.cgfay.cain.camerasample.data.Sticker;
import com.cgfay.cain.camerasample.task.JsonParser;
import com.cgfay.cain.camerasample.task.MediaSaver;
import com.cgfay.cain.camerasample.task.MediaSaverTask;
import com.cgfay.cain.camerasample.util.DisplayUtils;
import com.cgfay.cain.camerasample.util.FileUtils;
import com.cgfay.cain.camerasample.util.GLESUtils;
import com.cgfay.cain.camerasample.util.PermissionUtils;
import com.cgfay.cain.camerasample.util.SDCardUtils;
import com.cgfay.cain.camerasample.util.StickerUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


import static android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW;


public class Camera2Activity extends AppCompatActivity implements FrameCallback {

    private static final String TAG = "Camsera2Activity";

    private static final String CAMERA_PATH = "/DCIM/Camera/";

    private SurfaceView mSurfaceView;
    private Button mBtnViewPhoto;
    private Button mBtnTake;
    private Button mBtnSwitchCamera;
    private TextureController mController;
    private Renderer mRenderer;
    private int cameraId = 1;
    private int mWidth;
    private int mHeight;

    private MediaSaver mMediaSaver;
    // 用于标志保存照片的队列占用达到最大值
    private volatile boolean mMediaMemoryFull;

    // 路径列表
    private String[] folderPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtils.askPermission(this, new String[]{Manifest.permission.CAMERA, Manifest
            .permission.WRITE_EXTERNAL_STORAGE}, 10, initViewRunnable);
    }

    private Runnable initViewRunnable = new Runnable() {
        @Override
        public void run() {

            // 根据SDK版本判断使用哪一版的渲染器
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRenderer = new Camera2Renderer();
            }else{
                mRenderer = new Camera1Renderer();
            }
            // 获取解压后的路径列表
            folderPath = getIntent().getStringArrayExtra("folderPath");

            setContentView(R.layout.activity_camera2);

            mSurfaceView = (SurfaceView)findViewById(R.id.view_camera);
            mBtnViewPhoto = (Button) findViewById(R.id.btn_view_photo);
            mBtnTake = (Button) findViewById(R.id.btn_take);
            mBtnSwitchCamera = (Button) findViewById(R.id.btn_switch);

            // 查看照片
            mBtnViewPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewPhoto();
                }
            });

            // 拍照
            mBtnTake.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mController != null) {
                        mController.takePhoto();
                    }
                }
            });

            // 切换摄像头
            mBtnSwitchCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchCamera();
                }
            });

            mController = new TextureController(Camera2Activity.this);
            mController.setShowType(GLESUtils.TYPE_CENTERCROP);

            // 控制帧回调
            mController.setFrameCallback(720, 1280, Camera2Activity.this);
            // 根据路径列表获取文件列表
            StickerUtils.scanPackages(Camera2Activity.this, mController, folderPath);

            // 初始化SurfaceView参数
            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mController.surfaceCreated(holder);
                    mController.setRenderer(mRenderer);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder,
                                           int format,
                                           int width,
                                           int height) {
                    mWidth = width;
                    mHeight = height;
                    mController.surfaceChanged(width, height);
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mController.surfaceDestroyed();
                }
            });

            // 多媒体存储器初始化，主要用于存储图片和视频
            mMediaSaver = new MediaSaverTask(getContentResolver());
            mMediaMemoryFull = false;
            mMediaSaver.setQueueListener(new MediaSaver.QueueListener() {
                @Override
                public void onQueueStatus(boolean full) {
                    // 保存图片任务使用的内存达到最大值时，主要是连拍照片时，需要维持一个队列
                    if (full) {
                        mMediaMemoryFull = true;
                    } else {
                        mMediaMemoryFull = false;
                    }
                }
            });
            Log.d(TAG, "path : " + (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()+ File.separator + "Camera"));
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(requestCode == 10,
                grantResults, initViewRunnable, new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Camera2Activity.this,
                            "没有获得必要的权限", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mController != null) {
            mController.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mController != null) {
            mController.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mController != null) {
            mController.destroy();
        }
    }

    @Override
    public void onFrame(final byte[] bytes, long time) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO 这里的bytes数据可以直接存放而不用转成Bitmap再做，迟点修改
                Bitmap bitmap = Bitmap.createBitmap(720,1280, Bitmap.Config.ARGB_8888);
                ByteBuffer b = ByteBuffer.wrap(bytes);
                bitmap.copyPixelsFromBuffer(b);
                saveBitmap(bitmap);
                bitmap.recycle();
            }
        }).start();
    }

    /**
     * 查看照片
     */
    private void viewPhoto() {

    }

    /**
     * 保存图片
     * @param b bitmap数据
     */
    private void saveBitmap(Bitmap b) {
        // TODO 改写保存照片的代码，使用lruCache和队列防止OOM

        String path =  SDCardUtils.getInnerSDCardPath()+ CAMERA_PATH;
        File folder = new File(path);
        if (!folder.exists() && !folder.mkdirs()){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(Camera2Activity.this, "无法保存照片", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        long dataTake = System.currentTimeMillis();
        final String jpegName = path + dataTake + ".jpg";
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Camera2Activity.this, PhotoViewActivity.class);
                intent.putExtra(PhotoViewActivity.FILE_NAME, jpegName);
                startActivity(intent);
            }
        });
    }

    /**
     * 切换摄像头
     */
    private void switchCamera() {

    }

    /**
     * 旧版camera 渲染器
     */
    private class Camera1Renderer implements Renderer {

        private Camera mCamera;

        @Override
        public void onDestroy() {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
            mCamera = Camera.open(cameraId);
            mController.setImageDirection(cameraId);
            Camera.Size size = mCamera.getParameters().getPreviewSize();
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
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }

    }


    /**
     * Android 5.0 以后的渲染器
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class Camera2Renderer implements Renderer {

        CameraDevice mDevice;
        CameraManager mCameraManager;
        private HandlerThread mThread;
        private Handler mHandler;
        private Size mPreviewSize;

        Camera2Renderer() {
            mCameraManager = (CameraManager)getSystemService(CAMERA_SERVICE);
            mThread = new HandlerThread("camera2 ");
            mThread.start();
            mHandler = new Handler(mThread.getLooper());
        }

        @Override
        public void onDestroy() {
            if(mDevice!=null){
                mDevice.close();
                mDevice=null;
            }
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            try {
                if(mDevice!=null){
                    mDevice.close();
                    mDevice=null;
                }
                CameraCharacteristics c=mCameraManager.getCameraCharacteristics(cameraId+"");
                StreamConfigurationMap map=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes=map.getOutputSizes(SurfaceHolder.class);
                //自定义规则，选个大小
                mPreviewSize=sizes[0];
                mController.setDataSize(mPreviewSize.getHeight(),mPreviewSize.getWidth());
                mCameraManager.openCamera(cameraId + "", new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(CameraDevice camera) {
                        mDevice = camera;
                        try {
                            Surface surface = new Surface(mController.getTexture());
                            final CaptureRequest.Builder builder = mDevice.createCaptureRequest(TEMPLATE_PREVIEW);
                            builder.addTarget(surface);
                            mController.getTexture().setDefaultBufferSize(mPreviewSize.getWidth(),
                                    mPreviewSize.getHeight());
                            mDevice.createCaptureSession(Arrays.asList(surface),
                                    new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(CameraCaptureSession session) {
                                    try {
                                        session.setRepeatingRequest(builder.build(),
                                                new CameraCaptureSession.CaptureCallback() {
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
                                        }, mHandler);
                                    } catch (CameraAccessException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onConfigureFailed(CameraCaptureSession session) {

                                }
                            },mHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDisconnected(CameraDevice camera) {
                        mDevice=null;
                    }

                    @Override
                    public void onError(CameraDevice camera, int error) {

                    }
                }, mHandler);
            } catch (SecurityException | CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {

        }
    }
}
