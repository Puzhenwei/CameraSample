
package com.cgfay.cain.camerasample.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cgfay.cain.camerasample.R;
import com.cgfay.cain.camerasample.camera.KitkatCameraRenderer;
import com.cgfay.cain.camerasample.camera2.LollipopCameraRenderer;
import com.cgfay.cain.camerasample.camera2.FrameCallback;
import com.cgfay.cain.camerasample.camera2.Renderer;
import com.cgfay.cain.camerasample.camera2.TextureController;
import com.cgfay.cain.camerasample.task.MediaSaver;
import com.cgfay.cain.camerasample.task.MediaSaverTask;
import com.cgfay.cain.camerasample.util.DisplayUtils;
import com.cgfay.cain.camerasample.util.GLESUtils;
import com.cgfay.cain.camerasample.util.PermissionUtils;
import com.cgfay.cain.camerasample.util.StickerUtils;

import java.nio.ByteBuffer;


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
            // 获取屏幕默认分辨率
            mWidth = DisplayUtils.getScreenWidth(Camera2Activity.this);
            mHeight = DisplayUtils.getScreenHeight(Camera2Activity.this);

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
            mController.setFrameCallback(mWidth, mHeight, Camera2Activity.this);

            // 根据SDK版本判断使用哪一版的渲染器
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                mRenderer = new LollipopCameraRenderer(Camera2Activity.this, cameraId, mController);
//            }else{
//                mRenderer = new KitkatCameraRenderer(Camera2Activity.this, cameraId, mController);
//            }
            mRenderer = new KitkatCameraRenderer(Camera2Activity.this, cameraId, mController);

            // 设置默认的分辨率
            mRenderer.setDefaultPreviewSize(mWidth, mHeight);

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
    public void onFrame(final byte[] bytes, final long time) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                saveImage(bytes, time);
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
     * @param bytes
     * @param time
     */
    private void saveImage(byte[]bytes, long time) {
        final Bitmap bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        ByteBuffer b = ByteBuffer.wrap(bytes);
        bitmap.copyPixelsFromBuffer(b);

        mMediaSaver.saveImage(bitmap, time + "", null,
                mWidth, mHeight, 1, null, new MediaSaver.OnMediaSavedListener() {
                    @Override
                    public void onMediaSaved(Uri uri) {
                        if (uri != null) {
                            Intent intent = new Intent(Camera2Activity.this, PhotoViewActivity.class);
                            intent.putExtra(PhotoViewActivity.URI_NAME, uri.toString());
                            startActivity(intent);
                        }
                    }
                });
    }

    /**
     * 切换摄像头
     */
    private void switchCamera() {
        if (cameraId == 1) {
            cameraId = 0;
        } else if (cameraId == 0) {
            cameraId = 1;
        }
        mRenderer.switchCamera(cameraId);
    }

}
