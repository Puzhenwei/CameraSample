package com.cgfay.cain.camerasample.detection;

import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;


public class StickerFaceDetection implements Camera.FaceDetectionListener {

    private static final String TAG = "StickerFaceDetection";

    private Context mContext;
    private Handler mHandler;

    public StickerFaceDetection(Context mContext, Handler mHandler) {
        this.mContext = mContext;
        this.mHandler = mHandler;
    }

    @Override
    public void onFaceDetection(Camera.Face[] faces, Camera camera) {
        if (faces != null) {
            Message message = mHandler.obtainMessage();
            message.what = FaceEvent.UPDATE_FACE_RECT;
            message.obj = faces;
            message.sendToTarget();
        }
    }
}
