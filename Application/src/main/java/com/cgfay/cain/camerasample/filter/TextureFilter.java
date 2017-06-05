package com.cgfay.cain.camerasample.filter;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import java.nio.ByteBuffer;

public class TextureFilter extends Filter {

    private CameraFilter mFilter;
    private int width = 0;
    private int height = 0;

    private int[] mFrame = new int[1];
    private int[] mTexture = new int[1];
    private int[] mCameraTexture = new int[1];

    private ByteBuffer vtBuffer;

    private SurfaceTexture mSurfaceTexture;
    private float[] mCoordOM = new float[16];

    private ByteBuffer mBuffer;
    public TextureFilter(Resources mRes) {
        super(mRes);
        mFilter =  new CameraFilter(mRes);
    }

    @Override
    protected void onCreate() {
        mFilter.create();
        GLES20.glGenTextures(1, mCameraTexture, 0);
        mSurfaceTexture = new SurfaceTexture(mCameraTexture[0]);
    }

    public void setCoordMatrix(float[] matrix) {
        mFilter.setCoordMatrix(matrix);
    }

    @Override
    public void setFlag(int flag) {
        mFilter.setFlag(flag);
    }

    @Override
    public void setMatrix(float[] matrix) {
        mFilter.setMatrix(matrix);
    }

    @Override
    public int getOutputTexture() {
        return mTexture[0];
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        mFilter.setSize(width, height);
        if (this.width != height
                 || this.height != height) {
            this.width = width;
            this.height = height;
            GLES20.glDeleteFramebuffers(1, mFrame, 0);
            GLES20.glDeleteTextures(1, mTexture, 0);
            GLES20.glGenFramebuffers(1, mFrame, 0);
            GLES20.glGenTextures(1, mTexture, 0);
            for (int i = 0; i < 1; i++) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[i]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                        0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                useTexParameter();
            }
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
        }
    }

    @Override
    public void draw() {
        boolean enable = GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST);
        if (enable) {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mCoordOM);
            mFilter.setCoordMatrix(mCoordOM);
        }
        bindFrameTexture(mFrame[0], mTexture[0]);
        GLES20.glViewport(0, 0, width, height);
        mFilter.setTextureId(mCameraTexture[0]);
        mFilter.draw();
        unBindFrameBuffer();

        if (enable) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
    }

    private void bindFrameTexture(int frameBufferId,int textureId){
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    private void unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void useTexParameter() {
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
    }

    public void deldetBuffer() {
        GLES20.glDeleteFramebuffers(1, mFrame, 0);
    }

    public SurfaceTexture getTexture() {
        return mSurfaceTexture;
    }
}
