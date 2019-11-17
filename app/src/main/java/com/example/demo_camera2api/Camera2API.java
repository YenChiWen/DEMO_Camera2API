package com.example.demo_camera2api;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.ContactsContract;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2API {

    // input parameter
    private int mHeight, mWidth;
    private TextureView mTextureView;
    private Context mContext;

    // camera parameter
    private Size mPreviewSize;
    private int lensFacing = 1;
    private String mCameraID;
    private Surface mSurface;
    private CameraManager mCameraManager;   // list all camera, and set camera ID
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    //
    private final int REQUEST_CAMERA_PERMISSION = 0;


    Camera2API(Context context, TextureView textureView){
        this.mContext = context;
        this.mTextureView = textureView;
        this.mWidth = textureView.getWidth();
        this.mHeight = textureView.getHeight();
    }

    public void init(){
        mCameraManager = (CameraManager) this.mContext.getSystemService(Context.CAMERA_SERVICE);

        this.cameraRegister(this.lensFacing);
        this.cameraOpen();
    }



    private void cameraRegister(int lens_facing){
        // 0: LENS_FACING_FRONT
        // 1: LENS_FACING_BACK
        // 2: LENS_FACING_EXTERNAL
        try {
            for(String sCameraID : mCameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(sCameraID);
                if(lens_facing == cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)){
                    StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    this.mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), this.mWidth, this.mHeight);
                    this.mCameraID = sCameraID;
                    setupImageReader();
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight()
                            - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private void setupImageReader(){
        this.mImageReader = ImageReader.newInstance(this.mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 1);
        this.mImageReader.setOnImageAvailableListener(onImageAvailableListener, this.mBackgroundHandler);
    }



    public void cameraOpen() {
        try {
            if( ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this.mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                ActivityCompat.requestPermissions((Activity) this.mContext, new String[]{
                        Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
            }

            if(!this.mCameraID.equals(null))
                mCameraManager.openCamera(this.mCameraID, mCameraDeviceStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    };

    @SuppressLint("NewApi")
    private void createCameraPreview(){
        SurfaceTexture mSurfaceTexture = this.mTextureView.getSurfaceTexture();
        assert mSurfaceTexture != null;
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        try {
            // captureRequest.Builder, CameraCaptureSession.stateCallback
            mSurface = new Surface(mSurfaceTexture);
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            try {
                if(mCameraDevice == null) return;

                mPreviewSession = session;
                mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                mCaptureRequest = mCaptureRequestBuilder.build();
                mPreviewSession.setRepeatingRequest(mCaptureRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Toast.makeText(mContext, "Camera configuration change", Toast.LENGTH_SHORT).show();
        }
    };



    public void closeCamera(){
        if(mPreviewSession != null){
            mPreviewSession.close();
            mPreviewSession = null;
        }

        if(mCameraDevice != null){
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if(mImageReader != null){
            mImageReader.close();
            mImageReader = null;
        }
    }

    public void setLensFacing(int lensFacing) {
        this.lensFacing = lensFacing;
    }

    public int getLensFacing() {
        return lensFacing;
    }










    public void getCapture(){
        startBackgroundThread();

        try {
            this.mPreviewSession.stopRepeating();
            this.mPreviewSession.abortCaptures();

            this.mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            this.mPreviewSession.capture(mCaptureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                }


            }, this.mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        stopBackgroundThread();
    }

    private ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            final Image image = reader.acquireNextImage();
            mBackgroundHandler.post(new CaptureImage(image));
        }
    };



    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    public void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
