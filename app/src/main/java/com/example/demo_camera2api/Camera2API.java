package com.example.demo_camera2api;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


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
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private CameraCaptureSession mPreviewSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    //
    private final int REQUEST_CAMERA_PERMISSION = 0;

    Camera2API(Context context, TextureView textureView){
        this.mContext = context;
        this.mTextureView = textureView;
        this.mWidth = textureView.getWidth();
        this.mHeight = textureView.getHeight();
        this.mCameraManager = (CameraManager) this.mContext.getSystemService(Context.CAMERA_SERVICE);
    }

    public void init(){
        this.cameraRegister(this.lensFacing);
        this.cameraOpen();
    }

    static List<String> ScanAllCamera(Context context){
        List<String> sResult = new ArrayList<>();
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            for(String sCameraID : cameraManager.getCameraIdList()){
                sResult.add(Parameter.getSOURCE().get(sCameraID));
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return sResult;
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
        startBackgroundThread();
        this.mImageReader = ImageReader.newInstance(this.mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.JPEG, 1);
        this.mImageReader.setOnImageAvailableListener(imageAvailableListener, this.mBackgroundHandler);
    }

    ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader imageReader) {

            final Image image = imageReader.acquireNextImage();
            mBackgroundHandler.post(new CaptureImage(image, captureCallback));
        }
    };





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
                mPreviewSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
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

        stopBackgroundThread();

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

    public void setLensFacing(int lensFacing) {
        this.lensFacing = lensFacing;
    }

    public int getLensFacing() {
        return lensFacing;
    }

    private CaptureCallback captureCallback;
    public void getCapture(CaptureCallback captureCallback){
        try {
            this.captureCallback = captureCallback;

            this.mCaptureRequestBuilder.addTarget(this.mImageReader.getSurface());
            this.mPreviewSession.capture(this.mCaptureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
