package com.example.demo_camera2api;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;

import static com.example.demo_camera2api.R.id.layout_process;
import static com.example.demo_camera2api.R.id.masked;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity {

    // view
    Button btnCapture;
    Button btnRecode;
    Button btn_CPU;
    Button btn_GPU;
    Button btn_NNAPI;
    LinearLayout layoutProcess;
    FloatingActionButton fabSync;
    TextureView textureView;
    TextView textView_confidence_plus;
    TextView textView_confidence_minus;
    TextView textView_confidence;
    TextView textView_thread_plus;
    TextView textView_thread_minus;
    TextView textView_thread;
    OverlayView overlayView;
    Switch switchObject;
    Switch switchFace;
    Switch switchQuant;

    // variable
    int lens;
    String TAG = "YEN_camera2Activity";

    // class
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
        btn_CPU = findViewById(R.id.btn_cpu);
        btn_GPU = findViewById(R.id.btn_GPU);
        btn_NNAPI = findViewById(R.id.btn_nnapi);
        layoutProcess = findViewById(layout_process);
        textureView = findViewById(R.id.textureView);
        overlayView = findViewById(R.id.overlayView_camera2);
        fabSync = findViewById(R.id.fab_sync);
        switchObject = findViewById(R.id.switch_object);
        switchFace = findViewById(R.id.switch_face);
        switchQuant = findViewById(R.id.switch_quant);
        textView_confidence_plus = findViewById(R.id.textView_confidence_plus);
        textView_confidence_minus = findViewById(R.id.textView_confidence_minus);
        textView_confidence = findViewById(R.id.textView_confidence);
        textView_thread_plus = findViewById(R.id.textView_thread_plus);
        textView_thread_minus = findViewById(R.id.textView_thread_minus);
        textView_thread = findViewById(R.id.textView_thread);


        btnCapture.setOnClickListener(listenerCapture);
        btnRecode.setOnClickListener(listenerRecode);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        fabSync.setOnClickListener(listenerSync);
        switchFace.setOnCheckedChangeListener(listener_face);
        switchObject.setOnCheckedChangeListener(listener_object);
        switchQuant.setOnCheckedChangeListener(listener_quant);
        textView_confidence_plus.setOnClickListener(listener_confidence_plus);
        textView_confidence_minus.setOnClickListener(listener_confidence_minus);
        textView_thread_plus.setOnClickListener(listener_thread_plus);
        textView_thread_minus.setOnClickListener(listener_thread_minus);
        btn_CPU.setOnClickListener(listener_usingCpu);
        btn_GPU.setOnClickListener(listener_usingGpu);
        btn_NNAPI.setOnClickListener(listener_usingNNAPI);
        setTextView_confidence();
        setTextView_thread();

        overlayView.addCallback(drawCanvas.drawObjectCallback);
    }

    @SuppressLint("NewApi")
    private void initTfModel(){
        Size previewSize = camera2.getmPreviewSize();

        if(parameter.TF_OD_IS_QUANTIZED){
            objectClassifier = new Detector(getAssets(),
                    parameter.TF_OD_MODEL_QUANT,
                    parameter.TF_OD_LABEL,
                    parameter.TF_OD_INPUT_SIZE,
                    previewSize,
                    90,
                    parameter.NUM_DETECTIONS,
                    parameter.TF_OD_IS_QUANTIZED);
        }
        else{
            objectClassifier = new Detector(getAssets(),
                    parameter.TF_OD_MODEL,
                    parameter.TF_OD_LABEL,
                    parameter.TF_OD_INPUT_SIZE,
                    previewSize,
                    90,
                    parameter.NUM_DETECTIONS,
                    parameter.TF_OD_IS_QUANTIZED);
        }
        objectClassifier.create();
    }

    private void initCamera(){
        if(getIntent().getExtras() != null)
            lens = getIntent().getExtras().getInt("lens");
        camera2 = new Camera2API(Camera2Activity.this, textureView);
        camera2.setLensFacing(lens);
        camera2.init();
        camera2.setObjectDetectorCallback(detectorCallback);

        initTfModel();
    }

    private void setTextView_confidence() {
        int confidence = (int) (parameter.MINIMUM_CONFIDENCE_TF_OD_API * 100);
        textView_confidence.setText(String.valueOf(confidence));
    }

    private void setTextView_thread() {
        textView_thread.setText(String.valueOf(parameter.NUM_THREAD));

        if(objectClassifier != null){
            objectClassifier.setNumThreads(parameter.NUM_THREAD);
            camera2.setDetector(objectClassifier);
        }
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
            overlayView.setVisibility(bRecode ? View.VISIBLE : View.INVISIBLE);
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
            initCamera();
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

    CaptureImage.callback detectorCallback = new CaptureImage.callback() {
        @Override
        public void capture(Bitmap bmp) {
            Log.d(TAG, "Detector.");
            drawCanvas.setMappedRecognitions(parameter.objectDetector(objectClassifier, bmp));
            drawCanvas.setDetector(objectClassifier);
            overlayView.postInvalidate();
        }
    };

    CaptureImage.callback captureCallback = new CaptureImage.callback() {
        @Override
        public void capture(Bitmap bmp) {
            Log.d(TAG, "Capture.");
            camera2.setbCapture(false);

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, bs);
            byte[] bitmapByte = bs.toByteArray();

            Intent intent = new Intent(Camera2Activity.this, ImageProcessorActivity.class);
            intent.putExtra("CaptureImage", bitmapByte);
            startActivity(intent);
        }
    };

    CompoundButton.OnCheckedChangeListener listener_face = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(camera2 != null) {
                Toast.makeText(Camera2Activity.this, "face", Toast.LENGTH_LONG).show();

                camera2.setbFaceDetector(isChecked);
            }
        }
    };

    CompoundButton.OnCheckedChangeListener listener_object = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(camera2 != null) {
                Toast.makeText(Camera2Activity.this, "object detect", Toast.LENGTH_LONG).show();

                camera2.setbObjectDetector(isChecked);
                overlayView.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);

                parameter.enableDisableView(layoutProcess, isChecked);
            }
        }
    };

    CompoundButton.OnCheckedChangeListener listener_quant = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Toast.makeText(Camera2Activity.this, isChecked?"using quant model":"using float model", Toast.LENGTH_LONG).show();
            parameter.TF_OD_IS_QUANTIZED = isChecked;

            camera2.setbObjectDetector(false);
            initTfModel();
            camera2.setDetector(objectClassifier);
        }
    };

    View.OnClickListener listener_confidence_plus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(parameter.MINIMUM_CONFIDENCE_TF_OD_API < 1){
                parameter.MINIMUM_CONFIDENCE_TF_OD_API +=  0.1f;
                Toast.makeText(Camera2Activity.this,"confidence : " + String.valueOf(parameter.MINIMUM_CONFIDENCE_TF_OD_API), Toast.LENGTH_LONG).show();
                setTextView_confidence();
            }
            else{
                Log.d(TAG, "Confidence is over than 100%.");
            }
        }
    };

    View.OnClickListener listener_confidence_minus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(parameter.MINIMUM_CONFIDENCE_TF_OD_API > 1E-2){
                parameter.MINIMUM_CONFIDENCE_TF_OD_API -= 0.1f;
                Toast.makeText(Camera2Activity.this, "confidence : " + String.valueOf(parameter.MINIMUM_CONFIDENCE_TF_OD_API), Toast.LENGTH_LONG).show();
                setTextView_confidence();
            }
            else{
                Log.d(TAG, "Confidence is less than 0%.");
            }
        }
    };

    View.OnClickListener listener_thread_plus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(parameter.NUM_THREAD < 10){
                parameter.NUM_THREAD +=  1;
                Toast.makeText(Camera2Activity.this, "thread num : " + String.valueOf(parameter.NUM_THREAD), Toast.LENGTH_LONG).show();
                setTextView_thread();
            }
            else{
                Log.d(TAG, "Thread is over than 10.");
            }
        }
    };

    View.OnClickListener listener_thread_minus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(parameter.NUM_THREAD > 1){
                parameter.NUM_THREAD -= 1;
                Toast.makeText(Camera2Activity.this, "thread num : " + String.valueOf(parameter.NUM_THREAD), Toast.LENGTH_LONG).show();
                setTextView_thread();
            }
            else{
                Log.d(TAG, "Thread is less than 1.");
            }
        }
    };

    View.OnClickListener listener_usingCpu = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "listener_usingCpu: ");

            objectClassifier.useCpu();
            camera2.setDetector(objectClassifier);

            Toast.makeText(Camera2Activity.this, "using CPU", Toast.LENGTH_LONG).show();
        }
    };

    View.OnClickListener listener_usingGpu = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            Log.d(TAG, "listener_usingGpu: ");

            objectClassifier.useGpu();
            camera2.setDetector(objectClassifier);

            Toast.makeText(Camera2Activity.this, "using GPU", Toast.LENGTH_LONG).show();
        }
    };

    View.OnClickListener listener_usingNNAPI = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "listener_usingNNAPI: ");

            objectClassifier.useNNAPI();
            camera2.setDetector(objectClassifier);

            Toast.makeText(Camera2Activity.this, "using NNAPI", Toast.LENGTH_LONG).show();
        }
    };
}
