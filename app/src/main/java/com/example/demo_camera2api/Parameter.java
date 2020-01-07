package com.example.demo_camera2api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.example.demo_camera2api.tflite.Classifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressLint("NewApi")
public class Parameter {
    // main activity
    public static FPSCalculator fpsCalculator = new FPSCalculator("detector_fps");

    private static Map<String, String> SOURCE = new HashMap<String, String>();
    public static Map<String, String> getSOURCE() {
        SOURCE.put("-1", "IMAGE");
        SOURCE.put("0", "LENS_FACING_FRONT");
        SOURCE.put("1", "LENS_FACING_BACK");
        SOURCE.put("2", "LENS_FACING_EXTERNAL");
        SOURCE.put("99", "test");
        return SOURCE;
    }


    //
    public final String TF_OD_MODEL = "detect_ori.tflite";
    public final String TF_OD_LABEL = "labelmap.txt";
    public static final Size TF_OD_INPUT_SIZE = new Size(300,300);
    public final boolean TF_OD_IS_QUANTIZED = true;

    public static final float IMAGE_MEAN = 128.0f;
    public static final float IMAGE_STD = 128.0f;
    public float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    public int NUM_DETECTIONS = 10;
    public int NUM_THREAD = 1;


    //
    public static void checkDir(String sPath){
        File file = new File(sPath);
        if(!file.exists())
            file.mkdirs();
    }

    public List<Classifier.Recognition> objectDetector(Detector detector, Bitmap bmp){
        List<Classifier.Recognition> results = detector.recognizeImage(bmp);

        List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();
        for(final Classifier.Recognition result : results){
            RectF location = result.getLocation();
            if(location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API){
                detector.getCropToFrameTransform().mapRect(location);
                result.setLocation(location);

                mappedRecognitions.add(result);
            }
        }

        fpsCalculator.calculate();

        return mappedRecognitions;
    }
}
