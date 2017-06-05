package com.cgfay.cain.camerasample.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.cgfay.cain.camerasample.R;

import java.io.File;

public class PhotoViewActivity extends AppCompatActivity {

    public static final String FILE_NAME = "photo_path";

    // 照片的绝对路径
    private String mFileName;

    private ImageView mImageShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFileName = getIntent().getStringExtra(FILE_NAME);
        setContentView(R.layout.activity_photo_view);
        mImageShow = (ImageView) findViewById(R.id.iv_show);
        Glide.with(this).load(mFileName).into(mImageShow);
    }
}
