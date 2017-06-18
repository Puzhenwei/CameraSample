
package com.cgfay.cain.camerasample.camera;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.cgfay.cain.camerasample.detection.FaceEvent;
import com.cgfay.cain.camerasample.detection.StickerFaceDetection;
import com.cgfay.cain.camerasample.util.DisplayUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class KitkatCamera implements ICamera {

    private static final String TAG = "KitkatCamera";

    private Config mConfig;
    private Camera mCamera;
    private CameraSizeComparator sizeComparator;

    private Camera.Size picSize;
    private Camera.Size preSize;

    private Point mPicSize;
    private Point mPreSize;

    private Handler mHandler = null;
    private StickerFaceDetection mDetection;

    private CameraView mCameraView;

    public KitkatCamera(Context context, CameraView cameraView) {
        this.mConfig = new Config();
        int width = DisplayUtils.getScreenWidth(context);
        mConfig.minPreviewWidth = width;
        mConfig.minPictureWidth = width;
        mConfig.rate = 1.778f;
        sizeComparator = new CameraSizeComparator();
        mHandler = new DetectHandler();
        mDetection = new StickerFaceDetection(context, mHandler);
        mCameraView = cameraView;
    }

    @Override
    public boolean open(int cameraId) {

        mCamera = Camera.open(cameraId);

        if (mCamera != null) {
            Camera.Parameters param = mCamera.getParameters();
            picSize = getPropPictureSize(param.getSupportedPictureSizes(), mConfig.rate,
                    mConfig.minPictureWidth);
            preSize = getPropPreviewSize(param.getSupportedPreviewSizes(), mConfig.rate,
                    mConfig.minPreviewWidth);
            param.setPictureSize(picSize.width, picSize.height);
            param.setPreviewSize(preSize.width, preSize.height);
            param.setRotation(90);
            mCamera.setParameters(param);
            mCamera.setDisplayOrientation(90);
            Camera.Size pre = param.getPreviewSize();
            Camera.Size pic = param.getPictureSize();
            mPicSize = new Point(pic.height, pic.width);
            mPreSize = new Point(pre.height, pre.width);
            return true;
        }
        return false;
    }

    public void setPreviewTexture(SurfaceTexture texture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(texture);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setConfig(Config config) {
        this.mConfig=config;
    }

    @Override
    public boolean startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
            mHandler.sendEmptyMessage(FaceEvent.CAMERA_HAS_STARTED_PREVIEW);
        }
        return false;
    }

    @Override
    public void stopPreview() {
        if (mCamera != null) {
            stopFaceDetector();
            mCamera.stopPreview();
        }
    }

    @Override
    public boolean switchTo(int cameraId) {
        close();
        open(cameraId);
        return false;
    }

    @Override
    public void takePhoto(Camera.AutoFocusCallback callback) {
        if (mCamera != null) {
            mCamera.autoFocus(callback);
        }
    }

    @Override
    public boolean close() {
        if(mCamera != null){
            try{
                mCamera.stopPreview();
                mCamera.release();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public Point getPreviewSize() {
        return mPreSize;
    }

    @Override
    public Point getPictureSize() {
        return mPicSize;
    }

    @Override
    public void setOnPreviewFrameCallback (final PreviewFrameCallback callback) {
        if(mCamera!=null){
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    callback.onPreviewFrame(data,mPreSize.x,mPreSize.y);
                }
            });
        }
    }

    public void addBuffer(byte[] buffer) {
        if(mCamera!=null){
            mCamera.addCallbackBuffer(buffer);
        }
    }

    public void setOnPreviewFrameCallbackWithBuffer(final PreviewFrameCallback callback) {
        if(mCamera!=null){
            Log.e(TAG, "Camera set CallbackWithBuffer");
            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    callback.onPreviewFrame(data,mPreSize.x, mPreSize.y);
                }
            });
        }
    }


    private Camera.Size getPropPreviewSize(List<Camera.Size> list, float th, int minWidth) {
        Collections.sort(list, sizeComparator);

        int i = 0;
        for(Camera.Size s:list){
            if((s.height >= minWidth) && equalRate(s, th)){
                break;
            }
            i++;
        }
        if(i == list.size()){
            i = 0;
        }
        return list.get(i);
    }

    private Camera.Size getPropPictureSize(List<Camera.Size> list, float th, int minWidth) {
        Collections.sort(list, sizeComparator);

        int i = 0;
        for(Camera.Size s:list){
            if((s.height >= minWidth) && equalRate(s, th)){
                break;
            }
            i++;
        }
        if(i == list.size()){
            i = 0;
        }
        return list.get(i);
    }

    private boolean equalRate(Camera.Size s, float rate) {
        float r = (float)(s.width)/(float)(s.height);
        if(Math.abs(r - rate) <= 0.03)
        {
            return true;
        }
        else{
            return false;
        }
    }

    private class CameraSizeComparator implements Comparator<Camera.Size> {
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            // TODO Auto-generated method stub
            if(lhs.height == rhs.height){
                return 0;
            }
            else if(lhs.height > rhs.height){
                return 1;
            }
            else{
                return -1;
            }
        }

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
                    mCameraView.setDetectedFaces(faces);
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
