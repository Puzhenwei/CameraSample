package com.cgfay.cain.camerasample.filter;

import android.content.res.Resources;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;

import com.cgfay.cain.camerasample.util.GLESUtils;

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
        mCameraTexture[0] = GLESUtils.getExternalOESTextureID();
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
            // 删除当前的帧缓冲区
            GLES20.glDeleteFramebuffers(1, mFrame, 0);
            // 删除当前的texture
            GLES20.glDeleteTextures(1, mTexture, 0);
            // 重新获取帧缓冲区
            GLES20.glGenFramebuffers(1, mFrame, 0);
            // 重新生成texture
            GLES20.glGenTextures(1, mTexture, 0);
            // 读取位图并绑定Texture
            for (int i = 0; i < mTexture.length; i++) {
                // 绑定Texture
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture[i]);
                // 读取位图信息
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height,
                        0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                // 设置过滤参数
                setTexParameter();
            }
            // 取消绑定
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        }
    }

    @Override
    public void draw() {
        // 判断是否存在深度测试,如果存在则先关闭，避免影响
        boolean enable = GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST);
        if (enable) {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }
        // 判断texture是否存在，如果存在则更新texture并设置位移
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(mCoordOM);
            mFilter.setCoordMatrix(mCoordOM);
        }
        // 绑定帧缓冲区
        bindFrameTexture(mFrame[0], mTexture[0]);
        GLES20.glViewport(0, 0, width, height);
        // 绑定Camera的texture
        mFilter.setTextureId(mCameraTexture[0]);
        // 绘制
        mFilter.draw();
        // 取消绑定
        unBindFrameBuffer();

        // 绘制完成后，如果原来存在深度测试，则需要冲洗你开启深度测试，恢复现场
        if (enable) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        }
    }

    // 绑定帧缓冲区
    private void bindFrameTexture(int frameBufferId, int textureId){
        // 根据Id绑定帧缓冲区
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBufferId);
        // 连接一个2D纹理作为帧缓冲区附着
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
    }

    // 取消绑定帧缓冲区
    private void unBindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    // 设置过滤参数
    private void setTexParameter() {
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

    // 删除帧缓冲区
    public void deldetBuffer() {
        GLES20.glDeleteFramebuffers(1, mFrame, 0);
    }

    // 获取当前渲染的texture
    public SurfaceTexture getTexture() {
        return mSurfaceTexture;
    }
}
