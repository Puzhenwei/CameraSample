package com.cgfay.cain.camerasample.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cgfay.cain.camerasample.R;
import com.cgfay.cain.camerasample.task.MediaSaver;
import com.cgfay.cain.camerasample.task.MediaSaverTask;
import com.cgfay.cain.camerasample.util.PermissionUtils;
import com.cgfay.cain.camerasample.camera.CameraView;


public class GLCaptureViewActivity extends AppCompatActivity {

    private static final String TAG = "GLCaptureViewActivity";

    private CameraView mCameraView;
    private Button mBtnViewPhoto;
    private Button mBtnTake;
    private Button mBtnSwitchCamera;

    private MediaSaver mMediaSaver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionUtils.askPermission(this,
                new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                10, initRunnable);
    }


    private Runnable initRunnable = new Runnable() {
        @Override
        public void run() {
            setContentView(R.layout.activity_glcapture);
            mCameraView = (CameraView) findViewById(R.id.view_camera);

            mBtnViewPhoto = (Button) findViewById(R.id.btn_view_photo);
            mBtnTake = (Button) findViewById(R.id.btn_take);
            mBtnSwitchCamera = (Button) findViewById(R.id.btn_switch);

            // 用于保存照片
            mMediaSaver = new MediaSaverTask(getContentResolver());

            mBtnViewPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 判断是否拍摄了照片，如果拍摄了照片，则打开照片查看
                }
            });

            mBtnTake.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 判断是否处于预览状态。如果处于预览状态，则可以拍摄
                    if (mCameraView != null) {
                        mCameraView.takePhoto(autoFocusCallback);
                    }
                }
            });

            mBtnSwitchCamera.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mCameraView != null) {
                        mCameraView.switchCamera();
                    }
                }
            });
        }
    };


    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            // 对焦成功后
            if (success) {
                // 按下快门
                camera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {
                        // 按下快门瞬间操作
                    }
                }, new Camera.PictureCallback() {
                    // 是否保存原始图片的信息
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                    }
                }, pictureCallback);
            }
        }
    };

    /**
     * 获取图片
     */
    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap == null) {
                Toast.makeText(GLCaptureViewActivity.this, "拍照失败",Toast.LENGTH_SHORT).show();
            } else {
                mMediaSaver.saveImage(bitmap, System.currentTimeMillis() + "", null,
                        bitmap.getWidth(), bitmap.getHeight(), 90, null,
                        new MediaSaver.OnMediaSavedListener() {
                            @Override
                            public void onMediaSaved(Uri uri) {
                                if (uri != null) {
                                    Intent intent = new Intent(GLCaptureViewActivity.this,
                                            PhotoViewActivity.class);
                                    intent.putExtra(PhotoViewActivity.URI_NAME, uri.toString());
                                    startActivity(intent);
                                }
                            }
                        });
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(requestCode == 10,
                grantResults, initRunnable, new Runnable() {
            @Override
            public void run() {
                Toast.makeText(GLCaptureViewActivity.this,
                        "没有相机权限", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraView != null) {
            mCameraView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraView != null) {
            mCameraView.onPause();
        }
    }
}
