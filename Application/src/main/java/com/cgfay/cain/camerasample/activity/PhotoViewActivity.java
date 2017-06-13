package com.cgfay.cain.camerasample.activity;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.cgfay.cain.camerasample.R;


public class PhotoViewActivity extends AppCompatActivity {

    public static final String FILE_NAME = "photo_path";
    public static final String URI_NAME = "URI_NAME";

    // 照片的绝对路径
    private String mFileName;
    private Uri mUriName;

    private ImageView mImageShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFileName = getIntent().getStringExtra(FILE_NAME);
        if (TextUtils.isEmpty(mFileName)) {
            mUriName = Uri.parse(getIntent().getStringExtra(URI_NAME));
        }
        setContentView(R.layout.activity_photo_view);
        mImageShow = (ImageView) findViewById(R.id.iv_show);
        Glide.with(this)
                .load(TextUtils.isEmpty(mFileName) ? mUriName : mFileName)
                .into(mImageShow);
    }
}
