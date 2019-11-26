package com.example.demo_camera2api;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

public class ImageProcessorActivity extends AppCompatActivity {
    Button btnLoadImage;
    Button btnSaveImage;
    Switch switchFace;
    Switch switchObject;
    ImageView imageView;
    TextView textView_plus;
    TextView textView_minus;
    TextView textView_confidence;

    Detector objectClassifier;
    Parameter parameter;
    DrawCanvas drawCanvas;

    Bitmap bmpOri;
    Bitmap bmpCanvas;
    boolean bObjectDetector = false;
    String TAG = "YEN_ImageProcess";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processor);

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();

        getPutExtra();
    }

    private void init(){
        parameter = new Parameter();
        drawCanvas = new DrawCanvas();

        btnLoadImage = findViewById(R.id.btn_load);
        btnSaveImage = findViewById(R.id.btn_save);
        switchFace = findViewById(R.id.switch_face);
        switchObject = findViewById(R.id.switch_object);
        imageView = findViewById(R.id.imageView);
        textView_plus = findViewById(R.id.textView_plus);
        textView_minus = findViewById(R.id.textView_minus);
        textView_confidence = findViewById(R.id.textView_confidence);

        btnLoadImage.setOnClickListener(listener_load);
        btnSaveImage.setOnClickListener(listener_save);
        switchFace.setOnCheckedChangeListener(listener_face);
        switchObject.setOnCheckedChangeListener(listener_object);
        textView_plus.setOnClickListener(listener_plus);
        textView_minus.setOnClickListener(listener_minus);
        setTextView_confidence();

        bmpOri = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
        tfModelInit();
    }

    @SuppressLint("NewApi")
    private void tfModelInit(){
        Size previewSize = new Size(bmpOri.getWidth(),bmpOri.getHeight());

        objectClassifier = new Detector(getAssets(),
                Parameter.TF_OD_MODEL,
                Parameter.TF_OD_LABEL,
                Parameter.TF_OD_INPUT_SIZE,
                previewSize,
                0,
                Parameter.TF_OD_IS_QUANTIZED);
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

    public void setTextView_confidence() {
        int confidence = (int) (parameter.MINIMUM_CONFIDENCE_TF_OD_API * 100);
        textView_confidence.setText(String.valueOf(confidence));
    }

    private void ImageDetecte(){
        drawCanvas.setMappedRecognitions(parameter.objectDetector(objectClassifier, bmpOri));
        drawCanvas.drawBitmap(bmpCanvas);
        imageView.setImageBitmap(bmpCanvas);
    }

    private void reDetecte(){
        if(bObjectDetector){
            imageView.setImageBitmap(bmpOri);
            bmpCanvas = bmpOri.copy(Bitmap.Config.ARGB_8888, true);
            ImageDetecte();
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
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            bObjectDetector = isChecked;
            if(isChecked){
                reDetecte();
            }
            else{
                imageView.setImageBitmap(bmpOri);
            }
        }
    };

    View.OnClickListener listener_plus = new View.OnClickListener() {
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

    View.OnClickListener listener_minus = new View.OnClickListener() {
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
