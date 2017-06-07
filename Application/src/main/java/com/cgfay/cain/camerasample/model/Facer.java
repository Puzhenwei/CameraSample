package com.cgfay.cain.camerasample.model;

import java.util.List;

public class Facer {
    private String type;
    private int tier;
    private float scale;
    private List<Float> offset;
    private String action;
    private int layerCompositeMode;
    private int layerOpaqueness;
    private boolean gif;
    private List<String> i;
    private List<String> d;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public List<Float> getOffset() {
        return offset;
    }

    public void setOffset(List<Float> offset) {
        this.offset = offset;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getLayerCompositeMode() {
        return layerCompositeMode;
    }

    public void setLayerCompositeMode(int layerCompositeMode) {
        this.layerCompositeMode = layerCompositeMode;
    }

    public int getLayerOpaqueness() {
        return layerOpaqueness;
    }

    public void setLayerOpaqueness(int layerOpaqueness) {
        this.layerOpaqueness = layerOpaqueness;
    }

    public boolean isGif() {
        return gif;
    }

    public void setGif(boolean gif) {
        this.gif = gif;
    }

    public List<String> getI() {
        return i;
    }

    public void setI(List<String> i) {
        this.i = i;
    }

    public List<String> getD() {
        return d;
    }

    public void setD(List<String> d) {
        this.d = d;
    }

    @Override
    public String toString() {
        return "type : " + type + ", tier : " + tier
                + ", scale : " + scale + ", offset : "
                + offset.toString() + ", action : " + action
                + ", layerCompositeMode : " + layerCompositeMode
                + ", layerOpaqueness" + layerOpaqueness
                + ", gif : " + gif + ", i : " + i + ", d : " + d;
    }
}
