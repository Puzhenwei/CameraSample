
package com.cgfay.cain.camerasample.activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Point;
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
import android.os.Process;
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
import com.cgfay.cain.camerasample.model.Facer;
import com.cgfay.cain.camerasample.model.Frame;
import com.cgfay.cain.camerasample.model.Meta;
import com.cgfay.cain.camerasample.model.Organ;
import com.cgfay.cain.camerasample.model.Sticker;
import com.cgfay.cain.camerasample.task.JsonParser;
import com.cgfay.cain.camerasample.util.DisplayUtils;
import com.cgfay.cain.camerasample.util.FileUtils;
import com.cgfay.cain.camerasample.util.GLESUtils;
import com.cgfay.cain.camerasample.util.PermissionUtils;
import com.cgfay.cain.camerasample.util.SDCardUtils;

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mRenderer = new Camera2Renderer();
            }else{
                mRenderer = new Camera1Renderer();
            }
            // 获取解压后的路径列表
            folderPath = getIntent().getStringArrayExtra("folderPath");
            // 根据路径列表获取文件列表
            scanPackages();

            setContentView(R.layout.activity_camera2);

            mSurfaceView = (SurfaceView)findViewById(R.id.view_camera);
            mBtnViewPhoto = (Button) findViewById(R.id.btn_view_photo);
            mBtnTake = (Button) findViewById(R.id.btn_take);
            mBtnSwitchCamera = (Button) findViewById(R.id.btn_switch);

            // 查看照片
            mBtnViewPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

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

                }
            });

            mController = new TextureController(Camera2Activity.this);
            mController.setShowType(GLESUtils.TYPE_CENTERCROP);

            // 控制帧率回调
            mController.setFrameCallback(720, 1280, Camera2Activity.this);
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
                Bitmap bitmap=Bitmap.createBitmap(720,1280, Bitmap.Config.ARGB_8888);
                ByteBuffer b=ByteBuffer.wrap(bytes);
                bitmap.copyPixelsFromBuffer(b);
                saveBitmap(bitmap);
                bitmap.recycle();
            }
        }).start();
    }

    /**
     * 扫描贴图包
     */
    private void scanPackages() {
        // 遍历所有文件目录
        for (int i = 0; i < folderPath.length; i++) {
            List<String> theme = FileUtils.getAbsolutePathlist(folderPath[i]);
            // 将zip解压得到的json文件和png文件提取出来
            for (int j = 0; j < theme.size(); j++) {
                // 如果是sticker.json文件，则解析该文件
                if (theme.get(j).endsWith("sticker.json")) {
                    // 直接获取当前的目录，后续解析头、鼻子、前景、背景json和图片
                    final String path = theme.get(j).replace("sticker.json", "");
                    // 解析json文件
                    JsonParser parser = new JsonParser(theme.get(j), Sticker.class);
                    parser.addJsonParserCallback(new JsonParser.JsonParserCallback() {
                        @Override
                        public void onComplete(Object object) {
                            // 解析成功后，进入添加贴纸过程
                            addStickerToTexture(path, (Sticker) object);
                        }
                    });
                    parser.execute();
                    break;
                }
            }
        }
    }

    /**
     * 给Texture添加Sticker贴纸
     * @param filePath  sticker.json文件的路径
     * @param sticker   通过json生成的Sticker对象
     */
    private void addStickerToTexture(final String filePath, Sticker sticker) {
        for (int i = 0; i < sticker.getRes().size(); i++) {
            final Facer facer = sticker.getRes().get(i);
            List<String> jsons = facer.getI();
            JsonParser parser = new JsonParser(filePath + File.separator + jsons.get(0), Organ.class);
            parser.addJsonParserCallback(new JsonParser.JsonParserCallback() {
                @Override
                public void onComplete(Object object) {
                    addTexture(facer, (Organ) object,
                            filePath + File.separator + facer.getD().get(0));
                }
            });
            parser.execute();
        }
    }

    /**
     * 添加texture
     * @param facer     脸部数据
     * @param organ     脸部器官数据
     * @param bitmapPath    图片地址
     */
    private void addTexture(Facer facer, Organ organ, String bitmapPath) {
        // 是否允许动画
        if (!facer.isGif()) {
            Frame frame = organ.getFrames().get(0).getFrame();
            float scale = facer.getScale();
            try {
                // 解析大图中的部分区域
                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(bitmapPath, false);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Rect rect = new Rect(frame.getX(), frame.getY(),
                        frame.getX() + frame.getW(),
                        frame.getY() + frame.getH());
                Bitmap temp = decoder.decodeRegion(rect, options);
                Matrix matrix = new Matrix();
                if (scale == 0) {
                    float scaleW = temp.getWidth() / DisplayUtils.getScreenWidth(Camera2Activity.this);
                    float scaleH = temp.getHeight() / DisplayUtils.getScreenHeight(Camera2Activity.this);
                    scale = scaleW > scaleH ? scaleW : scaleH;
                }
                matrix.postScale(scale, scale);
                // 根据返回的数据进行缩放
                Bitmap bitmap = Bitmap.createBitmap(temp, 0, 0,
                        temp.getWidth(), temp.getHeight(), matrix, true);


                Log.d(TAG, bitmapPath);
                // 添加贴图
                WaterMaskFilter filter = new WaterMaskFilter(getResources());
                filter.setWaterMask(bitmap);
                filter.setOffset(facer.getOffset().get(0), facer.getOffset().get(1));
                filter.setPosition(0, 0, bitmap.getWidth(), bitmap.getHeight());
                mController.addFilter(filter);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            addGifTexture(facer, organ, bitmapPath);
        }
    }

    /**
     * 添加gif 动画的texture
     */
    private void addGifTexture(Facer facer, Organ organ, String bitmapPath) {
        // 清除原来的所有Filters
        mController.clearAllFilters();
        // 添加所有的Filters
    }

    // 保存图片
    public void saveBitmap(Bitmap b){
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
