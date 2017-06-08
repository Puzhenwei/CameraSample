package com.cgfay.cain.camerasample.model;

import java.util.List;

public class Sticker {
    private String id;
    private String name;
    private int sw;
    private int sh;
    private List<Facer> res;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSw() {
        return sw;
    }

    public void setSw(int sw) {
        this.sw = sw;
    }

    public int getSh() {
        return sh;
    }

    public void setSh(int sh) {
        this.sh = sh;
    }

    public List<Facer> getRes() {
        return res;
    }

    public void setRes(List<Facer> res) {
        this.res = res;
    }

    @Override
    public String toString() {
        return "id : " + id + ", name : " + name
                + ", sw : " + sw + ", sh : " + sh
                + ", res : " + res.toString();
    }
}
