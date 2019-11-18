package com.example.demo_camera2api;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ImageProcessorActivity extends AppCompatActivity {
    ImageView imageView;
    Button btnLoadImage;
    Button btnSaveImage;
    Button btnDetect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_peocessor);

        init();
    }

    private void init(){
        btnDetect = findViewById(R.id.btn_detect);
        btnLoadImage = findViewById(R.id.btn_load);
        btnSaveImage = findViewById(R.id.btn_save);
        imageView = findViewById(R.id.imageView);

        btnLoadImage.setOnClickListener(listener_load);
        btnSaveImage.setOnClickListener(listener_save);
        btnDetect.setOnClickListener(listener_detect);
    }

    View.OnClickListener listener_load = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    View.OnClickListener listener_save = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    View.OnClickListener listener_detect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };
}
