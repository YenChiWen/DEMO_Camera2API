package com.example.demo_camera2api;

import android.graphics.Color;
import android.graphics.Paint;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Parameter {
    private static Map<String, String> SOURCE = new HashMap<String, String>();

    public static Map<String, String> getSOURCE() {
        SOURCE.put("-1", "IMAGE");
        SOURCE.put("0", "LENS_FACING_FRONT");
        SOURCE.put("1", "LENS_FACING_BACK");
        SOURCE.put("2", "LENS_FACING_EXTERNAL");
        SOURCE.put("99", "test");
        return SOURCE;
    }

    public static void checkDir(String sPath){
        File file = new File(sPath);
        if(!file.exists())
            file.mkdirs();
    }

    public static Paint setPaint(int color, int strokeWidth){
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        return paint;
    }
}
