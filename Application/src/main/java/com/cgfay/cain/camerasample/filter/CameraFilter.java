
package com.cgfay.cain.camerasample.filter;

import android.content.res.Resources;
import android.hardware.Camera;

public class CameraFilter extends BaseFilter {

    public CameraFilter(Resources resources) {
        super(resources);
    }

    @Override
    protected void initBuffer() {
        super.initBuffer();
        movie();
    }

    @Override
    public void setFlag(int flag) {
        super.setFlag(flag);
//        // 前置摄像头
//        if (getFlag() == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            cameraFront();
//        }
//        //后置摄像头
//        else if(getFlag() == Camera.CameraInfo.CAMERA_FACING_BACK) {
//            cameraBack();
//        }
    }

    private void cameraFront() {
        float[] coord = new float[] {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
        };
        mTexBuffer.clear();
        mTexBuffer.put(coord);
        mTexBuffer.position(0);
    }

    private void cameraBack() {
        float[] coord = new float[] {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
        };
        mTexBuffer.clear();
        mTexBuffer.put(coord);
        mTexBuffer.position(0);
    }

    private void movie() {
        float[] coord = new float[] {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
        };
        mTexBuffer.clear();
        mTexBuffer.put(coord);
        mTexBuffer.position(0);
    }
}
