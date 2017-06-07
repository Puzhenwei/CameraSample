package com.cgfay.cain.camerasample.model;

/**
 * Created by Administrator on 2017/6/7.
 */

public class Size {
    private int w;
    private int h;

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

    @Override
    public String toString() {
        return "width : " + w + ", height : " + h;
    }
}
