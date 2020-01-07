package com.example.demo_camera2api;

import android.util.Log;

/**
 * Created by HankWu on 2017/9/15.
 */

public class FPSCalculator {
    long ms;
    long totalCount;
    int fps;
    int numberOfCountFPS = 0;
    int totalFPSCount = 0;
    long time = -1;
    long startTime = -1;
    int avg = -1;
    int max = -1;
    int min = -1;
    long endTime = 0;
    String TAG = "FPS";
    String PREFIX_TAG = "";

    private String rightpad(String text, int length) {
        return String.format("%-" + length + "." + length + "s", text);
    }

    public int getTotalCount() {
        return (int)totalCount;
    }

    public FPSCalculator() {

    }

    public FPSCalculator(String prefix) {
        PREFIX_TAG = prefix;
        TAG = PREFIX_TAG + " " + TAG;
    }

    public void calculateMilliSeconds(){
        if(time == -1) time = System.currentTimeMillis();
        if(startTime == -1) startTime = System.currentTimeMillis();

        endTime = System.currentTimeMillis();
        ms = endTime - startTime;
        Log.d(TAG, "ms: " + ms);
        startTime = endTime;
    }


    public void calculate() {
        if(time==-1) time = System.currentTimeMillis();
        if(startTime==-1) startTime = System.currentTimeMillis();


        endTime = System.currentTimeMillis();
        fps++;
        totalCount++;
        if(System.currentTimeMillis()-time >= 1000) {
            Log.d(rightpad(TAG, 25), "FPS:"+fps+",avg:"+getAVG()+",max:"+getMAX()+",min:"+getMIN());
            time = System.currentTimeMillis();
            if(max==-1) max = fps;
            if(min==-1) min = fps;
            if(fps>max) max = fps;
            if(fps<min) min = fps;
            numberOfCountFPS++;
            totalFPSCount += fps;
            fps = 0;
        }
    }

    public int getMAX() {
        return max;
    }

    public int getMIN() {
        return min;
    }

    public int getAVG() {
        if(numberOfCountFPS==0) return 0;
        return (int) (totalFPSCount/numberOfCountFPS);
    }
}
