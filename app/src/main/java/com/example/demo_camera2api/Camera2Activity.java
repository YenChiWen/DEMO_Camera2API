package com.example.demo_camera2api;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.FileOutputStream;
import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity {

    Button btnCapture;
    Button btnRecode;
    FloatingActionButton fabSync;
    TextureView textureView;
    OverlayView overlayView;
    Switch switchObject;
    Switch switchFace;
    TextView textView_plus;
    TextView textView_minus;
    TextView textView_confidence;

    int lens;
    String TAG = "YEN_camera2Activity";

    Camera2API camera2;
    Detector objectClassifier;
    Parameter parameter;
    DrawCanvas drawCanvas;

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
        parameter = new Parameter();
        drawCanvas = new DrawCanvas();

        btnCapture = findViewById(R.id.btn_capture);
        btnRecode = findViewById(R.id.btn_record);
        textureView = findViewById(R.id.textureView);
        fabSync = findViewById(R.id.fab_sync);
        overlayView = findViewById(R.id.overlayView_camera2);
        switchObject = findViewById(R.id.switch_object);
        switchFace = findViewById(R.id.switch_face);
        textView_plus = findViewById(R.id.textView_plus);
        textView_minus = findViewById(R.id.textView_minus);
        textView_confidence = findViewById(R.id.textView_confidence);

        btnCapture.setOnClickListener(listenerCapture);
        btnRecode.setOnClickListener(listenerRecode);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        fabSync.setOnClickListener(listenerSync);
        switchFace.setOnCheckedChangeListener(listener_face);
        switchObject.setOnCheckedChangeListener(listener_object);
        textView_plus.setOnClickListener(listener_plus);
        textView_minus.setOnClickListener(listener_minus);

        overlayView.addCallback(drawCanvas.drawCallback);
    }

    @SuppressLint("NewApi")
    private void tfModelInit(){
        Size previewSize = camera2.getmPreviewSize();

        objectClassifier = new Detector(getAssets(),
                Parameter.TF_OD_MODEL,
                Parameter.TF_OD_LABEL,
                Parameter.TF_OD_INPUT_SIZE,
                previewSize,
                90,
                Parameter.TF_OD_IS_QUANTIZED);
        objectClassifier.create();
    }

    public void setTextView_confidence() {
        int confidence = (int) (parameter.MINIMUM_CONFIDENCE_TF_OD_API * 100);
        textView_confidence.setText(String.valueOf(confidence));
    }



    View.OnClickListener listenerCapture = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            camera2.setbCapture(true);
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

            tfModelInit();
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

    CompoundButton.OnCheckedChangeListener listener_face = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(camera2 != null) {
                camera2.setbFaceDetector(isChecked);
            }
        }
    };

    CaptureImage.callback detectorCallback = new CaptureImage.callback() {
        @Override
        public void capture(Bitmap bmp) {
            Log.d(TAG, "Detector.");
            drawCanvas.setMappedRecognitions(parameter.objectDetector(objectClassifier, bmp));
            overlayView.postInvalidate();
        }
    };

    CaptureImage.callback captureCallback = new CaptureImage.callback() {
        @Override
        public void capture(Bitmap bmp) {
            Log.d(TAG, "Capture.");
            camera2.setbCapture(false);
        }
    };

    CompoundButton.OnCheckedChangeListener listener_object = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(camera2 != null) {
                camera2.setDetecteCallback(detectorCallback);
                camera2.setbObjectDetector(isChecked);
            }
        }
    };

    View.OnClickListener listener_plus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(parameter.MINIMUM_CONFIDENCE_TF_OD_API < 1){
                parameter.MINIMUM_CONFIDENCE_TF_OD_API +=  0.1f;
                setTextView_confidence();
            }
            else{
                Log.d(TAG, "Confidence is over then 100%.");
            }
        }
    };

    View.OnClickListener listener_minus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(parameter.MINIMUM_CONFIDENCE_TF_OD_API > 1E-2){
                parameter.MINIMUM_CONFIDENCE_TF_OD_API -= 0.1f;
                setTextView_confidence();
            }
            else{
                Log.d(TAG, "Confidence is less then 0%.");
            }
        }
    };
}
