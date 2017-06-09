package com.cgfay.cain.camerasample.data;

/**
 * Created by Administrator on 2017/6/7.
 */

public class Size {
    private int w;
    private int h;

    public Size(int width, int height) {
        this.h = height;
        this.w = width;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public void setWidth(int width) {
        w = width;
    }

    public int getWidth() {
        return w;
    }

    public int width() {
        return w;
    }


    public int getHeight() {
        return h;
    }

    public void setHeight(int height) {
        h = height;
    }

    public int height() {
        return h;
    }

    @Override
    public String toString() {
        return "width : " + w + ", height : " + h;
    }
}
