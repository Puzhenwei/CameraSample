package com.cgfay.cain.camerasample.data;

import java.util.List;

// 每个器官的Json对象
public class Organ {
    private List<Frames> frames;
    private Meta meta;

    public List<Frames> getFrames() {
        return frames;
    }

    public void setFrames(List<Frames> frames) {
        this.frames = frames;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    @Override
    public String toString() {
        return "frames : " + frames.toString() + ", meta : " + meta;
    }
}
