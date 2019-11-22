package com.example.demo_camera2api;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.example.demo_camera2api.tflite.Classifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class ImageProcessorActivity extends AppCompatActivity {
    Button btnLoadImage;
    Button btnSaveImage;
    Switch switchFace;
    Switch switchObject;
    ImageView imageView;
    TextView textView_plus;
    TextView textView_minus;
    TextView textView_confidence;

    Detector detector;
    Paint paint;

    Bitmap bmpOri;
    Bitmap bmpCanvas;
    float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    boolean bDetector = false;

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
        paint = Parameter.setPaint(Color.RED, 6);
    }

    @SuppressLint("NewApi")
    private void tfModelInit(){
        Size previewSize = new Size(bmpOri.getWidth(),bmpOri.getHeight());

        detector = new Detector(getAssets(),
                Parameter.TF_OD_MODEL,
                Parameter.TF_OD_LABEL,
                Parameter.TF_OD_INPUT_SIZE,
                previewSize,
                Parameter.TF_OD_IS_QUANTIZED);
        detector.create();
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
        int confidence = (int) (MINIMUM_CONFIDENCE_TF_OD_API * 100);
        textView_confidence.setText(String.valueOf(confidence));
    }

    private void ImageDetecte(){
        List<Classifier.Recognition> results = detector.recognizeImage(bmpOri);
        Log.d("YEN", "success");

        Canvas canvas = new Canvas(bmpCanvas);

        for(final Classifier.Recognition result : results){
            RectF location = result.getLocation();
            if(location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API){
                detector.getCropToFrameTransform().mapRect(location);
                result.setLocation(location);

                canvas.drawRect(location, paint);
            }
        }

        imageView.setImageBitmap(bmpCanvas);
    }

    private void reDetecte(){
        if(bDetector){
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
            bDetector = isChecked;
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
            MINIMUM_CONFIDENCE_TF_OD_API +=  0.1f;
            setTextView_confidence();
            reDetecte();
        }
    };

    View.OnClickListener listener_minus = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MINIMUM_CONFIDENCE_TF_OD_API -= 0.1f;
            setTextView_confidence();
            reDetecte();
        }
    };
}
