package com.cgfay.cain.camerasample.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cgfay.cain.camerasample.R;
import com.cgfay.cain.camerasample.util.PermissionUtils;
import com.cgfay.cain.camerasample.widget.CameraView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class GLCaptureViewActivity extends AppCompatActivity {

    private CameraView mCameraView;
    private Button mBtnViewPhoto;
    private Button mBtnTake;
    private Button mBtnSwitchCamera;

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
                // 将图片保存本地
                String fileName = Environment.getExternalStorageDirectory().toString() +
                        File.separator + "DCIM" + File.separator + "Camera" + File.separator
                        + "PicTest_" + System.currentTimeMillis() + ".jpg";
                File file = new File(fileName);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdir();
                }
                try {
                    // 向缓冲区压缩图片
                    BufferedOutputStream bufferedOutputStream =
                            new BufferedOutputStream(new FileOutputStream(file));
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bufferedOutputStream);
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                    // 跳转至预览页面
                    Intent intent = new Intent(GLCaptureViewActivity.this, PhotoViewActivity.class);
                    intent.putExtra(PhotoViewActivity.FILE_NAME, fileName);
                    startActivity(intent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
