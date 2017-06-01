package com.cgfay.cain.camerasample.widget;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

public interface ICamera {
    boolean open(int cameraID);
    void setConfig(Config config);
    boolean startPreview();
    boolean switchTo(int cameraID);
    void takePhoto(Camera.AutoFocusCallback callback);
    void stopPreview();
    boolean close();
    void setPreviewTexture(SurfaceTexture texture);

    Point getPreviewSize();
    Point getPictureSize();

    void setOnPreviewFrameCallback(PreviewFrameCallback callback);

    class Config {
        float rate;
        int minPreviewWidth;
        int minPictureWidth;
    }

    interface  PreviewFrameCallback{
        void onPreviewFrame(byte[] bytes, int width, int height);
    }
}
