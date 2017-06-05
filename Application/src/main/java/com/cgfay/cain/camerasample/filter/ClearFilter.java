package com.cgfay.cain.camerasample.filter;

import android.content.res.Resources;


public class ClearFilter extends Filter {

    public ClearFilter(Resources mRes) {
        super(mRes);
    }

    @Override
    protected void onCreate() {
        createProgramByAssetsFile("shader/clear_vertex.glsl",
                "shader/clear_fragment.glsl");
    }

    @Override
    protected void onSizeChanged(int width, int height) {

    }
}
