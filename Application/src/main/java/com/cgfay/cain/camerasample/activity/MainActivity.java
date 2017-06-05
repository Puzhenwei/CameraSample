package com.cgfay.cain.camerasample.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.cgfay.cain.camerasample.R;

public class MainActivity extends AppCompatActivity {

    Button mBtnCamera;

    Button mBtnGLES;

    Button mBtnCamera2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                startActivity(intent);
            }
        });

    }
}
