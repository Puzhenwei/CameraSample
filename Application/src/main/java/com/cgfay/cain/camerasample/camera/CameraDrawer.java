
package com.cgfay.cain.camerasample.camera;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.cgfay.cain.camerasample.filter.AFilter;
import com.cgfay.cain.camerasample.filter.CameraFilter;
import com.cgfay.cain.camerasample.util.GLESUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class CameraDrawer implements GLSurfaceView.Renderer {

    private float[] matrix = new float[16];
    private SurfaceTexture surfaceTexture;
    private int width,height;
    private int dataWidth,dataHeight;
    private AFilter mBaseFilter;
    private int cameraId = 1;

    public CameraDrawer(Resources res){
        mBaseFilter = new CameraFilter(res);
    }

    public void setDataSize(int dataWidth,int dataHeight){
        this.dataWidth = dataWidth;
        this.dataHeight = dataHeight;
        calculateMatrix();
    }

    public void setViewSize(int width,int height) {
        this.width = width;
        this.height = height;
        calculateMatrix();
    }

    private void calculateMatrix() {
        matrix = GLESUtils.getShowMatrix(this.dataWidth, this.dataHeight, this.width, this.height);
        if (cameraId == 1) {
            GLESUtils.flip(matrix, true, false);
            GLESUtils.rotate(matrix, 90);
        } else {
            GLESUtils.rotate(matrix, 270);
        }
        mBaseFilter.setMatrix(matrix);
    }

    public SurfaceTexture getSurfaceTexture(){
        return surfaceTexture;
    }

    public void setCameraId(int id){
        this.cameraId = id;
        calculateMatrix();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int texture = createTextureID();
        surfaceTexture = new SurfaceTexture(texture);
        mBaseFilter.create();
        mBaseFilter.setTextureId(texture);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        setViewSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
        }
        mBaseFilter.draw();
    }

    private int createTextureID() {
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }

}
