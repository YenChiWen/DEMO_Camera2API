package com.example.demo_camera2api;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Size;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("NewApi")
public class Parameter {
    // main activity
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
    public static final String TF_OD_MODEL = "detect.tflite";
    public static final String TF_OD_LABEL = "labelmap.txt";
    public static final Size TF_OD_INPUT_SIZE = new Size(300,300);
    public static final boolean TF_OD_IS_QUANTIZED = true;
    public static final int NUM_THREADS = 4;

    public static final float IMAGE_MEAN = 128.0f;
    public static final float IMAGE_STD = 128.0f;



    //
    public static void checkDir(String sPath){
        File file = new File(sPath);
        if(!file.exists())
            file.mkdirs();
    }

    public static Paint setPaint(int color, int strokeWidth){
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }
}
