package com.cgfay.cain.camerasample.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.cgfay.cain.camerasample.R;

public class PhotoViewActivity extends AppCompatActivity {

    public static final String FILE_NAME = "photo_path";

    // 照片的绝对路径
    private String mFileName;

    private ImageView mImageShow;
    private Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFileName = getIntent().getStringExtra(FILE_NAME);
        setContentView(R.layout.activity_photo_view);
        mImageShow = (ImageView) findViewById(R.id.iv_show);

        // 加载图片
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        mBitmap = BitmapFactory.decodeFile(mFileName, options);
        mImageShow.setImageBitmap(mBitmap);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
    }
}
