package com.example.demo_camera2api;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    Button btnCapture;
    Button btnRecode;
    Button btnDetect;
    FloatingActionButton fabSync;
    TextureView textureView;

    Camera2API camera2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init(){
        btnCapture = findViewById(R.id.btn_capture);
        btnRecode = findViewById(R.id.btn_record);
        btnDetect = findViewById(R.id.btn_detect);
        textureView = findViewById(R.id.textureView);
        fabSync = findViewById(R.id.fab_sync);

        btnCapture.setOnClickListener(listenerCapture);
        btnRecode.setOnClickListener(listenerRecode);
        btnDetect.setOnClickListener(listenerDetect);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        fabSync.setOnClickListener(listenerSync);
    }

    View.OnClickListener listenerCapture = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CaptureCallback captureCallback = new CaptureCallback() {
                @Override
                public void update(final Bitmap bmp) {
                    Log.d("YEN", "Capture.");
                }
            };
            camera2.getCapture(captureCallback);
        }
    };

    View.OnClickListener listenerRecode = new View.OnClickListener(){
        @Override
        public void onClick(View v) {

        }
    };

    View.OnClickListener listenerDetect = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    View.OnClickListener listenerSync = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(camera2 != null)
                camera2.closeCamera();

            camera2.setLensFacing((camera2.getLensFacing()+1) % 2);
            camera2.init();
        }
    };

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @SuppressLint("NewApi")
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            camera2 = new Camera2API(MainActivity.this, textureView);
            camera2.setLensFacing(1);
            camera2.init();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
}
