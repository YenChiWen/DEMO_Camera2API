package com.example.demo_camera2api;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

public class ImageProcessorActivity extends AppCompatActivity {
    Button btnLoadImage;
    Button btnSaveImage;
    Switch switchFace;
    Switch switchObject;
    ImageView imageView;
    OverlayView overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processor);

        init();
    }

    private void init(){
        btnLoadImage = findViewById(R.id.btn_load);
        btnSaveImage = findViewById(R.id.btn_save);
        switchFace = findViewById(R.id.switch_face);
        switchObject = findViewById(R.id.switch_object);
        imageView = findViewById(R.id.imageView);
        overlayView = findViewById(R.id.overlayView_image);

        btnLoadImage.setOnClickListener(listener_load);
        btnSaveImage.setOnClickListener(listener_save);
        switchFace.setOnCheckedChangeListener(listener_face);
        switchObject.setOnCheckedChangeListener(listener_object);
        overlayView.addCallback(drawCallback);

        getPutExtra();
    }

    private void getPutExtra(){
        if(getIntent().getExtras() != null){
            Intent intent = getIntent();
            Bitmap bitmap = intent.getParcelableExtra("CaptureImage");
            imageView.setImageBitmap(bitmap);
        }
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

    CompoundButton.OnCheckedChangeListener listener_face = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        }
    };

    CompoundButton.OnCheckedChangeListener listener_object = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        }
    };

    OverlayView.DrawCallback drawCallback = new OverlayView.DrawCallback() {
        @Override
        public void drawCallback(Canvas canvas) {

        }
    };
}
