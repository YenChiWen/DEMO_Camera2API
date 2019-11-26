package com.example.demo_camera2api;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.example.demo_camera2api.tflite.Classifier;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

public class DrawCanvas {
    private Paint paint_rect, paint_text;
    private int text_size = 30;
    private int margin = 5;
    private String TAG = "YEN_DrawCanvas";

    DecimalFormat df = new DecimalFormat("#.##");
    public static final int[] COLORS = {
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"),
            Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
    };

    List<Classifier.Recognition> mappedRecognitions = new LinkedList<Classifier.Recognition>();

    DrawCanvas(){
        paint_rect = setPaint(Color.RED, 6);
        paint_text = setPaint(Color.RED, 3);
        paint_text.setTextSize(text_size);
        paint_text.setStyle(Paint.Style.FILL);
    }

    private static Paint setPaint(int color, int strokeWidth){
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }

    public void drawBitmap(Bitmap bmpCanvas){
        Canvas canvas = new Canvas(bmpCanvas);
        draw(canvas);
    }

    OverlayView.DrawCallback drawCallback = new OverlayView.DrawCallback() {
        @Override
        public void drawCallback(Canvas canvas) {
            draw(canvas);
        }
    };

    private void draw(Canvas canvas){
        int iCount = 0;
        for(Classifier.Recognition result : mappedRecognitions){
            int color = DrawCanvas.COLORS[iCount++];
            getPaint_rect().setColor(color);
            getPaint_text().setColor(color);
            String text = result.getTitle() + " " + df.format(result.getConfidence());

            canvas.drawText(text, result.getLocation().left + margin, result.getLocation().top + text_size + margin, getPaint_text());
            canvas.drawRect(result.getLocation(), getPaint_rect());

            Log.d(TAG, text);
        }
    }

    public Paint getPaint_rect() {
        return paint_rect;
    }

    public Paint getPaint_text() {
        return paint_text;
    }

    public void setMappedRecognitions(List<Classifier.Recognition> mappedRecognitions) {
        this.mappedRecognitions = mappedRecognitions;
    }
}
