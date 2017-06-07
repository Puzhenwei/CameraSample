package com.cgfay.cain.camerasample.model;

/**
 * Created by Administrator on 2017/6/7.
 */

public class Meta {
    private String image;
    private Size size;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Size getSize() {
        return size;
    }

    public void setSize(Size size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "image : " + image + ", size : " + size;
    }
}
