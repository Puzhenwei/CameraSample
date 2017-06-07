package com.cgfay.cain.camerasample.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.cgfay.cain.camerasample.R;
import com.cgfay.cain.camerasample.task.ZipExtractor;
import com.cgfay.cain.camerasample.util.AssetsUtils;
import com.cgfay.cain.camerasample.util.FileUtils;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String ZIP_PATH = "zips";

    Button mBtnCamera;

    Button mBtnGLES;

    Button mBtnCamera2;

    String[] folderPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        copyZipsFromAssets();
        setContentView(R.layout.activity_main);
        mBtnCamera = (Button) findViewById(R.id.btn_camera);
        mBtnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CaptureViewActivity.class);
                startActivity(intent);
            }
        });
        mBtnGLES = (Button) findViewById(R.id.btn_gles);
        mBtnGLES.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GLCaptureViewActivity.class);
                startActivity(intent);
            }
        });

        mBtnCamera2 = (Button) findViewById(R.id.btn_camera2);
        mBtnCamera2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Camera2Activity.class);
                intent.putExtra("folderPath", folderPath);
                startActivity(intent);
            }
        });

    }

    /**
     * 从Assets下面你复制压缩包并解压
     */
    private void copyZipsFromAssets() {
        String inPath = getExternalFilesDir(null).getAbsolutePath()
                + File.separator + ZIP_PATH;
        // 首先删除旧的压缩包
        FileUtils.recursionDeleteFile(new File(inPath));
        // 复制压缩包
        AssetsUtils.copyFromAssets(MainActivity.this, ZIP_PATH, inPath);

        // 获取复制后的压缩包绝对路径
        List<String> fileLists = FileUtils.listFolder(inPath);
        folderPath = new String[fileLists.size()];
        for (int i = 0; i < fileLists.size(); i++) {
            String inputPath = fileLists.get(i);
            String outputPath = fileLists.get(i).substring(0, inputPath.lastIndexOf("."));
            folderPath[i] = outputPath;
            ZipExtractor extractor = new ZipExtractor(MainActivity.this, inputPath, outputPath);
            extractor.execute();
        }
    }
}
