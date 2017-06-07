package com.cgfay.cain.camerasample.model;

/**
 * Created by Administrator on 2017/6/7.
 */

public class Frame {
    private int x;
    private int y;
    private int w;
    private int h;

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
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

    @Override
    public String toString() {
        return "x : " + x + ", y : " + y + ", width : " + w + ", height : " + h;
    }
}
