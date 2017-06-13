/*
 *
 * Renderer.java
 * 
 * Created by Wuwang on 2017/3/3
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.cgfay.cain.camerasample.camera2;

import android.opengl.GLSurfaceView;

/**
 * Description:
 */
public interface Renderer extends GLSurfaceView.Renderer {

    void setDefaultPreviewSize(int width, int height);
    void closeCamera();
    void openCamera();
    void switchCamera(int cameraID);
    void onDestroy();
}
