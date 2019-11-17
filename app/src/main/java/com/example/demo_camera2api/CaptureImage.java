package com.example.demo_camera2api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class CaptureImage implements Runnable{
    Image mImage;
    CaptureCallback captureCallback;
    private Bitmap bmpCapture;

    CaptureImage(Image image, CaptureCallback captureCallback){
        this.mImage = image;
        this.captureCallback = captureCallback;
    }

    @Override
    public void run() {
        ByteBuffer buffer = this.mImage.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        this.bmpCapture = BitmapFactory.decodeByteArray(data, 0, data.length);

        this.mImage.close();
        captureCallback.update(this.bmpCapture);
    }
}


interface CaptureCallback{
    void update(Bitmap bmp);
}

