package com.cgfay.cain.camerasample.filter;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.cgfay.cain.camerasample.util.GLESUtils;

import java.lang.reflect.Array;
import java.util.Arrays;


// 添加水印
public class WaterMaskFilter extends ClearFilter {

    private static final String TAG = "WaterMaskFilter";

    private Bitmap mBitmap;
    private ClearFilter mFilter;
    private int mWidth, mHeight;
    private int x, y, width, height;
    private float mOffsetX = 0, mOffsetY = 0;

    private int[] mTextures = new int[1];

    public WaterMaskFilter(Resources resources) {
        super(resources);
        mFilter = new ClearFilter(resources) {
            @Override
            protected void onClear() {

            }
        };
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mFilter.create();
        createTexture();
    }

    @Override
    protected void onClear() {
        super.onClear();
    }


    @Override
    public void draw() {
        super.draw();
        GLES20.glViewport(x, y,
                width == 0 ? mBitmap.getWidth() : width,
                height == 0 ? mBitmap.getHeight() : height);

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA);
        mFilter.draw();
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glViewport(0, 0, mWidth, mHeight);
    }

    @Override
    protected void onSizeChanged(int width, int height) {
        super.onSizeChanged(width, height);
        mWidth = width;
        mHeight = height;
        mFilter.setSize(width, height);
    }

    private void  createTexture() {
        if (mBitmap != null) {
            // 创建texture
            GLES20.glGenTextures(1, mTextures, 0);
            // 绑定texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
            // 设置参数
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_LINEAR);
            // 创建mip贴图
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
            // 镜像翻转
            GLESUtils.flip(mFilter.getMatrix(), false, true);
            // 贴图壁纸翻转
            mFilter.setTextureId(mTextures[0]);
        }
    }

    /**
     * 设置水印图片
     * @param bitmap
     */
    public void setWaterMask(Bitmap bitmap) {
        if (mBitmap != null) {
            mBitmap.recycle();
        }
        mBitmap = bitmap;
    }

    /**
     * 设置位置
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void setPosition(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }


    /**
     * 偏移
     * @param x
     * @param y
     */
    public void setOffset(float x, float y) {
        mOffsetX = x;
        mOffsetY = y;
    }

    @Override
    protected void setExpandData() {
        super.setExpandData();
    }
}
