package com.example.demo_camera2api;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity {

    Button btnCapture;
    Button btnRecode;
    FloatingActionButton fabSync;
    TextureView textureView;
    OverlayView overlayView;

    Camera2API camera2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(camera2 != null)
            camera2.closeCamera();
    }

    private void init(){
        btnCapture = findViewById(R.id.btn_capture);
        btnRecode = findViewById(R.id.btn_record);
        textureView = findViewById(R.id.textureView);
        fabSync = findViewById(R.id.fab_sync);
        overlayView = findViewById(R.id.overlayView_camera2);

        btnCapture.setOnClickListener(listenerCapture);
        btnRecode.setOnClickListener(listenerRecode);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        fabSync.setOnClickListener(listenerSync);

        overlayView.addCallback(drawCallback);
    }

    View.OnClickListener listenerCapture = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CaptureImage.updateCallback captureCallback = new CaptureImage.updateCallback() {
                @Override
                public void update(final Bitmap bmp) {
                    Log.d("YEN", "Capture.");

//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Intent intent = new Intent(Camera2Activity.this, ImageProcessorActivity.class);
//                            intent.putExtra("CaptureImage", bmp);
//                            startActivity(intent);
//                        }
//                    });
                }
            };
            camera2.getCapture(captureCallback);
        }
    };

    private boolean bRecode = false;
    View.OnClickListener listenerRecode = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if(bRecode){
                bRecode = false;
                camera2.stopRecordingVideo();
                btnRecode.setText("Record");
            }
            else{
                bRecode = true;
                camera2.startRecordingVideo();
                btnRecode.setText("Stop");
            }
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
            int lens = getIntent().getExtras().getInt("lens");
            camera2 = new Camera2API(Camera2Activity.this, textureView);
            camera2.setLensFacing(lens);
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


    OverlayView.DrawCallback drawCallback = new OverlayView.DrawCallback() {
        @Override
        public void drawCallback(Canvas canvas) {
            Paint paint = Parameter.setPaint(Color.RED, 6);
            canvas.drawRect(0, 0, 100, 100, paint);
        }
    };
}
