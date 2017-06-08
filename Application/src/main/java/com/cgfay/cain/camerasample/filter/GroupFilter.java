package com.cgfay.cain.camerasample.filter;

import android.content.res.Resources;
import android.opengl.GLES20;

import com.cgfay.cain.camerasample.util.GLESUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GroupFilter extends Filter {


    private Queue<Filter> mFilterQueue;

    private List<Filter> mFilters;
    private int width = 0, height = 0;
    private int size = 0;

    // 创建离屏渲染buffer
    private int textSize = 2;
    private int[] frame = new int[1];
    private int[] render = new int[1];
    private int[] texture = new int[textSize];
    private int textureIndex = 0;

    public GroupFilter(Resources resources) {
        super(resources);
        mFilters = new ArrayList<>();
        mFilterQueue = new ConcurrentLinkedDeque<>();
    }

    /**
     * 添加一个特效
     * @param filter
     */
    public void addFilter(Filter filter) {
        // Android屏幕相对GL世界的纹理 y轴是反过来的
        GLESUtils.flip(filter.getMatrix(), false, true);
        mFilterQueue.add(filter);
    }


    /**
     * 移除某个特效
     * @param filter
     * @return
     */
    public boolean removeFilter(Filter filter) {
        boolean result = mFilters.remove(filter);
        if (result) {
            size--;
        }
        return result;
    }

    /**
     * 清空所有特效
     */
    public void clearAll() {
        mFilterQueue.clear();
        mFilters.clear();
        size = 0;
    }

    /**
     * 绘制过程
     */
    public void draw() {
        updateFilter();
        textureIndex = 0;
        if (size > 0) {
            for (Filter filter : mFilters) {
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frame[0]);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                        GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D,
                        texture[textureIndex % 2], 0);
                GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                        GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, render[0]);
                GLES20.glViewport(0, 0, width, height);
                if (textureIndex == 0) {
                    filter.setTextureId(getTextureId());
                } else {
                    filter.setTextureId(texture[(textureIndex - 1) % 2]);
                }
                filter.draw();
                unBindTexture();
                textureIndex++;
            }
        }
    }

    /**
     * 刷新特效
     */
    private void updateFilter() {
        Filter filter;
        while ((filter = mFilterQueue.poll()) != null) {
            filter.create();
            filter.setSize(width, height);
            mFilters.add(filter);
            size++;
        }
    }

    @Override
    public int getOutputTexture() {
        return size == 0 ? getTextureId() : texture[(textureIndex - 1) % 2];
    }

    @Override
    protected void onCreate() {

    }

    @Override
    protected void onSizeChanged(int width, int height) {
        this.width = width;
        this.height = height;
        updateFilter();
        createFrameBuffer();
    }

    /**
     * 创建帧缓冲区
     * @return
     */
    private boolean createFrameBuffer() {
        GLES20.glGenFramebuffers(1, frame, 0);
        GLES20.glGenRenderbuffers(1, render, 0);

        createTexture();

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, render[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER,
                GLES20.GL_DEPTH_COMPONENT16, width, height);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frame[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture[0], 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, render[0]);

        unBindTexture();
        return false;
    }

    /**
     * 生成Texture
     */
    private void createTexture() {
        GLES20.glGenTextures(textSize, texture, 0);

        for (int i = 0; i < textSize; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
    }

    /**
     * 解绑texture
     */
    private void unBindTexture() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
    }

    /**
     * 删除帧缓冲区
     */
    private void deleteFrameBuffer() {
        GLES20.glDeleteRenderbuffers(1, render, 0);
        GLES20.glDeleteFramebuffers(1, frame, 0);
        GLES20.glDeleteTextures(1, texture, 0);
    }
}
