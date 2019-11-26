package com.example.demo_camera2api;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.example.demo_camera2api.tflite.ImageUtils;

import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
@SuppressLint("NewApi")
public class CaptureImage implements Runnable{
    Image mImage;
    callback callback;
    private int[] rgbBytes = null;
    private Bitmap bmpCapture;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];

    CaptureImage(Image image, callback callback){
        this.mImage = image;
        this.callback = callback;
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    @Override
    public void run() {
        if(mImage == null)
            return;
        if(isProcessingFrame){
            mImage.close();
            return;
        }

        isProcessingFrame = true;
        if(mImage.getFormat() == ImageFormat.JPEG){
            ByteBuffer buffer = this.mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            this.bmpCapture = BitmapFactory.decodeByteArray(data, 0, data.length);
        }
        else if(mImage.getFormat() == ImageFormat.YUV_420_888){
            final Image.Plane[] planes = mImage.getPlanes();
            fillBytes(planes, yuvBytes);
            int yRowStride = planes[0].getRowStride();
            int uvRowStride = planes[1].getRowStride();
            int uvPixelStride = planes[1].getPixelStride();
            rgbBytes = new int[mImage.getWidth()*mImage.getHeight()];

            ImageUtils.convertYUV420ToARGB8888(
                    yuvBytes[0], yuvBytes[1], yuvBytes[2],
                    mImage.getWidth(), mImage.getHeight(),
                    yRowStride, uvRowStride, uvPixelStride,
                    rgbBytes);

            bmpCapture = Bitmap.createBitmap(mImage.getWidth(), mImage.getHeight(), Bitmap.Config.ARGB_8888);
            bmpCapture.setPixels(rgbBytes, 0, mImage.getWidth(), 0, 0, mImage.getWidth(), mImage.getHeight());
        }
        isProcessingFrame = false;

        this.mImage.close();
        callback.capture(bmpCapture);
    }

    interface callback {
        void capture(Bitmap bmp);
    }
}

