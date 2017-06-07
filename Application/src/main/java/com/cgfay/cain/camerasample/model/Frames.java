package com.cgfay.cain.camerasample.model;

/**
 * Created by Administrator on 2017/6/7.
 */

public class Frames {
    private String filename;
    private float d;
    private Frame frame;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public float getD() {
        return d;
    }

    public void setD(float d) {
        this.d = d;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    @Override
    public String toString() {
        return "fileName : " + filename + ", d : " + d + ", frame : " + frame;
    }
}
