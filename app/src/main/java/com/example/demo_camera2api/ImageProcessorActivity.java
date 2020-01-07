package com.example.demo_camera2api;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class ImageProcessorActivity extends AppCompatActivity {
    Button btnLoadImage;
    Button btnSaveImage;
    Switch switchFace;
    Switch switchObject;
    ImageView imageView;
    TextView textView_confidence_plus;
    TextView textView_confidence_minus;
    TextView textView_confidence;
    LinearLayout layoutProcess;

    Detector objectClassifier;
    Parameter parameter;
    DrawCanvas drawCanvas;

    Bitmap bmpOri;
    Bitmap bmpCanvas;
    String TAG = "YEN_ImageProcess";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processor);

        init();
    }

    private void init(){
        parameter = new Parameter();
        drawCanvas = new DrawCanvas();

        btnLoadImage = findViewById(R.id.btn_load);
        btnSaveImage = findViewById(R.id.btn_save);
        switchFace = findViewById(R.id.switch_face);
        switchObject = findViewById(R.id.switch_object);
        imageView = findViewById(R.id.imageView);
        textView_confidence_plus = findViewById(R.id.textView_confidence_plus);
        textView_confidence_minus = findViewById(R.id.textView_confidence_minus);
        textView_confidence = findViewById(R.id.textView_confidence);
        layoutProcess = findViewById(R.id.layout_process);

        btnLoadImage.setOnClickListener(listener_load);
        btnSaveImage.setOnClickListener(listener_save);
        switchFace.setOnCheckedChangeListener(listener_face);
        switchObject.setOnCheckedChangeListener(listener_object);
        textView_confidence_plus.setOnClickListener(listener_confidence_plus);
        textView_confidence_minus.setOnClickListener(listener_confidence_minus);

        setTextView_confidence();

        getPutExtra();
        bmpOri = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        initTfModel();
    }

    @SuppressLint("NewApi")
    private void initTfModel(){
        Size previewSize = new Size(bmpOri.getWidth(),bmpOri.getHeight());

        objectClassifier = new Detector(getAssets(),
                parameter.TF_OD_MODEL,
                parameter.TF_OD_LABEL,
                parameter.TF_OD_INPUT_SIZE,
                previewSize,
                0,
                parameter.NUM_DETECTIONS,
                parameter.TF_OD_IS_QUANTIZED);
        objectClassifier.create();
    }

    private void getPutExtra(){
        if(getIntent().getExtras() != null){
            imageView.setImageBitmap(null);

            Intent intent = getIntent();
            byte[] data = intent.getByteArrayExtra("CaptureImage");
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            imageView.setImageBitmap(bitmap);
        }
    }

    private void setTextView_confidence() {
        int confidence = (int) (parameter.MINIMUM_CONFIDENCE_TF_OD_API * 100);
        textView_confidence.setText(String.valueOf(confidence));
    }

    private void ImageDetecte(){
        drawCanvas.setMappedRecognitions(parameter.objectDetector(objectClassifier, bmpOri));
        drawCanvas.drawBitmap(bmpCanvas);
        imageView.setImageBitmap(bmpCanvas);
    }

    private void reDetecte(){
        imageView.setImageBitmap(bmpOri);
        bmpCanvas = bmpOri.copy(Bitmap.Config.ARGB_8888, true);
        ImageDetecte();
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
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            ConstraintLayout layout_ob_confidence = findViewById(R.id.layout_ob_confidence);

            Parameter.enableDisableView(layoutProcess, isChecked);
            if(isChecked){
                Toast.makeText(ImageProcessorActivity.this, "object detect", Toast.LENGTH_LONG).show();
                reDetecte();
            }
            else {
                imageView.setImageBitmap(bmpOri);
            }
        }
    };

    View.OnClickListener listener_confidence_plus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(parameter.MINIMUM_CONFIDENCE_TF_OD_API < 1){
                parameter.MINIMUM_CONFIDENCE_TF_OD_API +=  0.1f;
                setTextView_confidence();
                reDetecte();
            }
            else{
                Log.d(TAG, "Confidence is over then 100%.");
            }
        }
    };

    View.OnClickListener listener_confidence_minus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(parameter.MINIMUM_CONFIDENCE_TF_OD_API > 1E-2){
                parameter.MINIMUM_CONFIDENCE_TF_OD_API -= 0.1f;
                setTextView_confidence();
                reDetecte();
            }
            else{
                Log.d(TAG, "Confidence is less then 0%.");
            }

        }
    };
}
