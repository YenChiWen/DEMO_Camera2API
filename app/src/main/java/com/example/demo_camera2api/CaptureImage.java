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
    updateCallback updateCallback;
    private Bitmap bmpCapture;

    CaptureImage(Image image, updateCallback updateCallback){
        this.mImage = image;
        this.updateCallback = updateCallback;
    }

    @Override
    public void run() {
        ByteBuffer buffer = this.mImage.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        this.bmpCapture = BitmapFactory.decodeByteArray(data, 0, data.length);

        this.mImage.close();
        updateCallback.update(this.bmpCapture);
    }

    interface updateCallback {
        void update(Bitmap bmp);
    }
}

