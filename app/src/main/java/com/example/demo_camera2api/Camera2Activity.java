package com.example.demo_camera2api;

import androidx.annotation.Nullable;
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
    int lens;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


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
            CaptureImage.previewCallback captureCallback = new CaptureImage.previewCallback() {
                @Override
                public void preview(final byte[] data) {
                    Log.d("YEN", "Capture.");
                    for(int i=0; i<Rect.length; i++){
                        Rect[i] = Rect[i] + 10*(i+1);
                    }
                    overlayView.postInvalidate();

//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Intent intent = new Intent(Camera2Activity.this, ImageProcessorActivity.class);
//                            intent.putExtra("CaptureImage", data);
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

            if(getIntent().getExtras() != null)
                lens = getIntent().getExtras().getInt("lens");
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

    int[] Rect = {0, 0, 0, 0};
    OverlayView.DrawCallback drawCallback = new OverlayView.DrawCallback() {
        @Override
        public void drawCallback(Canvas canvas) {
            Paint paint = Parameter.setPaint(Color.RED, 6);
            canvas.drawRect(Rect[0], Rect[1], Rect[2], Rect[3], paint);
        }
    };
}
